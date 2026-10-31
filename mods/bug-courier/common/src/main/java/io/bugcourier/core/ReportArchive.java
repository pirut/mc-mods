package io.bugcourier.core;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Saves the report and its included attachments as one zip the player drags into the issue. */
public final class ReportArchive {
    private ReportArchive() {
    }

    public static Path write(Report report, String fullMarkdown, Path file) throws IOException {
        Files.createDirectories(file.getParent());
        try (OutputStream out = Files.newOutputStream(file); ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            Set<String> used = new HashSet<>();
            put(zip, unique(used, "report.md"), fullMarkdown);
            for (Attachment a : report.includedAttachments()) put(zip, unique(used, safeName(a.fileName())), a.text());
        }
        return file;
    }

    private static void put(ZipOutputStream zip, String name, String text) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(text.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    // A zip can't hold two entries with one name, and a mod may attach a file called latest.log too.
    static String unique(Set<String> used, String name) {
        if (used.add(name)) return name;
        int dot = name.lastIndexOf('.');
        String stem = dot > 0 ? name.substring(0, dot) : name;
        String ext = dot > 0 ? name.substring(dot) : "";
        for (int i = 2; ; i++) {
            String candidate = stem + "-" + i + ext;
            if (used.add(candidate)) return candidate;
        }
    }

    static String safeName(String name) {
        String n = name == null ? "" : name.replace('\\', '/');
        n = n.substring(n.lastIndexOf('/') + 1);
        return n.isBlank() ? "attachment.txt" : n;
    }
}
