package net.minecraft.client.resources.metadata.animation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import javax.annotation.Nullable;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.util.GsonHelper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.Validate;

@OnlyIn(Dist.CLIENT)
public class AnimationMetadataSectionSerializer implements MetadataSectionSerializer<AnimationMetadataSection> {
   public AnimationMetadataSection fromJson(JsonObject p_119064_) {
      Builder<AnimationFrame> $$1 = ImmutableList.builder();
      int $$2 = GsonHelper.getAsInt(p_119064_, "frametime", 1);
      if ($$2 != 1) {
         Validate.inclusiveBetween(1L, 2147483647L, $$2, "Invalid default frame time");
      }

      if (p_119064_.has("frames")) {
         try {
            JsonArray $$3 = GsonHelper.getAsJsonArray(p_119064_, "frames");

            for (int $$4 = 0; $$4 < $$3.size(); $$4++) {
               JsonElement $$5 = $$3.get($$4);
               AnimationFrame $$6 = this.getFrame($$4, $$5);
               if ($$6 != null) {
                  $$1.add($$6);
               }
            }
         } catch (ClassCastException var8) {
            throw new JsonParseException("Invalid animation->frames: expected array, was " + p_119064_.get("frames"), var8);
         }
      }

      int $$8 = GsonHelper.getAsInt(p_119064_, "width", -1);
      int $$9 = GsonHelper.getAsInt(p_119064_, "height", -1);
      if ($$8 != -1) {
         Validate.inclusiveBetween(1L, 2147483647L, $$8, "Invalid width");
      }

      if ($$9 != -1) {
         Validate.inclusiveBetween(1L, 2147483647L, $$9, "Invalid height");
      }

      boolean $$10 = GsonHelper.getAsBoolean(p_119064_, "interpolate", false);
      return new AnimationMetadataSection($$1.build(), $$8, $$9, $$2, $$10);
   }

   @Nullable
   private AnimationFrame getFrame(int p_119059_, JsonElement p_119060_) {
      if (p_119060_.isJsonPrimitive()) {
         return new AnimationFrame(GsonHelper.convertToInt(p_119060_, "frames[" + p_119059_ + "]"));
      } else if (p_119060_.isJsonObject()) {
         JsonObject $$2 = GsonHelper.convertToJsonObject(p_119060_, "frames[" + p_119059_ + "]");
         int $$3 = GsonHelper.getAsInt($$2, "time", -1);
         if ($$2.has("time")) {
            Validate.inclusiveBetween(1L, 2147483647L, $$3, "Invalid frame time");
         }

         int $$4 = GsonHelper.getAsInt($$2, "index");
         Validate.inclusiveBetween(0L, 2147483647L, $$4, "Invalid frame index");
         return new AnimationFrame($$4, $$3);
      } else {
         return null;
      }
   }

   @Override
   public String getMetadataSectionName() {
      return "animation";
   }
}
