package com.example.genz.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.genz.dto.QrCodeDTO.GoalWalletRequest;
import com.example.genz.dto.QrCodeDTO.QrResponse;
import com.example.genz.service.QrCodeService.QrService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class QrController {

    private final QrService qrService;

    @PostMapping("/generate-goal")
    public QrResponse generateGoalWalletQR(@Valid @RequestBody GoalWalletRequest goalWalletRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String qrData = qrService.generateGoalWalletQR(authentication, goalWalletRequest);
        return new QrResponse(qrData);
    }
}
