# ProspectAI — UI/UX Design Specification

## 1. Product Feel
ProspectAI should feel like a professional financial research workspace: clear, trustworthy, data-focused, and calm. Avoid a generic AI landing page filled with decorative gradients. Prioritize readable documents, source traceability, and clear analysis hierarchy.

## 2. Visual Direction
- **Style:** modern fintech / research SaaS.
- **Layout:** dashboard shell with persistent sidebar and focused content panels.
- **Density:** moderate; display financial information in compact, well-spaced sections.
- **Typography:** a highly readable sans-serif such as Inter or a system font stack.
- **Borders:** subtle neutral borders, restrained shadows, consistent corner radii.
- **Motion:** minimal and purposeful; use transitions for navigation, loading, and panel changes only.
- **Responsive:** desktop-first research workspace that remains usable on tablet and mobile.

## 3. Suggested Design Tokens
Use Tailwind utility classes and CSS variables so the palette can be changed centrally.

```css
:root {
  --background: 248 250 252;
  --surface: 255 255 255;
  --foreground: 15 23 42;
  --muted: 100 116 139;
  --border: 226 232 240;
  --primary: 37 99 235;
  --success: 22 163 74;
  --warning: 217 119 6;
  --danger: 220 38 38;
}
```

Suggested implementation:
- Page background: very light cool neutral.
- Cards: white surfaces with thin neutral borders.
- Primary actions: restrained blue.
- Success: green only for successful states.
- Warning: amber for processing or caution.
- Error: red for failures.
- Dark mode may be added later; do not block MVP on it.

## 4. Application Shell
### Sidebar
- ProspectAI logo and wordmark.
- Dashboard.
- My Documents.
- Recent Chats.
- RHP Analysis.
- Settings / Profile.
- Collapse to icon rail on smaller desktop widths.
- On mobile, use a drawer with accessible focus handling.

### Top Bar
- Current page title or breadcrumb.
- Selected document when relevant.
- User menu and sign-out action.
- Optional global search only if implemented.

## 5. Pages
### Landing Page
- Clear headline: “Understand every RHP with evidence-backed AI.”
- Short explanation of upload → ask → cited answer.
- Primary CTA: “Get Started.”
- Feature highlights: source citations, risk-factor exploration, financial summaries.
- Avoid promising investment returns or guaranteed analysis accuracy.

### Login / Register
- Minimal centered form.
- Labeled fields, password visibility toggle, validation feedback.
- Clear loading and authentication error states.
- Link between login and registration.
- Do not reveal whether an email is registered through overly specific errors unless the product's account-recovery policy allows it.

### Dashboard
- Welcome heading and “Upload RHP” primary action.
- Summary cards: total documents, ready documents, processing documents.
- Recent RHP list with company/document name, upload date, status, and actions.
- Empty state with a clear upload CTA.
- Do not show fabricated statistics or demo financial values as real user data.

### Upload Flow
- Drag-and-drop target plus browse button.
- Accepted format and max file size visible.
- Selected filename and file size.
- Uploading and processing states distinguished.
- Retry option on failure.
- Explain that processing may take time.
- Prevent duplicate submission while uploading.

### Document Analysis Workspace
Desktop layout:
- Left/primary content: document overview and analysis tabs.
- Right panel: RHP chat, or a switchable chat workspace.
- Header: company/document name, status, upload date, overflow menu.
- Tabs: Overview, Financials, Risk Factors, Promoters, Use of Proceeds, Chat.
- Show “not found in document” where evidence is absent.
- Provide citations near each claim.

### Chat
- Message thread with clear user/assistant distinction.
- Suggested prompts: “Summarize the business,” “Explain key risks,” “How will IPO proceeds be used?”, “Summarize recent financials.”
- Multiline input and send action.
- Streaming indicator only if streaming is actually implemented.
- Citation chips such as “Page 42”; clicking opens a source panel or PDF viewer at that page if supported.
- Show retrieved excerpt and document name.
- Include retry for transient failures.
- Do not show fabricated citations.

### Analysis Components
- Risk factors: severity only if a defined, transparent rubric exists; otherwise group by source section.
- Financials: use tables for period-based figures; always show currency, units, and fiscal year.
- Promoter details: show source-backed names and shareholding percentages.
- Use of proceeds: list purpose and amount/percentage only when present in the source.
- Litigation: distinguish company disclosures from AI summaries.
- Add “Source: RHP, page X” for every material claim.

## 6. Reusable Components
- `Button`
- `Input`
- `FormField`
- `Modal`
- `Toast`
- `Loader` / `Skeleton`
- `StatusBadge`
- `DocumentCard`
- `PdfUpload`
- `ProcessingStatus`
- `ChatMessage`
- `SourceCitation`
- `AnalysisCard`
- `FinancialTable`
- `EmptyState`
- `ErrorState`

Build components around accessible HTML semantics and consistent spacing.

## 7. Interaction Rules
- Use explicit loading state for all asynchronous actions.
- Disable controls when duplicate submission could cause problems.
- Confirm destructive document deletion.
- Show processing status and allow safe refresh/polling.
- Keep the selected document visible during chat.
- Make citations keyboard-accessible.
- Preserve chat history when navigating within the same conversation.
- Provide a useful empty state when no documents or messages exist.
- Never claim upload, deletion, or analysis succeeded until the API confirms success.

## 8. Accessibility and Responsive Behavior
- Use semantic landmarks and heading hierarchy.
- Label all form fields.
- Support keyboard navigation and visible focus rings.
- Maintain readable contrast and avoid color-only status communication.
- Respect reduced-motion preferences.
- On narrow screens, stack analysis panels and place chat in a dedicated view or drawer.
- Ensure tables can scroll horizontally without breaking the page.

## 9. Content and Trust
- Explain that answers are AI-generated and based on retrieved RHP content.
- Cite pages and show excerpts where available.
- Clearly indicate when data is unavailable or extraction may be incomplete.
- Show an informational disclaimer: ProspectAI supports research and does not provide guaranteed investment, legal, or financial advice.
- Never display generated numbers as verified facts without source evidence.
