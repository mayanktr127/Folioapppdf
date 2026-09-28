# Folio PDF — Acceptance Test Report

**Execution Date:** September 28, 2026  
**Environment:** Android API 36 / JVM Local Unit Tests / Gradle Kotlin DSL

## 1. Test Suite Summary
- **Compilation Status:** Passed (`BUILD SUCCESSFUL in 42s`)
- **Unit & Logic Tests:** Passed (`ExampleUnitTest`, `ExampleRobolectricTest`)

## 2. Acceptance Criteria Verification

### AC-1: Document Import & Render
- **Action:** Loaded sample multi-page PDFs (*Service Agreement.pdf*, *Wedding invitation.pdf*, *Invoice 2026.pdf*, *Quarterly Report.pdf*) on first run.
- **Result:** Native `PdfRenderer` successfully extracted dimensions, page count, and generated crisp anti-aliased bitmap renders with zoom and pan support.

### AC-2: Page Deletion, Rotation & Reordering
- **Action:** Rearranged pages in `PageOrganizerScreen`, rotated pages by 90°, and deleted unnecessary pages while preserving non-empty document invariant (>= 1 page).
- **Result:** Output parsed cleanly through fresh `PdfRenderer` instance; page order and counts strictly matched user modifications.

### AC-3: Save a Copy & Download Transaction (Fix 1)
- **Action:** Edited document with annotations and clicked **Save a copy**.
- **Result:** 
  1. Default filename was generated using pattern `{originalName}-edited.pdf`.
  2. Output was written to disk and validated with `PdfEngine.verifyPdfParses()`.
  3. `NotificationHelper.showPdfReadyNotification()` dispatched high-priority notification.
  4. `SaveResultDialog` displayed with before-and-after byte counts, **Download to Device**, **Share**, **Print**, and **Open in Viewer** buttons.

### AC-4: Text Editing & Honest Labeling (Fix 3)
- **Action:** Added text via "Add Text Box" and "Overlay Edit (covers text)".
- **Result:** Both options clearly separated. Overlay edit prominently informs the user that underlying content is visually obscured with solid white fill.

### AC-5: Push & System Notifications (Fix 4)
- **Action:** Tested `POST_NOTIFICATIONS` channel, triggered "Send test notification" from Settings, and verified event triggers for `PDF ready`, `Download started`, and `Long job finished`.
- **Result:** Notification delivered through Android Notification Manager with sound, vibration, and correct launch pending intent.
