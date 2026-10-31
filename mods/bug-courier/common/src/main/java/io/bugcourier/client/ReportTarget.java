package io.bugcourier.client;

import io.bugcourier.api.ReportConfig;
import io.bugcourier.core.Tracker;
import java.util.List;

/** An installed mod the player can report to, with where its reports go. */
public record ReportTarget(String modId, String name, String version, Tracker tracker, List<String> labels, ReportConfig config) {
}
