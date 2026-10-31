package io.bugcourier.api;

import io.bugcourier.core.Tracker;
import io.bugcourier.core.Trackers;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** How one mod takes bug reports. Made through {@link BugReports#register}, or read from neoforge.mods.toml. */
public final class ReportConfig {
    private Tracker tracker;
    private final List<String> labels = new ArrayList<>();
    private final List<Context> context = new ArrayList<>();
    private final List<FileAttachment> files = new ArrayList<>();

    /** Where reports go. */
    public ReportConfig tracker(Tracker tracker) {
        this.tracker = tracker;
        return this;
    }

    /** Where reports go, as {@code github:owner/repo}, {@code gitlab:group/project}, {@code codeberg:owner/repo} or a link. */
    public ReportConfig tracker(String spec) {
        this.tracker = Trackers.parse(spec).orElseThrow(() -> new IllegalArgumentException("Unknown issue tracker: " + spec));
        return this;
    }

    /** Labels to ask for on new issues, such as "bug" or "player-report". */
    public ReportConfig labels(String... labels) {
        for (String l : labels) if (l != null && !l.isBlank()) this.labels.add(l.strip());
        return this;
    }

    /**
     * A short fact shown in the issue's Environment table, such as a render setting. Called when the player opens
     * the preview, on the render thread; keep it quick and leave out anything personal.
     */
    public ReportConfig context(String label, Supplier<String> value) {
        context.add(new Context(label, value));
        return this;
    }

    /** A file the player can choose to send, such as the mod's config. Read and redacted when the report opens. */
    public ReportConfig attach(String fileName, String label, Path path) {
        files.add(new FileAttachment(fileName, label, path));
        return this;
    }

    public Tracker tracker() {
        return tracker;
    }

    public List<String> labels() {
        return List.copyOf(labels);
    }

    public List<Context> contextRows() {
        return List.copyOf(context);
    }

    public List<FileAttachment> files() {
        return List.copyOf(files);
    }

    public record Context(String label, Supplier<String> value) {
    }

    public record FileAttachment(String fileName, String label, Path path) {
    }
}
