package com.skyline.org.user.repository;

import com.skyline.org.testsupport.AbstractIntegrationTest;
import com.skyline.org.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class UserRepositoryTest extends AbstractIntegrationTest {

    @Autowired UserRepository userRepository;

    @Test
    void savesAndFindsUserByUsername() {
        User user = new User();
        user.setUsername("repo_" + System.nanoTime());
        user.setEmail(user.getUsername() + "@example.com");
        user.setPasswordHash("hash");
        user.setEnabled(true);
        user.setEmailVerified(true);
        userRepository.save(user);

        assertThat(userRepository.findByUsername(user.getUsername())).isPresent();
    }
}
