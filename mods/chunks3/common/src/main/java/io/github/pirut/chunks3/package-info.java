/**
 * The cubic world engine: a Minecraft level split into 16-block cubes from Y -16384 to 16383, so the
 * Earth can stand at true height. Vanilla chunks still exist but are empty 16-block "carriers"; the
 * blocks live in cubes.
 *
 * <ul>
 *   <li>{@link io.github.pirut.chunks3.CubeWorld} holds a level's cubes;
 *       {@link io.github.pirut.chunks3.ServerCubeWorld} makes, loads, saves and sends them
 *       around players, and {@link io.github.pirut.chunks3.CubeStorage} keeps edited cubes on disk.</li>
 *   <li>{@link io.github.pirut.chunks3.CubicEvents} wires the engine into the game's events;
 *       {@link io.github.pirut.chunks3.Cubic} registers the cubic dimensions and the generators their cubes come from.</li>
 *   <li>{@link io.github.pirut.chunks3.CubeLight}, {@link io.github.pirut.chunks3.CubicSpawns},
 *       {@link io.github.pirut.chunks3.CubicWeather} and the point-of-interest classes give vanilla
 *       rules their meaning at cubic heights.</li>
 * </ul>
 *
 * <p>Subpackages: {@code gen} and {@code earth} generate terrain, {@code far} and {@code client} make and
 * draw the far terrain and near cubes, {@code net} carries cubes to clients, {@code play} is everything a
 * player or server operator uses (commands, settings, screens), {@code mixin} holds the hooks into vanilla
 * and other mods (listed in {@code chunks3.mixins.json}), and {@code compat}, {@code portal}
 * and {@code jet} cover other mods, other dimensions and the jet stream.
 *
 * <p>Developer flags ({@code -Dchunks3.*}) are listed in {@code docs/FLAGS.md}.
 */
package io.github.pirut.chunks3;
