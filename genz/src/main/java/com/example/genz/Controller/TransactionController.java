package com.example.genz.Controller;

import com.example.genz.DTO.TransferDTO.TransferRequestDTO;
import com.example.genz.DTO.TransferDTO.TransferResponseDTO;
import com.example.genz.Service.TransactionService.TransferService;
import com.example.genz.Exception.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class TransactionController {
    @Autowired
    private TransferService transferService;
    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<TransferResponseDTO>> transfer(Authentication authentication, @RequestBody TransferRequestDTO requestDTO){
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>("success",transferService.transfer(authentication,requestDTO)));
    }
}
