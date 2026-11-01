package net.minecraft.client.renderer.item;

import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record ModelRenderProperties(boolean usesBlockLight, TextureAtlasSprite particleIcon, ItemTransforms transforms) {
   public static ModelRenderProperties fromResolvedModel(ModelBaker p_404716_, ResolvedModel p_405086_, TextureSlots p_405810_) {
      TextureAtlasSprite textureatlassprite = p_405086_.resolveParticleSprite(p_405810_, p_404716_);
      return new ModelRenderProperties(p_405086_.getTopGuiLight().lightLikeBlock(), textureatlassprite, p_405086_.getTopTransforms());
   }

   public void applyToLayer(ItemStackRenderState.LayerRenderState p_405601_, ItemDisplayContext p_405202_) {
      p_405601_.setUsesBlockLight(this.usesBlockLight);
      p_405601_.setParticleIcon(this.particleIcon);
      p_405601_.setTransform(this.transforms.getTransform(p_405202_));
   }
}
