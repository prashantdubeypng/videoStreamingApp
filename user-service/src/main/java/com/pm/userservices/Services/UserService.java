package com.pm.userservices.Services;

import com.pm.userservices.DTO.Signup;
import com.pm.userservices.Model.User;
import com.pm.userservices.Reprositery.RefreshTokenReposiotory;
import com.pm.userservices.Reprositery.UserRepostiory;
import com.pm.userservices.config.security.SecurityConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepostiory userrepo;
    private final RefreshTokenReposiotory refreshtokenrepo;
    private final SecurityConfig securityconfig;
// need to implement the bloom filter

    public User createUser(Signup signup){
        User user = User.builder()
                .username(signup.getUsername().toLowerCase())
                .name(signup.getName())
                .email(signup.getEmail())
                .passwordHash(securityconfig.passwordEncoder().encode(signup.getPassword()))
                .isActive(true)
                .isVerified(false)
                .build();
        try {
            return userrepo.save(user);
        } catch (DataIntegrityViolationException e) {

            String message = e.getMostSpecificCause().getMessage();

            if (message.contains("uq_users_username")) {
                throw new RuntimeException("Username already exists");
            }

            if (message.contains("uq_users_email")) {
                throw new RuntimeException("Email already exists");
            }

            throw new RuntimeException("Database error");
        }
    }
}
