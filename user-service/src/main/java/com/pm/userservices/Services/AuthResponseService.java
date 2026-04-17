package com.pm.userservices.Services;

import com.pm.userservices.DTO.AuthResponse;
import com.pm.userservices.DTO.Login;
import com.pm.userservices.Model.Refresh_Token;
import com.pm.userservices.Model.User;
import com.pm.userservices.Reprositery.RefreshTokenReposiotory;
import com.pm.userservices.Reprositery.UserRepostiory;
import com.pm.userservices.config.security.SecurityConfig;
import com.pm.userservices.security.JwtUtility;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthResponseService {
    private final JwtUtility jwtUtility;
    private final UserRepostiory userrepo;
    private final RefreshTokenReposiotory refreshTokenReposiotory;
    private final SecurityConfig securityconfig;
    public AuthResponse login(Login logindata , String useragent , String Ip){
        User user = userrepo.findByUsername(logindata.getUsername())
                .orElseThrow(()-> new RuntimeException("user not found"));
        if(!securityconfig.passwordEncoder().matches(logindata.getPassword(), user.getPasswordHash())){
            throw new RuntimeException("invalid user or password");
        }
        manageUserSessions(user);
        // TODO: generate JWT + refresh token
        String acessToken = jwtUtility.generateAccessToken(logindata.getUsername());
        String refreshToken = UUID.randomUUID().toString();
        String hashToken = hashToken(refreshToken);
        Refresh_Token refresh = new Refresh_Token().builder()
                .user(user)
                .deviceInfo(useragent)
                .ipAddress(Ip)
                .token(hashToken)
                .expiryAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .isRevoked(false)
                .build();
        try {
            refreshTokenReposiotory.save(refresh);
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to save refresh token, login aborted", e);
        }

        return new AuthResponse(acessToken, refreshToken);
    }
    private String hashToken(String token) {
        try{
            return Base64.getEncoder().encodeToString(
                    MessageDigest.getInstance("SHA-256").digest(token.getBytes())
            );
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
//    it is used to check the refresh token expiry date
//    public boolean isRefreshTokenExpired(String token){
//        Refresh_Token refreshtoken = refreshTokenReposiotory.findByToken(token)
//                .orElseThrow(() -> new RuntimeException("Refresh token not found"));
//        return refreshtoken.getExpiryAt().isBefore(Instant.now());
//    }
    public String refresh(String token){
        String hashrefreshtoken = hashToken(token);
        Refresh_Token data = refreshTokenReposiotory.findByToken(hashrefreshtoken)
                .orElseThrow(()-> new RuntimeException("invalid token"));
        if (data.getIsRevoked() || data.getExpiryAt().isBefore(Instant.now())) {
            throw new RuntimeException("Token expired");
        }
        return jwtUtility.generateAccessToken(data.getUser().getUsername());
    }
    public void logout(String refreshToken){
        String hashtoken = hashToken(refreshToken);
        Refresh_Token response = refreshTokenReposiotory.findByToken(hashtoken)
                .orElseThrow(()-> new RuntimeException("Server issue"));
        response.setIsRevoked(true);
    }
    public String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
    public void manageUserSessions(User user) {

        List<Refresh_Token> page =
                refreshTokenReposiotory.findOldestToken(user.getId(), PageRequest.of(0, 1));

        if (!page.isEmpty()) {
            Refresh_Token oldest = page.get(0);
            oldest.setIsRevoked(true);
            refreshTokenReposiotory.save(oldest);
        }
    }
}
