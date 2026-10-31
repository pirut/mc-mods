package io.github.pirut.seaworthy.hull;

/**
 * A snapshot of a ship's blocks as far as water is concerned: one cell per block, with a one-cell border of open
 * air around the ship so the outside of the grid is always open water.
 *
 * <p>No Minecraft classes here, so the hull logic runs and is tested on plain Java (and off the game thread).
 *
 * <p>Directions use Minecraft's {@code Direction} order: 0 down, 1 up, 2 north (-z), 3 south (+z), 4 west (-x),
 * 5 east (+x). The opposite of {@code d} is {@code d ^ 1}.
 */
public final class HullGrid {
    /** Bits 0-5: the block's face in that direction is full, so water can't pass through it. */
    public static final int FACE_MASK = 0x3F;
    /** The block fills the whole cell: water can never be in it. */
    public static final int SOLID = 1 << 6;
    /** The ship's own fluid (a pool, a tank of water as a block). Never counted as dry air. */
    public static final int FLUID = 1 << 7;

    public static final int[] DX = {0, 0, 0, 0, -1, 1};
    public static final int[] DY = {-1, 1, 0, 0, 0, 0};
    public static final int[] DZ = {0, 0, -1, 1, 0, 0};

    public final int sx, sy, sz;
    private final byte[] cells;
    /** How much of each cell is air that displaces water, 0..1 (a slab inside the hull leaves half). */
    private final float[] air;

    public HullGrid(int sx, int sy, int sz) {
        if (sx < 3 || sy < 3 || sz < 3) throw new IllegalArgumentException("grid needs a border: " + sx + "x" + sy + "x" + sz);
        this.sx = sx;
        this.sy = sy;
        this.sz = sz;
        this.cells = new byte[sx * sy * sz];
        this.air = new float[sx * sy * sz];
        java.util.Arrays.fill(this.air, 1.0f);
    }

    public int volume() {
        return cells.length;
    }

    public int index(int x, int y, int z) {
        return (y * sz + z) * sx + x;
    }

    public int x(int i) {
        return i % sx;
    }

    public int y(int i) {
        return i / (sx * sz);
    }

    public int z(int i) {
        return (i / sx) % sz;
    }

    public boolean inBounds(int x, int y, int z) {
        return x >= 0 && y >= 0 && z >= 0 && x < sx && y < sy && z < sz;
    }

    public boolean onBorder(int i) {
        int x = x(i), y = y(i), z = z(i);
        return x == 0 || y == 0 || z == 0 || x == sx - 1 || y == sy - 1 || z == sz - 1;
    }

    /** The neighbour of cell {@code i} in direction {@code d}, or -1 off the grid. */
    public int neighbour(int i, int d) {
        int x = x(i) + DX[d], y = y(i) + DY[d], z = z(i) + DZ[d];
        return inBounds(x, y, z) ? index(x, y, z) : -1;
    }

    public void set(int x, int y, int z, int flags, float airFraction) {
        int i = index(x, y, z);
        cells[i] = (byte) flags;
        air[i] = airFraction;
    }

    public void set(int x, int y, int z, int flags) {
        set(x, y, z, flags, (flags & (SOLID | FLUID)) != 0 ? 0.0f : 1.0f);
    }

    public int flags(int i) {
        return cells[i] & 0xFF;
    }

    public float air(int i) {
        return air[i];
    }

    public boolean solid(int i) {
        return (cells[i] & SOLID) != 0;
    }

    /** Whether water can move from cell {@code a} to its neighbour {@code b} in direction {@code d}. */
    public boolean open(int a, int b, int d) {
        int fa = cells[a] & 0xFF, fb = cells[b] & 0xFF;
        if (((fa | fb) & SOLID) != 0) return false;
        return (fa & (1 << d)) == 0 && (fb & (1 << (d ^ 1))) == 0;
    }

    /** Whether the cell holds something a hole can be measured against: a full block or a block with a full face. */
    boolean wall(int i) {
        int f = cells[i] & 0xFF;
        return (f & SOLID) != 0 || (f & FACE_MASK) != 0;
    }
}
