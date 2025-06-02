package com.formacionbdi.microservicios.app.oauth.services;

import com.formacionbdi.microservicios.app.oauth.models.dto.ClientResponse;
import java.util.Optional;

public interface OAuthClientService {
    Optional<ClientResponse> findByClientId(String clientId);
}