package com.formacionbdi.microservicios.app.listas.models.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MedicalInfo {

    @JsonProperty("primary_diagnosis")
    private String primaryDiagnosis;

    @JsonProperty("primary_diagnosis_code")
    private String primaryDiagnosisCode;

    @JsonProperty("secondary_diagnosis")
    private String secondaryDiagnosis;

    @JsonProperty("admission_date")
    private String admissionDate;

    @JsonProperty("medical_record")
    private String medicalRecord;

    @JsonProperty("admission_reason")
    private String admissionReason;

    @JsonProperty("attending_physician")
    private String attendingPhysician;

    @JsonProperty("blood_type")
    private String bloodType;

    @JsonProperty("allergies")
    private String allergies;

    @JsonProperty("current_medications")
    private List<String> currentMedications;
}
