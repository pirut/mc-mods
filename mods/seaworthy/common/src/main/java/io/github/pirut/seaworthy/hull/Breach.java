package io.github.pirut.seaworthy.hull;

/**
 * An open face between a cell inside a compartment and a cell outside the hull: where water gets in (or out).
 *
 * @param inside    grid index of the compartment's cell
 * @param outside   grid index of the outside cell
 * @param direction direction from {@code inside} to {@code outside} (0 down ... 5 east)
 */
public record Breach(int inside, int outside, int direction) {

    /** Height of the middle of the hole, in grid y: the bottom of a floor hole, the top of a gunwale. */
    public double height(HullGrid grid) {
        int y = grid.y(inside);
        return switch (direction) {
            case 0 -> y;
            case 1 -> y + 1.0;
            default -> y + 0.5;
        };
    }
}
