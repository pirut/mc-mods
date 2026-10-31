package io.bugcourier.core;

import java.net.URI;
import java.util.List;

/** Where a mod's bug reports go. Each tracker turns a report into a page the player opens in the browser. */
public interface Tracker {
    /** Short name for the screen, such as "GitHub (pirut/earth-platform)". */
    String displayName();

    /** The page that starts a new issue with the title, body and labels filled in where the tracker allows it. */
    URI newIssue(String title, String body, List<String> labels);

    /** False for a plain link, where nothing can be filled in and the player pastes the report. */
    default boolean prefills() {
        return true;
    }

    /** Longest URL the tracker is known to accept; the body is cut to fit. */
    default int maxUrlLength() {
        return 8000;
    }
}
