//#if MC >= 1.21.11
//$$ package io.github.pirut.chunks3.mixin;

//$$ import com.mojang.blaze3d.textures.GpuSampler;
//$$ import io.github.pirut.chunks3.client.CubicClient;
//$$ import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
//$$ import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//$$ /** The cubes and far terrain draw just after the game's own sections of each group, on either loader. */
//$$ @Mixin(ChunkSectionsToRender.class)
//$$ abstract class CubicSectionGroupMixin {
    //$$ @Inject(method = "renderGroup", at = @At("TAIL"))
    //$$ private void chunks3$afterGroup(ChunkSectionLayerGroup group, GpuSampler sampler, CallbackInfo info) {
        //$$ CubicClient.afterSectionGroup(group == ChunkSectionLayerGroup.OPAQUE, group == ChunkSectionLayerGroup.TRANSLUCENT);
    //$$ }
//$$ }
//#endif
