package net.neoforged.neoforge.client.model;

import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextMap;
import org.jspecify.annotations.Nullable;

public abstract class AbstractUnbakedModel implements UnbakedModel {
   protected final StandardModelParameters parameters;

   protected AbstractUnbakedModel(StandardModelParameters parameters) {
      this.parameters = parameters;
   }

   @Nullable
   @Override
   public Boolean ambientOcclusion() {
      return this.parameters.ambientOcclusion();
   }

   @Nullable
   @Override
   public UnbakedModel.GuiLight guiLight() {
      return this.parameters.guiLight();
   }

   @Nullable
   @Override
   public ItemTransforms transforms() {
      return this.parameters.itemTransforms();
   }

   @Override
   public TextureSlots.Data textureSlots() {
      return this.parameters.textures();
   }

   @Nullable
   @Override
   public Identifier parent() {
      return this.parameters.parent();
   }

   @Override
   public void fillAdditionalProperties(ContextMap.Builder propertiesBuilder) {
      NeoForgeModelProperties.fillRootTransformProperty(propertiesBuilder, this.parameters.rootTransform());
      NeoForgeModelProperties.fillRenderTypeProperty(propertiesBuilder, this.parameters.renderTypeGroup());
      NeoForgeModelProperties.fillPartVisibilityProperty(propertiesBuilder, this.parameters.partVisibility());
   }
}
