package com.formacionbdi.microservicios.app.usuarios.services;

import com.formacionbdi.microservicios.app.usuarios.models.dto.UserDto;
import com.formacionbdi.microservicios.app.usuarios.models.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserService {
    List<UserDto> findAll();
    Optional<UserDto> findById(Long id);
    Optional<UserDto> findByUsername(String username);
    Optional<UserDto> findByEmail(String email);
    UserDto save(UserDto userDto);
    void deleteById(Long id);
    List<UserDto> findAllEnabled();
    UserDto update(UserDto userDto, Long id);
}
