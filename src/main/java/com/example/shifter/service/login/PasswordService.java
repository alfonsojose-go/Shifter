package com.example.shifter.service.login;

import com.example.shifter.model.User;
import com.example.shifter.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;


    public PasswordService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Method to hash a plain password
    public String hashPassword(String plainPassword) {
//        validation
        if (plainPassword == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }
        return passwordEncoder.encode(plainPassword);
    }

    // Method to update user's password with hash
    public void updateUserPassword(Long userId, String newPlainPassword) {

        //validation
        if (newPlainPassword == null) {
            throw new IllegalArgumentException("New password cannot be null");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String hashedPassword = passwordEncoder.encode(newPlainPassword);
        user.setPassword(hashedPassword);
        userRepository.save(user);
    }

    // Method to verify if password matches hash
    public boolean verifyPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }
        return passwordEncoder.matches(plainPassword, hashedPassword);
    }
}
