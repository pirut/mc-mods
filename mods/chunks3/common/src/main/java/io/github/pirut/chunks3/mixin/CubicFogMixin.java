//#if MC >= 1.21.11
//$$ package io.github.pirut.chunks3.mixin;

//$$ import io.github.pirut.chunks3.client.CubicClient;
//$$ import io.github.pirut.chunks3.client.Gl;
//$$ import net.minecraft.client.Camera;
//$$ import net.minecraft.client.renderer.fog.FogRenderer;
//$$ import net.minecraft.world.level.material.FogType;
//$$ import org.joml.Vector4f;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.ModifyArgs;
//$$ import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

//$$ /**
 //$$ * From 1.21.5 the game fills a fog buffer once a frame: terrain fog is pushed out to the far terrain's reach and deep
 //$$ * water darkens there (see {@link CubicClient#fog} and {@link CubicClient#waterFogLight}), on either loader, and what
 //$$ * goes in is kept for the far terrain's shaders.
 //$$ */
//$$ @Mixin(FogRenderer.class)
//$$ abstract class CubicFogMixin {
    //$$ @ModifyArgs(method = "setupFog", at = @At(value = "INVOKE",
            //$$ target = "Lnet/minecraft/client/renderer/fog/FogRenderer;updateBuffer(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V"))
    //$$ private void chunks3$fog(Args args, Camera camera, int renderDistance, net.minecraft.client.DeltaTracker delta, float darken,
                             //$$ net.minecraft.client.multiplayer.ClientLevel level) {
        //$$ Vector4f colour = args.get(2);
        //$$ float environmentalStart = args.get(3), environmentalEnd = args.get(4), distanceStart = args.get(5), distanceEnd = args.get(6);
        //$$ float light = CubicClient.waterFogLight(camera);
        //$$ if (light != 1.0F) {
            //$$ colour = new Vector4f(colour.x * light, colour.y * light, colour.z * light, colour.w);
            //$$ args.set(2, colour);
        //$$ }
        //$$ float[] fog = CubicClient.fog(camera.getFluidInCamera(), camera, distanceEnd, renderDistance * 16.0F);
        //$$ if (fog != null) {
            //$$ distanceStart = fog[0];
            //$$ distanceEnd = fog[1];
            //$$ environmentalStart = Math.max(environmentalStart, fog[0]);
            //$$ environmentalEnd = Math.max(environmentalEnd, fog[1]);
            //$$ args.set(3, environmentalStart);
            //$$ args.set(4, environmentalEnd);
            //$$ args.set(5, distanceStart);
            //$$ args.set(6, distanceEnd);
        //$$ }
        //$$ Gl.FogState.set(environmentalStart, environmentalEnd, distanceStart, distanceEnd, colour.x, colour.y, colour.z, colour.w);
    //$$ }
//$$ }
//#endif
