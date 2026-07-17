# Formatted Link Rendering for URL Conversation Responses — Design

**Date:** 2026-07-17
**Status:** Approved (pending spec review)

## Summary

When a householder's answer to a conversation question is a URL (e.g. a `jw.org` article
link), selecting that conversation from the subject dropdown currently inserts the raw
`question\nresponse` text into the visit subject. In-progress work on this branch
(uncommitted `VisitDetailViewModel.kt` changes) instead formats it as `(question)[url]` when
the response is a valid HTTP(S) address, but the URL-validity check is a `fooBar` stub and
the formatted text has no special rendering or edit protection yet.

This design:

1. Replaces the `fooBar` stub with a real, DI-injectable URL validator.
2. Renders `(question)[url]` spans in the subject `TextField` as styled links (colored,
   underlined) — visible in screenshot tests, not just at runtime.
3. Makes the `[url]` portion (brackets + URL) atomic: any edit that touches it removes the
   whole `(question)[url]` span. The `question` text between the parens stays freely
   editable.

## Current behavior (baseline)

- `VisitDetailViewModel.kt:702-742` (`conversationSelected`) — already builds
  `"(${conversation.question})[${conversation.response}]"` when
  `isValidHttpAddress(conversation.response)` is true, else falls back to
  `conversation.questionAndResponse`. `isValidHttpAddress` (`:1313-1315`) currently calls a
  non-existent `fooBar(address)`.
- `VisitDetailViewModel.kt:598-628` (`visitSubjectChanged`) — handles every keystroke in the
  subject field; takes the proposed new text/caret position from the UI and commits it to
  `VisitState.editable.subject` with no validation of formatted-link spans.
- `VisitDetailScreen.kt:936-1049` (`VisitSubjectDropdownList`) — a plain Material3 `TextField`
  bound to a locally-remembered `TextFieldValue`, resynced from `visit.subject` via
  `LaunchedEffect` whenever the ViewModel's value differs from what the user typed (already
  used for clear/suggestion-insert flows — the same mechanism will reflect atomic-delete
  corrections back into the field).
- `ConversationState` (`:1527-1540`) already carries the raw `response: String` (added in the
  in-progress diff) alongside `questionAndResponse`.
- `VisitDetailPreviewConfigProvider.kt:339` constructs a `ConversationState` without the new
  `response` field — currently a compile error against the in-progress `ConversationState`
  shape and must be fixed as part of this change.

## Design

### 1. `UrlValidator` (new)

`app/src/main/java/com/msmobile/visitas/util/UrlValidator.kt`:

```kotlin
class UrlValidator @Inject constructor() {
    fun isValid(value: String): Boolean {
        if (value.isBlank() || value.any(Char::isWhitespace)) return false
        val uri = runCatching { URI(value) }.getOrNull() ?: return false
        return uri.isAbsolute &&
            uri.scheme?.lowercase() in setOf("http", "https") &&
            !uri.host.isNullOrBlank()
    }
}
```

Pure `java.net.URI` parsing — no `android.util.Patterns`, so it runs in plain JVM unit tests
(this codebase's `VisitDetailViewModel` tests use Mockito only, no Robolectric). Mirrors the
existing `LatLongParser` shape: a no-arg `@Inject constructor()` class, injected into
`VisitDetailViewModel` and independently unit-testable.

`VisitDetailViewModel` gains a `urlValidator: UrlValidator` constructor parameter;
`isValidHttpAddress(address)` becomes `urlValidator.isValid(address)`.

### 2. `FormattedLink` utility (new)

`app/src/main/java/com/msmobile/visitas/util/FormattedLink.kt` — pure functions, no DI, used
by both the ViewModel (edit protection) and the Composable (styling), so link-detection logic
has exactly one implementation:

```kotlin
data class FormattedLinkSpan(
    val start: Int,        // index of '('
    val end: Int,          // exclusive, index after ']'
    val questionStart: Int, // index of first question char
    val questionEnd: Int,   // exclusive, index of ')'
    val question: String,
    val url: String
)

fun findFormattedLinks(text: String, isValidUrl: (String) -> Boolean): List<FormattedLinkSpan>

fun sanitizeFormattedLinkEdit(
    oldText: String,
    newText: String,
    proposedCaretPosition: Int,
    links: List<FormattedLinkSpan>
): Pair<String, Int>
```

**`findFormattedLinks`** matches `\(([^()\[\]\n]*)\)\[([^\[\]\n]*)]` against `text`, keeping
only matches whose captured URL passes `isValidUrl`. Kotlin's `MatchResult.groups[n]!!.range`
gives exact character offsets for `start`/`end`/`questionStart`/`questionEnd` directly — no
manual offset arithmetic. Excluding `\n` from both capture groups keeps a link scoped to a
single subject line, matching how links are only ever inserted per-line today.

**`sanitizeFormattedLinkEdit`** implements the atomicity rule. It diffs `oldText` against
`newText` using a common-prefix/common-suffix scan to find the changed region
`[removeStart, removeEnd)` in `oldText` coordinates (this single diff form covers insertion,
deletion, and replacement — for a pure insertion at position `p`, `removeStart == removeEnd
== p`). For each span, the edit collides if it overlaps the span at all but is **not** fully
contained in `[questionStart, questionEnd]`:

```kotlin
val overlapsSpan = removeStart < span.end && removeEnd > span.start
val fullyWithinQuestion = removeStart >= span.questionStart && removeEnd <= span.questionEnd
val collides = overlapsSpan && !fullyWithinQuestion
```

On the first colliding span, the function discards the proposed edit entirely and returns
`oldText` with that span's `[start, end)` range removed, caret placed at `span.start`. With no
collision, `(newText, proposedCaretPosition)` passes through unchanged. This single formula
covers every case from the approved UX:

| Action | Region touched | Result |
|---|---|---|
| Delete/insert inside `question` | fully within `[questionStart, questionEnd]` | passes through — question shrinks/grows normally |
| Delete `(` | `span.start` | whole span removed |
| Delete `)` | `questionEnd` (start of protected zone) | whole span removed |
| Delete/insert inside `[url]` | `(questionEnd, end)` | whole span removed |
| Edit entirely outside the span | no overlap | passes through, unaffected |

Empty question (`()[url]`) is allowed — it's just never specially collapsed by this function;
a user can keep deleting into the protected zone to remove the link entirely.

### 3. `VisitDetailViewModel` wiring

`visitSubjectChanged(value, visit, caretPosition)` computes links from the **old** subject
and sanitizes before committing:

```kotlin
private fun visitSubjectChanged(value: String, visit: VisitState, caretPosition: Int) {
    newState {
        val links = findFormattedLinks(visit.subject, urlValidator::isValid)
        val (sanitizedValue, sanitizedCaretPosition) =
            sanitizeFormattedLinkEdit(visit.subject, value, caretPosition, links)
        // ...existing logic continues using sanitizedValue / sanitizedCaretPosition
    }
}
```

No changes needed in `VisitDetailScreen`'s optimistic local `textFieldValueState` handling —
when the ViewModel's sanitized `visit.subject` differs from what the user actually typed, the
existing `LaunchedEffect(visit.subject)` resync (already used for clear/suggestion-insert)
overwrites the field with the corrected text and caret position. This is the same mechanism,
reused, not a new one.

Also fixes `VisitDetailPreviewConfigProvider.kt:339` — adds the missing `response` argument to
`previewConversationSuggestion` (empty string; it's not URL-backed).

### 4. Rendering (`VisitDetailScreen`)

`VisitSubjectDropdownList`'s subject `TextField` gets a `visualTransformation`:

```kotlin
private class FormattedLinkVisualTransformation(
    private val linkColor: Color,
    private val isValidUrl: (String) -> Boolean
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val links = findFormattedLinks(text.text, isValidUrl)
        if (links.isEmpty()) return TransformedText(text, OffsetMapping.Identity)
        val styled = buildAnnotatedString {
            append(text)
            links.forEach { span ->
                addStyle(
                    SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
                    span.start,
                    span.end
                )
            }
        }
        return TransformedText(styled, OffsetMapping.Identity)
    }
}
```

Applied as:

```kotlin
visualTransformation = remember {
    FormattedLinkVisualTransformation(linkColor, UrlValidator()::isValid)
}
```

`linkColor` is `MaterialTheme.colorScheme.primary`, resolved in the composable and passed in
(matches `EditableTextFieldColors`'s existing use of theme colors). `UrlValidator` has no
dependencies of its own, so direct instantiation here (rather than DI) is consistent with how
Composables in this codebase are state-hoisted and don't receive injected services directly.

Because the transform runs during composition — not gated behind focus or a runtime
interaction — it renders identically in `@PreviewTest` screenshot captures. Tap behavior is
unchanged (places the cursor, as approved); no click-to-open-URL handling is added.

Since the transform never changes text length (raw syntax stays visible, just styled),
`OffsetMapping.Identity` is correct and needs no custom offset logic.

### 5. Preview / screenshot coverage

Add a `"Formatted Link"` entry to `VisitDetailPreviewConfigProvider` with a visit subject
containing a `(question)[https://...]` span, so `VisitDetailScreenshotTest` exercises the
styled rendering.

## Testing

- **`UrlValidatorTest`** (new) — valid `http`/`https` URLs accepted; missing scheme, `ftp://`,
  blank, whitespace-containing, and malformed strings rejected.
- **`FormattedLinkTest`** (new) — `findFormattedLinks`: detects a valid-URL span, ignores an
  invalid-URL span, finds multiple spans, respects the single-line boundary. 
  `sanitizeFormattedLinkEdit`: all rows of the table above (question edit passes through,
  deleting `(`/`)`/inside-URL collapses, insertion inside URL collapses, no-link text always
  passes through, edit outside a span is unaffected).
- **`VisitDetailViewModelTest`** — extend with: `ConversationSelected` on a URL-response
  conversation inserts `(question)[url]` (already partially covered by existing diff logic,
  needs a URL-response fixture); `VisitSubjectChanged` end-to-end enforces atomicity through
  the real ViewModel using a real `UrlValidator()` instance (not mocked — matches how
  `VisitDataFormatter(LocaleProvider())` is used directly today rather than via Mockito).

## Files touched

- `app/src/main/java/com/msmobile/visitas/util/UrlValidator.kt` (new)
- `app/src/main/java/com/msmobile/visitas/util/FormattedLink.kt` (new)
- `app/src/main/java/com/msmobile/visitas/visit/VisitDetailViewModel.kt` — inject
  `UrlValidator`, fix `isValidHttpAddress`, sanitize in `visitSubjectChanged`.
- `app/src/main/java/com/msmobile/visitas/visit/VisitDetailScreen.kt` — `visualTransformation`
  on the subject `TextField`.
- `app/src/main/java/com/msmobile/visitas/visit/VisitDetailPreviewConfigProvider.kt` — fix
  missing `response` arg, add a link-rendering preview config.
- `app/src/test/java/com/msmobile/visitas/util/UrlValidatorTest.kt` (new)
- `app/src/test/java/com/msmobile/visitas/util/FormattedLinkTest.kt` (new)
- `app/src/test/java/com/msmobile/visitas/visit/VisitDetailViewModelTest.kt` — new cases,
  builder function gets a real `UrlValidator()`.

## Out of scope (YAGNI)

- Tap-to-open-URL from the subject field (visual styling only, per approved design).
- Markdown-style syntax hiding (raw `(question)[url]` stays visible, styled — not collapsed to
  just "question").
- Any change to how conversation responses are captured/stored (`Conversation.response`
  itself is untouched).
- Link support outside the visit subject field (e.g. notes field).
