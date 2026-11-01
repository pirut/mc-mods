package net.neoforged.neoforge.client.extensions;

import java.util.function.UnaryOperator;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.TexturedModel;

public interface ITexturedModelExtension {
   default TexturedModel updateTemplate(UnaryOperator<ModelTemplate> modifier) {
      return new TexturedModel(this.self().getMapping(), modifier.apply(this.self().getTemplate()));
   }

   private TexturedModel self() {
      return (TexturedModel)this;
   }

   public interface Provider {
      default TexturedModel.Provider updateTemplate(UnaryOperator<ModelTemplate> modifier) {
         return block -> this.self().get(block).updateTemplate(modifier);
      }

      private TexturedModel.Provider self() {
         return (TexturedModel.Provider)this;
      }
   }
}
