# Kitsune Capture Backend

This folder contains an AWS SAM project that exposes the image-analysis endpoint expected by the Android app:

- Method: `POST`
- Path: `/analyze-japanese`
- Request body: `{"image":"<base64>","mimeType":"image/jpeg"}`
- Response body: `{"terms":[{"kanji":"...","reading":"...","meaning":"...","notes":"..."}]}`

The Lambda uses Gemini `gemini-2.5-flash`, so the API key stays server-side.

## Security model

This project now applies layered abuse controls:

- AWS WAF IP rate limit in front of API Gateway (5-minute rolling window)
- Lambda-side per-device minute and per-device daily quotas
- Lambda-side per-IP minute quota (hashed before persistence)
- DynamoDB-backed usage counters with TTL cleanup
- DynamoDB-backed temporary blacklist for abusive device fingerprints
- Device profile tracking for review and manual enforcement

Device identification:

- Preferred: send `x-device-id` header from the app (stable random install ID)
- Fallback (already supported): server computes a fingerprint from source IP + user-agent hash

The endpoint still accepts the same JSON body your app already sends.

## Files

- `template.yaml`: SAM infrastructure definition
- `src/app.js`: Lambda handler
- `package.json`: Node dependencies
- `samconfig.example.toml`: deploy config template

## Prerequisites

- AWS CLI configured for your account
- SAM CLI installed
- Node.js 20+

## First-time setup

From this folder:

```powershell
npm install
sam build
sam deploy --guided
```

If you want SAM to reuse saved deploy settings, copy `samconfig.example.toml` to `samconfig.toml` and replace the placeholder values first.

Recommended answers during `sam deploy --guided`:

- Stack Name: `kitsune-capture-backend`
- AWS Region: pick the region you want to host in
- Parameter `GeminiApiKey`: your Gemini API key
- Parameter `AllowedOrigin`: `*` for Android or your exact origin if you later add a web client
- Parameter `DevicePerMinuteLimit`: default `15`
- Parameter `DevicePerDayLimit`: default `300`
- Parameter `IpPerMinuteLimit`: default `60`
- Parameter `DeviceHardBlockThresholdPerMinute`: default `120` (auto-blacklist for 24h)
- Parameter `WafIpRateLimit5Min`: default `2000`
- Parameter `EnableWafAssociation`: default `false` (set `true` only after validating WAF stage ARN support in your API setup)
- Confirm changes before deploy: `Y`
- Allow SAM CLI IAM role creation: `Y`
- Save arguments to configuration file: `Y`

If you prefer a non-interactive deploy after creating `samconfig.toml`:

```powershell
sam build
sam deploy --parameter-overrides GeminiApiKey="YOUR_KEY" AllowedOrigin="*" DevicePerMinuteLimit="15" DevicePerDayLimit="300" IpPerMinuteLimit="60" DeviceHardBlockThresholdPerMinute="120" WafIpRateLimit5Min="2000" EnableWafAssociation="false"
```

## Endpoint URL

After deploy, SAM prints an output named `AnalyzeJapaneseUrl`. Use that exact value as `CAPTURE_BACKEND_URL`.

Example:

```text
https://abc123.execute-api.us-east-1.amazonaws.com/analyze-japanese
```

## Android wiring

Update [`app/build.gradle.kts`](C:/Users/tanji/AndroidStudioProjects/Kitsune/app/build.gradle.kts) and replace the empty backend URL:

```kotlin
buildConfigField("String", "CAPTURE_BACKEND_URL", "\"https://abc123.execute-api.us-east-1.amazonaws.com/analyze-japanese\"")
```

Then rebuild the app.

## Optional app hardening

For better per-device enforcement, add a stable random install ID to each request as:

- Header: `x-device-id: <uuid>`

Use a generated UUID stored in DataStore (do not send raw hardware IDs).

## Manual blacklist management

The stack creates a DynamoDB table named `<stack>-DeviceBlacklistTable-*`.

Temporary block example (24h):

```powershell
aws dynamodb put-item `
  --table-name YOUR_BLACKLIST_TABLE `
  --item '{"deviceId":{"S":"your-device-id"},"reason":{"S":"manual_block"},"createdAt":{"N":"1741267200"},"expiresAt":{"N":"1741353600"}}'
```

Unblock immediately:

```powershell
aws dynamodb delete-item `
  --table-name YOUR_BLACKLIST_TABLE `
  --key '{"deviceId":{"S":"your-device-id"}}'
```

## Local invoke

You can smoke-test the function locally after `sam build`:

```powershell
sam local invoke AnalyzeJapaneseFunction --event events/sample-request.json --env-vars env.example.json
```

Replace `REPLACE_WITH_BASE64_IMAGE` in `events/sample-request.json` with a real base64 image payload before running it.

`env.example.json` sets `DISABLE_ABUSE_PROTECTION=true` for local runs only. Do not set this in deployed environments.
