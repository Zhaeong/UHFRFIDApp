package com.idata.workingrfid;

import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.idata.workingrfid.database.RFIDDatabaseHelper;
import com.idata.workingrfid.database.RFIDItem;
import com.uhf.base.UHFManager;
import com.uhf.base.UHFModuleType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LocationScanActivity extends AppCompatActivity {

    private static final String TAG = "LocationScan";

    private Spinner spinnerLocation;
    private Button btnStartScan, btnBack;
    private ImageView imgStatus;
    private TextView tvStatus, tvItems;
    private RFIDDatabaseHelper dbHelper;
    private UHFManager uhfManager;
    private volatile boolean scanning = false;
    private volatile boolean running = true;
    private Thread scanThread;
    private Set<String> scannedTags;
    private List<RFIDItem> locationItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_scan);

        spinnerLocation = findViewById(R.id.spinnerLocation);
        btnStartScan = findViewById(R.id.btnStartScan);
        btnBack = findViewById(R.id.btnBack);
        imgStatus = findViewById(R.id.imgStatus);
        tvStatus = findViewById(R.id.tvStatus);
        tvItems = findViewById(R.id.tvItems);

        dbHelper = new RFIDDatabaseHelper(this);
        scannedTags = new HashSet<>();

        loadLocations();

        spinnerLocation.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                scannedTags.clear();
                imgStatus.setVisibility(ImageView.INVISIBLE);
                tvStatus.setText("Select location and start scanning");
                if (scanning) {
                    stopLocationScan();
                }
                displayLocationItems();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        btnStartScan.setOnClickListener(v -> {
            if (!scanning) {
                startLocationScan();
            } else {
                stopLocationScan();
            }
        });

        btnBack.setOnClickListener(v -> {
            stopLocationScan();
            finish();
        });

        initUHF();
    }

    private void initUHF() {
        // Get UHF manager from MainActivity if available
        uhfManager = MainActivity.uhfManagerStatic;

        if (uhfManager != null) {
            Log.d(TAG, "Using existing UHF manager from MainActivity");
            tvStatus.setText("UHF Ready - Select location and scan");
            btnStartScan.setEnabled(true);
        } else {
            tvStatus.setText("UHF not ready. Please go back and wait.");
            btnStartScan.setEnabled(false);
        }
    }

    private void loadLocations() {
        List<RFIDItem> items = dbHelper.getAllItems();
        Set<String> locations = new HashSet<>();

        for (RFIDItem item : items) {
            if (item.getLocation() != null && !item.getLocation().isEmpty()) {
                locations.add(item.getLocation());
            }
        }

        ArrayList<String> locationList = new ArrayList<>(locations);
        if (locationList.isEmpty()) {
            locationList.add("No locations available");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, locationList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLocation.setAdapter(adapter);

        displayLocationItems();
    }

    private void displayLocationItems() {
        String selectedLocation = spinnerLocation.getSelectedItem().toString();
        if (selectedLocation.equals("No locations available")) {
            tvItems.setText("No items to display");
            return;
        }

        locationItems = dbHelper.getAllItems();
        locationItems.removeIf(item -> !selectedLocation.equals(item.getLocation()));

        if (locationItems.isEmpty()) {
            tvItems.setText("No items at this location");
        } else {
            StringBuilder sb = new StringBuilder();
            for (RFIDItem item : locationItems) {
                String status = scannedTags.contains(item.getTag()) ? "✓" : "○";
                sb.append(status).append(" ").append(item.getName())
                        .append(" (").append(item.getTag()).append(")\n");
            }
            tvItems.setText(sb.toString());
        }
    }

    private void startLocationScan() {
        if (MainActivity.uhfManagerStatic == null) {
            Toast.makeText(this, "UHF not initialized. Go back and wait for UHF Ready.", Toast.LENGTH_LONG).show();
            return;
        }
        uhfManager = MainActivity.uhfManagerStatic;

        String selectedLocation = spinnerLocation.getSelectedItem().toString();
        if (selectedLocation.equals("No locations available")) {
            Toast.makeText(this, "No locations to scan", Toast.LENGTH_SHORT).show();
            return;
        }

        scannedTags.clear();
        imgStatus.setVisibility(ImageView.INVISIBLE);
        scanning = true;
        btnStartScan.setText("Stop Scan");
        tvStatus.setText("Scanning for items at: " + selectedLocation);

        // Stop any existing inventory
        try {
            uhfManager.stopInventory();
            SystemClock.sleep(300);
        } catch (Exception e) {}

        boolean started = uhfManager.startInventoryTag();
        Log.d(TAG, "Inventory started: " + started);

        if (!started) {
            tvStatus.setText("Failed to start inventory - retrying...");
            // Retry once
            try {
                SystemClock.sleep(500);
                uhfManager.stopInventory();
                SystemClock.sleep(200);
                started = uhfManager.startInventoryTag();
                Log.d(TAG, "Inventory retry: " + started);
            } catch (Exception e) {}

            if (!started) {
                tvStatus.setText("Failed to start inventory");
                scanning = false;
                btnStartScan.setText("Start Scan");
                return;
            }
        }

        scanThread = new Thread(() -> {
            while (running && scanning) {
                try {
                    String[] tags = uhfManager.readTagFromBuffer();
                    if (tags != null && tags.length > 0) {
                        for (String tag : tags) {
                            if (tag != null && tag.startsWith("E")) {
                                checkTagLocation(tag);
                            }
                        }
                    }
                    SystemClock.sleep(100);
                } catch (Exception e) {
                    Log.e(TAG, "Read error: " + e.getMessage());
                }
            }
        });
        scanThread.start();
    }

    private void checkTagLocation(String tag) {
        if (locationItems == null) return;

        for (RFIDItem item : locationItems) {
            if (tag.equals(item.getTag())) {
                scannedTags.add(tag);
                runOnUiThread(() -> {
                    displayLocationItems();
                    tvStatus.setText("Found: " + item.getName());

                    // Check if all items are scanned
                    if (scannedTags.size() == locationItems.size()) {
                        imgStatus.setImageResource(android.R.drawable.checkbox_on_background);
                        imgStatus.setColorFilter(Color.GREEN);
                        imgStatus.setVisibility(ImageView.VISIBLE);
                        tvStatus.setText("All items found at this location!");
                        stopLocationScan();
                    }
                });
                break;
            }
        }
    }

    private void stopLocationScan() {
        scanning = false;

        // Wait a bit for the scan thread to finish
        if (scanThread != null && scanThread.isAlive()) {
            try {
                scanThread.join(500);
            } catch (InterruptedException e) {}
        }

        if (uhfManager != null) {
            try {
                uhfManager.stopInventory();
                SystemClock.sleep(200);
            } catch (Exception e) {
                Log.e(TAG, "Error stopping inventory: " + e.getMessage());
            }
        }
        btnStartScan.setText("Start Scan");
        if (imgStatus.getVisibility() != ImageView.VISIBLE) {
            tvStatus.setText("Scan stopped - Click Start to scan again");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        running = false;
        scanning = false;
        if (uhfManager != null) {
            try {
                uhfManager.stopInventory();
                // Don't power off - MainActivity owns the UHF manager
            } catch (Exception e) {}
        }
    }
}
