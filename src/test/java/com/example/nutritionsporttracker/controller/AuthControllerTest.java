package com.example.nutritionsporttracker.controller;

import com.example.nutritionsporttracker.dto.AuthResponse;
import com.example.nutritionsporttracker.dto.RegisterRequest;
import com.example.nutritionsporttracker.model.User;
import com.example.nutritionsporttracker.security.JwtTokenProvider;
import com.example.nutritionsporttracker.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthController authController;

    @Test
    void shouldRegisterUserWithRequestFields() {
        RegisterRequest request = createRegisterRequest();

        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setEmail(request.getEmail());
        savedUser.setFullName(request.getFullName());

        when(userService.registerUser(any(User.class)))
                .thenReturn(savedUser);

        ResponseEntity<User> response =
                authController.register(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(savedUser, response.getBody());

        ArgumentCaptor<User> captor =
                ArgumentCaptor.forClass(User.class);

        verify(userService).registerUser(captor.capture());

        User submittedUser = captor.getValue();

        assertEquals(request.getEmail(), submittedUser.getEmail());
        assertEquals(request.getPassword(), submittedUser.getPassword());
        assertEquals(request.getFullName(), submittedUser.getFullName());
        assertEquals(request.getAge(), submittedUser.getAge());
        assertEquals(request.getWeight(), submittedUser.getWeight());
        assertEquals(request.getHeight(), submittedUser.getHeight());
        assertEquals(
                request.getActivityLevel(),
                submittedUser.getActivityLevel()
        );
        assertEquals(request.getGoal(), submittedUser.getGoal());
    }

    @Test
    void shouldReturnTokenAndUserDataWhenLoginSucceeds() {
        User loginRequest = new User();
        loginRequest.setEmail("meral@example.com");
        loginRequest.setPassword("Password123");

        User storedUser = new User();
        storedUser.setId(42L);
        storedUser.setEmail("meral@example.com");
        storedUser.setFullName("Meral Ateş");
        storedUser.setPassword("$2a$encoded");

        when(userService.findByEmail(loginRequest.getEmail()))
                .thenReturn(Optional.of(storedUser));
        when(passwordEncoder.matches(
                loginRequest.getPassword(),
                storedUser.getPassword()
        )).thenReturn(true);
        when(jwtTokenProvider.generateToken(storedUser.getEmail()))
                .thenReturn("test-jwt-token");

        ResponseEntity<?> response =
                authController.login(loginRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        AuthResponse body = assertInstanceOf(
                AuthResponse.class,
                response.getBody()
        );

        assertEquals("test-jwt-token", body.getToken());
        assertEquals(42L, body.getUserId());
        assertEquals("Meral Ateş", body.getFullName());
        assertEquals("meral@example.com", body.getEmail());
    }

    @Test
    void shouldReturnUnauthorizedWhenPasswordIsIncorrect() {
        User loginRequest = new User();
        loginRequest.setEmail("meral@example.com");
        loginRequest.setPassword("WrongPassword");

        User storedUser = new User();
        storedUser.setEmail("meral@example.com");
        storedUser.setPassword("$2a$encoded");

        when(userService.findByEmail(loginRequest.getEmail()))
                .thenReturn(Optional.of(storedUser));
        when(passwordEncoder.matches(
                loginRequest.getPassword(),
                storedUser.getPassword()
        )).thenReturn(false);

        ResponseEntity<?> response =
                authController.login(loginRequest);

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                response.getStatusCode()
        );
        assertEquals("Invalid credentials", response.getBody());

        verify(jwtTokenProvider, never())
                .generateToken(anyString());
    }

    @Test
    void shouldReturnUnauthorizedWhenUserDoesNotExist() {
        User loginRequest = new User();
        loginRequest.setEmail("missing@example.com");
        loginRequest.setPassword("Password123");

        when(userService.findByEmail(loginRequest.getEmail()))
                .thenReturn(Optional.empty());

        ResponseEntity<?> response =
                authController.login(loginRequest);

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                response.getStatusCode()
        );

        verify(passwordEncoder, never())
                .matches(anyString(), anyString());
        verify(jwtTokenProvider, never())
                .generateToken(anyString());
    }

    private RegisterRequest createRegisterRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("meral@example.com");
        request.setPassword("Password123");
        request.setFullName("Meral Ateş");
        request.setAge(24);
        request.setWeight(60.0);
        request.setHeight(165.0);
        request.setGender("FEMALE");
        request.setActivityLevel(
                User.ActivityLevel.MODERATELY_ACTIVE
        );
        request.setGoal(User.Goal.MAINTAIN_WEIGHT);
        return request;
    }
}
