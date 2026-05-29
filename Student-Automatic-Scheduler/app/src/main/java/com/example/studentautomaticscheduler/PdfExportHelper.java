package com.example.studentautomaticscheduler;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;

import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.font.PDFont;
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font;
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject;
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;

import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PdfExportHelper {

    private static final String[] TIME_SLOTS = {
            "06:00AM - 07:00AM", "07:00AM - 08:00AM", "08:00AM - 09:00AM",
            "09:00AM - 10:00AM", "10:00AM - 11:00AM", "11:00AM - 12:00PM",
            "12:00PM - 01:00PM", "01:00PM - 02:00PM", "02:00PM - 03:00PM",
            "03:00PM - 04:00PM", "04:00PM - 05:00PM", "05:00PM - 06:00PM",
            "06:00PM - 07:00PM", "07:00PM - 08:00PM", "08:00PM - 09:00PM"
    };

    private static final String[] DAYS = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
    private static final int SLOTS_PER_PAGE = 9;

    public static boolean exportToPdf(Context context, Uri uri, List<ScheduleItem> items) {
        try (PDDocument document = new PDDocument();
             OutputStream os = context.getContentResolver().openOutputStream(uri)) {

            PDRectangle landscape = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
            
            Map<String, Map<String, ScheduleItem>> tableData = mapItemsToGrid(items);

            PDPage page = new PDPage(landscape);
            document.addPage(page);
            PDPageContentStream content = new PDPageContentStream(document, page);

            drawPageContent(document, page, content, context, tableData, landscape, true);

            content.close();
            document.save(os);
            return true;
        } catch (Exception e) {
            Log.e("PdfExport", "Error generating PDF", e);
            return false;
        }
    }

    private static void drawPageContent(PDDocument document, PDPage page, PDPageContentStream content, Context context, 
                                      Map<String, Map<String, ScheduleItem>> tableData, PDRectangle landscape, boolean firstPage) throws Exception {
        
        float pageWidth = landscape.getWidth();
        float pageHeight = landscape.getHeight();
        float margin = 30;
        float startY = pageHeight - margin;
        float startX = margin;

        PDFont fontBold = PDType1Font.HELVETICA_BOLD;
        PDFont fontReg = PDType1Font.HELVETICA;
        PDFont fontItalic = PDType1Font.HELVETICA_OBLIQUE;

        // 1. Draw Watermark First
        drawWatermark(document, page, content, context);

        // 2. Title
        if (firstPage) {
            content.beginText();
            content.setFont(fontBold, 18);
            content.setNonStrokingColor(0f, 0f, 0f);
            content.newLineAtOffset(startX, startY);
            content.showText("Weekly Class Schedule");
            content.endText();
            startY -= 35;
        }

        float tableWidth = pageWidth - (2 * margin);
        float timeColWidth = 90;
        float dayColWidth = (tableWidth - timeColWidth) / 7;
        float rowHeight = 52;

        drawHeaderRow(content, startX, startY, timeColWidth, dayColWidth, fontBold);
        startY -= 25;

        for (int i = 0; i < TIME_SLOTS.length; i++) {
            String slot = TIME_SLOTS[i];
            
            // Fix pagination logic
            if (firstPage && i >= SLOTS_PER_PAGE) break;
            if (!firstPage && i < SLOTS_PER_PAGE) continue;

            if (startY - rowHeight < margin) break;

            // Reverted Time label to light gray for better contrast
            drawCell(content, startX, startY, timeColWidth, rowHeight, slot, fontReg, 8, "#F5F5F5", false);

            Map<String, ScheduleItem> dayMap = tableData.get(slot);
            for (int j = 0; j < DAYS.length; j++) {
                float x = startX + timeColWidth + (j * dayColWidth);
                ScheduleItem item = (dayMap != null) ? dayMap.get(DAYS[j]) : null;

                if (item != null) {
                    drawScheduleCell(content, x, startY, dayColWidth, rowHeight, item, fontBold, fontReg, fontItalic);
                } else {
                    // Transparent cell for empty slots (null background)
                    drawCell(content, x, startY, dayColWidth, rowHeight, "", fontReg, 8, null, false);
                }
            }
            startY -= rowHeight;
        }

        if (firstPage && TIME_SLOTS.length > SLOTS_PER_PAGE) {
            PDPage nextPage = new PDPage(landscape);
            document.addPage(nextPage);
            PDPageContentStream nextContent = new PDPageContentStream(document, nextPage);
            drawPageContent(document, nextPage, nextContent, context, tableData, landscape, false);
            nextContent.close();
        }
    }

    private static void drawWatermark(PDDocument document, PDPage page, PDPageContentStream content, Context context) throws Exception {
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.nu_logo);
        if (bitmap == null) return;
        PDImageXObject pdImage = LosslessFactory.createFromImage(document, bitmap);
        
        float pageWidth = page.getMediaBox().getWidth();
        float pageHeight = page.getMediaBox().getHeight();
        
        float imgWidth = 350;
        float imgHeight = (imgWidth / pdImage.getWidth()) * pdImage.getHeight();
        
        float x = (pageWidth - imgWidth) / 2;
        float y = (pageHeight - imgHeight) / 2;
        
        PDExtendedGraphicsState graphicsState = new PDExtendedGraphicsState();
        graphicsState.setNonStrokingAlphaConstant(0.12f);
        content.setGraphicsStateParameters(graphicsState);
        content.drawImage(pdImage, x, y, imgWidth, imgHeight);
        
        PDExtendedGraphicsState resetState = new PDExtendedGraphicsState();
        resetState.setNonStrokingAlphaConstant(1.0f);
        content.setGraphicsStateParameters(resetState);
    }

    private static void drawHeaderRow(PDPageContentStream content, float x, float y, float timeW, float dayW, PDFont font) throws Exception {
        // Reverted Header Row to light gray for better contrast
        drawCell(content, x, y, timeW, 25, "Time", font, 10, "#F5F5F5", false);
        for (int i = 0; i < DAYS.length; i++) {
            drawCell(content, x + timeW + (i * dayW), y, dayW, 25, DAYS[i], font, 10, "#F5F5F5", false);
        }
    }

    private static void drawCell(PDPageContentStream content, float x, float y, float w, float h, String text, PDFont font, int size, String hexColor, boolean whiteText) throws Exception {
        if (hexColor != null) {
            int color = Color.parseColor(hexColor);
            content.setNonStrokingColor(Color.red(color) / 255f, Color.green(color) / 255f, Color.blue(color) / 255f);
            content.addRect(x, y - h, w, h);
            content.fill();
        }

        content.setStrokingColor(0.7f, 0.7f, 0.7f); // Light gray border
        content.setLineWidth(0.5f);
        content.addRect(x, y - h, w, h);
        content.stroke();

        if (text != null && !text.isEmpty()) {
            content.beginText();
            if (whiteText) content.setNonStrokingColor(1f, 1f, 1f);
            else content.setNonStrokingColor(0f, 0f, 0f);
            
            content.setFont(font, size);
            float textWidth = font.getStringWidth(text) / 1000 * size;
            content.newLineAtOffset(x + (w - textWidth) / 2.0f, y - (h / 2.0f) - (size / 2.0f) + 2);
            content.showText(text);
            content.endText();
        }
    }

    private static void drawScheduleCell(PDPageContentStream content, float x, float y, float w, float h, ScheduleItem item, PDFont bold, PDFont reg, PDFont italic) throws Exception {
        String bgColor = "#333333"; // Dark mode default
        if (item.classMode != null) {
            if (item.classMode.equalsIgnoreCase("Online")) bgColor = "#1565C0";
            else if (item.classMode.equalsIgnoreCase("Face-to-Face")) bgColor = "#2E7D32";
        }

        int color = Color.parseColor(bgColor);
        content.setNonStrokingColor(Color.red(color) / 255f, Color.green(color) / 255f, Color.blue(color) / 255f);
        content.addRect(x, y - h, w, h);
        content.fill();

        content.setStrokingColor(0.6f, 0.6f, 0.6f);
        content.setLineWidth(0.5f);
        content.addRect(x, y - h, w, h);
        content.stroke();

        float padding = 4;
        float currentY = y - 12;
        float fontSize = 7.5f;
        
        // White text for dark mode colors
        content.setNonStrokingColor(1f, 1f, 1f);

        content.setFont(bold, fontSize);
        List<String> subjectLines = wrapText(item.subject, bold, fontSize, w - (2 * padding));
        for (int i = 0; i < Math.min(subjectLines.size(), 3); i++) {
            drawText(content, x + padding, currentY, subjectLines.get(i));
            currentY -= (fontSize + 1.5f);
        }

        content.setFont(reg, fontSize - 1);
        drawText(content, x + padding, currentY, item.room + " | " + item.section);
        currentY -= (fontSize + 1);

        if (item.instructor != null && !item.instructor.isEmpty()) {
            content.setFont(italic, fontSize - 1);
            List<String> instructorLines = wrapText(item.instructor, italic, fontSize - 1, w - (2 * padding));
            for (int i = 0; i < Math.min(instructorLines.size(), 2); i++) {
                drawText(content, x + padding, currentY, instructorLines.get(i));
                currentY -= (fontSize);
            }
        }
    }

    private static void drawText(PDPageContentStream content, float x, float y, String text) throws Exception {
        content.beginText();
        content.newLineAtOffset(x, y);
        String clean = text != null ? text.replaceAll("[^\\x00-\\x7F]", "") : "";
        content.showText(clean);
        content.endText();
    }

    private static Map<String, Map<String, ScheduleItem>> mapItemsToGrid(List<ScheduleItem> items) {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mma", Locale.US);
        Map<String, Map<String, ScheduleItem>> tableData = new HashMap<>();
        for (String slot : TIME_SLOTS) {
            tableData.put(slot, new HashMap<>());
        }
        for (ScheduleItem item : items) {
            if (item.time == null || !item.time.contains("-")) continue;
            try {
                String[] times = item.time.split("-");
                long itemStart = sdf.parse(times[0].trim()).getTime();
                long itemEnd = sdf.parse(times[1].trim()).getTime();
                for (String slot : TIME_SLOTS) {
                    String[] slotTimes = slot.split("-");
                    long slotStart = sdf.parse(slotTimes[0].trim()).getTime();
                    long slotEnd = sdf.parse(slotTimes[1].trim()).getTime();
                    if (itemStart < slotEnd && slotStart < itemEnd) {
                        Map<String, ScheduleItem> dayMap = tableData.get(slot);
                        if (dayMap != null) dayMap.put(item.day, item);
                    }
                }
            } catch (ParseException e) {
                Log.e("PdfExport", "Error parsing time", e);
            }
        }
        return tableData;
    }

    private static List<String> wrapText(String text, PDFont font, float fontSize, float width) throws Exception {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) return lines;
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            String testLine = line.length() == 0 ? word : line + " " + word;
            if (font.getStringWidth(testLine) / 1000 * fontSize > width) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(testLine);
            }
        }
        lines.add(line.toString());
        return lines;
    }
}
