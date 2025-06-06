package com.formacionbdi.microservicios.app.orden.models.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrdenResponseDTO<T> {

    private boolean success;
    private String message;
    private T data;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    // MÉTODOS HELPER
    public static <T> OrdenResponseDTO<T> success(String message, T data) {
        return OrdenResponseDTO.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> OrdenResponseDTO<T> error(String message) {
        return OrdenResponseDTO.<T>builder()
                .success(false)
                .message(message)
                .data(null)
                .build();
    }
}