package com.example.nutritionsporttracker.service;

import com.example.nutritionsporttracker.dto.UpdateMeRequest;
import com.example.nutritionsporttracker.exception.EmailAlreadyExistsException;
import com.example.nutritionsporttracker.model.User;
import com.example.nutritionsporttracker.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // === REGISTER ===
    public User registerUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new EmailAlreadyExistsException(user.getEmail());
        }

        if (user.getPassword() != null && !user.getPassword().startsWith("$2a$")) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        return userRepository.save(user);
    }

    // === FIND ===
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // === UPDATE (LOGGED-IN USER) ===
    // Profil ekranından güncellenmesine izin verdiğimiz alanlar:
    // weight, activityLevel, goal, dailyCalories
    public User updateMeByEmail(String email, UpdateMeRequest req) {
        return userRepository.findByEmail(email)
                .map(user -> {
                    if (req.getWeight() != null) user.setWeight(req.getWeight());
                    if (req.getActivityLevel() != null) user.setActivityLevel(req.getActivityLevel());
                    if (req.getGoal() != null) user.setGoal(req.getGoal());

                    // (profilde kullanıyorsan kalsın)
                    if (req.getDailyCalories() != null) user.setDailyCalories(req.getDailyCalories());

                    return userRepository.save(user);
                })
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));
    }

    // === DELETE (OPTIONAL) ===
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("Kullanıcı bulunamadı");
        }
        userRepository.deleteById(id);
    }
}
