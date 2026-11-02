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
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL13C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sable's water occlusion with Sodium, and with Iris shader packs on top of it.
 *
 * <p>Sable hides the sea inside a ship by discarding water fragments that lie between the near and far faces of the
 * ship's dry space. It does that in vanilla's translucent terrain shader, so with Sodium, which draws terrain with
 * its own shaders, the sea showed inside every hull (players still couldn't swim in it). This adds the same test to
 * Sodium's terrain shader and gives it Sable's two depth buffers while Sodium draws translucent terrain. With a
 * shader pack, Iris swaps in programs built from the pack; the test goes into those too.
 */
public final class SodiumOcclusion {
    static final String ENABLED = "seaworthy_Occlusion";
    static final String SCREEN = "seaworthy_ScreenSize";
    static final String CLOSE = "seaworthy_CloseDepth";
    static final String FAR = "seaworthy_FarDepth";
    private static final Pattern MAIN = Pattern.compile("void\\s+main\\s*\\(\\s*(void)?\\s*\\)\\s*\\{");

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
    /** The top two texture units, which neither Sodium nor a shader pack's samplers get to. */
    private static int closeUnit = -1, farUnit;
    private static @Nullable Method irisShadows;
    private static boolean irisChecked;
    private static @Nullable Method modelViewOf, projectionOf;
    private static boolean patchedLogged, workingLogged;

    private SodiumOcclusion() {
    }

    /** Adds the occlusion test to Sodium's terrain fragment shader. */
    public static String patchShader(String source) {
        LOCATIONS.clear(); // the programs are being made again
        if (source.contains(ENABLED)) return source;
        Matcher m = MAIN.matcher(source);
        if (!m.find()) {
            Seaworthy.LOG.warn("Couldn't find main() in a terrain shader, so the sea may show inside ships");
            return source;
        }
        if (!patchedLogged) {
            patchedLogged = true;
            Seaworthy.LOG.info("Added ship water hiding to Sodium's terrain shader");
        }
        return source.substring(0, m.start()) + UNIFORMS + "\n" + source.substring(m.start(), m.end()) + "\n" + TEST
                + source.substring(m.end());
    }

    /** Iris has turned a shader pack's terrain program into Sodium's: add the test to its fragment shader. */
    public static <K> Map<K, String> patchIris(Map<K, String> sources) {
        if (sources == null) return null;
        Map<K, String> patched = new LinkedHashMap<>(sources);
        for (Map.Entry<K, String> e : patched.entrySet()) {
            if (e.getValue() != null && "FRAGMENT".equals(String.valueOf(e.getKey()))) e.setValue(patchShader(e.getValue()));
        }
        return patched;
    }

    /** Sodium is about to draw a terrain layer with these {@code ChunkRenderMatrices}. */
    public static void beginLayer(boolean isTranslucent, @Nullable Object matrices) {
        translucent = isTranslucent && !irisShadowPass();
        if (!translucent || broken) return;
        try {
            if (!WaterOcclusionRenderer.isEnabled()) return;
            // Sable only does this from vanilla's terrain renderer, which Sodium replaces. It needs the camera's view:
            // by the time translucent terrain is drawn, RenderSystem's model view no longer holds it, so the ship's dry
            // space was drawn facing the wrong way and nothing was hidden.
            Matrix4f modelView = matrix(matrices, true);
            Matrix4f projection = matrix(matrices, false);
            if (modelView == null) modelView = new Matrix4f(RenderSystem.getModelViewMatrix());
            if (projection == null) projection = new Matrix4f(RenderSystem.getProjectionMatrix());

            // Draw into Sable's buffers without losing the frame Sodium (or a shader pack) is drawing into.
            int draw = GL11C.glGetInteger(GL30C.GL_DRAW_FRAMEBUFFER_BINDING);
            int read = GL11C.glGetInteger(GL30C.GL_READ_FRAMEBUFFER_BINDING);
            int[] viewport = new int[4];
            GL11C.glGetIntegerv(GL11C.GL_VIEWPORT, viewport);
            try {
                SableClient.WATER_OCCLUSION_RENDERER.preRenderTranslucent(modelView, projection);
            } finally {
                GL30C.glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, draw);
                GL30C.glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, read);
                GL11C.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            }
        } catch (RuntimeException | LinkageError e) {
            fail(e);
        }
    }

    /** The model view or projection matrix out of Sodium's ChunkRenderMatrices, or null if it can't be read. */
    private static @Nullable Matrix4f matrix(@Nullable Object matrices, boolean modelView) {
        if (matrices == null) return null;
        try {
            if (modelViewOf == null) {
                modelViewOf = matrices.getClass().getMethod("modelView");
                projectionOf = matrices.getClass().getMethod("projection");
            }
            Object m = (modelView ? modelViewOf : projectionOf).invoke(matrices);
            return m instanceof Matrix4fc c ? new Matrix4f(c) : null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
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
        if (!workingLogged) {
            workingLogged = true;
            Seaworthy.LOG.info("Hiding the sea inside ships in Sodium's terrain");
        }
        if (at[1] >= 0) GL20C.glUniform2f(at[1], target.width, target.height);
        if (closeUnit < 0) {
            int units = GL20C.glGetInteger(GL20C.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS);
            closeUnit = units - 2;
            farUnit = units - 1;
        }
        // Bound with plain GL: vanilla's texture tracking only knows the first 12 units, and leaves these alone.
        int active = GlStateManager._getActiveTexture();
        bind(closeUnit, close, at[2]);
        bind(farUnit, far, at[3]);
        GL13C.glActiveTexture(active);
    }

    private static void bind(int unit, int texture, int location) {
        GL13C.glActiveTexture(GL13C.GL_TEXTURE0 + unit);
        GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, texture);
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

    /** True while Iris draws the shadow map, which goes through the same terrain renderer from the sun's view. */
    private static boolean irisShadowPass() {
        if (!irisChecked) {
            irisChecked = true;
            try {
                irisShadows = Class.forName("net.irisshaders.iris.shadows.ShadowRenderingState")
                        .getMethod("areShadowsCurrentlyBeingRendered");
            } catch (ReflectiveOperationException | LinkageError e) {
                irisShadows = null; // no Iris
            }
        }
        if (irisShadows == null) return false;
        try {
            return (boolean) irisShadows.invoke(null);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    private static void fail(Throwable e) {
        if (broken) return;
        broken = true;
        Seaworthy.LOG.error("Couldn't hide the sea inside ships with Sodium; it will show through", e);
    }

}
