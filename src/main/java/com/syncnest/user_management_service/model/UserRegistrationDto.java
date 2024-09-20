package com.syncnest.user_management_service.model;

import com.syncnest.user_management_service.entity.UserInfo;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.extern.flogger.Flogger;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Slf4j
@AllArgsConstructor
@NoArgsConstructor
public class UserRegistrationDto
{

    @Size(min = 3, max = 50, message = "First name must be between 3 and 50 characters")
    private String firstName;


    @Size(min = 3, max = 50, message = "Last name must be between 3 and 50 characters")
    private String lastName;

    @NotBlank
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String password;

}
