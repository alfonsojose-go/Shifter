package com.example.shifter.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * DTO for updating a user's password.
 */
@Getter
@Setter
public class UpdatePasswordDTO {

    private String currentPassword;
    private String newPassword;
    private String confirmNewPassword;
}