package com.example.studentautomaticscheduler;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import android.view.View;


public class MonthFragment extends Fragment implements ScheduleAdapter.OnItemLongClick {

    private List<ScheduleItem> list;
    private ScheduleAdapter adapter;

    public MonthFragment() {
        super(R.layout.fragment_month);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recycler = view.findViewById(R.id.recyclerSchedule);

        recycler.setLayoutManager(new LinearLayoutManager(getContext()));

        DatabaseHelper db = new DatabaseHelper(getContext());


        // TEMP: insert sample data only first run
        if (db.getAll().isEmpty()) {
            db.insert("Mon", "8:00-9:30", "Mathematics");
            db.insert("Mon", "10:00-11:30", "Programming");
            db.insert("Tue", "1:00-3:00", "Database");
            db.insert("Wed", "9:00-11:00", "Networking");
        }


        list = db.getAll();
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
