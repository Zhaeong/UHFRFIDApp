package com.idata.workingrfid;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.idata.workingrfid.database.RFIDDatabaseHelper;
import com.idata.workingrfid.database.RFIDItem;

import java.util.List;

public class ItemsListActivity extends AppCompatActivity {

    private TextView tvItems;
    private RFIDDatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_items_list);

        tvItems = findViewById(R.id.tvItems);
        Button btnRefresh = findViewById(R.id.btnRefresh);
        Button btnBack = findViewById(R.id.btnBack);

        dbHelper = new RFIDDatabaseHelper(this);

        displayItems();

        btnRefresh.setOnClickListener(v -> displayItems());
        btnBack.setOnClickListener(v -> finish());
    }

    private void displayItems() {
        List<RFIDItem> items = dbHelper.getAllItems();

        if (items.isEmpty()) {
            tvItems.setText("No items in database");
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < items.size(); i++) {
                RFIDItem item = items.get(i);
                sb.append("##").append(i + 1).append("\n");
                sb.append("Tag: ").append(item.getTag()).append("\n");
                sb.append("Name: ").append(item.getName()).append("\n");
                sb.append("Location: ").append(item.getLocation()).append("\n\n");
            }
            tvItems.setText(sb.toString());
        }
    }
}
