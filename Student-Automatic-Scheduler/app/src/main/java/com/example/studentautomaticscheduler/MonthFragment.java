package com.example.studentautomaticscheduler;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


public class MonthFragment extends Fragment {

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


        List<ScheduleItem> list = db.getAll();


        recycler.setAdapter(new ScheduleAdapter(list));
    }
}