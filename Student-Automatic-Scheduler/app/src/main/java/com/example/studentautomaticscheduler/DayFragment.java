package com.example.studentautomaticscheduler;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import android.view.View;


public class DayFragment extends Fragment implements ScheduleAdapter.OnItemLongClick {

    private List<ScheduleItem> list;
    private ScheduleAdapter adapter;

    public DayFragment() {
        super(R.layout.fragment_day);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recycler = view.findViewById(R.id.recyclerSchedule);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));

        DatabaseHelper db = new DatabaseHelper(getContext());

        // get today name (Mon, Tue, etc.)
        String today = new java.text.SimpleDateFormat("EEE").format(new java.util.Date());

        list = db.getByDay(today);
        adapter = new ScheduleAdapter(list, this);
        recycler.setAdapter(adapter);
    }

    @Override
    public void onDelete(int position) {
        ScheduleItem item = list.get(position);
        DatabaseHelper db = new DatabaseHelper(getContext());
        db.getWritableDatabase().delete(
                "schedule",
                "day=? AND time=? AND subject=?",
                new String[]{item.day, item.time, item.subject}
        );
        list.remove(position);
        adapter.notifyItemRemoved(position);
    }
}
