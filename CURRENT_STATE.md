# Folio PDF — Current State Assessment

**Date:** September 28, 2026  
**Platform:** Native Android (Kotlin / Jetpack Compose / AGP 9.1 / MinSdk 24 / TargetSdk 36)

## 1. PDF Rendering & Serialization Architecture
- **Rendering Engine:** Android platform `android.graphics.pdf.PdfRenderer` via `ParcelFileDescriptor`. It renders high-resolution bitmaps (1080p–1440p) with hardware acceleration.
- **Serialization Engine:** Android platform `android.graphics.pdf.PdfDocument` via `com.example.engine.PdfEngine`.
- **Page Ordering & Deletion:** Performed in `PdfEngine.rearrangePages()`. It takes a `0-indexed` list of page indices (`pageOrder: List<Int>`), renders each specified page, and streams it into a fresh `PdfDocument` file.
- **State Storage:** 
  - Document metadata, recents, favorites, and saved signatures are stored in Room SQLite database (`FolioDatabase`).
  - Active editor canvas state, annotations, undo/redo stacks, and page indices are managed in `PdfViewModel`.
  - Serialized files are saved to private app storage (`context.filesDir/documents/`) and exported via `FileProvider` (`Intent.ACTION_SEND`), `PrintManager`, and `MediaStore` / `Download` directories.

## 2. Gaps Identified & Improvement Plan
- **Save Flow:** Currently saves with a basic dialog; needs the dedicated **Result & Download Screen** featuring `{originalName}-edited.pdf` default filename formatting, before/after byte inspection, parse verification reload check, and direct download/export options.
- **Text Editing Distinction:** UI needs clear, honest labeling between "Add Text" (vector text element) and "Overlay Text Edit" (covers underlying text), with no false claims of reverse-engineering rasterized or subset-font glyph streams.
- **Notifications Pipeline:** Android 13+ (API 33+) requires `POST_NOTIFICATIONS` permission with a dedicated `NotificationChannel` ("folio_pdf_notifications") and real notification triggers for:
  1. `PDF ready` (after serialization and parse validation)
  2. `Download started` (when exported to device Downloads folder)
  3. `Long job finished` (compression or merge completion)
  4. `Draft recovered` (restoring unsaved annotations on app resume)
- **Draft Recovery:** Uncommitted annotations and active document ID should be preserved in Room or DataStore so drafts survive app termination.
