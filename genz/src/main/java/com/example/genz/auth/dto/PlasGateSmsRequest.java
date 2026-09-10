package com.example.genz.auth.dto;


public record PlasGateSmsRequest(
    String sender,
    String to, 
    String content
) {

}
