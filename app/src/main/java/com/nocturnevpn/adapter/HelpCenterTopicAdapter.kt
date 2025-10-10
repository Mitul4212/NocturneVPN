package com.nocturnevpn.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.nocturnevpn.R

class HelpCenterTopicAdapter(
    private val topics: List<String>,
    private val onTopicSelected: (Int) -> Unit
) : RecyclerView.Adapter<HelpCenterTopicAdapter.TopicViewHolder>() {

    private var selectedPosition = RecyclerView.NO_POSITION

    inner class TopicViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.topicTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopicViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_help_center_topic, parent, false)
        return TopicViewHolder(view)
    }

    override fun getItemCount(): Int = topics.size

    override fun onBindViewHolder(holder: TopicViewHolder, position: Int) {
        holder.textView.text = topics[position]
        val context = holder.itemView.context
        val adapterPosition = holder.adapterPosition
        holder.textView.setBackgroundResource(
            if (adapterPosition == selectedPosition) R.drawable.topic_selected_bg else R.drawable.topic_unselected_bg
        )
        holder.textView.setTextColor(
            ContextCompat.getColor(context, if (adapterPosition == selectedPosition) R.color.white else R.color.strong_violet)
        )
        holder.itemView.setOnClickListener {
            val previousPosition = selectedPosition
            val currentPosition = holder.adapterPosition
            if (currentPosition == RecyclerView.NO_POSITION) return@setOnClickListener
            selectedPosition = currentPosition
            notifyItemChanged(previousPosition)
            notifyItemChanged(selectedPosition)
            onTopicSelected(currentPosition)
        }
    }

    fun clearSelection() {
        val previousPosition = selectedPosition
        selectedPosition = RecyclerView.NO_POSITION
        notifyItemChanged(previousPosition)
    }
} 