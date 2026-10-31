package io.github.pirut.ccagent;

/** A tool failed in a way the agent should read and act on. */
public final class ToolException extends RuntimeException {
    public ToolException(String message) {
        super(message);
    }
}
