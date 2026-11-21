package org.example.config;

import org.example.auth.model.AppUser;
import org.example.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class DemoUserConfigurationTest {
    private final DemoUserConfiguration configuration = new DemoUserConfiguration();

    @Test
    void createsDemoUserWithEncodedPasswordWhenMissing() throws Exception {
        UserRepository users = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(users.findById("demo")).thenReturn(Optional.empty());
        when(encoder.encode("demo123")).thenReturn("encoded-password");
        when(users.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CommandLineRunner runner = configuration.createLocalDemoUser(users, encoder);
        runner.run();

        verify(encoder).encode("demo123");
        verify(users).save(argThat(user -> user.getUsername().equals("demo")
                && user.getPasswordHash().equals("encoded-password")));
    }

    @Test
    void doesNotReplaceExistingDemoUser() throws Exception {
        UserRepository users = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        AppUser existing = new AppUser("demo", "existing-hash");
        when(users.findById("demo")).thenReturn(Optional.of(existing));

        configuration.createLocalDemoUser(users, encoder).run();

        verify(users, never()).save(any());
        verifyNoInteractions(encoder);
        assertEquals("existing-hash", existing.getPasswordHash());
    }
}
