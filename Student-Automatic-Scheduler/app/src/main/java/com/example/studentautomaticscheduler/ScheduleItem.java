package com.example.studentautomaticscheduler;

public class ScheduleItem {
    public String day;
    public String time;
    public String subject;
    public String section;
    public String room;
    public String instructor;

    public ScheduleItem(String day, String time, String subject, String section, String room, String instructor) {
        this.day = day;
        this.time = time;
        this.subject = subject;
        this.section = section;
        this.room = room;
        this.instructor = instructor;
    }
}
