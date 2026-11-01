package net.neoforged.neoforge.client.textures;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceMetadata;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface SpriteContentsConstructor {
   @Nullable
   SpriteContents create(ResourceLocation var1, FrameSize var2, NativeImage var3, ResourceMetadata var4);
}
