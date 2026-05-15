package com.example.shifter.repository;

import com.example.shifter.enums.RequestStatus;
import com.example.shifter.model.Request;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * This Repository is for shift change request
 */
public interface RequestRepository extends JpaRepository<Request, Long>{
    List<Request> findByUserId(Long userId);

    // -----------  For employee and manager dashboard -------------------------
    long countByUserIdAndStatus(Long userId, RequestStatus status);

    long countByStatus(RequestStatus status);

    List<Request> findTop5ByUserIdOrderByIdDesc(Long userId);

    List<Request> findTop3ByStatusOrderByIdAsc(RequestStatus status);

}
