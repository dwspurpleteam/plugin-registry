package ru.itmo.pluginservice.simplepluginregistry.service;

import com.networknt.schema.ExecutionContext;
import com.networknt.schema.format.Format;

import java.net.IDN;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.regex.Pattern;

/** URL policy used by schemas without coupling Java code to manifest field paths. */
final class ManifestUrlFormat implements Format {

    static final ManifestUrlFormat HTTPS_URI = new ManifestUrlFormat("https-uri", false);
    static final ManifestUrlFormat PUBLIC_HTTPS_URI = new ManifestUrlFormat("public-https-uri", true);

    private static final Pattern DNS_LABEL = Pattern.compile("[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?");
    private static final Pattern IPV4_LITERAL = Pattern.compile("[0-9]+(?:\\.[0-9]+){3}");
    private static final Pattern NUMERIC_HOST = Pattern.compile("[0-9.]+");
    private static final Pattern HEX_ADDRESS = Pattern.compile("(?i)0x[0-9a-f]+");

    private final String name;
    private final boolean publicTarget;

    private ManifestUrlFormat(String name, boolean publicTarget) {
        this.name = name;
        this.publicTarget = publicTarget;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean matches(ExecutionContext executionContext, String value) {
        URI uri;
        try {
            uri = new URI(value);
        } catch (URISyntaxException exception) {
            return false;
        }
        if (!uri.isAbsolute() || !"https".equals(uri.getScheme()) || uri.getRawUserInfo() != null
                || uri.getRawFragment() != null || !hasValidPort(uri)) {
            return false;
        }
        String host = uri.getHost();
        if (host == null || host.isBlank() || !isValidHost(host)) {
            return false;
        }
        return !publicTarget || isAllowedPublicTarget(host);
    }

    private static boolean hasValidPort(URI uri) {
        String authority = uri.getRawAuthority();
        if (authority == null) return false;
        String hostAndPort = authority.substring(authority.lastIndexOf('@') + 1);
        String port = null;
        if (hostAndPort.startsWith("[")) {
            int closingBracket = hostAndPort.indexOf(']');
            if (closingBracket < 0) return false;
            String suffix = hostAndPort.substring(closingBracket + 1);
            if (!suffix.isEmpty()) {
                if (!suffix.startsWith(":")) return false;
                port = suffix.substring(1);
            }
        } else {
            int colon = hostAndPort.lastIndexOf(':');
            if (colon >= 0) port = hostAndPort.substring(colon + 1);
        }
        if (port == null) return true;
        if (port.isEmpty() || !port.chars().allMatch(Character::isDigit)) return false;
        try {
            int number = Integer.parseInt(port);
            return number >= 1 && number <= 65_535;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private static boolean isValidHost(String host) {
        String normalized = normalizeHost(host);
        if (normalized.contains(":")) return parseIpLiteral(normalized) instanceof Inet6Address;
        if (NUMERIC_HOST.matcher(normalized).matches()) return parseIpLiteral(normalized) instanceof Inet4Address;
        if (HEX_ADDRESS.matcher(normalized).matches()) return false;
        try {
            String ascii = IDN.toASCII(normalized, IDN.USE_STD3_ASCII_RULES);
            if (ascii.endsWith(".")) ascii = ascii.substring(0, ascii.length() - 1);
            if (ascii.isBlank() || ascii.length() > 253) return false;
            for (String label : ascii.split("\\.")) {
                if (!DNS_LABEL.matcher(label).matches()) return false;
            }
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static boolean isAllowedPublicTarget(String host) {
        String normalized = normalizeHost(host).toLowerCase(Locale.ROOT);
        if (normalized.equals("localhost") || normalized.endsWith(".localhost") || normalized.endsWith(".local")) {
            return false;
        }
        InetAddress address = parseIpLiteral(normalized);
        return address == null || isPublicAddress(address);
    }

    private static String normalizeHost(String host) {
        String normalized = host.startsWith("[") && host.endsWith("]") ? host.substring(1, host.length() - 1) : host;
        return normalized.endsWith(".") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }

    private static InetAddress parseIpLiteral(String host) {
        if ((!host.contains(":") && !IPV4_LITERAL.matcher(host).matches()) || host.contains("%")) return null;
        if (IPV4_LITERAL.matcher(host).matches() && !isCanonicalIpv4(host)) return null;
        try {
            return InetAddress.getByName(host);
        } catch (UnknownHostException exception) {
            return null;
        }
    }

    private static boolean isCanonicalIpv4(String host) {
        String[] octets = host.split("\\.", -1);
        if (octets.length != 4) return false;
        for (String octet : octets) {
            if (octet.isEmpty() || octet.length() > 3 || (octet.length() > 1 && octet.startsWith("0"))) return false;
            try {
                if (Integer.parseInt(octet) > 255) return false;
            } catch (NumberFormatException exception) {
                return false;
            }
        }
        return true;
    }

    private static boolean isPublicAddress(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) return false;
        byte[] bytes = address.getAddress();
        if (address instanceof Inet4Address) {
            int first = Byte.toUnsignedInt(bytes[0]);
            int second = Byte.toUnsignedInt(bytes[1]);
            int third = Byte.toUnsignedInt(bytes[2]);
            return first != 0 && !(first == 100 && second >= 64 && second <= 127)
                    && !(first == 192 && second == 0 && third == 0)
                    && !(first == 192 && second == 0 && third == 2)
                    && !(first == 198 && (second == 18 || second == 19))
                    && !(first == 198 && second == 51 && third == 100)
                    && !(first == 203 && second == 0 && third == 113) && first < 224;
        }
        if (address instanceof Inet6Address) {
            int first = Byte.toUnsignedInt(bytes[0]);
            int second = Byte.toUnsignedInt(bytes[1]);
            return (first & 0xfe) != 0xfc && !(first == 0x20 && second == 0x01
                    && Byte.toUnsignedInt(bytes[2]) == 0x0d && Byte.toUnsignedInt(bytes[3]) == 0xb8);
        }
        return false;
    }
}
