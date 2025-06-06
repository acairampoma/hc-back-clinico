package com.formacionbdi.microservicios.app.catalogos.models.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Wrapper genérico para todas las respuestas de la API
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private String error;
    private LocalDateTime timestamp;
    private String path;

    // Constructor para respuestas exitosas
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message("Operación exitosa")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // Constructor para respuestas exitosas con mensaje personalizado
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // Constructor para respuestas de error
    public static <T> ApiResponse<T> error(String error) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(error)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // Constructor para respuestas de error con path
    public static <T> ApiResponse<T> error(String error, String path) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(error)
                .path(path)
                .timestamp(LocalDateTime.now())
                .build();
    }
}