package com.example.nocturnevpn.view.activitys;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import com.badoo.mobile.util.WeakHandler;
import com.example.nocturnevpn.BuildConfig;
import com.example.nocturnevpn.R;
import com.example.nocturnevpn.SharedPreference;
import com.example.nocturnevpn.adapter.ServerAdapter;
import com.example.nocturnevpn.databinding.ActivityChangeServerBinding;
import com.example.nocturnevpn.db.DbHelper;
import com.example.nocturnevpn.model.Server;
import com.example.nocturnevpn.utils.CsvParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
    private Request request;
    private Call mCall;
    private ServerAdapter adapter;
    private DbHelper dbHelper;

    private SharedPreference sharedPreference;


    private Server globalServer;

    private Dialog infoAlertDialog;



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


    private void setupRecyclerView() {
        adapter = new ServerAdapter(servers, serverClickCallback);

        // ✅ Load saved selected IP
        String savedIp = getSharedPreferences("vpn_prefs", MODE_PRIVATE)
                .getString("selected_ip", "");
        adapter.setSelectedServer(savedIp);

        RecyclerView.ItemDecoration itemDecoration = new
                DividerItemDecoration(ChangeServerActivity.this, 0);
        binding.recyclerView.setHasFixedSize(true);
        binding.recyclerView.addItemDecoration(itemDecoration);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(binding.recyclerView.getContext()));
        binding.recyclerView.setAdapter(adapter);
    }

    private void loadServerList(List<Server> serverList) {
        adapter.setServerList(serverList);

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

        mCall = okHttpClient.newCall(request);
        mCall.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        binding.swipeRefresh.setRefreshing(false);
                        binding.recyclerView.setVisibility(View.VISIBLE);
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
                        }
                    });
                }
            }
        });
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

//    private void setIntentResult(Server server) {
//        Intent resultIntent = new Intent();
//        resultIntent.putExtra("serverextra", server);
//        setResult(RESULT_OK, resultIntent);
//        finish();
//    }

}