package net.minecraft.client.resources.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

@Environment(EnvType.CLIENT)
public interface SpriteGetter {
   TextureAtlasSprite get(Material material, ModelDebugName debugName);

   TextureAtlasSprite reportMissingReference(String name, ModelDebugName debugName);

   default TextureAtlasSprite resolveSlot(TextureSlots textureSlots, String name, ModelDebugName modelDebugName) {
      Material material = textureSlots.getMaterial(name);
      return material != null ? this.get(material, modelDebugName) : this.reportMissingReference(name, modelDebugName);
   }
}
