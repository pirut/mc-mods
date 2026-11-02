package io.github.pirut.seaworthy.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.ryanhcode.sable.SableClient;
import dev.ryanhcode.sable.render.water_occlusion.WaterOcclusionRenderer;
import io.github.pirut.seaworthy.Seaworthy;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL20C;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Sable's water occlusion with Sodium.
 *
 * <p>Sable hides the sea inside a ship by discarding water fragments that lie between the near and far faces of the
 * ship's dry space. It does that in vanilla's translucent terrain shader, so with Sodium, which draws terrain with
 * its own shaders, the sea showed inside every hull (players still couldn't swim in it). This adds the same test to
 * Sodium's terrain shader and gives it Sable's two depth buffers while Sodium draws translucent terrain.
 */
public final class SodiumOcclusion {
    static final String ENABLED = "seaworthy_Occlusion";
    static final String SCREEN = "seaworthy_ScreenSize";
    static final String CLOSE = "seaworthy_CloseDepth";
    static final String FAR = "seaworthy_FarDepth";
    /** Texture units Sodium's terrain shader leaves free (it uses 0 and 1). */
    private static final int CLOSE_UNIT = 6, FAR_UNIT = 7;

    private static final String UNIFORMS = """
            uniform float %s;
            uniform vec2 %s;
            uniform sampler2D %s;
            uniform sampler2D %s;
            """.formatted(ENABLED, SCREEN, CLOSE, FAR);
    private static final String TEST = """
                if (%1$s > 0.0) {
                    vec2 seaworthyUv = gl_FragCoord.xy / %2$s;
                    float seaworthyDepth = gl_FragCoord.z;
                    if (seaworthyDepth > texture(%3$s, seaworthyUv).r && seaworthyDepth < texture(%4$s, seaworthyUv).r) discard;
                }
            """.formatted(ENABLED, SCREEN, CLOSE, FAR);

    private static boolean translucent;
    private static boolean broken;
    private static final Int2ObjectOpenHashMap<int[]> LOCATIONS = new Int2ObjectOpenHashMap<>();
    private static @Nullable Field closeField, farField;

    private SodiumOcclusion() {
    }

    /** Adds the occlusion test to Sodium's terrain fragment shader. */
    public static String patchShader(String source) {
        LOCATIONS.clear(); // Sodium is making its programs again
        int main = source.indexOf("void main()");
        int body = main < 0 ? -1 : source.indexOf('{', main);
        if (body < 0) {
            Seaworthy.LOG.warn("Couldn't find main() in Sodium's terrain shader, so the sea may show inside ships");
            return source;
        }
        return source.substring(0, main) + UNIFORMS + "\n" + source.substring(main, body + 1) + "\n" + TEST
                + source.substring(body + 1);
    }

    /** Sodium is about to draw a terrain layer. */
    public static void beginLayer(boolean isTranslucent) {
        translucent = isTranslucent;
        if (!isTranslucent || broken) return;
        try {
            if (!WaterOcclusionRenderer.isEnabled()) return;
            // Sable only does this from vanilla's terrain renderer, which Sodium replaces.
            SableClient.WATER_OCCLUSION_RENDERER.preRenderTranslucent(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix());
        } catch (RuntimeException | LinkageError e) {
            fail(e);
        }
    }

    public static void endLayer() {
        translucent = false;
    }

    /** Sodium has bound a terrain shader and set it up: give it the depth buffers, or turn the test off. */
    public static void setupShader() {
        if (broken) return;
        try {
            setupProgram();
        } catch (RuntimeException | LinkageError e) {
            fail(e);
        }
    }

    private static void setupProgram() {
        int program = GL20C.glGetInteger(GL20C.GL_CURRENT_PROGRAM);
        if (program == 0) return;
        int[] at = LOCATIONS.computeIfAbsent(program, p -> new int[]{
                GL20C.glGetUniformLocation(p, ENABLED), GL20C.glGetUniformLocation(p, SCREEN),
                GL20C.glGetUniformLocation(p, CLOSE), GL20C.glGetUniformLocation(p, FAR)});
        if (at[0] < 0) return;

        int close = translucent && WaterOcclusionRenderer.isEnabled() ? depthTexture(true) : 0;
        int far = close != 0 ? depthTexture(false) : 0;
        if (close == 0 || far == 0) {
            GL20C.glUniform1f(at[0], 0.0f);
            return;
        }
        var target = Minecraft.getInstance().getMainRenderTarget();
        GL20C.glUniform1f(at[0], 1.0f);
        if (at[1] >= 0) GL20C.glUniform2f(at[1], target.width, target.height);
        bind(CLOSE_UNIT, close, at[2]);
        bind(FAR_UNIT, far, at[3]);
        GlStateManager._activeTexture(GL20C.GL_TEXTURE0);
    }

    private static void bind(int unit, int texture, int location) {
        GlStateManager._activeTexture(GL20C.GL_TEXTURE0 + unit);
        GlStateManager._bindTexture(texture);
        if (location >= 0) GL20C.glUniform1i(location, unit);
    }

    /** The depth texture of Sable's near or far buffer, or 0 if it has none this frame. */
    private static int depthTexture(boolean close) {
        try {
            if (closeField == null) {
                closeField = WaterOcclusionRenderer.class.getDeclaredField("closeBuffer");
                farField = WaterOcclusionRenderer.class.getDeclaredField("farBuffer");
                closeField.setAccessible(true);
                farField.setAccessible(true);
            }
            Object fbo = (close ? closeField : farField).get(SableClient.WATER_OCCLUSION_RENDERER);
            if (fbo == null) return 0;
            Method attachment = fbo.getClass().getMethod("getDepthTextureAttachment");
            Object texture = attachment.invoke(fbo);
            if (texture instanceof AbstractTexture t) return t.getId();
            if (texture == null) return 0;
            return (int) texture.getClass().getMethod("getId").invoke(texture);
        } catch (ReflectiveOperationException | RuntimeException e) {
            fail(e);
            return 0;
        }
    }

    private static void fail(Throwable e) {
        if (broken) return;
        broken = true;
        Seaworthy.LOG.error("Couldn't hide the sea inside ships with Sodium; it will show through", e);
    }

}
