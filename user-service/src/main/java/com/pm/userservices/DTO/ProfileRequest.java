package com.pm.userservices.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProfileRequest {
    private String username;
    private String email;
    private String name;
}
