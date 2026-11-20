package io.bugcourier.client;

import io.bugcourier.BugCourier;
import io.bugcourier.api.ReportConfig;
import io.bugcourier.core.Attachment;
import io.bugcourier.core.CrashReports;
import io.bugcourier.core.IssueBody;
import io.bugcourier.core.Redactor;
import io.bugcourier.core.Report;
import io.bugcourier.platform.Platform;
import io.bugcourier.client.version.Mc;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;

/** Gathers what a report can carry. Every text passes through the redactor before anyone sees it. */
final class Collectors {
    private static final String LOG = IssueBody.LOG_FILE;
    private static final String CRASH = IssueBody.CRASH_FILE;
    private static final String MODS = "mods.txt";
    private static final int LOG_TAIL_BYTES = 256 * 1024;
    private static final int FILE_MAX_BYTES = 128 * 1024;
    /** How old a crash report may be and still be offered, after a crash or in a report made by hand. */
    static final Duration RECENT_CRASH = Duration.ofDays(3);

    private Collectors() {
    }

    static Redactor redactor(boolean includePlayer) {
        Redactor r = new Redactor();
        // Four-part versions look like IP addresses; keep every installed mod's so the mod list stays readable.
        for (Platform.ModEntry mod : Platform.get().allMods()) r.keep(mod.version());
        if (!includePlayer) {
            var user = Minecraft.getInstance().getUser();
            r.hide(user.getName());
            if (user.getProfileId() != null) {
                r.hide(user.getProfileId().toString());
                r.hide(user.getProfileId().toString().replace("-", ""));
            }
        }
        return r;
    }

    static Report build(ReportTarget target, String title, String description, String crashText, boolean includePlayer) {
        Redactor redact = redactor(includePlayer);
        Report report = new Report(target.modId(), target.name(), target.version());
        report.setTitle(redact.apply(title));
        report.setDescription(redact.apply(description));

        var env = report.environment();
        env.put(target.name(), target.version());
        env.put("Minecraft", Mc.gameVersion());
        Platform platform = Platform.get();
        env.put(platform.loaderName(), platform.loaderVersion());
        env.put("Bug Courier", Sender.libraryVersion());
        env.put("Mods installed", Integer.toString(platform.allMods().size()));
        env.put("OS", System.getProperty("os.name") + " " + System.getProperty("os.version") + " (" + System.getProperty("os.arch") + ")");
        env.put("Java", System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")");
        env.put("CPU", Mc.cpu() + ", " + Runtime.getRuntime().availableProcessors() + " threads");
        env.put("GPU", Mc.gpu());
        env.put("Memory", (Runtime.getRuntime().maxMemory() >> 20) + " MB allowed for Java");
        ReportConfig config = target.config();
        if (config != null) {
            for (ReportConfig.Context row : config.contextRows()) {
                try {
                    env.put(row.label(), redact.apply(String.valueOf(row.value().get())));
                } catch (RuntimeException e) {
                    env.put(row.label(), "(failed: " + e.getClass().getSimpleName() + ")");
                }
            }
        }
        // The rows above come from the game and the system, not the player; context rows were redacted one by one.
        // (Redacting them all hid four-part Java versions such as 21.0.12.1 as IP addresses.)

        // Opened from the crash prompt: that crash is the report. Opened by hand: offer a recent crash, unticked.
        boolean fromCrash = crashText != null;
        if (!fromCrash) crashText = newestCrash(RECENT_CRASH).map(Collectors::read).orElse(null);
        if (crashText != null) {
            report.attachments().add(new Attachment(CRASH, "Crash report", redact.apply(crashText), fromCrash));
            if (fromCrash) report.setCrashSignature(CrashReports.signature(crashText));
        }
        readTail(gameDir().resolve("logs").resolve(LOG), LOG_TAIL_BYTES)
                .ifPresent(text -> report.attachments().add(new Attachment(LOG, "Game log", redact.apply(text), true)));
        report.attachments().add(new Attachment(MODS, "Mod list", redact.apply(modList()), true));
        if (config != null) {
            for (ReportConfig.FileAttachment f : config.files()) {
                readTail(f.path(), FILE_MAX_BYTES).ifPresent(text -> report.attachments().add(new Attachment(f.fileName(), f.label(), redact.apply(text), true)));
            }
        }
        return report;
    }

    /** The newest crash report, if it was written within {@code age}. */
    static Optional<Path> newestCrash(Duration age) {
        Path dir = gameDir().resolve("crash-reports");
        if (!Files.isDirectory(dir)) return Optional.empty();
        Instant since = Instant.now().minus(age);
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(p -> p.getFileName().toString().endsWith(".txt"))
                    .filter(p -> modified(p).isAfter(since))
                    .max(Comparator.comparing(Collectors::modified));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    static Instant modified(Path p) {
        try {
            return Files.getLastModifiedTime(p).toInstant();
        } catch (IOException e) {
            return Instant.EPOCH;
        }
    }

    static String read(Path p) {
        return readTail(p, Integer.MAX_VALUE).orElse(null);
    }

    static Path gameDir() {
        return Platform.get().gameDir();
    }

    private static String modList() {
        StringBuilder b = new StringBuilder();
        Platform.get().allMods().stream().sorted(Comparator.comparing(Platform.ModEntry::id)).forEach(m -> b.append(m.id()).append(' ')
                .append(m.version()).append("  (").append(m.file()).append(")\n"));
        return b.toString();
    }

    // The log is still being written, so read only its end and start at a whole line.
    private static Optional<String> readTail(Path p, int maxBytes) {
        if (p == null || !Files.isRegularFile(p)) return Optional.empty();
        try (RandomAccessFile f = new RandomAccessFile(p.toFile(), "r")) {
            long len = f.length();
            long start = Math.max(0, len - maxBytes);
            byte[] bytes = new byte[(int) (len - start)];
            f.seek(start);
            f.readFully(bytes);
            String text = new String(bytes, StandardCharsets.UTF_8);
            if (start > 0) {
                int nl = text.indexOf('\n');
                text = "[... earlier lines left out ...]\n" + (nl >= 0 ? text.substring(nl + 1) : text);
            }
            return Optional.of(text);
        } catch (IOException e) {
            BugCourier.LOG.warn("Could not read {}: {}", p.getFileName(), e.toString());
            return Optional.empty();
        }
    }
}
