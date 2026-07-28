---
name: appfunctions
description: Use when adding or modifying Android AppFunctions (Gemini on-device integration) in Visitas, or verifying them via ADB
---

# AppFunctions (Gemini integration)

Visitas exposes functionality to Gemini as on-device tools via `androidx.appfunctions` (alpha).

## Adding a function

Add a new `@AppFunction` **suspend** method to `VisitAppFunctions`. No other wiring needed — KSP generates the schema, and `VisitasApp` (implements `AppFunctionConfiguration.Provider`) injects `VisitAppFunctions` via Hilt.

## Key files

- `app/src/main/java/com/msmobile/visitas/appfunctions/VisitAppFunctions.kt` — all `@AppFunction` methods; add new ones here.
- `app/src/main/java/com/msmobile/visitas/VisitasApp.kt` — `AppFunctionConfiguration.Provider`, Hilt injection.
- `AndroidManifest.xml` — **no manual entries needed**; the `appfunctions-service` library auto-injects service declarations + `<meta-data>` (pointing at generated `assets/app_functions.xml`) via manifest merging. **Do NOT** add a `<property ... @xml/app_metadata>` — that resource doesn't exist in this alpha and breaks resource linking.

## Parameter validation (verified)

- Gemini passes parameter **values in the documented vocabulary (English tokens from the KDoc), not the user's spoken language.** A Portuguese prompt still yields e.g. `dateFilter="today"`.
- Free-form `String` params are not enforced, so harden the impl: match `null` explicitly (intended default), match known tokens, and `else -> throw AppFunctionInvalidArgumentException(...)`. **Never use a silent `else` fallback** — it returns plausible-but-wrong data; throwing makes Gemini retry with a documented value.
- Kotlin `enum class` as a **parameter** type was not confirmed supported in this alpha (primitives, `@AppFunctionSerializable` data classes, and `List<T>` are). Validate in code rather than relying on an enum param type.

Current function: `listVisits(dateFilter: String?)` — `"today"`/`"tomorrow"`/`"past_due"`/`"done"`/`null` (all upcoming); unrecognized non-null → throws.

## Status: EAP-gated

The app is fully wired and verified end-to-end on a Galaxy S23 (Android 16 / SDK 36) through the OS `AppFunctionManagerService`. But the real **Gemini agent path is EAP-gated**: the OS only indexes AppFunctions from a pre-approved set of packages (Samsung/Google first-party). This is enforced at the OS registry-indexing level — **not** about debug vs release, signing, or code correctness. Unblocking requires Google Early Access Program enrollment, not code changes (EAP form submitted for `com.msmobile.visitas`). Re-verify this gate if/when EAP access lands.

## Verify via ADB (needs Android 16 device)

adb is at `~/AppData/Local/Android/Sdk/platform-tools/adb.exe` (not on PATH). The debug build is `com.msmobile.visitas.debug` with its **own empty database** (returns count:0 until visits are added in that installed app). Two package IDs = two separate OS registries.

```bash
# List registration
adb shell cmd app_function list-app-functions | grep -A30 visitas

# Execute (closest thing to Gemini; wrap whole cmd in double quotes to keep JSON quotes)
adb shell "cmd app_function execute-app-function --package com.msmobile.visitas.debug --function 'com.msmobile.visitas.appfunctions.VisitAppFunctions#listVisits' --parameters '{\"dateFilter\":\"today\"}' --brief-yaml"
```

The ADB `execute-app-function` tool bypasses normal indexing with elevated shell permissions — it's a test path, not the real Gemini consumer path.

## Build gotcha

Full `:app:installDebug` needs `VERSION_CODE=1` env var. The PC has memory pressure — cap heap with `-Dorg.gradle.jvmargs="-Xmx3072m ..."` to avoid a daemon JVM crash.
