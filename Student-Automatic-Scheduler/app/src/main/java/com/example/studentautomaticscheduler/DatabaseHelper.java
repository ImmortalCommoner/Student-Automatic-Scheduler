package com.example.studentautomaticscheduler;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import android.content.ContentValues;
import android.database.Cursor;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "schedule.db";
    private static final int DB_VERSION = 1;

    public static final String TABLE = "schedule";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(
                "CREATE TABLE " + TABLE + " (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "day TEXT," +
                        "time TEXT," +
                        "subject TEXT)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
        onCreate(db);
    }

    public void insert(String day, String time, String subject) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put("day", day);
        cv.put("time", time);
        cv.put("subject", subject);

        db.insert(TABLE, null, cv);
    }

    public List<ScheduleItem> getAll() {

        List<ScheduleItem> list = new ArrayList<>();

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE + " ORDER BY time ASC",
                null
        );


        while (cursor.moveToNext()) {

            String day = cursor.getString(cursor.getColumnIndexOrThrow("day"));
            String time = cursor.getString(cursor.getColumnIndexOrThrow("time"));
            String subject = cursor.getString(cursor.getColumnIndexOrThrow("subject"));

            list.add(new ScheduleItem(day, time, subject));
        }

        cursor.close();

        return list;
    }

    public List<ScheduleItem> getByDay(String dayFilter) {

        List<ScheduleItem> list = new ArrayList<>();

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE + " WHERE day=? ORDER BY time ASC",
                new String[]{dayFilter}
        );

        while (cursor.moveToNext()) {

            String day = cursor.getString(cursor.getColumnIndexOrThrow("day"));
            String time = cursor.getString(cursor.getColumnIndexOrThrow("time"));
            String subject = cursor.getString(cursor.getColumnIndexOrThrow("subject"));

            list.add(new ScheduleItem(day, time, subject));
        }

        cursor.close();

        return list;
    }

    public List<ScheduleItem> getWeek() {
        return getAll();
    }

}




