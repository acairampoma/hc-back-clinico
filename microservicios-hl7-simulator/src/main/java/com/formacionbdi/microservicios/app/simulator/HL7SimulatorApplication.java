package com.formacionbdi.microservicios.app.simulator;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.formacionbdi.microservicios.app.simulator.gui.SimulatorMainFrame;

/**
 * 🏥 SIMULADOR DE EQUIPO MÉDICO HL7 - SPRING BOOT APPLICATION
 *
 * Simula Monitor Philips MX450 enviando signos vitales via HL7 MLLP
 * ✅ SOLUCIÓN CORREGIDA: Manejo seguro del ApplicationContext
 *
 * @author Alan Cairampoma
 * @version 2.1.1 - CONTEXT FIX
 */
@SpringBootApplication(scanBasePackages = {
        "com.formacionbdi.microservicios.app.simulator"
})
@EnableScheduling
public class HL7SimulatorApplication {

    private static final Logger logger = LoggerFactory.getLogger(HL7SimulatorApplication.class);
    private static ApplicationContext applicationContext;

    public static void main(String[] args) {
        // Banner de inicio
        printStartupBanner();

        try {
            // ✅ CONFIGURACIÓN CRÍTICA PARA SWING + SPRING
            System.setProperty("java.awt.headless", "false");
            System.setProperty("spring.main.web-application-type", "none");
            System.setProperty("spring.main.allow-bean-definition-overriding", "true");

            // Configurar Look and Feel
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                logger.info("✅ Look and Feel configurado");
            } catch (ClassNotFoundException | InstantiationException |
                     IllegalAccessException | UnsupportedLookAndFeelException e) {
                logger.warn("⚠️ Look and Feel por defecto: {}", e.getMessage());
            }

            // ✅ INICIAR SPRING BOOT
            SpringApplication app = new SpringApplication(HL7SimulatorApplication.class);
            app.setHeadless(false);
            applicationContext = app.run(args);

            logger.info("🚀 Spring Boot iniciado correctamente");

        } catch (Exception e) {
            logger.error("💥 Error fatal iniciando simulador", e);
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * ✅ INICIAR GUI CUANDO SPRING ESTÉ LISTO - VERSIÓN CORREGIDA
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(ApplicationReadyEvent event) {
        logger.info("🔧 Spring Context listo, iniciando GUI...");

        // ✅ USAR EL CONTEXTO DEL EVENTO - SOLUCIÓN DEFINITIVA
        ApplicationContext eventContext = event.getApplicationContext();

        // ✅ DEBUG: Verificar beans registrados con contexto del evento
        debugSpringBeans(eventContext);

        SwingUtilities.invokeLater(() -> {
            try {
                logger.info("🖥️ Creando interfaz gráfica...");

                // ✅ INTENTAR OBTENER BEAN DE SPRING CON CONTEXTO DEL EVENTO
                try {
                    SimulatorMainFrame frame = eventContext.getBean(SimulatorMainFrame.class);
                    frame.setVisible(true);
                    logger.info("✅ GUI iniciada con Spring Context");
                } catch (Exception e) {
                    logger.error("❌ Error obteniendo bean de Spring: {}", e.getMessage());
                    logger.info("🔄 Intentando creación manual...");

                    // ✅ FALLBACK: Crear manualmente si Spring falla
                    SimulatorMainFrame frame = new SimulatorMainFrame();
                    frame.setVisible(true);
                    logger.info("✅ GUI iniciada manualmente");
                }

            } catch (Exception e) {
                logger.error("❌ Error crítico iniciando GUI", e);
                e.printStackTrace();
                System.exit(1);
            }
        });
    }

    /**
     * ✅ DEBUG: Verificar qué beans está registrando Spring - VERSIÓN SEGURA
     */
    private void debugSpringBeans(ApplicationContext context) {
        logger.info("🔍 Verificando beans de Spring...");

        // ✅ VERIFICACIÓN DE SEGURIDAD
        if (context == null) {
            logger.error("❌ ApplicationContext es null - problema de inicialización");
            return;
        }

        try {
            String[] beanNames = context.getBeanDefinitionNames();

            int totalBeans = 0;
            int simulatorBeans = 0;

            for (String beanName : beanNames) {
                totalBeans++;
                if (beanName.toLowerCase().contains("hl7") ||
                        beanName.toLowerCase().contains("patient") ||
                        beanName.toLowerCase().contains("simulator")) {
                    logger.info("  ✅ Bean encontrado: {}", beanName);
                    simulatorBeans++;
                }
            }

            logger.info("📊 Total beans: {}, Beans del simulador: {}", totalBeans, simulatorBeans);

            // ✅ Verificar beans específicos del simulador
            try {
                Object hl7Client = context.getBean("HL7MLLPClient");
                logger.info("  ✅ HL7MLLPClient: OK - {}", hl7Client.getClass().getSimpleName());
            } catch (Exception e) {
                logger.warn("  ❌ HL7MLLPClient: {}", e.getMessage());
            }

            try {
                Object patientService = context.getBean("patientService");
                logger.info("  ✅ PatientService: OK - {}", patientService.getClass().getSimpleName());
            } catch (Exception e) {
                logger.warn("  ❌ PatientService: {}", e.getMessage());
            }

            try {
                Object mainFrame = context.getBean("simulatorMainFrame");
                logger.info("  ✅ SimulatorMainFrame: OK - {}", mainFrame.getClass().getSimpleName());
            } catch (Exception e) {
                logger.warn("  ❌ SimulatorMainFrame: {}", e.getMessage());
            }

        } catch (Exception e) {
            logger.error("💥 Error durante debug de beans: {}", e.getMessage());
        }
    }

    /**
     * ✅ MÉTODO ESTÁTICO PARA ACCEDER AL CONTEXTO
     */
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * Banner de inicio mejorado
     */
    private static void printStartupBanner() {
        System.out.println("╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║  🏥 SIMULADOR HL7 v2.1.1 - CONTEXT FIX                       ║");
        System.out.println("║                                                                ║");
        System.out.println("║  📟 Monitor Philips MX450 → bd_hdigital:2575                  ║");
        System.out.println("║  🔧 Spring Boot + Swing GUI                                   ║");
        System.out.println("║  ✅ Conexión automática al servidor HL7                       ║");
        System.out.println("║  📊 Búsqueda real de pacientes por cama                       ║");
        System.out.println("║  📡 Envío de signos vitales via MLLP                          ║");
        System.out.println("║                                                                ║");
        System.out.println("║  Desarrollado por: Alan Cairampoma                            ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝");
        System.out.println();
    }
}