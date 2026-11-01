//#if MC >= 1.21.11
//$$ package io.github.pirut.chunks3.mixin;

//$$ import com.mojang.blaze3d.opengl.GlCommandEncoder;
//$$ import com.mojang.blaze3d.opengl.GlProgram;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.gen.Accessor;

//$$ /** The program the game's GL device last bound, which it binds again only when it changes: unset after GL calls of ours. */
//$$ @Mixin(GlCommandEncoder.class)
//$$ public interface GlCommandEncoderAccess {
    //$$ @Accessor("lastProgram") void chunks3$setLastProgram(GlProgram program);
//$$ }
//#endif
