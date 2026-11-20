package io.github.pirut.seaworthy.hull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Works out which parts of a ship are inside its hull, how they split into compartments, and where the holes are.
 *
 * <p>The outside is everything water reaches from the edge of the grid moving sideways or up, never down. So the
 * hold of an open-topped boat counts as inside as long as nothing lets water in below its rim: water would have
 * to climb over the gunwale and fall in. The gunwale then shows up as a breach at the rim's height, and the hold
 * only floods if the water outside rises above it.
 *
 * <p>Holes up to {@code maxHoleWidth} blocks across are patched for that test, so a cannonball hole or a broken
 * plank doesn't turn the whole hold into the open sea. Instead the hole becomes a {@link Breach}, and the
 * compartment behind it fills over time. A gap wider than that is open water, as it would be in real life.
 */
public final class HullAnalysis {
    private static final int UNSET = Integer.MAX_VALUE;

    private HullAnalysis() {
    }

    public record Result(HullGrid grid, boolean[] outside, int[] compartmentOf, List<Compartment> compartments) {
        public boolean inside(int i) {
            return compartmentOf[i] >= 0;
        }

        public int interiorCells() {
            int n = 0;
            for (Compartment c : compartments) n += c.size();
            return n;
        }
    }

    public static Result analyze(HullGrid grid, int maxHoleWidth) {
        int n = grid.volume();
        byte[] hole = findHoles(grid, maxHoleWidth);

        // 1. The open sea: everything reachable from the border sideways or up, through unpatched open cells.
        boolean[] outside = new boolean[n];
        int[] queue = new int[n];
        int head = 0, tail = 0;
        for (int i = 0; i < n; i++) {
            if (grid.onBorder(i) && !grid.solid(i) && hole[i] < 0) {
                outside[i] = true;
                queue[tail++] = i;
            }
        }
        while (head < tail) {
            int c = queue[head++];
            for (int d = 1; d < 6; d++) { // never down
                int nb = grid.neighbour(c, d);
                if (nb < 0 || outside[nb] || hole[nb] >= 0 || !grid.open(c, nb, d)) continue;
                outside[nb] = true;
                queue[tail++] = nb;
            }
        }

        // 2. Holes that touch the sea are part of the sea too, but the space behind a hole is not: water only gets
        //    into a hole cell by passing straight through it, along its normal.
        head = 0;
        tail = 0;
        for (int i = 0; i < n; i++) if (outside[i]) queue[tail++] = i;
        while (head < tail) {
            int c = queue[head++];
            for (int d = 1; d < 6; d++) {
                int nb = grid.neighbour(c, d);
                if (nb < 0 || outside[nb] || hole[nb] != axis(d) || !grid.open(c, nb, d)) continue;
                outside[nb] = true;
                queue[tail++] = nb;
            }
        }

        // 3. Compartments: connected open spaces inside.
        int[] compartmentOf = new int[n];
        Arrays.fill(compartmentOf, -1);
        List<int[]> members = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (grid.solid(i) || outside[i] || compartmentOf[i] >= 0) continue;
            int id = members.size();
            head = 0;
            tail = 0;
            compartmentOf[i] = id;
            queue[tail++] = i;
            while (head < tail) {
                int c = queue[head++];
                for (int d = 0; d < 6; d++) {
                    int nb = grid.neighbour(c, d);
                    if (nb < 0 || compartmentOf[nb] >= 0 || outside[nb] || !grid.open(c, nb, d)) continue;
                    compartmentOf[nb] = id;
                    queue[tail++] = nb;
                }
            }
            members.add(Arrays.copyOf(queue, tail));
        }

        // 4. Breaches, then each compartment's fill order.
        int[] key = new int[n];
        Arrays.fill(key, UNSET);
        List<Compartment> compartments = new ArrayList<>(members.size());
        for (int id = 0; id < members.size(); id++) {
            int[] cells = members.get(id);
            List<Breach> breaches = new ArrayList<>();
            for (int c : cells) {
                for (int d = 0; d < 6; d++) {
                    int nb = grid.neighbour(c, d);
                    if (nb >= 0 && outside[nb] && grid.open(c, nb, d)) breaches.add(new Breach(c, nb, d));
                }
            }
            compartments.add(fillOrder(grid, cells, breaches, compartmentOf, id, key));
        }

        return new Result(grid, outside, compartmentOf, List.copyOf(compartments));
    }

    /**
     * Holes: open cells hemmed in by walls no more than {@code width} cells apart along at least two axes, like a
     * missing block in a wall (walls left and right, above and below). The third axis, the one water would pass
     * along to get through, is the hole's normal: {@code 0} x, {@code 1} y, {@code 2} z, or {@code -1} for cells
     * that aren't holes. A long gap open at both ends (between two hulls, or a corridor seen end on) is only
     * hemmed in along one axis, so it isn't a hole.
     */
    static byte[] findHoles(HullGrid grid, int width) {
        int n = grid.volume();
        byte[] normal = new byte[n];
        java.util.Arrays.fill(normal, (byte) -1);
        if (width <= 0) return normal;
        int[][] gap = new int[3][n];
        for (int[] g : gap) java.util.Arrays.fill(g, Integer.MAX_VALUE);
        for (int y = 0; y < grid.sy; y++)
            for (int z = 0; z < grid.sz; z++) gapLine(grid, gap[0], grid.sx, i -> grid.index(i, 0, 0), grid.index(0, y, z));
        for (int x = 0; x < grid.sx; x++)
            for (int z = 0; z < grid.sz; z++) gapLine(grid, gap[1], grid.sy, i -> grid.index(0, i, 0), grid.index(x, 0, z));
        for (int y = 0; y < grid.sy; y++)
            for (int x = 0; x < grid.sx; x++) gapLine(grid, gap[2], grid.sz, i -> grid.index(0, 0, i), grid.index(x, y, 0));
        for (int i = 0; i < n; i++) {
            if (grid.solid(i)) continue;
            int bounded = 0, widest = 0;
            for (int a = 0; a < 3; a++) {
                if (gap[a][i] <= width) bounded++;
                if (gap[a][i] > gap[widest][i]) widest = a;
            }
            if (bounded >= 2) normal[i] = (byte) widest;
        }
        return normal;
    }

    /** Fills {@code gap} along one line with the length of the run of open cells each cell is in, if walled at both ends. */
    private static void gapLine(HullGrid grid, int[] gap, int length, java.util.function.IntUnaryOperator offset, int base) {
        int last = -1; // position of the last wall on this line
        for (int p = 0; p <= length; p++) {
            boolean wall = p < length && grid.wall(base + offset.applyAsInt(p));
            if (p < length && !wall) continue;
            int run = p - last - 1;
            if (last >= 0 && p < length && run > 0) {
                for (int q = last + 1; q < p; q++) gap[base + offset.applyAsInt(q)] = run;
            }
            last = p;
        }
    }

    static int axis(int direction) {
        return direction < 2 ? 1 : direction < 4 ? 2 : 0;
    }

    /**
     * Orders a compartment's cells the way water fills it. Each cell's key is the highest cell on the lowest path
     * from a breach to it (a priority flood); cells then fill by key, and by height within a key.
     */
    private static Compartment fillOrder(HullGrid grid, int[] cells, List<Breach> breaches, int[] compartmentOf,
                                         int id, int[] key) {
        int anchor = Integer.MAX_VALUE;
        for (int c : cells) anchor = Math.min(anchor, c);

        if (breaches.isEmpty()) {
            // Sealed: water never gets in, so the order only matters for counting. Bottom up.
            int[] order = sortByKeyThenY(grid, cells, c -> grid.y(c), c -> grid.y(c));
            return group(grid, anchor, order, breaches);
        }

        // Bucket queue on key (grid heights are small).
        int levels = grid.sy + 1;
        IntList[] buckets = new IntList[levels];
        for (Breach b : breaches) {
            int c = b.inside();
            int k = grid.y(c);
            if (k < key[c]) {
                key[c] = k;
                bucket(buckets, k).add(c);
            }
        }
        for (int level = 0; level < levels; level++) {
            IntList bucket = buckets[level];
            if (bucket == null) continue;
            for (int bi = 0; bi < bucket.size; bi++) { // grows while we walk it
                int c = bucket.data[bi];
                if (key[c] != level) continue; // stale entry
                for (int d = 0; d < 6; d++) {
                    int nb = grid.neighbour(c, d);
                    if (nb < 0 || compartmentOf[nb] != id || !grid.open(c, nb, d)) continue;
                    int k = Math.max(level, grid.y(nb));
                    if (k < key[nb]) {
                        key[nb] = k;
                        bucket(buckets, k).add(nb);
                    }
                }
            }
        }
        int[] order = sortByKeyThenY(grid, cells, c -> key[c] == UNSET ? grid.sy : key[c], grid::y);
        return group(grid, anchor, order, breaches);
    }

    private static int[] sortByKeyThenY(HullGrid grid, int[] cells, java.util.function.IntUnaryOperator keyOf,
                                        java.util.function.IntUnaryOperator yOf) {
        int levels = grid.sy + 1;
        // Counting sort on (key, y): key and y are both 0..sy.
        int[] count = new int[levels * levels + 1];
        for (int c : cells) count[keyOf.applyAsInt(c) * levels + yOf.applyAsInt(c) + 1]++;
        for (int i = 1; i < count.length; i++) count[i] += count[i - 1];
        int[] order = new int[cells.length];
        int[] cells2 = cells.clone();
        Arrays.sort(cells2); // deterministic order within a run, so server and client agree
        for (int c : cells2) order[count[keyOf.applyAsInt(c) * levels + yOf.applyAsInt(c)]++] = c;
        return order;
    }

    private static Compartment group(HullGrid grid, int anchor, int[] order, List<Breach> breaches) {
        IntList starts = new IntList();
        IntList ys = new IntList();
        int lastY = Integer.MIN_VALUE;
        for (int i = 0; i < order.length; i++) {
            int y = grid.y(order[i]);
            // A new run whenever the height changes. Within a key heights only go up, so two runs at the same
            // height always have a different height between them.
            if (y != lastY) {
                starts.add(i);
                ys.add(y);
                lastY = y;
            }
        }
        return new Compartment(anchor, order, starts.toArray(), ys.toArray(), List.copyOf(breaches));
    }

    private static IntList bucket(IntList[] buckets, int k) {
        IntList b = buckets[k];
        if (b == null) buckets[k] = b = new IntList();
        return b;
    }

    static final class IntList {
        int[] data = new int[16];
        int size;

        void add(int v) {
            if (size == data.length) data = Arrays.copyOf(data, size * 2);
            data[size++] = v;
        }

        int[] toArray() {
            return Arrays.copyOf(data, size);
        }
    }
}
