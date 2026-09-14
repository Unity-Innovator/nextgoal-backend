# Frontend authentication API

This is the source of truth for the mobile/frontend authentication flow. The
API is phone-number based; email is neither collected nor used for sign-in.

Set `baseUrl` to the deployed backend origin, without a trailing slash. Send
JSON requests with `Content-Type: application/json`.

## Response format

Successful requests use this envelope:

```json
{
  "success": true,
  "message": "...",
  "data": {}
}
```

Failures use:

```json
{
  "message": "Human-readable reason",
  "status": 400,
  "timestamp": "2026-09-11T10:30:00"
}
```

Treat the HTTP status as authoritative. Do not rely on the exact error message
for application logic.

## Token handling

Every authenticated REST request needs:

```http
Authorization: Bearer <accessToken>
```

Keep both tokens in platform secure storage, never in logs, analytics, URL
parameters, or normal preferences.

| Token | Purpose | Default lifetime |
|---|---|---:|
| `accessToken` | Authenticated API and WebSocket requests | 15 minutes |
| `refreshToken` | Obtain a new access-token pair | 7 days |
| `stepUpToken` | Sensitive financial operation after PIN confirmation | 5 minutes |

The server returns `expiresIn` in seconds for access and step-up tokens. Use
the returned value instead of hard-coding lifetimes. A refresh response rotates
both access and refresh tokens: atomically replace both stored values.

There is no server logout endpoint. Logout means clearing the locally stored
access token, refresh token, and any step-up token.

## Shared types

```ts
type Gender = "MALE" | "FEMALE" | "OTHER";
type OtpPurpose = "REGISTRATION" | "PASSWORD_RESET" | "LOGIN";

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

interface User {
  id: string;
  fullName: string;
  phoneNumber: string;
  gender: Gender;
  dateOfBirth: string; // YYYY-MM-DD
  status: string;
}

interface TokenSession {
  accessToken: string;
  refreshToken: string;
  tokenType: "Bearer";
  expiresIn: number;
  user: User;
}
```

Use exactly the same phone-number representation through OTP send, OTP verify,
registration, password login, and password reset. Phone fields accept 8–15
digits with an optional leading `+` for OTP endpoints.

## 1. Check whether a phone number exists

```http
GET /api/auth/check?phoneNumber=012345678
```

No token is required. A known user returns `200` with `data: true`; an unknown
phone number returns `404`. Use this only to choose an onboarding screen, not
as a security decision.

## 2. Registration with SMS OTP

Registration has three mandatory calls.

### 2.1 Send registration OTP

```http
POST /api/auth/otp/send
```

```json
{
  "phoneNumber": "012345678",
  "purpose": "REGISTRATION"
}
```

The response has `data: null`; it never includes the OTP. The code is six
digits and expires after five minutes. The resend cooldown is 60 seconds.

### 2.2 Verify registration OTP

```http
POST /api/auth/otp/verify
```

```json
{
  "phoneNumber": "012345678",
  "otp": "583921",
  "purpose": "REGISTRATION"
}
```

On success, retain only `data.verificationToken` temporarily. It is single-use,
bound to this phone number and purpose, and expires after ten minutes.

```json
{
  "success": true,
  "message": "OTP verified successfully",
  "data": {
    "verificationToken": "..."
  }
}
```

### 2.3 Create the account

```http
POST /api/auth/register
```

```json
{
  "firstName": "Dara",
  "lastName": "Sok",
  "phoneNumber": "012345678",
  "password": "Password1",
  "confirmPassword": "Password1",
  "gender": "OTHER",
  "dateOfBirth": "2000-01-15",
  "pin": "123456",
  "verificationToken": "<token from OTP verify>"
}
```

`password` must contain at least eight characters including uppercase,
lowercase, and a number; both password fields are limited to 72 characters.
`pin` is exactly six digits during registration. A successful response contains
a complete `TokenSession`; persist it and remove the temporary verification
token.

## 3. Password login

```http
POST /api/auth/login
```

```json
{
  "phoneNumber": "012345678",
  "password": "Password1"
}
```

The response is a `TokenSession`. Incorrect credentials return `401` with an
authentication error. Do not disclose whether the phone number or password was
incorrect in the UI.

## 4. Passwordless SMS login

For an existing active account, use the `LOGIN` OTP purpose.

```http
POST /api/auth/otp/send
```

```json
{
  "phoneNumber": "012345678",
  "purpose": "LOGIN"
}
```

Then verify:

```http
POST /api/auth/otp/verify
```

```json
{
  "phoneNumber": "012345678",
  "otp": "583921",
  "purpose": "LOGIN"
}
```

The successful verification response is a `TokenSession`—there is no
registration token in this flow. Persist the session as for password login.

## 5. Forgot-password reset

This flow is also three calls. It is for an existing active account only.

1. Send an OTP with `purpose: "PASSWORD_RESET"`.
2. Verify the OTP with the same phone number and purpose. Save
   `data.verificationToken` temporarily.
3. Reset the password:

```http
POST /api/auth/reset-password
```

```json
{
  "resetToken": "<token from OTP verify>",
  "newPassword": "NewPassword1"
}
```

The reset token is consumed after success. The reset endpoint returns
`data: null` and does not issue a session; send the user to password login or
passwordless login afterwards. `newPassword` is required and limited to 72
characters. The frontend should apply the same strong-password UI rule as
registration even though the reset endpoint currently only enforces the length.

## 6. Refresh an expired access token

```http
POST /api/auth/refresh
```

```json
{
  "refreshToken": "<stored refresh token>"
}
```

On `200`, save the returned complete `TokenSession` before retrying the failed
request. If refresh fails, clear local session state and return to login.

Recommended client behavior:

1. Attach the access token to protected requests.
2. On one `401` or authentication failure, run exactly one shared refresh
   request for all concurrent requests.
3. Retry each original request once with the new access token.
4. If refresh or the retry fails, clear session and show login.

Do not use the refresh token as a Bearer token.

## 7. PIN step-up for sensitive operations

`POST /api/auth/verify-pin` is authenticated. First send the regular access
token, then send the six-digit PIN:

```http
POST /api/auth/verify-pin
Authorization: Bearer <accessToken>
```

```json
{
  "pin": "123456"
}
```

Success returns:

```json
{
  "success": true,
  "data": {
    "stepUpToken": "...",
    "expiresIn": 300,
    "tokenType": "Step-Up"
  }
}
```

For endpoints marked as sensitive (currently the wallet operation guarded by
step-up), send both headers:

```http
Authorization: Bearer <accessToken>
X-Step-Up-Token: <stepUpToken>
```

The token is bound to the authenticated user and financial-operation scope. It
must not be placed after `Bearer` in the step-up header. Five failed PIN checks
lock PIN verification for 15 minutes; show the server error and stop retrying.

## OTP handling and errors

- OTPs are six digits, expire after five minutes, and are consumed once.
- The server permits five failed attempts. A sixth verification attempt requires
  a new OTP.
- A resend replaces the previous OTP after the 60-second cooldown.
- A wrong `purpose` counts as an incorrect OTP attempt.
- For compatibility only, omitting `purpose` in **send** selects
  `REGISTRATION` for a new/pending account and `PASSWORD_RESET` for an existing
  account. Always send an explicit purpose in new frontend code.
- `purpose` is required for **verify**.

## WebSocket authentication

For STOMP chat, authenticate the `CONNECT` frame with the current access token:

```ts
connectHeaders: {
  Authorization: `Bearer ${accessToken}`,
}
```

Putting the token only in the WebSocket URL does not authenticate the session.
Reconnect with a refreshed access token after an access-token refresh.

## Implementation checklist

- Store tokens using platform secure storage.
- Keep one refresh operation in flight at a time.
- Clear temporary OTP verification/reset tokens immediately after use or when
  leaving the flow.
- Respect the OTP resend timer; do not continuously call send.
- Clear all local auth state on logout or refresh failure.
- Never send the Supabase secret key or any backend environment secret from the
  frontend.
