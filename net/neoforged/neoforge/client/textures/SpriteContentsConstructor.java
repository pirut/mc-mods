package net.neoforged.neoforge.client.textures;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import org.jspecify.annotations.Nullable;

@FunctionalInterface
public interface SpriteContentsConstructor {
   @Nullable
   SpriteContents create(
      Identifier var1,
      FrameSize var2,
      NativeImage var3,
      Optional<AnimationMetadataSection> var4,
      List<MetadataSectionType.WithValue<?>> var5,
      Optional<TextureMetadataSection> var6
   );
}
