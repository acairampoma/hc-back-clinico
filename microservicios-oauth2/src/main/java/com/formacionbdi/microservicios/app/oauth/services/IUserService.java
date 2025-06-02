package com.formacionbdi.microservicios.app.oauth.services;

import com.formacionbdi.microservicios.app.oauth.models.dto.UserResponse;

public interface IUserService {
    UserResponse findByUsername(String username);
    UserResponse findById(Long id);
}
