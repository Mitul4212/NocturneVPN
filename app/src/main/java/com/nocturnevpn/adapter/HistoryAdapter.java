package com.nocturnevpn.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nocturnevpn.R;
import com.nocturnevpn.model.History;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private List<History> historyList;
    private OnHistoryItemClickListener listener;

    public interface OnHistoryItemClickListener {
        void onHistoryItemClick(History history);
    }

    public HistoryAdapter() {
        this.historyList = new ArrayList<>();
    }

    public void setOnHistoryItemClickListener(OnHistoryItemClickListener listener) {
        this.listener = listener;
    }

    public void setHistoryList(List<History> historyList) {
        this.historyList = historyList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        History history = historyList.get(position);
        holder.bind(history);
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    class HistoryViewHolder extends RecyclerView.ViewHolder {
        private TextView countryText;
        private TextView ipText;
        private TextView statusText;
        private TextView durationText;
        private TextView dataUsedText;
        private TextView dateText;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            countryText = itemView.findViewById(R.id.serverNameText);
            ipText = itemView.findViewById(R.id.serverCountryText);
            statusText = itemView.findViewById(R.id.statusText);
            durationText = itemView.findViewById(R.id.durationText);
            dataUsedText = itemView.findViewById(R.id.dataUsedText);
            dateText = itemView.findViewById(R.id.dateText);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onHistoryItemClick(historyList.get(position));
                }
            });
        }

        public void bind(History history) {
            countryText.setText(history.getServerCountry());
            ipText.setText(history.getServerIp());
            statusText.setText(history.getStatus());
            durationText.setText(history.getFormattedDuration());
            dataUsedText.setText(history.getFormattedDataUsed());
            dateText.setText(formatDate(history.getConnectionDate()));

            // Set status background based on status
            if ("failed".equalsIgnoreCase(history.getStatus())) {
                statusText.setBackgroundResource(R.drawable.status_failed_background);
            } else {
                statusText.setBackgroundResource(R.drawable.status_background);
            }
        }

        private String formatDate(Date date) {
            Calendar calendar = Calendar.getInstance();
            Calendar today = Calendar.getInstance();
            Calendar yesterday = Calendar.getInstance();
            yesterday.add(Calendar.DAY_OF_YEAR, -1);

            calendar.setTime(date);

            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM", Locale.getDefault());

            if (calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
                return "Today " + timeFormat.format(date);
            } else if (calendar.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
                       calendar.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR)) {
                return "Yesterday " + timeFormat.format(date);
            } else {
                return dateFormat.format(date) + " " + timeFormat.format(date);
            }
        }
    }
} 