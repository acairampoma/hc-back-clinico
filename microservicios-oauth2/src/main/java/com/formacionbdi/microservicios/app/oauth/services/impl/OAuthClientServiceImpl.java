package com.formacionbdi.microservicios.app.oauth.services.impl;

import com.formacionbdi.microservicios.app.oauth.clients.UsuariosClient;
import com.formacionbdi.microservicios.app.oauth.models.dto.ClientResponse;
import com.formacionbdi.microservicios.app.oauth.services.OAuthClientService;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class OAuthClientServiceImpl implements OAuthClientService {

    private static final Logger log = LoggerFactory.getLogger(OAuthClientServiceImpl.class);
    private final UsuariosClient usuariosClient;

    @Autowired
    public OAuthClientServiceImpl(UsuariosClient usuariosClient) {
        this.usuariosClient = usuariosClient;
    }

    @Override
    public Optional<ClientResponse> findByClientId(String clientId) {
        try {
            log.info("Buscando cliente OAuth: {}", clientId);

            ClientResponse client = usuariosClient.findClientByClientId(clientId);

            if (client != null && client.getActive()) {
                log.info("Cliente OAuth encontrado y activo: {}", clientId);
                return Optional.of(client);
            }

            log.warn("Cliente OAuth no encontrado o inactivo: {}", clientId);
            return Optional.empty();

        } catch (FeignException.NotFound e) {
            log.warn("Cliente OAuth no existe: {}", clientId);
            return Optional.empty();
        } catch (FeignException e) {
            log.error("Error en Feign al buscar cliente OAuth {}: {} - {}",
                    clientId, e.status(), e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error inesperado al buscar cliente OAuth {}: {}", clientId, e.getMessage());
            return Optional.empty();
        }
    }
}