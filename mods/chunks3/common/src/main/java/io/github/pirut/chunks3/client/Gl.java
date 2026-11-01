package io.github.pirut.chunks3.client;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
//#if MC >= 1.21.11
//$$ import com.mojang.blaze3d.opengl.GlDevice;
//$$ import com.mojang.blaze3d.opengl.GlTexture;
//$$ import com.mojang.blaze3d.pipeline.RenderTarget;
//$$ import com.mojang.blaze3d.systems.RenderSystem;
//$$ import io.github.pirut.chunks3.mixin.GlCommandEncoderAccess;
//$$ import net.minecraft.client.renderer.texture.TextureAtlas;
//$$ import org.lwjgl.opengl.GL11C;
//$$ import org.lwjgl.opengl.GL13C;
//#else
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import net.minecraft.world.inventory.InventoryMenu;
//#endif

/**
 * The game's GL state that the far terrain's own GL calls read and leave, the same on every version. Up to 1.21.4 the
 * game drew with GL state it set as it went; from 1.21.5 it draws through its own GPU device, which binds what each draw
 * needs itself, keeps the fog and lightmap in its own buffers and textures, and binds no framebuffer between passes.
 */
public final class Gl {
    private Gl() {}

    /** The texture unit made active last. */
    static int activeTexture() {
        //#if MC >= 1.21.11
        //$$ return GL11C.glGetInteger(GL13C.GL_ACTIVE_TEXTURE);
        //#else
        return GlStateManager._getActiveTexture();
        //#endif
    }

    /** After GL calls of our own: the game must bind its program and arrays again before its next draw. */
    static void invalidate() {
        //#if MC >= 1.21.11
        //$$ if (RenderSystem.getDevice() instanceof GlDevice device && device.createCommandEncoder() instanceof GlCommandEncoderAccess encoder) {
        //$$     encoder.chunks3$setLastProgram(null);
        //$$ }
        //#else
        BufferUploader.invalidate();
        //#endif
    }

    /** The block atlas's GL texture. */
    static int blockAtlas() {
        //#if MC >= 1.21.11
        //$$ return ((GlTexture) Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS).getTexture()).glId();
        //#else
        return Minecraft.getInstance().getTextureManager().getTexture(InventoryMenu.BLOCK_ATLAS).getId();
        //#endif
    }

    /** The lightmap's GL texture. */
    static int lightmap() {
        //#if MC >= 1.21.11
        //$$ return ((GlTexture) Minecraft.getInstance().gameRenderer.lightTexture().getTextureView().texture()).glId();
        //#else
        return RenderSystem.getShaderTexture(2);
        //#endif
    }

    /**
     * Binds the framebuffer the level is drawn into, with its viewport, for GL calls of our own. Up to 1.21.4 it is
     * bound already; from 1.21.5 each of the game's passes binds its own and unbinds it after.
     */
    static void bindLevelTarget(boolean translucent) {
        //#if MC >= 1.21.11
        //$$ RenderTarget target = translucent && Minecraft.getInstance().levelRenderer.getTranslucentTarget() != null
        //$$         ? Minecraft.getInstance().levelRenderer.getTranslucentTarget() : Minecraft.getInstance().getMainRenderTarget();
        //$$ if (!(RenderSystem.getDevice() instanceof GlDevice device) || target.getColorTexture() == null) return;
        //$$ int framebuffer = ((GlTexture) target.getColorTexture()).getFbo(device.directStateAccess(), target.getDepthTexture());
        //$$ GlStateManager._glBindFramebuffer(org.lwjgl.opengl.GL30C.GL_FRAMEBUFFER, framebuffer);
        //$$ GlStateManager._viewport(0, 0, target.width, target.height);
        //#endif
    }

    /** Unbinds the framebuffer {@link #bindLevelTarget} bound, as the game's passes leave it. */
    static void unbindLevelTarget() {
        //#if MC >= 1.21.11
        //$$ GlStateManager._glBindFramebuffer(org.lwjgl.opengl.GL30C.GL_FRAMEBUFFER, 0);
        //#endif
    }

    /** The terrain fog this frame, as vanilla's shaders take it: 0 spherical, 1 cylindrical. */
    static int fogShape() {
        //#if MC >= 1.21.11
        //$$ return FogState.shape;
        //#else
        return RenderSystem.getShaderFogShape().getIndex();
        //#endif
    }

    static float fogStart() {
        //#if MC >= 1.21.11
        //$$ return FogState.start;
        //#else
        return RenderSystem.getShaderFogStart();
        //#endif
    }

    static float fogEnd() {
        //#if MC >= 1.21.11
        //$$ return FogState.end;
        //#else
        return RenderSystem.getShaderFogEnd();
        //#endif
    }

    static float[] fogColor() {
        //#if MC >= 1.21.11
        //$$ return FogState.color.clone();
        //#else
        return RenderSystem.getShaderFogColor();
        //#endif
    }

    /** The colour the game multiplies what it draws by (white from 1.21.5, where each draw sets its own). */
    static float[] shaderColor() {
        //#if MC >= 1.21.11
        //$$ return new float[] {1.0F, 1.0F, 1.0F, 1.0F};
        //#else
        return RenderSystem.getShaderColor();
        //#endif
    }

    /**
     * From 1.21.5 the fog lives in a buffer the game fills once a frame: what went in, kept for the far terrain's own
     * shaders (see {@link CubicClient#fog}).
     */
    public static final class FogState {
        private FogState() {}
        static int shape = 1;
        static float start = Float.MAX_VALUE, end = Float.MAX_VALUE;
        static float[] color = {0.0F, 0.0F, 0.0F, 0.0F};

        /** Called as the game fills its fog buffer. */
        public static void set(float environmentalStart, float environmentalEnd, float distanceStart, float distanceEnd, float r, float g, float b, float a) {
            // The game's shaders take the thicker of the two fogs at each point; the far shaders take one, so the nearer.
            if (environmentalEnd < distanceEnd) {
                shape = 0; start = environmentalStart; end = environmentalEnd;
            } else {
                shape = 1; start = distanceStart; end = distanceEnd;
            }
            color = new float[] {r, g, b, a};
        }
    }
}
