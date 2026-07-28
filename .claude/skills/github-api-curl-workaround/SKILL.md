---
name: github-api-curl-workaround
description: Use when gh / api.github.com times out from this machine, or when sending a GitHub API request body containing non-ASCII (emoji) via curl on Windows
---

# GitHub API from this machine

`gh` and `api.github.com` work normally here. A one-time TCP block was a temporary outage, not a firewall rule — use `gh` normally.

## Non-ASCII request bodies (the durable gotcha)

When POST/PATCH bodies contain non-ASCII (e.g. emoji from the PR template), **never pass JSON inline** with `curl -d '{...}'` — the Windows shell→curl hop mangles UTF-8 into `?`. Write the JSON to a UTF-8 file (with the Write tool) and send `-d @file.json`.

## Outage fallback (only if gh times out again)

Pin the REST API to an older GitHub IP:

```powershell
$token = gh auth token
curl.exe -s --resolve "api.github.com:443:140.82.121.6" -H "Authorization: Bearer $token" "https://api.github.com/..."
```
