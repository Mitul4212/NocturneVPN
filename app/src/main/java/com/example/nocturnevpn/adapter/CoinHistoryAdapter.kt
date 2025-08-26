package com.example.nocturnevpn.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.nocturnevpn.R
import com.example.nocturnevpn.model.CoinHistory
import com.example.nocturnevpn.model.HistoryType

class CoinHistoryAdapter(private var items: MutableList<CoinHistory>) : RecyclerView.Adapter<CoinHistoryAdapter.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val amount: TextView = view.findViewById(R.id.historyAmount)
        val type: TextView = view.findViewById(R.id.historyType)
        val date: TextView = view.findViewById(R.id.historyDate)
        val description: TextView = view.findViewById(R.id.historyDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_coin_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.amount.text = (if (item.type == HistoryType.EARN) "+" else "-") + item.amount.toString()
        holder.type.text = if (item.type == HistoryType.EARN) "Earned" else "Spent"
        holder.date.text = item.date
        holder.description.text = item.description
        holder.amount.setTextColor(
            holder.itemView.context.getColor(
                if (item.type == HistoryType.EARN) R.color.green else R.color.red
            )
        )
    }

    override fun getItemCount() = items.size

    fun updateList(newList: List<CoinHistory>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }
} 