package com.example.shifter.repository.login;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.shifter.model.login.Creds;

import java.util.Optional;

@Repository
public interface CredsRepository extends JpaRepository<Creds, Long> {
    Optional<Creds> findByUsername(String username);
}
