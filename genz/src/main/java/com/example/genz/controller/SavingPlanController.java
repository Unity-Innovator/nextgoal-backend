package com.example.genz.controller;

import com.example.genz.service.SavingPlanService.SavingPlanService;
import com.example.genz.exception.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/saving-plans")
@RequiredArgsConstructor
public class SavingPlanController {

    private final SavingPlanService savingPlanService;

    @PostMapping("/run")
    public ResponseEntity<ApiResponse<Integer>> runDueContributions() {
        int processed = savingPlanService.processDueContributions();
        return ResponseEntity.ok(ApiResponse.success("Processed due contribution plans", processed));
    }
}
