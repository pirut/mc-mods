package io.bugcourier.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReportArchiveTest {
    @Test
    void sameFileNameTwiceStillMakesAZip(@TempDir Path dir) throws IOException {
        Report r = new Report("mymod", "My Mod", "1.0");
        r.attachments().add(new Attachment("latest.log", "Game log", "game", true));
        r.attachments().add(new Attachment("latest.log", "My mod's log", "mine", true));
        r.attachments().add(new Attachment("../../report.md", "Sneaky", "x", true));
        r.attachments().add(new Attachment("skipped.txt", "Unticked", "no", false));
        Path zip = ReportArchive.write(r, "# report", dir.resolve("out/report.zip"));

        List<String> names = new ArrayList<>();
        try (ZipInputStream in = new ZipInputStream(Files.newInputStream(zip))) {
            for (ZipEntry e; (e = in.getNextEntry()) != null; ) names.add(e.getName());
        }
        assertEquals(List.of("report.md", "latest.log", "latest-2.log", "report-2.md"), names);
    }

    @Test
    void uniqueNamesWithoutExtension() {
        Set<String> used = new HashSet<>();
        assertEquals("README", ReportArchive.unique(used, "README"));
        assertEquals("README-2", ReportArchive.unique(used, "README"));
        assertEquals("README-3", ReportArchive.unique(used, "README"));
    }
}
