package net.neoforged.neoforge.client.model.generators.template;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import org.jspecify.annotations.Nullable;

public class ExtendedModelTemplateBuilder {
   Optional<Identifier> parent = Optional.empty();
   Optional<String> suffix = Optional.empty();
   final Set<TextureSlot> requiredSlots = new HashSet<>();
   final Map<ItemDisplayContext, TransformVecBuilder> transforms = new LinkedHashMap<>();
   final List<ElementBuilder> elements = new ArrayList<>();
   @Nullable
   CustomLoaderBuilder customLoader = null;
   final RootTransformsBuilder rootTransforms = new RootTransformsBuilder();
   @Nullable
   Identifier renderType = null;
   @Nullable
   Boolean ambientOcclusion = null;
   @Nullable
   UnbakedModel.GuiLight guiLight = null;

   public static ExtendedModelTemplateBuilder of(ModelTemplate template) {
      ExtendedModelTemplateBuilder builder = new ExtendedModelTemplateBuilder();
      builder.parent = template.model;
      builder.suffix = template.suffix;
      builder.requiredSlots.addAll(template.requiredSlots);
      if (template instanceof ExtendedModelTemplate ext) {
         ext.transforms.forEach((ctx, vecBuilder) -> builder.transforms.put(ctx, vecBuilder.copy()));
         ext.elements.forEach(elem -> builder.elements.add(elem.copy()));
         builder.customLoader = ext.customLoader != null ? ext.customLoader.copy() : null;
         builder.rootTransforms.copyFrom(ext.rootTransforms);
         builder.renderType = ext.renderType;
         builder.ambientOcclusion = ext.ambientOcclusion;
         builder.guiLight = ext.guiLight;
      }

      return builder;
   }

   public static ExtendedModelTemplateBuilder builder() {
      return new ExtendedModelTemplateBuilder();
   }

   public ExtendedModelTemplateBuilder parent(Identifier parent) {
      this.parent = Optional.of(parent);
      return this;
   }

   public ExtendedModelTemplateBuilder suffix(String suffix) {
      this.suffix = Optional.of(suffix);
      return this;
   }

   public ExtendedModelTemplateBuilder requiredTextureSlot(TextureSlot slot) {
      this.requiredSlots.add(slot);
      return this;
   }

   public ExtendedModelTemplateBuilder renderType(String renderType) {
      Preconditions.checkNotNull(renderType, "Render type must not be null");
      return this.renderType(Identifier.parse(renderType));
   }

   public ExtendedModelTemplateBuilder renderType(Identifier renderType) {
      Preconditions.checkNotNull(renderType, "Render type must not be null");
      this.renderType = renderType;
      return this;
   }

   public ExtendedModelTemplateBuilder transform(ItemDisplayContext type, Consumer<TransformVecBuilder> action) {
      Preconditions.checkNotNull(type, "Perspective cannot be null");
      TransformVecBuilder builder = this.transforms.computeIfAbsent(type, TransformVecBuilder::new);
      action.accept(builder);
      return this;
   }

   public ExtendedModelTemplateBuilder ambientOcclusion(boolean ambientOcclusion) {
      this.ambientOcclusion = ambientOcclusion;
      return this;
   }

   public ExtendedModelTemplateBuilder guiLight(UnbakedModel.GuiLight light) {
      this.guiLight = light;
      return this;
   }

   public ExtendedModelTemplateBuilder element(Consumer<ElementBuilder> action) {
      Preconditions.checkState(
         this.customLoader == null || this.customLoader.allowInlineElements,
         "Custom model loader %s does not support inline elements",
         this.customLoader != null ? this.customLoader.loaderId : null
      );
      ElementBuilder ret = new ElementBuilder();
      action.accept(ret);
      this.elements.add(ret);
      return this;
   }

   public ExtendedModelTemplateBuilder element(int index, Consumer<ElementBuilder> action) {
      Preconditions.checkState(
         this.customLoader == null || this.customLoader.allowInlineElements,
         "Custom model loader %s does not support inline elements",
         this.customLoader != null ? this.customLoader.loaderId : null
      );
      Preconditions.checkElementIndex(index, this.elements.size(), "Element index");
      action.accept(this.elements.get(index));
      return this;
   }

   public int getElementCount() {
      return this.elements.size();
   }

   public <L extends CustomLoaderBuilder> ExtendedModelTemplateBuilder customLoader(Supplier<L> customLoaderFactory, Consumer<L> action) {
      Preconditions.checkNotNull(customLoaderFactory, "customLoaderFactory must not be null");
      L customLoader = (L)customLoaderFactory.get();
      Preconditions.checkState(
         customLoader.allowInlineElements || this.elements.isEmpty(), "Custom model loader %s does not support inline elements", customLoader.loaderId
      );
      this.customLoader = customLoader;
      action.accept(customLoader);
      return this;
   }

   public ExtendedModelTemplateBuilder rootTransforms(Consumer<RootTransformsBuilder> action) {
      action.accept(this.rootTransforms);
      return this;
   }

   public ExtendedModelTemplate build() {
      return new ExtendedModelTemplate(this);
   }
}
