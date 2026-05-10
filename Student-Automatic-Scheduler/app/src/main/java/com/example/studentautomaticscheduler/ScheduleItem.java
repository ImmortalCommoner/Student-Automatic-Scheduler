package com.example.studentautomaticscheduler;

import java.io.Serializable;

public class ScheduleItem implements Serializable {
    public int id = -1; // Default for new items
    public String day;
    public String time;
    public String subject;
    public String subjectCode;
    public String section;
    public String room;
    public String instructor;
    public String status;
    public String units;

    public ScheduleItem(String day, String time, String subject, String subjectCode, String section, String room, String instructor, String status, String units) {
        this.day = day;
        this.time = time;
        this.subject = subject;
        this.subjectCode = subjectCode;
        this.section = section;
        this.room = room;
        this.instructor = instructor;
        this.status = status;
        this.units = units;
    }

    public ScheduleItem(int id, String day, String time, String subject, String subjectCode, String section, String room, String instructor, String status, String units) {
        this(day, time, subject, subjectCode, section, room, instructor, status, units);
        this.id = id;
    }

    public String toCsvRow() {
        return escapeCsv(subjectCode) + "," +
                escapeCsv(subject) + "," +
                escapeCsv(section) + "," +
                escapeCsv(day) + "," +
                escapeCsv(time) + "," +
                escapeCsv(room) + "," +
                escapeCsv(instructor) + "," +
                escapeCsv(status) + "," +
                escapeCsv(units);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    public static ScheduleItem fromCsvRow(String row) {
        String[] p = row.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
        if (p.length < 9) return null;
        
        for (int i = 0; i < p.length; i++) {
            p[i] = p[i].trim();
            if (p[i].startsWith("\"") && p[i].endsWith("\"")) {
                p[i] = p[i].substring(1, p[i].length() - 1).replace("\"\"", "\"");
            }
        }

        return new ScheduleItem(p[3], p[4], p[1], p[0], p[2], p[5], p[6], p[7], p[8]);
    }
}
