package com.kafica.ott.tis;

import android.app.Activity;
import android.media.tv.TvInputInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.kafica.ott.R;
import com.kafica.ott.repository.OttChannelRepository;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Launched by TvInputManager via setupActivity intent
// show OTT installation screen from diagram
public class OttInstallSetupActivity extends Activity {

    private TextView tvStatus;
    private ProgressBar progressBar;
    private Button btnStart;
    private android.widget.ListView lvChannels;
    private android.widget.ArrayAdapter<String> adapter;
    private java.util.ArrayList<String> installedChannelsList = new java.util.ArrayList<>();

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ott_install);

        tvStatus = findViewById(R.id.tvStatus);
        progressBar = findViewById(R.id.progressBar);
        btnStart = findViewById(R.id.btnStart);

        // inputId provided by TvInputManager — required for TV Provider
        String inputId = getIntent()
                .getStringExtra(TvInputInfo.EXTRA_INPUT_ID);

        String inputId2 = "com.kafica.ott/.tis.OttTvInputService";

        android.util.Log.d("OTT_SETUP", "Received Input ID: " + inputId2);

        lvChannels = findViewById(R.id.lvChannels);
// Use a simple built-in Android layout for the list items
        adapter = new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, installedChannelsList);

// Make the text white so it's visible on your dark background
        lvChannels.setAdapter(adapter);

        btnStart.setOnClickListener(v -> startInstallation(inputId2));
    }

    private void startInstallation(String inputId) {
        btnStart.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        tvStatus.setText("Starting installation...");

        OttChannelRepository repository =
                new OttChannelRepository(this, inputId);

        OttInstallController controller = new OttInstallController(
                this,
                repository,
                new OttInstallController.InstallCallback() {

                    @Override
                    public void onProgress(int processed, int total, String serviceId) {
                        mainHandler.post(() -> {
                            // Update the progress bar and text as you already do
                            int pct = (processed * 100) / total;
                            progressBar.setProgress(pct);
                            tvStatus.setText(processed + " / " + total + " channels installed");

                            // NEW: Add the channel to the list and refresh the UI
                            installedChannelsList.add("✔ Installed: " + serviceId);
                            adapter.notifyDataSetChanged();

                            // Optional: Auto-scroll to the bottom as new channels come in
                            lvChannels.setSelection(adapter.getCount() - 1);
                        });
                    }

                    @Override
                    public void onServiceSkipped(String serviceId) {
                        mainHandler.post(() ->
                                tvStatus.setText("Skipped: " + serviceId));
                    }

                    @Override
                    public void onCompleted(int serviceCount) {
                        mainHandler.post(() -> {
                            progressBar.setProgress(100);
                            tvStatus.setText("Done! "
                                    + serviceCount + " channels installed.");

                            setResult(Activity.RESULT_OK);
                            /*
                            mainHandler.postDelayed(
                                    OttInstallSetupActivity.this::finish,
                                    2000);*/
                        });
                    }

                    @Override
                    public void onCatalogError(String error) {
                        mainHandler.post(() -> {
                            tvStatus.setText(
                                    "Error: Cannot load OTT catalog\n" + error);
                            btnStart.setEnabled(true);
                        });
                    }

                    @Override
                    public void onTvProviderError(String error) {
                        mainHandler.post(() -> {
                            tvStatus.setText(
                                    "Error: Cannot save channels\n" + error);
                            btnStart.setEnabled(true);
                        });
                    }
                });

        // network calls must run off main thread
        executor.execute(controller::startInstallation);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}