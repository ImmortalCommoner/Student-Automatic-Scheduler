package com.example.studentautomaticscheduler;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import android.view.View;
import android.widget.CalendarView;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MonthFragment extends Fragment implements ScheduleAdapter.OnItemLongClick {

    private List<ScheduleItem> list;
    private ScheduleAdapter adapter;
    private RecyclerView recycler;
    private TextView txtSelectedDate;

    public MonthFragment() {
        super(R.layout.fragment_month);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recycler = view.findViewById(R.id.recyclerSchedule);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        
        txtSelectedDate = view.findViewById(R.id.txtSelectedDate);
        CalendarView calendarView = view.findViewById(R.id.calendarView);

        DatabaseHelper db = new DatabaseHelper(getContext());

        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth);
            String dayOfWeek = new SimpleDateFormat("EEE", Locale.US).format(calendar.getTime());
            String fullDate = new SimpleDateFormat("MMMM dd, yyyy", Locale.US).format(calendar.getTime());
            
            txtSelectedDate.setText("Schedules for " + fullDate + " (" + dayOfWeek + "):");
            
            list = db.getSchedulesByDay(dayOfWeek);
            adapter = new ScheduleAdapter(list, this);
            recycler.setAdapter(adapter);
        });

        // Initialize with today's date
        String today = new SimpleDateFormat("EEE", Locale.US).format(Calendar.getInstance().getTime());
        list = db.getSchedulesByDay(today);
        adapter = new ScheduleAdapter(list, this);
        recycler.setAdapter(adapter);
    }

    @Override
    public void onDelete(int position) {
        ScheduleItem item = list.get(position);
        DatabaseHelper db = new DatabaseHelper(getContext());
        db.getWritableDatabase().delete(
                DatabaseHelper.TABLE_SCHEDULE,
                "day=? AND time=? AND subject=?",
                new String[]{item.day, item.time, item.subject}
        );
        list.remove(position);
        adapter.notifyItemRemoved(position);
    }
}
