package com.example.genz.qr.controller;


import com.example.genz.qr.dto.QrResponse;
import com.example.genz.qr.dto.WalletRequest;
import com.example.genz.qr.service.QrService;
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
}

