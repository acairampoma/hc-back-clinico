package com.formacionbdi.microservicios.app.usuarios.controllers;

import com.formacionbdi.microservicios.app.usuarios.models.dto.OAuthClientDto;
import com.formacionbdi.microservicios.app.usuarios.services.OAuthClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/clients")
@RequiredArgsConstructor
public class OAuthClientController {
    
    private final OAuthClientService oAuthClientService;

    @GetMapping
    public ResponseEntity<List<OAuthClientDto>> findAll() {
        return ResponseEntity.ok(oAuthClientService.findAll());
    }

    @GetMapping("/{clientId}")
    public ResponseEntity<OAuthClientDto> findById(@PathVariable String clientId) {
        return oAuthClientService.findById(clientId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<OAuthClientDto> create(@Valid @RequestBody OAuthClientDto clientDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(oAuthClientService.save(clientDto));
    }

    @PutMapping("/{clientId}")
    public ResponseEntity<OAuthClientDto> update(
            @PathVariable String clientId,
            @Valid @RequestBody OAuthClientDto clientDto) {
        return ResponseEntity.ok(oAuthClientService.update(clientDto, clientId));
    }

    @DeleteMapping("/{clientId}")
    public ResponseEntity<Void> delete(@PathVariable String clientId) {
        oAuthClientService.deleteById(clientId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/active")
    public ResponseEntity<List<OAuthClientDto>> findAllActive() {
        return ResponseEntity.ok(oAuthClientService.findAllActive());
    }
}
