#!/bin/bash
echo "🤖 Iniciando Simulador HL7..."
echo "📟 Monitor Philips MX450"
echo "📤 Conectando a localhost:2575"
echo ""

# Verificar Java
if ! command -v java &> /dev/null; then
    echo "❌ Error: Java 11+ no encontrado"
    exit 1
fi

# Ejecutar simulador
if [ -f "target/hl7-simulator.jar" ]; then
    java -jar target/hl7-simulator.jar
else
    echo "⚠️  JAR no encontrado, compilando..."
    mvn clean package -q
    if [ $? -eq 0 ]; then
        java -jar target/hl7-simulator.jar
    else
        echo "❌ Error en compilación"
        exit 1
    fi
fi
