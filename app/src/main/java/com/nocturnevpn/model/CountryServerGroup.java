package com.nocturnevpn.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CountryServerGroup {
    private String countryName;
    private String flagUrl;
    private String countryCode;
    private List<Server> servers;
    private boolean expanded;

    public CountryServerGroup(String countryName, String countryCode, List<Server> servers) {
        this.countryName = countryName;
        this.countryCode = countryCode;
        this.servers = servers;
        this.expanded = false;
    }

    public String getCountryName() {
        return countryName;
    }

    public String getFlagUrl() {
        return flagUrl;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public List<Server> getServers() {
        return servers;
    }

    public void setFlagUrl(String flagUrl) {
        this.flagUrl = flagUrl;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }



}
