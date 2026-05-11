package com.example.studentautomaticscheduler;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class EditScheduleActivity extends AppCompatActivity {

    private List<ScheduleItem> items;
    private EditAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_schedule);

        items = (List<ScheduleItem>) getIntent().getSerializableExtra("SCHEDULE_ITEMS");
        if (items == null) items = new ArrayList<>();

        RecyclerView rv = findViewById(R.id.rvEditSchedule);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EditAdapter(items);
        rv.setAdapter(adapter);

        findViewById(R.id.btnSaveAll).setOnClickListener(v -> saveAll());
        findViewById(R.id.fabAddRow).setOnClickListener(v -> {
            items.add(new ScheduleItem("", "", "", "", "", "", "", "", ""));
            adapter.notifyItemInserted(items.size() - 1);
            rv.scrollToPosition(items.size() - 1);
        });
    }

    private void saveAll() {
        DatabaseHelper db = new DatabaseHelper(this);
        boolean singleEdit = getIntent().getBooleanExtra("SINGLE_EDIT_MODE", false);

        if (singleEdit) {
            for (ScheduleItem item : items) {
                if (item.id != -1) {
                    db.updateSchedule(item);
                } else {
                    db.insertSchedule(item);
                }
            }
        } else {
            db.getWritableDatabase().delete(DatabaseHelper.TABLE_SCHEDULE, null, null);
            for (ScheduleItem item : items) {
                if (!item.subject.isEmpty()) {
                    db.insertSchedule(item);
                }
            }
        }
        
        NotificationHelper.scheduleClassReminders(this);
        Toast.makeText(this, "Schedule saved successfully!", Toast.LENGTH_SHORT).show();
        finish();
    }

    class EditAdapter extends RecyclerView.Adapter<EditAdapter.ViewHolder> {
        private List<ScheduleItem> list;

        EditAdapter(List<ScheduleItem> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_edit_schedule, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ScheduleItem item = list.get(holder.getBindingAdapterPosition());

            holder.etSubjectCode.setText(item.subjectCode);
            holder.etDescription.setText(item.subject);
            holder.etDay.setText(item.day);
            holder.etTime.setText(item.time);
            holder.etSection.setText(item.section);
            holder.etRoom.setText(item.room);
            holder.etInstructor.setText(item.instructor);
            holder.etStatus.setText(item.status);
            holder.etUnits.setText(item.units);

            String[] modes = {"Face-to-Face", "Online", "Default"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(EditScheduleActivity.this, android.R.layout.simple_spinner_item, modes);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            holder.spinnerClassMode.setAdapter(adapter);

            if (item.classMode != null) {
                for (int i = 0; i < modes.length; i++) {
                    if (modes[i].equalsIgnoreCase(item.classMode)) {
                        holder.spinnerClassMode.setSelection(i);
                        break;
                    }
                }
            }

            holder.spinnerClassMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    item.classMode = modes[position];
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });

            setupTextWatcher(holder.etSubjectCode, pos -> list.get(pos).subjectCode = holder.etSubjectCode.getText().toString(), holder);
            setupTextWatcher(holder.etDescription, pos -> list.get(pos).subject = holder.etDescription.getText().toString(), holder);
            setupTextWatcher(holder.etDay, pos -> list.get(pos).day = holder.etDay.getText().toString(), holder);
            setupTextWatcher(holder.etTime, pos -> list.get(pos).time = holder.etTime.getText().toString(), holder);
            setupTextWatcher(holder.etSection, pos -> list.get(pos).section = holder.etSection.getText().toString(), holder);
            setupTextWatcher(holder.etRoom, pos -> list.get(pos).room = holder.etRoom.getText().toString(), holder);
            setupTextWatcher(holder.etInstructor, pos -> list.get(pos).instructor = holder.etInstructor.getText().toString(), holder);
            setupTextWatcher(holder.etStatus, pos -> list.get(pos).status = holder.etStatus.getText().toString(), holder);
            setupTextWatcher(holder.etUnits, pos -> list.get(pos).units = holder.etUnits.getText().toString(), holder);

            holder.btnRemove.setOnClickListener(v -> {
                int pos = holder.getBindingAdapterPosition();
                list.remove(pos);
                notifyItemRemoved(pos);
            });
        }

        private void setupTextWatcher(TextInputEditText et, OnTextChanged listener, ViewHolder holder) {
            et.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    int pos = holder.getBindingAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        listener.onChanged(pos);
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextInputEditText etSubjectCode, etDescription, etDay, etTime, etSection, etRoom, etInstructor, etStatus, etUnits;
            Spinner spinnerClassMode;
            ImageButton btnRemove;

            ViewHolder(View v) {
                super(v);
                etSubjectCode = v.findViewById(R.id.etSubjectCode);
                etDescription = v.findViewById(R.id.etDescription);
                etDay = v.findViewById(R.id.etDay);
                etTime = v.findViewById(R.id.etTime);
                etSection = v.findViewById(R.id.etSection);
                etRoom = v.findViewById(R.id.etRoom);
                etInstructor = v.findViewById(R.id.etInstructor);
                etStatus = v.findViewById(R.id.etStatus);
                etUnits = v.findViewById(R.id.etUnits);
                spinnerClassMode = v.findViewById(R.id.spinnerClassMode);
                btnRemove = v.findViewById(R.id.btnRemove);
            }
        }
    }

    interface OnTextChanged {
        void onChanged(int position);
    }
}
