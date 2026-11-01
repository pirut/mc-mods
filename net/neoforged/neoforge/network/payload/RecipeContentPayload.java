package net.neoforged.neoforge.network.payload;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public record RecipeContentPayload(Set<RecipeType<?>> recipeTypes, List<RecipeHolder<?>> recipes) implements CustomPacketPayload {
   public static final CustomPacketPayload.Type<RecipeContentPayload> TYPE = new CustomPacketPayload.Type<>(
      Identifier.fromNamespaceAndPath("neoforge", "recipe_content")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, RecipeContentPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.registry(Registries.RECIPE_TYPE).apply(ByteBufCodecs.collection(HashSet::new)),
      RecipeContentPayload::recipeTypes,
      RecipeHolder.STREAM_CODEC.apply(ByteBufCodecs.list()),
      RecipeContentPayload::recipes,
      RecipeContentPayload::new
   );

   public static RecipeContentPayload create(Collection<RecipeType<?>> recipeTypes, RecipeMap recipes) {
      Set<RecipeType<?>> recipeTypeSet = Set.copyOf(recipeTypes);
      if (recipeTypeSet.isEmpty()) {
         return new RecipeContentPayload(recipeTypeSet, List.of());
      } else {
         List<RecipeHolder<?>> recipeSubset = recipes.values().stream().filter(h -> recipeTypeSet.contains(h.value().getType())).toList();
         return new RecipeContentPayload(recipeTypeSet, recipeSubset);
      }
   }

   @Override
   public CustomPacketPayload.Type<RecipeContentPayload> type() {
      return TYPE;
   }
}
