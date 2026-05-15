package com.example.shifter.repository;

import com.example.shifter.model.ContractType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


//public interface ContractTypeRepository extends JpaRepository<ContractType, Long>{
//    boolean existsByName(String name);
//
//}


//added by Laarni
@Repository
public interface ContractTypeRepository extends JpaRepository<ContractType, Long> {
    Optional<ContractType> findByName(String name);
}
//end
