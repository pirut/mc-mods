package net.neoforged.neoforge.common.crafting;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.util.NeoForgeExtraCodecs;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public class CraftingHelper {
   public static Codec<Ingredient> makeIngredientCodec(boolean allowEmpty) {
      Codec<List<Ingredient>> listCodec = Codec.lazyInitialized(() -> allowEmpty ? Ingredient.LIST_CODEC : Ingredient.LIST_CODEC_NONEMPTY);
      return Codec.either(listCodec, makeIngredientMapCodec().codec())
         .xmap(either -> (Ingredient)either.map(list -> CompoundIngredient.of(list.toArray(Ingredient[]::new)), i -> i), ingredient -> {
            if (ingredient.isCustom()) {
               if (ingredient.getCustomIngredient() instanceof CompoundIngredient compound) {
                  return Either.left(compound.children());
               }
            } else if (ingredient.getValues().length != 1) {
               return Either.left(Stream.of(ingredient.getValues()).map(v -> Ingredient.fromValues(Stream.of((Ingredient.Value)v))).toList());
            }

            return Either.right(ingredient);
         });
   }

   public static MapCodec<Ingredient> makeIngredientMapCodec() {
      return NeoForgeExtraCodecs.dispatchMapOrElse(
            NeoForgeRegistries.INGREDIENT_TYPES.byNameCodec(), ICustomIngredient::getType, IngredientType::codec, Ingredient.Value.MAP_CODEC
         )
         .xmap(
            either -> (Ingredient)either.map(ICustomIngredient::toVanilla, v -> Ingredient.fromValues(Stream.of(v))),
            ingredient -> {
               if (!ingredient.isCustom()) {
                  Ingredient.Value[] values = ingredient.getValues();
                  return values.length == 1
                     ? Either.right(values[0])
                     : Either.left(
                        new CompoundIngredient(Stream.of(ingredient.getValues()).map(v -> Ingredient.fromValues(Stream.of((Ingredient.Value)v))).toList())
                     );
               } else {
                  return Either.left(ingredient.getCustomIngredient());
               }
            }
         )
         .validate(
            ingredient -> !ingredient.isCustom() && ingredient.getValues().length == 0
               ? DataResult.error(() -> "Cannot serialize empty ingredient using the map codec")
               : DataResult.success(ingredient)
         );
   }
}
