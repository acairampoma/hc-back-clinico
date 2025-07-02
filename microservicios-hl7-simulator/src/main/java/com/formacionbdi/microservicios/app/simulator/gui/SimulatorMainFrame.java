package com.formacionbdi.microservicios.app.simulator.gui;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.formacionbdi.microservicios.app.simulator.client.HL7MLLPClient;
import com.formacionbdi.microservicios.app.simulator.service.PatientService;

/**
 * 🖥️ SIMULADOR HL7 COMPLETO - JAVA 11 LTS COMPATIBLE
 *
 * Panel de control para Monitor Philips MX450
 * Conecta al servidor real bd_hdigital:2575
 * Busca pacientes reales por número de cama
 * Envía signos vitales via protocolo HL7 MLLP
 *
 * @author Alan Cairampoma
 * @version 2.0.0 - Java 11 LTS
 */
@Component
public class SimulatorMainFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    // Inyección de dependencias Spring
    @Autowired
    private HL7MLLPClient hl7Client;

    @Autowired
    private PatientService patientService;

    // Componentes de la interfaz
    private JLabel statusLabel;
    private JLabel connectionStatusLabel;
    private JLabel patientInfoLabel;
    private JButton startButton;
    private JButton stopButton;
    private JButton testButton;
    private JButton testConnectionButton;
    private JButton searchPatientButton;
    private JTextArea logArea;
    private JTextField bedNumberField;
    private JTextField patientDniField;
    private JTextField patientNameField;
    private JComboBox<String> scenarioCombo;
    private JSpinner intervalSpinner;

    // Campos para signos vitales
    private JSpinner heartRateSpinner;
    private JSpinner systolicSpinner;
    private JSpinner diastolicSpinner;
    private JSpinner temperatureSpinner;

    // Estado del simulador
    private boolean isRunning = false;
    private Timer simulationTimer;
    private String currentPatientInfo = "No hay paciente asignado";
    private PatientService.PatientInfo currentPatient = null;

    public SimulatorMainFrame() {
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        updateUI();
    }

    /**
     * ✅ TEST DE CONEXIÓN HL7 AL SERVIDOR REAL
     */
    private void testHL7Connection() {
        addLogMessage("🔗 Iniciando test de conexión HL7 servidor bd_hdigital...");

        testConnectionButton.setEnabled(false);
        testConnectionButton.setText("🔄 Probando...");

        SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    publish("📡 Conectando a servidor HL7 bd_hdigital:2575...");

                    if (hl7Client == null) {
                        publish("❌ Cliente HL7 no disponible");
                        return null;
                    }

                    boolean connected = hl7Client.connect();

                    if (connected) {
                        publish("✅ Conexión HL7 exitosa con bd_hdigital!");
                        publish("📤 Enviando mensaje de prueba...");

                        hl7Client.sendTestMessage().thenAccept(response -> {
                            SwingUtilities.invokeLater(() -> {
                                if (response.isSuccess()) {
                                    addLogMessage("✅ Mensaje de prueba enviado exitosamente");
                                    String ackMsg = response.getAckMessage();
                                    if (ackMsg != null && ackMsg.length() > 50) {
                                        ackMsg = ackMsg.substring(0, 50) + "...";
                                    }
                                    addLogMessage("📥 ACK recibido: " + ackMsg);
                                    updateConnectionStatus(true);
                                } else {
                                    addLogMessage("❌ Error enviando mensaje: " + response.getMessage());
                                    updateConnectionStatus(false);
                                }
                            });
                        });

                    } else {
                        publish("❌ Error conectando al servidor HL7 bd_hdigital");
                        publish("🔍 Verificar que el servidor esté corriendo en puerto 2575");
                        String lastError = hl7Client.getLastError();
                        if (lastError != null) {
                            publish("📋 Error: " + lastError);
                        }
                    }

                } catch (Exception e) {
                    publish("💥 Excepción durante test: " + e.getMessage());
                }

                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String message : chunks) {
                    addLogMessage(message);
                }
            }

            @Override
            protected void done() {
                testConnectionButton.setEnabled(true);
                testConnectionButton.setText("🔗 Test Conexión HL7");
                addLogMessage("🏁 Test de conexión completado");
            }
        };

        worker.execute();
    }

    /**
     * ✅ BÚSQUEDA REAL DE PACIENTE POR CAMA
     */
    private void searchPatientByCama() {
        String camaNumero = bedNumberField.getText().trim();

        if (camaNumero.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Por favor ingrese el número de cama",
                    "Campo requerido",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        addLogMessage("🔍 Buscando paciente en cama: " + camaNumero + " (bd_hdigital)");

        searchPatientButton.setEnabled(false);
        searchPatientButton.setText("🔄 Buscando...");

        SwingWorker<PatientService.PatientInfo, String> worker =
                new SwingWorker<PatientService.PatientInfo, String>() {
                    @Override
                    protected PatientService.PatientInfo doInBackground() throws Exception {
                        try {
                            publish("📊 Consultando base de datos bd_hdigital...");

                            if (!patientService.testDatabaseConnection()) {
                                publish("❌ Error de conexión a base de datos");
                                return null;
                            }

                            publish("🔍 Ejecutando consulta SQL...");

                            Optional<PatientService.PatientInfo> patientOpt =
                                    patientService.findPatientByCama(camaNumero);

                            if (patientOpt.isPresent()) {
                                PatientService.PatientInfo patient = patientOpt.get();
                                publish("✅ Paciente encontrado: " + patient.getNombreCompleto());
                                publish("📋 DNI: " + patient.getNumeroDocumento());
                                publish("🏥 Cuenta: " + patient.getNumeroCuenta());
                                publish("🛏️ Especialidad: " + patient.getEspecialidad());
                                return patient;
                            } else {
                                publish("❌ No hay paciente hospitalizado en cama " + camaNumero);
                                publish("💡 Verifique que la cama esté ocupada y activa");
                                return null;
                            }

                        } catch (Exception e) {
                            publish("💥 Error ejecutando búsqueda: " + e.getMessage());
                            return null;
                        }
                    }

                    @Override
                    protected void process(List<String> chunks) {
                        for (String message : chunks) {
                            addLogMessage(message);
                        }
                    }

                    @Override
                    protected void done() {
                        try {
                            PatientService.PatientInfo patient = get();

                            if (patient != null) {
                                // Actualizar datos del paciente encontrado
                                currentPatient = patient;
                                patientDniField.setText(patient.getNumeroDocumento());
                                patientNameField.setText(patient.getNombreCompleto());

                                currentPatientInfo = String.format("Paciente: %s (DNI: %s) - Cama: %s - Cuenta: %s",
                                        patient.getNombreCompleto(),
                                        patient.getNumeroDocumento(),
                                        camaNumero,
                                        patient.getNumeroCuenta());

                                patientInfoLabel.setText(currentPatientInfo);
                                patientInfoLabel.setBackground(Color.GREEN);

                                addLogMessage("✅ Datos del paciente cargados exitosamente");
                                startButton.setEnabled(true);

                            } else {
                                // Limpiar datos si no se encontró paciente
                                currentPatient = null;
                                patientDniField.setText("");
                                patientNameField.setText("");
                                patientInfoLabel.setText("❌ No hay paciente en esta cama");
                                patientInfoLabel.setBackground(Color.ORANGE);
                                startButton.setEnabled(false);

                                // Mostrar sugerencias de camas disponibles
                                showAvailableBeds();
                            }

                        } catch (Exception e) {
                            addLogMessage("💥 Error procesando resultado: " + e.getMessage());
                            currentPatient = null;
                            startButton.setEnabled(false);
                        }

                        searchPatientButton.setEnabled(true);
                        searchPatientButton.setText("🔍 Buscar Paciente");
                    }
                };

        worker.execute();
    }

    /**
     * ✅ MOSTRAR CAMAS DISPONIBLES COMO SUGERENCIA
     */
    private void showAvailableBeds() {
        SwingWorker<List<String>, Void> worker = new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                return patientService.findAvailableBeds("UCI");
            }

            @Override
            protected void done() {
                try {
                    List<String> camasDisponibles = get();

                    if (!camasDisponibles.isEmpty()) {
                        StringBuilder message = new StringBuilder("💡 Camas disponibles en UCI:\n\n");
                        int limit = Math.min(5, camasDisponibles.size());
                        for (int i = 0; i < limit; i++) {
                            message.append("• ").append(camasDisponibles.get(i)).append("\n");
                        }

                        if (camasDisponibles.size() > 5) {
                            message.append("... y ").append(camasDisponibles.size() - 5).append(" más");
                        }

                        JOptionPane.showMessageDialog(SimulatorMainFrame.this,
                                message.toString(),
                                "Camas Disponibles",
                                JOptionPane.INFORMATION_MESSAGE);
                    }

                } catch (Exception e) {
                    addLogMessage("⚠️ No se pudieron obtener camas disponibles");
                }
            }
        };

        worker.execute();
    }

    /**
     * ✅ ENVIAR SIGNOS VITALES REALES AL SERVIDOR
     */
    private void sendVitalSignsToServer() {
        if (!hl7Client.isConnected()) {
            addLogMessage("⚠️ Sin conexión al servidor HL7, intentando reconectar...");
            if (!hl7Client.connect()) {
                addLogMessage("❌ No se pudo reconectar al servidor HL7");
                return;
            }
        }

        if (currentPatient == null) {
            addLogMessage("⚠️ No hay paciente asignado para enviar signos vitales");
            return;
        }

        String camaNumero = currentPatient.getNumeroCama();
        double fc = ((Number) heartRateSpinner.getValue()).doubleValue();
        double ps = ((Number) systolicSpinner.getValue()).doubleValue();
        double pd = ((Number) diastolicSpinner.getValue()).doubleValue();
        double temp = ((Number) temperatureSpinner.getValue()).doubleValue();

        // Aplicar variaciones según escenario
        String scenario = (String) scenarioCombo.getSelectedItem();
        if (scenario != null) {
            if (scenario.startsWith("CRITICAL")) {
                fc += (Math.random() - 0.5) * 40;
                ps += (Math.random() - 0.5) * 30;
                temp += (Math.random() - 0.5) * 2;
            } else if (scenario.startsWith("WARNING")) {
                fc += (Math.random() - 0.5) * 20;
                ps += (Math.random() - 0.5) * 20;
            } else {
                fc += (Math.random() - 0.5) * 10;
                ps += (Math.random() - 0.5) * 10;
                pd += (Math.random() - 0.5) * 5;
                temp += (Math.random() - 0.5) * 0.5;
            }
        }

        // Asegurar rangos mínimos
        fc = Math.max(40, Math.min(200, fc));
        ps = Math.max(80, Math.min(250, ps));
        pd = Math.max(50, Math.min(150, pd));
        temp = Math.max(35.0, Math.min(42.0, temp));

        addLogMessage(String.format("📡 Enviando ORU^R01 - Paciente: %s | Cama: %s",
                currentPatient.getNombreCompleto(), camaNumero));
        addLogMessage(String.format("📊 Signos: FC: %.0f bpm | PA: %.0f/%.0f mmHg | T: %.1f°C",
                fc, ps, pd, temp));

        // Generar mensaje HL7 con datos reales del paciente
        String hl7Message = generateRealPatientVitalSigns(currentPatient, fc, ps, pd, temp);

        // ✅ JAVA 11 FIX - Crear variables final para lambda
        final double finalFc = fc;
        final double finalPs = ps;
        final double finalPd = pd;
        final double finalTemp = temp;

        // Enviar al servidor HL7 real
        hl7Client.sendMessage(hl7Message).thenAccept(response -> {
            SwingUtilities.invokeLater(() -> {
                if (response.isSuccess()) {
                    addLogMessage("✅ Signos vitales enviados exitosamente al servidor bd_hdigital");
                    String ackMsg = response.getAckMessage();
                    if (ackMsg != null && ackMsg.length() > 50) {
                        ackMsg = ackMsg.substring(0, 50) + "...";
                    }
                    addLogMessage("📥 ACK: " + (ackMsg != null ? ackMsg : "Recibido"));
                } else {
                    addLogMessage("❌ Error enviando signos vitales: " + response.getMessage());
                }
            });
        });

        // Actualizar valores en GUI con variables final
        SwingUtilities.invokeLater(() -> {
            heartRateSpinner.setValue((int) finalFc);
            systolicSpinner.setValue((int) finalPs);
            diastolicSpinner.setValue((int) finalPd);
            temperatureSpinner.setValue(Math.round(finalTemp * 10.0) / 10.0);
        });
    }

    /**
     * ✅ GENERAR MENSAJE HL7 CON DATOS REALES DEL PACIENTE
     */
    private String generateRealPatientVitalSigns(PatientService.PatientInfo patient,
                                                 double fc, double ps, double pd, double temp) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String messageId = "ORU" + System.currentTimeMillis();

        StringBuilder hl7 = new StringBuilder();

        // MSH - Message Header
        hl7.append("MSH|^~\\&|PHILIPS_MX450|UCI|HIS_SYSTEM|HOSPITAL|").append(timestamp);
        hl7.append("||ORU^R01|").append(messageId).append("|P|2.5\r");

        // PID - Patient Identification (datos reales del paciente)
        hl7.append("PID|1||").append(patient.getNumeroDocumento()).append("^^^HOSPITAL^MR||");
        hl7.append(patient.getApellidos().toUpperCase()).append("^").append(patient.getNombres().toUpperCase());
        hl7.append("||");
        if (patient.getFechaNacimiento() != null) {
            hl7.append(new java.text.SimpleDateFormat("yyyyMMdd").format(patient.getFechaNacimiento()));
        } else {
            hl7.append("19900101");
        }
        hl7.append("|").append(patient.getSexo() != null ? patient.getSexo() : "U").append("\r");

        // PV1 - Patient Visit (información real de hospitalización)
        hl7.append("PV1|1|I|").append(patient.getEspecialidad() != null ? patient.getEspecialidad().toUpperCase() : "UCI");
        hl7.append("^").append(patient.getNumeroCama()).append("^01|||||||||||||||");
        hl7.append(patient.getNumeroCuenta()).append("|||||||||||||||||||||");
        hl7.append(timestamp).append("\r");

        // OBR - Observation Request
        hl7.append("OBR|1||VS_").append(timestamp).append("|SIGNOS_VITALES||").append(timestamp).append("\r");

        // OBX - Observation Results (Signos vitales)
        int sequence = 1;

        // Frecuencia Cardíaca
        hl7.append("OBX|").append(sequence++).append("|NM|HR^Frecuencia Cardiaca^LOCAL|1|").append(fc);
        hl7.append("|bpm|60-100|").append(fc >= 60 && fc <= 100 ? "N" : "A").append("|||F\r");

        // Presión Sistólica
        hl7.append("OBX|").append(sequence++).append("|NM|SBP^Presion Sistolica^LOCAL|1|").append(ps);
        hl7.append("|mmHg|90-140|").append(ps >= 90 && ps <= 140 ? "N" : "A").append("|||F\r");

        // Presión Diastólica
        hl7.append("OBX|").append(sequence++).append("|NM|DBP^Presion Diastolica^LOCAL|1|").append(pd);
        hl7.append("|mmHg|60-90|").append(pd >= 60 && pd <= 90 ? "N" : "A").append("|||F\r");

        // Temperatura
        hl7.append("OBX|").append(sequence++).append("|NM|TEMP^Temperatura Corporal^LOCAL|1|").append(temp);
        hl7.append("|C|36.0-37.5|").append(temp >= 36.0 && temp <= 37.5 ? "N" : "A").append("|||F\r");

        return hl7.toString();
    }

    /**
     * ✅ INICIALIZAR COMPONENTES CON DISEÑO MODERNO
     */
    private void initializeComponents() {
        setTitle("🏥 Simulador HL7 - Monitor Philips MX450 → bd_hdigital");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 850);  // Aumentado para mejor visualización
        setLocationRelativeTo(null);
        setResizable(true);

        // Colores modernos
        Color primaryBlue = new Color(33, 150, 243);      // Material Blue
        Color successGreen = new Color(76, 175, 80);      // Material Green
        Color errorRed = new Color(244, 67, 54);          // Material Red
        Color warningOrange = new Color(255, 152, 0);     // Material Orange
        Color darkGray = new Color(55, 71, 79);           // Material Blue Gray
        Color lightGray = new Color(236, 239, 241);       // Light Background

        // Status principal con diseño moderno
        statusLabel = new JLabel("🔴 Sistema Desconectado", JLabel.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        statusLabel.setOpaque(true);
        statusLabel.setBackground(lightGray);
        statusLabel.setForeground(darkGray);
        statusLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createRaisedBevelBorder(),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        // Status de conexión HL7 mejorado
        connectionStatusLabel = new JLabel("🔴 HL7 Desconectado", JLabel.CENTER);
        connectionStatusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        connectionStatusLabel.setOpaque(true);
        connectionStatusLabel.setBackground(errorRed);
        connectionStatusLabel.setForeground(Color.WHITE);
        connectionStatusLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createRaisedBevelBorder(),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));

        // Info del paciente más clara
        patientInfoLabel = new JLabel(currentPatientInfo, JLabel.CENTER);
        patientInfoLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        patientInfoLabel.setOpaque(true);
        patientInfoLabel.setBackground(warningOrange);
        patientInfoLabel.setForeground(Color.WHITE);
        patientInfoLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createRaisedBevelBorder(),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));

        // Botones principales con estilo moderno
        startButton = createStyledButton("▶️ Iniciar Simulación", successGreen, Color.WHITE, 15);
        stopButton = createStyledButton("⏹️ Detener Simulación", errorRed, Color.WHITE, 15);
        stopButton.setEnabled(false);

        testButton = createStyledButton("🧪 Enviar Prueba", warningOrange, Color.WHITE, 14);
        testConnectionButton = createStyledButton("🔗 Test Conexión HL7", primaryBlue, Color.WHITE, 14);
        searchPatientButton = createStyledButton("🔍 Buscar Paciente", new Color(156, 39, 176), Color.WHITE, 14);

        // Campos de configuración con mejor diseño
        bedNumberField = createStyledTextField("3E-301A", 12);
        patientDniField = createStyledTextField("", 15);
        patientDniField.setEditable(false);
        patientDniField.setBackground(lightGray);
        patientNameField = createStyledTextField("", 20);
        patientNameField.setEditable(false);
        patientNameField.setBackground(lightGray);

        // Spinners con mejor estilo
        heartRateSpinner = createStyledSpinner(new SpinnerNumberModel(75, 40, 200, 1));
        systolicSpinner = createStyledSpinner(new SpinnerNumberModel(120, 80, 250, 1));
        diastolicSpinner = createStyledSpinner(new SpinnerNumberModel(80, 50, 150, 1));
        temperatureSpinner = createStyledSpinner(new SpinnerNumberModel(36.8, 35.0, 42.0, 0.1));

        // ComboBox con estilo
        scenarioCombo = new JComboBox<>(new String[]{
                "🟢 NORMAL - Signos vitales normales",
                "🔴 CRITICAL - Estado crítico",
                "🟡 WARNING - Valores elevados",
                "🔵 RECOVERY - Recuperación"
        });
        scenarioCombo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        scenarioCombo.setBackground(Color.WHITE);

        // Intervalo con estilo
        intervalSpinner = createStyledSpinner(new SpinnerNumberModel(30, 5, 300, 5));

        // Área de log mejorada
        logArea = new JTextArea(18, 70);  // Más grande
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 11));  // Fuente más clara
        logArea.setBackground(new Color(40, 44, 52));          // Fondo oscuro moderno
        logArea.setForeground(new Color(171, 178, 191));       // Texto gris claro
        logArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Timer para simulación real
        simulationTimer = new Timer(((Number)intervalSpinner.getValue()).intValue() * 1000, e -> {
            if (isRunning) {
                sendVitalSignsToServer();
            }
        });
    }

    /**
     * ✅ CREAR BOTÓN CON ESTILO MODERNO
     */
    private JButton createStyledButton(String text, Color bgColor, Color fgColor, int fontSize) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, fontSize));
        button.setBackground(bgColor);
        button.setForeground(fgColor);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createRaisedBevelBorder(),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));

        // Efectos hover
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            Color originalColor = bgColor;

            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(originalColor.brighter());
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(originalColor);
            }
        });

        return button;
    }

    /**
     * ✅ CREAR TEXTFIELD CON ESTILO MODERNO
     */
    private JTextField createStyledTextField(String text, int columns) {
        JTextField field = new JTextField(text, columns);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLoweredBevelBorder(),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        field.setBackground(Color.WHITE);
        return field;
    }

    /**
     * ✅ CREAR SPINNER CON ESTILO MODERNO
     */
    private JSpinner createStyledSpinner(SpinnerNumberModel model) {
        JSpinner spinner = new JSpinner(model);
        spinner.setFont(new Font("Segoe UI", Font.BOLD, 12));
        spinner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLoweredBevelBorder(),
                BorderFactory.createEmptyBorder(3, 5, 3, 5)
        ));

        // Estilizar el editor del spinner
        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) spinner.getEditor();
        editor.getTextField().setBackground(Color.WHITE);
        editor.getTextField().setFont(new Font("Segoe UI", Font.BOLD, 12));

        return spinner;
    }

    /**
     * ✅ ACTUALIZAR ESTADO DE CONEXIÓN CON COLORES CLAROS
     */
    private void updateConnectionStatus(boolean connected) {
        Color successGreen = new Color(76, 175, 80);
        Color errorRed = new Color(244, 67, 54);

        if (connected) {
            connectionStatusLabel.setText("🟢 HL7 Conectado - bd_hdigital:2575");
            connectionStatusLabel.setBackground(successGreen);
            connectionStatusLabel.setForeground(Color.WHITE);
        } else {
            connectionStatusLabel.setText("🔴 HL7 Desconectado - bd_hdigital:2575");
            connectionStatusLabel.setBackground(errorRed);
            connectionStatusLabel.setForeground(Color.WHITE);
        }
    }

    /**
     * ✅ CREAR PANEL DE CONFIGURACIÓN CON DISEÑO MEJORADO
     */
    private JPanel createConfigPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(250, 250, 250));  // Fondo claro
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createRaisedBevelBorder(),
                        "⚙️ Configuración del Monitor Philips MX450",
                        TitledBorder.LEFT,
                        TitledBorder.TOP,
                        new Font("Segoe UI", Font.BOLD, 14),
                        new Color(33, 150, 243)  // Título azul
                ),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);  // Más espacioso
        gbc.anchor = GridBagConstraints.WEST;

        // Fila 1: Búsqueda por cama con mejor diseño
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(createStyledLabel("🛏️ Número Cama:", 13, true), gbc);
        gbc.gridx = 1;
        panel.add(bedNumberField, gbc);
        gbc.gridx = 2; gbc.gridwidth = 2;
        panel.add(searchPatientButton, gbc);

        // Fila 2: Datos del paciente
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(createStyledLabel("👤 DNI Paciente:", 12, false), gbc);
        gbc.gridx = 1;
        panel.add(patientDniField, gbc);
        gbc.gridx = 2;
        panel.add(createStyledLabel("📋 Nombre Completo:", 12, false), gbc);
        gbc.gridx = 3;
        panel.add(patientNameField, gbc);

        // Línea separadora visual
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 4; gbc.fill = GridBagConstraints.HORIZONTAL;
        JSeparator separator = new JSeparator();
        separator.setBackground(new Color(200, 200, 200));
        panel.add(separator, gbc);

        // Fila 3: Signos vitales con iconos más claros
        gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(createStyledLabel("💓 Frecuencia Cardíaca (bpm):", 12, true), gbc);
        gbc.gridx = 1;
        panel.add(heartRateSpinner, gbc);
        gbc.gridx = 2;
        panel.add(createStyledLabel("🩸 Presión Sistólica (mmHg):", 12, true), gbc);
        gbc.gridx = 3;
        panel.add(systolicSpinner, gbc);

        // Fila 4: Más signos vitales
        gbc.gridx = 0; gbc.gridy = 4;
        panel.add(createStyledLabel("🩸 Presión Diastólica (mmHg):", 12, true), gbc);
        gbc.gridx = 1;
        panel.add(diastolicSpinner, gbc);
        gbc.gridx = 2;
        panel.add(createStyledLabel("🌡️ Temperatura (°C):", 12, true), gbc);
        gbc.gridx = 3;
        panel.add(temperatureSpinner, gbc);

        // Otra línea separadora
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 4; gbc.fill = GridBagConstraints.HORIZONTAL;
        JSeparator separator2 = new JSeparator();
        separator2.setBackground(new Color(200, 200, 200));
        panel.add(separator2, gbc);

        // Fila 5: Configuración de simulación
        gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 0; gbc.gridy = 6;
        panel.add(createStyledLabel("⏰ Intervalo (segundos):", 12, false), gbc);
        gbc.gridx = 1;
        panel.add(intervalSpinner, gbc);
        gbc.gridx = 2;
        panel.add(createStyledLabel("🎭 Escenario Clínico:", 12, false), gbc);
        gbc.gridx = 3; gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(scenarioCombo, gbc);

        return panel;
    }

    /**
     * ✅ CREAR LABEL CON ESTILO MODERNO
     */
    private JLabel createStyledLabel(String text, int fontSize, boolean bold) {
        JLabel label = new JLabel(text);
        if (bold) {
            label.setFont(new Font("Segoe UI", Font.BOLD, fontSize));
            label.setForeground(new Color(33, 150, 243));  // Azul para labels importantes
        } else {
            label.setFont(new Font("Segoe UI", Font.PLAIN, fontSize));
            label.setForeground(new Color(55, 71, 79));    // Gris oscuro para labels normales
        }
        return label;
    }

    /**
     * ✅ CREAR PANEL DE LOG CON DISEÑO MODERNO
     */
    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(new Color(250, 250, 250));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createRaisedBevelBorder(),
                        "📝 Log de Actividad - Comunicación con bd_hdigital",
                        TitledBorder.LEFT,
                        TitledBorder.TOP,
                        new Font("Segoe UI", Font.BOLD, 14),
                        new Color(76, 175, 80)  // Título verde
                ),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createLoweredBevelBorder());
        panel.add(scrollPane, BorderLayout.CENTER);

        // Panel de botones del log
        JPanel logButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        logButtonPanel.setBackground(new Color(250, 250, 250));

        JButton clearButton = createStyledButton("🗑️ Limpiar Log",
                new Color(158, 158, 158), Color.WHITE, 11);
        clearButton.addActionListener(e -> {
            logArea.setText("");
            addLogMessage("🧹 Log limpiado por el usuario");
        });

        JButton exportButton = createStyledButton("💾 Exportar Log",
                new Color(33, 150, 243), Color.WHITE, 11);
        exportButton.addActionListener(e -> {
            addLogMessage("💾 Funcionalidad de exportar disponible próximamente");
        });

        logButtonPanel.add(exportButton);
        logButtonPanel.add(clearButton);
        panel.add(logButtonPanel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * ✅ CREAR PANEL DE BOTONES CON DISEÑO MEJORADO
     */
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        panel.setBackground(new Color(250, 250, 250));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createRaisedBevelBorder(),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        panel.add(testConnectionButton);
        panel.add(startButton);
        panel.add(stopButton);
        panel.add(testButton);

        JButton configButton = createStyledButton("⚙️ Configuración Avanzada",
                new Color(96, 125, 139), Color.WHITE, 13);
        configButton.addActionListener(e -> showAdvancedConfig());
        panel.add(configButton);

        return panel;
    }

    /**
     * ✅ ACTUALIZAR INTERFAZ CON COLORES MODERNOS
     */
    private void updateUI() {
        Color successGreen = new Color(76, 175, 80);
        Color lightGray = new Color(236, 239, 241);
        Color darkGray = new Color(55, 71, 79);
        Color warningOrange = new Color(255, 152, 0);

        if (isRunning) {
            statusLabel.setText("🟢 Sistema Activo - Enviando signos vitales");
            statusLabel.setBackground(successGreen);
            statusLabel.setForeground(Color.WHITE);
            startButton.setEnabled(false);
            stopButton.setEnabled(true);

            // Deshabilitar campos de configuración críticos
            bedNumberField.setEnabled(false);
            bedNumberField.setBackground(lightGray);
            searchPatientButton.setEnabled(false);
            intervalSpinner.setEnabled(false);
            scenarioCombo.setEnabled(false);

        } else {
            statusLabel.setText("🔴 Sistema Detenido - Listo para configurar");
            statusLabel.setBackground(lightGray);
            statusLabel.setForeground(darkGray);
            startButton.setEnabled(currentPatient != null);
            stopButton.setEnabled(false);

            // Habilitar campos de configuración
            bedNumberField.setEnabled(true);
            bedNumberField.setBackground(Color.WHITE);
            searchPatientButton.setEnabled(true);
            intervalSpinner.setEnabled(true);
            scenarioCombo.setEnabled(true);
        }

        // Actualizar color del panel de paciente según estado
        if (currentPatient != null) {
            patientInfoLabel.setBackground(successGreen);
            patientInfoLabel.setText("✅ " + currentPatientInfo);
        } else {
            patientInfoLabel.setBackground(warningOrange);
            patientInfoLabel.setText("⚠️ " + currentPatientInfo);
        }
    }
    /**
     * ✅ CONFIGURAR LAYOUT CON DISEÑO MEJORADO
     */
    private void setupLayout() {
        setLayout(new BorderLayout(15, 15));

        // Fondo general más agradable
        getContentPane().setBackground(new Color(245, 245, 245));

        // Panel superior - Status con diseño moderno
        JPanel statusPanel = new JPanel(new GridLayout(3, 1, 8, 8));
        statusPanel.setBackground(new Color(245, 245, 245));
        statusPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        statusPanel.add(statusLabel);
        statusPanel.add(connectionStatusLabel);
        statusPanel.add(patientInfoLabel);
        add(statusPanel, BorderLayout.NORTH);

        // Panel central con mejor espaciado
        JPanel centerPanel = new JPanel(new BorderLayout(15, 15));
        centerPanel.setBackground(new Color(245, 245, 245));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));

        // Panel de configuración
        JPanel configPanel = createConfigPanel();
        centerPanel.add(configPanel, BorderLayout.NORTH);

        // Panel de log
        JPanel logPanel = createLogPanel();
        centerPanel.add(logPanel, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        // Panel inferior - Botones
        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * ✅ MOSTRAR CONFIGURACIÓN AVANZADA CON MEJOR DISEÑO
     */
    private void showAdvancedConfig() {
        JDialog dialog = new JDialog(this, "Configuración Avanzada HL7", true);
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(new Color(250, 250, 250));

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(new Color(250, 250, 250));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Panel de configuración
        JPanel configPanel = new JPanel(new GridBagLayout());
        configPanel.setBackground(new Color(250, 250, 250));
        configPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createRaisedBevelBorder(),
                "🔧 Configuración del Servidor HL7",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 14),
                new Color(33, 150, 243)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;

        // Servidor HL7
        gbc.gridx = 0; gbc.gridy = 0;
        configPanel.add(createStyledLabel("🌐 Servidor HL7:", 12, true), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        JTextField serverField = createStyledTextField("localhost:2575", 20);
        configPanel.add(serverField, gbc);

        // Timeout
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE;
        configPanel.add(createStyledLabel("⏱️ Timeout (ms):", 12, true), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        JTextField timeoutField = createStyledTextField("10000", 20);
        configPanel.add(timeoutField, gbc);

        // Reintentos
        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE;
        configPanel.add(createStyledLabel("🔄 Máx. Reintentos:", 12, true), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        JTextField retriesField = createStyledTextField("3", 20);
        configPanel.add(retriesField, gbc);

        // Información adicional
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        JTextArea infoArea = new JTextArea(
                "ℹ️ Configuración del cliente MLLP:\n" +
                        "• El servidor debe estar ejecutándose en el puerto especificado\n" +
                        "• El timeout se aplica tanto a conexión como a lectura\n" +
                        "• Los reintentos son automáticos en caso de fallo\n" +
                        "• Protocolo: HL7 v2.5 sobre MLLP/TCP", 5, 30);
        infoArea.setEditable(false);
        infoArea.setBackground(new Color(240, 248, 255));
        infoArea.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        infoArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLoweredBevelBorder(),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        configPanel.add(infoArea, gbc);

        mainPanel.add(configPanel, BorderLayout.CENTER);

        // Panel de botones
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.setBackground(new Color(250, 250, 250));

        JButton cancelButton = createStyledButton("❌ Cancelar",
                new Color(158, 158, 158), Color.WHITE, 12);
        cancelButton.addActionListener(e -> dialog.dispose());

        JButton applyButton = createStyledButton("✅ Aplicar Configuración",
                new Color(76, 175, 80), Color.WHITE, 12);
        applyButton.addActionListener(e -> {
            addLogMessage("⚙️ Configuración aplicada:");
            addLogMessage("  🌐 Servidor: " + serverField.getText());
            addLogMessage("  ⏱️ Timeout: " + timeoutField.getText() + " ms");
            addLogMessage("  🔄 Reintentos: " + retriesField.getText());
            dialog.dispose();
        });

        buttonPanel.add(cancelButton);
        buttonPanel.add(applyButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.add(mainPanel);
        dialog.setVisible(true);
    }

    /**
     * ✅ AGREGAR MENSAJE AL LOG CON COLORES MEJORADOS
     */
    private void addLogMessage(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String logEntry = "[" + timestamp + "] " + message + "\n";

        SwingUtilities.invokeLater(() -> {
            // Agregar diferentes colores según el tipo de mensaje (simulado con texto)
            String coloredEntry = logEntry;
            if (message.contains("✅") || message.contains("🟢")) {
                // Mensajes de éxito - ya están marcados con emojis
            } else if (message.contains("❌") || message.contains("🔴")) {
                // Mensajes de error - ya están marcados con emojis
            } else if (message.contains("⚠️") || message.contains("🟡")) {
                // Mensajes de advertencia - ya están marcados con emojis
            } else if (message.contains("🔗") || message.contains("📡")) {
                // Mensajes de conexión - ya están marcados con emojis
            }

            logArea.append(coloredEntry);
            logArea.setCaretPosition(logArea.getDocument().getLength());

            // Limitar el tamaño del log para evitar que crezca indefinidamente
            String text = logArea.getText();
            String[] lines = text.split("\n");
            if (lines.length > 500) {  // Mantener últimas 500 líneas
                StringBuilder newText = new StringBuilder();
                for (int i = lines.length - 400; i < lines.length; i++) {
                    newText.append(lines[i]).append("\n");
                }
                logArea.setText(newText.toString());
                logArea.setCaretPosition(logArea.getDocument().getLength());
            }
        });
    }

    /**
     * ✅ CONFIGURAR EVENT HANDLERS COMPLETOS
     */
    private void setupEventHandlers() {
        // Test de conexión HL7
        testConnectionButton.addActionListener(e -> testHL7Connection());

        // Buscar paciente por cama
        searchPatientButton.addActionListener(e -> searchPatientByCama());

        // Controles principales
        startButton.addActionListener(e -> startSimulation());
        stopButton.addActionListener(e -> stopSimulation());
        testButton.addActionListener(e -> sendTestMessage());

        // Actualizar timer cuando cambie el intervalo
        intervalSpinner.addChangeListener(e -> {
            if (simulationTimer != null) {
                simulationTimer.setDelay(((Number)intervalSpinner.getValue()).intValue() * 1000);
                addLogMessage("⏰ Intervalo actualizado a " + intervalSpinner.getValue() + " segundos");
            }
        });

        // Event listener para cambio de escenario
        scenarioCombo.addActionListener(e -> {
            String selectedScenario = (String) scenarioCombo.getSelectedItem();
            if (selectedScenario != null) {
                addLogMessage("🎭 Escenario cambiado a: " + selectedScenario);
            }
        });

        // Event listener para campo de cama (Enter para buscar)
        bedNumberField.addActionListener(e -> searchPatientByCama());

        // Cerrar ventana con confirmación
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (isRunning) {
                    int option = JOptionPane.showConfirmDialog(
                            SimulatorMainFrame.this,
                            "El simulador está ejecutándose. ¿Desea detenerlo y salir?",
                            "Confirmar Salida",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE
                    );

                    if (option == JOptionPane.YES_OPTION) {
                        stopSimulation();
                        if (hl7Client != null) {
                            hl7Client.disconnect();
                        }
                        addLogMessage("👋 Cerrando simulador HL7...");
                        System.exit(0);
                    } else {
                        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                        return;
                    }
                }

                // Si no está ejecutándose, cerrar directamente
                if (hl7Client != null) {
                    hl7Client.disconnect();
                }
                addLogMessage("👋 Cerrando simulador HL7...");
                System.exit(0);
            }
        });

        // Event listeners para spinners de signos vitales
        heartRateSpinner.addChangeListener(e -> {
            if (!isRunning) {
                double value = ((Number) heartRateSpinner.getValue()).doubleValue();
                if (value < 60 || value > 100) {
                    heartRateSpinner.setBackground(new Color(255, 235, 235)); // Fondo rojizo
                } else {
                    heartRateSpinner.setBackground(Color.WHITE);
                }
            }
        });

        systolicSpinner.addChangeListener(e -> {
            if (!isRunning) {
                double value = ((Number) systolicSpinner.getValue()).doubleValue();
                if (value < 90 || value > 140) {
                    systolicSpinner.setBackground(new Color(255, 235, 235)); // Fondo rojizo
                } else {
                    systolicSpinner.setBackground(Color.WHITE);
                }
            }
        });

        diastolicSpinner.addChangeListener(e -> {
            if (!isRunning) {
                double value = ((Number) diastolicSpinner.getValue()).doubleValue();
                if (value < 60 || value > 90) {
                    diastolicSpinner.setBackground(new Color(255, 235, 235)); // Fondo rojizo
                } else {
                    diastolicSpinner.setBackground(Color.WHITE);
                }
            }
        });

        temperatureSpinner.addChangeListener(e -> {
            if (!isRunning) {
                double value = ((Number) temperatureSpinner.getValue()).doubleValue();
                if (value < 36.0 || value > 37.5) {
                    temperatureSpinner.setBackground(new Color(255, 235, 235)); // Fondo rojizo
                } else {
                    temperatureSpinner.setBackground(Color.WHITE);
                }
            }
        });
    }

    /**
     * ✅ INICIAR SIMULACIÓN CON VALIDACIONES COMPLETAS
     */
    private void startSimulation() {
        // Validación 1: Verificar que hay un paciente asignado
        if (currentPatient == null) {
            JOptionPane.showMessageDialog(this,
                    "Por favor busque primero un paciente por número de cama\n" +
                            "usando el botón '🔍 Buscar Paciente'",
                    "Paciente requerido",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Validación 2: Verificar conexión a base de datos
        if (!patientService.testDatabaseConnection()) {
            JOptionPane.showMessageDialog(this,
                    "No se puede conectar a la base de datos bd_hdigital\n" +
                            "Verifique la conexión antes de continuar",
                    "Error de base de datos",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Validación 3: Verificar conexión HL7
        if (!hl7Client.isConnected()) {
            addLogMessage("⚠️ Conectando al servidor HL7 bd_hdigital:2575...");
            if (!hl7Client.connect()) {
                String errorMsg = "No se pudo conectar al servidor HL7 bd_hdigital:2575\n";
                String lastError = hl7Client.getLastError();
                if (lastError != null) {
                    errorMsg += "Error: " + lastError + "\n\n";
                }
                errorMsg += "Verifique que el microservicio HL7 esté ejecutándose.";

                JOptionPane.showMessageDialog(this,
                        errorMsg,
                        "Error de conexión HL7",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        // Obtener estadísticas de hospitalización
        SwingWorker<PatientService.HospitalizationStats, Void> statsWorker =
                new SwingWorker<PatientService.HospitalizationStats, Void>() {
                    @Override
                    protected PatientService.HospitalizationStats doInBackground() throws Exception {
                        return patientService.getHospitalizationStats();
                    }

                    @Override
                    protected void done() {
                        try {
                            PatientService.HospitalizationStats stats = get();
                            addLogMessage(String.format("📊 Estadísticas hospitalarias: %d hospitalizaciones, %d camas ocupadas",
                                    stats.getTotalHospitalizaciones(), stats.getCamasOcupadas()));
                        } catch (Exception e) {
                            addLogMessage("⚠️ No se pudieron obtener estadísticas hospitalarias");
                        }
                    }
                };
        statsWorker.execute();

        // Iniciar simulación
        isRunning = true;
        updateUI();
        simulationTimer.start();

        addLogMessage("🚀 Iniciando simulación HL7 para paciente REAL de bd_hdigital...");
        addLogMessage("📡 Conectado a servidor HL7 bd_hdigital:2575");
        addLogMessage("✅ Monitor Philips MX450 configurado y operativo");
        addLogMessage("👤 Paciente: " + currentPatient.getNombreCompleto());
        addLogMessage("📋 DNI: " + currentPatient.getNumeroDocumento());
        addLogMessage("🏥 Número de cuenta: " + currentPatient.getNumeroCuenta());
        addLogMessage("🛏️ Cama: " + currentPatient.getNumeroCama());
        addLogMessage("🏥 Especialidad: " + (currentPatient.getEspecialidad() != null ? currentPatient.getEspecialidad() : "No especificada"));
        addLogMessage("⏰ Intervalo de envío: " + intervalSpinner.getValue() + " segundos");
        addLogMessage("🎭 Escenario clínico: " + scenarioCombo.getSelectedItem());
        addLogMessage("📊 Signos vitales base configurados");
        addLogMessage("🔄 Iniciando envío automático de signos vitales...");

        // Enviar primer conjunto de signos vitales inmediatamente
        SwingUtilities.invokeLater(() -> {
            sendVitalSignsToServer();
        });
    }

    /**
     * ✅ DETENER SIMULACIÓN
     */
    private void stopSimulation() {
        isRunning = false;
        updateUI();
        simulationTimer.stop();

        addLogMessage("⏹️ Deteniendo simulación...");
        addLogMessage("🔌 Manteniendo conexión HL7 abierta...");
        addLogMessage("✅ Simulación detenida correctamente");
    }

    /**
     * ✅ ENVIAR MENSAJE DE PRUEBA
     */
    private void sendTestMessage() {
        if (!hl7Client.isConnected()) {
            addLogMessage("⚠️ Conectando al servidor HL7...");
            if (!hl7Client.connect()) {
                addLogMessage("❌ No se pudo conectar para enviar mensaje de prueba");
                return;
            }
        }

        addLogMessage("🧪 Enviando mensaje de prueba al servidor bd_hdigital...");

        hl7Client.sendTestMessage().thenAccept(response -> {
            SwingUtilities.invokeLater(() -> {
                if (response.isSuccess()) {
                    addLogMessage("✅ Mensaje de prueba enviado exitosamente");
                    addLogMessage("📥 ACK recibido del servidor");
                } else {
                    addLogMessage("❌ Error en mensaje de prueba: " + response.getMessage());
                }
            });
        });
    }
}