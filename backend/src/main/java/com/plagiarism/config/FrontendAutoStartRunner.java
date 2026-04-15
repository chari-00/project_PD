package com.plagiarism.config;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Component
public class FrontendAutoStartRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(FrontendAutoStartRunner.class);

    @Value("${frontend.auto-start:true}")
    private boolean autoStart;

    @Value("${frontend.port:5500}")
    private int frontendPort;

    @Value("${frontend.directory:../code}")
    private String frontendDirectory;

    private Process frontendProcess;

    @Override
    public void run(ApplicationArguments args) {
        if (!autoStart) {
            log.info("Frontend auto-start disabled");
            return;
        }

        if (isPortOpen(frontendPort)) {
            log.info("Frontend already running on port {}", frontendPort);
            return;
        }

        Path dir = resolveFrontendDirectory();
        if (dir == null) {
            log.warn("Frontend directory not found");
            return;
        }

        List<String> command = buildPythonCommand();
        if (command.isEmpty()) {
            log.warn("Python not found; cannot auto-start frontend");
            return;
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(dir.toFile());
            pb.redirectErrorStream(true);
            frontendProcess = pb.start();
            log.info("Frontend auto-started at http://localhost:{}", frontendPort);
        } catch (IOException ex) {
            log.warn("Failed to start frontend: {}", ex.getMessage());
        }
    }

    private Path resolveFrontendDirectory() {
        Path userDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path configured = Paths.get(frontendDirectory);
        Path resolved = configured.isAbsolute() ? configured : userDir.resolve(configured).normalize();
        if (Files.isDirectory(resolved)) {
            return resolved;
        }

        Path fallback = userDir.resolve("../code").normalize();
        if (Files.isDirectory(fallback)) {
            return fallback;
        }

        return null;
    }

    private List<String> buildPythonCommand() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);

        if (os.contains("win")) {
            if (isCommandAvailable("python", "--version")) {
                return List.of("python", "-m", "http.server", String.valueOf(frontendPort));
            }
            if (isCommandAvailable("py", "-3", "--version")) {
                return List.of("py", "-3", "-m", "http.server", String.valueOf(frontendPort));
            }
        } else {
            if (isCommandAvailable("python3", "--version")) {
                return List.of("python3", "-m", "http.server", String.valueOf(frontendPort));
            }
            if (isCommandAvailable("python", "--version")) {
                return List.of("python", "-m", "http.server", String.valueOf(frontendPort));
            }
        }

        return List.of();
    }

    private boolean isCommandAvailable(String... cmd) {
        try {
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            boolean finished = p.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                return false;
            }
            return p.exitValue() == 0;
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean isPortOpen(int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", port), 500);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    @PreDestroy
    public void stopFrontend() {
        if (frontendProcess != null && frontendProcess.isAlive()) {
            frontendProcess.destroy();
            try {
                if (!frontendProcess.waitFor(3, TimeUnit.SECONDS)) {
                    frontendProcess.destroyForcibly();
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                frontendProcess.destroyForcibly();
            }
        }
    }
}
