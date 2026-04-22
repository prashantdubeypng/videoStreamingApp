package com.pm.userservices.controller;

import com.pm.userservices.DTO.AuthResponse;
import com.pm.userservices.DTO.EditProfile;
import com.pm.userservices.DTO.ProfileRequest;
import com.pm.userservices.Services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/v1/user/apis")
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    @GetMapping("/profile")
    public ResponseEntity<?> profile(Authentication authentication) {

        String username = authentication.getName();

        ProfileRequest response = userService.profile(username);

        return ResponseEntity.ok(response);
    }
    @PatchMapping("/edit-profile")
    public ResponseEntity<?> editProfile(@RequestBody EditProfile request,
                                         Authentication authentication) {

        String username = authentication.getName();

        userService.EditProfile(request.getName(), username);

        return ResponseEntity.ok("edited successfully");
    }
}
