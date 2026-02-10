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
    private final OnItemLongClick listener;

    public interface OnItemLongClick {
        void onDelete(int position);
    }

    public ScheduleAdapter(List<ScheduleItem> list, OnItemLongClick listener) {
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

        holder.day.setText(item.day);
        holder.time.setText(item.time);
        holder.subject.setText(item.subject);
        holder.section.setText(item.section);
        holder.room.setText(item.room);
        holder.instructor.setText(item.instructor);

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onDelete(holder.getBindingAdapterPosition());
            }
            return true;
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
