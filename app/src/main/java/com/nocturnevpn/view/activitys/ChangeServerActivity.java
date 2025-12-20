package com.nocturnevpn.view.activitys;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.core.view.WindowCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

import com.badoo.mobile.util.WeakHandler;
import com.nocturnevpn.BuildConfig;
import com.nocturnevpn.R;
import com.nocturnevpn.SharedPreference;
import com.nocturnevpn.adapter.ExpandableServerAdapter;
import com.nocturnevpn.adapter.ServerAdapter;
import com.nocturnevpn.databinding.ActivityChangeServerBinding;
import com.nocturnevpn.db.DbHelper;
import com.nocturnevpn.model.CountryServerGroup;
import com.nocturnevpn.model.Server;
import com.nocturnevpn.utils.CsvParser;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import androidx.lifecycle.Observer;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ChangeServerActivity extends AppCompatActivity {

    private ActivityChangeServerBinding binding;

    private WeakHandler handler;
    private OkHttpClient okHttpClient = new OkHttpClient();
    private List<Server> servers = new ArrayList<>();
    private List<CountryServerGroup> groupedServers = new ArrayList<>();

    private final List<CountryServerGroup> fullGroupList = new ArrayList<>();


    private Request request;
    private SearchView searchView;
    private Call mCall;
//    private ServerAdapter adapter;

    private ExpandableServerAdapter adapter;

    private DbHelper dbHelper;

    private SharedPreference sharedPreference;


    private Server globalServer;

    private Dialog infoAlertDialog;

    private TextInputEditText serverSearchEditText;
    private TextInputLayout serverSearchInputLayout;

    // BroadcastReceiver for server list fetched notification
    private final BroadcastReceiver serverListFetchedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("ServerPageDebug", "[Broadcast] SERVER_LIST_FETCHED received");
            if (binding.swipeRefresh.isRefreshing()) {
                binding.swipeRefresh.setRefreshing(false);
                Log.d("ServerPageDebug", "[Broadcast] Stopped swipeRefresh spinner after server list fetched");
            }
            List<Server> cachedServerList = sharedPreference.loadServerList();
            if (cachedServerList != null && !cachedServerList.isEmpty()) {
                loadServerList(cachedServerList);
                Toast.makeText(ChangeServerActivity.this, "Server list updated! (Premium logic running in background)", Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("ServerPageDebug", "onCreate called! (Activity is starting)");
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_change_server);
        handler = new WeakHandler();
        dbHelper = DbHelper.getInstance(getApplicationContext());
        sharedPreference = new SharedPreference(ChangeServerActivity.this);
        binding = ActivityChangeServerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Edge-to-edge with transparent bars; handle insets to avoid overlap
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Apply only top inset on root to avoid double bottom padding
            v.setPadding(v.getPaddingLeft(), bars.top, v.getPaddingRight(), v.getPaddingBottom());
            // Ensure list content is above nav bar
            binding.swipeRefresh.setPadding(
                    binding.swipeRefresh.getPaddingLeft(),
                    binding.swipeRefresh.getPaddingTop(),
                    binding.swipeRefresh.getPaddingRight(),
                    bars.bottom
            );
            return insets;
        });

        // Do NOT trigger Worker or server fetch on page open for best UX
        // Only periodic Worker and manual refresh will update data

        setupSwipeRefreshLayout();
        setupRecyclerView();

        // Always initialize request before any possible use
        if (request == null) {
            request = new okhttp3.Request.Builder()
                    .url(BuildConfig.VPN_GATE_API)
                    .build();
        }

        // 1. Try to load cached server list (with ping and premium status)
        List<Server> cachedServerList = sharedPreference.loadServerList();
        if (cachedServerList != null && !cachedServerList.isEmpty()) {
            Log.d("ServerPageDebug", "[onCreate] Loaded cached server list, count: " + cachedServerList.size());
            loadServerList(cachedServerList);
        } else {
            Log.d("ServerPageDebug", "[onCreate] No cached server list found, loading from DB/API");
            // Load cached servers from database (original API data)
            servers.addAll(dbHelper.getAll());
            if (servers.isEmpty()) {
                Log.d("ServerPageDebug", "[onCreate] DB empty → fetching from API now");
                showSkeletonPlaceholders();
                populateServerList();
            } else {
                loadServerList(servers);
            }
        }

        binding.backArrow.setOnClickListener(view -> {
            finish();
        });

        binding.infoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInfoDialog();
            }
        });

        serverSearchInputLayout = binding.serverSearchInputLayout;
        serverSearchEditText = binding.serverSearchEditText;

        // Search filter logic
        serverSearchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterServers(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
        serverSearchInputLayout.setEndIconOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                serverSearchEditText.setText("");
                serverSearchEditText.clearFocus();
                filterServers("");
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(serverListFetchedReceiver, new IntentFilter("com.nocturnevpn.SERVER_LIST_FETCHED"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(serverListFetchedReceiver);
    }

    private void filterServers(String query) {
        String lowerQuery = query.toLowerCase().trim();
        // Read protocol filter from preferences (ALL, TCP, UDP)
        String protocolFilter = sharedPreference.getProtocolFilter();
        Log.d("ServerPageDebug", "[Filter] Query='" + lowerQuery + "' ProtocolFilter=" + protocolFilter);
        List<CountryServerGroup> filteredGroups = new ArrayList<>();

        for (CountryServerGroup group : fullGroupList) {
            List<Server> filteredServers = new ArrayList<>();
            for (Server server : group.getServers()) {
                boolean protocolMatches =
                        "ALL".equalsIgnoreCase(protocolFilter) ||
                        ("TCP".equalsIgnoreCase(protocolFilter) && "tcp".equalsIgnoreCase(server.getProtocol())) ||
                        ("UDP".equalsIgnoreCase(protocolFilter) && "udp".equalsIgnoreCase(server.getProtocol()));

                boolean textMatches = server.getCountryLong().toLowerCase().contains(lowerQuery) ||
                        server.getIpAddress().toLowerCase().contains(lowerQuery) ||
                        server.getProtocol().toLowerCase().contains(lowerQuery) ||
                        server.getHostName().toLowerCase().contains(lowerQuery);

                if (protocolMatches && textMatches) {
                    filteredServers.add(server);
                }
            }
            if (!filteredServers.isEmpty()) {
                filteredGroups.add(new CountryServerGroup(group.getCountryName(), group.getCountryCode(), filteredServers));
            }
        }

        groupedServers.clear();
        groupedServers.addAll(filteredGroups);
        adapter.updateList(filteredGroups);  // Adapter should reflect changes here
        Log.d("ServerPageDebug", "[Filter] Result groups=" + filteredGroups.size());
//        adapter.updateList(filteredGroups);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mCall != null) {
            mCall.cancel();
            mCall = null;
        }
        if (infoAlertDialog != null) {
            if (infoAlertDialog.isShowing()) {
                infoAlertDialog.dismiss();
            }
        }
        binding.swipeRefresh.setOnRefreshListener(null);
    }

    private void setupSwipeRefreshLayout() {
        binding.swipeRefresh.setColorSchemeResources(R.color.strong_violet);
        binding.swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.d("ServerPageDebug", "[ManualRefresh] User triggered manual refresh");
                triggerServerFetch();
                observeWorkerCompletion();
            }
        });
    }

    // Listen for ServerFetchWorker completion and update UI
    private void observeWorkerCompletion() {
        WorkManager.getInstance(this).getWorkInfosForUniqueWorkLiveData("vpn_server_fetch")
            .observe(this, new Observer<java.util.List<WorkInfo>>() {
                @Override
                public void onChanged(java.util.List<WorkInfo> workInfos) {
                    if (workInfos != null && !workInfos.isEmpty()) {
                        WorkInfo workInfo = workInfos.get(0);
                        Log.d("ServerPageDebug", "[ManualRefresh] Worker state: " + workInfo.getState());
                        if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                            // Worker finished, reload cached data
                            List<Server> cachedServerList = sharedPreference.loadServerList();
                            if (cachedServerList != null && !cachedServerList.isEmpty()) {
                                Log.d("ServerPageDebug", "[ManualRefresh] Reloaded cached server list after refresh, count: " + cachedServerList.size());
                                loadServerList(cachedServerList);
                                Toast.makeText(ChangeServerActivity.this, "Server list updated! (Premium logic running in background)", Toast.LENGTH_SHORT).show();
                            }
                            // Stop the spinner immediately after API fetch/parse
                            if (binding.swipeRefresh.isRefreshing()) {
                                binding.swipeRefresh.setRefreshing(false);
                                Log.d("ServerPageDebug", "[ManualRefresh] Stopped swipeRefresh spinner after API fetch");
                            }
                        } else if (workInfo.getState() == WorkInfo.State.FAILED || workInfo.getState() == WorkInfo.State.CANCELLED) {
                            if (binding.swipeRefresh.isRefreshing()) {
                                binding.swipeRefresh.setRefreshing(false);
                            }
                            Toast.makeText(ChangeServerActivity.this, "Failed to update server list.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });
    }

    private void triggerServerFetch() {
        androidx.work.Data inputData = new androidx.work.Data.Builder()
                .putBoolean("is_manual_trigger", true)
                .build();

        androidx.work.OneTimeWorkRequest workRequest =
                new androidx.work.OneTimeWorkRequest.Builder(com.nocturnevpn.workers.ServerFetchWorker.class)
                        .setInputData(inputData)
                        .build();
        androidx.work.WorkManager.getInstance(this).enqueue(workRequest);
        Log.d("ChangeServerActivity", "Manually triggered server fetch");
    }


//    private void setupRecyclerView() {
//        adapter = new ServerAdapter(servers, serverClickCallback);
//
//        // ✅ Load saved selected IP
//        String savedIp = getSharedPreferences("vpn_prefs", MODE_PRIVATE)
//                .getString("selected_ip", "");
//        adapter.setSelectedServer(savedIp);
//
//        RecyclerView.ItemDecoration itemDecoration = new
//                DividerItemDecoration(ChangeServerActivity.this, 0);
//        binding.recyclerView.setHasFixedSize(true);
//        binding.recyclerView.addItemDecoration(itemDecoration);
//        binding.recyclerView.setLayoutManager(new LinearLayoutManager(binding.recyclerView.getContext()));
//        binding.recyclerView.setAdapter(adapter);
//    }

    private void setupRecyclerView() {
        adapter = new ExpandableServerAdapter(groupedServers, server -> {
            // Save selected server to shared preferences
            sharedPreference.saveServer(server);

            getSharedPreferences("vpn_prefs", MODE_PRIVATE)
                    .edit()
                    .putString("selected_ip", server.getIpAddress())
                    .apply();

            // Send result back to previous activity
            Intent resultIntent = new Intent();
            resultIntent.putExtra("serverextra", server);
            setResult(RESULT_OK, resultIntent);
            finish();
        });

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setHasFixedSize(true);
        binding.recyclerView.setAdapter(adapter);

        RecyclerView recyclerView = binding.recyclerView;
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null) {
                    int firstVisible = layoutManager.findFirstCompletelyVisibleItemPosition();
                    binding.swipeRefresh.setEnabled(firstVisible == 0);
                }
            }
        });
    }

    private void showSkeletonPlaceholders() {
        binding.recyclerView.setVisibility(View.GONE);
        binding.skeletonContainer.setVisibility(View.VISIBLE);
        binding.skeletonContainer.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < 9; i++) {
            View skeletonView = inflater.inflate(R.layout.server_list_skeleton, binding.skeletonContainer, false);
            binding.skeletonContainer.addView(skeletonView);
        }
    }

    private void hideSkeletonPlaceholders() {
        binding.skeletonContainer.setVisibility(View.GONE);
        binding.recyclerView.setVisibility(View.VISIBLE);
    }


    private void loadServerList(List<Server> serverList) {
        // Apply protocol filter before grouping
        String protocolFilter = sharedPreference.getProtocolFilter();
        Log.d("ServerPageDebug", "[Load] Applying protocol filter: " + protocolFilter + " to " + serverList.size() + " servers");
        List<Server> protocolFiltered = new ArrayList<>();
        for (Server s : serverList) {
            if ("ALL".equalsIgnoreCase(protocolFilter) ||
                ("TCP".equalsIgnoreCase(protocolFilter) && "tcp".equalsIgnoreCase(s.getProtocol())) ||
                ("UDP".equalsIgnoreCase(protocolFilter) && "udp".equalsIgnoreCase(s.getProtocol()))) {
                protocolFiltered.add(s);
            }
        }
        Log.d("ServerPageDebug", "[Load] After protocol filter -> " + protocolFiltered.size() + " servers");

        List<CountryServerGroup> grouped = groupServersByCountry(protocolFiltered);
        Log.d("ServerPageDebug", "[Load] Grouped countries count=" + grouped.size());

        // Always keep a fresh copy of full list
        fullGroupList.clear();
        fullGroupList.addAll(grouped);

        groupedServers.clear();
        groupedServers.addAll(grouped);
        adapter.updateList(grouped);  // Make sure you implement this in ExpandableServerAdapter

        // If after grouping nothing to show, trigger a fresh API fetch
        if (grouped.isEmpty()) {
            Log.d("ServerPageDebug", "[Load] Grouped list empty → fetching from API now");
            showSkeletonPlaceholders();
            populateServerList();
            return;
        }

        Server savedServer = sharedPreference.getServer();
        if (savedServer != null) {
            adapter.setSelectedServer(savedServer.getIpAddress()); // Highlight selected
        }

        dbHelper.save(serverList, this);
        selectBestPremiumServer(serverList);
    }

    /**
     * Displays the updated list of VPN servers
     */
    private void populateServerList() {
        binding.swipeRefresh.setRefreshing(true);
        binding.recyclerView.setVisibility(View.INVISIBLE);
        showSkeletonPlaceholders(); // Show skeletons

        if (request == null) {
            request = new okhttp3.Request.Builder()
                    .url(BuildConfig.VPN_GATE_API)
                    .build();
        }
        mCall = okHttpClient.newCall(request);
        mCall.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        binding.swipeRefresh.setRefreshing(false);
                        binding.recyclerView.setVisibility(View.VISIBLE);
                        hideSkeletonPlaceholders(); // Hide on fail

                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    final List<Server> servers = CsvParser.parse(response);

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            loadServerList(servers);
                            binding.swipeRefresh.setRefreshing(false);
                            binding.recyclerView.setVisibility(View.VISIBLE);
                            hideSkeletonPlaceholders(); // Hide on success

                        }
                    });
                }
            }
        });
    }

    private List<CountryServerGroup> groupServersByCountry(List<Server> servers) {
        Map<String, List<Server>> groupedMap = new LinkedHashMap<>();
        Map<String, String> countryCodeMap = new LinkedHashMap<>();

        for (Server server : servers) {
            groupedMap.computeIfAbsent(server.getCountryLong(), k -> new ArrayList<>()).add(server);
            countryCodeMap.put(server.getCountryLong(), server.getCountryShort());
        }

        List<CountryServerGroup> groupedList = new ArrayList<>();
        for (Map.Entry<String, List<Server>> entry : groupedMap.entrySet()) {
            String countryName = entry.getKey();
            String countryCode = countryCodeMap.get(countryName);
            groupedList.add(new CountryServerGroup(countryName, countryCode, entry.getValue()));
        }

        return groupedList;
    }

    private final ServerAdapter.ServerClickCallback serverClickCallback =
            server -> {
                Server selectedServer = new Server(
                        server.hostName,
                        server.ipAddress,
                        server.ping,
                        server.speed,
                        server.countryLong,
                        server.countryShort,
                        server.ovpnConfigData,
                        server.port,
                        server.protocol
                );

                sharedPreference.saveServer(selectedServer);

                // ✅ Save selected IP in SharedPreferences
                getSharedPreferences("vpn_prefs", MODE_PRIVATE)
                        .edit()
                        .putString("selected_ip", selectedServer.getIpAddress())
                        .apply();

                // ✅ Highlight the selected item in adapter
                adapter.setSelectedServer(selectedServer.getIpAddress());

                Log.d("ServerSelect", "Saved server IP: " + selectedServer.getIpAddress());


                // Create result intent
                Intent resultIntent = new Intent();
                resultIntent.putExtra("serverextra", selectedServer);
//                resultIntent.putParcelableArrayListExtra("serverlist", new ArrayList<>(servers));

                setResult(RESULT_OK, resultIntent);
                finish();

            };

    private void checkScheduledWorkStatus() {
        androidx.work.WorkManager.getInstance(this)
                .getWorkInfosForUniqueWorkLiveData("vpn_server_fetch")
                .observe(this, workInfos -> {
                    if (workInfos != null && !workInfos.isEmpty()) {
                        androidx.work.WorkInfo workInfo = workInfos.get(0);
                        Log.d("ChangeServerActivity", "Scheduled work state: " + workInfo.getState());

                        // If work is not scheduled, schedule it
                        if (workInfo.getState() == androidx.work.WorkInfo.State.CANCELLED) {
                            triggerServerFetch();
                        }
                    } else {
                        Log.d("ChangeServerActivity", "No scheduled work found");
                        triggerServerFetch();
                    }
                });
    }

    private void showInfoDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.info_dialog);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        Button okButton = dialog.findViewById(R.id.info_dialog_btn);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialog.show();
    }


//    private void setIntentResult(Server server) {
//        Intent resultIntent = new Intent();
//        resultIntent.putExtra("serverextra", server);
//        setResult(RESULT_OK, resultIntent);
//        finish();
//    }

    private void selectBestPremiumServer(List<Server> serverList) {
        Log.d("PremiumSelect", "Starting premium server selection, total servers: " + serverList.size());
        new Thread(() -> {
            // Step 1: Deduplicate by ip+port
            Map<String, Server> uniqueServers = new HashMap<>();
            for (Server s : serverList) {
                String key = s.getIpAddress() + ":" + s.port;
                if (!uniqueServers.containsKey(key)) {
                    uniqueServers.put(key, s);
                }
            }
            List<Server> filtered = new ArrayList<>();
            for (Server s : uniqueServers.values()) {
                // Step 2: Filter by protocol (TCP or UDP)
                if (!"tcp".equalsIgnoreCase(s.protocol) && !"udp".equalsIgnoreCase(s.protocol)) continue;
                // Step 3: Filter by load
                if (s.vpnSessions > 500) continue;
                filtered.add(s);
            }
            Log.d("PremiumSelect", "After filtering, servers left: " + filtered.size());
            // Step 4: TCP/UDP ping (use fallback if fails)
            for (Server s : filtered) {
                int livePing = -1;
                String pingType = "";
                try {
                    int port = s.port > 0 ? s.port : ("tcp".equalsIgnoreCase(s.protocol) ? 443 : 1194);
                    if ("tcp".equalsIgnoreCase(s.protocol)) {
                        livePing = com.nocturnevpn.utils.Utils.tcpPing(s.getIpAddress(), port, 2000);
                        pingType = "tcpPing";
                    } else if ("udp".equalsIgnoreCase(s.protocol)) {
                        // For UDP, always use reported/default ping
                        try {
                            livePing = Integer.parseInt(s.ping);
                            pingType = "defaultPing";
                        } catch (Exception e) {
                            livePing = 999;
                            pingType = "defaultPing";
                        }
                    }
                } catch (Exception e) {
                    livePing = -1;
                }
                if (livePing < 0) {
                    // Fallback to reported ping or set high value
                    try {
                        livePing = Integer.parseInt(s.ping);
                        pingType = "defaultPing";
                    } catch (Exception e) {
                        livePing = 999;
                        pingType = "defaultPing";
                    }
                }
                s.ping = String.valueOf(livePing);
                Log.d("PremiumSelect", "Ping result for " + s.getIpAddress() + " (" + s.protocol + ":" + s.port + "): " + livePing + " [" + pingType + "]");
            }
            Log.d("PremiumSelect", "After ping, servers left: " + filtered.size());
            if (filtered.isEmpty()) {
                Log.d("PremiumSelect", "No servers left after ping. Exiting.");
                return;
            }
            // Step 5: Find min/max for normalization
            int minPing = Integer.MAX_VALUE, maxPing = Integer.MIN_VALUE;
            long minSpeed = Long.MAX_VALUE, maxSpeed = Long.MIN_VALUE;
            long minSessions = Long.MAX_VALUE, maxSessions = Long.MIN_VALUE;
            for (Server s : filtered) {
                int p = Integer.parseInt(s.ping);
                minPing = Math.min(minPing, p); maxPing = Math.max(maxPing, p);
                minSpeed = Math.min(minSpeed, s.speed); maxSpeed = Math.max(maxSpeed, s.speed);
                minSessions = Math.min(minSessions, s.vpnSessions); maxSessions = Math.max(maxSessions, s.vpnSessions);
            }
            Log.d("PremiumSelect", "minPing=" + minPing + ", maxPing=" + maxPing + ", minSpeed=" + minSpeed + ", maxSpeed=" + maxSpeed + ", minSessions=" + minSessions + ", maxSessions=" + maxSessions);
            // Step 6: Score
            double w_ping = 0.5, w_speed = 0.3, w_load = 0.2;
            Server best = null;
            double bestScore = -1;
            Map<Server, Double> serverScoreMap = new HashMap<>();
            for (Server s : filtered) {
                int p = Integer.parseInt(s.ping);
                double score_ping = 1.0 - ((double)(p - minPing) / Math.max(1, maxPing - minPing));
                double score_speed = (double)(s.speed - minSpeed) / Math.max(1, maxSpeed - minSpeed);
                double score_load = 1.0 - ((double)(s.vpnSessions - minSessions) / Math.max(1, maxSessions - minSessions));
                double finalScore;
                if ("udp".equalsIgnoreCase(s.protocol)) {
                    // For UDP, ignore ping: only use speed and load
                    finalScore = (w_speed / (w_speed + w_load)) * score_speed + (w_load / (w_speed + w_load)) * score_load;
                } else {
                    // For TCP, use all three
                    finalScore = w_ping * score_ping + w_speed * score_speed + w_load * score_load;
                }
                Log.d("PremiumSelect", "Server " + s.getIpAddress() + " score: " + finalScore + " (ping=" + s.ping + ", speed=" + s.speed + ", sessions=" + s.vpnSessions + ", protocol=" + s.protocol + ")");
                serverScoreMap.put(s, finalScore);
                if (finalScore > bestScore) {
                    bestScore = finalScore;
                    best = s;
                }
            }
            if (best != null) {
                sharedPreference.saveServer(best);
                Log.d("PremiumSelect", "Best premium server: " + best.getIpAddress() + " (score=" + bestScore + ")");
                final String bestIp = best.getIpAddress();
                runOnUiThread(() -> adapter.setSelectedServer(bestIp));
            } else {
                Log.d("PremiumSelect", "No best server found after scoring.");
            }

            // After scoring all servers, select the greater of 20 or 20% as premium
            List<Server> sortedByScore = new ArrayList<>(filtered);
            sortedByScore.sort((a, b) -> Double.compare(serverScoreMap.get(b), serverScoreMap.get(a)));
            int percentCount = (int) Math.ceil(sortedByScore.size() * 0.2);
            int premiumCount = Math.max(20, percentCount);
            if (percentCount < 1) {
                premiumCount = 0;
            }
            // Clamp premiumCount so we never index beyond the list size
            if (premiumCount > sortedByScore.size()) {
                premiumCount = sortedByScore.size();
            }
            for (int i = 0; i < sortedByScore.size(); i++) {
                Server s = sortedByScore.get(i);
                s.setPremium(i < premiumCount);
            }
            // Log premium servers
            if (premiumCount > 0) {
                StringBuilder premiumLog = new StringBuilder("Premium servers selected: ");
                for (int i = 0; i < premiumCount; i++) {
                    Server s = sortedByScore.get(i);
                    premiumLog.append(s.getIpAddress())
                              .append(" (score=")
                              .append(serverScoreMap.get(s))
                              .append(") ");
                }
                Log.d("PremiumSelect", premiumLog.toString());
            } else {
                Log.d("PremiumSelect", "No premium servers selected (premiumCount=0)");
            }

            // --- Save updated server list to cache ---
            sharedPreference.saveServerList(serverList);
            Log.d("ServerPageDebug", "[PremiumCalc] Saved updated server list to cache, count: " + serverList.size());
        }).start();
    }

}