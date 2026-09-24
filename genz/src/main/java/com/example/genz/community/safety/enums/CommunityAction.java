package com.example.genz.community.safety.enums;

public enum CommunityAction {
    CREATE_POST(10, 60),
    UPDATE_POST(20, 60),
    SHARE_POST(10, 60),
    POST_LIKE(60, 60),
    COMMENT_WRITE(30, 60),
    COMMENT_LIKE(60, 60),
    FRIEND_REQUEST(20, 3600),
    REPORT_CONTENT(10, 3600);

    private final int limit;
    private final long windowSeconds;

    CommunityAction(int limit, long windowSeconds) {
        this.limit = limit;
        this.windowSeconds = windowSeconds;
    }

    public int limit() {
        return limit;
    }

    public long windowSeconds() {
        return windowSeconds;
    }
}
