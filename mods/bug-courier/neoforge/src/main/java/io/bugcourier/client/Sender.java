package io.bugcourier.client;

import io.bugcourier.BugCourier;
import io.bugcourier.core.Cooldown;
import io.bugcourier.core.IssueBody;
import io.bugcourier.core.Report;
import io.bugcourier.core.ReportArchive;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.fml.ModList;

/**
 * The browser handoff: save the zip, copy the full report, open the tracker's new-issue page filled in.
 * No account, token or server of ours is involved; the player files the issue themselves.
 */
final class Sender {
    private static final Cooldown COOLDOWN = new Cooldown(Clock.systemUTC(), Duration.ofMinutes(5));
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private Sender() {
    }

    static Duration cooldownLeft(String modId) {
        return COOLDOWN.remaining(modId);
    }

    static String libraryVersion() {
        return ModList.get().getModContainerById(BugCourier.MOD_ID).map(c -> c.getModInfo().getVersion().toString()).orElse("?");
    }

    static Screen send(Screen preview, Screen form, ReportTarget target, Report report, String full) {
        Minecraft mc = Minecraft.getInstance();
        Path dir = Collectors.gameDir().resolve("bug-reports");
        String name = "bug-report-" + target.modId() + "-" + LocalDateTime.now().format(STAMP) + ".zip";
        Path zip = null;
        try {
            zip = ReportArchive.write(report, full, dir.resolve(name));
        } catch (IOException e) {
            BugCourier.LOG.error("Could not save the bug report zip", e);
        }
        mc.keyboardHandler.setClipboard(full);
        IssueBody.Prefilled prefilled = IssueBody.prefill(report, target.tracker(), target.labels(), name, libraryVersion());
        Util.getPlatform().openUri(prefilled.uri());
        COOLDOWN.mark(target.modId());
        BugCourier.LOG.info("Opened a bug report for {} at {} ({} character link)", target.modId(), target.tracker().displayName(),
                prefilled.uri().toString().length());
        return new SentScreen(formParent(form), target, prefilled, zip, full);
    }

    private static Screen formParent(Screen form) {
        return form instanceof ReportScreen r ? r.parentScreen() : form;
    }
}
