package com.nocturnevpn.model

data class CoinHistory(
    val type: HistoryType, // EARN or SPENT
    val amount: Int,
    val date: String,
    val description: String
)

enum class HistoryType {
    EARN, SPENT
} 