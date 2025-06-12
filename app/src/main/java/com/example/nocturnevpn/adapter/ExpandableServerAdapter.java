package com.example.nocturnevpn.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nocturnevpn.R;
import com.example.nocturnevpn.model.CountryServerGroup;

import java.util.List;
import com.murgupluoglu.flagkit.FlagKit;


public class ExpandableServerAdapter extends RecyclerView.Adapter<ExpandableServerAdapter.GroupViewHolder> {

    private final List<CountryServerGroup> groupList;
    private final ServerAdapter.ServerClickCallback clickCallback;
    private int expandedPosition = -1;
    private String selectedIp = "";

    public ExpandableServerAdapter(List<CountryServerGroup> groupList, ServerAdapter.ServerClickCallback callback) {
        this.groupList = groupList;
        this.clickCallback = callback;
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.country_header_item, parent, false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        CountryServerGroup group = groupList.get(position);
        holder.tvCountry.setText(group.getCountryName());

        int flagResId = FlagKit.INSTANCE.getResId(holder.flagImage.getContext(), group.getCountryCode().toLowerCase());
        holder.flagImage.setImageResource(flagResId != 0 ? flagResId : R.drawable.ic_server_flage_icon);

        boolean isExpanded = expandedPosition == position;
//        holder.childRecyclerView.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

        // Animate layout changes
        android.transition.TransitionManager.beginDelayedTransition(holder.itemView.findViewById(R.id.server_group), new android.transition.AutoTransition());

        holder.childRecyclerView.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.arrowIcon.animate().rotation(isExpanded ? 90f : 0f).setDuration(300).start();


        if (isExpanded) {

            // Setup ServerAdapter and shimmer loading as before
            ServerAdapter serverAdapter = new ServerAdapter(group.getServers(), selectedIp, clickCallback);
            serverAdapter.setLoading(true);

            holder.childRecyclerView.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext()));
            holder.childRecyclerView.setAdapter(serverAdapter);
            holder.childRecyclerView.setItemAnimator(new DefaultItemAnimator());

            holder.childRecyclerView.postDelayed(() -> {
                serverAdapter.setLoading(false);
            }, 600);

        }



        holder.serverGroup.setOnClickListener(v -> {
            int previousExpanded = expandedPosition;
            expandedPosition = (expandedPosition == position) ? -1 : position;
            notifyItemChanged(previousExpanded);
            notifyItemChanged(expandedPosition);
        });
    }

    @Override
    public int getItemCount() {
        return groupList.size();
    }

    public void setSelectedServer(String ip) {
        this.selectedIp = ip;
        notifyDataSetChanged();
    }


    public void updateList(List<CountryServerGroup> newList) {
        groupList.clear();
        groupList.addAll(newList);
        notifyDataSetChanged();
    }

    static class GroupViewHolder extends RecyclerView.ViewHolder {
        TextView tvCountry;
        ImageView arrowIcon, flagImage;
        RecyclerView childRecyclerView;
        LinearLayout serverGroup;

        public GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCountry = itemView.findViewById(R.id.tvCountry);
            arrowIcon = itemView.findViewById(R.id.arrowIcon);
            flagImage = itemView.findViewById(R.id.flagImageView);
            childRecyclerView = itemView.findViewById(R.id.childContainer);
            serverGroup = itemView.findViewById(R.id.server_group);
        }
    }
}

