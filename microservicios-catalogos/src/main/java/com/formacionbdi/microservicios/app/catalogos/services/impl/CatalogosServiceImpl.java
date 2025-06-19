package com.formacionbdi.microservicios.app.catalogos.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.formacionbdi.microservicios.app.catalogos.exception.CatalogosException;
import com.formacionbdi.microservicios.app.catalogos.exception.ResourceNotFoundException;
import com.formacionbdi.microservicios.app.catalogos.models.dto.CatalogosDTO;
import com.formacionbdi.microservicios.app.catalogos.repository.CatalogosRepository;
import com.formacionbdi.microservicios.app.catalogos.services.CatalogosService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 🔍 Implementación del servicio para gestión de catálogos
 * Puerto: 8009 - Microservicio Catálogos
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CatalogosServiceImpl implements CatalogosService {

    private final CatalogosRepository catalogosRepository;
    private final ObjectMapper objectMapper;

    @Override
    public List<CatalogosDTO> buscarPorTermino(String termino) {
        log.info("🔍 Búsqueda general por término: {}", termino);
        return convertirResultados(catalogosRepository.buscarPorTermino(termino));
    }

    @Override
    public Optional<CatalogosDTO> buscarPorCodigoExacto(String codigo) {
        log.info("🎯 Búsqueda exacta por código: {}", codigo);
        return catalogosRepository.buscarPorCodigoExacto(codigo)
                .map(this::convertirResultado);
    }

    @Override
    public List<CatalogosDTO> buscarExamenes(String termino) {
        log.info("🔬 Búsqueda en exámenes: {}", termino);
        return convertirResultados(catalogosRepository.buscarExamenes(termino));
    }

    @Override
    public List<CatalogosDTO> buscarMedicamentos(String termino) {
        log.info("💊 Búsqueda en medicamentos: {}", termino);
        return convertirResultados(catalogosRepository.buscarMedicamentos(termino));
    }

    @Override
    public List<CatalogosDTO> buscarCatalogos(String termino) {
        log.info("📊 Búsqueda en catálogos: {}", termino);
        return convertirResultados(catalogosRepository.buscarCatalogos(termino));
    }

    @Override
    public List<CatalogosDTO> buscarPorCategoria(String categoria) {
        log.info("🏷️ Búsqueda por categoría: {}", categoria);
        return convertirResultados(catalogosRepository.buscarPorCategoria(categoria));
    }

    @Override
    public List<CatalogosDTO> buscarEnTablaEspecifica(String tablaCodigo) {
        log.info("📋 Búsqueda en tabla: {}", tablaCodigo);
        return convertirResultados(catalogosRepository.buscarEnTablaEspecifica(tablaCodigo));
    }

    @Override
    public List<CatalogosDTO> obtenerTodosLosExamenes() {
        log.info("📋 Obteniendo todos los exámenes");
        return convertirResultados(catalogosRepository.obtenerTodosLosExamenes());
    }

    @Override
    public List<CatalogosDTO> obtenerTodosLosMedicamentos() {
        log.info("💊 Obteniendo todos los medicamentos");
        return convertirResultados(catalogosRepository.obtenerTodosLosMedicamentos());
    }

    @Override
    public List<String> obtenerTodasLasCategorias() {
        log.info("🏷️ Obteniendo todas las categorías");
        return catalogosRepository.obtenerTodasLasCategorias();
    }

    @Override
    public List<String> obtenerTiposDeTabla() {
        log.info("📊 Obteniendo tipos de tabla");
        return catalogosRepository.obtenerTiposDeTabla();
    }

    // ===== MÉTODOS HELPER =====

    private List<CatalogosDTO> convertirResultados(List<Object[]> resultados) {
        return resultados.stream()
                .map(this::convertirResultado)
                .collect(Collectors.toList());
    }

    private CatalogosDTO convertirResultado(Object[] row) {
        try {
            // Método helper para convertir Object a String de forma segura
            String tablaOrigen = convertToString(row[0]);
            String codigoBusqueda = convertToString(row[1]);
            String descripcionPrincipal = convertToString(row[2]);
            String categoriaPrincipal = convertToString(row[3]);
            String estado = convertToString(row[4]);
            String datosJson = convertToString(row[5]);

            return convertirJsonADto(tablaOrigen, codigoBusqueda, descripcionPrincipal,
                    categoriaPrincipal, estado, datosJson);
        } catch (Exception e) {
            log.error("Error al convertir resultado", e);
            throw new CatalogosException("Error al procesar datos del catálogo", e);
        }
    }

    @Override
    public List<CatalogosDTO> buscarEnTablaEspecifica(String tablaOrigen, String termino) {
        log.info("🎯 Búsqueda en tabla {} con término: {}", tablaOrigen, termino);
        return convertirResultados(catalogosRepository.buscarEnTablaEspecifica(tablaOrigen, termino));
    }


    private String convertToString(Object obj) {
        if (obj == null) {
            return null;
        }

        // Si ya es String, retornarlo directamente
        if (obj instanceof String) {
            return (String) obj;
        }

        // Si es Character, convertir a String
        if (obj instanceof Character) {
            return ((Character) obj).toString();
        }

        // Para cualquier otro tipo, usar toString()
        return obj.toString();
    }

    private CatalogosDTO convertirJsonADto(String tablaOrigen, String codigo, String descripcion,
                                           String categoria, String estado, String json) {
        try {
            CatalogosDTO dto = CatalogosDTO.builder()
                    .tablaOrigen(tablaOrigen)
                    .codigoBusqueda(codigo)
                    .descripcionPrincipal(descripcion)
                    .categoriaPrincipal(categoria)
                    .estado(estado)
                    .build();

            // Convertir JSON según el tipo
            if ("examenes".equals(tablaOrigen)) {
                dto.setTipoBusqueda("EXAMEN");
                dto.setExamen(objectMapper.readValue(json, CatalogosDTO.ExamenDTO.class));
            } else if ("medicamentos".equals(tablaOrigen)) {
                dto.setTipoBusqueda("MEDICAMENTO");
                dto.setMedicamento(objectMapper.readValue(json, CatalogosDTO.MedicamentoDTO.class));
            } else if (tablaOrigen.startsWith("catalogo_")) {
                dto.setTipoBusqueda("CATALOGO");
                dto.setCatalogo(objectMapper.readValue(json, CatalogosDTO.CatalogoDTO.class));
            }

            return dto;
        } catch (Exception e) {
            log.error("Error al convertir JSON: {}", json, e);
            throw new CatalogosException("Error al procesar datos JSON", e);
        }
    }
}