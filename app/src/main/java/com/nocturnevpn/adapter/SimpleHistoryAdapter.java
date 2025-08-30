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

public class SimpleHistoryAdapter extends RecyclerView.Adapter<SimpleHistoryAdapter.SimpleHistoryViewHolder> {

    private List<History> historyList;

    public SimpleHistoryAdapter() {
        this.historyList = new ArrayList<>();
    }

    public void setHistoryList(List<History> historyList) {
        this.historyList = historyList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SimpleHistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history_simple, parent, false);
        return new SimpleHistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SimpleHistoryViewHolder holder, int position) {
        History history = historyList.get(position);
        holder.bind(history);
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    static class SimpleHistoryViewHolder extends RecyclerView.ViewHolder {
        private TextView countryText;
        private TextView ipText;
        private TextView timeText;

        public SimpleHistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            countryText = itemView.findViewById(R.id.countryText);
            ipText = itemView.findViewById(R.id.ipText);
            timeText = itemView.findViewById(R.id.timeText);
        }

        public void bind(History history) {
            countryText.setText(history.getServerCountry());
            ipText.setText(history.getServerIp());
            timeText.setText(formatTime(history.getConnectionDate()));
        }

        private String formatTime(Date date) {
            Calendar calendar = Calendar.getInstance();
            Calendar today = Calendar.getInstance();
            Calendar yesterday = Calendar.getInstance();
            yesterday.add(Calendar.DAY_OF_YEAR, -1);

            calendar.setTime(date);

            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

            if (calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
                return "Today " + timeFormat.format(date);
            } else if (calendar.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
                       calendar.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR)) {
                return "Yesterday " + timeFormat.format(date);
            } else {
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM", Locale.getDefault());
                return dateFormat.format(date) + " " + timeFormat.format(date);
            }
        }
    }
} 