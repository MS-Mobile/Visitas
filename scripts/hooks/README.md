# Git Hooks Setup

This project uses custom Git hooks to enforce code quality checks.

## Automatic Setup

Run the following command once after cloning the repository:

```bash
git config core.hooksPath scripts/hooks
```

## What the hooks do

### pre-commit
- **Triggers when:** `VisitasDatabase.kt` is modified
- **Action:** Runs `BackupHandlerTest` to ensure backup/restore compatibility
- **Requires:** A connected Android device or emulator

## Bypassing hooks (not recommended)

If you need to bypass the hooks temporarily:

```bash
git commit --no-verify
```
