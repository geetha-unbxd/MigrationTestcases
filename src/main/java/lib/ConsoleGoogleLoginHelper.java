package lib;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Runs the Node-based Google login script and injects cookies into Selenium.
 */
public final class ConsoleGoogleLoginHelper {

    private ConsoleGoogleLoginHelper() {
    }

    public static boolean loginAndInjectCookies(WebDriver driver, String fallbackTargetUrl) {
        if (!ConsoleCredentialsHelper.hasGoogleCredentials()) {
            return false;
        }

        Path consoleLoginDir = ConsoleCredentialsHelper.getConsoleLoginDirectory();
        Path script = consoleLoginDir.resolve("export-console-login.js");
        if (!Files.isRegularFile(script)) {
            System.out.println("Google login script not found at " + script.toAbsolutePath()
                    + " (cwd=" + System.getProperty("user.dir") + "). "
                    + "Set CONSOLE_LOGIN_DIR or -Dconsole.login.dir to the console-login folder.");
            return false;
        }

        LoginResult result = runLoginScript(consoleLoginDir.toFile(), fallbackTargetUrl);
        if (!result.success || result.cookies.isEmpty()) {
            throw new RuntimeException("Google login script returned no cookies.");
        }

        String targetUrl = resolveTargetUrl(fallbackTargetUrl);
        injectCookies(driver, targetUrl, result.cookies);

        driver.get(targetUrl);
        driver.navigate().refresh();

        String currentUrl = driver.getCurrentUrl();
        return currentUrl != null && !currentUrl.contains("/login");
    }

    private static LoginResult runLoginScript(File workingDirectory, String fallbackTargetUrl) {
        ProcessBuilder pb = new ProcessBuilder("node", "export-console-login.js");
        pb.directory(workingDirectory);
        pb.redirectErrorStream(true);

        Map<String, String> env = pb.environment();
        env.put("GOOGLE_EMAIL", ConsoleCredentialsHelper.getGoogleEmail());
        env.put("GOOGLE_PASSWORD", ConsoleCredentialsHelper.getGooglePassword());

        String totp = ConsoleCredentialsHelper.getTotpSecret();
        if (totp != null && !totp.isBlank()) {
            env.put("TOTP_SECRET", totp);
        }

        env.put("HEADLESS", ConsoleCredentialsHelper.getHeadless());
        env.put("CONSOLE_TARGET_URL", resolveTargetUrl(fallbackTargetUrl));

        int exitCode;
        String output;
        try {
            Process process = pb.start();
            output = readAll(process.getInputStream());
            exitCode = process.waitFor();
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RuntimeException("Failed to execute console login script.", e);
        }

        if (exitCode != 0) {
            throw new RuntimeException("Console login script failed. " + output);
        }

        String jsonLine = extractLastJsonLine(output);
        return parseResult(jsonLine);
    }

    private static void injectCookies(WebDriver driver, String baseUrl, List<CookieEntry> cookies) {
        driver.get(baseUrl);

        int injected = 0;
        for (CookieEntry c : cookies) {
            try {
                Cookie.Builder builder = new Cookie.Builder(c.name, c.value)
                        .path(c.path == null || c.path.isBlank() ? "/" : c.path)
                        .isHttpOnly(c.httpOnly)
                        .isSecure(c.secure);

                if (c.domain != null && !c.domain.isBlank()) {
                    builder.domain(c.domain);
                }
                if (c.expiryEpochSeconds > 0) {
                    builder.expiresOn(java.util.Date.from(java.time.Instant.ofEpochSecond(c.expiryEpochSeconds)));
                }
                driver.manage().addCookie(builder.build());
                injected++;
            } catch (Exception ignored) {
                // Some cookies can be host-bound and fail from current origin; keep adding remaining cookies.
            }
        }

        if (injected == 0) {
            throw new RuntimeException("No cookies were injected into browser session.");
        }
    }

    private static LoginResult parseResult(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        boolean success = root.has("success") && root.get("success").getAsBoolean();
        if (!success) {
            String error = root.has("error") ? root.get("error").getAsString() : "Unknown login error";
            throw new RuntimeException(error);
        }

        List<CookieEntry> cookies = new ArrayList<>();
        if (root.has("cookieList") && root.get("cookieList").isJsonArray()) {
            JsonArray arr = root.getAsJsonArray("cookieList");
            for (JsonElement element : arr) {
                JsonObject cookie = element.getAsJsonObject();
                cookies.add(new CookieEntry(
                        getAsString(cookie, "name"),
                        getAsString(cookie, "value"),
                        getAsString(cookie, "domain"),
                        getAsString(cookie, "path"),
                        cookie.has("secure") && cookie.get("secure").getAsBoolean(),
                        cookie.has("httpOnly") && cookie.get("httpOnly").getAsBoolean(),
                        cookie.has("expires") && cookie.get("expires").isJsonPrimitive()
                                ? cookie.get("expires").getAsLong() : -1
                ));
            }
        }
        return new LoginResult(success, cookies);
    }

    private static String getAsString(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) {
            return "";
        }
        return obj.get(key).getAsString();
    }

    private static String readAll(java.io.InputStream stream) throws IOException {
        StringBuilder out = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line).append('\n');
            }
        }
        return out.toString();
    }

    private static String extractLastJsonLine(String output) {
        String[] lines = output.split("\\R");
        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i].trim();
            if (line.startsWith("{") && line.endsWith("}")) {
                return line;
            }
        }
        throw new RuntimeException("Could not parse login output JSON.");
    }

    private static String resolveTargetUrl(String fallbackTargetUrl) {
        String explicit = ConsoleCredentialsHelper.getConsoleTargetUrl();
        if (explicit != null && !explicit.isBlank()) {
            return explicit.trim();
        }
        if (fallbackTargetUrl != null && !fallbackTargetUrl.isBlank()) {
            return fallbackTargetUrl.trim();
        }
        String fromEnvConfig = EnvironmentConfig.getApplicationUrl();
        if (fromEnvConfig != null && !fromEnvConfig.isBlank()) {
            return fromEnvConfig.trim();
        }
        return "https://console.unbxd.io";
    }

    private static final class LoginResult {
        private final boolean success;
        private final List<CookieEntry> cookies;

        private LoginResult(boolean success, List<CookieEntry> cookies) {
            this.success = success;
            this.cookies = cookies;
        }
    }

    private static final class CookieEntry {
        private final String name;
        private final String value;
        private final String domain;
        private final String path;
        private final boolean secure;
        private final boolean httpOnly;
        private final long expiryEpochSeconds;

        private CookieEntry(String name, String value, String domain, String path,
                            boolean secure, boolean httpOnly, long expiryEpochSeconds) {
            this.name = name;
            this.value = value;
            this.domain = domain;
            this.path = path;
            this.secure = secure;
            this.httpOnly = httpOnly;
            this.expiryEpochSeconds = expiryEpochSeconds;
        }
    }
}
