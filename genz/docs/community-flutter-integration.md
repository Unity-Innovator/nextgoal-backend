# Community feature: Flutter mobile integration

This is the Flutter/Dart implementation companion to the complete
[community API contract](community-frontend-api.md). The API contract remains
the source of truth for every endpoint, enum, validation rule, privacy rule,
and response field. This guide shows how a Flutter mobile application should
call those APIs and consume the real-time STOMP events.

## 1. Recommended packages

Add the networking, secure-storage, WebSocket, and JSON-model packages:

```bash
flutter pub add dio
flutter pub add flutter_secure_storage
flutter pub add stomp_dart_client
flutter pub add uuid
flutter pub add json_annotation
flutter pub add --dev build_runner
flutter pub add --dev json_serializable
```

`dio` provides interceptors and structured errors. Store JWTs in platform
secure storage, not `SharedPreferences`. `stomp_dart_client` connects directly
to the backend's raw STOMP endpoint and sends the JWT in the STOMP `CONNECT`
frame.

## 2. Mobile development URLs

`localhost` inside a device or emulator means that device, not the development
computer.

| Runtime | REST base URL | WebSocket URL |
|---|---|---|
| Android emulator | `http://10.0.2.2:8080` | `ws://10.0.2.2:8080/ws-raw` |
| iOS simulator | `http://127.0.0.1:8080` | `ws://127.0.0.1:8080/ws-raw` |
| Physical device | `http://<computer-LAN-IP>:8080` | `ws://<computer-LAN-IP>:8080/ws-raw` |
| Production | `https://api.example.com` | `wss://api.example.com/ws-raw` |

Pass the environment at build/run time instead of editing source code:

```bash
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080
```

```dart
// lib/core/config/api_config.dart
abstract final class ApiConfig {
  static const restBaseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'http://10.0.2.2:8080',
  );

  static String get webSocketUrl {
    final uri = Uri.parse(restBaseUrl);
    return uri.replace(
      scheme: uri.scheme == 'https' ? 'wss' : 'ws',
      path: '/ws-raw',
      query: null,
      fragment: null,
    ).toString();
  }
}
```

Android requires Internet permission. Plain HTTP should only be enabled for a
local debug build:

```xml
<!-- android/app/src/main/AndroidManifest.xml -->
<uses-permission android:name="android.permission.INTERNET" />

<application
    android:usesCleartextTraffic="true"
    ...>
```

Use HTTPS/WSS in production and remove the cleartext exception. iOS may also
require an App Transport Security development exception for a plain HTTP local
server; do not ship a broad ATS exception in production.

Native Android/iOS calls are not controlled by browser CORS. The backend CORS
allowlist matters only if the project also targets Flutter Web.

## 3. Suggested Flutter folder structure

```text
lib/
├── core/
│   ├── config/
│   │   └── api_config.dart
│   ├── errors/
│   │   └── api_failure.dart
│   ├── network/
│   │   ├── api_client.dart
│   │   └── api_envelope.dart
│   └── storage/
│       └── token_store.dart
└── features/
    └── community/
        ├── data/
        │   ├── models/
        │   │   ├── friendship_models.dart
        │   │   ├── post_models.dart
        │   │   ├── engagement_models.dart
        │   │   ├── chat_models.dart
        │   │   ├── notification_models.dart
        │   │   └── moderation_models.dart
        │   ├── remote/
        │   │   ├── friendship_api.dart
        │   │   ├── post_api.dart
        │   │   ├── engagement_api.dart
        │   │   ├── chat_api.dart
        │   │   ├── notification_api.dart
        │   │   └── community_realtime_client.dart
        │   └── repositories/
        └── presentation/
            ├── friendship/
            ├── feed/
            ├── post_detail/
            ├── chat/
            └── notifications/
```

Keep raw HTTP/STOMP code in `data/remote`. Widgets should talk to state
controllers or repositories, not call Dio directly.

## 4. Secure token storage

```dart
// lib/core/storage/token_store.dart
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class TokenStore {
  static const _accessTokenKey = 'access_token';
  static const _refreshTokenKey = 'refresh_token';

  const TokenStore(this._storage);

  final FlutterSecureStorage _storage;

  Future<String?> readAccessToken() =>
      _storage.read(key: _accessTokenKey);

  Future<void> saveTokens({
    required String accessToken,
    required String refreshToken,
  }) async {
    await Future.wait([
      _storage.write(key: _accessTokenKey, value: accessToken),
      _storage.write(key: _refreshTokenKey, value: refreshToken),
    ]);
  }

  Future<void> clear() => _storage.deleteAll();
}
```

Initialize platform bindings before reading secure storage in startup code:

```dart
Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const App());
}
```

## 5. Dio client and error mapping

```dart
// lib/core/errors/api_failure.dart
import 'package:dio/dio.dart';

class ApiFailure implements Exception {
  const ApiFailure({
    required this.status,
    required this.message,
    this.retryAfterSeconds,
  });

  final int? status;
  final String message;
  final int? retryAfterSeconds;

  bool get isUnauthorized => status == 401;
  bool get isRateLimited => status == 429;

  factory ApiFailure.fromDio(DioException error) {
    final response = error.response;
    final data = response?.data;
    final body = data is Map
        ? Map<String, dynamic>.from(data)
        : const <String, dynamic>{};
    final retryAfter = response?.headers.value('retry-after');

    return ApiFailure(
      status: response?.statusCode,
      message: body['message'] as String? ??
          error.message ??
          'Unable to contact the server',
      retryAfterSeconds: int.tryParse(retryAfter ?? ''),
    );
  }

  @override
  String toString() => message;
}
```

```dart
// lib/core/network/api_client.dart
import 'package:dio/dio.dart';

import '../config/api_config.dart';
import '../errors/api_failure.dart';
import '../storage/token_store.dart';

typedef JsonMap = Map<String, dynamic>;

JsonMap jsonMap(Object? value) =>
    Map<String, dynamic>.from(value! as Map);

class ApiClient {
  ApiClient(TokenStore tokenStore)
      : _dio = Dio(BaseOptions(
          baseUrl: ApiConfig.restBaseUrl,
          connectTimeout: const Duration(seconds: 15),
          receiveTimeout: const Duration(seconds: 20),
          headers: const {'Accept': 'application/json'},
        )) {
    _dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) async {
          final token = await tokenStore.readAccessToken();
          if (token != null && token.isNotEmpty) {
            options.headers['Authorization'] = 'Bearer $token';
          }
          handler.next(options);
        },
      ),
    );
  }

  final Dio _dio;

  Future<T> get<T>(
    String path, {
    Map<String, dynamic>? query,
    required T Function(Object? data) decode,
  }) => _request(
        () => _dio.get<Object?>(path, queryParameters: query),
        decode,
      );

  Future<T> post<T>(
    String path, {
    Object? body,
    required T Function(Object? data) decode,
  }) => _request(() => _dio.post<Object?>(path, data: body), decode);

  Future<T> put<T>(
    String path, {
    Object? body,
    required T Function(Object? data) decode,
  }) => _request(() => _dio.put<Object?>(path, data: body), decode);

  Future<T> delete<T>(
    String path, {
    required T Function(Object? data) decode,
  }) => _request(() => _dio.delete<Object?>(path), decode);

  Future<T> _request<T>(
    Future<Response<Object?>> Function() request,
    T Function(Object? data) decode,
  ) async {
    try {
      final response = await request();
      final envelope = jsonMap(response.data);
      if (envelope['success'] != true) {
        throw ApiFailure(
          status: response.statusCode,
          message: envelope['message'] as String? ?? 'Request failed',
        );
      }
      return decode(envelope['data']);
    } on DioException catch (error) {
      throw ApiFailure.fromDio(error);
    }
  }
}
```

The backend may return an empty body for a failure produced directly by Spring
Security. `ApiFailure.fromDio` handles both structured error JSON and an empty
body. Do not log JWT headers or full response bodies in production.

## 6. Dart model pattern

Use `json_serializable` so backend field names remain explicit and model
updates are easy to review. Example post models:

```dart
// lib/features/community/data/models/post_models.dart
import 'package:json_annotation/json_annotation.dart';

part 'post_models.g.dart';

enum PostType {
  @JsonValue('GOAL_SUCCESS') goalSuccess,
  @JsonValue('MILESTONE') milestone,
  @JsonValue('TIP') tip,
  @JsonValue('EXPERIENCE') experience,
  @JsonValue('LIFE_UPDATE') lifeUpdate,
  @JsonValue('SHARED_POST') sharedPost,
}

enum PostVisibility {
  @JsonValue('PUBLIC') public,
  @JsonValue('FRIENDS_ONLY') friendsOnly,
  @JsonValue('PRIVATE') private,
}

enum PostStatus {
  @JsonValue('ACTIVE') active,
  @JsonValue('HIDDEN') hidden,
  @JsonValue('DELETED') deleted,
  @JsonValue('FLAGGED') flagged,
}

@JsonSerializable()
class PostAuthor {
  const PostAuthor({required this.id, this.fullName});

  final String id;
  final String? fullName;

  factory PostAuthor.fromJson(Map<String, dynamic> json) =>
      _$PostAuthorFromJson(json);
  Map<String, dynamic> toJson() => _$PostAuthorToJson(this);
}

@JsonSerializable()
class PostMedia {
  const PostMedia({
    required this.id,
    required this.mediaType,
    required this.mediaUrl,
    this.thumbnailUrl,
    this.widthPx,
    this.heightPx,
    this.durationMs,
    required this.sortOrder,
    required this.createdAt,
  });

  final String id;
  final String mediaType;
  final String mediaUrl;
  final String? thumbnailUrl;
  final int? widthPx;
  final int? heightPx;
  final int? durationMs;
  final int sortOrder;
  final DateTime createdAt;

  factory PostMedia.fromJson(Map<String, dynamic> json) =>
      _$PostMediaFromJson(json);
  Map<String, dynamic> toJson() => _$PostMediaToJson(this);
}

@JsonSerializable()
class CommunityPost {
  const CommunityPost({
    required this.id,
    required this.author,
    this.goalId,
    this.sharedPostId,
    required this.postType,
    this.content,
    required this.visibility,
    required this.status,
    required this.pinned,
    required this.ownedByCurrentUser,
    required this.media,
    required this.likeCount,
    required this.commentCount,
    required this.shareCount,
    required this.createdAt,
    required this.updatedAt,
  });

  final String id;
  final PostAuthor author;
  final String? goalId;
  final String? sharedPostId;
  final PostType postType;
  final String? content;
  final PostVisibility visibility;
  final PostStatus status;
  final bool pinned;
  final bool ownedByCurrentUser;
  final List<PostMedia> media;
  final int likeCount;
  final int commentCount;
  final int shareCount;
  final DateTime createdAt;
  final DateTime updatedAt;

  factory CommunityPost.fromJson(Map<String, dynamic> json) =>
      _$CommunityPostFromJson(json);
  Map<String, dynamic> toJson() => _$CommunityPostToJson(this);
}

@JsonSerializable()
class PostCursor {
  const PostCursor({required this.createdAt, required this.postId});

  final DateTime createdAt;
  final String postId;

  factory PostCursor.fromJson(Map<String, dynamic> json) =>
      _$PostCursorFromJson(json);
  Map<String, dynamic> toJson() => _$PostCursorToJson(this);
}

@JsonSerializable()
class PostPage {
  const PostPage({
    required this.items,
    this.nextCursor,
    required this.hasNext,
  });

  final List<CommunityPost> items;
  final PostCursor? nextCursor;
  final bool hasNext;

  factory PostPage.fromJson(Map<String, dynamic> json) =>
      _$PostPageFromJson(json);
  Map<String, dynamic> toJson() => _$PostPageToJson(this);
}
```

Generate serializers after changing models:

```bash
dart run build_runner build --delete-conflicting-outputs
```

Create the friendship, engagement, chat, notification, and moderation models
from the exact Dart-equivalent fields in the API contract. Important nullable
fields include post `author.fullName`, `goalId`, `sharedPostId`, chat
`lastMessage`, message `sender/body/editedAt/deletedAt`, and notification
`readAt`.

## 7. Posts and cursor pagination

```dart
// lib/features/community/data/remote/post_api.dart
import '../../../../core/network/api_client.dart';
import '../models/post_models.dart';

class PostApi {
  const PostApi(this._client);

  final ApiClient _client;

  Future<PostPage> getPublicFeed({
    PostCursor? cursor,
    int limit = 20,
  }) {
    return _client.get(
      '/api/community/feed/public',
      query: {
        'limit': limit,
        if (cursor != null)
          'cursorCreatedAt': cursor.createdAt.toUtc().toIso8601String(),
        if (cursor != null) 'cursorPostId': cursor.postId,
      },
      decode: (data) => PostPage.fromJson(jsonMap(data)),
    );
  }

  Future<PostPage> getFriendsFeed({
    PostCursor? cursor,
    int limit = 20,
  }) {
    return _client.get(
      '/api/community/feed/friends',
      query: {
        'limit': limit,
        if (cursor != null)
          'cursorCreatedAt': cursor.createdAt.toUtc().toIso8601String(),
        if (cursor != null) 'cursorPostId': cursor.postId,
      },
      decode: (data) => PostPage.fromJson(jsonMap(data)),
    );
  }

  Future<PostPage> getUserPosts(
    String userId, {
    PostCursor? cursor,
    int limit = 20,
  }) {
    return _client.get(
      '/api/community/users/$userId/posts',
      query: {
        'limit': limit,
        if (cursor != null)
          'cursorCreatedAt': cursor.createdAt.toUtc().toIso8601String(),
        if (cursor != null) 'cursorPostId': cursor.postId,
      },
      decode: (data) => PostPage.fromJson(jsonMap(data)),
    );
  }

  Future<CommunityPost> create(Map<String, dynamic> body) {
    return _client.post(
      '/api/community/posts',
      body: body,
      decode: (data) => CommunityPost.fromJson(jsonMap(data)),
    );
  }

  Future<CommunityPost> update(
    String postId,
    Map<String, dynamic> body,
  ) {
    return _client.put(
      '/api/community/posts/$postId',
      body: body,
      decode: (data) => CommunityPost.fromJson(jsonMap(data)),
    );
  }

  Future<void> delete(String postId) {
    return _client.delete(
      '/api/community/posts/$postId',
      decode: (_) {},
    );
  }

  Future<CommunityPost> share(
    String sourcePostId, {
    String? content,
    required String visibility,
  }) {
    return _client.post(
      '/api/community/posts/$sourcePostId/share',
      body: {'content': content, 'visibility': visibility},
      decode: (data) => CommunityPost.fromJson(jsonMap(data)),
    );
  }
}
```

Always send both cursor fields or neither. Keep one cursor per public feed,
friends feed, and profile feed. When `hasNext == false`, stop requesting and
expect `nextCursor == null`. Deduplicate appended items by post ID.

Example create payload:

```dart
await postApi.create({
  'postType': 'LIFE_UPDATE',
  'content': 'Started my emergency fund today!',
  'visibility': 'FRIENDS_ONLY',
  'goalId': null,
  'media': [
    {
      'mediaType': 'IMAGE',
      'mediaUrl': uploadedImageUrl,
      'thumbnailUrl': uploadedThumbnailUrl,
      'widthPx': 1200,
      'heightPx': 900,
      'durationMs': null,
    },
  ],
});
```

The backend accepts media metadata, not binary uploads. Upload the file first,
then submit its HTTP/HTTPS URL.

## 8. Friendship and engagement calls

The same `ApiClient` pattern applies to all endpoints:

```dart
Future<JsonMap> sendFriendRequest(ApiClient client, String userId) {
  return client.post(
    '/api/community/friend-requests',
    body: {'userId': userId},
    decode: jsonMap,
  );
}

Future<JsonMap> acceptFriendRequest(ApiClient client, String requestId) {
  return client.post(
    '/api/community/friend-requests/$requestId/accept',
    decode: jsonMap,
  );
}

Future<JsonMap> likePost(ApiClient client, String postId) {
  return client.post(
    '/api/community/posts/$postId/like',
    decode: jsonMap,
  );
}

Future<JsonMap> unlikePost(ApiClient client, String postId) {
  return client.delete(
    '/api/community/posts/$postId/like',
    decode: jsonMap,
  );
}

Future<JsonMap> addComment(
  ApiClient client,
  String postId,
  String content,
) {
  return client.post(
    '/api/community/posts/$postId/comments',
    body: {'content': content},
    decode: jsonMap,
  );
}

Future<JsonMap> replyToComment(
  ApiClient client,
  String commentId,
  String content,
) {
  return client.post(
    '/api/community/comments/$commentId/replies',
    body: {'content': content},
    decode: jsonMap,
  );
}
```

Like/unlike responses contain `liked`, `changed`, and the authoritative
`likeCount`. Replace the UI counter with the returned counter instead of only
incrementing locally. The complete friendship, block, comment, like, and report
paths are listed in the API contract.

## 9. Chat REST calls

```dart
Future<JsonMap> openDirectChat(ApiClient client, String friendUserId) {
  return client.post(
    '/api/chat/conversations/direct',
    body: {'userId': friendUserId},
    decode: jsonMap,
  );
}

Future<JsonMap> createGroupChat(
  ApiClient client, {
  required String title,
  String? imageUrl,
  required Set<String> memberIds,
}) {
  return client.post(
    '/api/chat/conversations/group',
    body: {
      'title': title,
      'imageUrl': imageUrl,
      'memberIds': memberIds.toList(),
    },
    decode: jsonMap,
  );
}

Future<JsonMap> getInbox(
  ApiClient client, {
  int page = 0,
  int size = 30,
}) {
  return client.get(
    '/api/chat/conversations',
    query: {'page': page, 'size': size},
    decode: jsonMap,
  );
}

Future<JsonMap> getMessageHistory(
  ApiClient client,
  String conversationId, {
  int page = 0,
  int size = 50,
}) {
  return client.get(
    '/api/chat/conversations/$conversationId/messages',
    query: {'page': page, 'size': size},
    decode: jsonMap,
  );
}
```

Message history is newest-first. Reverse each page for a chronological list or
prepend older pages above the currently displayed messages without changing
the server page boundaries.

Generate a `clientMessageId` once per logical outgoing message and reuse it on
network retries:

```dart
import 'package:uuid/uuid.dart';

const uuid = Uuid();
final clientMessageId = uuid.v4();

await client.post(
  '/api/chat/conversations/$conversationId/messages',
  body: {
    'clientMessageId': clientMessageId,
    'body': text,
    'replyToMessageId': replyToMessageId,
  },
  decode: jsonMap,
);
```

The new message returns HTTP `201`; an idempotent retry returns the original
message with HTTP `200`.

## 10. Flutter STOMP client

Use `/ws-raw` for Android and iOS. Authentication belongs in
`stompConnectHeaders`; the backend reads the JWT from the STOMP `CONNECT`
frame.

```dart
// lib/features/community/data/remote/community_realtime_client.dart
import 'dart:async';
import 'dart:convert';

import 'package:stomp_dart_client/stomp_dart_client.dart';

import '../../../../core/config/api_config.dart';
import '../../../../core/network/api_client.dart';

class CommunityRealtimeClient {
  StompClient? _client;

  final _messages = StreamController<JsonMap>.broadcast();
  final _receipts = StreamController<JsonMap>.broadcast();
  final _typing = StreamController<JsonMap>.broadcast();
  final _chatErrors = StreamController<JsonMap>.broadcast();
  final _notifications = StreamController<JsonMap>.broadcast();
  final _connection = StreamController<bool>.broadcast();

  Stream<JsonMap> get messages => _messages.stream;
  Stream<JsonMap> get receipts => _receipts.stream;
  Stream<JsonMap> get typing => _typing.stream;
  Stream<JsonMap> get chatErrors => _chatErrors.stream;
  Stream<JsonMap> get notifications => _notifications.stream;
  Stream<bool> get connection => _connection.stream;

  bool get isConnected => _client?.connected == true;

  void connect({required String accessToken}) {
    _client?.deactivate();

    late final StompClient client;
    client = StompClient(
      config: StompConfig(
        url: ApiConfig.webSocketUrl,
        stompConnectHeaders: {
          'Authorization': 'Bearer $accessToken',
        },
        reconnectDelay: const Duration(seconds: 5),
        heartbeatIncoming: const Duration(seconds: 10),
        heartbeatOutgoing: const Duration(seconds: 10),
        onConnect: (_) {
          _connection.add(true);
          _subscribe(client);
        },
        onDisconnect: (_) => _connection.add(false),
        onWebSocketError: (_) => _connection.add(false),
        onStompError: (frame) {
          _connection.add(false);
          _chatErrors.add({
            'event': 'STOMP_ERROR',
            'message': frame.headers['message'] ?? frame.body ?? 'STOMP error',
          });
        },
      ),
    );

    _client = client;
    client.activate();
  }

  void _subscribe(StompClient client) {
    void subscribe(String destination, StreamController<JsonMap> output) {
      client.subscribe(
        destination: destination,
        callback: (frame) {
          final body = frame.body;
          if (body != null) {
            output.add(Map<String, dynamic>.from(jsonDecode(body) as Map));
          }
        },
      );
    }

    subscribe('/user/queue/chat/messages', _messages);
    subscribe('/user/queue/chat/receipts', _receipts);
    subscribe('/user/queue/chat/typing', _typing);
    subscribe('/user/queue/chat/errors', _chatErrors);
    subscribe(
      '/user/queue/community/notifications',
      _notifications,
    );
  }

  void sendMessage({
    required String conversationId,
    required String clientMessageId,
    required String body,
    String? replyToMessageId,
  }) {
    _send('/app/chat/$conversationId/send', {
      'clientMessageId': clientMessageId,
      'body': body,
      'replyToMessageId': replyToMessageId,
    });
  }

  void markDelivered({
    required String conversationId,
    required String messageId,
  }) {
    _send('/app/chat/$conversationId/delivered', {'messageId': messageId});
  }

  void markRead({
    required String conversationId,
    required String messageId,
  }) {
    _send('/app/chat/$conversationId/read', {'messageId': messageId});
  }

  void sendTyping({
    required String conversationId,
    required bool typing,
  }) {
    _send('/app/chat/$conversationId/typing', {'typing': typing});
  }

  void _send(String destination, JsonMap body) {
    final client = _client;
    if (client == null || !client.connected) {
      throw StateError('Chat socket is not connected');
    }
    client.send(
      destination: destination,
      headers: const {'content-type': 'application/json'},
      body: jsonEncode(body),
    );
  }

  Future<void> disconnect() async {
    _connection.add(false);
    _client?.deactivate();
    _client = null;
  }

  Future<void> dispose() async {
    await disconnect();
    await Future.wait([
      _messages.close(),
      _receipts.close(),
      _typing.close(),
      _chatErrors.close(),
      _notifications.close(),
      _connection.close(),
    ]);
  }
}
```

The package's reconnect logic calls `onConnect` again, so subscriptions are
installed again after a successful reconnect. At app logout, deactivate the
client before clearing the JWT.

### Event routing

| Subscription | Event/payload |
|---|---|
| `/user/queue/chat/messages` | `MESSAGE_CREATED`, containing `message` and `duplicate` |
| `/user/queue/chat/receipts` | `MESSAGE_DELIVERED` or `MESSAGES_READ` |
| `/user/queue/chat/typing` | `TYPING` |
| `/user/queue/chat/errors` | `CHAT_ERROR` |
| `/user/queue/community/notifications` | A notification object directly |

Reconcile an optimistic message using `clientMessageId`, not the server ID.
Replace the optimistic item with the authoritative server message so the same
message is not displayed twice.

## 11. Mobile lifecycle and reconnection

Android and iOS may suspend or terminate sockets while the app is backgrounded.
WebSocket events must not be treated as permanent offline storage.

On application resume or STOMP reconnect:

1. Refetch the chat inbox.
2. Refetch the active conversation's first history page.
3. Refetch notification unread count and the first notification page.
4. Merge by ID instead of appending blindly.
5. Restart typing state as false; typing is transient.

Use `WidgetsBindingObserver.didChangeAppLifecycleState` in the application or
session controller. Whether the app keeps the connection while backgrounded is
a product choice, but the refetch-on-resume rule is still required.

## 12. Notifications

Fetch durable notification history through REST and use STOMP only for live
updates:

```dart
Future<JsonMap> getNotifications(
  ApiClient client, {
  bool unreadOnly = false,
  DateTime? cursorCreatedAt,
  String? cursorNotificationId,
  int limit = 20,
}) {
  if ((cursorCreatedAt == null) != (cursorNotificationId == null)) {
    throw ArgumentError('Both notification cursor values are required');
  }

  return client.get(
    '/api/community/notifications',
    query: {
      'unreadOnly': unreadOnly,
      'limit': limit,
      if (cursorCreatedAt != null)
        'cursorCreatedAt': cursorCreatedAt.toUtc().toIso8601String(),
      if (cursorNotificationId != null)
        'cursorNotificationId': cursorNotificationId,
    },
    decode: jsonMap,
  );
}

Future<int> getUnreadCount(ApiClient client) async {
  final data = await client.get(
    '/api/community/notifications/unread-count',
    decode: jsonMap,
  );
  return data['unreadCount'] as int;
}
```

Deduplicate REST and STOMP notifications by notification ID. Use notification
`type` plus `payload` for navigation; do not parse display text. The payload
routing keys are listed in the API contract.

## 13. State-management rules

These rules apply whether the app uses Bloc, Riverpod, Provider, or another
state-management library:

- Keep API DTOs in the data layer and expose immutable screen state.
- Keep separate feed state and cursors for public, friends, and user feeds.
- Prevent two load-more calls for the same cursor.
- Deduplicate posts, messages, and notifications by ID.
- Reconcile outgoing messages by `clientMessageId`.
- Replace like/comment/share counters with the server-returned value.
- Cancel subscriptions and HTTP requests when a controller is disposed.
- On `429`, disable the action for the `Retry-After` duration.
- On a confirmed authentication failure, stop STOMP, clear session state, then
  start the refresh/login flow. Do not log out merely because one operation
  returns `403`; it can mean a valid user lacks permission for that resource.
- Never render community text with an HTML widget; render it as plain text.

## 14. Current backend gaps affecting Flutter

Do not implement fake client endpoints to work around these gaps:

1. There is no GET endpoint for a post's comment list or a comment's replies.
2. Post/feed responses do not contain `likedByCurrentUser`.
3. The backend has no media upload endpoint; Flutter needs a separate storage
   upload contract before sending media metadata.
4. Chat sends text only. Attachment response models exist, but no public
   attachment send/upload endpoint exists.
5. Group member management, message edit/delete, and reactions are not exposed.
6. Community user discovery/search is not exposed by the community API.

The Flutter team can implement feeds, friendship, mutations, text chat, live
events, notifications, and reporting now. Comment-list rendering and restored
post-like state require the first two backend additions.

## 15. Flutter integration test checklist

- Login and persist both tokens in secure storage.
- Verify the emulator/device reaches the correct non-`localhost` development URL.
- Load public and friends feeds and fetch a second cursor page.
- Create, update, share, and delete an owned post.
- Verify a private/friends-only post is unavailable to an unauthorized account.
- Send, accept, decline, cancel, remove, block, and unblock friendship actions.
- Open a direct chat only after friendship exists.
- Send the same `clientMessageId` twice and render only one message.
- Receive message, delivery, read, typing, and error events on their queues.
- Background/resume the app and verify REST reconciliation catches missed data.
- Receive a live community notification and reconcile its unread count.
- Mark one and all notifications read.
- Handle `400`, `403`, `404`, `409`, and `429` without crashing.
- Confirm no JWT or sensitive response body appears in production logs.

Package APIs used by the examples are documented by
[`dio`](https://pub.dev/packages/dio),
[`stomp_dart_client`](https://pub.dev/packages/stomp_dart_client), and
[`flutter_secure_storage`](https://pub.dev/packages/flutter_secure_storage).
