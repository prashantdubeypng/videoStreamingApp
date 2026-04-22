package com.pm.userservices.Reprositery;

import com.pm.userservices.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface UserRepostiory extends JpaRepository<User, UUID> {
    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);
    @Modifying
    @Query("UPDATE User u SET u.name = :name WHERE u.username = :username")
    int updateNameByUsername(String username, String name);
}
