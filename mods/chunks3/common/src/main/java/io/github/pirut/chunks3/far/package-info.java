/**
 * The server side of far terrain: a voxel pyramid ({@link io.github.pirut.chunks3.far.Far}) whose
 * columns {@link io.github.pirut.chunks3.far.FarServer} makes on low-priority
 * {@link io.github.pirut.chunks3.far.FarWorkers} and sends to players, with what players built
 * shown through {@link io.github.pirut.chunks3.far.FarBuilds}. The client meshes and draws them in
 * {@code cubic.client} ({@code FarRenderer}, {@code FarMesher}, {@code FarGpu}).
 */
package io.github.pirut.chunks3.far;
