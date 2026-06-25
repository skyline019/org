package com.skyline.org.auth.repository;

import com.skyline.org.auth.entity.UserMfaRecoveryCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserMfaRecoveryCodeRepository extends JpaRepository<UserMfaRecoveryCode, Long> {

    @Query("""
            SELECT c FROM UserMfaRecoveryCode c
            WHERE c.user.id = :userId AND c.usedAt IS NULL
            """)
    List<UserMfaRecoveryCode> findUnusedByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM UserMfaRecoveryCode c WHERE c.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    long countByUserIdAndUsedAtIsNull(Long userId);
}
