package com.example.nocturnevpn.model;

import java.util.Date;

public class History {
    private int id;
    private String serverName;
    private String serverCountry;
    private String serverIp;
    private Date connectionDate;
    private long duration; // in milliseconds
    private String status; // "Connected", "Disconnected", "Failed"
    private long dataUsed; // in bytes

    public History() {}

    public History(String serverName, String serverCountry, String serverIp, Date connectionDate, long duration, String status, long dataUsed) {
        this.serverName = serverName;
        this.serverCountry = serverCountry;
        this.serverIp = serverIp;
        this.connectionDate = connectionDate;
        this.duration = duration;
        this.status = status;
        this.dataUsed = dataUsed;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getServerCountry() {
        return serverCountry;
    }

    public void setServerCountry(String serverCountry) {
        this.serverCountry = serverCountry;
    }

    public String getServerIp() {
        return serverIp;
    }

    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
    }

    public Date getConnectionDate() {
        return connectionDate;
    }

    public void setConnectionDate(Date connectionDate) {
        this.connectionDate = connectionDate;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getDataUsed() {
        return dataUsed;
    }

    public void setDataUsed(long dataUsed) {
        this.dataUsed = dataUsed;
    }

    // Helper methods
    public String getFormattedDuration() {
        long seconds = duration / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }

    public String getFormattedDataUsed() {
        if (dataUsed < 1024) {
            return dataUsed + " B";
        } else if (dataUsed < 1024 * 1024) {
            return String.format("%.1f KB", dataUsed / 1024.0);
        } else if (dataUsed < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", dataUsed / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", dataUsed / (1024.0 * 1024.0 * 1024.0));
        }
    }
} 