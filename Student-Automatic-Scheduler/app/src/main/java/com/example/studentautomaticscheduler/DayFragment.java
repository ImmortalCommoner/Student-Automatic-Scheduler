package com.example.studentautomaticscheduler;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import android.view.View;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DayFragment extends Fragment implements ScheduleAdapter.OnItemActionListener {

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
        String today = new SimpleDateFormat("EEE", Locale.US).format(new Date());

        list = db.getSchedulesByDay(today);
        adapter = new ScheduleAdapter(list, this);
        recycler.setAdapter(adapter);
    }

    @Override
    public void onItemAction(int position, String action) {
        if ("PROMPT".equals(action)) {
            showActionDialog(position);
        }
    }

    private void showActionDialog(int position) {
        String[] options = {"Edit", "Delete"};
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Schedule Action")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        editItem(position);
                    } else if (which == 1) {
                        deleteItem(position);
                    }
                })
                .show();
    }

    private void editItem(int position) {
        ScheduleItem item = list.get(position);
        java.util.ArrayList<ScheduleItem> editList = new java.util.ArrayList<>();
        editList.add(item);

        android.content.Intent intent = new android.content.Intent(getContext(), EditScheduleActivity.class);
        intent.putExtra("SCHEDULE_ITEMS", editList);
        intent.putExtra("SINGLE_EDIT_MODE", true);
        startActivity(intent);
    }

    private void deleteItem(int position) {
        ScheduleItem item = list.get(position);
        DatabaseHelper db = new DatabaseHelper(getContext());
        db.getWritableDatabase().delete(
                DatabaseHelper.TABLE_SCHEDULE,
                "id=?",
                new String[]{String.valueOf(item.id)}
        );
        list.remove(position);
        adapter.notifyItemRemoved(position);

        // Refresh notifications
        NotificationHelper.scheduleClassReminders(requireContext());
    }
}
