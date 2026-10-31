package io.bugcourier.core;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Writes the issue text. A prefilled new-issue link only holds about 8 KB, so the body carries the
 * description, the environment and short excerpts; the whole report goes to the clipboard and a zip.
 */
public final class IssueBody {
    public static final int MIN_DESCRIPTION = 20;
    private static final int[] EXCERPT_LINES = {40, 20, 10, 0};
    /** File names the short body looks for when it adds excerpts. */
    public static final String CRASH_FILE = "crash-report.txt";
    public static final String LOG_FILE = "latest.log";

    private IssueBody() {
    }

    /** The issue title: the mod's version first, so reports sort by build, then the crash tag if any. */
    public static String title(Report report) {
        StringBuilder t = new StringBuilder();
        if (report.modVersion() != null && !report.modVersion().isBlank()) t.append('[').append(report.modVersion()).append("] ");
        t.append(report.title().isBlank() ? "Bug report" : report.title());
        if (report.crashSignature() != null) t.append(" (crash:").append(report.crashSignature()).append(')');
        return t.toString();
    }

    /** Body and link that fit the tracker's URL limit. */
    public static Prefilled prefill(Report report, Tracker tracker, List<String> labels, String archiveName, String libraryVersion) {
        String title = title(report);
        String description = report.description();
        for (int round = 0; round < 8; round++) {
            for (int lines : EXCERPT_LINES) {
                String body = shortBody(report, description, lines, archiveName, libraryVersion);
                URI uri = tracker.newIssue(title, body, labels);
                if (!tracker.prefills() || uri.toString().length() <= tracker.maxUrlLength()) return new Prefilled(title, body, uri);
            }
            // Even with no excerpts it is too long: the player wrote a lot, so keep the start of it.
            description = cut(description, Math.max(200, description.length() / 2)) + "\n\n_(description cut to fit the link; the full text is in the zip)_";
        }
        String body = shortBody(report, "", 0, archiveName, libraryVersion);
        return new Prefilled(title, body, tracker.newIssue(title, body, labels));
    }

    /** The whole report as Markdown, for the clipboard and as report.md in the zip. */
    public static String full(Report report, String libraryVersion) {
        StringBuilder b = new StringBuilder();
        b.append("# ").append(title(report)).append("\n\n");
        b.append(report.description().isBlank() ? "_No description._" : report.description()).append("\n\n");
        environment(b, report.environment());
        for (Attachment a : report.includedAttachments()) {
            b.append("<details><summary>").append(a.label()).append(" (").append(a.fileName()).append(")</summary>\n\n");
            b.append(fence(a.text())).append("\n\n</details>\n\n");
        }
        b.append(footerLine(libraryVersion));
        return b.toString();
    }

    private static String shortBody(Report report, String description, int excerptLines, String archiveName, String libraryVersion) {
        StringBuilder b = new StringBuilder();
        b.append("### What happened\n\n").append(description.isBlank() ? "_No description._" : description).append("\n\n");
        environment(b, report.environment());
        if (excerptLines > 0) {
            Attachment crash = report.attachment(CRASH_FILE);
            if (crash != null && crash.included()) excerpt(b, "Crash report (start)", head(crash.text(), excerptLines));
            Attachment log = report.attachment(LOG_FILE);
            if (log != null && log.included()) excerpt(b, "Log (last lines)", tail(log.text(), excerptLines));
        }
        List<String> names = new ArrayList<>();
        for (Attachment a : report.includedAttachments()) names.add("`" + a.fileName() + "`");
        if (!names.isEmpty()) {
            b.append("### Attached files\n\n").append(String.join(", ", names)).append(" are in `").append(archiveName)
                    .append("`. **Drag that zip into this box**, or paste the full report from your clipboard.\n\n");
        }
        b.append(footerLine(libraryVersion));
        return b.toString();
    }

    private static void environment(StringBuilder b, Map<String, String> env) {
        if (env.isEmpty()) return;
        b.append("### Environment\n\n| | |\n| --- | --- |\n");
        env.forEach((k, v) -> b.append("| ").append(cell(k)).append(" | ").append(cell(v)).append(" |\n"));
        b.append('\n');
    }

    private static void excerpt(StringBuilder b, String label, String text) {
        if (text.isBlank()) return;
        b.append("<details><summary>").append(label).append("</summary>\n\n").append(fence(text)).append("\n\n</details>\n\n");
    }

    private static String footerLine(String libraryVersion) {
        return "<sub>Sent with Bug Courier " + libraryVersion + ". The player saw and approved everything above.</sub>\n";
    }

    // A fence longer than any backtick run inside, so logs that hold ``` cannot break out.
    static String fence(String text) {
        int longest = 0, run = 0;
        for (int i = 0; i < text.length(); i++) {
            run = text.charAt(i) == '`' ? run + 1 : 0;
            longest = Math.max(longest, run);
        }
        String f = "`".repeat(Math.max(3, longest + 1));
        return f + "\n" + text.stripTrailing() + "\n" + f;
    }

    private static String cell(String s) {
        return s.replace("|", "\\|").replace("\n", " ");
    }

    static String head(String text, int lines) {
        String[] all = text.split("\\R", -1);
        if (all.length <= lines) return text;
        return String.join("\n", Arrays.copyOfRange(all, 0, lines)) + "\n...";
    }

    static String tail(String text, int lines) {
        String[] all = text.stripTrailing().split("\\R", -1);
        if (all.length <= lines) return text;
        return "...\n" + String.join("\n", Arrays.copyOfRange(all, all.length - lines, all.length));
    }

    private static String cut(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }

    public record Prefilled(String title, String body, URI uri) {
    }
}
