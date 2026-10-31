package io.github.pirut.mcmods.lib;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.function.BiConsumer;

/**
 * Small .properties config and state files, the way the mods here keep them: UTF-8, read with defaults when the
 * file is missing or broken, written whole so a crash mid-write never leaves half a file.
 */
public final class PropertiesFile {
    private PropertiesFile() {
    }

    /** Reads {@code file}. A missing file gives empty properties; a broken one is reported to {@code onError}. */
    public static Properties read(Path file, BiConsumer<Path, IOException> onError) {
        Properties props = new Properties();
        if (Files.isRegularFile(file)) {
            try (Reader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                props.load(in);
            } catch (IOException e) {
                onError.accept(file, e);
            }
        }
        return props;
    }

    /** Writes {@code props} with a {@code #} comment line on top. Returns false (after telling onError) if it failed. */
    public static boolean write(Path file, Properties props, String comment, BiConsumer<Path, IOException> onError) {
        StringWriter text = new StringWriter();
        try {
            props.store(text, comment);
        } catch (IOException e) {
            onError.accept(file, e);
            return false;
        }
        return writeText(file, text.toString(), onError);
    }

    /**
     * Writes hand-laid-out text (with comments and blank lines where you want them). Values that may hold
     * backslashes, such as Windows paths, go through {@link #escape} first.
     */
    public static boolean writeText(Path file, String text, BiConsumer<Path, IOException> onError) {
        try {
            Path dir = file.toAbsolutePath().getParent();
            Files.createDirectories(dir);
            Path tmp = Files.createTempFile(dir, file.getFileName().toString(), ".tmp");
            try (Writer out = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                out.write(text);
            }
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicFailed) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException e) {
            onError.accept(file, e);
            return false;
        }
    }

    /** A trimmed value, or {@code fallback} when the key is missing. */
    public static String string(Properties props, String key, String fallback) {
        String value = props.getProperty(key);
        return value == null ? fallback : value.trim();
    }

    /** "true" (any case) is true; any other value is false; a missing key is {@code fallback}. */
    public static boolean bool(Properties props, String key, boolean fallback) {
        String value = props.getProperty(key);
        return value == null ? fallback : Boolean.parseBoolean(value.trim());
    }

    /** A whole number, or {@code fallback} when the key is missing or not a number. */
    public static int integer(Properties props, String key, int fallback) {
        String value = props.getProperty(key);
        if (value == null) return fallback;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** Escapes backslashes for a value written with {@link #writeText}, which .properties reads as escapes. */
    public static String escape(String value) {
        return value.replace("\\", "\\\\");
    }
}
