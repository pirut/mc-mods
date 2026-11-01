package net.minecraft.client.renderer.item;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.world.item.ItemDisplayContext;

@Environment(EnvType.CLIENT)
public record ModelRenderProperties(boolean usesBlockLight, TextureAtlasSprite particleIcon, ItemTransforms transforms) {
   public static ModelRenderProperties fromResolvedModel(ModelBaker baker, ResolvedModel model, TextureSlots textureSlots) {
      TextureAtlasSprite textureAtlasSprite = model.resolveParticleSprite(textureSlots, baker);
      return new ModelRenderProperties(model.getTopGuiLight().lightLikeBlock(), textureAtlasSprite, model.getTopTransforms());
   }

   public void applyToLayer(ItemStackRenderState.LayerRenderState renderState, ItemDisplayContext displayContext) {
      renderState.setUsesBlockLight(this.usesBlockLight);
      renderState.setParticleIcon(this.particleIcon);
      renderState.setTransform(this.transforms.getTransform(displayContext));
   }
}
