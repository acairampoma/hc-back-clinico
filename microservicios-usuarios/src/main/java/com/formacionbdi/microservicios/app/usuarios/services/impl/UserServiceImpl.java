package com.formacionbdi.microservicios.app.usuarios.services.impl;

import com.formacionbdi.microservicios.app.usuarios.excepcion.ResourceNotFoundException;
import com.formacionbdi.microservicios.app.usuarios.models.dto.UserDto;
import com.formacionbdi.microservicios.app.usuarios.models.entity.Role;
import com.formacionbdi.microservicios.app.usuarios.models.entity.User;
import com.formacionbdi.microservicios.app.usuarios.repository.UserRepository;
import com.formacionbdi.microservicios.app.usuarios.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> findAll() {
        return userRepository.findAllWithRoles().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserDto> findById(Long id) {
        return userRepository.findByIdWithRoles(id).map(this::convertToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserDto> findByUsername(String username) {
        return userRepository.findByUsernameWithRoles(username).map(this::convertToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserDto> findByEmail(String email) {
        return userRepository.findByEmailWithRoles(email).map(this::convertToDto);
    }

    @Override
    @Transactional
    public UserDto save(UserDto userDto) {
        User user = convertToEntity(userDto);
        return convertToDto(userRepository.save(user));
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> findAllEnabled() {
        return userRepository.findAllEnabledWithRoles().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserDto update(UserDto userDto, Long id) {
        return userRepository.findById(id)
                .map(existingUser -> {
                    User user = convertToEntity(userDto);
                    user.setId(id);
                    return convertToDto(userRepository.save(user));
                })
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    private UserDto convertToDto(User user) {
        if (user == null) {
            return null;
        }

        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .password(user.getPassword())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .enabled(user.getEnabled())
                .roles(user.getRoles() != null ?
                        user.getRoles().stream()
                                .filter(Objects::nonNull)
                                .map(Role::getName)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet()) :
                        Collections.emptySet())
                .build();
    }

    private User convertToEntity(UserDto userDto) {
        User user = new User();

        // Si el DTO tiene un ID, lo establecemos
        if (userDto.getId() != null) {
            user.setId(userDto.getId());
        }

        user.setUsername(userDto.getUsername());
         user.setPassword(userDto.getPassword()); // En un caso real, deberías codificar la contraseña
        user.setEmail(userDto.getEmail());
        user.setFirstName(userDto.getFirstName());
        user.setLastName(userDto.getLastName());
        user.setEnabled(userDto.getEnabled() != null ? userDto.getEnabled() : true);

        return user;
    }
}