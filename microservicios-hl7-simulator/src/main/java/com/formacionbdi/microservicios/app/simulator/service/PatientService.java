package com.formacionbdi.microservicios.app.simulator.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 🔍 SERVICIO DE BÚSQUEDA DE PACIENTES POR CAMA - JAVA 11 LTS PURO
 *
 * Conecta directamente a la base de datos bd_hdigital para:
 * - Buscar paciente hospitalizado por número de cama
 * - Obtener datos básicos para HL7 (DNI, nombre, cuenta)
 * - Validar que el paciente esté actualmente hospitalizado
 *
 * @author Alan Cairampoma
 * @version 1.0.0 - Java 11 LTS Compatible
 */
@Service
public class PatientService {

    private static final Logger logger = LoggerFactory.getLogger(PatientService.class);

    @Value("${spring.datasource.url:jdbc:postgresql://localhost:5432/bd_hdigital}")
    private String databaseUrl;

    @Value("${spring.datasource.username:postgres}")
    private String databaseUsername;

    @Value("${spring.datasource.password:123456}")
    private String databasePassword;

    /**
     * 🔍 BUSCAR PACIENTE POR NÚMERO DE CAMA
     */
    public Optional<PatientInfo> findPatientByCama(String camaNumero) {
        logger.info("🔍 Buscando paciente en cama: {}", camaNumero);

        String sql = "SELECT " +
                "p.id as paciente_id, " +
                "p.numero_doc, " +
                "p.nombres, " +
                "p.apellidos, " +
                "p.tipo_doc, " +
                "p.sexo, " +
                "p.fecha_nacimiento, " +
                "hc.numero_cuenta, " +
                "c.numero as numero_cama, " +
                "c.codigo as codigo_cama, " +
                "hm.fecha_asignacion, " +
                "hc.fecha_ingreso, " +
                "e.nombre as especialidad " +
                "FROM pacientes p " +
                "INNER JOIN hospitalizacion_cab hc ON hc.paciente_id = p.id " +
                "INNER JOIN hospitalizacion_mov hm ON hm.hospitalizacion_id = hc.id " +
                "INNER JOIN camas c ON c.id = hm.cama_destino_id " +
                "LEFT JOIN especialidades e ON e.id = hc.especialidad_id " +
                "WHERE c.codigo = ? " +
                "AND hc.estado = '01' " +
                "AND hm.estado = '01' " +
                "AND hm.fecha_liberacion IS NULL " +
                "AND p.activo = 'S' " +
                "ORDER BY hm.fecha_asignacion DESC " +
                "LIMIT 1";

        try (Connection conn = DriverManager.getConnection(databaseUrl, databaseUsername, databasePassword);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, camaNumero);
            logger.debug("📊 Ejecutando consulta SQL para cama: {}", camaNumero);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    PatientInfo patient = new PatientInfo.Builder()
                            .pacienteId(rs.getLong("paciente_id"))
                            .numeroDocumento(rs.getString("numero_doc"))
                            .nombres(rs.getString("nombres"))
                            .apellidos(rs.getString("apellidos"))
                            .tipoDocumento(rs.getString("tipo_doc"))
                            .sexo(rs.getString("sexo"))
                            .fechaNacimiento(rs.getDate("fecha_nacimiento"))
                            .numeroCuenta(rs.getString("numero_cuenta"))
                            .numeroCama(rs.getString("numero_cama"))
                            .codigoCama(rs.getString("codigo_cama"))
                            .fechaAsignacion(rs.getTimestamp("fecha_asignacion"))
                            .fechaIngreso(rs.getTimestamp("fecha_ingreso"))
                            .especialidad(rs.getString("especialidad"))
                            .build();

                    logger.info("✅ Paciente encontrado: {} {} (DNI: {}) en cama {}",
                            patient.getNombres(), patient.getApellidos(),
                            patient.getNumeroDocumento(), camaNumero);

                    return Optional.of(patient);
                } else {
                    logger.warn("❌ No se encontró paciente hospitalizado en cama: {}", camaNumero);
                    return Optional.empty();
                }
            }

        } catch (SQLException e) {
            logger.error("💥 Error ejecutando consulta de búsqueda por cama {}: {}", camaNumero, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 🔍 VERIFICAR CONEXIÓN A BASE DE DATOS
     */
    public boolean testDatabaseConnection() {
        logger.info("🔗 Probando conexión a base de datos bd_hdigital...");

        try (Connection conn = DriverManager.getConnection(databaseUrl, databaseUsername, databasePassword)) {
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as total FROM pacientes WHERE activo = 'S'")) {

                if (rs.next()) {
                    int totalPacientes = rs.getInt("total");
                    logger.info("✅ Conexión exitosa - {} pacientes activos en BD", totalPacientes);
                    return true;
                }
            }
        } catch (SQLException e) {
            logger.error("❌ Error conectando a base de datos: {}", e.getMessage());
        }

        return false;
    }

    /**
     * 📊 OBTENER ESTADÍSTICAS DE HOSPITALIZACIÓN
     */
    public HospitalizationStats getHospitalizationStats() {
        logger.debug("📊 Obteniendo estadísticas de hospitalización...");

        String sql = "SELECT " +
                "COUNT(DISTINCT hc.id) as total_hospitalizaciones, " +
                "COUNT(DISTINCT hm.cama_destino_id) as camas_ocupadas, " +
                "COUNT(DISTINCT CASE WHEN hc.fecha_ingreso > CURRENT_DATE THEN hc.id END) as ingresos_hoy, " +
                "COUNT(DISTINCT e.id) as especialidades_activas " +
                "FROM hospitalizacion_cab hc " +
                "INNER JOIN hospitalizacion_mov hm ON hm.hospitalizacion_id = hc.id " +
                "LEFT JOIN especialidades e ON e.id = hc.especialidad_id " +
                "WHERE hc.estado = '01' " +
                "AND hm.estado = '01' " +
                "AND hm.fecha_liberacion IS NULL";

        try (Connection conn = DriverManager.getConnection(databaseUrl, databaseUsername, databasePassword);
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return new HospitalizationStats.Builder()
                        .totalHospitalizaciones(rs.getInt("total_hospitalizaciones"))
                        .camasOcupadas(rs.getInt("camas_ocupadas"))
                        .ingresosHoy(rs.getInt("ingresos_hoy"))
                        .especialidadesActivas(rs.getInt("especialidades_activas"))
                        .fechaConsulta(LocalDateTime.now())
                        .build();
            }

        } catch (SQLException e) {
            logger.error("💥 Error obteniendo estadísticas: {}", e.getMessage());
        }

        return HospitalizationStats.empty();
    }

    /**
     * 🔍 BUSCAR CAMAS DISPONIBLES POR SERVICIO
     */
    public List<String> findAvailableBeds(String servicio) {
        logger.debug("🔍 Buscando camas disponibles en servicio: {}", servicio);

        String sql = "SELECT c.numero, c.codigo, p.nombre as piso_nombre " +
                "FROM camas c " +
                "INNER JOIN pisos p ON p.id = c.piso_id " +
                "LEFT JOIN hospitalizacion_mov hm ON hm.cama_destino_id = c.id " +
                "AND hm.estado = '01' " +
                "AND hm.fecha_liberacion IS NULL " +
                "WHERE c.activo = 'S' " +
                "AND hm.id IS NULL " +
                "AND (? IS NULL OR UPPER(p.nombre) LIKE UPPER(?)) " +
                "ORDER BY p.numero, c.numero " +
                "LIMIT 20";

        List<String> camasDisponibles = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(databaseUrl, databaseUsername, databasePassword);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, servicio);
            stmt.setString(2, servicio != null ? "%" + servicio + "%" : null);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String camaInfo = String.format("%s (%s) - %s",
                            rs.getString("numero"),
                            rs.getString("codigo"),
                            rs.getString("piso_nombre"));
                    camasDisponibles.add(camaInfo);
                }
            }

            logger.info("📋 Encontradas {} camas disponibles", camasDisponibles.size());

        } catch (SQLException e) {
            logger.error("💥 Error buscando camas disponibles: {}", e.getMessage());
        }

        return camasDisponibles;
    }

    // =====================================================
    // 📊 CLASE PATIENTINFO - INMUTABLE JAVA 11
    // =====================================================

    public static class PatientInfo {
        private final Long pacienteId;
        private final String numeroDocumento;
        private final String nombres;
        private final String apellidos;
        private final String tipoDocumento;
        private final String sexo;
        private final Date fechaNacimiento;
        private final String numeroCuenta;
        private final String numeroCama;
        private final String codigoCama;
        private final Timestamp fechaAsignacion;
        private final Timestamp fechaIngreso;
        private final String especialidad;

        private PatientInfo(Builder builder) {
            this.pacienteId = builder.pacienteId;
            this.numeroDocumento = builder.numeroDocumento;
            this.nombres = builder.nombres;
            this.apellidos = builder.apellidos;
            this.tipoDocumento = builder.tipoDocumento;
            this.sexo = builder.sexo;
            this.fechaNacimiento = builder.fechaNacimiento;
            this.numeroCuenta = builder.numeroCuenta;
            this.numeroCama = builder.numeroCama;
            this.codigoCama = builder.codigoCama;
            this.fechaAsignacion = builder.fechaAsignacion;
            this.fechaIngreso = builder.fechaIngreso;
            this.especialidad = builder.especialidad;
        }

        // Getters
        public Long getPacienteId() { return pacienteId; }
        public String getNumeroDocumento() { return numeroDocumento; }
        public String getNombres() { return nombres; }
        public String getApellidos() { return apellidos; }
        public String getTipoDocumento() { return tipoDocumento; }
        public String getSexo() { return sexo; }
        public Date getFechaNacimiento() { return fechaNacimiento; }
        public String getNumeroCuenta() { return numeroCuenta; }
        public String getNumeroCama() { return numeroCama; }
        public String getCodigoCama() { return codigoCama; }
        public Timestamp getFechaAsignacion() { return fechaAsignacion; }
        public Timestamp getFechaIngreso() { return fechaIngreso; }
        public String getEspecialidad() { return especialidad; }

        public String getNombreCompleto() {
            return nombres + " " + apellidos;
        }

        public String getHL7PatientId() {
            return numeroDocumento + "^^^HOSPITAL^MR";
        }

        @Override
        public String toString() {
            return String.format("PatientInfo{nombres='%s', apellidos='%s', dni='%s', cama='%s'}",
                    nombres, apellidos, numeroDocumento, numeroCama);
        }

        // Builder Pattern
        public static class Builder {
            private Long pacienteId;
            private String numeroDocumento;
            private String nombres;
            private String apellidos;
            private String tipoDocumento;
            private String sexo;
            private Date fechaNacimiento;
            private String numeroCuenta;
            private String numeroCama;
            private String codigoCama;
            private Timestamp fechaAsignacion;
            private Timestamp fechaIngreso;
            private String especialidad;

            public Builder pacienteId(Long pacienteId) {
                this.pacienteId = pacienteId;
                return this;
            }

            public Builder numeroDocumento(String numeroDocumento) {
                this.numeroDocumento = numeroDocumento;
                return this;
            }

            public Builder nombres(String nombres) {
                this.nombres = nombres;
                return this;
            }

            public Builder apellidos(String apellidos) {
                this.apellidos = apellidos;
                return this;
            }

            public Builder tipoDocumento(String tipoDocumento) {
                this.tipoDocumento = tipoDocumento;
                return this;
            }

            public Builder sexo(String sexo) {
                this.sexo = sexo;
                return this;
            }

            public Builder fechaNacimiento(Date fechaNacimiento) {
                this.fechaNacimiento = fechaNacimiento;
                return this;
            }

            public Builder numeroCuenta(String numeroCuenta) {
                this.numeroCuenta = numeroCuenta;
                return this;
            }

            public Builder numeroCama(String numeroCama) {
                this.numeroCama = numeroCama;
                return this;
            }

            public Builder codigoCama(String codigoCama) {
                this.codigoCama = codigoCama;
                return this;
            }

            public Builder fechaAsignacion(Timestamp fechaAsignacion) {
                this.fechaAsignacion = fechaAsignacion;
                return this;
            }

            public Builder fechaIngreso(Timestamp fechaIngreso) {
                this.fechaIngreso = fechaIngreso;
                return this;
            }

            public Builder especialidad(String especialidad) {
                this.especialidad = especialidad;
                return this;
            }

            public PatientInfo build() {
                return new PatientInfo(this);
            }
        }
    }

    // =====================================================
    // 📊 CLASE HOSPITALIZATIONSTATS - INMUTABLE JAVA 11
    // =====================================================

    public static class HospitalizationStats {
        private final int totalHospitalizaciones;
        private final int camasOcupadas;
        private final int ingresosHoy;
        private final int especialidadesActivas;
        private final LocalDateTime fechaConsulta;

        private HospitalizationStats(Builder builder) {
            this.totalHospitalizaciones = builder.totalHospitalizaciones;
            this.camasOcupadas = builder.camasOcupadas;
            this.ingresosHoy = builder.ingresosHoy;
            this.especialidadesActivas = builder.especialidadesActivas;
            this.fechaConsulta = builder.fechaConsulta;
        }

        public static HospitalizationStats empty() {
            return new Builder()
                    .totalHospitalizaciones(0)
                    .camasOcupadas(0)
                    .ingresosHoy(0)
                    .especialidadesActivas(0)
                    .fechaConsulta(LocalDateTime.now())
                    .build();
        }

        // Getters
        public int getTotalHospitalizaciones() { return totalHospitalizaciones; }
        public int getCamasOcupadas() { return camasOcupadas; }
        public int getIngresosHoy() { return ingresosHoy; }
        public int getEspecialidadesActivas() { return especialidadesActivas; }
        public LocalDateTime getFechaConsulta() { return fechaConsulta; }

        @Override
        public String toString() {
            return String.format("Stats{hospitalizaciones=%d, camas=%d, ingresos=%d, especialidades=%d}",
                    totalHospitalizaciones, camasOcupadas, ingresosHoy, especialidadesActivas);
        }

        // Builder Pattern
        public static class Builder {
            private int totalHospitalizaciones;
            private int camasOcupadas;
            private int ingresosHoy;
            private int especialidadesActivas;
            private LocalDateTime fechaConsulta;

            public Builder totalHospitalizaciones(int total) {
                this.totalHospitalizaciones = total;
                return this;
            }

            public Builder camasOcupadas(int camas) {
                this.camasOcupadas = camas;
                return this;
            }

            public Builder ingresosHoy(int ingresos) {
                this.ingresosHoy = ingresos;
                return this;
            }

            public Builder especialidadesActivas(int especialidades) {
                this.especialidadesActivas = especialidades;
                return this;
            }

            public Builder fechaConsulta(LocalDateTime fecha) {
                this.fechaConsulta = fecha;
                return this;
            }

            public HospitalizationStats build() {
                return new HospitalizationStats(this);
            }
        }
    }
}