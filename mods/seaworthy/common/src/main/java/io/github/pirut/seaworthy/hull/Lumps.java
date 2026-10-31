package io.github.pirut.seaworthy.hull;

/**
 * The hull's air and floodwater, gathered into cubes of {@code size} blocks so buoyancy costs the same for a
 * dinghy and a battleship. Each lump has the centre and amount of its dry air (which pushes the ship up when it is
 * under water) and of its floodwater (which only weighs the ship down when it is above the water outside).
 *
 * <p>Positions are cell centres in grid coordinates.
 */
public final class Lumps {
    public final int count;
    public final float[] x, y, z;
    public final float[] air;
    public final float[] wx, wy, wz;
    public final float[] water;
    /** Half the height of a lump, in blocks: how far above and below its centre it reaches. */
    public final float halfHeight;

    private Lumps(int count, float halfHeight) {
        this.count = count;
        this.halfHeight = halfHeight;
        x = new float[count];
        y = new float[count];
        z = new float[count];
        air = new float[count];
        wx = new float[count];
        wy = new float[count];
        wz = new float[count];
        water = new float[count];
    }

    public static final Lumps NONE = new Lumps(0, 0.5f);

    /** A lump size that keeps a hull with {@code interiorCells} cells under {@code maxLumps} lumps. */
    public static int sizeFor(int interiorCells, int maxLumps) {
        int size = 1;
        while (size < 16 && (long) interiorCells / ((long) size * size * size) > maxLumps) size *= 2;
        return size;
    }

    /**
     * @param wet which cells are flooded; the rest of each compartment is dry air
     */
    public static Lumps build(HullAnalysis.Result result, boolean[] wet, int size) {
        HullGrid g = result.grid();
        int lx = (g.sx + size - 1) / size, ly = (g.sy + size - 1) / size, lz = (g.sz + size - 1) / size;
        int cells = lx * ly * lz;
        double[] ax = new double[cells], ay = new double[cells], az = new double[cells], av = new double[cells];
        double[] bx = new double[cells], by = new double[cells], bz = new double[cells], bv = new double[cells];
        for (Compartment c : result.compartments()) {
            for (int i : c.cells()) {
                int x = g.x(i), y = g.y(i), z = g.z(i);
                int l = ((y / size) * lz + z / size) * lx + x / size;
                if (wet[i]) {
                    bx[l] += x + 0.5;
                    by[l] += y + 0.5;
                    bz[l] += z + 0.5;
                    bv[l] += 1;
                } else {
                    double a = g.air(i);
                    if (a <= 0) continue;
                    ax[l] += (x + 0.5) * a;
                    ay[l] += (y + 0.5) * a;
                    az[l] += (z + 0.5) * a;
                    av[l] += a;
                }
            }
        }
        int used = 0;
        for (int l = 0; l < cells; l++) if (av[l] > 0 || bv[l] > 0) used++;
        Lumps out = new Lumps(used, size * 0.5f);
        int k = 0;
        for (int l = 0; l < cells; l++) {
            if (av[l] <= 0 && bv[l] <= 0) continue;
            if (av[l] > 0) {
                out.x[k] = (float) (ax[l] / av[l]);
                out.y[k] = (float) (ay[l] / av[l]);
                out.z[k] = (float) (az[l] / av[l]);
                out.air[k] = (float) av[l];
            }
            if (bv[l] > 0) {
                out.wx[k] = (float) (bx[l] / bv[l]);
                out.wy[k] = (float) (by[l] / bv[l]);
                out.wz[k] = (float) (bz[l] / bv[l]);
                out.water[k] = (float) bv[l];
            }
            k++;
        }
        return out;
    }
}
