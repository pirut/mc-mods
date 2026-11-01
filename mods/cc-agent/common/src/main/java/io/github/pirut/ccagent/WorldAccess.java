package io.github.pirut.ccagent;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dan200.computercraft.shared.computer.core.ServerComputer;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.TreeMap;

/** Blocks, chests and turtle inventories, read on the server thread. */
public final class WorldAccess {
    private final Computers computers;

    public WorldAccess(Computers computers) {
        this.computers = computers;
    }

    public ServerLevel level(String dimension) {
        var server = computers.server();
        if (dimension == null || dimension.isBlank()) return server.overworld();
        var key = ResourceKey.create(Registries.DIMENSION, McNames.parseId(dimension.contains(":") ? dimension : "minecraft:" + dimension));
        var level = server.getLevel(key);
        if (level == null) throw new ToolException("unknown dimension " + dimension);
        return level;
    }

    public static JsonObject item(ItemStack stack, int slot) {
        var out = new JsonObject();
        if (slot > 0) out.addProperty("slot", slot);
        out.addProperty("item", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        out.addProperty("count", stack.getCount());
        out.addProperty("name", stack.getHoverName().getString());
        if (stack.isDamageableItem()) out.addProperty("damage", stack.getDamageValue() + "/" + stack.getMaxDamage());
        return out;
    }

    public static JsonArray items(Container container) {
        var list = new JsonArray();
        for (int i = 0; i < container.getContainerSize(); i++) {
            var stack = container.getItem(i);
            if (!stack.isEmpty()) list.add(item(stack, i + 1));
        }
        return list;
    }

    public JsonObject inspect(ServerLevel level, BlockPos pos) {
        return computers.onServer(() -> {
            var out = new JsonObject();
            out.addProperty("x", pos.getX());
            out.addProperty("y", pos.getY());
            out.addProperty("z", pos.getZ());
            if (!level.isLoaded(pos)) {
                out.addProperty("loaded", false);
                return out;
            }
            var state = level.getBlockState(pos);
            out.addProperty("block", BlockStateParser.serialize(state));
            var be = level.getBlockEntity(pos);
            if (be != null) {
                out.addProperty("block_entity", BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType()).toString());
                if (be instanceof Container container) {
                    out.addProperty("slots", container.getContainerSize());
                    out.add("items", items(container));
                }
                var nbt = be.saveWithoutMetadata(level.registryAccess()).toString();
                out.addProperty("nbt", nbt.length() > 4000 ? nbt.substring(0, 4000) + "..." : nbt);
            }
            return out;
        });
    }

    public JsonObject scan(ServerLevel level, BlockPos center, int radius, boolean includeAir) {
        int r = Math.max(0, Math.min(radius, 8));
        return computers.onServer(() -> {
            var out = new JsonObject();
            out.addProperty("center", center.getX() + "," + center.getY() + "," + center.getZ());
            out.addProperty("radius", r);
            var blocks = new JsonArray();
            Map<String, Integer> counts = new TreeMap<>();
            var pos = new BlockPos.MutableBlockPos();
            int unloaded = 0;
            for (int dy = r; dy >= -r; dy--) {
                for (int dz = -r; dz <= r; dz++) {
                    for (int dx = -r; dx <= r; dx++) {
                        pos.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                        if (!level.isLoaded(pos)) {
                            unloaded++;
                            continue;
                        }
                        var state = level.getBlockState(pos);
                        if (state.isAir() && !includeAir) continue;
                        var name = BlockStateParser.serialize(state);
                        counts.merge(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(), 1, Integer::sum);
                        blocks.add(dx + " " + dy + " " + dz + " " + name);
                    }
                }
            }
            out.addProperty("format", "dx dy dz block[state], offsets from center; +x east, +y up, +z south");
            out.add("blocks", blocks);
            var summary = new JsonObject();
            counts.forEach(summary::addProperty);
            out.add("counts", summary);
            if (unloaded > 0) out.addProperty("unloaded_positions", unloaded);
            return out;
        });
    }

    public JsonObject turtleInventory(ServerComputer computer) {
        var turtle = computers.turtle(computer);
        if (turtle == null) throw new ToolException("computer #" + computer.getID() + " isn't a turtle");
        return computers.onServer(() -> {
            var access = turtle.getAccess();
            var out = new JsonObject();
            out.addProperty("selected_slot", access.getSelectedSlot() + 1);
            out.addProperty("fuel", access.isFuelNeeded() ? Integer.toString(access.getFuelLevel()) : "unlimited");
            out.addProperty("fuel_limit", access.getFuelLimit());
            out.addProperty("facing", access.getDirection().getName());
            out.add("items", items(access.getInventory()));
            for (var side : dan200.computercraft.api.turtle.TurtleSide.values()) {
                var upgrade = access.getUpgrade(side);
                if (upgrade != null) out.addProperty(side.name().toLowerCase() + "_upgrade", upgrade.getAdjective().getString());
            }
            return out;
        });
    }
}
