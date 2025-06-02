package com.formacionbdi.microservicios.app.oauth.models.dto;

import lombok.Data;

@Data
public class RoleResponse {
    private Long id;
    private String name;
    private String code;
}
