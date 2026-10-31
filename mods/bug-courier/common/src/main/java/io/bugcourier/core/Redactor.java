package io.bugcourier.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scrubs secrets and personal details from report text before the player sees the preview.
 * Everything the preview shows is what leaves the computer, so redaction runs first, not on send.
 */
public final class Redactor {
    public static final String HIDDEN = "<hidden>";

    // Launchers pass the session token on the command line and some print it into latest.log.
    private static final Pattern ACCESS_TOKEN_ARG = Pattern.compile(
            "(--(?:accessToken|session|xuid|clientId)\\s+)(\\S+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern TOKEN_ASSIGN = Pattern.compile(
            "((?:access_?token|session_?id|auth_?token|refresh_?token|api_?key|password)\\s*[=:]\\s*\"?)([^\\s\",;]+)",
            Pattern.CASE_INSENSITIVE);
    // JSON web tokens (Microsoft and Minecraft services tokens are JWTs).
    private static final Pattern JWT = Pattern.compile("eyJ[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{8,}");
    private static final Pattern WINDOWS_HOME = Pattern.compile("([A-Za-z]:[\\\\/](?:Users|Documents and Settings)[\\\\/])([^\\\\/\\s:*?\"<>|]+)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern UNIX_HOME = Pattern.compile("(/(?:Users|home)/)([^/\\s:]+)");
    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern IPV4 = Pattern.compile(
            "(?<![\\d.])((?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(?:\\.(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3})(?::\\d{1,5})?(?![\\d.])");
    private static final Pattern IPV6 = Pattern.compile("(?<![0-9A-Fa-f:])(?:[0-9A-Fa-f]{1,4}:){7}[0-9A-Fa-f]{1,4}(?![0-9A-Fa-f:])");

    private final List<Pattern> literals = new ArrayList<>();
    private final Set<String> kept = new HashSet<>();

    /**
     * Also hides these exact strings, such as the player's name and UUID when the player keeps them off. Only whole
     * words match, so a player called "Dev" doesn't turn "device" into "<hidden>ice".
     */
    public Redactor hide(String literal) {
        if (literal != null && literal.length() >= 3) {
            literals.add(Pattern.compile("(?<![A-Za-z0-9_])" + Pattern.quote(literal) + "(?![A-Za-z0-9_])", Pattern.CASE_INSENSITIVE));
        }
        return this;
    }

    /**
     * Never hides this string as an address. For version numbers with four parts, such as a mod at "1.0.0.0",
     * which look exactly like an IP address. Only the address-like part of the version is kept.
     */
    public Redactor keep(String version) {
        if (version == null) return this;
        // The address-like part of versions such as "1.0.0.0-beta" or "mc1.21-2.0.1.3".
        Matcher m = IPV4.matcher(version);
        while (m.find()) kept.add(m.group(1));
        return this;
    }

    public String apply(String text) {
        if (text == null || text.isEmpty()) return text;
        String out = text;
        for (Pattern literal : literals) out = literal.matcher(out).replaceAll(Matcher.quoteReplacement(HIDDEN));
        out = ACCESS_TOKEN_ARG.matcher(out).replaceAll("$1" + Matcher.quoteReplacement(HIDDEN));
        out = TOKEN_ASSIGN.matcher(out).replaceAll("$1" + Matcher.quoteReplacement(HIDDEN));
        out = JWT.matcher(out).replaceAll(Matcher.quoteReplacement(HIDDEN));
        out = WINDOWS_HOME.matcher(out).replaceAll(m -> Matcher.quoteReplacement(m.group(1) + "<user>"));
        out = UNIX_HOME.matcher(out).replaceAll(m -> Matcher.quoteReplacement(m.group(1) + "<user>"));
        out = EMAIL.matcher(out).replaceAll("<email>");
        out = IPV4.matcher(out).replaceAll(m -> keepIp(m.group(1)) ? Matcher.quoteReplacement(m.group()) : "<ip>");
        out = IPV6.matcher(out).replaceAll("<ip>");
        return out;
    }

    // Loopback and the unspecified address say nothing about the player and help reading logs.
    private boolean keepIp(String ip) {
        return ip.startsWith("127.") || ip.equals("0.0.0.0") || kept.contains(ip);
    }
}
