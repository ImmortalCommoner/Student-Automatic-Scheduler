package com.example.studentautomaticscheduler;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import android.content.ContentValues;
import android.database.Cursor;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "scheduler.db";
    private static final int DB_VERSION = 5; // Upgraded for more fields

    public static final String TABLE_SCHEDULE = "schedule";
    public static final String TABLE_USERS = "users";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE " + TABLE_SCHEDULE + " (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "day TEXT," +
                        "time TEXT," +
                        "subject TEXT," +
                        "subject_code TEXT," +
                        "section TEXT," +
                        "room TEXT," +
                        "instructor TEXT," +
                        "status TEXT," +
                        "units TEXT)"
        );

        db.execSQL(
                "CREATE TABLE " + TABLE_USERS + " (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "username TEXT UNIQUE," +
                        "password TEXT)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_USERS + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "username TEXT UNIQUE," +
                    "password TEXT)");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_SCHEDULE + " ADD COLUMN section TEXT");
            db.execSQL("ALTER TABLE " + TABLE_SCHEDULE + " ADD COLUMN room TEXT");
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE " + TABLE_SCHEDULE + " ADD COLUMN instructor TEXT");
        }
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE " + TABLE_SCHEDULE + " ADD COLUMN subject_code TEXT");
            db.execSQL("ALTER TABLE " + TABLE_SCHEDULE + " ADD COLUMN status TEXT");
            db.execSQL("ALTER TABLE " + TABLE_SCHEDULE + " ADD COLUMN units TEXT");
        }
    }

    public void insertSchedule(ScheduleItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = createContentValues(item);
        db.insert(TABLE_SCHEDULE, null, cv);
    }

    public void updateSchedule(ScheduleItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = createContentValues(item);
        db.update(TABLE_SCHEDULE, cv, "id=?", new String[]{String.valueOf(item.id)});
    }

    private ContentValues createContentValues(ScheduleItem item) {
        ContentValues cv = new ContentValues();
        cv.put("day", item.day);
        cv.put("time", item.time);
        cv.put("subject", item.subject);
        cv.put("subject_code", item.subjectCode);
        cv.put("section", item.section);
        cv.put("room", item.room);
        cv.put("instructor", item.instructor);
        cv.put("status", item.status);
        cv.put("units", item.units);
        return cv;
    }

    public List<ScheduleItem> getAllSchedules() {
        List<ScheduleItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_SCHEDULE + " ORDER BY day, time ASC", null);
        while (cursor.moveToNext()) {
            list.add(createItemFromCursor(cursor));
        }
        cursor.close();
        return list;
    }

    public List<ScheduleItem> getSchedulesByDay(String dayFilter) {
        List<ScheduleItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_SCHEDULE, null, "day=?", new String[]{dayFilter}, null, null, "time ASC");
        while (cursor.moveToNext()) {
            list.add(createItemFromCursor(cursor));
        }
        cursor.close();
        return list;
    }

    private ScheduleItem createItemFromCursor(Cursor cursor) {
        return new ScheduleItem(
                cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                cursor.getString(cursor.getColumnIndexOrThrow("day")),
                cursor.getString(cursor.getColumnIndexOrThrow("time")),
                cursor.getString(cursor.getColumnIndexOrThrow("subject")),
                cursor.getString(cursor.getColumnIndexOrThrow("subject_code")),
                cursor.getString(cursor.getColumnIndexOrThrow("section")),
                cursor.getString(cursor.getColumnIndexOrThrow("room")),
                cursor.getString(cursor.getColumnIndexOrThrow("instructor")),
                cursor.getString(cursor.getColumnIndexOrThrow("status")),
                cursor.getString(cursor.getColumnIndexOrThrow("units"))
        );
    }

    public boolean registerUser(String username, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("username", username);
        cv.put("password", password);
        long result = db.insert(TABLE_USERS, null, cv);
        return result != -1;
    }

    public boolean checkUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, null, "username=? AND password=?", new String[]{username, password}, null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }
}
