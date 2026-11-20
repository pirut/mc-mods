package io.github.pirut.seaworthy.sable;

import com.mojang.brigadier.CommandDispatcher;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** {@code /seaworthy status}: how much water each ship nearby has in it. */
public final class StatusCommand {
    /** Anyone can look this far. Operators and the console see every ship in the level. */
    private static final double RANGE = 128;
    private static final int MAX_SHIPS = 10;

    private StatusCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("seaworthy").then(Commands.literal("status").executes(context -> {
            return status(context.getSource());
        })));
    }

    private static int status(CommandSourceStack source) {
        ShipTracker tracker = ShipTracker.of(source.getLevel());
        Vec3 from = source.getPosition();
        boolean everywhere = source.hasPermission(2);
        List<ShipHull> hulls = new ArrayList<>();
        if (tracker != null) {
            tracker.forEachHull(hull -> {
                if (everywhere || distance(hull, from) <= RANGE) hulls.add(hull);
            });
        }
        if (hulls.isEmpty()) {
            source.sendSuccess(() -> Component.literal(everywhere ? "No ships here." : "No ships nearby."), false);
            return 0;
        }
        hulls.sort(Comparator.comparingDouble(hull -> distance(hull, from)));
        for (ShipHull hull : hulls.subList(0, Math.min(MAX_SHIPS, hulls.size()))) {
            for (String line : describe(hull)) source.sendSuccess(() -> Component.literal(line), false);
        }
        if (hulls.size() > MAX_SHIPS) {
            int more = hulls.size() - MAX_SHIPS;
            source.sendSuccess(() -> Component.literal("...and " + more + " more further away."), false);
        }
        return hulls.size();
    }

    private static double distance(ShipHull hull, Vec3 from) {
        var p = hull.subLevel().logicalPose().position();
        return Math.sqrt(from.distanceToSqr(p.x(), p.y(), p.z()));
    }

    private static List<String> describe(ShipHull hull) {
        SubLevel subLevel = hull.subLevel();
        Pose3dc pose = subLevel.logicalPose();
        String name = subLevel.getName() != null ? subLevel.getName() : subLevel.getUniqueId().toString().substring(0, 8);
        List<ShipHull.CompartmentStatus> compartments = hull.status();
        List<String> lines = new ArrayList<>();
        lines.add(String.format(Locale.ROOT, "Ship %s at %.0f %.0f %.0f: %s", name, pose.position().x(), pose.position().y(),
                pose.position().z(), compartments.isEmpty() ? "open to the sea" : compartments.size() == 1 ? "1 compartment" : compartments.size() + " compartments"));
        int n = 1;
        for (ShipHull.CompartmentStatus c : compartments) {
            String leaks = c.leaks() == 0 ? "no leaks" : c.leaks() == 1 ? "1 leak" : c.leaks() + " leaks";
            String pumps = c.pumps() == 0 ? "" : c.pumps() == 1 ? ", 1 pump running" : ", " + c.pumps() + " pumps running";
            lines.add(String.format(Locale.ROOT, "  #%d: %.0f%% flooded (%.0f of %d), %s%s", n++, 100 * c.volume() / c.size(), c.volume(), c.size(), leaks, pumps));
        }
        return lines;
    }
}
