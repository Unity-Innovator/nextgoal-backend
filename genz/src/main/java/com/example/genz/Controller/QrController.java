package com.example.genz.Controller;

import com.example.genz.DTO.QrCodeDTO.GoalWalletRequest;
import com.example.genz.DTO.QrCodeDTO.QrResponse;
import com.example.genz.Service.QrCodeService.QrService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

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
