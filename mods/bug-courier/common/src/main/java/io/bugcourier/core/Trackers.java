package io.bugcourier.core;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Built-in trackers and the parser for the short specs and links mods declare. */
public final class Trackers {
    private static final Pattern GITHUB_URL = Pattern.compile("https?://(?:www\\.)?github\\.com/([\\w.-]+)/([\\w.-]+?)(?:\\.git)?(?:/.*)?");
    private static final Pattern GITLAB_URL = Pattern.compile("https?://(gitlab\\.[\\w.-]+)/(.+?)(?:\\.git)?(?:/-/.*|/issues.*)?");
    private static final Pattern GITEA_URL = Pattern.compile("https?://((?:codeberg\\.org|gitea\\.[\\w.-]+|forgejo\\.[\\w.-]+|git\\.[\\w.-]+))/([\\w.-]+)/([\\w.-]+?)(?:\\.git)?(?:/.*)?");
    private static final Pattern REPO = Pattern.compile("[\\w.-]+/[\\w.-]+");

    private Trackers() {
    }

    public static Tracker github(String ownerRepo) {
        requireRepo(ownerRepo);
        return new GitHub(ownerRepo);
    }

    /** {@code host} like "gitlab.com"; {@code projectPath} may hold subgroups, such as "group/sub/project". */
    public static Tracker gitlab(String host, String projectPath) {
        return new GitLab(host, trimSlashes(projectPath));
    }

    /** Gitea, Forgejo and Codeberg share the same new-issue page. */
    public static Tracker gitea(String host, String ownerRepo) {
        requireRepo(ownerRepo);
        return new Gitea(host, ownerRepo);
    }

    /** Any other board: {@code {title}} and {@code {body}} in the template are replaced with encoded text. */
    public static Tracker template(String name, String urlTemplate) {
        return new Template(name, urlTemplate);
    }

    /** A page that cannot be prefilled; the player pastes the report from the clipboard. */
    public static Tracker link(String url) {
        return new Link(URI.create(url));
    }

    /**
     * Reads what a mod declares: a spec such as {@code github:owner/repo}, {@code gitlab:host/group/project},
     * {@code gitea:codeberg.org/owner/repo}, or a link such as the {@code issueTrackerURL} in neoforge.mods.toml.
     */
    public static Optional<Tracker> parse(String declared) {
        if (declared == null) return Optional.empty();
        String s = declared.trim();
        if (s.isEmpty()) return Optional.empty();
        String lower = s.toLowerCase(Locale.ROOT);
        try {
            if (lower.startsWith("github:")) return Optional.of(github(s.substring(7)));
            if (lower.startsWith("gitlab:")) {
                String rest = trimSlashes(s.substring(7));
                int slash = rest.indexOf('/');
                if (slash < 0) return Optional.empty();
                String first = rest.substring(0, slash);
                // "gitlab:group/project" means gitlab.com; "gitlab:host.tld/group/project" a self-hosted one.
                return first.contains(".") ? Optional.of(gitlab(first, rest.substring(slash + 1))) : Optional.of(gitlab("gitlab.com", rest));
            }
            if (lower.startsWith("gitea:") || lower.startsWith("codeberg:") || lower.startsWith("forgejo:")) {
                boolean codeberg = lower.startsWith("codeberg:");
                String rest = trimSlashes(s.substring(s.indexOf(':') + 1));
                if (codeberg) return Optional.of(gitea("codeberg.org", rest));
                int slash = rest.indexOf('/');
                if (slash < 0) return Optional.empty();
                return Optional.of(gitea(rest.substring(0, slash), rest.substring(slash + 1)));
            }
            if (!lower.startsWith("http://") && !lower.startsWith("https://")) return Optional.empty();
            Matcher m = GITHUB_URL.matcher(s);
            if (m.matches()) return Optional.of(github(m.group(1) + "/" + m.group(2)));
            m = GITEA_URL.matcher(s);
            if (m.matches()) return Optional.of(gitea(m.group(1), m.group(2) + "/" + m.group(3)));
            m = GITLAB_URL.matcher(s);
            if (m.matches()) return Optional.of(gitlab(m.group(1), m.group(2)));
            return Optional.of(link(s));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    static String encode(String text) {
        return URLEncoder.encode(text, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static void requireRepo(String ownerRepo) {
        if (ownerRepo == null || !REPO.matcher(ownerRepo).matches())
            throw new IllegalArgumentException("Expected owner/repo, got " + ownerRepo);
    }

    private static String trimSlashes(String s) {
        String out = s.trim();
        while (out.startsWith("/")) out = out.substring(1);
        while (out.endsWith("/")) out = out.substring(0, out.length() - 1);
        return out;
    }

    private record GitHub(String repo) implements Tracker {
        @Override
        public String displayName() {
            return "GitHub (" + repo + ")";
        }

        @Override
        public URI newIssue(String title, String body, List<String> labels) {
            StringBuilder url = new StringBuilder("https://github.com/").append(repo).append("/issues/new?title=").append(encode(title))
                    .append("&body=").append(encode(body));
            // GitHub drops labels the reporter may not set, so asking costs nothing.
            if (!labels.isEmpty()) url.append("&labels=").append(encode(String.join(",", labels)));
            return URI.create(url.toString());
        }
    }

    private record GitLab(String host, String project) implements Tracker {
        @Override
        public String displayName() {
            return "GitLab (" + project + ")";
        }

        @Override
        public URI newIssue(String title, String body, List<String> labels) {
            return URI.create("https://" + host + "/" + project + "/-/issues/new?issue%5Btitle%5D=" + encode(title)
                    + "&issue%5Bdescription%5D=" + encode(body));
        }
    }

    private record Gitea(String host, String repo) implements Tracker {
        @Override
        public String displayName() {
            return (host.equalsIgnoreCase("codeberg.org") ? "Codeberg (" : host + " (") + repo + ")";
        }

        @Override
        public URI newIssue(String title, String body, List<String> labels) {
            return URI.create("https://" + host + "/" + repo + "/issues/new?title=" + encode(title) + "&body=" + encode(body));
        }
    }

    private record Template(String name, String urlTemplate) implements Tracker {
        @Override
        public String displayName() {
            return name;
        }

        @Override
        public URI newIssue(String title, String body, List<String> labels) {
            return URI.create(urlTemplate.replace("{title}", encode(title)).replace("{body}", encode(body))
                    .replace("{labels}", encode(String.join(",", labels))));
        }
    }

    private record Link(URI uri) implements Tracker {
        @Override
        public String displayName() {
            return uri.getHost() == null ? uri.toString() : uri.getHost();
        }

        @Override
        public URI newIssue(String title, String body, List<String> labels) {
            return uri;
        }

        @Override
        public boolean prefills() {
            return false;
        }
    }
}
