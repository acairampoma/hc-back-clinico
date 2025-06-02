package com.formacionbdi.microservicios.app.oauth.models.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Set;

@Data
public class ClientResponse {
    private String clientId;
    private String clientSecret;

    @JsonProperty("scope")  // Mapea "scope" del JSON
    private Set<String> scopes;

    @JsonProperty("authorizedGrantTypes")  // Mapea "authorizedGrantTypes" del JSON
    private Set<String> grantTypes;

    private Integer accessTokenValidity;
    private Integer refreshTokenValidity;
    private Boolean active;
}