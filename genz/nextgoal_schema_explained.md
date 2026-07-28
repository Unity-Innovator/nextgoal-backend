# Next goal Database Schema — Full Explanation

This document explains every table and column in the SaveMate ERD, organized by feature area. For each table: **what it's for**, **why it exists as its own table**, and **what each column does**.

---

## 1. Users & Authentication 
//done
### `USERS`
The core identity table — every other table ultimately traces back to a user.

| Column | Purpose |
|---|---|
| `id` | Unique identifier (UUID) for the user, used as the FK anchor everywhere else in the schema. |
| `full_name` | Display name shown across the app (profile, posts, bill splits). |
| `phone_number` | Primary login identifier — Cambodia's mobile-first context means phone, not email, is the main way users authenticate. |
| `email` | Optional secondary contact, useful for account recovery or notifications. |
| `password_hash` | BCrypt-hashed password — never store plain text. |
| `pin_hash` | A separate, shorter PIN (e.g. 4–6 digits) for quick re-authentication inside the app (like unlocking a wallet), distinct from the login password. |
| `status` | Account state: `ACTIVE`, `SUSPENDED`, `DEACTIVATED` — lets you disable an account without deleting data. |
| `otp_code` | The current one-time password sent via SMS, used for phone verification, login 2FA, or password reset. |
| `otp_expires_at` | Timestamp after which `otp_code` is no longer valid — OTPs should always expire (typically 5–10 min). |
| `otp_attempts` | Counter for failed OTP entry attempts — lets you lock out after too many tries (brute-force protection). |
| `otp_verified` | Whether the user has completed phone verification at least once. |
| `otp_purpose` | What the current OTP is for: `REGISTRATION`, `LOGIN`, `PASSWORD_RESET`, `PHONE_CHANGE` — lets one OTP mechanism serve multiple flows. |
| `created_at` / `updated_at` | Standard audit timestamps. |

**Why OTP fields live directly on `USERS`** rather than a separate `otp_codes` table: a user only ever has one *active* OTP at a time, so there's no need for a history table unless you want to audit past OTP attempts — in which case you'd add an `OTP_LOGS` table later.

---
done
### `USER_SETTINGS`
A 1:1 extension of `USERS` holding preferences and toggles, kept separate so the core `USERS` table (security-sensitive) stays lean.

| Column                        | Purpose |
|-------------------------------|---|
| `user_id`                     | Links back to the owning user (unique — enforces 1:1). |
| `language`                    | UI language preference (e.g. Khmer or English). |
| `theme`                       | Light/dark/system UI theme. |
| `bill_reminder`               | Toggle: notify user about pending bill splits. |
| `avatar_url`                  | Profile picture, shown in community posts and group member lists. |
| `goal_alert`                  | Toggle: notify user about savings goal progress/deadlines. |
| `lesson_reminder`             | Toggle: notify user to continue financial literacy lessons. |
| `ai_tips`                     | Toggle: whether the AI assistant proactively sends financial tips. |
| `face_id_enabled`             | Whether biometric login is enabled on this device. |
| `auto_lock_minutes`           | How long the app stays unlocked before requiring PIN/biometric re-entry. |
| `push_notifications_enabled`  | Master toggle for push notifications. |
| `sms_notifications_enabled`   | Master toggle for SMS notifications. |
| `email_notifications_enabled` | Master toggle for email notifications. |
| `profile_visibility`          | Who can see this user's profile: `PUBLIC`, `FRIENDS_ONLY`, `PRIVATE`. |
| `show_savings_goals_publicly` | Whether goal progress appears on the user's public profile/posts. |
| `show_full_name_on_posts`     | Privacy toggle — some users may prefer a nickname or initials on community posts. |
| `allow_comments_on_posts`     | Whether others can comment on this user's posts. |
| `preferred_currency`          | Default currency (KHR or USD) shown throughout the app. |

**Why separate from `USERS`**: settings change frequently and have no security implications, whereas `USERS` holds credentials — separating them means you can cache/read settings without touching sensitive auth data, and evolve settings independently as you add features.

---
done
### `SAVING_CONTRIBUTIONS`
Each individual deposit toward a goal.

| Column | Purpose |
|---|---|
| `goal_id` | Which goal this contributes to. |
| `user_id` | Who made the contribution (normally the goal owner). |
| `amount` | How much was added. |
| `note` | Optional memo (e.g. "birthday money"). |
| `contributed_at` | When the deposit happened. |

**Why a separate table instead of just updating `current_amount` directly**: keeping every contribution as its own row gives you a full history/timeline (useful for charts, streaks, and the "share your success" community posts), not just a final number.
done
### `SAVING_MILESTONES`
Checkpoints within a goal (e.g. 25%, 50%, 75%, 100%) that can trigger rewards.

| Column | Purpose |
|---|---|
| `goal_id` | Which goal this milestone belongs to. |
| `percentage` | The checkpoint threshold. |
| `amount_required` | The exact amount needed to hit this milestone. |
| `is_reached` | Whether the user has crossed this threshold yet. |
| `reached_at` | When they reached it. |

**Why separate from `SAVING_GOALS`**: a goal can have multiple milestones, and each milestone independently triggers something (like a reward) — modeling them as rows lets you query "which milestones has this user hit" without parsing logic out of a single goal row.

---

## 4. Gamification & Rewards
done
### `REWARDS`
The catalog of things a user can earn (badges, points, perks) — this is reference/lookup data, not tied to any one user.

| Column | Purpose |
|---|---|
| `name` | Reward name (e.g. "First Goal Completed"). |
| `type` | `BADGE`, `POINTS`, `DISCOUNT_VOUCHER`, etc. |
| `icon_url` | Visual badge/icon. |
| `description` | What the reward is and how it was earned. |
| `points_value` | Points awarded, if applicable. |
| `is_active` | Whether this reward is currently obtainable (lets you retire old rewards without deleting history). |
done
### `USER_REWARDS`
Tracks which rewards a specific user has actually earned — the junction between `USERS` and `REWARDS`.

| Column | Purpose |
|---|---|
| `user_id` | Who earned it. |
| `reward_id` | Which reward from the catalog. |
| `milestone_id` | If earned via a savings milestone, which one (nullable — some rewards come from lessons instead). |
| `source` | `SAVING_MILESTONE`, `LESSON_COMPLETION`, `STREAK`, etc. — lets one rewards system serve multiple features. |
| `points_earned` | Points actually credited (may differ from `REWARDS.points_value` if there are multipliers). |
| `earned_at` | When it was earned. |

---

## 5. Financial Literacy Lessons
done
### `LESSONS`
The catalog of educational content (e.g. "Budgeting 101", "Understanding Interest Rates").

| Column | Purpose |
|---|---|
| `title` | Lesson name. |
| `category` | e.g. Budgeting, Investing, Debt Management. |
| `content` | The actual lesson text/body. |
| `difficulty` | `BEGINNER`, `INTERMEDIATE`, `ADVANCED`. |
| `estimated_minutes` | Expected time to complete. |
| `points_reward` | Points earned on completion. |
| `is_active` | Whether the lesson is currently published. |
done
### `LESSON_QUESTIONS`
Quiz questions attached to a lesson, used to test comprehension.

| Column | Purpose |
|---|---|
| `lesson_id` | Which lesson this question belongs to. |
| `question` | The question text. |
| `option_a` – `option_d` | Multiple choice answers. |
| `correct_answer` | Which option is correct. |
| `order_index` | Display order within the lesson's quiz. |
done
### `USER_LESSONS`
Tracks an individual user's progress through a specific lesson — the junction between `USERS` and `LESSONS`.

| Column | Purpose |
|---|---|
| `user_id` | Who is taking the lesson. |
| `lesson_id` | Which lesson. |
| `status` | `NOT_STARTED`, `IN_PROGRESS`, `COMPLETED`. |
| `score` | Quiz score achieved. |
| `streak_count` | Consecutive days of lesson engagement — powers streak-based gamification. |
| `started_at` / `completed_at` | Progress timestamps. |

---

## 6. AI Assistant

### `AI_CHAT_MESSAGES`
Stores the conversation history between a user and the in-app AI financial assistant.

| Column | Purpose |
|---|---|
| `user_id` | Whose conversation this belongs to. |
| `role` | `USER` or `ASSISTANT` — distinguishes who sent the message. |
| `content` | The actual message text. |
| `context_type` | What the conversation is about, e.g. `BUDGETING_TIP`, `GOAL_ADVICE`, `GENERAL` — lets you scope AI responses or analyze what users ask about most. |
| `created_at` | Message timestamp, used to reconstruct conversation order. |

---
done
## 7. Community & Social Feed

### `POSTS`
The core feed table — users sharing experiences, goal successes, or life updates.

| Column | Purpose |
|---|---|
| `user_id` | Who posted. |
| `post_type` | `EXPERIENCE`, `GOAL_SUCCESS`, `LIFE_UPDATE`, `TIP`, `MILESTONE` — one table serves multiple content types instead of needing separate tables per type. |
| `content` | The post text. |
| `goal_id` | If this post celebrates a personal savings goal, links back to it (nullable). |
| `group_id` | If this post celebrates a group savings goal, links back to it (nullable). |
| `visibility` | `PUBLIC`, `FRIENDS_ONLY`, `PRIVATE`. |
| `is_pinned` | Whether the post is pinned to the top of the user's profile. |
| `status` | `ACTIVE`, `HIDDEN`, `DELETED`, `FLAGGED` — soft-moderation states. |
| `like_count` | Denormalized count for fast feed rendering (avoids a `COUNT()` join on every load). |
| `comment_count` | Same reasoning as `like_count`. |
done
### `POST_LIKES`
Junction table tracking who liked which post.

| Column | Purpose |
|---|---|
| `post_id` | Which post was liked. |
| `user_id` | Who liked it. |
| Unique constraint on `(post_id, user_id)` | Prevents the same user liking a post twice. |
done
### `POST_COMMENTS`
Comments on posts, with support for threaded replies.

| Column | Purpose |
|---|---|
| `post_id` | Which post this comment belongs to. |
| `user_id` | Who wrote it. |
| `parent_comment_id` | If this is a reply to another comment (nullable, self-referencing FK) — enables threaded discussions. |
| `content` | The comment text. |
| `status` | `ACTIVE`, `HIDDEN`, `DELETED`. |
done
### `USER_FOLLOWS`
Lets users follow each other, so the feed can show followed users' posts rather than everything public.

| Column | Purpose |
|---|---|
| `follower_id` | The user doing the following. |
| `following_id` | The user being followed. |
| Unique constraint on `(follower_id, following_id)` | Prevents duplicate follows; a `CHECK` prevents following yourself. |
done
### `POST_REPORTS`
Moderation — lets users flag inappropriate content.

| Column | Purpose |
|---|---|
| `post_id` | Which post was reported. |
| `reported_by` | Who filed the report. |
| `reason` | `SPAM`, `INAPPROPRIATE`, `HARASSMENT`, `MISINFORMATION`, `OTHER`. |
| `details` | Free-text explanation from the reporter. |
| `status` | `PENDING`, `REVIEWED`, `DISMISSED` — moderation workflow state. |

---
done
### `SAVING_GROUPS`
A shared savings pool multiple users contribute to (e.g. roommates saving for a trip, a family goal).

| Column | Purpose |
|---|---|
| `creator_id` | Who started the group. |
| `name` / `description` | Group identity. |
| `target_amount` / `current_amount` | Same denormalized pattern as `SAVING_GOALS`. |
| `currency` | KHR or USD. |
| `icon` | Visual identifier. |
| `deadline` | Target date. |
| `invite_code` | A shareable code/link so others can join without needing a direct invitation. |
| `status` | `ACTIVE`, `COMPLETED`, `ARCHIVED`, `CANCELLED`. |

**Why not just reuse `SAVING_GOALS` with a `group_id`?** A group needs membership, roles, and multi-party permissions — concerns a personal goal never has. Keeping them separate avoids a pile of nullable "only applies if it's a group" columns on `SAVING_GOALS`, and lets group-specific features (like withdrawal approvals) attach cleanly.

done
### `GROUP_MEMBERS`
Junction table tracking who belongs to which group and their role.

| Column | Purpose |
|---|---|
| `group_id` | Which group. |
| `user_id` | Which member. |
| `role` | `ADMIN` or `MEMBER` — determines permissions (e.g. only admins might approve withdrawals). |
| `status` | `ACTIVE`, `LEFT`, `REMOVED`. |
| `joined_at` | When they joined. |
done
### `GROUP_CONTRIBUTIONS`
Each individual deposit into a group's shared goal — mirrors `SAVING_CONTRIBUTIONS` but scoped to groups.

| Column | Purpose |
|---|---|
| `group_id` | Which group this contributes to. |
| `user_id` | Which member contributed. |
| `amount` / `currency` | How much, in what currency. |
| `note` | Optional memo. |
| `contributed_at` | Timestamp. |
done
### `GROUP_INVITATIONS`
Tracks pending invites to join a group before someone actually accepts.

| Column | Purpose |
|---|---|
| `group_id` | Which group the invite is for. |
| `invited_by` | Who sent it. |
| `phone_number` | Who's being invited (may not be a registered user yet). |
| `status` | `PENDING`, `ACCEPTED`, `DECLINED`, `EXPIRED`. |
| `qr_code_data` / `qr_image_url` | Optional QR version of the invite, so joining can happen via scan instead of typing a code. |
| `expires_at` | Invite expiry, so stale invites can't be used indefinitely. |
done
---
### `PAYMENT_METHODS`
External bank accounts/cards linked for moving real money in and out of the app.

| Column | Purpose |
|---|---|
| `user_id` | Owner. |
| `method_type` | `BANK_ACCOUNT`, `CARD`, `ACLEDA_ACCOUNT`. |
| `provider` | Which bank/service (e.g. ACLEDA, ABA, Wing). |
| `account_name` / `account_number_masked` | Identifying info, with the number masked for security (never store full account/card numbers in plain text). |
| `is_default` | Which method is used automatically for top-ups. |
| `status` | `ACTIVE`, `INACTIVE`, `VERIFICATION_PENDING`. |


---

## 10. Group Wallets
done
### `GROUP_WALLETS`
Same concept as `WALLETS`, but for a group's pooled money — kept separate because group funds need different rules (multiple contributors, approval-gated withdrawals).

| Column | Purpose |
|---|---|
| `group_id` | Which group this wallet belongs to (1:1). |
| `balance_khr` / `balance_usd` | Same dual-currency pattern as personal wallets. |
| `status` | `ACTIVE`, `FROZEN`, `CLOSED`. |
done
### `GROUP_WALLET_TRANSACTIONS`
Ledger for the group's shared pool — mirrors `WALLET_TRANSACTIONS`.

| Column | Purpose |
|---|---|
| `group_wallet_id` | Which group wallet. |
| `user_id` | Which member initiated this transaction. |
| `payment_method_id` | External account used, if this was an external top-up/withdrawal. |
| `transaction_type` | `CONTRIBUTION`, `WITHDRAWAL`, `PAYOUT`, `REFUND`. |
| `amount` / `currency` | Transaction details. |
| `balance_before` / `balance_after` | Same auditability reasoning as personal wallet transactions. |
| `status` | `PENDING`, `COMPLETED`, `FAILED`, `REVERSED`. |
done
### `GROUP_WITHDRAWAL_REQUESTS`
Since group money belongs to multiple people, a withdrawal isn't a unilateral action — it starts as a *request* that needs approval.

| Column | Purpose |
|---|---|
| `group_wallet_id` | Which wallet the withdrawal is from. |
| `requested_by` | Which member is asking to withdraw. |
| `amount` / `currency` | How much they want to withdraw. |
| `reason` | Justification shown to other members. |
| `status` | `PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`. |
| `approvals_required` | How many "yes" votes are needed (e.g. majority of active members). |
| `approvals_received` | Running count of approvals so far. |
| `resolved_at` | When the request was finally approved/rejected. |
done
### `GROUP_WITHDRAWAL_APPROVALS`
Each individual member's vote on a withdrawal request.

| Column | Purpose |
|---|---|
| `withdrawal_request_id` | Which request this vote is for. |
| `user_id` | Which member voted. |
| `decision` | `APPROVED` or `REJECTED`. |
| `decided_at` | When they voted. |

**Why this matters**: without this approval layer, any single group member could drain shared funds — a real trust and fraud risk. Multi-party approval is standard practice for pooled money and is likely to be viewed favorably by ACLEDA reviewers evaluating your competition submission.

---

## 11. QR Codes
done
### `PAYMENT_QR_CODES`
Generates scannable codes for receiving payments — works for both personal and group wallets via two mutually-exclusive nullable FKs.

| Column | Purpose |
|---|---|
| `user_id` | Set if this QR is for a personal wallet (nullable). |
| `group_wallet_id` | Set if this QR is for a group wallet (nullable — exactly one of `user_id`/`group_wallet_id` should be set, enforced via a `CHECK` constraint). |
| `qr_type` | `STATIC` (open amount, never expires — like a printed "pay me" code) or `DYNAMIC` (fixed amount, short expiry — for a specific payment request). |
| `amount` | Required for `DYNAMIC`, null for `STATIC` (payer enters their own amount). |
| `currency` | KHR or USD. |
| `qr_code_data` | The actual encoded payload/token — the source of truth. |
| `qr_image_url` | Pre-rendered image, regenerable anytime from `qr_code_data`. |
| `status` | `ACTIVE`, `EXPIRED`, `USED`, `CANCELLED`. |
| `expires_at` | Nullable for `STATIC`, set for `DYNAMIC`. |
| `used_at` | When the QR was successfully scanned and paid. |


---

## Recurring design patterns used throughout this schema

1. **Denormalized counters** (`current_amount`, `like_count`, `comment_count`, wallet balances) are kept as cached values on the parent row, updated whenever a related transaction/event row is inserted — this avoids expensive aggregation queries on every read, at the cost of needing to keep them in sync in your service layer.

2. **Polymorphic references** (`reference_type` + `reference_id` in `WALLET_TRANSACTIONS`; `transaction_id` in `QR_SCAN_LOGS`) let one table point to rows in multiple other tables without needing a separate nullable FK column for each possible target.

3. **Soft status fields** (`status` columns everywhere) are used instead of hard deletes almost everywhere — this preserves history for auditing, undo, and analytics, which matters a lot in a financial app where you generally never want to truly lose a record.

4. **1:1 extension tables** (`USER_SETTINGS`, `WALLETS`, `GROUP_WALLETS`) keep the core identity tables (`USERS`, `SAVING_GROUPS`) lean and security-focused, while related-but-separate concerns live in their own table.

5. **Junction tables** (`BILL_PARTICIPANTS`, `GROUP_MEMBERS`, `POST_LIKES`, `USER_FOLLOWS`, `USER_REWARDS`, `USER_LESSONS`) model many-to-many or one-to-many relationships explicitly, each carrying its own metadata (role, status, timestamps) rather than being a bare linking table.
