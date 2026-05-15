package com.example.shifter.service;

import com.example.shifter.dto.EmployeeTableResponse;
import com.example.shifter.dto.UpdateEmployeeTableRequest;
import com.example.shifter.model.Skill;
import com.example.shifter.model.User;
import com.example.shifter.repository.ContractTypeRepository;
import com.example.shifter.repository.PositionRepository;
import com.example.shifter.repository.SkillRepository;
import com.example.shifter.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

//added by Laarni
import com.example.shifter.model.Position;
import com.example.shifter.model.ContractType;
//end

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ManageEmployeeTableServiceImpl implements ManageEmployeeTableService{
    private final UserRepository userRepository;
    private final PositionRepository positionRepository;
    private final ContractTypeRepository contractTypeRepository;
    private final SkillRepository skillRepository;

    /**
     * Returns all employees for the dashboard table.
     */
    @Override
    public List<EmployeeTableResponse> getAllEmployees() {
        return userRepository.findAll().stream()
                .map(this::mapToTableResponse)
                .toList();
    }

    /**
     * Search employee by full name.
     */
    @Override
    public EmployeeTableResponse getEmployeeByName(String name) {

        List<User> users = userRepository.findByFullNameContainingIgnoreCase(name);

        if (users.isEmpty()) {
            throw new RuntimeException("No users found");
        }

        return (EmployeeTableResponse) users.stream()
                .map(this::mapToTableResponse)
                .toList();
    }

    /**
     * Updates employee job-related information.
     */
//    @Override
//    public void updateEmployee(Long userId, UpdateEmployeeTableRequest request) {
//
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new RuntimeException("User not found"));
//
//        if (request.getPositionId() != null) {
//            user.setPosition(positionRepository.findById(request.getPositionId())
//                    .orElseThrow());
//        }
//
//        if (request.getContractTypeId() != null) {
//            user.setContractType(contractTypeRepository.findById(request.getContractTypeId())
//                    .orElseThrow());
//        }
//
//        if (request.getSkillIds() != null) {
//            Set<Skill> skills = request.getSkillIds().stream()
//                    .map(id -> skillRepository.findById(id).orElseThrow())
//                    .collect(Collectors.toSet());
//            user.setSkills(skills);
//        }
//
//        userRepository.save(user);
//    }

    //added by Laarni
    @Override
    public void updateEmployee(Long userId, UpdateEmployeeTableRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 1. Handle Position and Hourly Wage
        if (request.getPositionName() != null && !request.getPositionName().trim().isEmpty()) {
            String positionName = request.getPositionName().trim();

            // Find existing position by name, or create a new one if it doesn't exist
            Position position = positionRepository.findByName(positionName)
                    .orElseGet(() -> {
                        Position newPosition = new Position();
                        newPosition.setName(positionName);
                        return newPosition;
                    });

            // Update the hourly wage if provided by the frontend
            if (request.getHourlyWage() != null) {
                position.setHourlyWage(request.getHourlyWage());
            }

            // Save the position (This persists new positions or updates the wage of existing ones)
            position = positionRepository.save(position);
            user.setPosition(position);
        }

        // 2. Handle Contract Type
        if (request.getContractTypeName() != null && !request.getContractTypeName().trim().isEmpty()) {
            String contractName = request.getContractTypeName().trim();

            ContractType contractType = contractTypeRepository.findByName(contractName)
                    .orElseGet(() -> {
                        ContractType newContract = new ContractType();
                        newContract.setName(contractName);
                        return contractTypeRepository.save(newContract);
                    });
            user.setContractType(contractType);
        }

        // 3. Handle Skills (CSV String)
        if (request.getSkillsCsv() != null && !request.getSkillsCsv().trim().isEmpty()) {
            // Split the string by commas to get individual skills
            String[] skillNames = request.getSkillsCsv().split(",");

            Set<Skill> skills = java.util.Arrays.stream(skillNames)
                    .map(String::trim)
                    .filter(name -> !name.isEmpty())
                    // Find each skill by name, or create it if missing
                    .map(name -> skillRepository.findByName(name)
                            .orElseGet(() -> {
                                Skill newSkill = new Skill();
                                newSkill.setName(name);
                                return skillRepository.save(newSkill);
                            }))
                    .collect(Collectors.toSet());

            user.setSkills(skills);
        }

        userRepository.save(user);
    }
    //END

    /**
     * Maps User → DTO for frontend.
     */
    private EmployeeTableResponse mapToTableResponse(User user) {

        EmployeeTableResponse dto = new EmployeeTableResponse();
        dto.setUserId(user.getId());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail()); //added by Laarni Cerna

        // Safely extract the single Role String from the Set of Roles - added by Laarni Cerna
        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            // Grabs the name of the first role (Assumes Role entity uses .getName())
            String roleName = user.getRoles().iterator().next().getName();

            // Optional: If roles are saved as "ROLE_MANAGER", remove "ROLE_" for the UI
            if (roleName.startsWith("ROLE_")) {
                roleName = roleName.substring(5);
            }

            dto.setRole(roleName);
        } else {
            dto.setRole("Employee"); // Safe fallback
        }
        // END

        if (user.getPosition() != null) {
            dto.setPosition(user.getPosition().getName());
            dto.setHourlyWage(user.getPosition().getHourlyWage());
        }

        if (user.getContractType() != null) {
            dto.setContractType(user.getContractType().getName());
        }

        dto.setSkills(
                user.getSkills().stream()
                        .map(Skill::getName)
                        .collect(Collectors.toSet())
        );

        return dto;
    }

}
