package com.example.nocturnevpn.view.activitys;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.badoo.mobile.util.WeakHandler;
import com.example.nocturnevpn.BuildConfig;
import com.example.nocturnevpn.R;
import com.example.nocturnevpn.SharedPreference;
import com.example.nocturnevpn.adapter.ExpandableServerAdapter;
import com.example.nocturnevpn.adapter.ServerAdapter;
import com.example.nocturnevpn.databinding.ActivityChangeServerBinding;
import com.example.nocturnevpn.db.DbHelper;
import com.example.nocturnevpn.model.CountryServerGroup;
import com.example.nocturnevpn.model.Server;
import com.example.nocturnevpn.utils.CsvParser;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_change_server);
        handler = new WeakHandler();
        dbHelper = DbHelper.getInstance(getApplicationContext());
        sharedPreference = new SharedPreference(ChangeServerActivity.this);
        binding = ActivityChangeServerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Check scheduled work status
        checkScheduledWorkStatus();

        // Load cached servers from database
        servers.addAll(dbHelper.getAll());
        setupSwipeRefreshLayout();
        setupRecyclerView();

        if (request == null) {
            request = new Request.Builder()
                    .url(BuildConfig.VPN_GATE_API)
                    .build();
        }

        if (servers.isEmpty()) {
            populateServerList();
        } else {
            // Use cached data
            loadServerList(servers);
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

    private void filterServers(String query) {
        String lowerQuery = query.toLowerCase().trim();
        List<CountryServerGroup> filteredGroups = new ArrayList<>();

        for (CountryServerGroup group : fullGroupList) {
            List<Server> filteredServers = new ArrayList<>();
            for (Server server : group.getServers()) {
                if (server.getCountryLong().toLowerCase().contains(query.toLowerCase()) ||
                        server.getIpAddress().toLowerCase().contains(query.toLowerCase()) ||
                        server.getProtocol().toLowerCase().contains(query.toLowerCase()) ||
                        server.getHostName().toLowerCase().contains(query.toLowerCase())) {
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
                triggerServerFetch();
                populateServerList();
            }
        });
    }

    private void triggerServerFetch() {
        androidx.work.Data inputData = new androidx.work.Data.Builder()
                .putBoolean("is_manual_trigger", true)
                .build();

        androidx.work.OneTimeWorkRequest workRequest =
                new androidx.work.OneTimeWorkRequest.Builder(com.example.nocturnevpn.workers.ServerFetchWorker.class)
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
        List<CountryServerGroup> grouped = groupServersByCountry(serverList);

        // Always keep a fresh copy of full list
        fullGroupList.clear();
        fullGroupList.addAll(grouped);

        groupedServers.clear();
        groupedServers.addAll(grouped);
        adapter.updateList(grouped);  // Make sure you implement this in ExpandableServerAdapter

        Server savedServer = sharedPreference.getServer();
        if (savedServer != null) {
            adapter.setSelectedServer(savedServer.getIpAddress()); // Highlight selected
        }

        dbHelper.save(serverList, this);
    }

    /**
     * Displays the updated list of VPN servers
     */
    private void populateServerList() {
        binding.swipeRefresh.setRefreshing(true);
        binding.recyclerView.setVisibility(View.INVISIBLE);
        showSkeletonPlaceholders(); // Show skeletons

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

}