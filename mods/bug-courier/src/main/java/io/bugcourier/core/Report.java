package io.bugcourier.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Everything one bug report holds, as the player is about to send it. */
public final class Report {
    private final String modId;
    private final String modName;
    private final String modVersion;
    private String title = "";
    private String description = "";
    // Short facts shown inline in the issue, such as versions and hardware. Already redacted.
    private final Map<String, String> environment = new LinkedHashMap<>();
    private final List<Attachment> attachments = new ArrayList<>();
    private String crashSignature;

    public Report(String modId, String modName, String modVersion) {
        this.modId = modId;
        this.modName = modName;
        this.modVersion = modVersion;
    }

    public String modId() {
        return modId;
    }

    public String modName() {
        return modName;
    }

    public String modVersion() {
        return modVersion;
    }

    public String title() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title == null ? "" : title.strip();
    }

    public String description() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description == null ? "" : description.strip();
    }

    public Map<String, String> environment() {
        return environment;
    }

    public List<Attachment> attachments() {
        return attachments;
    }

    public List<Attachment> includedAttachments() {
        return attachments.stream().filter(Attachment::included).toList();
    }

    public Attachment attachment(String fileName) {
        for (Attachment a : attachments) if (a.fileName().equals(fileName)) return a;
        return null;
    }

    public String crashSignature() {
        return crashSignature;
    }

    public void setCrashSignature(String crashSignature) {
        this.crashSignature = crashSignature;
    }
}
