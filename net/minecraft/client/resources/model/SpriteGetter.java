package net.minecraft.client.resources.model;

import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface SpriteGetter {
   TextureAtlasSprite get(Material var1, ModelDebugName var2);

   TextureAtlasSprite reportMissingReference(String var1, ModelDebugName var2);

   default TextureAtlasSprite resolveSlot(TextureSlots p_404867_, String p_405322_, ModelDebugName p_405457_) {
      Material material = p_404867_.getMaterial(p_405322_);
      return material != null ? this.get(material, p_405457_) : this.reportMissingReference(p_405322_, p_405457_);
   }
}
