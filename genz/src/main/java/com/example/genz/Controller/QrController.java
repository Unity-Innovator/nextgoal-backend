package com.example.genz.Controller;


import com.example.genz.DTO.QrCodeDTO.GoalWalletRequest;
import com.example.genz.DTO.QrCodeDTO.QrResponse;
import com.example.genz.DTO.QrCodeDTO.WalletRequest;
import com.example.genz.Service.QrCodeService.QrService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class QrController {

    @Autowired
    private QrService qrService;

    @PostMapping("/generate")
    public QrResponse generateQR(@RequestBody WalletRequest walletRequest) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String qrData = qrService.generateFakeQR(
                authentication,walletRequest
        );

        return new QrResponse(qrData);
    }

    @PostMapping("/generate-goal")
    public QrResponse generateGoalWalletQR(@Valid @RequestBody GoalWalletRequest goalWalletRequest) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String qrData = qrService.generateGoalWalletQR(
                authentication, goalWalletRequest
        );

        return new QrResponse(qrData);
    }
}

