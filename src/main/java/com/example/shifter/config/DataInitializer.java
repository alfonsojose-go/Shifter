package com.example.shifter.config;

import com.example.shifter.model.Role;
import com.example.shifter.model.User;
import com.example.shifter.repository.RoleRepository;
import com.example.shifter.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;


@Configuration
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder; //Changed to BCryptPasswordEncoder since PasswordService uses it

    @Override
    public void run(String... args) {


        // 1️⃣ Create ADMIN role if not exists
        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseGet(() -> roleRepository.save(new Role("ADMIN")));

        // 2️⃣ Create USER role if not exists (recommended)
        roleRepository.findByName("USER")
                .orElseGet(() -> roleRepository.save(new Role("USER")));

        // 3️⃣ Create default admin user
        if (!userRepository.existsByUsername("admin")) {

            User admin = new User(
                    "System Administrator",
                    "admin",
                    passwordEncoder.encode("admin123"),
                    "admin@shifter.com"
            );

            admin.addRole(adminRole);
            userRepository.save(admin);
        }

    }
}
