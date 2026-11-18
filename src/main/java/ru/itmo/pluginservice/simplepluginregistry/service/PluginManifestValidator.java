package ru.itmo.pluginservice.simplepluginregistry.service;

import com.networknt.schema.Error;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SchemaRegistryConfig;
import com.networknt.schema.SpecificationVersion;
import com.networknt.schema.ValidationLimitExceededException;
import org.springframework.stereotype.Component;
import ru.itmo.pluginservice.simplepluginregistry.exception.ManifestValidationException;
import tools.jackson.databind.JsonNode;

import java.io.InputStream;
import java.net.IDN;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class PluginManifestValidator {

    private static final String SCHEMA_RESOURCE = "/schema/plugin-manifest-v1.1.schema.json";
    private static final String SCHEMA_ID = "urn:plugin-registry:schema:plugin-manifest:1.1";
    private static final Pattern DNS_LABEL = Pattern.compile("[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?");
    private static final Pattern IPV4_LITERAL = Pattern.compile("[0-9]+(?:\\.[0-9]+){3}");
    private static final Pattern NUMERIC_HOST = Pattern.compile("[0-9.]+");
    private static final Pattern HEX_ADDRESS = Pattern.compile("(?i)0x[0-9a-f]+");

    private final Schema schema;

    public PluginManifestValidator() {
        SchemaRegistryConfig config = SchemaRegistryConfig.builder()
                .formatAssertionsEnabled(true)
                .build();
        SchemaRegistry registry = SchemaRegistry.withDefaultDialect(
                SpecificationVersion.DRAFT_2020_12,
                builder -> builder.schemaRegistryConfig(config)
        );

        try (InputStream schemaStream = PluginManifestValidator.class.getResourceAsStream(SCHEMA_RESOURCE)) {
            if (schemaStream == null) {
                throw new IllegalStateException("Manifest schema resource was not found: " + SCHEMA_RESOURCE);
            }
            this.schema = registry.getSchema(
                    SchemaLocation.of(SCHEMA_ID),
                    schemaStream,
                    InputFormat.JSON
            );
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load manifest schema 1.1", exception);
        }
    }

    public void validate(JsonNode manifest) {
        List<String> errors = new ArrayList<>();
        List<Error> schemaErrors;
        try {
            schemaErrors = schema.validate(manifest, executionContext ->
                    executionContext.executionConfig(builder -> builder
                            .formatAssertionsEnabled(true)
                            .maxEvaluationDepth(64)
                            .maxEvaluationSteps(10_000))
            );
        } catch (ValidationLimitExceededException exception) {
            throw new ManifestValidationException(List.of("$: manifest is too complex to validate"));
        }

        schemaErrors.stream()
                .map(this::formatSchemaError)
                .distinct()
                .limit(20)
                .forEach(errors::add);

        if (schemaErrors.isEmpty()) {
            validateUrl(manifest, "/frontend/url-web", "frontend.url-web", false, errors);
            validateUrl(manifest, "/backend/health-url", "backend.health-url", true, errors);
            validateUrl(manifest, "/backend/control-url", "backend.control-url", true, errors);
            validateUrl(
                    manifest,
                    "/backend/results/delivery-report-url",
                    "backend.results.delivery-report-url",
                    true,
                    errors
            );
        }

        if (!errors.isEmpty()) {
            throw new ManifestValidationException(errors);
        }
    }

    private String formatSchemaError(Error error) {
        String path = jsonPointerToField(error.getInstanceLocation().toString());
        if (error.getProperty() != null) {
            path = "$".equals(path) ? error.getProperty() : path + "." + error.getProperty();
        }
        return path + ": " + error.getMessage();
    }

    private String jsonPointerToField(String pointer) {
        if (pointer == null || pointer.isBlank()) {
            return "$";
        }

        StringBuilder field = new StringBuilder();
        for (String token : pointer.substring(1).split("/")) {
            String decoded = token.replace("~1", "/").replace("~0", "~");
            if (decoded.chars().allMatch(Character::isDigit)) {
                field.append('[').append(decoded).append(']');
            } else {
                if (!field.isEmpty()) {
                    field.append('.');
                }
                field.append(decoded);
            }
        }
        return field.toString();
    }

    private void validateUrl(
            JsonNode manifest,
            String jsonPointer,
            String field,
            boolean serverToServer,
            List<String> errors
    ) {
        JsonNode value = manifest.at(jsonPointer);
        if (value.isMissingNode()) {
            return;
        }

        URI uri;
        try {
            uri = new URI(value.asString());
        } catch (URISyntaxException exception) {
            errors.add(field + ": must be a valid absolute HTTPS URI");
            return;
        }

        if (!uri.isAbsolute() || !"https".equals(uri.getScheme())) {
            errors.add(field + ": must use the https scheme");
            return;
        }
        if (uri.getRawUserInfo() != null) {
            errors.add(field + ": user-info is not allowed");
        }
        if (uri.getRawFragment() != null) {
            errors.add(field + ": fragments are not allowed");
        }
        if (!hasValidPort(uri)) {
            errors.add(field + ": port must be between 1 and 65535");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank() || !isValidHost(host)) {
            errors.add(field + ": hostname is invalid");
            return;
        }

        if (serverToServer) {
            validateServerTarget(field, host, errors);
        }
    }

    private boolean hasValidPort(URI uri) {
        String authority = uri.getRawAuthority();
        if (authority == null) {
            return false;
        }
        int userInfoEnd = authority.lastIndexOf('@');
        String hostAndPort = userInfoEnd >= 0 ? authority.substring(userInfoEnd + 1) : authority;

        String port = null;
        if (hostAndPort.startsWith("[")) {
            int closingBracket = hostAndPort.indexOf(']');
            if (closingBracket < 0) {
                return false;
            }
            String suffix = hostAndPort.substring(closingBracket + 1);
            if (!suffix.isEmpty()) {
                if (!suffix.startsWith(":")) {
                    return false;
                }
                port = suffix.substring(1);
            }
        } else {
            int colon = hostAndPort.lastIndexOf(':');
            if (colon >= 0) {
                port = hostAndPort.substring(colon + 1);
            }
        }

        if (port == null) {
            return true;
        }
        if (port.isEmpty() || !port.chars().allMatch(Character::isDigit)) {
            return false;
        }
        try {
            int value = Integer.parseInt(port);
            return value >= 1 && value <= 65_535;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private boolean isValidHost(String host) {
        String normalized = normalizeHost(host);
        if (normalized.contains(":")) {
            return parseIpLiteral(normalized) instanceof Inet6Address;
        }
        if (NUMERIC_HOST.matcher(normalized).matches()) {
            return parseIpLiteral(normalized) instanceof Inet4Address;
        }
        if (HEX_ADDRESS.matcher(normalized).matches()) {
            return false;
        }

        String ascii;
        try {
            ascii = IDN.toASCII(normalized, IDN.USE_STD3_ASCII_RULES);
        } catch (IllegalArgumentException exception) {
            return false;
        }
        if (ascii.endsWith(".")) {
            ascii = ascii.substring(0, ascii.length() - 1);
        }
        if (ascii.isBlank() || ascii.length() > 253) {
            return false;
        }
        for (String label : ascii.split("\\.")) {
            if (!DNS_LABEL.matcher(label).matches()) {
                return false;
            }
        }
        return true;
    }

    private void validateServerTarget(String field, String host, List<String> errors) {
        String normalized = normalizeHost(host).toLowerCase(Locale.ROOT);
        if (normalized.equals("localhost") || normalized.endsWith(".localhost") || normalized.endsWith(".local")) {
            errors.add(field + ": local hostnames are not allowed for server-to-server callbacks");
            return;
        }

        InetAddress address = parseIpLiteral(normalized);
        if (address != null && !isPublicAddress(address)) {
            errors.add(field + ": private, local, reserved, and multicast IP addresses are not allowed");
        }
    }

    private String normalizeHost(String host) {
        String normalized = host.startsWith("[") && host.endsWith("]")
                ? host.substring(1, host.length() - 1)
                : host;
        return normalized.endsWith(".")
                ? normalized.substring(0, normalized.length() - 1)
                : normalized;
    }

    private InetAddress parseIpLiteral(String host) {
        if (!host.contains(":") && !IPV4_LITERAL.matcher(host).matches()) {
            return null;
        }
        if (host.contains("%")) {
            return null;
        }
        if (IPV4_LITERAL.matcher(host).matches() && !isCanonicalIpv4(host)) {
            return null;
        }
        try {
            InetAddress address = InetAddress.getByName(host);
            return address;
        } catch (UnknownHostException exception) {
            return null;
        }
    }

    private boolean isCanonicalIpv4(String host) {
        String[] octets = host.split("\\.", -1);
        if (octets.length != 4) {
            return false;
        }
        for (String octet : octets) {
            if (octet.isEmpty() || octet.length() > 3 || (octet.length() > 1 && octet.startsWith("0"))) {
                return false;
            }
            int value;
            try {
                value = Integer.parseInt(octet);
            } catch (NumberFormatException exception) {
                return false;
            }
            if (value > 255) {
                return false;
            }
        }
        return true;
    }

    private boolean isPublicAddress(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return false;
        }

        byte[] bytes = address.getAddress();
        if (address instanceof Inet4Address) {
            int first = Byte.toUnsignedInt(bytes[0]);
            int second = Byte.toUnsignedInt(bytes[1]);
            int third = Byte.toUnsignedInt(bytes[2]);
            return first != 0
                    && !(first == 100 && second >= 64 && second <= 127)
                    && !(first == 192 && second == 0 && third == 0)
                    && !(first == 192 && second == 0 && third == 2)
                    && !(first == 198 && (second == 18 || second == 19))
                    && !(first == 198 && second == 51 && third == 100)
                    && !(first == 203 && second == 0 && third == 113)
                    && first < 224;
        }

        if (address instanceof Inet6Address) {
            int first = Byte.toUnsignedInt(bytes[0]);
            int second = Byte.toUnsignedInt(bytes[1]);
            return (first & 0xfe) != 0xfc
                    && !(first == 0x20 && second == 0x01
                    && Byte.toUnsignedInt(bytes[2]) == 0x0d
                    && Byte.toUnsignedInt(bytes[3]) == 0xb8);
        }
        return false;
    }
}
