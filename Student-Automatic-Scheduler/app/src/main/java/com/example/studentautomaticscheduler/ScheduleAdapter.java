package com.example.studentautomaticscheduler;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
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

        boolean isDarkMode = (holder.itemView.getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) 
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;

        String mode = item.classMode != null ? item.classMode : "Default";
        int bgColor;
        if (mode.equalsIgnoreCase("Online")) {
            bgColor = isDarkMode ? Color.parseColor("#1565C0") : Color.parseColor("#E3F2FD");
        } else if (mode.equalsIgnoreCase("Face-to-Face")) {
            bgColor = isDarkMode ? Color.parseColor("#2E7D32") : Color.parseColor("#E8F5E9");
        } else {
            bgColor = isDarkMode ? Color.parseColor("#333333") : Color.parseColor("#F5F5F5");
        }
        
        CardView card = holder.itemView.findViewById(R.id.cardMain);
        card.setCardBackgroundColor(bgColor);

        if (isDarkMode) {
            holder.subject.setTextColor(Color.WHITE);
            holder.section.setTextColor(Color.parseColor("#BBBBBB"));
            holder.day.setTextColor(Color.parseColor("#AAAAAA"));
            holder.time.setTextColor(Color.parseColor("#AAAAAA"));
            holder.room.setTextColor(Color.parseColor("#BBBBBB"));
            holder.instructor.setTextColor(Color.parseColor("#BBBBBB"));
        } else {
            holder.subject.setTextColor(Color.parseColor("#111111"));
            holder.section.setTextColor(Color.parseColor("#666666"));
            holder.day.setTextColor(Color.BLACK);
            holder.time.setTextColor(Color.BLACK);
            holder.room.setTextColor(Color.parseColor("#444444"));
            holder.instructor.setTextColor(Color.parseColor("#444444"));
        }

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onItemAction(holder.getBindingAdapterPosition(), "PROMPT");
            }
            return true;
        });
        
        holder.itemView.setOnClickListener(v -> {});
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
