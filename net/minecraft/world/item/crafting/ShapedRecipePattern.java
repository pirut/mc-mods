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
   @Deprecated
   private static final int MAX_SIZE = 3;
   public static final char EMPTY_SLOT = ' ';
   static int maxWidth = 3;
   static int maxHeight = 3;
   public static final MapCodec<ShapedRecipePattern> MAP_CODEC = ShapedRecipePattern.Data.MAP_CODEC
      .flatXmap(
         ShapedRecipePattern::unpack,
         p_344423_ -> p_344423_.data.<DataResult>map(DataResult::success).orElseGet(() -> DataResult.error(() -> "Cannot encode unpacked recipe"))
      );
   public static final StreamCodec<RegistryFriendlyByteBuf, ShapedRecipePattern> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      p_360067_ -> p_360067_.width,
      ByteBufCodecs.VAR_INT,
      p_360066_ -> p_360066_.height,
      Ingredient.OPTIONAL_CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()),
      p_360065_ -> p_360065_.ingredients,
      ShapedRecipePattern::createFromNetwork
   );
   private final int width;
   private final int height;
   private final List<Optional<Ingredient>> ingredients;
   private final Optional<ShapedRecipePattern.Data> data;
   private final int ingredientCount;
   private final boolean symmetrical;

   public static int getMaxWidth() {
      return maxWidth;
   }

   public static int getMaxHeight() {
      return maxHeight;
   }

   public static void setCraftingSize(int width, int height) {
      if (maxWidth < width) {
         maxWidth = width;
      }

      if (maxHeight < height) {
         maxHeight = height;
      }
   }

   public ShapedRecipePattern(int p_311959_, int p_312714_, List<Optional<Ingredient>> p_360908_, Optional<ShapedRecipePattern.Data> p_312427_) {
      this.width = p_311959_;
      this.height = p_312714_;
      this.ingredients = p_360908_;
      this.data = p_312427_;
      this.ingredientCount = (int)p_360908_.stream().flatMap(Optional::stream).count();
      this.symmetrical = Util.isSymmetrical(p_311959_, p_312714_, p_360908_);
   }

   private static ShapedRecipePattern createFromNetwork(Integer p_363312_, Integer p_363884_, List<Optional<Ingredient>> p_364060_) {
      return new ShapedRecipePattern(p_363312_, p_363884_, p_364060_, Optional.empty());
   }

   public static ShapedRecipePattern of(Map<Character, Ingredient> p_312851_, String... p_312645_) {
      return of(p_312851_, List.of(p_312645_));
   }

   public static ShapedRecipePattern of(Map<Character, Ingredient> p_312370_, List<String> p_312701_) {
      ShapedRecipePattern.Data shapedrecipepattern$data = new ShapedRecipePattern.Data(p_312370_, p_312701_);
      return (ShapedRecipePattern)unpack(shapedrecipepattern$data).getOrThrow();
   }

   private static DataResult<ShapedRecipePattern> unpack(ShapedRecipePattern.Data p_312037_) {
      String[] astring = shrink(p_312037_.pattern);
      int i = astring[0].length();
      int j = astring.length;
      List<Optional<Ingredient>> list = new ArrayList<>(i * j);
      CharSet charset = new CharArraySet(p_312037_.key.keySet());

      for (String s : astring) {
         for (int k = 0; k < s.length(); k++) {
            char c0 = s.charAt(k);
            Optional<Ingredient> optional;
            if (c0 == ' ') {
               optional = Optional.empty();
            } else {
               Ingredient ingredient = p_312037_.key.get(c0);
               if (ingredient == null) {
                  return DataResult.error(() -> "Pattern references symbol '" + c0 + "' but it's not defined in the key");
               }

               optional = Optional.of(ingredient);
            }

            charset.remove(c0);
            list.add(optional);
         }
      }

      return !charset.isEmpty()
         ? DataResult.error(() -> "Key defines symbols that aren't used in pattern: " + charset)
         : DataResult.success(new ShapedRecipePattern(i, j, list, Optional.of(p_312037_)));
   }

   @VisibleForTesting
   static String[] shrink(List<String> p_311893_) {
      int i = Integer.MAX_VALUE;
      int j = 0;
      int k = 0;
      int l = 0;

      for (int i1 = 0; i1 < p_311893_.size(); i1++) {
         String s = p_311893_.get(i1);
         i = Math.min(i, firstNonEmpty(s));
         int j1 = lastNonEmpty(s);
         j = Math.max(j, j1);
         if (j1 < 0) {
            if (k == i1) {
               k++;
            }

            l++;
         } else {
            l = 0;
         }
      }

      if (p_311893_.size() == l) {
         return new String[0];
      } else {
         String[] astring = new String[p_311893_.size() - l - k];

         for (int k1 = 0; k1 < astring.length; k1++) {
            astring[k1] = p_311893_.get(k1 + k).substring(i, j + 1);
         }

         return astring;
      }
   }

   private static int firstNonEmpty(String p_312343_) {
      int i = 0;

      while (i < p_312343_.length() && p_312343_.charAt(i) == ' ') {
         i++;
      }

      return i;
   }

   private static int lastNonEmpty(String p_311944_) {
      int i = p_311944_.length() - 1;

      while (i >= 0 && p_311944_.charAt(i) == ' ') {
         i--;
      }

      return i;
   }

   public boolean matches(CraftingInput p_345063_) {
      if (p_345063_.ingredientCount() != this.ingredientCount) {
         return false;
      } else {
         if (p_345063_.width() == this.width && p_345063_.height() == this.height) {
            if (!this.symmetrical && this.matches(p_345063_, true)) {
               return true;
            }

            if (this.matches(p_345063_, false)) {
               return true;
            }
         }

         return false;
      }
   }

   private boolean matches(CraftingInput p_345835_, boolean p_344990_) {
      for (int i = 0; i < this.height; i++) {
         for (int j = 0; j < this.width; j++) {
            Optional<Ingredient> optional;
            if (p_344990_) {
               optional = this.ingredients.get(this.width - j - 1 + i * this.width);
            } else {
               optional = this.ingredients.get(j + i * this.width);
            }

            ItemStack itemstack = p_345835_.getItem(j, i);
            if (!Ingredient.testOptionalIngredient(optional, itemstack)) {
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
      private static final Codec<List<String>> PATTERN_CODEC = Codec.STRING.listOf().comapFlatMap(p_312085_ -> {
         if (p_312085_.size() > ShapedRecipePattern.maxHeight) {
            return DataResult.error(() -> "Invalid pattern: too many rows, %s is maximum".formatted(ShapedRecipePattern.maxHeight));
         } else if (p_312085_.isEmpty()) {
            return DataResult.error(() -> "Invalid pattern: empty pattern not allowed");
         } else {
            int i = ((String)p_312085_.getFirst()).length();

            for (String s : p_312085_) {
               if (s.length() > ShapedRecipePattern.maxWidth) {
                  return DataResult.error(() -> "Invalid pattern: too many columns, %s is maximum".formatted(ShapedRecipePattern.maxWidth));
               }

               if (i != s.length()) {
                  return DataResult.error(() -> "Invalid pattern: each row must be the same width");
               }
            }

            return DataResult.success(p_312085_);
         }
      }, Function.identity());
      private static final Codec<Character> SYMBOL_CODEC = Codec.STRING.comapFlatMap(p_312250_ -> {
         if (p_312250_.length() != 1) {
            return DataResult.error(() -> "Invalid key entry: '" + p_312250_ + "' is an invalid symbol (must be 1 character only).");
         } else {
            return " ".equals(p_312250_) ? DataResult.error(() -> "Invalid key entry: ' ' is a reserved symbol.") : DataResult.success(p_312250_.charAt(0));
         }
      }, String::valueOf);
      public static final MapCodec<ShapedRecipePattern.Data> MAP_CODEC = RecordCodecBuilder.mapCodec(
         p_360068_ -> p_360068_.group(
               ExtraCodecs.strictUnboundedMap(SYMBOL_CODEC, Ingredient.CODEC).fieldOf("key").forGetter(p_312509_ -> p_312509_.key),
               PATTERN_CODEC.fieldOf("pattern").forGetter(p_312713_ -> p_312713_.pattern)
            )
            .apply(p_360068_, ShapedRecipePattern.Data::new)
      );
   }
}
