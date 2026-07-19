# Visit Subject Markdown Links — Design

Date: 2026-07-18
Branch: fsomini/conversation-with-url

## Overview

Render markdown-style links (`[text](url)`) in the visit subject field as clickable,
non-editable hyperlinks — the same behavior a markdown preview gives — while the stored
text remains untouched plain text.

## Requirements

- The subject field renders each `[text](url)` token as just its label `text`, styled as a
  hyperlink (primary color, underlined). The brackets and URL are hidden.
- The rendered link is atomic and non-editable:
  - Backspace anywhere inside the token deletes the entire `[text](url)` from the stored text.
  - The caret cannot rest inside the link.
- Tapping the link triggers `Context.launchUrl(url)` regardless of focus state.
- Visual transformation only: the ViewModel and database always see the raw markdown text.
  `VisitSubjectChanged` events keep sending plain text; no schema or ViewModel changes.
- Links get into the field only by the user typing/pasting the markdown syntax (or a
  selected conversation containing it). No auto-detection of plain URLs.

## Non-goals

- Auto-wrapping plain URLs into markdown syntax.
- Rendering links in the conversation dropdown items or elsewhere outside the subject field.
- Pixel-precise tap hit-testing (Approach B with `BasicTextField` + `DecorationBox` was
  rejected as too much surface for the benefit).

## Components

All new logic lives in `app/src/main/java/com/msmobile/visitas/visit/MarkdownLinkTransformation.kt`
plus one extension in `ContextExtension.kt`.

### 1. Parser

```kotlin
data class MarkdownLink(val range: IntRange, val label: String, val url: String)

fun parseMarkdownLinks(text: String): List<MarkdownLink>
```

- Regex: `\[([^\[\]]+)\]\(([^)\s]+)\)` — non-empty label without nested brackets,
  non-empty URL with no whitespace, URL ends at the first `)`.
- Malformed or partial syntax is left as plain text (markdown-preview semantics).
- Multiple links per subject supported.

### 2. `MarkdownLinkVisualTransformation`

A `VisualTransformation` that:

- Builds an `AnnotatedString` where each token is replaced by its label with
  `SpanStyle(color = <link color>, textDecoration = Underline)`. Link color is passed in
  at construction (from `MaterialTheme.colorScheme.primary` in the composable, remembered).
- Provides a piecewise `OffsetMapping` for a token stored at `[s, e)` displayed at `[ts, te)`:
  - `originalToTransformed`: offsets outside tokens map linearly; interior original offsets
    snap to `te` (caret never renders inside a link).
  - `transformedToOriginal`: `ts → s`, `te → e`; interior label offsets map to interior
    original offsets (`s + 1 + (t - ts)`). Interior results are transient — the edit
    interceptor immediately reacts to them (tap detection) and snaps the caret out.

### 3. Edit interceptor (pure function)

```kotlin
data class MarkdownLinkEditResult(val value: TextFieldValue, val clickedUrl: String?)

fun interceptMarkdownLinkEdit(previous: TextFieldValue, proposed: TextFieldValue): MarkdownLinkEditResult
```

Called first thing in the field's `onValueChange`; its result is what gets written to local
state and sent as `VisitSubjectChanged`.

- **Backspace expansion**: if `proposed.text` equals `previous.text` with exactly one
  character removed at index `i`, and `i` falls inside a token `[s, e)`, replace the edit
  with deletion of the whole token: text = `previous.text` minus `[s, e)`, caret at `s`.
- **Tap detection**: if the text is unchanged, the selection is collapsed, differs from the
  previous selection, and rests strictly inside a token (`s < caret < e`), return
  `clickedUrl = token.url` and snap the caret to `e`.
- **Everything else** (typing, pasting, range-selection deletes, caret moves outside
  tokens) passes through untouched. A range delete that clips half a token simply leaves
  broken syntax, which renders as plain text again.

### 4. `Context.launchUrl`

In `ContextExtension.kt`, following the existing pattern:

```kotlin
fun Context.launchUrl(url: String) {
    val uri = Uri.parse(url).let { if (it.scheme == null) Uri.parse("https://$url") else it }
    startActivitySafely(Intent(Intent.ACTION_VIEW, uri))
}
```

Scheme-less URLs get `https://` prefixed; `startActivitySafely` already swallows
`ActivityNotFoundException`.

### 5. Wiring in `VisitSubjectDropdownList` (`VisitDetailScreen.kt`)

- `visualTransformation = remember(linkColor) { MarkdownLinkVisualTransformation(linkColor) }`
  on the existing `TextField` — no field migration.
- `onValueChange` runs `interceptMarkdownLinkEdit(textFieldValueState, value)` first, then
  uses `result.value` for local state and the `VisitSubjectChanged` event, and calls
  `context.launchUrl(result.clickedUrl)` when non-null (`LocalContext.current`).

## Data flow

Stored/ViewModel text is always raw markdown. Transformation applies only at render time.
`TextFieldValue` offsets seen in `onValueChange` are in original (stored) text coordinates,
which is what the parser and interceptor operate on.

## Known trade-off

Moving the caret into a link via arrow keys (physical keyboard) is indistinguishable from a
tap and will also open the URL. Accepted; can be revisited with Approach B if it ever
matters.

## Testing

JUnit unit tests (no instrumentation needed — all logic is pure) under
`app/src/test/java/com/msmobile/visitas/visit/`:

- Parser: valid token, multiple tokens, malformed variants (`[text] (url)`, missing paren,
  empty label/url, whitespace in url), text without links.
- Offset mapping: identity outside tokens, boundary mapping, interior snapping in both
  directions, multiple tokens, monotonicity.
- Interceptor: backspace at token end / inside token expands to full-token delete; normal
  typing passes through; tap inside token returns URL and snaps caret; caret move outside
  tokens passes through; range delete clipping a token passes through.

Manual verification: type `[Maps](https://maps.google.com)` in the subject field — it
renders as an underlined "Maps", backspace removes it entirely, tapping it opens the
browser, and the visit record stores the raw markdown string.
