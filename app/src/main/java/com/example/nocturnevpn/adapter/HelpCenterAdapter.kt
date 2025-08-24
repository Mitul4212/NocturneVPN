package com.example.nocturnevpn.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.nocturnevpn.R
import com.example.nocturnevpn.databinding.HelpcenterExpansionPanelRecyclerCellBinding

class HelpCenterAdapter(private val originalQuestions: Array<String>, private val originalAnswers: Array<String>) :
    RecyclerView.Adapter<HelpCenterAdapter.FaqViewHolder>() {

    private var questions: List<String> = originalQuestions.toList()
    private var answers: List<String> = originalAnswers.toList()
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
            val isExpanded = expandedState.getOrNull(position) ?: false
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
                    if (isExpanded) R.color.Quation_btn_background else R.color.light_strong_violet
                )
            )
            layoutFaqItem.setBackgroundColor(
                ContextCompat.getColor(
                    holder.itemView.context,
                    if (isExpanded) android.R.color.white else R.color.light_strong_violet
                )
            )
            layoutFaqItem.setOnClickListener {
                val wasExpanded = expandedState.getOrNull(position) ?: false
                if (position < expandedState.size) {
                    expandedState[position] = !wasExpanded
                    notifyItemChanged(position)
                }
            }
        }
    }

    fun filterByTopicAndQuery(topicIndexes: List<Int>, query: String?) {
        val filtered = topicIndexes.map { it to originalQuestions[it] }.filter { (_, q) ->
            query.isNullOrBlank() || q.contains(query, ignoreCase = true)
        }
        questions = filtered.map { (_, q) -> q }
        answers = filtered.map { (i, _) -> originalAnswers[i] }
        // Reset expanded state
        expandedState.fill(false, 0, questions.size.coerceAtMost(expandedState.size))
        notifyDataSetChanged()
    }

    fun reset() {
        questions = originalQuestions.toList()
        answers = originalAnswers.toList()
        expandedState.fill(false)
        notifyDataSetChanged()
    }
}
