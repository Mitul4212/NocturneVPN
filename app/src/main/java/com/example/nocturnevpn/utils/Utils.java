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


}
