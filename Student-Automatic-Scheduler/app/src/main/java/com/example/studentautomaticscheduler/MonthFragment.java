package com.example.studentautomaticscheduler;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import android.view.View;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import android.text.style.StyleSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import androidx.core.content.ContextCompat;
import android.graphics.Typeface;
import org.threeten.bp.LocalDate;
import org.threeten.bp.format.DateTimeFormatter;
import androidx.appcompat.app.AlertDialog;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MonthFragment extends Fragment implements ScheduleAdapter.OnItemActionListener {

    private List<ScheduleItem> list;
    private ScheduleAdapter adapter;
    private RecyclerView recycler;
    private TextView txtSelectedDate;
    private CurrentMonthDecorator currentMonthDecorator;
    private OtherMonthDecorator otherMonthDecorator;

    public MonthFragment() {
        super(R.layout.fragment_month);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recycler = view.findViewById(R.id.recyclerSchedule);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        
        txtSelectedDate = view.findViewById(R.id.txtSelectedDate);
        MaterialCalendarView calendarView = view.findViewById(R.id.calendarView);

        DatabaseHelper db = new DatabaseHelper(getContext());

        int initialMonth = CalendarDay.today().getMonth();
        currentMonthDecorator = new CurrentMonthDecorator(initialMonth);
        otherMonthDecorator = new OtherMonthDecorator(initialMonth);
        calendarView.addDecorators(currentMonthDecorator, otherMonthDecorator);

        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            LocalDate localDate = date.getDate();
            DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("EEE", Locale.US);
            DateTimeFormatter fullDateFormatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy", Locale.US);

            String dayOfWeek = localDate.format(dayFormatter);
            String fullDate = localDate.format(fullDateFormatter);
            
            txtSelectedDate.setText("Schedules for " + fullDate + " (" + dayOfWeek + "):");
            
            list = db.getSchedulesByDay(dayOfWeek);
            adapter = new ScheduleAdapter(list, this);
            recycler.setAdapter(adapter);
        });

        calendarView.setOnMonthChangedListener((widget, date) -> {
            currentMonthDecorator.setMonth(date.getMonth());
            otherMonthDecorator.setMonth(date.getMonth());
            widget.invalidateDecorators();
        });

        // Click title for month/year picker
        View titleView = calendarView.findViewById(getResources().getIdentifier("mcv_pager_title", "id", getContext().getPackageName()));
        if (titleView == null) {
            // Fallback: search for TextView within the header
            try {
                android.view.ViewGroup header = (android.view.ViewGroup) calendarView.getChildAt(0);
                for (int i = 0; i < header.getChildCount(); i++) {
                    if (header.getChildAt(i) instanceof TextView) {
                        titleView = header.getChildAt(i);
                        break;
                    }
                }
            } catch (Exception ignored) {}
        }
        
        if (titleView != null) {
            titleView.setOnClickListener(v -> showMonthYearPicker(calendarView));
        }

        calendarView.setSelectedDate(CalendarDay.today());

        String today = new SimpleDateFormat("EEE", Locale.US).format(Calendar.getInstance().getTime());
        list = db.getSchedulesByDay(today);
        adapter = new ScheduleAdapter(list, this);
        recycler.setAdapter(adapter);
    }

    private void showMonthYearPicker(MaterialCalendarView calendarView) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder builder = new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext());
        android.view.LayoutInflater inflater = getLayoutInflater();
        android.view.View dialogView = inflater.inflate(R.layout.dialog_month_year_picker, null);
        builder.setView(dialogView);

        android.widget.NumberPicker monthPicker = dialogView.findViewById(R.id.picker_month);
        android.widget.NumberPicker yearPicker = dialogView.findViewById(R.id.picker_year);

        String[] months = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
        monthPicker.setMinValue(0);
        monthPicker.setMaxValue(11);
        monthPicker.setDisplayedValues(months);
        monthPicker.setValue(calendarView.getCurrentDate().getMonth() - 1);

        int currentYear = LocalDate.now().getYear();
        yearPicker.setMinValue(currentYear - 10);
        yearPicker.setMaxValue(currentYear + 10);
        yearPicker.setValue(calendarView.getCurrentDate().getYear());

        builder.setTitle("Select Month and Year")
                .setPositiveButton("OK", (dialog, id) -> calendarView.setCurrentDate(CalendarDay.from(yearPicker.getValue(), monthPicker.getValue() + 1, 1)))
                .setNegativeButton("Cancel", (dialog, id) -> dialog.dismiss());
        builder.create().show();
    }

    private class CurrentMonthDecorator implements DayViewDecorator {
        private int month;

        CurrentMonthDecorator(int month) {
            this.month = month;
        }

        void setMonth(int month) {
            this.month = month;
        }

        @Override
        public boolean shouldDecorate(CalendarDay day) {
            return day.getMonth() == month;
        }

        @Override
        public void decorate(DayViewFacade view) {
            view.addSpan(new StyleSpan(Typeface.BOLD));
            view.addSpan(new RelativeSizeSpan(1.1f));
        }
    }

    private class OtherMonthDecorator implements DayViewDecorator {
        private int month;

        OtherMonthDecorator(int month) {
            this.month = month;
        }

        void setMonth(int month) {
            this.month = month;
        }

        @Override
        public boolean shouldDecorate(CalendarDay day) {
            return day.getMonth() != month;
        }

        @Override
        public void decorate(DayViewFacade view) {
            view.addSpan(new ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.text_secondary)));
            view.addSpan(new RelativeSizeSpan(0.9f));
        }
    }

    @Override
    public void onItemAction(int position, String action) {
        if ("PROMPT".equals(action)) {
            showActionDialog(position);
        }
    }

    private void showActionDialog(int position) {
        String[] options = {"Edit", "Delete"};
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
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
        NotificationHelper.cancelReminder(requireContext(), item);
        DatabaseHelper db = new DatabaseHelper(getContext());
        db.getWritableDatabase().delete(
                DatabaseHelper.TABLE_SCHEDULE,
                "id=?",
                new String[]{String.valueOf(item.id)}
        );
        list.remove(position);
        adapter.notifyItemRemoved(position);
        
        NotificationHelper.scheduleClassReminders(requireContext());
    }
}
