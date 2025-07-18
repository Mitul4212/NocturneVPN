package com.example.nocturnevpn.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.util.Pair;

import com.example.nocturnevpn.R;

import org.json.JSONObject;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;

public class Utils {

    private static final Map<String, Pair<Double, Double>> countryCoordinates = new HashMap<>();

    /**
     * Convert drawable image resource to string
     *
     * @param resourceId drawable image resource
     * @return image path
     */
    public static String getImgURL(int resourceId) {

        // Use BuildConfig.APPLICATION_ID instead of R.class.getPackage().getName() if both are not same
        return Uri.parse("android.resource://" + R.class.getPackage().getName() + "/" + resourceId).toString();
    }

//    private static final Map<String, Pair<Double, Double>> countryCoordinates = new HashMap<>();
//
//    static {
//        countryCoordinates.put("United States", new Pair<>(37.0902, -95.7129));
//        countryCoordinates.put("India", new Pair<>(20.5937, 78.9629));
//        countryCoordinates.put("Germany", new Pair<>(51.1657, 10.4515));
//        countryCoordinates.put("United Kingdom", new Pair<>(55.3781, -3.4360));
//        countryCoordinates.put("Japan", new Pair<>(36.2048, 138.2529));
//        // Add more countries here
//    }


    // Load country coordinates from JSON in assets
    public static void loadCountryCoordinates(Context context) {
        try {
            InputStream is = context.getAssets().open("allCountrys.json");
            Scanner scanner = new Scanner(is).useDelimiter("\\A");
            String jsonString = scanner.hasNext() ? scanner.next() : "";
            scanner.close();

            JSONObject jsonObject = new JSONObject(jsonString);
            Iterator<String> keys = jsonObject.keys();

            while (keys.hasNext()) {
                String country = keys.next();
                JSONObject obj = jsonObject.getJSONObject(country);
                double lat = obj.getDouble("latitude");
                double lng = obj.getDouble("longitude");
                countryCoordinates.put(country, new Pair<>(lat, lng));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns coordinates for a given country name.
     *
     * @param country Country name (e.g., "Germany")
     * @return Pair of latitude and longitude or null if not found
     */
    public static Pair<Double, Double> getCoordinatesByCountry(String country) {

        Pair<Double, Double> coords = countryCoordinates.get(country);
        if (coords != null) {
            Log.d("Utils", "Returning coordinates for: " + country +
                    " => Latitude: " + coords.first + ", Longitude: " + coords.second);
        } else {
            Log.w("Utils", "No coordinates found for: " + country);
        }

        return countryCoordinates.get(country);
    }

    /**
     * Performs a live ping to the given IP address. Returns round-trip time in ms, or -1 if unreachable.
     */
    public static int pingHost(String ipAddress, int timeoutMs) {
        try {
            long start = System.currentTimeMillis();
            // Use system ping command for better accuracy
            Process process = Runtime.getRuntime().exec("ping -c 1 -W " + (timeoutMs / 1000) + " " + ipAddress);
            int returnVal = process.waitFor();
            long end = System.currentTimeMillis();
            if (returnVal == 0) {
                return (int) (end - start);
            } else {
                return -1;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Performs a TCP ping to the given IP address and port. Returns round-trip time in ms, or -1 if unreachable.
     */
    public static int tcpPing(String ipAddress, int port, int timeoutMs) {
        try {
            long start = System.currentTimeMillis();
            java.net.Socket socket = new java.net.Socket();
            socket.connect(new java.net.InetSocketAddress(ipAddress, port), timeoutMs);
            socket.close();
            return (int) (System.currentTimeMillis() - start);
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Performs a UDP ping to the given IP address and port. Returns time in ms if no exception, or -1 if unreachable.
     * Note: Most servers will not reply, so this is best effort only.
     */
    public static int udpPing(String ipAddress, int port, int timeoutMs) {
        java.net.DatagramSocket socket = null;
        try {
            socket = new java.net.DatagramSocket();
            socket.setSoTimeout(timeoutMs);
            byte[] buf = new byte[1];
            java.net.DatagramPacket packet = new java.net.DatagramPacket(buf, buf.length, java.net.InetAddress.getByName(ipAddress), port);
            long start = System.currentTimeMillis();
            socket.send(packet);
            // Try to receive (most servers won't reply, so just measure send time)
            try {
                socket.receive(packet);
                return (int) (System.currentTimeMillis() - start);
            } catch (Exception e) {
                // No reply, but send succeeded
                return (int) (System.currentTimeMillis() - start);
            }
        } catch (Exception e) {
            return -1;
        } finally {
            if (socket != null) socket.close();
        }
    }
}
