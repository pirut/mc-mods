package net.minecraft.client.renderer.block.model;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.block.model.multipart.MultiPart;
import net.minecraft.client.renderer.block.model.multipart.Selector;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BlockModelDefinition {
   private final Map<String, MultiVariant> variants = Maps.newLinkedHashMap();
   private MultiPart multiPart;

   public static BlockModelDefinition fromStream(BlockModelDefinition.Context p_111541_, Reader p_111542_) {
      return GsonHelper.fromJson(p_111541_.gson, p_111542_, BlockModelDefinition.class);
   }

   public static BlockModelDefinition fromJsonElement(BlockModelDefinition.Context p_249700_, JsonElement p_250730_) {
      return (BlockModelDefinition)p_249700_.gson.fromJson(p_250730_, BlockModelDefinition.class);
   }

   public BlockModelDefinition(Map<String, MultiVariant> p_111537_, MultiPart p_111538_) {
      this.multiPart = p_111538_;
      this.variants.putAll(p_111537_);
   }

   public BlockModelDefinition(List<BlockModelDefinition> p_111535_) {
      BlockModelDefinition $$1 = null;

      for (BlockModelDefinition $$2 : p_111535_) {
         if ($$2.isMultiPart()) {
            this.variants.clear();
            $$1 = $$2;
         }

         this.variants.putAll($$2.variants);
      }

      if ($$1 != null) {
         this.multiPart = $$1.multiPart;
      }
   }

   @VisibleForTesting
   public boolean hasVariant(String p_173426_) {
      return this.variants.get(p_173426_) != null;
   }

   @VisibleForTesting
   public MultiVariant getVariant(String p_173429_) {
      MultiVariant $$1 = this.variants.get(p_173429_);
      if ($$1 == null) {
         throw new BlockModelDefinition.MissingVariantException();
      } else {
         return $$1;
      }
   }

   @Override
   public boolean equals(Object p_111546_) {
      if (this == p_111546_) {
         return true;
      } else if (p_111546_ instanceof BlockModelDefinition $$1 && this.variants.equals($$1.variants)) {
         return this.isMultiPart() ? this.multiPart.equals($$1.multiPart) : !$$1.isMultiPart();
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return 31 * this.variants.hashCode() + (this.isMultiPart() ? this.multiPart.hashCode() : 0);
   }

   public Map<String, MultiVariant> getVariants() {
      return this.variants;
   }

   @VisibleForTesting
   public Set<MultiVariant> getMultiVariants() {
      Set<MultiVariant> $$0 = Sets.newHashSet(this.variants.values());
      if (this.isMultiPart()) {
         $$0.addAll(this.multiPart.getMultiVariants());
      }

      return $$0;
   }

   public boolean isMultiPart() {
      return this.multiPart != null;
   }

   public MultiPart getMultiPart() {
      return this.multiPart;
   }

   @OnlyIn(Dist.CLIENT)
   public static final class Context {
      protected final Gson gson = new GsonBuilder()
         .registerTypeAdapter(BlockModelDefinition.class, new BlockModelDefinition.Deserializer())
         .registerTypeAdapter(Variant.class, new Variant.Deserializer())
         .registerTypeAdapter(MultiVariant.class, new MultiVariant.Deserializer())
         .registerTypeAdapter(MultiPart.class, new MultiPart.Deserializer(this))
         .registerTypeAdapter(Selector.class, new Selector.Deserializer())
         .create();
      private StateDefinition<Block, BlockState> definition;

      public StateDefinition<Block, BlockState> getDefinition() {
         return this.definition;
      }

      public void setDefinition(StateDefinition<Block, BlockState> p_111553_) {
         this.definition = p_111553_;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class Deserializer implements JsonDeserializer<BlockModelDefinition> {
      public BlockModelDefinition deserialize(JsonElement p_111559_, Type p_111560_, JsonDeserializationContext p_111561_) throws JsonParseException {
         JsonObject $$3 = p_111559_.getAsJsonObject();
         Map<String, MultiVariant> $$4 = this.getVariants(p_111561_, $$3);
         MultiPart $$5 = this.getMultiPart(p_111561_, $$3);
         if (!$$4.isEmpty() || $$5 != null && !$$5.getMultiVariants().isEmpty()) {
            return new BlockModelDefinition($$4, $$5);
         } else {
            throw new JsonParseException("Neither 'variants' nor 'multipart' found");
         }
      }

      protected Map<String, MultiVariant> getVariants(JsonDeserializationContext p_111556_, JsonObject p_111557_) {
         Map<String, MultiVariant> $$2 = Maps.newHashMap();
         if (p_111557_.has("variants")) {
            JsonObject $$3 = GsonHelper.getAsJsonObject(p_111557_, "variants");

            for (Entry<String, JsonElement> $$4 : $$3.entrySet()) {
               $$2.put($$4.getKey(), (MultiVariant)p_111556_.deserialize($$4.getValue(), MultiVariant.class));
            }
         }

         return $$2;
      }

      @Nullable
      protected MultiPart getMultiPart(JsonDeserializationContext p_111563_, JsonObject p_111564_) {
         if (!p_111564_.has("multipart")) {
            return null;
         } else {
            JsonArray $$2 = GsonHelper.getAsJsonArray(p_111564_, "multipart");
            return (MultiPart)p_111563_.deserialize($$2, MultiPart.class);
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   protected class MissingVariantException extends RuntimeException {
   }
}
