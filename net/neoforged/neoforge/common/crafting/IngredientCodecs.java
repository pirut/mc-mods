package net.neoforged.neoforge.common.crafting;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public class IngredientCodecs {
   private static final int CUSTOM_INGREDIENT_MARKER = -1000;
   private static final StreamCodec<RegistryFriendlyByteBuf, ICustomIngredient> CUSTOM_INGREDIENT_CODEC = ByteBufCodecs.registry(
         NeoForgeRegistries.Keys.INGREDIENT_TYPES
      )
      .dispatch(ICustomIngredient::getType, IngredientType::streamCodec);

   public static Codec<Ingredient> codec(Codec<Ingredient> vanillaCodec) {
      Codec<ICustomIngredient> customIngredientCodec = NeoForgeRegistries.INGREDIENT_TYPES
         .byNameCodec()
         .dispatch("neoforge:ingredient_type", ICustomIngredient::getType, IngredientType::codec);
      return Codec.xor(customIngredientCodec, vanillaCodec)
         .xmap(
            either -> (Ingredient)either.map(ICustomIngredient::toVanilla, i -> i),
            ingredient -> ingredient.isCustom() ? Either.left(ingredient.getCustomIngredient()) : Either.right(ingredient)
         );
   }

   public static StreamCodec<RegistryFriendlyByteBuf, Ingredient> streamCodec(final StreamCodec<RegistryFriendlyByteBuf, Ingredient> vanillaCodec) {
      return new StreamCodec<RegistryFriendlyByteBuf, Ingredient>() {
         public Ingredient decode(RegistryFriendlyByteBuf buf) {
            int readerIndex = buf.readerIndex();
            int length = buf.readVarInt();
            if (length == -1000) {
               return IngredientCodecs.CUSTOM_INGREDIENT_CODEC.decode(buf).toVanilla();
            } else {
               buf.readerIndex(readerIndex);
               return vanillaCodec.decode(buf);
            }
         }

         public void encode(RegistryFriendlyByteBuf buf, Ingredient ingredient) {
            if (ingredient.isCustom() && buf.getConnectionType().isNeoForge()) {
               buf.writeVarInt(-1000);
               IngredientCodecs.CUSTOM_INGREDIENT_CODEC.encode(buf, ingredient.getCustomIngredient());
            } else {
               vanillaCodec.encode(buf, ingredient);
            }
         }
      };
   }

   public static StreamCodec<RegistryFriendlyByteBuf, Optional<Ingredient>> optionalStreamCodec(
      final StreamCodec<RegistryFriendlyByteBuf, Optional<Ingredient>> vanillaCodec
   ) {
      return new StreamCodec<RegistryFriendlyByteBuf, Optional<Ingredient>>() {
         public Optional<Ingredient> decode(RegistryFriendlyByteBuf buf) {
            int readerIndex = buf.readerIndex();
            int length = buf.readVarInt();
            if (length == -1000) {
               return Optional.of(IngredientCodecs.CUSTOM_INGREDIENT_CODEC.decode(buf).toVanilla());
            } else {
               buf.readerIndex(readerIndex);
               return vanillaCodec.decode(buf);
            }
         }

         public void encode(RegistryFriendlyByteBuf buf, Optional<Ingredient> ingredient) {
            if (ingredient.isPresent() && ingredient.get().isCustom() && buf.getConnectionType().isNeoForge()) {
               buf.writeVarInt(-1000);
               IngredientCodecs.CUSTOM_INGREDIENT_CODEC.encode(buf, ingredient.get().getCustomIngredient());
            } else {
               vanillaCodec.encode(buf, ingredient);
            }
         }
      };
   }
}
