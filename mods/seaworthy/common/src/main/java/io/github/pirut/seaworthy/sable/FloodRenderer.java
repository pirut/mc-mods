package io.github.pirut.seaworthy.sable;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3d;

/**
 * Draws the top of the floodwater inside hulls. The only water really there is the sea, whose surface is at sea
 * level, so water that has come in through a hole and stands lower than that would be invisible.
 *
 * <p>It is drawn with the entity translucent shader rather than the translucent block one, which Sable's water
 * occlusion cuts out inside the hull. Only ever loaded on the client.
 */
public final class FloodRenderer {
    private static final ResourceLocation WATER = ResourceLocation.withDefaultNamespace("block/water_still");
    private static final int ALPHA = 0xC0;

    private FloodRenderer() {
    }

    /**
     * After the translucent blocks.
     *
     * @param view the camera's rotation (the level's model-view matrix), with the camera at the origin
     */
    public static void render(Matrix4f view, Camera camera, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        ShipTracker tracker = ShipTracker.of(level);
        if (tracker == null) return;

        TextureAtlasSprite sprite = mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(WATER);
        RenderType type = RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS);
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer out = buffers.getBuffer(type);
        Vec3 cam = camera.getPosition();
        // Depending on the loader and version the camera's rotation is already applied (in RenderSystem's model-view
        // matrix) or still to be, so apply it only if it isn't.
        Matrix4f m = RenderSystem.getModelViewMatrix().equals(new Matrix4f(), 1e-5f) ? new Matrix4f(view) : new Matrix4f();
        float u0 = sprite.getU0(), u1 = sprite.getU1(), v0 = sprite.getV0(), v1 = sprite.getV1();
        Vector3d[] corner = {new Vector3d(), new Vector3d(), new Vector3d(), new Vector3d()};
        Vector3d up = new Vector3d();
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        boolean[] any = {false};

        tracker.forEachHull(hull -> {
            if (!(hull.subLevel() instanceof ClientSubLevel subLevel) || subLevel.isRemoved()) return;
            Pose3dc pose = subLevel.renderPose(partialTick);
            Vector3d centre = pose.transformPosition(new Vector3d(subLevel.getPlot().getBoundingBox().minX(),
                    subLevel.getPlot().getBoundingBox().minY(), subLevel.getPlot().getBoundingBox().minZ()));
            if (centre.distanceSquared(cam.x, cam.y, cam.z) > 512 * 512) return;
            at.set(centre.x, centre.y, centre.z);
            int tint = BiomeColors.getAverageWaterColor(level, at);
            int r = (tint >> 16) & 0xFF, g = (tint >> 8) & 0xFF, b = tint & 0xFF;
            pose.orientation().transform(up.set(0, 1, 0));
            float nx = (float) up.x, ny = (float) up.y, nz = (float) up.z;

            hull.surface((x, y, z, height) -> {
                double top = y + height;
                pose.transformPosition(corner[0].set(x, top, z));
                pose.transformPosition(corner[1].set(x, top, z + 1));
                pose.transformPosition(corner[2].set(x + 1, top, z + 1));
                pose.transformPosition(corner[3].set(x + 1, top, z));
                Vector3d c0 = corner[0];
                at.set(c0.x + 0.5 * (corner[2].x - c0.x), c0.y + 0.5 * (corner[2].y - c0.y) + 0.5, c0.z + 0.5 * (corner[2].z - c0.z));
                int light = LevelRenderer.getLightColor(level, at);
                // Both sides, so it shows from below the surface too.
                vertex(out, m, corner[0], cam, r, g, b, u0, v0, light, nx, ny, nz);
                vertex(out, m, corner[1], cam, r, g, b, u0, v1, light, nx, ny, nz);
                vertex(out, m, corner[2], cam, r, g, b, u1, v1, light, nx, ny, nz);
                vertex(out, m, corner[3], cam, r, g, b, u1, v0, light, nx, ny, nz);
                vertex(out, m, corner[3], cam, r, g, b, u1, v0, light, -nx, -ny, -nz);
                vertex(out, m, corner[2], cam, r, g, b, u1, v1, light, -nx, -ny, -nz);
                vertex(out, m, corner[1], cam, r, g, b, u0, v1, light, -nx, -ny, -nz);
                vertex(out, m, corner[0], cam, r, g, b, u0, v0, light, -nx, -ny, -nz);
                any[0] = true;
            });
        });
        if (any[0]) buffers.endBatch(type);
    }

    private static void vertex(VertexConsumer out, Matrix4f m, Vector3d p, Vec3 cam, int r, int g, int b,
                               float u, float v, int light, float nx, float ny, float nz) {
        out.addVertex(m, (float) (p.x - cam.x), (float) (p.y - cam.y), (float) (p.z - cam.z))
                .setColor(r, g, b, ALPHA)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(nx, ny, nz);
    }
}
