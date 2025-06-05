package com.example.nocturnevpn.utils;

import com.example.nocturnevpn.model.Server;
import com.nocturne.vpn.models.ServerGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerGroupingUtil {
    private static int parsePing(String pingStr) {
        try {
            // Remove any non-numeric characters except decimal point
            String cleanPing = pingStr.replaceAll("[^0-9.]", "");
            if (cleanPing.isEmpty()) {
                return 0;
            }
            return Integer.parseInt(cleanPing);
        } catch (NumberFormatException e) {
            return 0; // Return 0 for invalid ping values
        }
    }

    public static List<ServerGroup> groupServersByCountry(List<Server> servers) {
        Map<String, List<Server>> countryMap = new HashMap<>();
        
        // Group servers by country
        for (Server server : servers) {
            String countryKey = server.getCountryLong();
            if (!countryMap.containsKey(countryKey)) {
                countryMap.put(countryKey, new ArrayList<>());
            }
            countryMap.get(countryKey).add(server);
        }
        
        // Convert to ServerGroup list
        List<ServerGroup> serverGroups = new ArrayList<>();
        for (Map.Entry<String, List<Server>> entry : countryMap.entrySet()) {
            String countryName = entry.getKey();
            List<Server> countryServers = entry.getValue();
            
            // Get country code from first server in the list
            String countryCode = countryServers.get(0).getCountryShort();
            
            // Convert Server to our new Server model
            List<com.nocturne.vpn.models.Server> convertedServers = new ArrayList<>();
            for (Server server : countryServers) {
                convertedServers.add(new com.nocturne.vpn.models.Server(
                    server.getHostName(), // Use hostName as ID
                    server.getHostName(), // Use hostName as name
                    server.getIpAddress(),
                    server.getPort(),
                    server.getProtocol(),
                    parsePing(server.getPing()), // Use safe parsing for ping
                    server.getIpAddress().equals(server.getIpAddress()) // isSelected
                ));
            }
            
            serverGroups.add(new ServerGroup(countryName, countryCode, convertedServers, false));
        }
        
        return serverGroups;
    }
} 