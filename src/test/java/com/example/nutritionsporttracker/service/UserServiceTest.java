package com.example.nutritionsporttracker.service;

import com.example.nutritionsporttracker.exception.EmailAlreadyExistsException;
import com.example.nutritionsporttracker.model.User;
import com.example.nutritionsporttracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(
                userRepository,
                passwordEncoder
        );
    }

    @Test
    void shouldEncodePasswordAndSaveUserWhenEmailIsAvailable() {
        User user = createUser("meral@example.com", "Password123");

        when(userRepository.existsByEmail(user.getEmail()))
                .thenReturn(false);
        when(passwordEncoder.encode("Password123"))
                .thenReturn("$2a$encoded-password");
        when(userRepository.save(user))
                .thenReturn(user);

        User savedUser = userService.registerUser(user);

        assertSame(user, savedUser);
        assertEquals("$2a$encoded-password", savedUser.getPassword());

        verify(passwordEncoder).encode("Password123");
        verify(userRepository).save(user);
    }

    @Test
    void shouldRejectRegistrationWhenEmailAlreadyExists() {
        User user = createUser("existing@example.com", "Password123");

        when(userRepository.existsByEmail(user.getEmail()))
                .thenReturn(true);

        EmailAlreadyExistsException exception = assertThrows(
                EmailAlreadyExistsException.class,
                () -> userService.registerUser(user)
        );

        assertTrue(exception.getMessage().contains(user.getEmail()));

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldNotEncodePasswordAgainWhenPasswordIsAlreadyEncoded() {
        User user = createUser(
                "encoded@example.com",
                "$2a$already-encoded"
        );

        when(userRepository.existsByEmail(user.getEmail()))
                .thenReturn(false);
        when(userRepository.save(user))
                .thenReturn(user);

        User savedUser = userService.registerUser(user);

        assertEquals(
                "$2a$already-encoded",
                savedUser.getPassword()
        );

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository).save(user);
    }

    private User createUser(String email, String password) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(password);
        return user;
    }
}
