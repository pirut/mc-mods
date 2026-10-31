package io.github.pirut.ccagent;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CliArgsTest {
    @Test
    void acceptsRealModelsEffortsAndSessions() {
        for (var value : List.of("sonnet", "opus", "claude-opus-4-1[1m]", "gpt-5-codex", "high",
                "3f2c7a10-9b1e-4c55-8f0a-1d2e3f4a5b6c", "us.anthropic.claude-sonnet-4@20250514")) {
            assertDoesNotThrow(() -> CliArgs.check("model", value), value);
        }
        assertDoesNotThrow(() -> CliArgs.check("model", null));
    }

    @Test
    void refusesShellMetacharacters() {
        // On Windows the npm shims run through cmd.exe, where these would start other programs.
        for (var value : List.of("sonnet&calc", "x|y", "a b", "\"q\"", "%PATH%", "x^y", "a>b", "", "--x;rm")) {
            assertThrows(ToolException.class, () -> CliArgs.check("model", value), value);
        }
    }

    @Test
    void splitsConfigArgsWithQuotes() {
        assertEquals(List.of("--model", "sonnet", "a b"), CliArgs.split("--model sonnet \"a b\""));
        assertEquals(List.of(), CliArgs.split("  "));
    }
}
