package com.example.shifter.repository;

import com.example.shifter.model.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


//public interface SkillRepository extends JpaRepository<Skill, Long>{
//    boolean existsByName(String name);
//}


//added by Laarni
@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {
    Optional<Skill> findByName(String name);
}
//end
