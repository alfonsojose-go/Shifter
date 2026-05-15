package com.example.shifter.repository;

import com.example.shifter.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface RoleRepository extends JpaRepository<Role, Long>
{
    Optional<Role> findByName(String name); //need to assign roles to users

    boolean existsByName(String name); //prevent duplicates
}


