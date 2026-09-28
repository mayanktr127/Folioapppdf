# Folio PDF — Feature Matrix & Implementation Status

| Feature ID | Workflow Description | Engine / Provider | Route | Configuration & Dependencies | Status | Test Fixture | Last Result |
|---|---|---|---|---|---|---|---|
| **F01** | Import, create & manage files | `PdfEngine` + Room DB | Local | Android Documents Storage | Implemented & Tested | `Service Agreement.pdf`, `Invoice 2026.pdf` | Pass |
| **F02** | Read, zoom, pan & page navigation | `android.graphics.pdf.PdfRenderer` | Local | Hardware accelerated bitmap canvas | Implemented & Tested | Multi-page contracts | Pass |
| **F03** | Text addition & overlay text editing | Native Canvas + `Paint` | Local | Honest overlay disclosure mode | Implemented & Tested | Custom text note & overlay | Pass |
| **F04** | Annotations (Ink, Highlight, Shapes, Underline) | `PdfEngine.saveAnnotatedPdf` | Local | Native Vector / Bitmap serialization | Implemented & Tested | Freehand strokes & yellow highlights | Pass |
| **F05** | Page organizer (Reorder, duplicate, delete, rotate) | `PdfEngine.rearrangePages` | Local | Native `PdfDocument` page builder | Implemented & Tested | 3-page reordered document | Pass |
| **F06** | File conversions (Images to PDF, Text note to PDF) | `PdfEngine.imagesToPdf`, `textToPdf` | Local | Photo Picker & Canvas typography | Implemented & Tested | Multi-image synthesis | Pass |
| **F07** | Multipage document scanner | Camera / Media + B&W filters | Local | `ColorMatrix` contrast enhancement | Implemented & Tested | Scanned receipt/photo batch | Pass |
| **F08** | Optimize & compress PDF | `PdfEngine.compressPdf` | Local | Resampled JPEG quality presets | Implemented & Tested | Standard, Minimal, Maximum presets | Pass |
| **F09** | Electronic signatures & initials | `SignatureDialog` canvas | Local | Reusable signature library in Room | Implemented & Tested | Custom cursive signature stamp | Pass |
| **F10** | Redaction & hidden info sanitization | Solid Blackout `REDACTION` | Local | Sanitized canvas rendering | Implemented & Tested | Redacted sensitive box | Pass |
| **F11** | Watermarks & Bates numbering | Rotated canvas text | Local | Transparent diagonal stamp | Implemented & Tested | `CONFIDENTIAL` watermark | Pass |
| **F12** | Save a copy, Download, Share, Print | `PdfEngine.exportToDownloads` + FileProvider | Local | Dedicated `SaveResultDialog` + Parse validation | Implemented & Tested | `{originalName}-edited.pdf` | Pass |
| **F13** | Team & multi-document workspaces | Room DB | Local | Single-writer conflict detection | Implemented & Tested | Local sandbox storage | Pass |
| **F14** | AI Document Assistant | Gemini API / Grounded citations | Local/Cloud | Secrets panel injected API key | Implemented & Tested | Contract Q&A & executive summary | Pass |
| **PUSH** | Push Notifications (PDF Ready, Download, Jobs, Draft) | `NotificationHelper` (`POST_NOTIFICATIONS`) | Local OS | Android NotificationChannel | Implemented & Tested | Test notification & PDF ready alert | Pass |
