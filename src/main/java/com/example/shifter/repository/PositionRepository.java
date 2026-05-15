package com.example.shifter.repository;

import com.example.shifter.model.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


//public interface PositionRepository extends JpaRepository<Position, Long>{
//    boolean existsByName(String name);
//}


//added by Laarni
@Repository
public interface PositionRepository extends JpaRepository<Position, Long> {
    Optional<Position> findByName(String name);
}
//end
