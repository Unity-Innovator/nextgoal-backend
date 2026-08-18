package com.example.genz.community.chat.dto;

import java.util.List;

public record ChatSliceResponse<T>(
        List<T> items,
        int page,
        int size,
        boolean hasNext
) {
}
