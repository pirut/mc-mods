package io.github.pirut.ccagent;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dan200.computercraft.shared.computer.core.ServerContext;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * A computer's own files, straight from the world save (computercraft/computer/ID), so they work
 * while the computer is off. /rom and disk drives aren't in there: read those with run_lua and fs.
 */
public final class FileAccess {
    private static final long MAX_READ = 1 << 20;
    private final MinecraftServer server;

    public FileAccess(MinecraftServer server) {
        this.server = server;
    }

    public Path root(int computerId) {
        return ServerContext.get(server).storageDir().resolve("computer").resolve(Integer.toString(computerId));
    }

    public Path resolve(int computerId, String path) {
        var root = root(computerId).toAbsolutePath().normalize();
        var clean = path == null ? "" : path.replace('\\', '/').trim();
        while (clean.startsWith("/")) clean = clean.substring(1);
        var first = clean.split("/", 2)[0];
        if (first.equals("rom")) throw new ToolException("/rom is the read-only ROM, not on disk: read it with run_lua and fs");
        if (first.matches("disk\\d*")) throw new ToolException("/" + first + " is a disk drive mount: use run_lua with fs for it");
        var resolved = root.resolve(clean).normalize();
        if (!resolved.startsWith(root)) throw new ToolException("path leaves the computer's drive: " + path);
        return resolved;
    }

    private static String shown(Path root, Path file) {
        var rel = root.relativize(file).toString().replace('\\', '/');
        return "/" + rel;
    }

    public JsonObject list(int computerId, String path, boolean recursive) throws IOException {
        var root = root(computerId).toAbsolutePath().normalize();
        var dir = resolve(computerId, path);
        var out = new JsonObject();
        // Relay players don't get to see where the server keeps its world.
        if (Access.current() == null) out.addProperty("drive", root.toString());
        var entries = new JsonArray();
        if (!Files.exists(dir)) {
            if (dir.equals(root)) {
                out.add("entries", entries);
                out.addProperty("note", "empty drive (the computer has never saved a file)");
                return out;
            }
            throw new ToolException("no such directory: " + path);
        }
        if (!Files.isDirectory(dir)) throw new ToolException("not a directory: " + path);
        var found = new ArrayList<Path>();
        try (Stream<Path> stream = recursive ? Files.walk(dir, 16) : Files.list(dir)) {
            stream.filter(p -> !p.equals(dir)).limit(2000).forEach(found::add);
        }
        found.sort(Comparator.comparing(Path::toString));
        long total = 0;
        for (var p : found) {
            var entry = new JsonObject();
            entry.addProperty("path", shown(root, p));
            boolean isDir = Files.isDirectory(p);
            entry.addProperty("dir", isDir);
            if (!isDir) {
                long size = Files.size(p);
                total += size;
                entry.addProperty("size", size);
            }
            entries.add(entry);
        }
        out.add("entries", entries);
        out.addProperty("bytes_listed", total);
        return out;
    }

    public String read(int computerId, String path) throws IOException {
        var file = resolve(computerId, path);
        if (!Files.isRegularFile(file)) throw new ToolException("no such file: " + path);
        if (Files.size(file) > MAX_READ) throw new ToolException("file is over 1 MiB");
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }

    public long write(int computerId, String path, String content, boolean append) throws IOException {
        checkWrites();
        var file = resolve(computerId, path);
        if (file.equals(root(computerId).toAbsolutePath().normalize())) throw new ToolException("give a file path");
        if (Files.isDirectory(file)) throw new ToolException("that's a directory: " + path);
        Files.createDirectories(file.getParent());
        var bytes = content.getBytes(StandardCharsets.UTF_8);
        if (append && Files.exists(file)) {
            Files.write(file, bytes, java.nio.file.StandardOpenOption.APPEND);
        } else {
            var tmp = file.resolveSibling("." + file.getFileName() + ".ccagent-tmp");
            Files.write(tmp, bytes);
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        return Files.size(file);
    }

    public int edit(int computerId, String path, String oldText, String newText, boolean all) throws IOException {
        var text = read(computerId, path);
        if (oldText.isEmpty()) throw new ToolException("old_text is empty");
        int count = 0;
        for (int i = text.indexOf(oldText); i >= 0; i = text.indexOf(oldText, i + oldText.length())) count++;
        if (count == 0) throw new ToolException("old_text not found in " + path);
        if (count > 1 && !all) throw new ToolException("old_text appears " + count + " times; add more context or set replace_all");
        var updated = all ? text.replace(oldText, newText) : text.replaceFirst(java.util.regex.Pattern.quote(oldText),
            java.util.regex.Matcher.quoteReplacement(newText));
        write(computerId, path, updated, false);
        return count;
    }

    public void delete(int computerId, String path) throws IOException {
        checkWrites();
        var file = resolve(computerId, path);
        if (file.equals(root(computerId).toAbsolutePath().normalize())) throw new ToolException("won't delete the whole drive");
        if (!Files.exists(file)) throw new ToolException("no such file or directory: " + path);
        try (var walk = Files.walk(file)) {
            for (var p : walk.sorted(Comparator.reverseOrder()).toList()) Files.delete(p);
        }
    }

    public void move(int computerId, String from, String to) throws IOException {
        checkWrites();
        var source = resolve(computerId, from);
        var target = resolve(computerId, to);
        if (!Files.exists(source)) throw new ToolException("no such file or directory: " + from);
        if (Files.exists(target)) throw new ToolException("destination exists: " + to);
        Files.createDirectories(target.getParent());
        Files.move(source, target);
    }

    public void mkdir(int computerId, String path) throws IOException {
        checkWrites();
        Files.createDirectories(resolve(computerId, path));
    }

    private static void checkWrites() {
        if (!CCAgent.config().allowFileWrites) throw new ToolException("file writes are turned off (allowFileWrites=false in ccagent.properties)");
    }
}
