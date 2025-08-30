package com.nocturnevpn;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.nocturnevpn.model.Server;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;

public class SharedPreference {

    private static final String APP_PREFS_NAME = "LibertyVPNPreference";

    private SharedPreferences mPreference;
    private SharedPreferences.Editor mPrefEditor;
    private Context context;

    private static final String SERVER_COUNTRY_LONG = "server_country_long";
    private static final String SERVER_COUNTRY_SHORT = "server_country_short";
    private static final String SERVER_SPEED = "server_speed";
    private static final String SERVER_PING = "server_ping";
    private static final String SERVER_PROTOCOL = "server_protocol";
    private static final String SERVER_IP_ADDRESS = "server_ip";
    private static final String SERVER_HOSTNAME = "server_hostname";
    private static final String SERVER_OVPN = "server_ovpn";
    private static final String SERVER_PORT = "server_port";
    private static final String SERVER_LIST_JSON = "server_list_json";
    private static final String USER_SIGNED_IN = "user_signed_in";
    private static final String USER_NAME = "user_name";
    private static final String PROTOCOL_FILTER = "protocol_filter"; // Values: ALL, TCP, UDP
    private static final String DARK_MODE_ENABLED = "dark_mode_enabled"; // Dark mode preference
    private static final Gson gson = new Gson();

    public SharedPreference(Context context) {
        this.mPreference = context.getSharedPreferences(APP_PREFS_NAME, Context.MODE_PRIVATE);
        this.mPrefEditor = mPreference.edit();
        this.context = context;
    }

    /**
     * Save server details
     *
     * @param server details of ovpn server
     */
    public void saveServer(Server server) {
        mPrefEditor.putString(SERVER_HOSTNAME, server.getHostName());
        mPrefEditor.putString(SERVER_IP_ADDRESS, server.getIpAddress());
        mPrefEditor.putString(SERVER_COUNTRY_LONG, server.getCountryLong());
        mPrefEditor.putString(SERVER_COUNTRY_SHORT, server.getCountryShort());
        mPrefEditor.putLong(SERVER_SPEED, server.getSpeed());
        mPrefEditor.putString(SERVER_PING, server.getPing());
        mPrefEditor.putString(SERVER_PROTOCOL, server.getProtocol());
        mPrefEditor.putString(SERVER_OVPN, server.getOvpnConfigData());
        mPrefEditor.putInt(SERVER_PORT, server.getPort());
        mPrefEditor.commit();

        Log.d("SharedPref", "save Server saved: " + server.getIpAddress());
    }

    /**
     * Get server data from shared preference
     *
     * @return server model object
     */
    public Server getServer() {

        Server server = new Server(
                mPreference.getString(SERVER_HOSTNAME,"Japan"),
                mPreference.getString(SERVER_IP_ADDRESS,"x.x.x.x"),
                mPreference.getString(SERVER_PING,"10ms"),
                mPreference.getLong(SERVER_SPEED,10),
                mPreference.getString(SERVER_COUNTRY_LONG,"Japan"),
                mPreference.getString(SERVER_COUNTRY_SHORT,"Japan"),
                mPreference.getString(SERVER_OVPN,"null"),
                mPreference.getInt(SERVER_PORT,402),
                mPreference.getString(SERVER_PROTOCOL,"UDP")
        );

        Log.d("SharedPref", "get Server saved: " + server.getIpAddress());

        return server;
    }

    /**
     * Save the full server list (with ping and premium status) as JSON
     */
    public void saveServerList(List<Server> serverList) {
        String json = gson.toJson(serverList);
        mPrefEditor.putString(SERVER_LIST_JSON, json);
        mPrefEditor.commit();
        Log.d("SharedPref", "[saveServerList] Saved server list JSON: " + json.substring(0, Math.min(200, json.length())) + (json.length() > 200 ? "..." : ""));
    }

    /**
     * Load the full server list (with ping and premium status) from JSON
     */
    public List<Server> loadServerList() {
        String json = mPreference.getString(SERVER_LIST_JSON, null);
        if (json == null) {
            Log.d("SharedPref", "[loadServerList] No cached server list found");
            return null;
        }
        try {
            Type listType = new TypeToken<List<Server>>(){}.getType();
            List<Server> serverList = gson.fromJson(json, listType);
            Log.d("SharedPref", "[loadServerList] Loaded server list, count: " + (serverList != null ? serverList.size() : 0));
            return serverList;
        } catch (Exception e) {
            Log.e("SharedPref", "[loadServerList] Error parsing server list JSON", e);
            return null;
        }
    }

    public Boolean isPrefsHasServer() {
        return mPreference.contains(SERVER_IP_ADDRESS);
    }

    /**
     * Check if user is signed in
     *
     * @return true if user is signed in, false otherwise
     */
    public Boolean isUserSignedIn() {
        return mPreference.getBoolean(USER_SIGNED_IN, false);
    }

    /**
     * Set user signed in status
     *
     * @param signedIn true if user is signed in, false otherwise
     */
    public void setUserSignedIn(Boolean signedIn) {
        mPrefEditor.putBoolean(USER_SIGNED_IN, signedIn);
        mPrefEditor.commit();
    }

    /**
     * Save user name
     *
     * @param name the user's name
     */
    public void saveUserName(String name) {
        mPrefEditor.putString(USER_NAME, name);
        mPrefEditor.commit();
        Log.d("SharedPref", "User name saved: " + name);
    }

    /**
     * Get user name
     *
     * @return the user's name, or null if not set
     */
    public String getUserName() {
        String name = mPreference.getString(USER_NAME, null);
        Log.d("SharedPref", "User name retrieved: " + (name != null ? name : "Not set"));
        return name;
    }

    /**
     * Clear user-specific data when signing out
     * This clears user-related preferences while keeping server and app settings
     */
    public void clearUserData() {
        // Clear user sign-in status
        mPrefEditor.remove(USER_SIGNED_IN);
        // Clear user name
        mPrefEditor.remove(USER_NAME);
        
        // You can add more user-specific data clearing here
        // For example: user profile, preferences, etc.
        // mPrefEditor.remove("USER_NAME");
        // mPrefEditor.remove("USER_EMAIL");
        // mPrefEditor.remove("USER_PREFERENCES");
        
        mPrefEditor.commit();
        Log.d("SharedPref", "User data cleared");
    }

    /**
     * Save protocol filter preference for server list ("ALL", "TCP", "UDP")
     */
    public void setProtocolFilter(String filter) {
        if (filter == null || filter.isEmpty()) filter = "ALL";
        mPrefEditor.putString(PROTOCOL_FILTER, filter.toUpperCase());
        mPrefEditor.commit();
        Log.d("SharedPref", "Protocol filter saved: " + filter.toUpperCase());
    }

    /**
     * Get protocol filter preference for server list
     * @return One of: "ALL" (default), "TCP", "UDP"
     */
    public String getProtocolFilter() {
        String value = mPreference.getString(PROTOCOL_FILTER, "ALL");
        if (value == null || value.isEmpty()) {
            Log.d("SharedPref", "Protocol filter read: default=ALL");
            return "ALL";
        }
        String normalized = value.toUpperCase();
        Log.d("SharedPref", "Protocol filter read: " + normalized);
        return normalized;
    }



    /**
     * Save dark mode preference
     * @param enabled true to enable dark mode, false to disable
     */
    public void setDarkModeEnabled(boolean enabled) {
        mPrefEditor.putBoolean(DARK_MODE_ENABLED, enabled);
        mPrefEditor.commit();
        Log.d("SharedPref", "Dark mode preference saved: " + enabled);
    }

    /**
     * Get dark mode preference
     * @return true if dark mode is enabled, false otherwise
     */
    public boolean isDarkModeEnabled() {
        boolean enabled = mPreference.getBoolean(DARK_MODE_ENABLED, false);
        Log.d("SharedPref", "Dark mode preference read: " + enabled);
        return enabled;
    }



}
