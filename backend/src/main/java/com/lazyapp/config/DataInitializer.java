package com.lazyapp.config;

import com.lazyapp.model.User;
import com.lazyapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            User defaultUser = User.builder()
                    .name("默认用户")
                    .email("user@lazyapp.com")
                    .passwordHash("placeholder")
                    .build();
            userRepository.save(defaultUser);
            log.info("已创建默认用户: {}", defaultUser.getEmail());
        }
    }
}
