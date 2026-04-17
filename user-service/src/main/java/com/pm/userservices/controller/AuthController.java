package com.pm.userservices.controller;

import com.pm.userservices.DTO.AuthResponse;
import com.pm.userservices.DTO.Login;
import com.pm.userservices.DTO.Signup;
import com.pm.userservices.Services.AuthResponseService;
import com.pm.userservices.Services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/user/apis/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthResponseService authservice;
    private final UserService userService;
    @Value("${server.port}")
    private String port;
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody Signup signup){
        userService.createUser(signup);
        return ResponseEntity.ok("user created successfully");

    }
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Login login
    , HttpServletRequest httpRequest){
        String getIpAddress = authservice.getClientIp(httpRequest);
        String useragent = httpRequest.getHeader("User-Agent");
        AuthResponse response = authservice.login(login,useragent,getIpAddress);
        return ResponseEntity.ok(response);
    }
    @GetMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestParam AuthResponse refreshToken){
        String accessToken = authservice.refresh(refreshToken.getRefreshToken());
        return ResponseEntity.ok(accessToken);
    }
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestParam AuthResponse request){
        authservice.logout(request.getRefreshToken());
        return ResponseEntity.ok("logout successfully");
    }
    @GetMapping("/test")
    public String test() {
        return "User Service running on port: " + port;
    }

}
