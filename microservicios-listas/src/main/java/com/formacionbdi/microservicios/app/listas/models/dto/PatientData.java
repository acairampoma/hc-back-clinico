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
public class PatientData {

    @JsonProperty("personal_info")
    private PersonalInfo personalInfo;

    @JsonProperty("medical_info")
    private MedicalInfo medicalInfo;

    @JsonProperty("hospitalizacion_id")
    private Long hospitalizacionId;

    @JsonProperty("notas_medicas")
    private List<String> notasMedicas;
}
