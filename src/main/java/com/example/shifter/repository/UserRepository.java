package com.example.shifter.repository;

import com.example.shifter.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String name);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String name);
    boolean existsByEmail(String email);
    /**
     * Finds users whose full name contains the given text (case-insensitive).
     */
    List<User> findByFullNameContainingIgnoreCase(String fullName);

    // For employee and manager dashboard
    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.name = :roleName")
    long countUsersByRoleName(String roleName);

}
