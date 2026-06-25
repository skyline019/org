package com.skyline.org.auth.repository;

import com.skyline.org.auth.entity.UserTotpCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserTotpRepository extends JpaRepository<UserTotpCredential, Long> {

    @Query("SELECT c FROM UserTotpCredential c JOIN FETCH c.user u WHERE u.username = :username")
    Optional<UserTotpCredential> findByUsername(@Param("username") String username);
}
