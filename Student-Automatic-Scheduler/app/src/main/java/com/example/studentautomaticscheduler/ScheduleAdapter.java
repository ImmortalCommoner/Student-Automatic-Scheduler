package com.example.studentautomaticscheduler;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ViewHolder> {

    private List<ScheduleItem> list;

    public ScheduleAdapter(List<ScheduleItem> list) {
        this.list = list;
    }

    public interface OnItemLongClick {
        void onDelete(int position);
    }

    private OnItemLongClick listener;


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_schedule, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        ScheduleItem item = list.get(position);

        holder.day.setText(item.day);
        holder.time.setText(item.time);
        holder.subject.setText(item.subject);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView day, time, subject;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            day = itemView.findViewById(R.id.txtDay);
            time = itemView.findViewById(R.id.txtTime);
            subject = itemView.findViewById(R.id.txtSubject);
        }
    }
}
