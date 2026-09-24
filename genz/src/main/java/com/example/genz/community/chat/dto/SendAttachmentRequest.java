package com.example.genz.community.chat.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Getter
@Setter
public class SendAttachmentRequest {

    @NotNull
    private UUID clientMessageId;

    private UUID replyToMessageId;

    @Size(max = 10_000)
    private String body;

    @NotNull
    private MultipartFile file;
}
