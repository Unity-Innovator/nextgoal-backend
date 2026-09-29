package com.example.genz.community.chat.service;

import com.example.genz.community.chat.entity.ChatMessageAttachment;
import com.example.genz.community.chat.enums.AttachmentType;
import com.example.genz.community.chat.exception.ChatStorageException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
@Slf4j
public class SupabaseStorageService {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final int SIGNED_URL_TTL_SECONDS = 3_600;

    private com.example.genz.common.cache.BackendResponseCache cache;

    @org.springframework.beans.factory.annotation.Autowired
    public void configureCache(com.example.genz.common.cache.BackendResponseCache cache) {
        this.cache = cache;
    }

    private String signedUrlCacheKey(String storagePath) {
        return com.example.genz.common.cache.BackendResponseCache.key(
                "signed-media-url", baseUrl, bucket, storagePath);
    }

    private final String baseUrl;
    private final String secretKey;
    private final String bucket;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public SupabaseStorageService(
            @Value("${supabase.url:}") String baseUrl,
            @Value("${supabase.secret-key:}") String secretKey,
            @Value("${supabase.storage.bucket:}") String bucket,
            ObjectMapper objectMapper
    ) {
        this.baseUrl = stripTrailingSlash(baseUrl);
        this.secretKey = secretKey;
        this.bucket = bucket;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public void upload(MultipartFile file, String storagePath, AttachmentType type) {
        upload(file, storagePath, type == AttachmentType.FILE);
    }

    public void uploadPostMedia(MultipartFile file, String storagePath) {
        upload(file, storagePath, false);
    }

    public String ownedPostStoragePath(String mediaUrl, java.util.UUID authorId) {
        if (mediaUrl == null || mediaUrl.isBlank() || !isConfigured()) {
            return null;
        }
        try {
            URI mediaUri = URI.create(mediaUrl);
            URI supabaseUri = URI.create(baseUrl);
            if (!java.util.Objects.equals(mediaUri.getScheme(), supabaseUri.getScheme())
                    || !java.util.Objects.equals(mediaUri.getHost(), supabaseUri.getHost())
                    || mediaUri.getPort() != supabaseUri.getPort()) {
                return null;
            }
            String prefix = "/storage/v1/object/sign/" + encode(bucket) + "/";
            String path = mediaUri.getPath();
            if (path == null || !path.startsWith(prefix)) {
                return null;
            }
            String storagePath = path.substring(prefix.length());
            return storagePath.startsWith("posts/" + authorId + "/") ? storagePath : null;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private void upload(MultipartFile file, String storagePath, boolean downloadOnly) {
        requireConfiguration();
        try {
            HttpRequest.Builder request = HttpRequest.newBuilder(objectUri(storagePath))
                    .timeout(REQUEST_TIMEOUT)
                    .header("apikey", secretKey)
                    .header("Authorization", "Bearer " + secretKey)
                    .header("x-upsert", "false")
                    .header("cache-control", "3600")
                    .header("Content-Type", contentType(file));
            if (downloadOnly) {
                request.header("Content-Disposition", "attachment");
            }

            HttpResponse<String> response = httpClient.send(
                    request.POST(HttpRequest.BodyPublishers.ofByteArray(file.getBytes())).build(),
                    HttpResponse.BodyHandlers.ofString()
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ChatStorageException("Attachment upload failed (HTTP "
                        + response.statusCode() + ")");
            }
        } catch (IOException exception) {
            throw new ChatStorageException("Attachment upload failed", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ChatStorageException("Attachment upload was interrupted", exception);
        }
    }

    public String createSignedUrl(ChatMessageAttachment attachment) {
        if (attachment.getStoragePath() == null || attachment.getStoragePath().isBlank()) {
            return attachment.getFileUrl();
        }
        return createSignedUrl(attachment.getStoragePath());
    }

    public String createSignedUrl(String storagePath) {
        requireConfiguration();
        if (cache == null) return requestSignedUrl(storagePath);
        // URLs are valid for one hour; reuse for only five minutes to leave
        // ample time for clients to download or play the attachment.
        return cache.get(signedUrlCacheKey(storagePath), String.class,
                Duration.ofMinutes(5), () -> requestSignedUrl(storagePath));
    }

    private String requestSignedUrl(String storagePath) {
        try {
            String requestBody = objectMapper.writeValueAsString(
                    java.util.Map.of("expiresIn", SIGNED_URL_TTL_SECONDS)
            );
            HttpRequest request = HttpRequest.newBuilder(signedUrlUri(storagePath))
                    .timeout(REQUEST_TIMEOUT)
                    .header("apikey", secretKey)
                    .header("Authorization", "Bearer " + secretKey)
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ChatStorageException("Attachment URL generation failed (HTTP "
                        + response.statusCode() + ")");
            }
            JsonNode body = objectMapper.readTree(response.body());
            String signedUrl = body.path("signedURL").asText(body.path("signedUrl").asText());
            if (signedUrl.isBlank()) {
                throw new ChatStorageException("Attachment URL generation returned no URL");
            }
            return signedUrl.startsWith("http") ? signedUrl : baseUrl + "/storage/v1" + signedUrl;
        } catch (IOException exception) {
            throw new ChatStorageException("Attachment URL generation failed", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ChatStorageException("Attachment URL generation was interrupted", exception);
        }
    }

    public void deleteQuietly(String storagePath) {
        if (storagePath == null || storagePath.isBlank() || !isConfigured()) {
            return;
        }
        if (cache != null) cache.evict(signedUrlCacheKey(storagePath));
        try {
            HttpRequest request = HttpRequest.newBuilder(objectUri(storagePath))
                    .timeout(REQUEST_TIMEOUT)
                    .header("apikey", secretKey)
                    .header("Authorization", "Bearer " + secretKey)
                    .DELETE()
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Failed to remove orphaned chat attachment from storage: HTTP {}", response.statusCode());
            }
        } catch (IOException exception) {
            log.warn("Failed to remove orphaned chat attachment from storage", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while removing orphaned chat attachment from storage");
        }
    }

    private URI objectUri(String storagePath) {
        return URI.create(baseUrl + "/storage/v1/object/" + encode(bucket) + "/" + storagePath);
    }

    private URI signedUrlUri(String storagePath) {
        return URI.create(baseUrl + "/storage/v1/object/sign/" + encode(bucket) + "/" + storagePath);
    }

    private String contentType(MultipartFile file) {
        String type = file.getContentType();
        return type == null || type.isBlank() ? MediaType.APPLICATION_OCTET_STREAM_VALUE : type;
    }

    private void requireConfiguration() {
        if (!isConfigured()) {
            throw new ChatStorageException("Chat attachment storage is not configured");
        }
    }

    private boolean isConfigured() {
        return !baseUrl.isBlank() && !secretKey.isBlank() && !bucket.isBlank();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String stripTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("/+$", "");
    }
}
