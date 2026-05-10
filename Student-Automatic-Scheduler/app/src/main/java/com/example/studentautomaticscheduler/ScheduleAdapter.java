package com.example.studentautomaticscheduler;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ViewHolder> {

    private final List<ScheduleItem> list;
    private final OnItemActionListener listener;

    public interface OnItemActionListener {
        void onItemAction(int position, String action);
    }

    public ScheduleAdapter(List<ScheduleItem> list, OnItemActionListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_schedule, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScheduleItem item = list.get(holder.getBindingAdapterPosition());

        String subjectDisplay = (item.subjectCode != null && !item.subjectCode.isEmpty()) 
                ? "[" + item.subjectCode + "] " + item.subject 
                : item.subject;
        
        holder.day.setText(item.day);
        holder.time.setText(item.time);
        holder.subject.setText(subjectDisplay);
        holder.section.setText("Section: " + item.section);
        holder.room.setText("Room: " + item.room);
        holder.instructor.setText("Instructor: " + item.instructor);

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onItemAction(holder.getBindingAdapterPosition(), "PROMPT");
            }
            return true;
        });
        
        holder.itemView.setOnClickListener(v -> {
            // Optional: short click action
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView day, time, subject, section, room, instructor;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            day = itemView.findViewById(R.id.txtDay);
            time = itemView.findViewById(R.id.txtTime);
            subject = itemView.findViewById(R.id.txtSubject);
            section = itemView.findViewById(R.id.txtSection);
            room = itemView.findViewById(R.id.txtRoom);
            instructor = itemView.findViewById(R.id.txtInstructor);
        }
    }
}
