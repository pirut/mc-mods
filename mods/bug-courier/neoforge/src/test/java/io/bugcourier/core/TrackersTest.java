package io.bugcourier.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class TrackersTest {
    @Test
    void readsIssueTrackerUrls() {
        assertEquals("GitHub (pirut/earth-platform)", Trackers.parse("https://github.com/pirut/earth-platform/issues").orElseThrow().displayName());
        assertEquals("GitHub (owner/repo)", Trackers.parse("https://github.com/owner/repo.git").orElseThrow().displayName());
        assertEquals("GitLab (group/sub/project)", Trackers.parse("https://gitlab.com/group/sub/project/-/issues").orElseThrow().displayName());
        assertEquals("Codeberg (me/mod)", Trackers.parse("https://codeberg.org/me/mod/issues").orElseThrow().displayName());
        Tracker discord = Trackers.parse("https://discord.gg/abc").orElseThrow();
        assertFalse(discord.prefills());
    }

    @Test
    void readsShortSpecs() {
        assertEquals("GitHub (pirut/earth-platform)", Trackers.parse("github:pirut/earth-platform").orElseThrow().displayName());
        assertEquals("GitLab (group/project)", Trackers.parse("gitlab:group/project").orElseThrow().displayName());
        assertTrue(Trackers.parse("gitlab:git.example.org/group/project").orElseThrow().newIssue("t", "b", List.of()).toString()
                .startsWith("https://git.example.org/group/project/-/issues/new?"));
        assertEquals("Codeberg (me/mod)", Trackers.parse("codeberg:me/mod").orElseThrow().displayName());
        assertTrue(Trackers.parse("github:not a repo").isEmpty());
        assertTrue(Trackers.parse("").isEmpty());
        assertTrue(Trackers.parse("ftp://x").isEmpty());
    }

    @Test
    void githubLinkCarriesTitleBodyAndLabels() {
        String url = Trackers.github("pirut/earth-platform").newIssue("Far terrain flickers", "a & b", List.of("bug", "player-report")).toString();
        assertEquals("https://github.com/pirut/earth-platform/issues/new?title=Far%20terrain%20flickers&body=a%20%26%20b&labels=bug%2Cplayer-report", url);
    }
}
