package net.neoforged.neoforge.client.extensions;

import net.minecraft.Optionull;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.UnbakedModel;
import org.jspecify.annotations.Nullable;

public interface ModelBakerExtension {
   default ResolvedModel resolveInlineModel(final UnbakedModel inlineModel, final ModelDebugName debugName) {
      final ResolvedModel resolvedParent = Optionull.map(inlineModel.parent(), this.self()::getModel);
      return new ResolvedModel() {
         @Override
         public UnbakedModel wrapped() {
            return inlineModel;
         }

         @Nullable
         @Override
         public ResolvedModel parent() {
            return resolvedParent;
         }

         @Override
         public String debugName() {
            return debugName.debugName();
         }
      };
   }

   private ModelBaker self() {
      return (ModelBaker)this;
   }
}
