# 🤖 Simulador de Equipo Médico HL7

Simulador que emula un **Monitor Philips MX450** enviando signos vitales vía HL7 v2.5 con protocolo MLLP.

## 🎯 Propósito

- **Demos sin hardware**: Mostrar funcionamiento HL7 sin equipos reales
- **Testing completo**: Probar servidor HL7 con datos realistas
- **Capacitación**: Entrenar personal en manejo de alertas
- **Desarrollo**: Validar integración antes de conectar equipos reales

## 🚀 Ejecución Rápida

```bash
# Compilar
mvn clean package

# Ejecutar
java -jar target/hl7-simulator.jar

# O con Maven
mvn exec:java -Dexec.mainClass="com.formacionbdi.microservicios.app.simulator.HL7SimulatorApplication"
```

## 🖥️ Interfaz Gráfica

El simulador incluye una interfaz gráfica que simula el panel de un monitor médico real:

- **Configuración del paciente** (DNI, nombre, cama)
- **Ajustes de conexión** (IP servidor, puerto)
- **Escenarios predefinidos** (Normal, Crítico, Arritmia)
- **Monitor en tiempo real** de signos vitales
- **Estado de conectividad** con servidor HL7

## 📊 Datos Simulados

### **Signos Vitales Generados:**
- **Frecuencia Cardíaca**: 60-180 lpm
- **Presión Arterial**: 90/60 - 200/120 mmHg  
- **Temperatura**: 36.0 - 40.0°C
- **Saturación O2**: 80 - 100%

### **Distribución Realista:**
- **80%** Valores normales
- **15%** Valores anormales (advertencia)
- **5%** Valores críticos (alerta)

## 📨 Mensaje HL7 Enviado

```hl7
MSH|^~\&|MONITOR_VS|HOSPITAL|HIS_SYSTEM|HOSPITAL|20250619143000||ORU^R01|MSG001234|P|2.5
PID|1||12345678^^^DNI||PEREZ^JUAN^CARLOS||19650315|M
PV1|1|I|3E-301A^301A^3E|||||||||||||||H2025-001234
OBR|1||VS001|SIGNOS VITALES||20250619143000
OBX|1|NM|HR|Frecuencia Cardiaca|78|lpm|60-100|N|||F
OBX|2|NM|SBP|Presión Sistólica|140|mmHg|90-140|H|||F
OBX|3|NM|DBP|Presión Diastólica|90|mmHg|60-90|H|||F
OBX|4|NM|TEMP|Temperatura|36.8|°C|36-37.5|N|||F
OBX|5|NM|SPO2|Saturación O2|98|%|95-100|N|||F
```

## ⚙️ Configuración

Editar `src/main/resources/application.properties`:

```properties
# Servidor HL7 destino
hl7.server.host=localhost
hl7.server.port=2575

# Intervalo de envío  
simulator.interval.seconds=30

# Paciente por defecto
patient.dni=12345678
patient.name=Juan Carlos Pérez
patient.bed=3E-301A
```

## 🎭 Escenarios Predefinidos

### **1. Normal** 
- Signos vitales estables
- Sin alertas
- Duración: 5 minutos

### **2. Crítico**
- Hipertensión severa
- Taquicardia
- Fiebre alta
- Duración: 3 minutos

### **3. Deterioro Gradual**
- Valores normales → críticos
- Simulación realista
- Duración: 10 minutos

## 🔌 Conexión con Servidor

El simulador se conecta al servidor HL7 en puerto 2575:

1. **Inicia conexión TCP**
2. **Envía mensaje ORU^R01** cada 30 segundos
3. **Espera ACK** del servidor
4. **Reintenta** en caso de error
5. **Log completo** de toda la comunicación

## 🧪 Testing

```bash
# Ejecutar tests
mvn test

# Test con cobertura
mvn test jacoco:report
```

## 📦 Distribución

```bash
# Crear JAR ejecutable
mvn clean package

# Resultado: target/hl7-simulator.jar (ejecutar en cualquier PC con Java 11+)
```

---

**🎯 Perfecto para demos, testing y capacitación sin necesidad de equipos médicos reales!**
