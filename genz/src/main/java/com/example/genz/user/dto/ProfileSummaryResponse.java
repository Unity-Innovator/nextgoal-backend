package com.example.genz.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileSummaryResponse {
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private BigDecimal totalSaved;
    private Long totalGoals;
    private Integer coins;
}
