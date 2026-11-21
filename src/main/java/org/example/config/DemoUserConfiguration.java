package org.example.config;

import org.example.auth.model.AppUser;
import org.example.auth.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DemoUserConfiguration {
    @Bean
    CommandLineRunner createLocalDemoUser(UserRepository users, PasswordEncoder encoder) {
        return args -> users.findById("demo").orElseGet(() -> users.save(new AppUser("demo", encoder.encode("demo123"), "ADMIN")));
    }
}
