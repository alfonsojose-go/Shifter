package com.example.shifter.service.impl;

import com.example.shifter.dto.RoleResponse;
import com.example.shifter.repository.RoleRepository;
import com.example.shifter.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService{
    private final RoleRepository roleRepository;

    @Override
    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll()
                .stream()
                .map(role -> new RoleResponse(role.getId(), role.getName()))
                .toList();
    }
}
