package net.neoforged.neoforge.client.model;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.mojang.datafixers.util.Either;
import java.util.Map.Entry;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.UnbakedGeometry;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.Identifier;

public class CompositeUnbakedModel extends AbstractUnbakedModel {
   private final CompositeUnbakedGeometry geometry;

   public CompositeUnbakedModel(StandardModelParameters parameters, CompositeUnbakedGeometry geometry) {
      super(parameters);
      this.geometry = geometry;
   }

   @Override
   public UnbakedGeometry geometry() {
      return this.geometry;
   }

   @Override
   public void resolveDependencies(ResolvableModel.Resolver resolver) {
      UnmodifiableIterator var2 = this.geometry.children.values().iterator();

      while (var2.hasNext()) {
         Either<Identifier, UnbakedModel> child = (Either<Identifier, UnbakedModel>)var2.next();
         child.ifLeft(resolver::markDependency).ifRight(model -> {
            Identifier parent = model.parent();
            if (parent != null) {
               resolver.markDependency(parent);
            }

            model.resolveDependencies(resolver);
         });
      }
   }

   public static final class Loader implements UnbakedModelLoader<CompositeUnbakedModel> {
      public static final CompositeUnbakedModel.Loader INSTANCE = new CompositeUnbakedModel.Loader();

      private Loader() {
      }

      public CompositeUnbakedModel read(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
         Builder<String, Either<Identifier, UnbakedModel>> childrenBuilder = ImmutableMap.builder();
         readChildren(jsonObject, "children", childrenBuilder, jsonDeserializationContext);
         ImmutableMap<String, Either<Identifier, UnbakedModel>> children = childrenBuilder.build();
         if (children.isEmpty()) {
            throw new JsonParseException("Composite model requires a \"children\" element with at least one element.");
         } else {
            StandardModelParameters parameters = StandardModelParameters.parse(jsonObject, jsonDeserializationContext);
            return new CompositeUnbakedModel(parameters, new CompositeUnbakedGeometry(children));
         }
      }

      private static void readChildren(
         JsonObject jsonObject, String name, Builder<String, Either<Identifier, UnbakedModel>> children, JsonDeserializationContext context
      ) {
         if (jsonObject.has(name)) {
            JsonObject childrenJsonObject = jsonObject.getAsJsonObject(name);

            for (Entry<String, JsonElement> entry : childrenJsonObject.entrySet()) {
               Either<Identifier, UnbakedModel> child = switch ((JsonElement)entry.getValue()) {
                  case JsonPrimitive reference -> Either.left(Identifier.parse(reference.getAsString()));
                  case JsonObject inline -> Either.right((UnbakedModel)context.deserialize(inline, UnbakedModel.class));
                  default -> throw new IllegalArgumentException("");
               };
               children.put(entry.getKey(), child);
            }
         }
      }
   }
}
