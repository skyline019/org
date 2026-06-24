package com.skyline.org.auth.repository;

import com.skyline.org.auth.entity.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    @Modifying
    @Query("DELETE FROM LoginAttempt la WHERE la.attemptedAt < :cutoff")
    int deleteByAttemptedAtBefore(@Param("cutoff") Instant cutoff);
}
