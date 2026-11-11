package com.apexagro.identity.internal.controller;

import com.apexagro.identity.internal.dto.request.SendOtpRequest;
import com.apexagro.identity.internal.dto.request.VerifyOtpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @PostMapping("/otp/send")
    public ResponseEntity<Void> sendOtp(@RequestBody(required = false) SendOtpRequest request) {
        // Заглушка BE1-06 (реализация логики будет в Sprint 1: BE1-09)
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<Void> verifyOtp(@RequestBody(required = false) VerifyOtpRequest request) {
        // Заглушка BE1-06 (реализация логики будет в Sprint 1: BE1-10)
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}