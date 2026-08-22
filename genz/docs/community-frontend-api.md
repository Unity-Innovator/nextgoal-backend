# Community feature: frontend integration guide

This document is the frontend contract for the community and chat APIs currently implemented in this repository. It is based on the controllers, DTOs, validation rules, security configuration, and WebSocket publishers in the backend source.

The mobile client is Flutter. Start with the copy-ready Dart networking and
STOMP examples in [the Flutter integration guide](community-flutter-integration.md),
then use this document for the exhaustive endpoint and payload contract.
TypeScript blocks below are compact schema notation for the wire format, not a
recommendation for the mobile implementation.

## 1. Connection and authentication

Local development URLs:

| Purpose | URL |
|---|---|
| REST API | `http://localhost:8080` |
| Raw WebSocket/STOMP | `ws://localhost:8080/ws-raw` |
| SockJS/STOMP | `http://localhost:8080/ws` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |
| Swagger UI | `http://localhost:8080/swagger-ui/index.html` |

For browser clients, the current REST CORS allowlist is
`https://10.110.1.110`, `http://localhost:49896`, and
`http://nextgoal.local`. A frontend served from another origin (for example,
`http://localhost:3000`) will be rejected by the browser until that origin is
added to `WebConfig`. WebSocket origins default to `http://localhost:*` and
`https://localhost:*` and can be configured with
`WEBSOCKET_ALLOWED_ORIGIN_PATTERNS`.

All community and chat REST endpoints require a JWT access token:

```http
Authorization: Bearer <accessToken>
Content-Type: application/json
```

Obtain the token through the existing login endpoint:

```http
POST /api/auth/login
Content-Type: application/json

{
  "phoneNumber": "0100000001",
  "password": "Password1"
}
```

The token is returned at `response.data.accessToken`. A successful REST response always uses this envelope:

```ts
export interface ApiResponse<T> {
  success: true;
  message: string;
  data: T;
}

export interface ApiError {
  message: string;
  status: number;
  timestamp: string;
}
```

Example helper:

```ts
const API_URL = "http://localhost:8080";

export async function apiRequest<T>(
  path: string,
  accessToken: string,
  init: RequestInit = {},
): Promise<T> {
  const response = await fetch(`${API_URL}${path}`, {
    ...init,
    headers: {
      Accept: "application/json",
      ...(init.body ? { "Content-Type": "application/json" } : {}),
      Authorization: `Bearer ${accessToken}`,
      ...init.headers,
    },
  });

  const responseText = await response.text();
  const body = responseText ? JSON.parse(responseText) : null;
  if (!response.ok) {
    const error: ApiError = body ?? {
      message: `Request failed with HTTP ${response.status}`,
      status: response.status,
      timestamp: new Date().toISOString(),
    };
    throw Object.assign(new Error(error.message), {
      status: response.status,
      retryAfter: response.headers.get("Retry-After"),
      body: error,
    });
  }

  if (!body) throw new Error("Server returned an empty success response");
  return (body as ApiResponse<T>).data;
}
```

Conventions:

- IDs are UUID strings in the standard 36-character format.
- REST timestamps such as `createdAt` are ISO-8601 strings, normally UTC, for example `2026-08-22T08:15:30.123Z`.
- A nullable backend field is represented by `null`; do not assume it is always omitted.
- Enum values are case-sensitive uppercase strings.
- `DELETE` endpoints in this feature return a success envelope, usually with `data: null`.

## 2. Endpoint summary

All paths below are relative to the REST base URL.

### Friendship and blocking

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/community/friend-requests` | Send a friend request |
| `GET` | `/api/community/friend-requests/incoming` | List incoming requests |
| `GET` | `/api/community/friend-requests/outgoing` | List outgoing requests |
| `POST` | `/api/community/friend-requests/{requestId}/accept` | Accept an incoming request |
| `POST` | `/api/community/friend-requests/{requestId}/decline` | Decline an incoming request |
| `DELETE` | `/api/community/friend-requests/{requestId}` | Cancel an outgoing request |
| `GET` | `/api/community/friends` | List friends |
| `DELETE` | `/api/community/friends/{friendUserId}` | Remove a friend |
| `POST` | `/api/community/blocks/{userId}` | Block a user |
| `GET` | `/api/community/blocks` | List blocked users |
| `DELETE` | `/api/community/blocks/{userId}` | Unblock a user |

### Posts, feeds, and engagement

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/community/posts` | Create a post |
| `GET` | `/api/community/posts/{postId}` | Get one visible post |
| `PUT` | `/api/community/posts/{postId}` | Update an owned post |
| `DELETE` | `/api/community/posts/{postId}` | Soft-delete an owned post |
| `POST` | `/api/community/posts/{postId}/share` | Share a visible post |
| `GET` | `/api/community/feed` | Public feed; alias of `/feed/public` |
| `GET` | `/api/community/feed/public` | Public feed |
| `GET` | `/api/community/feed/friends` | Friends feed |
| `GET` | `/api/community/users/{userId}/posts` | User profile feed |
| `POST` | `/api/community/posts/{postId}/like` | Like a post |
| `DELETE` | `/api/community/posts/{postId}/like` | Unlike a post |
| `POST` | `/api/community/posts/{postId}/comments` | Add a top-level comment |
| `POST` | `/api/community/comments/{commentId}/replies` | Reply to a comment |
| `PUT` | `/api/community/comments/{commentId}` | Edit an owned comment/reply |
| `DELETE` | `/api/community/comments/{commentId}` | Soft-delete an owned comment/reply |
| `POST` | `/api/community/comments/{commentId}/like` | Like a comment/reply |
| `DELETE` | `/api/community/comments/{commentId}/like` | Unlike a comment/reply |

### Chat

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/chat/conversations/direct` | Find or create a direct conversation |
| `POST` | `/api/chat/conversations/group` | Create a group conversation |
| `GET` | `/api/chat/conversations` | List the current user's conversations |
| `GET` | `/api/chat/conversations/{conversationId}` | Get one conversation |
| `GET` | `/api/chat/conversations/{conversationId}/messages` | Get message history |
| `POST` | `/api/chat/conversations/{conversationId}/messages` | Send a message through REST |
| `POST` | `/api/chat/conversations/{conversationId}/delivered` | Mark one message delivered |
| `POST` | `/api/chat/conversations/{conversationId}/read` | Mark the conversation read through a message |

### Notifications and moderation

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/community/notifications` | List community notifications |
| `GET` | `/api/community/notifications/unread-count` | Get unread count |
| `POST` | `/api/community/notifications/{notificationId}/read` | Mark one notification read |
| `POST` | `/api/community/notifications/read-all` | Mark all notifications read |
| `POST` | `/api/community/posts/{postId}/reports` | Report a post |
| `POST` | `/api/community/comments/{commentId}/reports` | Report a comment |

Admin-only moderation endpoints are documented in section 9.

## 3. Friendship and blocking

### Types

```ts
export type FriendRequestStatus =
  | "PENDING"
  | "ACCEPTED"
  | "DECLINED"
  | "CANCELLED";

export interface CommunityUser {
  id: string;
  fullName: string;
}

export interface FriendRequest {
  id: string;
  sender: CommunityUser;
  receiver: CommunityUser;
  status: FriendRequestStatus;
  createdAt: string;
  respondedAt: string | null;
}

export interface Friend {
  friendshipId: string;
  user: CommunityUser;
  friendsSince: string;
}

export interface BlockedUser {
  blockId: string;
  user: CommunityUser;
  blockedAt: string;
}

export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}
```

For all friendship page endpoints, `page` is zero-based, `size` must be `1..100`, and the default status for request lists is `PENDING`.

### Send a request

```ts
const request = await apiRequest<FriendRequest>(
  "/api/community/friend-requests",
  token,
  {
    method: "POST",
    body: JSON.stringify({ userId: otherUserId }),
  },
);
```

Returns `201 Created`. Sending to yourself is invalid. A request is rejected when either user has blocked the other, the users are already friends, or a pending request already exists in either direction.

### List requests and friends

```ts
const incoming = await apiRequest<PageResponse<FriendRequest>>(
  "/api/community/friend-requests/incoming?status=PENDING&page=0&size=20",
  token,
);

const outgoing = await apiRequest<PageResponse<FriendRequest>>(
  "/api/community/friend-requests/outgoing?status=PENDING&page=0&size=20",
  token,
);

const friends = await apiRequest<PageResponse<Friend>>(
  "/api/community/friends?page=0&size=30",
  token,
);
```

Only the receiver can accept or decline. Only the sender can cancel.

```ts
await apiRequest<Friend>(
  `/api/community/friend-requests/${requestId}/accept`,
  token,
  { method: "POST" },
);
```

### Blocking behavior

Blocking is idempotent: the first call normally returns `201`; calling it again returns `200`. Blocking a user also removes an existing friendship and cancels pending friend requests between the two users. Posts from either side are excluded from feeds and direct messages cannot be sent while either side blocks the other.

```ts
await apiRequest<BlockedUser>(`/api/community/blocks/${userId}`, token, {
  method: "POST",
});
```

Unblocking does not restore the old friendship.

## 4. Posts and feeds

### Types

```ts
export type PostType =
  | "GOAL_SUCCESS"
  | "MILESTONE"
  | "TIP"
  | "EXPERIENCE"
  | "LIFE_UPDATE"
  | "SHARED_POST";

export type PostVisibility = "PUBLIC" | "FRIENDS_ONLY" | "PRIVATE";
export type PostStatus = "ACTIVE" | "HIDDEN" | "DELETED" | "FLAGGED";
export type PostMediaType = "IMAGE" | "VIDEO";

export interface PostMediaInput {
  mediaType: PostMediaType;
  mediaUrl: string;
  thumbnailUrl?: string | null;
  widthPx?: number | null;
  heightPx?: number | null;
  durationMs?: number | null;
}

export interface PostMedia extends PostMediaInput {
  id: string;
  sortOrder: number;
  createdAt: string;
}

export interface PostAuthor {
  id: string;
  fullName: string | null;
}

export interface CommunityPost {
  id: string;
  author: PostAuthor;
  goalId: string | null;
  sharedPostId: string | null;
  postType: PostType;
  content: string | null;
  visibility: PostVisibility;
  status: PostStatus;
  pinned: boolean;
  ownedByCurrentUser: boolean;
  media: PostMedia[];
  likeCount: number;
  commentCount: number;
  shareCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface PostCursor {
  createdAt: string;
  postId: string;
}

export interface PostCursorPage {
  items: CommunityPost[];
  nextCursor: PostCursor | null;
  hasNext: boolean;
}
```

`fullName` may be `null` when the author disables name sharing. `goalId` may be `null` when there is no linked goal or the author hides savings goals. Use `ownedByCurrentUser` to show edit/delete controls.

### Create a post

```ts
const post = await apiRequest<CommunityPost>("/api/community/posts", token, {
  method: "POST",
  body: JSON.stringify({
    postType: "LIFE_UPDATE",
    content: "Started my emergency fund today!",
    visibility: "FRIENDS_ONLY",
    goalId: null,
    media: [
      {
        mediaType: "IMAGE",
        mediaUrl: "https://cdn.example.com/community/photo.jpg",
        thumbnailUrl: "https://cdn.example.com/community/photo-thumb.jpg",
        widthPx: 1200,
        heightPx: 900,
        durationMs: null,
      },
    ],
  }),
});
```

Returns `201 Created`. Rules:

- `postType` is required. Do not use `SHARED_POST` here; use the share endpoint.
- `content` is optional but limited to 5,000 characters.
- A normal post must contain text or at least one media item.
- `visibility` defaults to `PUBLIC` when omitted.
- `goalId`, when supplied, must belong to the authenticated user.
- A post accepts at most 10 media metadata items.
- `mediaUrl` and nonblank `thumbnailUrl` must be absolute HTTP/HTTPS URLs, up to 2,048 characters.
- Positive dimensions are required when present; `durationMs` may be zero or positive.
- The array order becomes `sortOrder`, starting at zero.
- This endpoint stores media metadata only. The client must upload the actual file to storage first, then send its URL.

### Update and delete

Update accepts any combination of `content`, `visibility`, and `media`, but at least one must be present:

```ts
await apiRequest<CommunityPost>(`/api/community/posts/${postId}`, token, {
  method: "PUT",
  body: JSON.stringify({
    content: "Updated text",
    visibility: "PUBLIC",
  }),
});
```

If `media` is present, it replaces the complete media list. An empty array removes all media. Only the owner can update or delete a post. Deletion is soft deletion and returns `data: null`.

### Share a post

```ts
const shared = await apiRequest<CommunityPost>(
  `/api/community/posts/${sourcePostId}/share`,
  token,
  {
    method: "POST",
    body: JSON.stringify({
      content: "This is useful!",
      visibility: "FRIENDS_ONLY",
    }),
  },
);
```

Returns a new post with `postType: "SHARED_POST"` and `sharedPostId` pointing to the source. The frontend can fetch the source with `GET /api/community/posts/{sharedPostId}`. Private posts cannot be shared, and a friends-only source cannot be reshared as public. If visibility is omitted, it inherits the source visibility.

### Cursor pagination

Feed and profile endpoints use a stable two-field cursor. First request:

```http
GET /api/community/feed/public?limit=20
```

When `hasNext` is true, pass both fields returned in `nextCursor`:

```ts
export function addPostCursor(path: string, cursor: PostCursor | null, limit = 20) {
  const params = new URLSearchParams({ limit: String(limit) });
  if (cursor) {
    params.set("cursorCreatedAt", cursor.createdAt);
    params.set("cursorPostId", cursor.postId);
  }
  return `${path}?${params}`;
}
```

Never send only one cursor field; that returns `400`. `limit` must be `1..100`. Stop loading when `hasNext` is false; `nextCursor` is then `null`.

Feed behavior:

- Public feed returns active public posts the viewer is allowed to see.
- Friends feed returns active public and friends-only posts authored by friends.
- Profile feed returns the owner's active posts filtered for the viewer. Owners can see their own private posts.
- All feeds sort by `createdAt DESC, postId DESC` and exclude users blocked in either direction.
- Deleted and moderator-hidden posts are excluded.

## 5. Likes, comments, and replies

### Types

```ts
export interface LikeResult {
  liked: boolean;
  changed: boolean;
  likeCount: number;
}

export type CommentStatus = "ACTIVE" | "HIDDEN" | "DELETED";

export interface CommentResponse {
  id: string;
  postId: string;
  parentCommentId: string | null;
  author: CommunityUser;
  content: string;
  status: CommentStatus;
  likeCount: number;
  replyCount: number;
  likedByCurrentUser: boolean;
  ownedByCurrentUser: boolean;
  createdAt: string;
  updatedAt: string;
}
```

Like and unlike are idempotent. `changed` tells the client whether the database state changed, while `likeCount` is the authoritative counter to place in the UI:

```ts
const result = await apiRequest<LikeResult>(
  `/api/community/posts/${postId}/like`,
  token,
  { method: "POST" },
);

setLiked(result.liked);
setLikeCount(result.likeCount);
```

Comments and replies use the same body:

```json
{ "content": "Great progress!" }
```

Content is required, trimmed, and limited to 2,000 characters. Replies return `parentCommentId`; top-level comments return `null`. Only the comment author can edit or delete it. A post author may disable comments for other users through privacy settings, but can still comment on their own post.

### Current frontend blocker

The current backend does **not** expose endpoints to list a post's comments or a comment's replies. `PostResponse` also does **not** contain `likedByCurrentUser`. Therefore, the frontend can immediately display comments returned by create/reply mutations and remember local post-like state, but it cannot reliably reconstruct those states after a refresh.

Before the engagement UI is treated as complete, add contracts similar to:

```http
GET /api/community/posts/{postId}/comments?cursorCreatedAt=...&cursorCommentId=...&limit=20
GET /api/community/comments/{commentId}/replies?cursorCreatedAt=...&cursorCommentId=...&limit=20
```

and add `likedByCurrentUser: boolean` to `PostResponse`. Until then, do not invent an undocumented comments GET call in the frontend.

## 6. Chat REST API

### Types

```ts
export type ConversationType = "DIRECT" | "GROUP";
export type ConversationMemberRole = "OWNER" | "ADMIN" | "MEMBER";
export type MessageType = "TEXT" | "IMAGE" | "VIDEO" | "FILE" | "VOICE" | "SYSTEM";
export type MessageStatus = "SENT" | "EDITED" | "DELETED";
export type AttachmentType = "IMAGE" | "VIDEO" | "FILE" | "VOICE";

export interface ChatUser {
  id: string;
  fullName: string;
  avatarUrl: string | null;
}

export interface ConversationMember {
  user: ChatUser;
  role: ConversationMemberRole;
  joinedAt: string;
}

export interface MessageSummary {
  id: string;
  senderId: string | null;
  type: MessageType;
  body: string | null;
  status: MessageStatus;
  createdAt: string;
}

export interface Conversation {
  id: string;
  type: ConversationType;
  title: string | null;
  imageUrl: string | null;
  members: ConversationMember[];
  lastMessage: MessageSummary | null;
  unreadCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface MessageAttachment {
  id: string;
  type: AttachmentType;
  fileUrl: string;
  thumbnailUrl: string | null;
  originalName: string | null;
  mimeType: string | null;
  fileSizeBytes: number | null;
  widthPx: number | null;
  heightPx: number | null;
  durationMs: number | null;
}

export interface MessageReceipt {
  userId: string;
  deliveredAt: string | null;
  readAt: string | null;
}

export interface ChatMessage {
  id: string;
  conversationId: string;
  clientMessageId: string;
  sender: ChatUser | null;
  replyToMessageId: string | null;
  type: MessageType;
  body: string | null;
  metadata: Record<string, unknown>;
  status: MessageStatus;
  attachments: MessageAttachment[];
  receipts: MessageReceipt[];
  createdAt: string;
  editedAt: string | null;
  deletedAt: string | null;
}

export interface SliceResponse<T> {
  items: T[];
  page: number;
  size: number;
  hasNext: boolean;
}
```

### Create or open a conversation

Direct chat requires an existing friendship. This endpoint returns `200` whether it creates a new direct conversation or returns the already existing one:

```ts
const conversation = await apiRequest<Conversation>(
  "/api/chat/conversations/direct",
  token,
  {
    method: "POST",
    body: JSON.stringify({ userId: friendUserId }),
  },
);
```

Create a group:

```ts
const group = await apiRequest<Conversation>(
  "/api/chat/conversations/group",
  token,
  {
    method: "POST",
    body: JSON.stringify({
      title: "Savings Squad",
      imageUrl: "https://cdn.example.com/groups/savings-squad.jpg",
      memberIds: [aliceId, bobId],
    }),
  },
);
```

Group title is required and limited to 120 characters. `memberIds` must contain `1..99` unique IDs; the creator is added automatically as `OWNER`. Every invited user must exist and be a friend of the creator.

### Inbox and history pagination

```ts
const inbox = await apiRequest<SliceResponse<Conversation>>(
  "/api/chat/conversations?page=0&size=30",
  token,
);

const history = await apiRequest<SliceResponse<ChatMessage>>(
  `/api/chat/conversations/${conversationId}/messages?page=0&size=50`,
  token,
);
```

`page` is zero-based and `size` must be `1..100`. Message history is newest-first. A typical chat UI should prepend later pages above the existing list or reverse each page for chronological rendering without changing page boundaries.

### Send a message

```ts
const clientMessageId = crypto.randomUUID();

const message = await apiRequest<ChatMessage>(
  `/api/chat/conversations/${conversationId}/messages`,
  token,
  {
    method: "POST",
    body: JSON.stringify({
      clientMessageId,
      body: "Hello!",
      replyToMessageId: null,
    }),
  },
);
```

`clientMessageId` is required and makes retries idempotent per sender. Generate it once when the user presses Send and reuse the same value for every retry of that logical message. A new message returns `201`; a duplicate retry returns the original message with `200`. Reusing the ID in another conversation returns `400`. The current send contract creates text messages only, with a maximum body length of 10,000 characters.

### Delivery and read positions

```ts
await apiRequest(
  `/api/chat/conversations/${conversationId}/delivered`,
  token,
  { method: "POST", body: JSON.stringify({ messageId }) },
);

await apiRequest(
  `/api/chat/conversations/${conversationId}/read`,
  token,
  { method: "POST", body: JSON.stringify({ messageId }) },
);
```

Mark-read means “read through this message” and updates all applicable receipt rows. The server never moves the read position backwards.

## 7. Real-time chat and notifications

The protocol is STOMP over WebSocket. JWT authentication is performed on the STOMP `CONNECT` frame. Put the token in the native `Authorization` header; adding it only to the WebSocket URL or only to REST requests does not authenticate the STOMP session.

Install one of these combinations:

```bash
npm install @stomp/stompjs
# Only when using /ws SockJS fallback:
npm install sockjs-client
```

Raw WebSocket example:

```ts
import { Client, IMessage } from "@stomp/stompjs";

const client = new Client({
  brokerURL: "ws://localhost:8080/ws-raw",
  connectHeaders: { Authorization: `Bearer ${accessToken}` },
  reconnectDelay: 5_000,
  heartbeatIncoming: 10_000,
  heartbeatOutgoing: 10_000,
});

client.onConnect = () => {
  client.subscribe("/user/queue/chat/messages", onMessageEvent);
  client.subscribe("/user/queue/chat/receipts", onReceiptEvent);
  client.subscribe("/user/queue/chat/typing", onTypingEvent);
  client.subscribe("/user/queue/chat/errors", onChatError);
  client.subscribe(
    "/user/queue/community/notifications",
    onCommunityNotification,
  );
};

client.onStompError = (frame) => {
  console.error("STOMP broker error", frame.headers.message, frame.body);
};

client.activate();
```

SockJS alternative:

```ts
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";

const client = new Client({
  webSocketFactory: () => new SockJS("http://localhost:8080/ws"),
  connectHeaders: { Authorization: `Bearer ${accessToken}` },
  reconnectDelay: 5_000,
  heartbeatIncoming: 10_000,
  heartbeatOutgoing: 10_000,
});
```

Do not include `/app` in subscription destinations. `/app` is only for client-to-server sends.

### Client-to-server destinations

| Destination | Body |
|---|---|
| `/app/chat/{conversationId}/send` | `{ clientMessageId, body, replyToMessageId }` |
| `/app/chat/{conversationId}/delivered` | `{ messageId }` |
| `/app/chat/{conversationId}/read` | `{ messageId }` |
| `/app/chat/{conversationId}/typing` | `{ typing: boolean }` |

```ts
client.publish({
  destination: `/app/chat/${conversationId}/send`,
  headers: { "content-type": "application/json" },
  body: JSON.stringify({
    clientMessageId: crypto.randomUUID(),
    body: "Sent in real time",
    replyToMessageId: null,
  }),
});
```

### Server-to-client events

```ts
export interface MessageCreatedEvent {
  event: "MESSAGE_CREATED";
  message: ChatMessage;
  duplicate: boolean;
}

export interface DeliveryReceiptEvent {
  event: "MESSAGE_DELIVERED";
  conversationId: string;
  messageId: string;
  userId: string;
  deliveredAt: string;
}

export interface ReadReceiptEvent {
  event: "MESSAGES_READ";
  conversationId: string;
  messageId: string;
  userId: string;
  readAt: string;
}

export interface TypingEvent {
  event: "TYPING";
  conversationId: string;
  userId: string;
  typing: boolean;
}

export interface ChatErrorEvent {
  event: "CHAT_ERROR";
  code: string;
  message: string;
  timestamp: string;
}
```

The message queue sends events to all active conversation members, including the sender. Reconcile an optimistic message by `clientMessageId`, then replace it with the authoritative `message`. Do not append both copies.

The receipt queue contains both delivery and read events, so discriminate using `event`. Typing is transient and not stored. Send `{ typing: false }` on blur/send when possible and apply a short client timeout in case the stop event is lost.

After reconnecting, refetch the inbox and the active conversation history. WebSocket queues are for live delivery, not durable offline replay.

## 8. Community notifications

### Types and payload routing

```ts
export type CommunityNotificationType =
  | "FRIEND_REQUEST"
  | "FRIEND_ACCEPTED"
  | "POST_LIKED"
  | "POST_COMMENTED"
  | "COMMENT_REPLIED"
  | "POST_SHARED"
  | "CHAT_MESSAGE";

export type NotificationReferenceType =
  | "FRIEND_REQUEST"
  | "POST"
  | "COMMENT"
  | "CONVERSATION"
  | "MESSAGE";

export interface NotificationActor {
  id: string;
  fullName: string;
}

export interface CommunityNotification {
  id: string;
  actor: NotificationActor | null;
  type: CommunityNotificationType;
  referenceType: NotificationReferenceType;
  referenceId: string;
  payload: Record<string, unknown>;
  read: boolean;
  readAt: string | null;
  createdAt: string;
}

export interface NotificationCursor {
  createdAt: string;
  notificationId: string;
}

export interface NotificationCursorPage {
  items: CommunityNotification[];
  nextCursor: NotificationCursor | null;
  hasNext: boolean;
}
```

Current payload keys:

| Type | Reference | Payload keys |
|---|---|---|
| `FRIEND_REQUEST` | friend request ID | `requestId`, `actorFullName` |
| `FRIEND_ACCEPTED` | friend request ID | `requestId`, `actorFullName` |
| `POST_LIKED` | post ID | `postId`, `actorFullName` |
| `POST_COMMENTED` | comment ID | `postId`, `commentId`, `actorFullName` |
| `COMMENT_REPLIED` | reply ID | `postId`, `parentCommentId`, `replyId`, `actorFullName` |
| `POST_SHARED` | source post ID | `sourcePostId`, `sharePostId`, `actorFullName` |

`CHAT_MESSAGE` exists in the enum but is not currently created by the community notification service; chat messages arrive on the dedicated chat queue.

Fetch notifications:

```ts
const notifications = await apiRequest<NotificationCursorPage>(
  "/api/community/notifications?unreadOnly=false&limit=20",
  token,
);
```

Pagination works like post cursors, but uses `cursorCreatedAt` plus `cursorNotificationId`. Both or neither must be provided, and `limit` must be `1..100`.

```ts
const { unreadCount } = await apiRequest<{ unreadCount: number }>(
  "/api/community/notifications/unread-count",
  token,
);

await apiRequest<CommunityNotification>(
  `/api/community/notifications/${notificationId}/read`,
  token,
  { method: "POST" },
);

const allRead = await apiRequest<{
  markedReadCount: number;
  unreadCount: number;
}>("/api/community/notifications/read-all", token, { method: "POST" });
```

The mark operations are idempotent. Real-time payloads on `/user/queue/community/notifications` are a `CommunityNotification` directly, not an `ApiResponse` envelope. No notification is created when actor and recipient are the same user.

Recommended UI flow:

1. Fetch the first notification page and unread count after login.
2. Connect STOMP and subscribe to the notification queue.
3. Prepend each live notification and increment the badge only when `read` is false.
4. Navigate using `type` and `payload`, not by parsing human-readable text.
5. Mark the notification read after opening its destination.

## 9. Reports and admin moderation

### User reports

```ts
export type ReportReason =
  | "SPAM"
  | "HARASSMENT"
  | "INAPPROPRIATE"
  | "MISINFORMATION"
  | "OTHER";

export type ReportStatus =
  | "PENDING"
  | "REVIEWED"
  | "DISMISSED"
  | "ACTION_TAKEN";

await apiRequest(`/api/community/posts/${postId}/reports`, token, {
  method: "POST",
  body: JSON.stringify({
    reason: "SPAM",
    details: "Repeated promotional content",
  }),
});
```

Use the same body for comment reports. `reason` is required; `details` is optional and limited to 1,000 characters. A user cannot report their own content or report the same target twice.

### Admin-only endpoints

These require a token whose user has role `ADMIN`:

| Method | Path |
|---|---|
| `GET` | `/api/community/admin/reports/posts?status=PENDING&page=0&size=20` |
| `GET` | `/api/community/admin/reports/comments?status=PENDING&page=0&size=20` |
| `PUT` | `/api/community/admin/reports/posts/{reportId}` |
| `PUT` | `/api/community/admin/reports/comments/{reportId}` |
| `DELETE` | `/api/community/admin/posts/{postId}` |
| `DELETE` | `/api/community/admin/comments/{commentId}` |

Review body:

```json
{
  "status": "ACTION_TAKEN",
  "moderationNote": "Removed after review"
}
```

A review status cannot remain `PENDING`. `ACTION_TAKEN` also hides the target content. A report can only be reviewed once. Admin list pages use the standard `PageResponse` shape and accept sizes `1..100`.

Moderation response:

```ts
export interface ModerationUser {
  id: string;
  fullName: string;
}

export interface ModerationReport {
  id: string;
  targetType: "POST" | "COMMENT";
  targetId: string;
  targetAuthorId: string;
  targetContent: string | null;
  reportedBy: ModerationUser;
  reason: ReportReason;
  details: string | null;
  status: ReportStatus;
  reviewedBy: ModerationUser | null;
  moderationNote: string | null;
  reviewedAt: string | null;
  createdAt: string;
}
```

## 10. Privacy, validation, and rate limits

Privacy rules are enforced by the backend and must also guide the UI:

- Profile visibility is the maximum audience for a post. A user with a friends-only profile cannot create a public post; a private profile cannot create a broader post.
- Private posts are visible only to their author.
- Friends-only posts require an active friendship.
- A block in either direction hides content and prevents friendship/direct-chat actions.
- `showFullNameOnPosts = false` produces `author.fullName: null` for other viewers.
- `showSavingsGoalsPublicly = false` produces `goalId: null` for other viewers.
- `allowCommentsOnPosts = false` prevents other users from commenting or replying.
- Hidden content is deliberately returned like unavailable content in many read paths. Treat `404` as unavailable, not proof that an ID never existed.

Text rejects unsupported control characters and unsafe script/iframe/object/embed or `javascript:` markup. The frontend should still render all user text as text, never inject it as raw HTML.

Current per-user limits:

| Action | Limit/window |
|---|---|
| Create post | 10/minute |
| Update post | 20/minute |
| Share post | 10/minute |
| Like post | 60/minute |
| Write/edit comment or reply | 30/minute |
| Like comment | 60/minute |
| Send friend request | 20/hour |
| Report content | 10/hour |

On `429 Too Many Requests`, use the `Retry-After` response header in seconds to disable/re-enable the action. Unlike and delete actions are not listed as rate-limited in the current implementation.

## 11. Error handling

Expected statuses:

| Status | Frontend handling |
|---|---|
| `200` | Successful read/update/idempotent repeat |
| `201` | Resource created |
| `400` | Invalid UUID/enum/JSON, validation, cursor, or business input |
| `401` | Authentication failure when an authentication entry point returns unauthorized |
| `403` | Missing/invalid authentication in the current security setup, or a valid user lacks access |
| `404` | Resource absent or intentionally hidden by privacy |
| `409` | State conflict, such as duplicate pending request/report |
| `429` | Rate limited; respect `Retry-After` |
| `500` | Unexpected server error; show retry UI and log request context |

Controller-handled REST errors are not wrapped in `ApiResponse`:

```json
{
  "message": "cursorCreatedAt and cursorPostId must be provided together",
  "status": 400,
  "timestamp": "2026-08-22T15:10:00.123456"
}
```

An error generated directly by Spring Security may have an empty body. The
fetch helper above handles both the structured body and that empty-body case.

WebSocket chat operation errors arrive on `/user/queue/chat/errors`. A STOMP connection authentication failure can close/reject the connection before a subscription is active, so handle the STOMP/WebSocket error callbacks as well.

## 12. Recommended frontend data flow

At login:

1. Store the access token in the application's secure authentication layer.
2. Fetch public feed, friends, inbox, and unread notification count in parallel.
3. Connect STOMP once and install all user-queue subscriptions.

For a feed screen:

1. Keep separate cursor state for public, friends, and each user-profile feed.
2. Deduplicate posts by `id` when appending pages or inserting newly created posts.
3. Replace counters with server-returned counters after every like/comment/share mutation.
4. Resolve shared content by `sharedPostId`; cache source posts by ID.
5. Clear the list and cursor on pull-to-refresh.

For chat:

1. Load the conversation and first history page through REST.
2. Generate one `clientMessageId` per outgoing logical message.
3. Render an optimistic pending item and send over STOMP or REST.
4. Reconcile `MESSAGE_CREATED` by `clientMessageId`.
5. Send delivered after receiving/displaying a remote message and read when the user views through it.
6. On reconnect, refetch the inbox and active conversation rather than assuming missed WebSocket events will replay.

For notifications:

1. Use REST as durable history and the WebSocket queue for live additions.
2. Deduplicate by notification `id`.
3. Reconcile the badge with `/unread-count` after reconnect or app foregrounding.

## 13. Known contract gaps

The following backend work is still needed for a fully fetchable frontend experience:

1. Add paginated GET endpoints for top-level comments and replies.
2. Add `likedByCurrentUser` to post/feed responses.
3. Add upload endpoints or a documented direct-upload flow; current post/chat models only expose storage metadata.
4. Chat currently sends text messages only. Attachment DTOs exist in responses, but no attachment upload/send endpoint is exposed.
5. Group member management, message edit/delete, and message reactions have no public endpoints yet.
6. There is no explicit user search/discovery endpoint within the community controller; the frontend needs an existing user endpoint or a dedicated community search contract to obtain target user IDs.

Treat this section as part of the contract: the frontend should not guess these APIs. Update this document whenever one of the missing endpoints is implemented.
