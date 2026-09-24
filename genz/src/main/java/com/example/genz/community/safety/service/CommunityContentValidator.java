package com.example.genz.community.safety.service;

import com.example.genz.community.post.dto.PostMediaRequest;
import com.example.genz.community.post.exception.PostBadRequestException;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class CommunityContentValidator {

    private static final Pattern DANGEROUS_MARKUP = Pattern.compile(
            "(?i)<\\s*(script|iframe|object|embed)\\b|javascript\\s*:"
    );

    public void validatePostContent(String content) {
        validateText(content, 5_000, "Post content", true);
    }

    public void validateCommentContent(String content) {
        validateText(content, 2_000, "Comment content", false);
    }

    public void validateReportDetails(String details) {
        validateText(details, 1_000, "Report details", true);
    }

    public void validateMedia(List<PostMediaRequest> media) {
        if (media == null) {
            return;
        }
        if (media.size() > 10) {
            throw new PostBadRequestException("A post can contain at most 10 media items");
        }
        media.forEach(item -> {
            if (item == null) {
                throw new PostBadRequestException("Media items cannot be null");
            }
            validateHttpUrl(item.mediaUrl(), "mediaUrl");
            if (item.thumbnailUrl() != null && !item.thumbnailUrl().isBlank()) {
                validateHttpUrl(item.thumbnailUrl(), "thumbnailUrl");
            }
        });
    }

    private void validateText(String value, int maximumLength, String field, boolean allowBlank) {
        if (value == null) {
            return;
        }
        String trimmed = value.trim();
        if (!allowBlank && trimmed.isEmpty()) {
            throw new PostBadRequestException(field + " cannot be blank");
        }
        if (value.length() > maximumLength) {
            throw new PostBadRequestException(field + " cannot exceed " + maximumLength + " characters");
        }
        if (value.chars().anyMatch(character ->
                Character.isISOControl(character) && character != '\n' && character != '\r' && character != '\t'
        )) {
            throw new PostBadRequestException(field + " contains unsupported control characters");
        }
        if (DANGEROUS_MARKUP.matcher(value).find()) {
            throw new PostBadRequestException(field + " contains unsafe markup");
        }
    }

    private void validateHttpUrl(String value, String field) {
        if (value == null || value.isBlank() || value.length() > 2_048) {
            throw new PostBadRequestException(field + " must be a valid URL up to 2048 characters");
        }
        try {
            URI uri = new URI(value.trim());
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!(scheme.equals("http") || scheme.equals("https")) || uri.getHost() == null) {
                throw new PostBadRequestException(field + " must use an absolute HTTP or HTTPS URL");
            }
        } catch (URISyntaxException exception) {
            throw new PostBadRequestException(field + " must be a valid URL");
        }
    }
}
