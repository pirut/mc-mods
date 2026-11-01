package net.neoforged.neoforge.client.textures;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.Identifier;

public class UnitTextureAtlasSprite extends TextureAtlasSprite {
   public static final Identifier LOCATION = Identifier.fromNamespaceAndPath("neoforge", "unit");
   public static final UnitTextureAtlasSprite INSTANCE = new UnitTextureAtlasSprite();

   private UnitTextureAtlasSprite() {
      super(LOCATION, new SpriteContents(LOCATION, new FrameSize(1, 1), new NativeImage(1, 1, false)), 1, 1, 0, 0, 0);
   }

   @Override
   public float getU(float u) {
      return u;
   }

   @Override
   public float getV(float v) {
      return v;
   }
}
