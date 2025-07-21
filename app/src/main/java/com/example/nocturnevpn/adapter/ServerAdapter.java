package com.example.nocturnevpn.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nocturnevpn.R;
import com.example.nocturnevpn.model.Server;
import com.example.nocturnevpn.utils.OvpnUtils;
import com.facebook.shimmer.ShimmerFrameLayout;

import java.util.List;
import java.util.Locale;

public class ServerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SHIMMER = 0;
    private static final int VIEW_TYPE_SERVER = 1;

    private final List<Server> servers;
    private final ServerClickCallback clickCallback;
    private final String selectedIp;

    private boolean isLoading = false;

    public interface ServerClickCallback {
        void onServerClick(Server server);
    }

    public ServerAdapter(List<Server> servers, String selectedIp, ServerClickCallback callback) {
        this.servers = servers;
        this.selectedIp = selectedIp;
        this.clickCallback = callback;
    }

    public void setLoading(boolean loading) {
        this.isLoading = loading;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (isLoading) {
            return VIEW_TYPE_SHIMMER;
        } else {
            return VIEW_TYPE_SERVER;
        }
    }

    @Override
    public int getItemCount() {
        if (isLoading) {
            // Show up to 3 shimmer placeholders during loading
            return Math.min(servers.size(), 3);
        } else {
            return servers.size();
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SHIMMER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_server_skeleton, parent, false);
            return new ShimmerViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_server_child, parent, false);
            return new ServerViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == VIEW_TYPE_SHIMMER) {
            ShimmerViewHolder shimmerHolder = (ShimmerViewHolder) holder;
            shimmerHolder.shimmerLayout.startShimmer();
        } else {
            Server server = servers.get(position);
            // Debug: Log all server pings
            Log.d("ServerPingDebug", "Server: " + server.getHostName() + " | IP: " + server.getIpAddress() + " | Ping: " + server.getPing());
            ServerViewHolder serverHolder = (ServerViewHolder) holder;
            serverHolder.tvIp.setText(server.getIpAddress());
            serverHolder.tvSpeed.setText(OvpnUtils.humanReadableCount(server.getSpeed(), true));
            serverHolder.tvProtocol.setText(server.getProtocol().toUpperCase(Locale.ROOT));

            // Set ping signal image (wider ranges)
            ImageView pingImage = serverHolder.itemView.findViewById(R.id.serverPingImage);
            int pingValue = parsePing(server.getPing());
            int signalRes;
            if (pingValue <= 0) {
                signalRes = R.drawable.ic_signal_no_signal;
            } else if (pingValue <= 100) {
                signalRes = R.drawable.ic_signal_four; // green
            } else if (pingValue <= 250) {
                signalRes = R.drawable.ic_signal_three; // yellow
            } else if (pingValue <= 500) {
                signalRes = R.drawable.ic_signal_two; // orange
            } else {
                signalRes = R.drawable.ic_signal_one; // red
            }
            pingImage.setImageResource(signalRes);

            if (server.getIpAddress().equals(selectedIp)) {
                serverHolder.itemView.setBackgroundColor(Color.parseColor("#33FFFFFF"));
            } else {
                serverHolder.itemView.setBackgroundColor(Color.TRANSPARENT);
            }

            ImageView preImage = serverHolder.itemView.findViewById(R.id.pre_imaage);
            View serverLayout = serverHolder.itemView.findViewById(R.id.serverLayout);

            // --- PREMIUM TIMER CHECK ---
            SharedPreferences prefs = serverHolder.itemView.getContext().getSharedPreferences("reward_prefs", Context.MODE_PRIVATE);
            long proTimerEnd = prefs.getLong("pro_timer_end", 0L);
            boolean isUserPremium = proTimerEnd > System.currentTimeMillis();
            // --- END PREMIUM TIMER CHECK ---

            if (server.isPremium() && !isUserPremium) {
                preImage.setVisibility(View.VISIBLE);
                preImage.setAlpha(1.0f); // Premium icon always fully visible
                serverLayout.setAlpha(0.5f); // Fade only the serverLayout
                serverHolder.itemView.setClickable(false);
                serverHolder.itemView.setOnClickListener(null);
            } else {
                preImage.setVisibility(server.isPremium() ? View.VISIBLE : View.GONE);
                serverLayout.setAlpha(1.0f);
                serverHolder.itemView.setClickable(true);
                serverHolder.itemView.setOnClickListener(v -> clickCallback.onServerClick(server));
            }
        }
    }

    private int parsePing(String ping) {
        if (ping == null) return -1;
        try {
            String digits = ping.replaceAll("[^0-9]", "");
            return Integer.parseInt(digits);
        } catch (Exception e) {
            return -1;
        }
    }

    private int getSignalResId(int ping) {
        if (ping == 0) return R.drawable.ic_signal_no_signal;
        if (ping > 0 && ping <= 20) return R.drawable.ic_signal_four;
        if (ping > 20 && ping <= 50) return R.drawable.ic_signal_three;
        if (ping > 50 && ping <= 100) return R.drawable.ic_signal_two;
        if (ping > 100 && ping <= 150) return R.drawable.ic_signal_one;
        if (ping > 150) return R.drawable.ic_signal_no_signal;
        return R.drawable.ic_signal_no_signal;
    }

    static class ServerViewHolder extends RecyclerView.ViewHolder {
        TextView tvIp, tvSpeed, tvProtocol;

        public ServerViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIp = itemView.findViewById(R.id.tv_ip_address);
            tvSpeed = itemView.findViewById(R.id.speedText);
            tvProtocol = itemView.findViewById(R.id.tv_protocol);
        }
    }

    static class ShimmerViewHolder extends RecyclerView.ViewHolder {
        ShimmerFrameLayout shimmerLayout;

        public ShimmerViewHolder(@NonNull View itemView) {
            super(itemView);
            shimmerLayout = itemView.findViewById(R.id.shimmer_container);
        }
    }
}





//package com.example.nocturnevpn.adapter;
//
//import android.content.Context;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.TextView;
//
//import androidx.annotation.NonNull;
//import androidx.constraintlayout.widget.ConstraintLayout;
//import androidx.recyclerview.widget.DiffUtil;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.example.nocturnevpn.R;
//import com.example.nocturnevpn.model.Server;
//import com.example.nocturnevpn.utils.OvpnUtils;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class ServerAdapter extends RecyclerView.Adapter<ServerAdapter.ViewHolder> {
//
//    /*
//     * Set the server the data
//     * */
//    private List<Server> servers = new ArrayList<>();
//
//    private String selectedIp = null;
//
//    private ServerClickCallback callback;
//
//    public ServerAdapter(List<Server> servers, @NonNull ServerClickCallback callback) {
//        this.servers.clear();
//        this.servers.addAll(servers);
//        this.callback = callback;
//    }
//
//    public void setServerList(@NonNull final List<Server> serverList) {
//        if (servers.isEmpty()) {
//            servers.clear();
//            servers.addAll(serverList);
//            notifyItemRangeInserted(0, serverList.size());
//        } else {
//            DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
//                @Override
//                public int getOldListSize() {
//                    return servers.size();
//                }
//
//                @Override
//                public int getNewListSize() {
//                    return serverList.size();
//                }
//
//                @Override
//                public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
//                    Server old = servers.get(oldItemPosition);
//                    Server server = serverList.get(newItemPosition);
//                    return old.hostName.equals(server.hostName);
//                }
//
//                @Override
//                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
//                    Server old = servers.get(oldItemPosition);
//                    Server server = serverList.get(newItemPosition);
//                    return old.hostName.equals(server.hostName)
//                            && old.ipAddress.equals(server.ipAddress)
//                            && old.countryLong.equals(server.countryLong);
//                }
//            });
//            servers.clear();
//            servers.addAll(serverList);
//            result.dispatchUpdatesTo(this);
//        }
//    }
//
//    @Override
//    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
//        View view = LayoutInflater.from(parent.getContext())
//                .inflate(R.layout.server_list_item, parent, false);
//        return new ViewHolder(view, callback);
//    }
//
//    public void setSelectedServer(String ip) {
//        this.selectedIp = ip;
//        notifyDataSetChanged(); // Refresh the UI
//    }
//
//    @Override
//    public void onBindViewHolder(ViewHolder holder, int position) {
////        holder.bind(servers.get(position));
//        Server server = servers.get(position);
//        holder.bind(server);
//
//        if (server.getIpAddress().equals(selectedIp)) {
//            holder.cardInnerLayout.setSelected(true);  // light gray
//        } else {
//            holder.cardInnerLayout.setSelected(false); // default
//        }
//
//        holder.itemView.setOnClickListener(v -> {
//            if (callback != null) {
//                callback.onItemClick(server);
//            }
//        });
//    }
//
//    @Override
//    public long getItemId(int position) {
//        return position;
//    }
//
//    @Override
//    public int getItemCount() {
//        return servers == null ? 0 : servers.size();
//    }
//
//    static class ViewHolder extends RecyclerView.ViewHolder {
//        final View rootView;
//        final TextView countryView;
//        final TextView protocolView;
//        final TextView ipAddressView;
//        final TextView speedView;
//        final TextView pingView;
//
//        final ConstraintLayout cardInnerLayout;
//
//        final ServerClickCallback callback;
//
//        public ViewHolder(View view, ServerClickCallback callback) {
//            super(view);
//            rootView = view;
//            countryView = view.findViewById(R.id.tv_country_name);
//            protocolView = view.findViewById(R.id.tv_protocol);
//            ipAddressView = view.findViewById(R.id.tv_ip_address);
//            speedView = view.findViewById(R.id.tv_speed);
//            pingView = view.findViewById(R.id.tv_ping);
//            cardInnerLayout = view.findViewById(R.id.cardInnerLayout);
//
//            this.callback = callback;
//        }
//
//        public void bind(@NonNull final Server server) {
//            final Context context = rootView.getContext();
//
//            countryView.setText(server.countryLong);
//            protocolView.setText(server.protocol.toUpperCase());
//            ipAddressView.setText(context.getString(R.string.format_ip_address,
//                    server.ipAddress, server.port));
//            speedView.setText(context.getString(R.string.format_speed,
//                    OvpnUtils.humanReadableCount(server.speed, true)));
//            pingView.setText(context.getString(R.string.format_ping, server.ping));
//            rootView.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    callback.onItemClick(server);
//                }
//            });
//        }
//    }
//
//
//
//    public interface ServerClickCallback {
//        void onItemClick(@NonNull Server server);
//    }
//}
