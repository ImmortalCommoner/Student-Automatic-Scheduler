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


public class DayFragment extends Fragment {

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

        List<ScheduleItem> list = db.getByDay(today);

        recycler.setAdapter(new ScheduleAdapter(list));
    }

}