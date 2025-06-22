package com.example.nocturnevpn.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.nocturnevpn.R
import com.example.nocturnevpn.databinding.HelpcenterExpansionPanelRecyclerCellBinding

class HelpCenterAdapter(private val questions: Array<String>, private val answers: Array<String>) :
    RecyclerView.Adapter<HelpCenterAdapter.FaqViewHolder>() {

    private val expandedState = BooleanArray(questions.size) { false }

    inner class FaqViewHolder(val binding: HelpcenterExpansionPanelRecyclerCellBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FaqViewHolder {
        val binding = HelpcenterExpansionPanelRecyclerCellBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FaqViewHolder(binding)
    }

    override fun getItemCount(): Int = questions.size

    override fun onBindViewHolder(holder: FaqViewHolder, position: Int) {
        with(holder.binding) {
            tvQuestion.text = questions[position]
            tvAnswer.text = answers[position]

            // Animate expand/collapse
            val isExpanded = expandedState[position]
            tvAnswer.visibility = if (isExpanded) View.VISIBLE else View.GONE
            tvQuestion.setTextColor(
                ContextCompat.getColor(
                    holder.itemView.context,
                    if (isExpanded) R.color.strong_violet else R.color.black
                )
            )
            cardView.setCardBackgroundColor(
                ContextCompat.getColor(
                    holder.itemView.context,
                    if (isExpanded) R.color.btn_background else R.color.light_strong_violet
                )
            )
            layoutFaqItem.setBackgroundColor(
                ContextCompat.getColor(
                    holder.itemView.context,
                    if (isExpanded) android.R.color.white else R.color.light_strong_violet
                )
            )

            layoutFaqItem.setOnClickListener {
                val wasExpanded = expandedState[position]
                expandedState[position] = !wasExpanded
                notifyItemChanged(position)
            }
        }
    }
}
