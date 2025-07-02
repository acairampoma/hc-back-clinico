# ?? Microservicio HL7 - Sistema Hospitalario

Microservicio especializado en el procesamiento bidireccional de mensajes HL7 v2.5 con protocolo MLLP.

## ?? Funcionalidades

### ?? INBOUND (Recibimos)
- **Servidor HL7**: Puerto 2575 (TCP MLLP)
- **Mensajes soportados**: ORU^R01, ADT^A08, ORM^O01, MDM^T02
- **Equipos**: Monitores, Laboratorio, Radiolog赤a, HIS externos
- **Destino**: Tablas PostgreSQL espec赤ficas

### ?? OUTBOUND (Enviamos)  
- **Cliente HL7**: Env赤o a sistemas externos
- **Mensajes soportados**: ADT^A28, SIU^S13, ORM^O01, RDE^O11
- **Triggers**: Eventos en tablas del sistema
- **Destino**: HIS externos, PACS, LIS, Farmacia

### ?? Caracter赤sticas T谷cnicas
- **Base de datos**: PostgreSQL (bd_hdigital)
- **API REST**: Puerto 8004 para consultas
- **Integracion**: Eureka Discovery
- **Monitoreo**: Actuator + Prometheus
- **Testing**: TestContainers + JUnit 5

## ?? Ejecuci車n

```bash
# Compilar
mvn clean compile

# Ejecutar
mvn spring-boot:run

# O con JAR
mvn clean package
java -jar target/microservicios-hl7-0.0.1-SNAPSHOT.jar
```

## ?? Endpoints

### HL7 MLLP
- **Puerto**: 2575
- **Protocolo**: TCP con MLLP
- **Formato**: HL7 v2.5

### REST API  
- **Base URL**: http://localhost:8004
- **Health Check**: GET /actuator/health
- **Mensajes**: GET /api/hl7/messages
- **Estad赤sticas**: GET /api/hl7/stats
- **Outbound**: GET /api/hl7/outbound/queue

## ?? Configuraci車n

Ver `application.yml` para configuraci車n de:
- Puerto HL7 (2575)
- Base de datos PostgreSQL  
- Timeouts y pools de conexi車n
- Threading y procesamiento
- Logging y m谷tricas

## ?? Mensajes Soportados

### INBOUND (Recibimos)
| Mensaje | Origen | Destino | Descripci車n |
|---------|--------|---------|-------------|
| ORU^R01 | Monitores VS | hospitalizacion_signos_vitales | Signos vitales |
| ORU^R01 | Laboratorio | resultados_examenes | Resultados lab |
| ADT^A08 | HIS externo | pacientes | Actualizar datos |
| ORM^O01 | HIS externo | orden_cab | 車rdenes m谷dicas |

### OUTBOUND (Enviamos)
| Mensaje | Trigger | Destino | Descripci車n |
|---------|---------|---------|-------------|
| ADT^A28 | INSERT pacientes | HIS externo | Afiliar paciente |
| SIU^S13 | INSERT prog_citas | Sistema citas | Programar cita |
| ORM^O01 | INSERT orden_cab | Laboratorio | Orden m谷dica |
| RDE^O11 | INSERT receta_cab | Farmacia | Receta digital |

## ?? Integracion con Equipos M谷dicos

El microservicio acepta mensajes HL7 de:
- Monitores de signos vitales (Philips, GE, Mindray)
- Sistemas de laboratorio (COBAS, Architect, Sysmex)
- Equipos de radiolog赤a (Carestream, Philips, Siemens)
- Sistemas HIS externos

## ?? Monitoreo

- **Logs**: `logs/hl7-microservice.log`
- **M谷tricas**: `/actuator/metrics`
- **Health**: `/actuator/health`
- **Prometheus**: `/actuator/prometheus`

## ?? Testing

```bash
# Ejecutar tests
mvn test

# Test con cobertura
mvn test jacoco:report

# Ver reporte de cobertura
open target/site/jacoco/index.html
```
