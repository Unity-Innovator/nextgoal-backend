# Redis and PlasGate OTP

## API

Send a six-digit SMS code with `POST /api/auth/otp/send`:

```json
{"phoneNumber":"012345678"}
```

The response is the existing API success envelope with no OTP in its body.
Set `purpose` to `LOGIN` for passwordless login. If omitted, the server keeps
the compatibility behavior of choosing `REGISTRATION` for a new or pending
account and `PASSWORD_RESET` for an existing active account.

```json
{"phoneNumber":"012345678","purpose":"LOGIN"}
```

Verify with `POST /api/auth/otp/verify`:

```json
{"phoneNumber":"012345678","otp":"583921","purpose":"REGISTRATION"}
```

For passwordless login, verify with the matching purpose:

```json
{"phoneNumber":"012345678","otp":"583921","purpose":"LOGIN"}
```

Use the same phone number representation throughout send, verify, registration,
and login. For SMS delivery only, local numbers starting with `0` are converted
to Cambodia's `855` prefix, and an international leading `+` is removed.

For `REGISTRATION`, success returns `data.verificationToken`; supply it as
`verificationToken` to `POST /api/auth/register`. For `PASSWORD_RESET`, supply
the returned token as `resetToken` to `POST /api/auth/reset-password`. These
single-use verification tokens remain in PostgreSQL and expire after ten minutes.

For `LOGIN`, success returns `data.accessToken`, `data.refreshToken`,
`data.tokenType`, `data.expiresIn`, and `data.user`. The OTP is consumed before
the JWTs are issued, so a replay cannot log in twice.

The old `/api/auth/send-otp` and `/api/auth/verify-otp` routes are removed.
Email is not used or stored by authentication. Registration requires only the
phone number and the other profile fields shown in its request model.

## Storage and delivery

Spring Boot generates codes with `SecureRandom`, computes HMAC-SHA256 using
`OTP_SECRET` and the phone, purpose, and code, and stores only the resulting hash
alongside purpose and attempt count in Redis. The challenge TTL is 300 seconds.
Redis Lua scripts atomically enforce the 60-second resend cooldown, replace old
challenges on resend, compare submitted hashes, and delete successful challenges.
Five incorrect attempts delete the challenge; wrong-purpose submissions count
as incorrect attempts. Verification does not extend the TTL.

PlasGate receives the SMS after Redis stores the challenge. Failed HTTP sends
remove the matching challenge and cooldown, without deleting a newer challenge.
OTP values are neither logged nor returned. Provider message masking hides the
code in PlasGate transaction reports. The request uses PlasGate's
[documented REST API](https://support.plasgate.com/article/api-overview).
HTTP acceptance does not guarantee handset delivery.

Redis consumption cannot roll back with a PostgreSQL transaction. If issuing the
verification token fails after consumption, request a new OTP after the cooldown.
Redis loss or restart invalidates outstanding OTPs; there is no database fallback.

## Configuration and rollout

Set `OTP_SECRET` to an independent random secret of at least 32 bytes, and configure
`PLASGATE_PRIVATE_KEY`, `PLASGATE_SECRET_KEY`, and `PLASGATE_SENDER`.
See `.env.example`. Missing configuration causes OTP requests to fail.
For a separately hosted Redis, configure `SPRING_DATA_REDIS_HOST`,
`SPRING_DATA_REDIS_PORT`, and optionally `SPRING_DATA_REDIS_PASSWORD`.
Both Compose files include a private Redis service and wire the backend to it.

The optional properties `otp.max-attempts` and `otp.resend-cooldown-seconds`
must be positive; defaults are 5 and 60. Code length and expiry are fixed at six
digits and five minutes.

Flyway migration `V9__remove_database_otps.sql` drops the obsolete `otps` table.
Keep previous migrations intact. Coordinate rollout so old application instances
are stopped before V9 runs, and update clients to the new routes. Existing database
OTPs are invalidated by this switch. With Flyway disabled, Hibernate update will
not remove the obsolete table; apply the migration through your migration process.

## Tests

Run `./mvnw test` (Windows: `./mvnw.cmd test`). HTTP integration tests mock Redis
and SMS to exercise registration and password reset without sending messages.
Set `OTP_TEST_REDIS_PORT` to a dedicated local Redis port to also run the real
Redis tests for TTL, expiry, failed attempts, resend, cleanup, and concurrent
verification. These tests use unique keys and delete only their own keys.
