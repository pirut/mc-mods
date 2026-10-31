package io.github.pirut.ccagent;

import dan200.computercraft.shared.computer.core.ServerComputer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Questions an agent puts to the player on a CC computer: the chat program shows them with buttons
 * ("ccagent_ask" events) and answers with {@code ccagent.answer}. Also carries Claude's permission prompts.
 */
public final class Questions {
    /** The computer whose chat the current MCP request comes from (the X-CC-Agent-Chat header), if any. */
    static final ThreadLocal<Integer> CHAT = new ThreadLocal<>();
    static final String CHAT_HEADER = "X-CC-Agent-Chat";

    private record Pending(int computerId, CompletableFuture<String> answer) {
    }

    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);
    private static final Map<Integer, Pending> PENDING = new ConcurrentHashMap<>();

    private Questions() {
    }

    /** Ask and wait for the player's answer. kind is "question" or "permission". */
    static String ask(ServerComputer computer, String kind, String question, List<String> options, boolean other,
                      int timeoutSeconds) {
        int id = NEXT_ID.getAndIncrement();
        var pending = new Pending(computer.getID(), new CompletableFuture<>());
        PENDING.put(id, pending);
        Map<Integer, String> luaOptions = new HashMap<>();
        for (int i = 0; i < options.size(); i++) luaOptions.put(i + 1, LuaStrings.toLua(options.get(i)));
        computer.queueEvent("ccagent_ask", new Object[]{id, kind, LuaStrings.toLua(question), luaOptions, other});
        try {
            return pending.answer.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            computer.queueEvent("ccagent_ask_closed", new Object[]{id});
            throw new ToolException("the player didn't answer within " + timeoutSeconds + " s");
        } catch (ExecutionException e) {
            throw new ToolException(e.getCause().getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ToolException("interrupted");
        } finally {
            PENDING.remove(id);
        }
    }

    static boolean answer(int computerId, int id, String text) {
        var pending = PENDING.get(id);
        if (pending == null || pending.computerId != computerId) return false;
        return pending.answer.complete(text);
    }

    static void computerStopped(int computerId) {
        for (var pending : PENDING.values()) {
            if (pending.computerId == computerId) pending.answer.completeExceptionally(new ToolException("the computer shut down"));
        }
    }

    static void cancelAll() {
        for (var pending : PENDING.values()) pending.answer.completeExceptionally(new ToolException("the world closed"));
        PENDING.clear();
    }
}
