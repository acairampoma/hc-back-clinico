package com.formacionbdi.microservicios.app.listas.models.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PersonalInfo {

    @JsonProperty("fullname")
    private String fullname;

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("last_name")
    private String lastName;

    @JsonProperty("age")
    private Integer age;

    @JsonProperty("gender")
    private String gender;

    @JsonProperty("dni")
    private String dni;

    @JsonProperty("phone")
    private String phone;

    @JsonProperty("medical_record")
    private String medicalRecord;

    @JsonProperty("emergency_contact")
    private String emergencyContact;
}
