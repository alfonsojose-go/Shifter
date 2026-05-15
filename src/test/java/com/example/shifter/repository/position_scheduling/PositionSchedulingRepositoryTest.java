package com.example.shifter.repository.position_scheduling;

import com.example.shifter.ShifterApplication;
import com.example.shifter.dto.position_scheduling.EmployeeScheduleDTO;
import com.example.shifter.model.Position;
import com.example.shifter.model.User;
import com.example.shifter.model.scheduling.Scheduling;
import com.example.shifter.model.scheduling.Scheduling.DayOfWeek;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest(classes = ShifterApplication.class)
@ActiveProfiles("test")
@Transactional  // This automatically rolls back after each test
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD) 
@DisplayName("PositionSchedulingRepository Tests")
class PositionSchedulingRepositoryTest {

    @Autowired
    private PositionSchedulingRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        // Disable foreign key checks temporarily (MySQL only)
        entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate();

        // Delete ALL tables that reference User or other tables
        try {
            entityManager.createQuery("DELETE FROM Scheduling").executeUpdate();
        } catch (Exception e) {
        }
        try {
            entityManager.createQuery("DELETE FROM Availability").executeUpdate();
        } catch (Exception e) {
        }
        try {
            entityManager.createQuery("DELETE FROM ClockRecord").executeUpdate();
        } catch (Exception e) {
        }
        try {
            entityManager.createQuery("DELETE FROM ScheduledShift").executeUpdate();
        } catch (Exception e) {
        }
        try {
            entityManager.createQuery("DELETE FROM AvailabilityException").executeUpdate();
        } catch (Exception e) {
        }
        try {
            entityManager.createQuery("DELETE FROM Requests").executeUpdate();
        } catch (Exception e) {
        } // NEW
        try {
            entityManager.createQuery("DELETE FROM user_roles").executeUpdate();
        } catch (Exception e) {
        }
        try {
            entityManager.createNativeQuery("DELETE FROM user_roles").executeUpdate();
        } catch (Exception e) {
        }

        // Now delete User
        try {
            entityManager.createQuery("DELETE FROM User").executeUpdate();
        } catch (Exception e) {
        }

        // Finally delete Position
        try {
            entityManager.createQuery("DELETE FROM Position").executeUpdate();
        } catch (Exception e) {
        }

        // Re-enable foreign key checks
        entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();

        // Reset auto-increment counters
        try {
            entityManager.createNativeQuery("ALTER TABLE positions AUTO_INCREMENT = 1").executeUpdate();
        } catch (Exception e) {
        }
        try {
            entityManager.createNativeQuery("ALTER TABLE users AUTO_INCREMENT = 1").executeUpdate();
        } catch (Exception e) {
        }

        // Now create fresh test data
        createTestData();
    }
    
    private void createTestData() {
        // Create Positions
        Position developerPosition = new Position();
        developerPosition.setName("Developer");
        developerPosition.setHourlyWage(new BigDecimal("35.50"));
        entityManager.persist(developerPosition);

        Position managerPosition = new Position();
        managerPosition.setName("Manager");
        managerPosition.setHourlyWage(new BigDecimal("45.00"));
        entityManager.persist(managerPosition);

        Position cashierPosition = new Position();
        cashierPosition.setName("Cashier");
        cashierPosition.setHourlyWage(new BigDecimal("18.75"));
        entityManager.persist(cashierPosition);

        // Create Employees - use defaults for age and phone!
        User employee1 = new User("John Doe", "johndoe", "password123", "john.doe@example.com");
        employee1.setPosition(developerPosition);
        // age and phone use defaults (16 and (222) 222-2222)
        entityManager.persist(employee1);

        User employee2 = new User("Jane Smith", "janesmith", "password123", "jane.smith@example.com");
        employee2.setPosition(managerPosition);
        entityManager.persist(employee2);

        User employee3 = new User("Bob Johnson", "bobjohnson", "password123", "bob.johnson@example.com");
        employee3.setPosition(null); // No position
        entityManager.persist(employee3);

        User employee4 = new User("Alice Williams", "alicew", "password123", "alice.williams@example.com");
        employee4.setPosition(cashierPosition);
        entityManager.persist(employee4);

        // Create Schedules (unchanged)
        entityManager.persist(new Scheduling(employee1, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(17, 0)));
        entityManager.persist(new Scheduling(employee1, DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(17, 0)));
        entityManager.persist(new Scheduling(employee2, DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(18, 0)));
        entityManager.persist(new Scheduling(employee3, DayOfWeek.WEDNESDAY, LocalTime.of(8, 0), LocalTime.of(16, 0)));
        entityManager.persist(new Scheduling(employee4, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(16, 0)));
        entityManager.persist(new Scheduling(employee4, DayOfWeek.WEDNESDAY, LocalTime.of(8, 0), LocalTime.of(16, 0)));
        entityManager.persist(new Scheduling(employee4, DayOfWeek.FRIDAY, LocalTime.of(8, 0), LocalTime.of(16, 0)));

        entityManager.flush();
    }

    @Test
    @DisplayName("findAllWithEmployeeAndPosition - Should return all schedules")
    void findAllWithEmployeeAndPosition_ShouldReturnAllSchedules() {
        // When
        List<Scheduling> result = repository.findAllWithEmployeeAndPosition();

        // Then
        assertThat(result).hasSize(7);
        assertThat(result)
            .extracting(s -> s.getEmployee().getFullName())
            .containsExactlyInAnyOrder(
                "John Doe", "John Doe", 
                "Jane Smith", 
                "Bob Johnson", 
                "Alice Williams", "Alice Williams", "Alice Williams"
            );
    }

    @Test
    @DisplayName("findAllWithEmployeeAndPosition - Should include employees without positions")
    void findAllWithEmployeeAndPosition_ShouldIncludeEmployeesWithoutPositions() {
        // When
        List<Scheduling> result = repository.findAllWithEmployeeAndPosition();

        // Then
        List<Scheduling> employeeWithoutPositionSchedules = result.stream()
            .filter(s -> s.getEmployee().getFullName().equals("Bob Johnson"))
            .toList();
        
        assertThat(employeeWithoutPositionSchedules).hasSize(1);
        assertThat(employeeWithoutPositionSchedules.get(0).getEmployee().getPosition()).isNull();
    }

    @Test
    @DisplayName("findByEmployeeIdWithPosition - Should return DTOs for employee with multiple schedules")
    void findByEmployeeIdWithPosition_ShouldReturnDTOsForEmployeeWithMultipleSchedules() {
        // Get employee ID
        Long employeeId = entityManager.createQuery("SELECT u.id FROM User u WHERE u.username = 'johndoe'", Long.class)
            .getSingleResult();

        // When
        List<EmployeeScheduleDTO> result = repository.findByEmployeeIdWithPosition(employeeId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result)
            .extracting(
                EmployeeScheduleDTO::getFullName,
                EmployeeScheduleDTO::getPositionName,
                EmployeeScheduleDTO::getDayOfWeek
            )
            .containsExactlyInAnyOrder(
                tuple("John Doe", "Developer", DayOfWeek.MONDAY),
                tuple("John Doe", "Developer", DayOfWeek.TUESDAY)
            );
    }

    @Test
    @DisplayName("findByEmployeeIdWithPosition - Should handle employee with no position")
    void findByEmployeeIdWithPosition_ShouldHandleEmployeeWithNoPosition() {
        // Get employee ID
        Long employeeId = entityManager.createQuery("SELECT u.id FROM User u WHERE u.username = 'bobjohnson'", Long.class)
            .getSingleResult();

        // When
        List<EmployeeScheduleDTO> result = repository.findByEmployeeIdWithPosition(employeeId);

        // Then
        assertThat(result).hasSize(1);
        EmployeeScheduleDTO dto = result.get(0);
        assertThat(dto.getFullName()).isEqualTo("Bob Johnson");
        assertThat(dto.getPositionName()).isNull();
        assertThat(dto.getHourlyWage()).isNull();
        assertThat(dto.getDayOfWeek()).isEqualTo(DayOfWeek.WEDNESDAY);
    }

    @Test
    @DisplayName("findByEmployeeIdWithPosition - Should return empty list for non-existent employee")
    void findByEmployeeIdWithPosition_ShouldReturnEmptyListForNonExistentEmployee() {
        // When
        List<EmployeeScheduleDTO> result = repository.findByEmployeeIdWithPosition(999L);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByEmployeeIdWithPosition - Should return all fields correctly for cashier")
    void findByEmployeeIdWithPosition_ShouldMapAllFieldsCorrectlyForCashier() {
        // Get employee ID
        Long employeeId = entityManager.createQuery("SELECT u.id FROM User u WHERE u.username = 'alicew'", Long.class)
            .getSingleResult();

        // When
        List<EmployeeScheduleDTO> result = repository.findByEmployeeIdWithPosition(employeeId);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result)
            .allSatisfy(dto -> {
                assertThat(dto.getFullName()).isEqualTo("Alice Williams");
                assertThat(dto.getPositionName()).isEqualTo("Cashier");
                assertThat(dto.getHourlyWage()).isEqualByComparingTo(new BigDecimal("18.75"));
            });
        assertThat(result)
            .extracting(EmployeeScheduleDTO::getDayOfWeek)
            .containsExactlyInAnyOrder(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);
    }
}