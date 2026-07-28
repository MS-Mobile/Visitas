---
name: android-studio-push-auth
description: Use when Android Studio reports "Push failed: Authentication failed" even though CLI git push works fine
---

# Fix Android Studio "Push failed: Authentication failed"

Android Studio authenticates HTTPS git pushes with its **own stored GitHub OAuth token**, bypassing Git Credential Manager (`credential.helper=manager`). After an AS reinstall that token can go stale → "Push failed: Authentication failed", even though CLI `git push` works fine (the CLI uses GCM / the `git:https://github.com` Windows Credential Manager entry).

**Fix:** File → Settings → Version Control → Git → enable **"Use credential helper"**. This makes AS delegate to GCM — the same path the CLI uses.

Re-adding the GitHub account in AS settings does **not** fix push — that account only powers API/PR features, not push auth. (Repo is `MS-Mobile/Visitas`, pushed with a personal `fabriciosomini` token that has `repo` scope.)
