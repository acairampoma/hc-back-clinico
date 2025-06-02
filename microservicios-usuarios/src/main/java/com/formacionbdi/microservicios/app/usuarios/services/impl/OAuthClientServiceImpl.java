package com.formacionbdi.microservicios.app.usuarios.services.impl;


import com.formacionbdi.microservicios.app.usuarios.excepcion.ResourceNotFoundException;
import com.formacionbdi.microservicios.app.usuarios.models.dto.OAuthClientDto;
import com.formacionbdi.microservicios.app.usuarios.models.entity.OAuthClientDetails;
import com.formacionbdi.microservicios.app.usuarios.repository.OAuthClientRepository;
import com.formacionbdi.microservicios.app.usuarios.services.OAuthClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OAuthClientServiceImpl implements OAuthClientService {

    private final OAuthClientRepository oAuthClientRepository;

    @Override
    @Transactional(readOnly = true)
    public List<OAuthClientDto> findAll() {
        return oAuthClientRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OAuthClientDto> findById(String clientId) {
        return oAuthClientRepository.findById(clientId)
                .map(this::convertToDto);
    }

    @Override
    @Transactional
    public OAuthClientDto save(OAuthClientDto clientDto) {
        OAuthClientDetails client = convertToEntity(clientDto);
        return convertToDto(oAuthClientRepository.save(client));
    }

    @Override
    @Transactional
    public OAuthClientDto update(OAuthClientDto clientDto, String clientId) {
        return oAuthClientRepository.findById(clientId)
                .map(existingClient -> {
                    OAuthClientDetails client = convertToEntity(clientDto);
                    return convertToDto(oAuthClientRepository.save(client));
                })
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + clientId));
    }

    @Override
    @Transactional
    public void deleteById(String clientId) {
        oAuthClientRepository.deleteById(clientId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OAuthClientDto> findAllActive() {
        return oAuthClientRepository.findAllActive().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private OAuthClientDto convertToDto(OAuthClientDetails client) {
        return OAuthClientDto.builder()
                .clientId(client.getClientId())
                .clientSecret(client.getClientSecret())
                .scope(stringToSet(client.getScope()))
                .authorizedGrantTypes(stringToSet(client.getAuthorizedGrantTypes()))
                .webServerRedirectUri(stringToSet(client.getWebServerRedirectUri()))
                .authorities(stringToSet(client.getAuthorities()))
                .accessTokenValidity(client.getAccessTokenValidity())
                .refreshTokenValidity(client.getRefreshTokenValidity())
                .additionalInformation(client.getAdditionalInformation())
                .autoApprove(stringToSet(client.getAutoApprove()))
                .active(client.isActive())
                .build();
    }

    private OAuthClientDetails convertToEntity(OAuthClientDto dto) {
        return OAuthClientDetails.builder()
                .clientId(dto.getClientId())
                .clientSecret(dto.getClientSecret())
                .scope(setToString(dto.getScope()))
                .authorizedGrantTypes(setToString(dto.getAuthorizedGrantTypes()))
                .webServerRedirectUri(setToString(dto.getWebServerRedirectUri()))
                .authorities(setToString(dto.getAuthorities()))
                .accessTokenValidity(dto.getAccessTokenValidity())
                .refreshTokenValidity(dto.getRefreshTokenValidity())
                .additionalInformation(dto.getAdditionalInformation())
                .autoApprove(setToString(dto.getAutoApprove()))
                .active(dto.getActive() != null ? dto.getActive() : true)
                .build();
    }

    private Set<String> stringToSet(String str) {
        if (str == null || str.trim().isEmpty()) {
            return Set.of();
        }
        return Set.of(str.split(","));
    }

    private String setToString(Set<String> set) {
        if (set == null || set.isEmpty()) {
            return "";
        }
        return String.join(",", set);
    }
}
