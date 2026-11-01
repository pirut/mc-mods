package io.github.pirut.chunks3.client;

import com.mojang.blaze3d.platform.GlStateManager;
//#if MC >= 1.21.11
//$$ import com.mojang.blaze3d.buffers.GpuBuffer;
//$$ import com.mojang.blaze3d.buffers.GpuBufferSlice;
//$$ import com.mojang.blaze3d.pipeline.RenderTarget;
//$$ import com.mojang.blaze3d.systems.CommandEncoder;
//$$ import com.mojang.blaze3d.systems.RenderPass;
//$$ import com.mojang.blaze3d.textures.FilterMode;
//$$ import com.mojang.blaze3d.textures.GpuTextureView;
//$$ import java.util.OptionalDouble;
//$$ import java.util.OptionalInt;
//$$ import net.minecraft.client.Minecraft;
//$$ import net.minecraft.client.renderer.DynamicUniforms;
//$$ import net.minecraft.client.renderer.RenderPipelines;
//$$ import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
//$$ import net.minecraft.client.renderer.texture.TextureAtlas;
//$$ import org.joml.Matrix4f;
//#endif
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL31C;
import org.lwjgl.opengl.GL32C;

/**
 * Far water's meshes, every column's surface and walls, in one vertex buffer per vertex format,
 * which a pass binds once and then draws column after column from.
 *
 * <p>Each column's water had a vertex array, vertex buffer and index buffer of its own (a
 * {@link CubeBuffer}), and each of far water's passes bound them before every column's draw: over
 * lakes and rivers thousands of binds and draws a frame, about 5 ms of the render thread under
 * Photon. A mesh here is a run of quads in a shared buffer, drawn with a base vertex into the shared
 * quad indices: the same corners in the same triangles, in the same order and with the same
 * uniforms as before. {@code -Dchunks3.far.sharedWater=false} gives each mesh a buffer of its
 * own again, bound before each of its draws. Everything here runs on the render thread.
 */
final class FarWaterBuffers {
    static final boolean SHARED = !"false".equals(System.getProperty("chunks3.far.sharedWater"));
    /** A shared buffer's room at first, in quads; it doubles as it fills. */
    private static final int FIRST_QUADS = 1 << 14;

    /** One surface or wall: a run of quads in a buffer. */
    static final class Mesh {
        /** Null, with no range, for a mesh with no quads. */
        @Nullable private final Buffer buffer;
        @Nullable private final FarArenaRanges.Allocation range;
        private final int indexCount, baseVertex;

        private Mesh(@Nullable Buffer buffer, @Nullable FarArenaRanges.Allocation range, int indexCount) {
            this.buffer = buffer;
            this.range = range;
            this.indexCount = indexCount;
            baseVertex = range == null ? 0 : range.start() * 4;
        }
    }

    /** Vertex buffers by format (two only while a shader pack comes or goes), each with its vertex array. */
    private final List<Buffer> buffers = new ArrayList<>();
    /** The format of the last mesh put in; an emptied buffer of another format is let go. */
    @Nullable private VertexFormat latest;
    /** The buffer whose array the pass under way has bound, or null before its first draw. */
    @Nullable private Buffer bound;

    /** Puts a built mesh in a buffer of its format, letting go of the one it replaces; null for none. */
    @Nullable Mesh upload(@Nullable Mesh old, @Nullable FarMesher.Part part) {
        free(old);
        if (part == null) return null;
        try {
            return add(part.mesh());
        } finally {
            part.memory().close();
        }
    }

    private Mesh add(MeshData mesh) {
        try (mesh) {
            RenderSystem.assertOnRenderThread();
            MeshData.DrawState state = mesh.drawState();
            if (state.mode() != VertexFormat.Mode.QUADS) throw new IllegalArgumentException("Far water holds quads, not " + state.mode());
            // Drawn with the shared quad indices, in the order built, as CubeBuffer draws a mesh with no sorted indices of its own.
            if (mesh.indexBuffer() != null) throw new IllegalArgumentException("Far water's quads are not sorted");
            VertexFormat format = state.format();
            if (!format.equals(latest)) {
                latest = format;
                // A shader pack came or went: buffers of the old format are let go as they empty.
                for (Buffer buffer : List.copyOf(buffers)) letGoIfEmpty(buffer);
            }
            int quads = state.vertexCount() / 4;
            ByteBuffer vertices = mesh.vertexBuffer();
            // A builder given no quads gives no mesh, so not met in practice; drawn as nothing, as CubeBuffer drew it.
            if (quads == 0 || vertices == null) return new Mesh(null, null, 0);
            Buffer buffer = null;
            if (SHARED) for (Buffer candidate : buffers) if (candidate.format.equals(format)) { buffer = candidate; break; }
            if (buffer == null) {
                buffer = new Buffer(format, SHARED ? Math.max(FIRST_QUADS, quads) : quads);
                buffers.add(buffer);
            }
            return buffer.add(vertices, quads, state.indexCount());
        }
    }

    /** Lets go of a mesh's quads. */
    void free(@Nullable Mesh mesh) {
        if (mesh == null || mesh.buffer == null) return;
        mesh.buffer.ranges.release(mesh.range);
        letGoIfEmpty(mesh.buffer);
    }

    /** Deletes a buffer holding nothing, unless it is the shared one meshes of the latest format go to. */
    private void letGoIfEmpty(Buffer buffer) {
        if (buffer.ranges.allocationCount() > 0 || SHARED && buffer.format.equals(latest)) return;
        buffers.remove(buffer);
        if (bound == buffer) bound = null;
        buffer.close();
    }

    /** Starts a pass: its first draw binds its buffer's array, whatever was bound before. */
    //#if MC >= 1.21.11
    //$$ // From 1.21.5 water is drawn in a render pass of the game's GPU device with its translucent terrain pipeline, each
    //$$ // mesh with its own section uniforms: its place, and the view it is drawn with (see FarRenderer#drawWaterColumns).
    //$$ private final List<Mesh> queued = new ArrayList<>();
    //$$ private final List<DynamicUniforms.ChunkSectionInfo> places = new ArrayList<>();

    //$$ void begin() { queued.clear(); places.clear(); }

    //$$ void draw(Mesh mesh, int x, int y, int z, Matrix4f modelView) {
        //$$ if (mesh.indexCount == 0 || mesh.buffer == null) return;
        //$$ GpuTextureView atlas = Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS).getTextureView();
        //$$ queued.add(mesh);
        //$$ places.add(new DynamicUniforms.ChunkSectionInfo(new Matrix4f(modelView), x, y, z, 1.0F, atlas.getWidth(0), atlas.getHeight(0)));
    //$$ }

    //$$ /** Only shader packs' own water draws took the place from GL state; they are not drawn with from 1.21.5 yet. */
    //$$ void draw(Mesh mesh) { throw new IllegalStateException("Far water drawn without its place"); }

    //$$ void end() {
        //$$ if (queued.isEmpty()) return;
        //$$ Minecraft minecraft = Minecraft.getInstance();
        //$$ int indices = 0;
        //$$ for (Mesh mesh : queued) indices = Math.max(indices, mesh.indexCount);
        //$$ GpuBufferSlice[] slices = RenderSystem.getDynamicUniforms().writeChunkSections(places.toArray(new DynamicUniforms.ChunkSectionInfo[0]));
        //$$ RenderSystem.AutoStorageIndexBuffer quads = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        //$$ GpuBuffer quadIndices = quads.getBuffer(indices);
        //$$ RenderTarget target = ChunkSectionLayerGroup.TRANSLUCENT.outputTarget();
        //$$ GpuTextureView atlas = minecraft.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS).getTextureView();
        //$$ try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Chunks3 far water",
                //$$ target.getColorTextureView(), OptionalInt.empty(), target.getDepthTextureView(), OptionalDouble.empty())) {
            //$$ RenderSystem.bindDefaultUniforms(pass);
            //$$ pass.bindTexture("Sampler2", minecraft.gameRenderer.lightTexture().getTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            //$$ pass.setPipeline(RenderPipelines.TRANSLUCENT_TERRAIN);
            //$$ pass.bindTexture("Sampler0", atlas, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
            //$$ pass.setIndexBuffer(quadIndices, quads.type());
            //$$ for (int i = 0; i < queued.size(); i++) {
                //$$ Mesh mesh = queued.get(i);
                //$$ pass.setUniform("ChunkSection", slices[i]);
                //$$ pass.setVertexBuffer(0, mesh.buffer.vertices);
                //$$ pass.drawIndexed(mesh.baseVertex, 0, mesh.indexCount, 1);
            //$$ }
        //$$ }
        //$$ queued.clear();
        //$$ places.clear();
    //$$ }
    //#else
    void begin() { bound = null; }

    /** Draws a mesh with the program in use, binding its buffer's array if the pass's last draw used another. */
    void draw(Mesh mesh) {
        if (mesh.indexCount == 0) return;
        if (mesh.buffer != bound) {
            bound = mesh.buffer;
            GlStateManager._glBindVertexArray(bound.array);
            // Bound again into the array before drawing, as CubeBuffer does, so nothing outside changes the indices used.
            GlStateManager._glBindBuffer(GL15C.GL_ELEMENT_ARRAY_BUFFER, CubeBuffer.QuadIndices.name);
        }
        GL32C.glDrawElementsBaseVertex(GL11C.GL_TRIANGLES, mesh.indexCount, GL11C.GL_UNSIGNED_INT, 0L, mesh.baseVertex);
    }

    /** Ends a pass, leaving no vertex array bound. */
    void end() {
        bound = null;
        CubeBuffer.unbind();
    }
    //#endif

    void close() {
        for (Buffer buffer : buffers) buffer.close();
        buffers.clear();
        bound = null;
        latest = null;
    }

    /** One vertex buffer of meshes of one format, and the vertex array reading it with the shared quad indices. */
    private static final class Buffer {
        final VertexFormat format;
        final FarArenaRanges ranges;
        //#if MC >= 1.21.11
        //$$ private final int stride;
        //$$ GpuBuffer vertices;
        //$$ private int quads;

        //$$ Buffer(VertexFormat format, int quads) {
            //$$ this.format = format;
            //$$ this.quads = quads;
            //$$ stride = format.getVertexSize();
            //$$ ranges = new FarArenaRanges(quads);
            //$$ vertices = create(quads);
        //$$ }

        //$$ private GpuBuffer create(int quads) {
            //$$ return RenderSystem.getDevice().createBuffer(() -> "Chunks3 far water", GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_COPY_SRC, bytes(quads));
        //$$ }

        //$$ private long bytes(int quads) { return (long) quads * 4 * stride; }

        //$$ Mesh add(ByteBuffer data, int quads, int indexCount) {
            //$$ FarArenaRanges.Allocation range = ranges.allocate(quads);
            //$$ if (range == null) {
                //$$ grow(quads);
                //$$ range = ranges.allocate(quads);
            //$$ }
            //$$ RenderSystem.getDevice().createCommandEncoder().writeToBuffer(vertices.slice(bytes(range.start()), data.remaining()), data);
            //$$ return new Mesh(this, range, indexCount);
        //$$ }

        //$$ private void grow(int quadsWanted) {
            //$$ int grown = Math.max(quads * 2, quads + quadsWanted);
            //$$ GpuBuffer bigger = create(grown);
            //$$ CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
            //$$ encoder.copyToBuffer(vertices.slice(0, bytes(quads)), bigger.slice(0, bytes(quads)));
            //$$ vertices.close();
            //$$ vertices = bigger;
            //$$ ranges.grow(grown);
            //$$ quads = grown;
        //$$ }

        //$$ void close() {
            //$$ if (vertices == null) return;
            //$$ vertices.close();
            //$$ vertices = null;
        //$$ }
        //#else
        private final int stride;
        private int vertices, array, quads;

        Buffer(VertexFormat format, int quads) {
            this.format = format;
            this.quads = quads;
            stride = format.getVertexSize();
            ranges = new FarArenaRanges(quads);
            vertices = GL15C.glGenBuffers();
            GL15C.glBindBuffer(GL31C.GL_COPY_WRITE_BUFFER, vertices);
            GL15C.glBufferData(GL31C.GL_COPY_WRITE_BUFFER, bytes(quads), GL15C.GL_DYNAMIC_DRAW);
            GL15C.glBindBuffer(GL31C.GL_COPY_WRITE_BUFFER, 0);
            array = GlStateManager._glGenVertexArrays();
            point();
        }

        private long bytes(int quads) { return (long) quads * 4 * stride; }

        /** Points the array's attributes at the vertex buffer, as CubeBuffer sets up its own. */
        private void point() {
            Gl.invalidate();
            GlStateManager._glBindVertexArray(array);
            GlStateManager._glBindBuffer(GL15C.GL_ARRAY_BUFFER, vertices);
            format.setupBufferState();
            GlStateManager._glBindVertexArray(0);
            Gl.invalidate();
        }

        Mesh add(ByteBuffer data, int quads, int indexCount) {
            FarArenaRanges.Allocation range = ranges.allocate(quads);
            if (range == null) {
                grow(quads);
                range = ranges.allocate(quads);
            }
            GL15C.glBindBuffer(GL31C.GL_COPY_WRITE_BUFFER, vertices);
            GL15C.glBufferSubData(GL31C.GL_COPY_WRITE_BUFFER, bytes(range.start()), data);
            GL15C.glBindBuffer(GL31C.GL_COPY_WRITE_BUFFER, 0);
            // The shared quad indices reach far enough for this mesh, bound into this array alone.
            GlStateManager._glBindVertexArray(array);
            CubeBuffer.QuadIndices.ensure(indexCount);
            GlStateManager._glBindVertexArray(0);
            Gl.invalidate();
            return new Mesh(this, range, indexCount);
        }

        /** Doubles the room, or more for a mesh larger than that, keeping every mesh where it is. */
        private void grow(int quadsWanted) {
            int grown = Math.max(quads * 2, quads + quadsWanted);
            int bigger = GL15C.glGenBuffers();
            GL15C.glBindBuffer(GL31C.GL_COPY_WRITE_BUFFER, bigger);
            GL15C.glBufferData(GL31C.GL_COPY_WRITE_BUFFER, bytes(grown), GL15C.GL_DYNAMIC_DRAW);
            GL15C.glBindBuffer(GL31C.GL_COPY_READ_BUFFER, vertices);
            GL31C.glCopyBufferSubData(GL31C.GL_COPY_READ_BUFFER, GL31C.GL_COPY_WRITE_BUFFER, 0, 0, bytes(quads));
            GL15C.glBindBuffer(GL31C.GL_COPY_READ_BUFFER, 0);
            GL15C.glBindBuffer(GL31C.GL_COPY_WRITE_BUFFER, 0);
            GL15C.glDeleteBuffers(vertices);
            vertices = bigger;
            ranges.grow(grown);
            quads = grown;
            point();
        }

        void close() {
            if (array < 0) return;
            GlStateManager._glDeleteBuffers(vertices);
            org.lwjgl.opengl.GL30C.glDeleteVertexArrays(array);
            array = vertices = -1;
        }
        //#endif
    }
}
