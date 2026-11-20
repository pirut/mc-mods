package io.github.pirut.seaworthy.sable;

import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Wearing Create's Engineer's Goggles and looking at a ship shows how flooded it is. Only ever loaded on the client.
 *
 * <p>Pumps aren't shown: only the server knows which are running ({@code /seaworthy status} says).
 */
public final class GoggleHud {
    private static final ResourceLocation GOGGLES = ResourceLocation.fromNamespaceAndPath("create", "goggles");
    private static final int REFRESH_TICKS = 10;
    private static final int MAX_LINES = 8;
    private static final int TEXT = 0xFFFFFFFF, WARNING = 0xFFFF7070, BACKGROUND = 0x90000000;

    private static List<Line> lines = List.of();
    private static ShipHull lastHull;
    private static long lastRefresh;

    private record Line(String text, int colour) {
    }

    private GoggleHud() {
    }

    public static void render(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui || mc.screen != null) return;
        if (!GOGGLES.equals(BuiltInRegistries.ITEM.getKey(mc.player.getItemBySlot(EquipmentSlot.HEAD).getItem()))) return;
        if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
        ShipHull hull = hullAt(hit.getBlockPos());
        if (hull == null) return;

        long now = mc.level.getGameTime();
        if (hull != lastHull || now - lastRefresh >= REFRESH_TICKS || now < lastRefresh) {
            lastHull = hull;
            lines = describe(hull);
            lastRefresh = now;
        }
        int width = 0;
        for (Line line : lines) width = Math.max(width, mc.font.width(line.text));
        int x = graphics.guiWidth() / 2 + 12, y = graphics.guiHeight() / 2 - lines.size() * 5;
        graphics.fill(x - 3, y - 3, x + width + 3, y + lines.size() * 10 + 1, BACKGROUND);
        for (Line line : lines) {
            graphics.drawString(mc.font, line.text, x, y, line.colour);
            y += 10;
        }
    }

    private static ShipHull hullAt(BlockPos pos) {
        ShipTracker tracker = ShipTracker.of(Minecraft.getInstance().level);
        if (tracker == null) return null;
        ShipHull[] found = {null};
        tracker.forEachHull(hull -> {
            BoundingBox3ic b = hull.subLevel().getPlot().getBoundingBox();
            if (pos.getX() >= b.minX() && pos.getX() <= b.maxX() && pos.getY() >= b.minY() && pos.getY() <= b.maxY()
                    && pos.getZ() >= b.minZ() && pos.getZ() <= b.maxZ()) {
                found[0] = hull;
            }
        });
        return found[0];
    }

    private static List<Line> describe(ShipHull hull) {
        List<ShipHull.CompartmentStatus> compartments = hull.status();
        List<Line> out = new ArrayList<>();
        if (compartments.isEmpty()) {
            out.add(new Line("Hull open to the sea", WARNING));
            return out;
        }
        out.add(new Line("Hull", TEXT));
        int n = 1;
        for (ShipHull.CompartmentStatus c : compartments) {
            if (out.size() == MAX_LINES) {
                out.add(new Line("...and " + (compartments.size() - n + 1) + " more", TEXT));
                break;
            }
            String leaks = c.leaks() == 0 ? "" : c.leaks() == 1 ? ", 1 leak" : ", " + c.leaks() + " leaks";
            String text = String.format(Locale.ROOT, "%d: %.0f%% flooded%s", n++, 100 * c.volume() / c.size(), leaks);
            out.add(new Line(text, c.leaks() > 0 || c.volume() >= 1 ? WARNING : TEXT));
        }
        return out;
    }
}
