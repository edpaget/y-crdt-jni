package net.carcdr.example.spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot collaborative editing server.
 *
 * <p>This application demonstrates the yhocuspocus-spring-websocket module.
 * All configuration is handled via application.yml and auto-configuration.</p>
 *
 * <p>To run:</p>
 * <pre>
 * ./gradlew :examples:spring-boot:backend:bootRun
 * </pre>
 *
 * <p>The WebSocket endpoint will be available at ws://localhost:1234/</p>
 */
@SpringBootApplication
public class CollaborativeSpringApplication {

    public static void main(String[] args) {
        SpringApplication.run(CollaborativeSpringApplication.class, args);
    }
}
