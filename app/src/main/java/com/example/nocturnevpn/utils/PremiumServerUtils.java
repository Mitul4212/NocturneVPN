package com.example.nocturnevpn.utils;

import android.util.Log;
import com.example.nocturnevpn.model.Server;
import java.util.*;

public class PremiumServerUtils {
    /**
     * Runs premium server selection and live ping logic on the given server list.
     * Updates ping and premium status in-place.
     * Returns the updated list.
     */
    public static List<Server> calculatePremiumServers(List<Server> serverList) {
        Log.d("PremiumServerUtils", "Starting premium server selection, total servers: " + serverList.size());
        // Step 1: Deduplicate by ip+port
        Map<String, Server> uniqueServers = new HashMap<>();
        for (Server s : serverList) {
            String key = s.getIpAddress() + ":" + s.port;
            if (!uniqueServers.containsKey(key)) {
                uniqueServers.put(key, s);
            }
        }
        List<Server> filtered = new ArrayList<>();
        for (Server s : uniqueServers.values()) {
            // Step 2: Filter by protocol (TCP or UDP)
            if (!"tcp".equalsIgnoreCase(s.protocol) && !"udp".equalsIgnoreCase(s.protocol)) continue;
            // Step 3: Filter by load
            if (s.vpnSessions > 500) continue;
            filtered.add(s);
        }
        Log.d("PremiumServerUtils", "After filtering, servers left: " + filtered.size());
        // Step 4: TCP/UDP ping (use fallback if fails)
        for (Server s : filtered) {
            int livePing = -1;
            String pingType = "";
            try {
                int port = s.port > 0 ? s.port : ("tcp".equalsIgnoreCase(s.protocol) ? 443 : 1194);
                if ("tcp".equalsIgnoreCase(s.protocol)) {
                    livePing = Utils.tcpPing(s.getIpAddress(), port, 2000);
                    pingType = "tcpPing";
                } else if ("udp".equalsIgnoreCase(s.protocol)) {
                    // For UDP, always use reported/default ping
                    try {
                        livePing = Integer.parseInt(s.ping);
                        pingType = "defaultPing";
                    } catch (Exception e) {
                        livePing = 999;
                        pingType = "defaultPing";
                    }
                }
            } catch (Exception e) {
                livePing = -1;
            }
            if (livePing < 0) {
                // Fallback to reported ping or set high value
                try {
                    livePing = Integer.parseInt(s.ping);
                    pingType = "defaultPing";
                } catch (Exception e) {
                    livePing = 999;
                    pingType = "defaultPing";
                }
            }
            s.ping = String.valueOf(livePing);
            Log.d("PremiumServerUtils", "Ping result for " + s.getIpAddress() + " (" + s.protocol + ":" + s.port + "): " + livePing + " [" + pingType + "]");
        }
        Log.d("PremiumServerUtils", "After ping, servers left: " + filtered.size());
        if (filtered.isEmpty()) {
            Log.d("PremiumServerUtils", "No servers left after ping. Exiting.");
            return serverList;
        }
        // Step 5: Find min/max for normalization
        int minPing = Integer.MAX_VALUE, maxPing = Integer.MIN_VALUE;
        long minSpeed = Long.MAX_VALUE, maxSpeed = Long.MIN_VALUE;
        long minSessions = Long.MAX_VALUE, maxSessions = Long.MIN_VALUE;
        for (Server s : filtered) {
            int p = Integer.parseInt(s.ping);
            minPing = Math.min(minPing, p); maxPing = Math.max(maxPing, p);
            minSpeed = Math.min(minSpeed, s.speed); maxSpeed = Math.max(maxSpeed, s.speed);
            minSessions = Math.min(minSessions, s.vpnSessions); maxSessions = Math.max(maxSessions, s.vpnSessions);
        }
        Log.d("PremiumServerUtils", "minPing=" + minPing + ", maxPing=" + maxPing + ", minSpeed=" + minSpeed + ", maxSpeed=" + maxSpeed + ", minSessions=" + minSessions + ", maxSessions=" + maxSessions);
        // Step 6: Score
        double w_ping = 0.5, w_speed = 0.3, w_load = 0.2;
        Map<Server, Double> serverScoreMap = new HashMap<>();
        for (Server s : filtered) {
            int p = Integer.parseInt(s.ping);
            double score_ping = 1.0 - ((double)(p - minPing) / Math.max(1, maxPing - minPing));
            double score_speed = (double)(s.speed - minSpeed) / Math.max(1, maxSpeed - minSpeed);
            double score_load = 1.0 - ((double)(s.vpnSessions - minSessions) / Math.max(1, maxSessions - minSessions));
            double finalScore;
            if ("udp".equalsIgnoreCase(s.protocol)) {
                // For UDP, ignore ping: only use speed and load
                finalScore = (w_speed / (w_speed + w_load)) * score_speed + (w_load / (w_speed + w_load)) * score_load;
            } else {
                // For TCP, use all three
                finalScore = w_ping * score_ping + w_speed * score_speed + w_load * score_load;
            }
            Log.d("PremiumServerUtils", "Server " + s.getIpAddress() + " score: " + finalScore + " (ping=" + s.ping + ", speed=" + s.speed + ", sessions=" + s.vpnSessions + ", protocol=" + s.protocol + ")");
            serverScoreMap.put(s, finalScore);
        }
        // After scoring all servers, select the greater of 20 or 20% as premium
        List<Server> sortedByScore = new ArrayList<>(filtered);
        sortedByScore.sort((a, b) -> Double.compare(serverScoreMap.get(b), serverScoreMap.get(a)));
        int percentCount = (int) Math.ceil(sortedByScore.size() * 0.2);
        int premiumCount = Math.max(20, percentCount);
        if (percentCount < 1) premiumCount = 0;
        for (int i = 0; i < sortedByScore.size(); i++) {
            Server s = sortedByScore.get(i);
            s.setPremium(i < premiumCount);
        }
        // Log premium servers
        StringBuilder premiumLog = new StringBuilder("Premium servers selected: ");
        for (int i = 0; i < premiumCount; i++) {
            Server s = sortedByScore.get(i);
            premiumLog.append(s.getIpAddress())
                      .append(" (score=")
                      .append(serverScoreMap.get(s))
                      .append(") ");
        }
        Log.d("PremiumServerUtils", premiumLog.toString());
        return serverList;
    }
} 