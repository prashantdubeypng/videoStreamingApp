package com.pm.userservices.Reprositery;

import com.pm.userservices.Model.Refresh_Token;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenReposiotory extends JpaRepository<Refresh_Token , UUID> {
    Optional<Refresh_Token> findByToken(String token);

    @Query("SELECT r FROM Refresh_Token r WHERE r.user.id = :userId ORDER BY r.createdAt ASC")
    List<Refresh_Token> findOldestToken(@Param("userId") UUID userId, Pageable pageable);
}
