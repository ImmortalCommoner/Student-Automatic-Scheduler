package com.example.studentautomaticscheduler;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.View;
import android.util.TypedValue;
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

        String[] timeSlots = {
                "06:00AM - 07:00AM", "07:00AM - 08:00AM", "08:00AM - 09:00AM",
                "09:00AM - 10:00AM", "10:00AM - 11:00AM", "11:00AM - 12:00PM",
                "12:00PM - 01:00PM", "01:00PM - 02:00PM", "02:00PM - 03:00PM",
                "03:00PM - 04:00PM", "04:00PM - 05:00PM", "05:00PM - 06:00PM",
                "06:00PM - 07:00PM", "07:00PM - 08:00PM", "08:00PM - 09:00PM"
        };

        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mma", Locale.US);

        Map<String, Map<String, ScheduleItem>> tableData = new LinkedHashMap<>();
        for (String slot : timeSlots) {
            tableData.put(slot, new HashMap<String, ScheduleItem>());
        }

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

                    if (itemStart < slotEnd && slotStart < itemEnd) {
                        tableData.get(slot).put(item.day, item);
                    }
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        for (String slot : timeSlots) {
            TableRow row = new TableRow(getContext());
            row.setPadding(0, 4, 0, 4);

            row.addView(createTableCell(slot, true, null));

            Map<String, ScheduleItem> dayMap = tableData.get(slot);
            for (String day : days) {
                if (dayMap.containsKey(day)) {
                    ScheduleItem item = dayMap.get(day);
                    TextView txtClass = createTableCell(item.subject + "\n" + item.room, false, item);
                    
                    txtClass.setOnLongClickListener(v -> {
                        showActionDialog(item);
                        return true;
                    });
                    
                    row.addView(txtClass);
                } else {
                    TextView txtFree = createTableCell("FREE", false, null);
                    row.addView(txtFree);
                }
            }
            table.addView(row);
        }
    }

    private void showActionDialog(ScheduleItem item) {
        String[] options = {"Edit", "Delete"};
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Schedule Action")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        editItem(item);
                    } else if (which == 1) {
                        deleteItem(item);
                    }
                })
                .show();
    }

    private void editItem(ScheduleItem item) {
        ArrayList<ScheduleItem> editList = new ArrayList<>();
        editList.add(item);
        android.content.Intent intent = new android.content.Intent(getContext(), EditScheduleActivity.class);
        intent.putExtra("SCHEDULE_ITEMS", editList);
        intent.putExtra("SINGLE_EDIT_MODE", true);
        startActivity(intent);
    }

    private void deleteItem(ScheduleItem item) {
        DatabaseHelper db = new DatabaseHelper(getContext());
        db.getWritableDatabase().delete(
                DatabaseHelper.TABLE_SCHEDULE,
                "id=?",
                new String[]{String.valueOf(item.id)}
        );
        getParentFragmentManager().beginTransaction().detach(this).attach(this).commit();
        NotificationHelper.scheduleClassReminders(requireContext());
    }

    private TextView createTableCell(String text, boolean isHeader, ScheduleItem item) {
        TextView tv = new TextView(getContext());
        tv.setText(text);
        tv.setPadding(24, 48, 24, 48); 
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setTextSize(10);
        
        int minHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80, getResources().getDisplayMetrics());
        tv.setMinHeight(minHeight);

        TableRow.LayoutParams lp = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.MATCH_PARENT);
        tv.setLayoutParams(lp);

        boolean isDarkMode = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) 
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;

        int bgColor;
        int textColor;

        if (isHeader) {
            bgColor = isDarkMode ? Color.parseColor("#1E1E1E") : Color.parseColor("#F5F5F5");
            textColor = isDarkMode ? Color.WHITE : Color.BLACK;
            tv.setTypeface(null, Typeface.BOLD);
        } else if (item != null) {
            String mode = item.classMode != null ? item.classMode : "Default";
            if (mode.equalsIgnoreCase("Online")) {
                bgColor = isDarkMode ? Color.parseColor("#1565C0") : Color.parseColor("#E3F2FD");
            } else if (mode.equalsIgnoreCase("Face-to-Face")) {
                bgColor = isDarkMode ? Color.parseColor("#2E7D32") : Color.parseColor("#E8F5E9");
            } else {
                bgColor = isDarkMode ? Color.parseColor("#333333") : Color.parseColor("#F5F5F5");
            }
            textColor = isDarkMode ? Color.WHITE : Color.BLACK;
        } else {
            bgColor = isDarkMode ? Color.parseColor("#121212") : Color.WHITE;
            textColor = isDarkMode ? Color.parseColor("#444444") : Color.parseColor("#CCCCCC");
        }

        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setColor(bgColor);
        gd.setStroke(1, isDarkMode ? Color.parseColor("#333333") : Color.parseColor("#CCCCCC"));
        tv.setBackground(gd);
        tv.setTextColor(textColor);

        return tv;
    }
}
