package com.formacionbdi.microservicios.app.usuarios.repository;

import com.formacionbdi.microservicios.app.usuarios.models.entity.OAuthClientDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OAuthClientRepository extends JpaRepository<OAuthClientDetails, String> {
    
    @Query("SELECT o FROM OAuthClientDetails o WHERE o.active = true")
    List<OAuthClientDetails> findAllActive();
}
