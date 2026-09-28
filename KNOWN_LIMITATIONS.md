# Folio PDF — Known Limitations & Architecture Constraints

**Date:** September 28, 2026

## 1. Native Text Reflow vs. Overlay Editing
- The native Android framework (`PdfRenderer` / `PdfDocument`) does not provide arbitrary font subsetting, glyph replacement, or paragraph reflow inside existing vector streams without a proprietary third-party commercial SDK license (such as Apryse or Nutrient).
- As specified, Folio PDF provides honest labeling: **"Add Text Box"** introduces crisp new vector text elements, whereas modifying existing text is clearly designated as **"Overlay Edit"** (applying an opaque white cover and drawing replacement text).

## 2. Platform Sandbox & Downloads
- When running inside web previews or restricted emulators, direct filesystem downloads to Android `/Download` may route through app-internal scoped storage.
- Sharing via **Share (FileProvider)** and **Print Document** bypass scoped storage restrictions and open directly in system print services or external viewer apps.

## 3. Push Notifications Permission
- On Android 13+ (API 33+), system notifications require the user to explicitly grant runtime permission via the system prompt (`POST_NOTIFICATIONS`). If denied, notifications are suppressed by the OS, and in-app feedback banners are displayed instead.
