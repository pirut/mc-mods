package io.github.pirut.seaworthy.sable;

import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.physics.force.QueuedForceGroup;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import io.github.pirut.seaworthy.SeaworthyConfig;
import io.github.pirut.seaworthy.hull.Lumps;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.joml.Vector3d;

/**
 * The lift of a hull's dry air, and the weight of floodwater that is above the sea.
 *
 * <p>Sable already floats every block of a ship by its own volume. What it can't see is the air inside the hull,
 * so a steel ship sinks like a pile of steel. Here every dry cell below the waterline pushes the ship up with the
 * weight of the water it keeps out, at the density Sable uses for blocks, so blocks and air add up to the
 * displacement of the whole hull. Floodwater below the waterline weighs nothing extra (the sea holds it up); only
 * water left above it, as a ship is lifted out, pulls down.
 */
public final class Buoyancy {
    /** Sable's water: 10.5 N of lift per cubic metre, at its gravity of 11 m/s^2. */
    private static final double SABLE_LIFT = 10.5;
    private static final double SABLE_GRAVITY = 11.0;

    private Buoyancy() {
    }

    /**
     * Lift and floodwater weight, gathered into one force each (they all point along gravity, so this is exact).
     *
     * @param lift   cubic metres of dry air under water
     * @param liftAt plot position the lift acts at
     * @param weight cubic metres of floodwater above the sea
     */
    record Sample(double lift, Vector3d liftAt, double weight, Vector3d weightAt) {
        static final Sample NONE = new Sample(0, new Vector3d(), 0, new Vector3d());
    }

    /** Called every game tick: how much of the hull's air is under water right now. */
    static Sample sample(SubLevel subLevel, BlockPos origin, Lumps lumps, WaterProbe probe, SeaworthyConfig config) {
        if (lumps.count == 0 || config.buoyancy() <= 0) return Sample.NONE;
        Pose3dc pose = subLevel.logicalPose();
        Vector3d p = new Vector3d();
        double lift = 0, lx = 0, ly = 0, lz = 0;
        double weight = 0, wx = 0, wy = 0, wz = 0;
        double h = lumps.halfHeight;
        for (int i = 0; i < lumps.count; i++) {
            if (lumps.air[i] > 0) {
                double px = origin.getX() + lumps.x[i], py = origin.getY() + lumps.y[i], pz = origin.getZ() + lumps.z[i];
                p = pose.transformPosition(p.set(px, py, pz));
                double under = probe.submerged(p.x, p.y - h, p.y + h, p.z);
                if (under > 0) {
                    double v = lumps.air[i] * under;
                    lift += v;
                    lx += px * v;
                    ly += py * v;
                    lz += pz * v;
                }
            }
            if (lumps.water[i] > 0) {
                double px = origin.getX() + lumps.wx[i], py = origin.getY() + lumps.wy[i], pz = origin.getZ() + lumps.wz[i];
                p = pose.transformPosition(p.set(px, py, pz));
                double above = 1.0 - probe.submerged(p.x, p.y - h, p.y + h, p.z);
                if (above > 0) {
                    double v = lumps.water[i] * above;
                    weight += v;
                    wx += px * v;
                    wy += py * v;
                    wz += pz * v;
                }
            }
        }
        if (lift <= 0 && weight <= 0) return Sample.NONE;
        return new Sample(lift * config.buoyancy(), lift > 0 ? new Vector3d(lx / lift, ly / lift, lz / lift) : new Vector3d(),
                weight, weight > 0 ? new Vector3d(wx / weight, wy / weight, wz / weight) : new Vector3d());
    }

    /** Sable's physics tick (several per game tick): push each ship by its latest sample. */
    public static void onPhysicsTick(SubLevelPhysicsSystem system, double timeStep) {
        ServerLevel level = system.getLevel();
        ShipTracker tracker = ShipTracker.of(level);
        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (tracker == null || container == null) return;
        Vector3d gravity = new Vector3d();
        Vector3d impulse = new Vector3d();
        for (ServerSubLevel subLevel : container.getAllSubLevels()) {
            if (subLevel.isRemoved()) continue;
            ShipHull hull = tracker.hull(subLevel.getUniqueId());
            if (hull == null || hull.subLevel() != subLevel) continue;
            Sample s = hull.sample();
            if (s.lift() <= 0 && s.weight() <= 0) continue;
            if (subLevel.getMassTracker() == null || subLevel.getMassTracker().getCenterOfMass() == null) continue;

            Pose3dc pose = subLevel.logicalPose();
            DimensionPhysicsData.getGravity(level, pose.position(), gravity);
            double g = gravity.length();
            if (g < 1e-6) continue;
            pose.orientation().transformInverse(gravity); // into the ship's frame
            QueuedForceGroup group = subLevel.getOrCreateQueuedForceGroup(ForceGroups.LEVITATION.get());

            if (s.lift() > 0) {
                // Upwards, against gravity, as hard as Sable floats a block of the same volume.
                double force = s.lift() * SABLE_LIFT * (g / SABLE_GRAVITY);
                impulse.set(gravity).mul(-force * timeStep / g);
                group.applyAndRecordPointForce(s.liftAt(), impulse);
            }
            if (s.weight() > 0) {
                double mass = s.weight() * SABLE_LIFT / SABLE_GRAVITY;
                impulse.set(gravity).mul(mass * timeStep);
                group.applyAndRecordPointForce(s.weightAt(), impulse);
            }
        }
    }
}
