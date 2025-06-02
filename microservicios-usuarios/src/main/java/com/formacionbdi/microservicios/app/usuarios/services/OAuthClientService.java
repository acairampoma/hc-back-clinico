package com.formacionbdi.microservicios.app.usuarios.services;

import com.formacionbdi.microservicios.app.usuarios.models.dto.OAuthClientDto;

import java.util.List;
import java.util.Optional;

public interface OAuthClientService {
    List<OAuthClientDto> findAll();
    Optional<OAuthClientDto> findById(String clientId);
    OAuthClientDto save(OAuthClientDto clientDto);
    OAuthClientDto update(OAuthClientDto clientDto, String clientId);
    void deleteById(String clientId);
    List<OAuthClientDto> findAllActive();
}
