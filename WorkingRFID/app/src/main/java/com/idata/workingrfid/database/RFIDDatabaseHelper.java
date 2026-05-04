package com.idata.workingrfid.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class RFIDDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "rfid_items.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_ITEMS = "items";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TAG = "tag";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_LOCATION = "location";

    public RFIDDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_ITEMS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_TAG + " TEXT UNIQUE,"
                + COLUMN_NAME + " TEXT,"
                + COLUMN_LOCATION + " TEXT" + ")";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ITEMS);
        onCreate(db);
    }

    public long addItem(RFIDItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TAG, item.getTag());
        values.put(COLUMN_NAME, item.getName());
        values.put(COLUMN_LOCATION, item.getLocation());
        long id = db.insert(TABLE_ITEMS, null, values);
        db.close();
        return id;
    }

    public List<RFIDItem> getAllItems() {
        List<RFIDItem> items = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_ITEMS + " ORDER BY " + COLUMN_NAME;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                RFIDItem item = new RFIDItem();
                item.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                item.setTag(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TAG)));
                item.setName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)));
                item.setLocation(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOCATION)));
                items.add(item);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return items;
    }

    public RFIDItem getItemByTag(String tag) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_ITEMS, null, COLUMN_TAG + "=?",
                new String[]{tag}, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            RFIDItem item = new RFIDItem();
            item.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
            item.setTag(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TAG)));
            item.setName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)));
            item.setLocation(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOCATION)));
            cursor.close();
            db.close();
            return item;
        }
        if (cursor != null) cursor.close();
        db.close();
        return null;
    }

    public void deleteItem(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_ITEMS, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }
}
