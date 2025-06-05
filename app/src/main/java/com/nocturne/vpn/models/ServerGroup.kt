package com.nocturne.vpn.models

data class ServerGroup(
    val countryName: String,
    val countryCode: String,
    val servers: List<Server>,
    var isExpanded: Boolean = false
)

data class Server(
    private val id: String,
    private val name: String,
    private val ip: String,
    private val port: Int,
    private val protocol: String,
    private val ping: Int = 0,
    private val isSelected: Boolean = false
) {
    fun getId() = id
    fun getName() = name
    fun getIp() = ip
    fun getPort() = port
    fun getProtocol() = protocol
    fun getPing() = ping
    fun isSelected() = isSelected
} 