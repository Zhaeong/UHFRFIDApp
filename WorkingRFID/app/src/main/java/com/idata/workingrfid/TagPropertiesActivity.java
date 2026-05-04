package com.idata.workingrfid;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.idata.workingrfid.database.RFIDDatabaseHelper;
import com.idata.workingrfid.database.RFIDItem;

import java.util.ArrayList;

public class TagPropertiesActivity extends AppCompatActivity {

    private Spinner spinnerTags;
    private EditText etName, etLocation;
    private RFIDDatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tag_properties);

        spinnerTags = findViewById(R.id.spinnerTags);
        etName = findViewById(R.id.etName);
        etLocation = findViewById(R.id.etLocation);
        Button btnSave = findViewById(R.id.btnSave);
        Button btnBack = findViewById(R.id.btnBack);

        dbHelper = new RFIDDatabaseHelper(this);

        loadTags();

        btnSave.setOnClickListener(v -> saveItem());
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadTags() {
        ArrayList<String> eTags = MainActivity.getETags();
        ArrayList<String> availableTags = new ArrayList<>();

        for (String tag : eTags) {
            if (dbHelper.getItemByTag(tag) == null) {
                availableTags.add(tag);
            }
        }

        if (availableTags.isEmpty()) {
            availableTags.add("No tags available");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, availableTags);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTags.setAdapter(adapter);
    }

    private void saveItem() {
        String selectedTag = spinnerTags.getSelectedItem().toString();
        String name = etName.getText().toString().trim();
        String location = etLocation.getText().toString().trim();

        if (selectedTag.equals("No tags available")) {
            Toast.makeText(this, "No tags to save", Toast.LENGTH_SHORT).show();
            return;
        }

        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show();
            return;
        }

        RFIDItem item = new RFIDItem(selectedTag, name, location);
        long result = dbHelper.addItem(item);

        if (result != -1) {
            Toast.makeText(this, "Item saved successfully", Toast.LENGTH_SHORT).show();
            etName.setText("");
            etLocation.setText("");
            loadTags();
        } else {
            Toast.makeText(this, "Failed to save item", Toast.LENGTH_SHORT).show();
        }
    }
}
