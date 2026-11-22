package io.earthplatform.minecraft.cubic.gen;

public interface CubeGenerator {
    int surfaceY(int x, int z);

    default int waterY(int x, int z) {
        return Integer.MIN_VALUE;
    }

    default int skyTopY(int x, int z) {
        return surfaceY(x, z);
    }

    default int[] arrival() {
        return new int[]{0, 0};
    }
}
