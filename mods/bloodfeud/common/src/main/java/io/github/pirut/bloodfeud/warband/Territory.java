package io.github.pirut.bloodfeud.warband;

/** The land a Warlord holds. Captains that serve under it roam here. */
public record Territory(int index, String dimension, int x, int z, int radius) {
    public boolean contains(String dim, double px, double pz) {
        if (!dimension.equals(dim)) return false;
        double dx = px - x, dz = pz - z;
        return dx * dx + dz * dz <= (double) radius * radius;
    }

    public double distanceSq(double px, double pz) {
        double dx = px - x, dz = pz - z;
        return dx * dx + dz * dz;
    }
}
