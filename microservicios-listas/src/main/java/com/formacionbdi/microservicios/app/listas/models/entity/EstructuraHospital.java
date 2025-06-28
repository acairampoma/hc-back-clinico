package com.formacionbdi.microservicios.app.listas.models.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.vladmihalcea.hibernate.type.json.JsonType;
import org.hibernate.annotations.Type;

/**
 * Entidad que mapea directamente el JSON de la vista PostgreSQL
 * vista_estructura_hospital
 */
@Entity
@Table(name = "vista_estructura_hospital")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EstructuraHospital {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "estructura_completa", columnDefinition = "json")
    @Type(JsonType.class)
    @JsonProperty("estructura_completa")
    private String estructuraCompleta;

    // Constructor para mapear directamente el JSON
    public EstructuraHospital(String estructuraCompleta) {
        this.estructuraCompleta = estructuraCompleta;
    }
}