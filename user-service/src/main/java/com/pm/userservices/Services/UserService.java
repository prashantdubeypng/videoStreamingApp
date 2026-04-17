package com.pm.userservices.Services;

import com.pm.userservices.DTO.AuthResponse;
import com.pm.userservices.DTO.ProfileRequest;
import com.pm.userservices.DTO.Signup;
import com.pm.userservices.Model.User;
import com.pm.userservices.Reprositery.RefreshTokenReposiotory;
import com.pm.userservices.Reprositery.UserRepostiory;
import com.pm.userservices.config.security.SecurityConfig;
import com.pm.userservices.security.JwtUtility;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final JwtUtility jwtUtil;
    private final UserRepostiory userrepo;
    private final RefreshTokenReposiotory refreshtokenrepo;
    private final SecurityConfig securityconfig;
//TODO: need to implement the bloom filter
//    for now we are using the indexing -> we are fully depened upon the db to verify is the username is taken or not

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
    public ProfileRequest profile(String token){
        String Username = jwtUtil.extractUsername(token);
        User data = userrepo.findByUsername(Username).orElseThrow(()-> new RuntimeException(
                "username is incorrect"
        ));
        return new ProfileRequest(data.getUsername(), data.getEmail(), data.getName());
    }
    public void EditProfile(String name , String token){
        int res = userrepo.updateNameByUsername(token , name);
        if(res == 0){
            throw new RuntimeException("User Not Found");
        }
    }
}
