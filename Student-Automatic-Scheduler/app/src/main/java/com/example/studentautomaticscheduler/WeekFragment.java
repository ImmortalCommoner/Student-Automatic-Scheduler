package com.example.studentautomaticscheduler;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WeekFragment extends Fragment {

    public WeekFragment() {
        super(R.layout.fragment_week);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TableLayout table = view.findViewById(R.id.tableSchedule);
        DatabaseHelper db = new DatabaseHelper(getContext());
        List<ScheduleItem> schedules = db.getAllSchedules();

        // Define fixed hourly slots from 6AM to 9PM
        String[] timeSlots = {
                "06:00AM - 07:00AM", "07:00AM - 08:00AM", "08:00AM - 09:00AM",
                "09:00AM - 10:00AM", "10:00AM - 11:00AM", "11:00AM - 12:00PM",
                "12:00PM - 01:00PM", "01:00PM - 02:00PM", "02:00PM - 03:00PM",
                "03:00PM - 04:00PM", "04:00PM - 05:00PM", "05:00PM - 06:00PM",
                "06:00PM - 07:00PM", "07:00PM - 08:00PM", "08:00PM - 09:00PM"
        };

        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mma", Locale.US);

        // Map to store which slot contains which class
        Map<String, Map<String, ScheduleItem>> tableData = new LinkedHashMap<>();
        for (String slot : timeSlots) {
            tableData.put(slot, new HashMap<String, ScheduleItem>());
        }

        // Map classes into slots
        for (ScheduleItem item : schedules) {
            if (item.time == null || !item.time.contains("-")) continue;

            try {
                String[] times = item.time.split("-");
                long itemStart = sdf.parse(times[0].trim()).getTime();
                long itemEnd = sdf.parse(times[1].trim()).getTime();

                for (String slot : timeSlots) {
                    String[] slotTimes = slot.split("-");
                    long slotStart = sdf.parse(slotTimes[0].trim()).getTime();
                    long slotEnd = sdf.parse(slotTimes[1].trim()).getTime();

                    // Check if class overlaps with this hourly slot
                    // Overlap: itemStart < slotEnd AND slotStart < itemEnd
                    if (itemStart < slotEnd && slotStart < itemEnd) {
                        tableData.get(slot).put(item.day, item);
                    }
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        // Build the table rows
        for (String slot : timeSlots) {
            TableRow row = new TableRow(getContext());
            row.setPadding(0, 4, 0, 4);

            // Time Slot Column
            row.addView(createTableCell(slot, true));

            // Day Columns
            Map<String, ScheduleItem> dayMap = tableData.get(slot);
            for (String day : days) {
                if (dayMap.containsKey(day)) {
                    ScheduleItem item = dayMap.get(day);
                    TextView txtClass = createTableCell(item.subject + "\n" + item.room, false);
                    txtClass.setBackgroundColor(Color.parseColor("#E3F2FD")); // Light blue for classes
                    row.addView(txtClass);
                } else {
                    // Empty cell (Free Time)
                    TextView txtFree = createTableCell("FREE", false);
                    txtFree.setTextColor(Color.parseColor("#CCCCCC"));
                    row.addView(txtFree);
                }
            }
            table.addView(row);
        }
    }

    private TextView createTableCell(String text, boolean isHeader) {
        TextView tv = new TextView(getContext());
        tv.setText(text);
        tv.setPadding(16, 24, 16, 24);
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setTextSize(11);
        
        if (isHeader) {
            tv.setBackgroundColor(Color.parseColor("#F5F5F5"));
            tv.setTypeface(null, Typeface.BOLD);
            tv.setMinWidth(300);
        } else {
            tv.setMinWidth(350);
        }

        tv.setBackgroundResource(R.drawable.cell_border);
        return tv;
    }
}
