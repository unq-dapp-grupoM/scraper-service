package com.dapp.scraper_service.model.dto;

import com.dapp.scraper_service.model.Role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserValidationResponse {
    private boolean valid;
    private Integer userId;
    private String email;
    private Role role;
}