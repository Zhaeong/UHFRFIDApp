package com.idata.workingrfid;

import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.content.Intent;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import androidx.appcompat.app.AppCompatActivity;

import com.uhf.base.UHFManager;
import com.uhf.base.UHFModuleType;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "RFID";

    private UHFManager uhfManager;
    private Button btnStart, btnStop, btnClear;
    private TextView tvStatus, tvTags;
    private volatile boolean running = true;
    private volatile boolean scanning = false;
    private int tagCount = 0;
    private static ArrayList<String> eTags = new ArrayList<>();
    private Thread readThread;

    public static ArrayList<String> getETags() {
        return eTags;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);
        btnClear = findViewById(R.id.btnClear);
        tvStatus = findViewById(R.id.tvStatus);
        tvTags = findViewById(R.id.tvTags);

        btnStart.setOnClickListener(v -> startScan());
        btnStop.setOnClickListener(v -> stopScan());
        btnClear.setOnClickListener(v -> { tvTags.setText(""); tagCount = 0; eTags.clear(); });

        Button btnViewETags = findViewById(R.id.btnViewETags);
        btnViewETags.setOnClickListener(v -> startActivity(new Intent(this, ETagsActivity.class)));

        btnStart.setEnabled(false);
        btnStop.setEnabled(false);

        initUHF();
    }

    private void initUHF() {
        try {
            tvStatus.setText("Initializing UHF...");

            // Must be called on main thread - SLRLib creates a Handler
            uhfManager = UHFManager.getUHFImplSigleInstance(UHFModuleType.SLR_MODULE, this);

            new Thread(() -> {
                try {
                    boolean powerOn = uhfManager.powerOn();
                    Log.d(TAG, "Power on: " + powerOn);

                    SystemClock.sleep(2000);

                    String moduleType = uhfManager.getUHFModuleType();
                    String firmware = uhfManager.firmwareVerGet();
                    Log.d(TAG, "Module: " + moduleType + ", Firmware: " + firmware);

                    uhfManager.powerSet(30);

                    String status = powerOn ? "UHF Ready (" + moduleType + ")" : "UHF Init Failed";
                    runOnUiThread(() -> {
                        tvStatus.setText(status);
                        btnStart.setEnabled(powerOn);
                    });

                    startReadThread();
                } catch (Exception e) {
                    Log.e(TAG, "Init error: " + e.getMessage(), e);
                    runOnUiThread(() -> tvStatus.setText("Error: " + e.getMessage()));
                }
            }).start();
        } catch (Exception e) {
            Log.e(TAG, "Manager creation error: " + e.getMessage(), e);
            tvStatus.setText("Error: " + e.getMessage());
        }
    }

    private void startReadThread() {
        readThread = new Thread(() -> {
            while (running) {
                if (scanning && uhfManager != null) {
                    try {
                        String[] tags = uhfManager.readTagFromBuffer();
                        if (tags != null && tags.length > 0) {
                            for (String tagData : tags) {
                                if (tagData != null && tagData.startsWith("E") && !eTags.contains(tagData)) {
                                    eTags.add(tagData);
                                }
                                runOnUiThread(() -> {
                                    tagCount++;
                                    tvTags.append("##" + tagCount + ": " + tagData + "\n");
                                });
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Read error: " + e.getMessage());
                    }
                }
                SystemClock.sleep(50);
            }
        });
        readThread.start();
    }

    private void startScan() {
        if (scanning || uhfManager == null) return;
        boolean started = uhfManager.startInventoryTag();
        if (started) {
            scanning = true;
            btnStart.setEnabled(false);
            btnStop.setEnabled(true);
            tvStatus.setText("Scanning tho...");
        } else {
            Toast.makeText(this, "Failed to start", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopScan() {
        if (!scanning || uhfManager == null) return;
        uhfManager.stopInventory();
        scanning = false;
        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
        tvStatus.setText("Stopped");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        running = false;
        if (scanning && uhfManager != null) {
            uhfManager.stopInventory();
        }
        if (uhfManager != null) {
            uhfManager.powerOff();
        }
    }
}
