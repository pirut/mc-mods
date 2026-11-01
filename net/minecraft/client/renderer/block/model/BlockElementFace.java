package net.minecraft.client.renderer.block.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.math.Quadrant;
import java.lang.reflect.Type;
import net.minecraft.core.Direction;
import net.minecraft.util.GsonHelper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.ExtraFaceData;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record BlockElementFace(
   @Nullable Direction cullForDirection,
   int tintIndex,
   String texture,
   @Nullable BlockElementFace.UVs uvs,
   Quadrant rotation,
   @Nullable ExtraFaceData faceData,
   MutableObject<BlockElement> parent
) {
   public static final int NO_TINT = -1;

   public BlockElementFace(@Nullable Direction cullForDirection, int tintIndex, String texture, @Nullable BlockElementFace.UVs uvs, Quadrant rotation) {
      this(cullForDirection, tintIndex, texture, uvs, rotation, null, new MutableObject());
   }

   public static float getU(BlockElementFace.UVs p_405199_, Quadrant p_404689_, int p_404785_) {
      return p_405199_.getVertexU(p_404689_.rotateVertexIndex(p_404785_)) / 16.0F;
   }

   public static float getV(BlockElementFace.UVs p_405500_, Quadrant p_404709_, int p_405113_) {
      return p_405500_.getVertexV(p_404709_.rotateVertexIndex(p_405113_)) / 16.0F;
   }

   public ExtraFaceData faceData() {
      if (this.faceData != null) {
         return this.faceData;
      } else {
         return this.parent.getValue() != null ? ((BlockElement)this.parent.getValue()).faceData() : ExtraFaceData.DEFAULT;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class Deserializer implements JsonDeserializer<BlockElementFace> {
      private static final int DEFAULT_TINT_INDEX = -1;
      private static final int DEFAULT_ROTATION = 0;

      public BlockElementFace deserialize(JsonElement p_111365_, Type p_111366_, JsonDeserializationContext p_111367_) throws JsonParseException {
         JsonObject jsonobject = p_111365_.getAsJsonObject();
         Direction direction = getCullFacing(jsonobject);
         int i = getTintIndex(jsonobject);
         String s = getTexture(jsonobject);
         BlockElementFace.UVs blockelementface$uvs = getUVs(jsonobject);
         Quadrant quadrant = getRotation(jsonobject);
         if (jsonobject.has("forge_data")) {
            throw new JsonParseException("forge_data should be replaced by neoforge_data");
         } else {
            ExtraFaceData faceData = ExtraFaceData.read(jsonobject.get("neoforge_data"), null);
            return new BlockElementFace(direction, i, s, blockelementface$uvs, quadrant, faceData, new MutableObject());
         }
      }

      private static int getTintIndex(JsonObject p_111369_) {
         return GsonHelper.getAsInt(p_111369_, "tintindex", -1);
      }

      private static String getTexture(JsonObject p_111371_) {
         return GsonHelper.getAsString(p_111371_, "texture");
      }

      @Nullable
      private static Direction getCullFacing(JsonObject p_111373_) {
         String s = GsonHelper.getAsString(p_111373_, "cullface", "");
         return Direction.byName(s);
      }

      private static Quadrant getRotation(JsonObject p_404961_) {
         int i = GsonHelper.getAsInt(p_404961_, "rotation", 0);
         return Quadrant.parseJson(i);
      }

      @Nullable
      private static BlockElementFace.UVs getUVs(JsonObject p_405376_) {
         if (!p_405376_.has("uv")) {
            return null;
         } else {
            JsonArray jsonarray = GsonHelper.getAsJsonArray(p_405376_, "uv");
            if (jsonarray.size() != 4) {
               throw new JsonParseException("Expected 4 uv values, found: " + jsonarray.size());
            } else {
               float f = GsonHelper.convertToFloat(jsonarray.get(0), "minU");
               float f1 = GsonHelper.convertToFloat(jsonarray.get(1), "minV");
               float f2 = GsonHelper.convertToFloat(jsonarray.get(2), "maxU");
               float f3 = GsonHelper.convertToFloat(jsonarray.get(3), "maxV");
               return new BlockElementFace.UVs(f, f1, f2, f3);
            }
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   public record UVs(float minU, float minV, float maxU, float maxV) {
      public float getVertexU(int p_405872_) {
         return p_405872_ != 0 && p_405872_ != 1 ? this.maxU : this.minU;
      }

      public float getVertexV(int p_405480_) {
         return p_405480_ != 0 && p_405480_ != 3 ? this.maxV : this.minV;
      }
   }
}
