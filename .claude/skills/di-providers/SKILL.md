---
name: di-providers
description: Use when writing or reviewing code that would call LocalDate.now(), LocalDateTime.now(), Locale.getDefault(), or Dispatchers.* inside a DI class (ViewModel, repository, AppFunctions, etc.)
---

# Inject utility providers, never call static platform APIs

In any class that participates in Hilt DI, use the injected provider wrappers instead of static platform calls:

| Instead of | Use |
|---|---|
| `LocalDate.now()` / `LocalDateTime.now()` | `dateTimeProvider.nowLocalDateTime()` |
| `Locale.getDefault()` | `localeProvider.getLocale()` |
| `Dispatchers.IO` / `Dispatchers.Main` | `dispatcherProvider.io` / `dispatcherProvider.main` |

All three are `@Singleton`, provided in `ApplicationModule.kt`, and injected via `@Inject constructor`.

**Why:** testability. Providers can be mocked to control time, locale, and dispatchers; static calls make tests brittle and non-deterministic.

**How to apply:** whenever you write or review a direct static call to one of the above in a DI class, replace it with the injected provider.
