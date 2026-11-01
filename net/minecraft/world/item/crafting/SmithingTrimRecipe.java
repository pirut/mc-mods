package net.minecraft.world.item.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SmithingRecipeDisplay;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimMaterials;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import org.jspecify.annotations.Nullable;

public class SmithingTrimRecipe implements SmithingRecipe {
   final Ingredient template;
   final Ingredient base;
   final Ingredient addition;
   final Holder<TrimPattern> pattern;
   @Nullable
   private PlacementInfo placementInfo;

   public SmithingTrimRecipe(Ingredient p_399501_, Ingredient p_399626_, Ingredient p_399756_, Holder<TrimPattern> p_399526_) {
      this.template = p_399501_;
      this.base = p_399626_;
      this.addition = p_399756_;
      this.pattern = p_399526_;
   }

   public ItemStack assemble(SmithingRecipeInput p_345750_, HolderLookup.Provider p_335536_) {
      return applyTrim(p_335536_, p_345750_.base(), p_345750_.addition(), this.pattern);
   }

   public static ItemStack applyTrim(HolderLookup.Provider p_381047_, ItemStack p_381169_, ItemStack p_381102_, Holder<TrimPattern> p_399470_) {
      Optional<Holder<TrimMaterial>> optional = TrimMaterials.getFromIngredient(p_381047_, p_381102_);
      if (optional.isPresent()) {
         ArmorTrim armortrim = p_381169_.get(DataComponents.TRIM);
         ArmorTrim armortrim1 = new ArmorTrim(optional.get(), p_399470_);
         if (Objects.equals(armortrim, armortrim1)) {
            return ItemStack.EMPTY;
         } else {
            ItemStack itemstack = p_381169_.copyWithCount(1);
            itemstack.set(DataComponents.TRIM, armortrim1);
            return itemstack;
         }
      } else {
         return ItemStack.EMPTY;
      }
   }

   @Override
   public Optional<Ingredient> templateIngredient() {
      return Optional.of(this.template);
   }

   @Override
   public Ingredient baseIngredient() {
      return this.base;
   }

   @Override
   public Optional<Ingredient> additionIngredient() {
      return Optional.of(this.addition);
   }

   @Override
   public RecipeSerializer<SmithingTrimRecipe> getSerializer() {
      return RecipeSerializer.SMITHING_TRIM;
   }

   @Override
   public PlacementInfo placementInfo() {
      if (this.placementInfo == null) {
         this.placementInfo = PlacementInfo.create(List.of(this.template, this.base, this.addition));
      }

      return this.placementInfo;
   }

   @Override
   public List<RecipeDisplay> display() {
      SlotDisplay slotdisplay = this.base.display();
      SlotDisplay slotdisplay1 = this.addition.display();
      SlotDisplay slotdisplay2 = this.template.display();
      return List.of(
         new SmithingRecipeDisplay(
            slotdisplay2,
            slotdisplay,
            slotdisplay1,
            new SlotDisplay.SmithingTrimDemoSlotDisplay(slotdisplay, slotdisplay1, this.pattern),
            new SlotDisplay.ItemSlotDisplay(Items.SMITHING_TABLE)
         )
      );
   }

   public static class Serializer implements RecipeSerializer<SmithingTrimRecipe> {
      private static final MapCodec<SmithingTrimRecipe> CODEC = RecordCodecBuilder.mapCodec(
         p_399429_ -> p_399429_.group(
               Ingredient.CODEC.fieldOf("template").forGetter(p_399421_ -> p_399421_.template),
               Ingredient.CODEC.fieldOf("base").forGetter(p_399423_ -> p_399423_.base),
               Ingredient.CODEC.fieldOf("addition").forGetter(p_399428_ -> p_399428_.addition),
               TrimPattern.CODEC.fieldOf("pattern").forGetter(p_399425_ -> p_399425_.pattern)
            )
            .apply(p_399429_, SmithingTrimRecipe::new)
      );
      public static final StreamCodec<RegistryFriendlyByteBuf, SmithingTrimRecipe> STREAM_CODEC = StreamCodec.composite(
         Ingredient.CONTENTS_STREAM_CODEC,
         p_399427_ -> p_399427_.template,
         Ingredient.CONTENTS_STREAM_CODEC,
         p_399424_ -> p_399424_.base,
         Ingredient.CONTENTS_STREAM_CODEC,
         p_399422_ -> p_399422_.addition,
         TrimPattern.STREAM_CODEC,
         p_399426_ -> p_399426_.pattern,
         SmithingTrimRecipe::new
      );

      @Override
      public MapCodec<SmithingTrimRecipe> codec() {
         return CODEC;
      }

      @Override
      public StreamCodec<RegistryFriendlyByteBuf, SmithingTrimRecipe> streamCodec() {
         return STREAM_CODEC;
      }
   }
}
