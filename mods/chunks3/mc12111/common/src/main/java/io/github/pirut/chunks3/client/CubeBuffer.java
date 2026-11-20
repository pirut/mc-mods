package io.github.pirut.chunks3.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.nio.ByteBuffer;
//#if MC >= 1.21.11
//$$ import com.mojang.blaze3d.buffers.GpuBuffer;
//$$ import com.mojang.blaze3d.systems.CommandEncoder;
//#else
//#endif
import java.nio.IntBuffer;
import javax.annotation.Nullable;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.system.MemoryUtil;

/**
 * One layer of a near cube's mesh on the GPU, with its own vertex array, vertex buffer and index
 * buffer.
 *
 * <p>Vanilla's VertexBuffer leaves the quad indices to the game's shared sequential index buffer
 * and binds it into its vertex array only once, at upload. A later bind of another element buffer
 * while that array is current (a shader pack restoring "the last vertex buffer", or any code that
 * binds indices without its own array) silently replaced them, and the next uploads never put
 * them back. Under a shader pack on the Mac that drew every block face as one triangle of two and
 * stretched slanted sheets across the cube. Here the quad indices are our own and the index
 * buffer is bound again before every draw, so nothing outside can change which ones are used.
 */
final class CubeBuffer implements AutoCloseable {
    //#if MC >= 1.21.11
    //$$ // From 1.21.5 the game draws through its own GPU device, which keeps vertex arrays itself, so a layer is a vertex
    //$$ // buffer and its sorted indices (translucent), drawn by CubeRenderer in a render pass with the game's own terrain
    //$$ // pipelines. Opaque layers use the game's shared quad indices.
    //$$ @Nullable GpuBuffer vertices, indices;
    //$$ @Nullable VertexFormat.IndexType indexType;
    //$$ int indexCount;

    //$$ CubeBuffer() {
        //$$ RenderSystem.assertOnRenderThread();
    //$$ }

    //$$ /** Uploads a mesh (and closes it). */
    //$$ void upload(MeshData mesh) {
        //$$ try (mesh) {
            //$$ RenderSystem.assertOnRenderThread();
            //$$ MeshData.DrawState state = mesh.drawState();
            //$$ if (state.mode() != VertexFormat.Mode.QUADS) throw new IllegalArgumentException("Cube buffers hold quads, not " + state.mode());
            //$$ CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
            //$$ ByteBuffer vertexData = mesh.vertexBuffer();
            //$$ if (vertices == null || vertices.isClosed() || vertices.size() < vertexData.remaining()) {
                //$$ if (vertices != null) vertices.close();
                //$$ vertices = RenderSystem.getDevice().createBuffer(() -> "Chunks3 cube vertices", GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST, vertexData);
            //$$ } else {
                //$$ encoder.writeToBuffer(vertices.slice(), vertexData);
            //$$ }
            //$$ ByteBuffer own = mesh.indexBuffer();
            //$$ if (own == null) {
                //$$ if (indices != null) indices.close();
                //$$ indices = null;
                //$$ indexType = null;
            //$$ } else {
                //$$ if (indices == null || indices.isClosed() || indices.size() < own.remaining()) {
                    //$$ if (indices != null) indices.close();
                    //$$ indices = RenderSystem.getDevice().createBuffer(() -> "Chunks3 cube indices", GpuBuffer.USAGE_INDEX | GpuBuffer.USAGE_COPY_DST, own);
                //$$ } else {
                    //$$ encoder.writeToBuffer(indices.slice(), own);
                //$$ }
                //$$ indexType = state.indexType();
            //$$ }
            //$$ indexCount = state.indexCount();
        //$$ }
    //$$ }

    //$$ @Override
    //$$ public void close() {
        //$$ if (vertices != null) vertices.close();
        //$$ if (indices != null) indices.close();
        //$$ vertices = indices = null;
        //$$ indexCount = 0;
    //$$ }


    //#else
    private int array, vertices, indices;
    @Nullable private VertexFormat format;
    private int indexCount, indexType;
    /** True when this layer has its own sorted indices (translucent), else the shared quad ones. */
    private boolean sorted;

    CubeBuffer() {
        RenderSystem.assertOnRenderThread();
        array = GlStateManager._glGenVertexArrays();
        vertices = GlStateManager._glGenBuffers();
        indices = GlStateManager._glGenBuffers();
    }

    /** Uploads a mesh (and closes it); leaves no vertex array bound. */
    void upload(MeshData mesh) {
        try (mesh) {
            if (array < 0) return;
            RenderSystem.assertOnRenderThread();
            MeshData.DrawState state = mesh.drawState();
            if (state.mode() != VertexFormat.Mode.QUADS) throw new IllegalArgumentException("Cube buffers hold quads, not " + state.mode());
            Gl.invalidate();
            GlStateManager._glBindVertexArray(array);
            GlStateManager._glBindBuffer(GL15C.GL_ARRAY_BUFFER, vertices);
            if (!state.format().equals(format)) {
                if (format != null) format.clearBufferState();
                state.format().setupBufferState();
                format = state.format();
            }
            ByteBuffer vertexData = mesh.vertexBuffer();
            if (vertexData != null) GlStateManager._glBufferData(GL15C.GL_ARRAY_BUFFER, vertexData, GL15C.GL_STATIC_DRAW);
            ByteBuffer own = mesh.indexBuffer();
            sorted = own != null;
            indexCount = state.indexCount();
            if (sorted) {
                GlStateManager._glBindBuffer(GL15C.GL_ELEMENT_ARRAY_BUFFER, indices);
                GlStateManager._glBufferData(GL15C.GL_ELEMENT_ARRAY_BUFFER, own, GL15C.GL_STATIC_DRAW);
                indexType = state.indexType().asGLType;
            }
            else {
                QuadIndices.ensure(indexCount);
                indexType = GL11C.GL_UNSIGNED_INT;
            }
            GlStateManager._glBindVertexArray(0);
            Gl.invalidate();
        }
    }

    /** Draws the mesh with the current shader; the caller unbinds the array afterwards. */
    void draw() {
        if (array < 0 || indexCount == 0) return;
        GlStateManager._glBindVertexArray(array);
        GlStateManager._glBindBuffer(GL15C.GL_ELEMENT_ARRAY_BUFFER, sorted ? indices : QuadIndices.name);
        GlStateManager._drawElements(GL11C.GL_TRIANGLES, indexCount, indexType, 0L);
    }

    static void unbind() {
        GlStateManager._glBindVertexArray(0);
        Gl.invalidate();
    }

    @Override
    public void close() {
        if (array < 0) return;
        GlStateManager._glDeleteBuffers(vertices);
        GlStateManager._glDeleteBuffers(indices);
        org.lwjgl.opengl.GL30C.glDeleteVertexArrays(array);
        array = vertices = indices = -1;
    }

    //#endif

    /** Indices 0 1 2 2 3 0 for every quad, as 32-bit ints, shared by every opaque layer and by far water ({@link FarWaterBuffers}). */
    static final class QuadIndices {
        static int name;
        static int capacity;

        static void ensure(int count) {
            if (name == 0) name = GlStateManager._glGenBuffers();
            //#if MC >= 1.21.11
            //$$ // The game's device leaves its last vertex array bound; binding indices then would change its arrays.
            //$$ GlStateManager._glBindVertexArray(0);
            //#endif
            GlStateManager._glBindBuffer(GL15C.GL_ELEMENT_ARRAY_BUFFER, name);
            if (count <= capacity) return;
            int quads = Math.max(1 << 14, Integer.highestOneBit(Math.max(1, count / 6 - 1)) << 1);
            IntBuffer data = MemoryUtil.memAllocInt(quads * 6);
            try {
                for (int quad = 0; quad < quads; quad++) {
                    int v = quad * 4;
                    data.put(v).put(v + 1).put(v + 2).put(v + 2).put(v + 3).put(v);
                }
                data.flip();
                GL15C.glBufferData(GL15C.GL_ELEMENT_ARRAY_BUFFER, data, GL15C.GL_STATIC_DRAW);
            }
            finally {
                MemoryUtil.memFree(data);
            }
            capacity = quads * 6;
        }
    }
}
