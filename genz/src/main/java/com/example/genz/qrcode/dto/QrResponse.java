package com.example.genz.qrcode.dto;

import lombok.Data;

@Data
public class QrResponse {
    private String qrData;
    public QrResponse(String qrData) {
        this.qrData = qrData;
    }
}
