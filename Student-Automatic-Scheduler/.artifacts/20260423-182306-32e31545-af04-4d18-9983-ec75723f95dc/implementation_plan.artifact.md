# Implementation Plan - CSV Support and Graphical Data Editing

This plan introduces the ability to export/import schedule data as CSV and provides a graphical table-like interface for the user to adjust parsed data before it's saved. It also expands the database schema to capture all fields from the student schedule.

## User Review Required

> [!IMPORTANT]
> The "Edit before saving" feature will now use a graphical interface where each parsed class is shown as a card with editable fields. This is much more user-friendly than editing raw CSV text.

## Proposed Changes

### Database and Data Model

#### [DatabaseHelper.java](file:///C:/Users/alvin/AndroidStudioProjects/Student-Automatic-Scheduler/Student-Automatic-Scheduler/app/src/main/java/com/example/studentautomaticscheduler/DatabaseHelper.java)
- Increase `DB_VERSION` to 5.
- Add columns: `subject_code`, `status`, `units`.
- Update `onCreate` and `onUpgrade` to include new columns.
- Update `insertSchedule` and query methods to handle the new fields.

#### [ScheduleItem.java](file:///C:/Users/alvin/AndroidStudioProjects/Student-Automatic-Scheduler/Student-Automatic-Scheduler/app/src/main/java/com/example/studentautomaticscheduler/ScheduleItem.java)
- Add fields: `subjectCode`, `status`, `units`.
- Implement `Serializable` or `Parcelable` to allow passing items to the Edit Activity.
- Add helper methods for CSV conversion.

---

### Graphical Editor

#### New Activity: `EditScheduleActivity.java`
- Displays a `RecyclerView` of parsed items.
- Each item uses a new layout `item_edit_schedule.xml` with `EditText` fields.
- Features a "Save All" button that commits everything to SQLite.
- Features an "Add Row" button for manual entries.

#### New Layout: `item_edit_schedule.xml`
- A card-based layout containing input fields for all schedule attributes (Subject, Code, Day, Time, Room, etc.).

---

### Parsing and CSV Integration

#### [MainActivity.java](file:///C:/Users/alvin/AndroidStudioProjects/Student-Automatic-Scheduler/Student-Automatic-Scheduler/app/src/main/java/com/example/studentautomaticscheduler/MainActivity.java)
- **Updated Flow**: `processText` now creates a list of `ScheduleItem` objects and starts `EditScheduleActivity` instead of saving directly.
- **CSV Export**: Add a method to generate a CSV string from the database and save it to the device's storage.
- **CSV Import**: Add a parser for CSV files that feeds into the same `EditScheduleActivity` flow.

## Verification Plan

### Manual Verification
1. **Schema Upgrade**: Run the app and ensure the database upgrades without crashing.
2. **Graphical Edit Flow**:
    - Upload a PDF.
    - Verify `EditScheduleActivity` opens with the parsed data.
    - Change some values and click "Save All".
    - Verify the Main Activity reflects these changes.
3. **CSV Export/Import**:
    - Export a schedule.
    - Delete all data in Settings.
    - Import the CSV and verify it appears in the editor first, then the main view.
