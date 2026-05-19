package com.example.studentautomaticscheduler;

import android.content.Context;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class ReferenceDatabaseHelper {

    private final Context context;

    public ReferenceDatabaseHelper(Context context) {
        this.context = context;
    }

    public void copyDatabase() {

        try {

            String dbPath =
                    context.getDatabasePath("reference.db").getPath();

            java.io.File dbFile = new java.io.File(dbPath);

            if(!dbFile.exists()) {

                dbFile.getParentFile().mkdirs();

                InputStream input =
                        context.getAssets().open("reference.db");

                OutputStream output =
                        new FileOutputStream(dbPath);

                byte[] buffer = new byte[1024];

                int length;

                while((length = input.read(buffer)) > 0) {
                    output.write(buffer, 0, length);
                }

                output.flush();
                output.close();
                input.close();
            }

        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}