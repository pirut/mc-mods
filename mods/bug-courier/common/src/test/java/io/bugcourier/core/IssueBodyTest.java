package io.bugcourier.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class IssueBodyTest {
    private static Report report(String logText) {
        Report r = new Report("earthplatform", "Earth Platform", "0.1.0-alpha.65");
        r.setTitle("Far terrain flickers");
        r.setDescription("When I zoom with the spyglass the far mountains flicker.");
        r.environment().put("Minecraft", "1.21.1");
        r.environment().put("GPU", "NVIDIA GeForce RTX 2080 Ti");
        r.attachments().add(new Attachment("latest.log", "Game log", logText, true));
        return r;
    }

    @Test
    void titleLeadsWithVersion() {
        Report r = report("");
        r.setCrashSignature("3f9a1c");
        assertEquals("[0.1.0-alpha.65] Far terrain flickers (crash:3f9a1c)", IssueBody.title(r));
    }

    @Test
    void hugeLogStillFitsTheLink() {
        String log = "[12:00:00] [Render thread/INFO]: a line of log that goes on for a while\n".repeat(5000);
        IssueBody.Prefilled p = IssueBody.prefill(report(log), Trackers.github("pirut/earth-platform"), List.of("bug"), "bugcourier-1.zip", "0.1.0");
        assertTrue(p.uri().toString().length() <= 8000, "link is " + p.uri().toString().length());
        assertTrue(p.body().contains("When I zoom with the spyglass"));
        assertTrue(p.body().contains("bugcourier-1.zip"));
    }

    @Test
    void hugeDescriptionIsCutNotDropped() {
        Report r = report("");
        r.setDescription("word ".repeat(4000));
        IssueBody.Prefilled p = IssueBody.prefill(r, Trackers.github("a/b"), List.of(), "x.zip", "0.1.0");
        assertTrue(p.uri().toString().length() <= 8000);
        assertTrue(p.body().contains("description cut to fit"));
    }

    @Test
    void fullReportHoldsEveryIncludedFile() {
        Report r = report("line one\nline two");
        r.attachments().add(new Attachment("mods.txt", "Mod list", "sodium 0.8.13", false));
        String full = IssueBody.full(r, "0.1.0");
        assertTrue(full.contains("line two"));
        assertTrue(!full.contains("sodium 0.8.13"));
    }

    @Test
    void fenceOutgrowsBackticksInside() {
        assertTrue(IssueBody.fence("a ``` b").startsWith("````\n"));
    }
}
