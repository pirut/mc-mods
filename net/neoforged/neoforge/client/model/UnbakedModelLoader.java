package net.neoforged.neoforge.client.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.client.resources.model.UnbakedModel;

public interface UnbakedModelLoader<T extends UnbakedModel> {
   T read(JsonObject var1, JsonDeserializationContext var2) throws JsonParseException;
}
