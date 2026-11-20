/**
 * Terrain generation shared by every cubic world: {@link io.github.pirut.chunks3.gen.SurfaceGenerator}
 * turns a ground and water height per column into cubes and far columns, and its helpers fill in what lies
 * under and on the ground: {@link io.github.pirut.chunks3.gen.Underground} (rock, caves, ores),
 * {@link io.github.pirut.chunks3.gen.Strata} and {@link io.github.pirut.chunks3.gen.Towers}
 * (rock layers and sheer hills), {@link io.github.pirut.chunks3.gen.Vegetation} and
 * {@link io.github.pirut.chunks3.gen.Trees} (plants and vanilla trees), and
 * {@link io.github.pirut.chunks3.gen.VanillaStructures} (vanilla's own structures, placed in cubes).
 *
 * <p>Generators run on many worker threads at once and must give the same blocks for the same seed and
 * place every time. Seed salts and hash functions here decide the terrain: changing one changes every
 * world. {@link io.github.pirut.chunks3.gen.HighlandsGenerator} is a small made-up world used
 * for engine tests; the Earth itself is in {@code cubic.earth}.
 */
package io.github.pirut.chunks3.gen;
