package com.example.nutritionsporttracker.controller;

import com.example.nutritionsporttracker.dto.AuthResponse;
import com.example.nutritionsporttracker.dto.RegisterRequest;
import com.example.nutritionsporttracker.model.User;
import com.example.nutritionsporttracker.security.JwtTokenProvider;
import com.example.nutritionsporttracker.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtProvider;

    public AuthController(UserService userService,
                          PasswordEncoder passwordEncoder,
                          JwtTokenProvider jwtProvider) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@Valid @RequestBody RegisterRequest registerRequest) {
        User user = new User();
        user.setEmail(registerRequest.getEmail());
        user.setPassword(registerRequest.getPassword()); // ❗ service encode edecek
        user.setFullName(registerRequest.getFullName());
        user.setGender(registerRequest.getGender());
        user.setAge(registerRequest.getAge());
        user.setWeight(registerRequest.getWeight());
        user.setHeight(registerRequest.getHeight());
        user.setActivityLevel(registerRequest.getActivityLevel());
        user.setGoal(registerRequest.getGoal());

        User savedUser = userService.registerUser(user);
        logger.info("Yeni kullanıcı kaydedildi: {}", savedUser.getEmail());
        return ResponseEntity.ok(savedUser);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User user) {
        Optional<User> foundUser = userService.findByEmail(user.getEmail());

        if (foundUser.isPresent()) {
            User u = foundUser.get();

            if (passwordEncoder.matches(user.getPassword(), u.getPassword())) {
                String token = jwtProvider.generateToken(u.getEmail());
                logger.info("Kullanıcı giriş yaptı: {}", u.getEmail());

                AuthResponse response = new AuthResponse(
                        token,
                        u.getId(),
                        u.getFullName(),
                        u.getEmail()
                );

                return ResponseEntity.ok(response);
            } else {
                logger.warn("Hatalı şifre denemesi: {}", user.getEmail());
            }
        } else {
            logger.warn("Kullanıcı bulunamadı: {}", user.getEmail());
        }

        return ResponseEntity.status(401).body("Invalid credentials");
    }
}
