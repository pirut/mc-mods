package net.minecraft.world.item.crafting;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.chars.CharArraySet;
import it.unimi.dsi.fastutil.chars.CharSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;

public final class ShapedRecipePattern {
   private static final int MAX_SIZE = 3;
   public static final char EMPTY_SLOT = ' ';
   public static final MapCodec<ShapedRecipePattern> MAP_CODEC = ShapedRecipePattern.Data.MAP_CODEC
      .flatXmap(
         ShapedRecipePattern::unpack,
         shapedRecipePattern -> shapedRecipePattern.data
            .<DataResult>map(DataResult::success)
            .orElseGet(() -> DataResult.error(() -> "Cannot encode unpacked recipe"))
      );
   public static final StreamCodec<RegistryFriendlyByteBuf, ShapedRecipePattern> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      shapedRecipePattern -> shapedRecipePattern.width,
      ByteBufCodecs.VAR_INT,
      shapedRecipePattern -> shapedRecipePattern.height,
      Ingredient.OPTIONAL_CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()),
      shapedRecipePattern -> shapedRecipePattern.ingredients,
      ShapedRecipePattern::createFromNetwork
   );
   private final int width;
   private final int height;
   private final List<Optional<Ingredient>> ingredients;
   private final Optional<ShapedRecipePattern.Data> data;
   private final int ingredientCount;
   private final boolean symmetrical;

   public ShapedRecipePattern(int width, int height, List<Optional<Ingredient>> ingredients, Optional<ShapedRecipePattern.Data> data) {
      this.width = width;
      this.height = height;
      this.ingredients = ingredients;
      this.data = data;
      this.ingredientCount = (int)ingredients.stream().flatMap(Optional::stream).count();
      this.symmetrical = Util.isSymmetrical(width, height, ingredients);
   }

   private static ShapedRecipePattern createFromNetwork(Integer width, Integer height, List<Optional<Ingredient>> ingredients) {
      return new ShapedRecipePattern(width, height, ingredients, Optional.empty());
   }

   public static ShapedRecipePattern of(Map<Character, Ingredient> key, String... pattern) {
      return of(key, List.of(pattern));
   }

   public static ShapedRecipePattern of(Map<Character, Ingredient> key, List<String> pattern) {
      ShapedRecipePattern.Data data = new ShapedRecipePattern.Data(key, pattern);
      return (ShapedRecipePattern)unpack(data).getOrThrow();
   }

   private static DataResult<ShapedRecipePattern> unpack(ShapedRecipePattern.Data data) {
      String[] strings = shrink(data.pattern);
      int i = strings[0].length();
      int j = strings.length;
      List<Optional<Ingredient>> list = new ArrayList<>(i * j);
      CharSet charSet = new CharArraySet(data.key.keySet());

      for (String string : strings) {
         for (int k = 0; k < string.length(); k++) {
            char c = string.charAt(k);
            Optional<Ingredient> optional;
            if (c == ' ') {
               optional = Optional.empty();
            } else {
               Ingredient ingredient = data.key.get(c);
               if (ingredient == null) {
                  return DataResult.error(() -> "Pattern references symbol '" + c + "' but it's not defined in the key");
               }

               optional = Optional.of(ingredient);
            }

            charSet.remove(c);
            list.add(optional);
         }
      }

      return !charSet.isEmpty()
         ? DataResult.error(() -> "Key defines symbols that aren't used in pattern: " + charSet)
         : DataResult.success(new ShapedRecipePattern(i, j, list, Optional.of(data)));
   }

   @VisibleForTesting
   static String[] shrink(List<String> pattern) {
      int i = Integer.MAX_VALUE;
      int j = 0;
      int k = 0;
      int l = 0;

      for (int m = 0; m < pattern.size(); m++) {
         String string = pattern.get(m);
         i = Math.min(i, firstNonEmpty(string));
         int n = lastNonEmpty(string);
         j = Math.max(j, n);
         if (n < 0) {
            if (k == m) {
               k++;
            }

            l++;
         } else {
            l = 0;
         }
      }

      if (pattern.size() == l) {
         return new String[0];
      } else {
         String[] strings = new String[pattern.size() - l - k];

         for (int o = 0; o < strings.length; o++) {
            strings[o] = pattern.get(o + k).substring(i, j + 1);
         }

         return strings;
      }
   }

   private static int firstNonEmpty(String row) {
      int i = 0;

      while (i < row.length() && row.charAt(i) == ' ') {
         i++;
      }

      return i;
   }

   private static int lastNonEmpty(String row) {
      int i = row.length() - 1;

      while (i >= 0 && row.charAt(i) == ' ') {
         i--;
      }

      return i;
   }

   public boolean matches(CraftingInput input) {
      if (input.ingredientCount() != this.ingredientCount) {
         return false;
      } else {
         if (input.width() == this.width && input.height() == this.height) {
            if (!this.symmetrical && this.matches(input, true)) {
               return true;
            }

            if (this.matches(input, false)) {
               return true;
            }
         }

         return false;
      }
   }

   private boolean matches(CraftingInput input, boolean symmetrical) {
      for (int i = 0; i < this.height; i++) {
         for (int j = 0; j < this.width; j++) {
            Optional<Ingredient> optional;
            if (symmetrical) {
               optional = this.ingredients.get(this.width - j - 1 + i * this.width);
            } else {
               optional = this.ingredients.get(j + i * this.width);
            }

            ItemStack itemStack = input.getItem(j, i);
            if (!Ingredient.testOptionalIngredient(optional, itemStack)) {
               return false;
            }
         }
      }

      return true;
   }

   public int width() {
      return this.width;
   }

   public int height() {
      return this.height;
   }

   public List<Optional<Ingredient>> ingredients() {
      return this.ingredients;
   }

   public record Data(Map<Character, Ingredient> key, List<String> pattern) {
      private static final Codec<List<String>> PATTERN_CODEC = Codec.STRING.listOf().comapFlatMap(list -> {
         if (list.size() > 3) {
            return DataResult.error(() -> "Invalid pattern: too many rows, 3 is maximum");
         } else if (list.isEmpty()) {
            return DataResult.error(() -> "Invalid pattern: empty pattern not allowed");
         } else {
            int i = ((String)list.getFirst()).length();

            for (String string : list) {
               if (string.length() > 3) {
                  return DataResult.error(() -> "Invalid pattern: too many columns, 3 is maximum");
               }

               if (i != string.length()) {
                  return DataResult.error(() -> "Invalid pattern: each row must be the same width");
               }
            }

            return DataResult.success(list);
         }
      }, Function.identity());
      private static final Codec<Character> SYMBOL_CODEC = Codec.STRING.comapFlatMap(string -> {
         if (string.length() != 1) {
            return DataResult.error(() -> "Invalid key entry: '" + string + "' is an invalid symbol (must be 1 character only).");
         } else {
            return " ".equals(string) ? DataResult.error(() -> "Invalid key entry: ' ' is a reserved symbol.") : DataResult.success(string.charAt(0));
         }
      }, String::valueOf);
      public static final MapCodec<ShapedRecipePattern.Data> MAP_CODEC = RecordCodecBuilder.mapCodec(
         instance -> instance.group(
               ExtraCodecs.strictUnboundedMap(SYMBOL_CODEC, Ingredient.CODEC).fieldOf("key").forGetter(data -> data.key),
               PATTERN_CODEC.fieldOf("pattern").forGetter(data -> data.pattern)
            )
            .apply(instance, ShapedRecipePattern.Data::new)
      );
   }
}
