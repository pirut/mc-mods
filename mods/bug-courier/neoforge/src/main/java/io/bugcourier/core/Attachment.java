package io.bugcourier.core;

import java.nio.charset.StandardCharsets;

/**
 * One file or block of text the player can send with a report. {@code included} is the player's checkbox;
 * the text is already redacted when the attachment is made, so the preview shows exactly what is sent.
 */
public final class Attachment {
    private final String fileName;
    private final String label;
    private final String text;
    private boolean included;

    public Attachment(String fileName, String label, String text, boolean included) {
        this.fileName = fileName;
        this.label = label;
        this.text = text == null ? "" : text;
        this.included = included;
    }

    public String fileName() {
        return fileName;
    }

    public String label() {
        return label;
    }

    public String text() {
        return text;
    }

    public boolean included() {
        return included;
    }

    public void setIncluded(boolean included) {
        this.included = included;
    }

    public int sizeBytes() {
        return text.getBytes(StandardCharsets.UTF_8).length;
    }
}
