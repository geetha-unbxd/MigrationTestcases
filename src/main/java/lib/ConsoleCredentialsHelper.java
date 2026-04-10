package lib;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resolves Google login credentials from environment variables,
 * JVM properties, or console-login/.env.
 */
public final class ConsoleCredentialsHelper {

    private static Map<String, String> dotEnvCache;

    private ConsoleCredentialsHelper() {
    }

    /**
     * Resolves the {@code console-login} directory (contains {@code export-console-login.js}) so Node runs
     * work when the JVM cwd is the repo root, a submodule folder, or an IDE uses another working directory.
     * <p>Override with env {@code CONSOLE_LOGIN_DIR} or JVM {@code -Dconsole.login.dir=/absolute/path/to/console-login}.
     */
    public static Path getConsoleLoginDirectory() {
        String prop = System.getProperty("console.login.dir");
        if (prop != null && !prop.isBlank()) {
            Path dir = Paths.get(prop.trim());
            if (isConsoleLoginDir(dir)) {
                return dir.toAbsolutePath().normalize();
            }
        }
        String env = System.getenv("CONSOLE_LOGIN_DIR");
        if (env != null && !env.isBlank()) {
            Path dir = Paths.get(env.trim());
            if (isConsoleLoginDir(dir)) {
                return dir.toAbsolutePath().normalize();
            }
        }

        Path start = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        for (int depth = 0; depth < 10 && start != null; depth++) {
            Path candidate = start.resolve("console-login");
            if (isConsoleLoginDir(candidate)) {
                return candidate.normalize();
            }
            start = start.getParent();
        }

        return Paths.get("console-login");
    }

    private static boolean isConsoleLoginDir(Path dir) {
        return Files.isDirectory(dir) && Files.isRegularFile(dir.resolve("export-console-login.js"));
    }

    public static String getGoogleEmail() {
        return resolve("GOOGLE_EMAIL");
    }

    public static String getGooglePassword() {
        return resolve("GOOGLE_PASSWORD");
    }

    public static String getTotpSecret() {
        return resolve("TOTP_SECRET");
    }

    public static String getConsoleTargetUrl() {
        return resolve("CONSOLE_TARGET_URL");
    }

    public static String getHeadless() {
        String v = resolve("HEADLESS");
        if (v == null || v.isBlank()) {
            v = System.getenv("CONSOLE_HEADLESS");
        }
        return (v == null || v.isBlank()) ? "true" : v;
    }

    public static boolean hasGoogleCredentials() {
        String email = getGoogleEmail();
        String password = getGooglePassword();
        return email != null && !email.isBlank() && password != null && !password.isBlank();
    }

    /**
     * When true, {@code LoginActions} uses only the Node/Puppeteer Google flow (no YAML email/password form).
     * <p>If {@code USE_CONSOLE_GOOGLE_LOGIN} / {@code -Duse.console.google.login} are unset, Google login is
     * enabled automatically when {@link #hasGoogleCredentials()} is true (e.g. {@code GOOGLE_EMAIL} and
     * {@code GOOGLE_PASSWORD} in {@code console-login/.env}). Set explicitly to {@code false} to use
     * YAML + normal login form only.
     */
    public static boolean isConsoleGoogleLoginEnabled() {
        String v = resolve("USE_CONSOLE_GOOGLE_LOGIN");
        if (v != null && !v.isBlank()) {
            return parseTruthy(v);
        }
        v = System.getProperty("use.console.google.login");
        if (v != null && !v.isBlank()) {
            return parseTruthy(v);
        }
        return hasGoogleCredentials();
    }

    private static boolean parseTruthy(String raw) {
        String s = raw.trim().toLowerCase();
        if ("false".equals(s) || "0".equals(s) || "no".equals(s) || "off".equals(s)) {
            return false;
        }
        return "true".equals(s) || "1".equals(s) || "yes".equals(s) || "on".equals(s);
    }

    private static String resolve(String key) {
        String value = System.getenv(key);
        if (value != null && !value.isBlank()) {
            return value;
        }

        value = System.getProperty(key);
        if (value != null && !value.isBlank()) {
            return value;
        }

        value = loadDotEnv().get(key);
        if (value != null && !value.isBlank()) {
            return value;
        }
        return null;
    }

    private static Map<String, String> loadDotEnv() {
        if (dotEnvCache != null) {
            return dotEnvCache;
        }

        dotEnvCache = new HashMap<>();
        Set<Path> seen = new LinkedHashSet<>();
        List<Path> candidates = new ArrayList<>();
        candidates.add(getConsoleLoginDirectory().resolve(".env"));
        candidates.add(Paths.get("console-login", ".env"));
        candidates.add(Paths.get(".env"));

        for (Path candidate : candidates) {
            Path abs = candidate.toAbsolutePath().normalize();
            if (!seen.add(abs)) {
                continue;
            }
            if (!Files.isRegularFile(candidate)) {
                continue;
            }
            try {
                List<String> lines = Files.readAllLines(candidate, StandardCharsets.UTF_8);
                for (String rawLine : lines) {
                    String line = rawLine.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    int eq = line.indexOf('=');
                    if (eq <= 0) {
                        continue;
                    }
                    String key = line.substring(0, eq).trim();
                    String value = line.substring(eq + 1).trim();
                    if ((value.startsWith("\"") && value.endsWith("\"")) ||
                            (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                    }
                    dotEnvCache.put(key, value);
                }
                break;
            } catch (IOException ignored) {
                // Ignore invalid local .env reads and continue with env/system props.
            }
        }

        return dotEnvCache;
    }
}
