package net.neoforged.neoforge.client.model;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.UnmodifiableIterator;
import com.mojang.datafixers.util.Either;
import com.mojang.math.Transformation;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextMap;

public class CompositeUnbakedGeometry implements ExtendedUnbakedGeometry {
   final ImmutableMap<String, Either<Identifier, UnbakedModel>> children;

   public CompositeUnbakedGeometry(ImmutableMap<String, Either<Identifier, UnbakedModel>> children) {
      this.children = children;
   }

   @Override
   public QuadCollection bake(TextureSlots textureSlots, ModelBaker baker, ModelState state, ModelDebugName debugName, ContextMap additionalProperties) {
      Transformation rootTransform = additionalProperties.getOrDefault(NeoForgeModelProperties.TRANSFORM, Transformation.identity());
      if (!rootTransform.isIdentity()) {
         state = UnbakedElementsHelper.composeRootTransformIntoModelState(state, rootTransform);
      }

      Map<String, Boolean> partVisibility = additionalProperties.getOrDefault(NeoForgeModelProperties.PART_VISIBILITY, Map.of());
      QuadCollection.Builder quadBuilder = new QuadCollection.Builder();
      UnmodifiableIterator var9 = this.children.entrySet().iterator();

      while (var9.hasNext()) {
         Entry<String, Either<Identifier, UnbakedModel>> entry = (Entry<String, Either<Identifier, UnbakedModel>>)var9.next();
         String name = entry.getKey();
         if (partVisibility.getOrDefault(name, true)) {
            ResolvedModel model = (ResolvedModel)entry.getValue()
               .map(baker::getModel, inline -> baker.resolveInlineModel(inline, () -> debugName.debugName() + "_" + entry.getKey()));
            QuadCollection modelGeometry = model.bakeTopGeometry(model.getTopTextureSlots(), baker, state);
            quadBuilder.addAll(modelGeometry);
         }
      }

      return quadBuilder.build();
   }
}
