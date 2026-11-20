package io.github.pirut.chunks3;

/**
 * Point-of-interest storage (beds, job sites, bells, portals) at cubic heights. Vanilla's store
 * only spans the carrier world's height; in the cubic world sections outside it are kept in
 * memory, rebuilt from a cube's blocks whenever the cube loads, and forgotten when it unloads.
 */
public interface CubicPoiStore {
    /** The sections held outside the carrier's height in a chunk column. */
    java.util.stream.Stream<?> chunks3$cubicSections(long chunk);
    /** Drops a section's points of interest once its cube unloads. */
    void chunks3$forget(long section);
}
