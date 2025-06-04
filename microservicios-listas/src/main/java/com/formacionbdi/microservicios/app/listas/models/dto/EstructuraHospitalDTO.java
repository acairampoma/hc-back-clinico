package com.formacionbdi.microservicios.app.listas.models.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * DTO que mapea la estructura completa del JSON de la vista PostgreSQL
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EstructuraHospitalDTO {

    private HospitalDTO hospital;
    private List<PisoDTO> floors;

    @JsonProperty("bed_status_legend")
    private BedStatusLegendDTO bedStatusLegend;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HospitalDTO {
        private String name;
        private String address;

        @JsonProperty("total_floors")
        private Integer totalFloors;

        @JsonProperty("beds_per_wing")
        private Integer bedsPerWing;

        @JsonProperty("wings_per_floor")
        private Integer wingsPerFloor;

        @JsonProperty("total_beds")
        private Integer totalBeds;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PisoDTO {

        @JsonProperty("floor_number")
        private Integer floorNumber;

        private String specialty;

        @JsonProperty("specialty_code")
        private String specialtyCode;

        @JsonProperty("department_head")
        private String departmentHead;

        @JsonProperty("phone_extension")
        private String phoneExtension;

        @JsonProperty("color_theme")
        private String colorTheme;

        private String icon;
        private WingsDTO wings;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WingsDTO {
        private AlaDTO east;
        private AlaDTO west;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlaDTO {
        private String name;

        @JsonProperty("wing_code")
        private String wingCode;

        private List<CamaDTO> beds;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CamaDTO {

        @JsonProperty("bed_number")
        private String bedNumber;

        private String status;
        private String gender;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BedStatusLegendDTO {
        private StatusInfoDTO available;
        private StatusInfoDTO occupied;
        private StatusInfoDTO maintenance;
        private StatusInfoDTO cleaning;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusInfoDTO {
        private String label;
        private String color;

        @JsonProperty("color_male")
        private String colorMale;

        @JsonProperty("color_female")
        private String colorFemale;

        private String icon;
    }
}