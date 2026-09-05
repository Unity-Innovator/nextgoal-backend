# Email OTP authentication flows

Users continue to identify their accounts with a phone number. The backend uses
that phone number to find the account and sends the code to the email stored on
the account. Registration is the only request that must include both a phone
number and an email address.

## Gmail configuration

1. Enable 2-Step Verification on the Gmail sender account.
2. Create a Google App Password for the backend.
3. Configure these environment variables without committing their real values:

```text
MAIL_USERNAME=sender@gmail.com
MAIL_APP_PASSWORD=the-16-character-app-password
```

The application uses `smtp.gmail.com` on port `587` with STARTTLS.

## Register and verify

`POST /api/auth/register` creates a `PENDING` user and emails a registration OTP.
It returns HTTP `202` and does not issue tokens yet.

```json
{
  "firstName": "Test",
  "lastName": "Person",
  "phoneNumber": "0123456789",
  "email": "person@example.com",
  "password": "Password1",
  "confirmPassword": "Password1",
  "gender": "OTHER",
  "dateOfBirth": "2000-01-01",
  "pin": "123456"
}
```

`POST /api/auth/register/verify-otp` consumes the registration OTP, changes the
user to `ACTIVE`, and returns access and refresh tokens.

```json
{
  "phoneNumber": "0123456789",
  "otp": "123456"
}
```

`POST /api/auth/register/resend-otp` sends another registration code after the
configured cooldown.

```json
{
  "phoneNumber": "0123456789"
}
```

## Forgot and reset password

`POST /api/auth/forgot-password` accepts only the phone number. Its response is
the same whether the account exists, which avoids exposing registered phone
numbers.

```json
{
  "phoneNumber": "0123456789"
}
```

`POST /api/auth/reset-password` consumes a password-reset OTP and changes the
password.

```json
{
  "phoneNumber": "0123456789",
  "otp": "654321",
  "newPassword": "NewPassword1",
  "confirmPassword": "NewPassword1"
}
```

Registration and password-reset codes have different stored purposes, so a code
issued for one flow cannot be used in the other flow. Codes expire after five
minutes, can be attempted five times, and cannot be resent for 60 seconds.
