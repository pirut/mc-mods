package io.github.pirut.chunks3.mixin;

import net.minecraft.client.renderer.culling.Frustum;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The culling view can differ from the draw matrices, especially with a captured frustum. */
@Mixin(Frustum.class)
public interface FrustumAccess {
    @Accessor("matrix") Matrix4f chunks3$matrix();
    @Accessor("intersection") FrustumIntersection chunks3$intersection();
    @Accessor("camX") double chunks3$camX();
    @Accessor("camY") double chunks3$camY();
    @Accessor("camZ") double chunks3$camZ();
}
