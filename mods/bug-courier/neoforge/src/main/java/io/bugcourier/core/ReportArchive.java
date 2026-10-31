package io.bugcourier.core;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Saves the report and its included attachments as one zip the player drags into the issue. */
public final class ReportArchive {
    private ReportArchive() {
    }

    public static Path write(Report report, String fullMarkdown, Path file) throws IOException {
        Files.createDirectories(file.getParent());
        try (OutputStream out = Files.newOutputStream(file); ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            put(zip, "report.md", fullMarkdown);
            for (Attachment a : report.includedAttachments()) put(zip, safeName(a.fileName()), a.text());
        }
        return file;
    }

    private static void put(ZipOutputStream zip, String name, String text) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(text.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    static String safeName(String name) {
        String n = name.replace('\\', '/');
        n = n.substring(n.lastIndexOf('/') + 1);
        return n.isBlank() ? "attachment.txt" : n;
    }
}
