package net.neoforged.neoforge.client.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import net.minecraft.client.resources.model.UnbakedGeometry;
import net.minecraft.client.resources.model.UnbakedModel;

public class EmptyModel implements UnbakedModel, JsonDeserializer<EmptyModel> {
   public static final EmptyModel INSTANCE = new EmptyModel();
   public static final UnbakedModelLoader<EmptyModel> LOADER = (object, context) -> INSTANCE;

   private EmptyModel() {
   }

   public EmptyModel deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
      return INSTANCE;
   }

   @Override
   public UnbakedGeometry geometry() {
      return UnbakedGeometry.EMPTY;
   }
}
