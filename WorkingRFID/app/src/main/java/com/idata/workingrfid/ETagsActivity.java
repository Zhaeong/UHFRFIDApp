package com.idata.workingrfid;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class ETagsActivity extends AppCompatActivity {

    private TextView tvETags;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_e_tags);

        tvETags = findViewById(R.id.tvETags);
        Button btnRefresh = findViewById(R.id.btnRefresh);
        Button btnBack = findViewById(R.id.btnBack);

        displayETags();

        btnRefresh.setOnClickListener(v -> displayETags());
        btnBack.setOnClickListener(v -> finish());
    }

    private void displayETags() {
        ArrayList<String> eTags = MainActivity.getETags();
        if (eTags.isEmpty()) {
            tvETags.setText("No tags starting with 'E' found");
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < eTags.size(); i++) {
                sb.append("##").append(i + 1).append(": ").append(eTags.get(i)).append("\n");
            }
            tvETags.setText(sb.toString());
        }
    }
}
