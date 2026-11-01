package net.minecraft.client.renderer.block.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.item.ItemDisplayContext;

@Environment(EnvType.CLIENT)
public record ItemTransforms(
   ItemTransform thirdPersonLeftHand,
   ItemTransform thirdPersonRightHand,
   ItemTransform firstPersonLeftHand,
   ItemTransform firstPersonRightHand,
   ItemTransform head,
   ItemTransform gui,
   ItemTransform ground,
   ItemTransform fixed,
   ItemTransform fixedFromBottom
) {
   public static final ItemTransforms NO_TRANSFORMS = new ItemTransforms(
      ItemTransform.NO_TRANSFORM,
      ItemTransform.NO_TRANSFORM,
      ItemTransform.NO_TRANSFORM,
      ItemTransform.NO_TRANSFORM,
      ItemTransform.NO_TRANSFORM,
      ItemTransform.NO_TRANSFORM,
      ItemTransform.NO_TRANSFORM,
      ItemTransform.NO_TRANSFORM,
      ItemTransform.NO_TRANSFORM
   );

   public ItemTransform getTransform(ItemDisplayContext displayContext) {
      return switch (displayContext) {
         case THIRD_PERSON_LEFT_HAND -> this.thirdPersonLeftHand;
         case THIRD_PERSON_RIGHT_HAND -> this.thirdPersonRightHand;
         case FIRST_PERSON_LEFT_HAND -> this.firstPersonLeftHand;
         case FIRST_PERSON_RIGHT_HAND -> this.firstPersonRightHand;
         case HEAD -> this.head;
         case GUI -> this.gui;
         case GROUND -> this.ground;
         case FIXED -> this.fixed;
         case ON_SHELF -> this.fixedFromBottom;
         default -> ItemTransform.NO_TRANSFORM;
      };
   }

   @Environment(EnvType.CLIENT)
   protected static class Deserializer implements JsonDeserializer<ItemTransforms> {
      public ItemTransforms deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
         JsonObject jsonObject = json.getAsJsonObject();
         ItemTransform itemTransform = this.getTransform(context, jsonObject, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND);
         ItemTransform itemTransform2 = this.getTransform(context, jsonObject, ItemDisplayContext.THIRD_PERSON_LEFT_HAND);
         if (itemTransform2 == ItemTransform.NO_TRANSFORM) {
            itemTransform2 = itemTransform;
         }

         ItemTransform itemTransform3 = this.getTransform(context, jsonObject, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND);
         ItemTransform itemTransform4 = this.getTransform(context, jsonObject, ItemDisplayContext.FIRST_PERSON_LEFT_HAND);
         if (itemTransform4 == ItemTransform.NO_TRANSFORM) {
            itemTransform4 = itemTransform3;
         }

         ItemTransform itemTransform5 = this.getTransform(context, jsonObject, ItemDisplayContext.HEAD);
         ItemTransform itemTransform6 = this.getTransform(context, jsonObject, ItemDisplayContext.GUI);
         ItemTransform itemTransform7 = this.getTransform(context, jsonObject, ItemDisplayContext.GROUND);
         ItemTransform itemTransform8 = this.getTransform(context, jsonObject, ItemDisplayContext.FIXED);
         ItemTransform itemTransform9 = this.getTransform(context, jsonObject, ItemDisplayContext.ON_SHELF);
         return new ItemTransforms(
            itemTransform2, itemTransform, itemTransform4, itemTransform3, itemTransform5, itemTransform6, itemTransform7, itemTransform8, itemTransform9
         );
      }

      private ItemTransform getTransform(JsonDeserializationContext deserializationContext, JsonObject json, ItemDisplayContext displayContext) {
         String string = displayContext.getSerializedName();
         return json.has(string) ? (ItemTransform)deserializationContext.deserialize(json.get(string), ItemTransform.class) : ItemTransform.NO_TRANSFORM;
      }
   }
}
