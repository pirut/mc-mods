//#if MC >= 1.21.11
//$$ package io.github.pirut.chunks3.mixin;

//$$ import com.mojang.blaze3d.buffers.GpuBufferSlice;
//$$ import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
//$$ import io.github.pirut.chunks3.client.CubicClient;
//$$ import javax.annotation.Nullable;
//$$ import net.minecraft.client.Camera;
//$$ import net.minecraft.client.DeltaTracker;
//$$ import net.minecraft.client.renderer.LevelRenderer;
//$$ import net.minecraft.client.renderer.culling.Frustum;
//$$ import net.minecraft.client.renderer.state.LevelRenderState;
//$$ import net.minecraft.world.phys.Vec3;
//$$ import org.joml.Matrix4f;
//$$ import org.joml.Vector4f;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//$$ /**
 //$$ * From 1.21.5 the level is drawn in passes that run after it is set up, and the stages they reach carry no camera or
 //$$ * matrices: the level's camera, matrices and culling view are kept here as it is set up, for the cubes and far terrain
 //$$ * to draw with. The cubes' block entities join the game's own, to be drawn with them.
 //$$ */
//$$ @Mixin(LevelRenderer.class)
//$$ abstract class CubicLevelFrameMixin {
    //$$ @Inject(method = "renderLevel", at = @At("HEAD"))
    //$$ private void chunks3$frame(GraphicsResourceAllocator allocator, DeltaTracker delta, boolean outline, Camera camera, Matrix4f modelView,
                               //$$ Matrix4f projection, Matrix4f cullProjection, GpuBufferSlice fog, Vector4f fogColor, boolean sky, CallbackInfo info) {
        //$$ CubicClient.LevelFrame.begin(camera, modelView, projection, delta.getGameTimeDeltaPartialTick(false));
    //$$ }

    //$$ @Inject(method = "prepareCullFrustum", at = @At("RETURN"))
    //$$ private void chunks3$frustum(Matrix4f modelView, Matrix4f projection, Vec3 camera, CallbackInfoReturnable<Frustum> info) {
        //$$ CubicClient.LevelFrame.frustum = info.getReturnValue();
    //$$ }

    //$$ @Inject(method = "extractVisibleBlockEntities", at = @At("TAIL"))
    //#if NEOFORGE
    //$$ private void chunks3$blockEntities(Camera camera, float partialTick, LevelRenderState state, @Nullable Frustum frustum, CallbackInfo info) {
    //#else
    //$$ private void chunks3$blockEntities(Camera camera, float partialTick, LevelRenderState state, CallbackInfo info) {
        //$$ Frustum frustum = CubicClient.LevelFrame.frustum; // vanilla has no frustum parameter (NeoForge adds it)
    //#endif
        //$$ CubicClient.extractBlockEntities(state.blockEntityRenderStates, frustum, partialTick);
    //$$ }
//$$ }
//#endif
