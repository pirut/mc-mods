package net.minecraft.stats;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.UnaryOperator;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.inventory.RecipeBookType;
import net.neoforged.neoforge.common.CommonHooks;

public final class RecipeBookSettings {
   public static final StreamCodec<RegistryFriendlyByteBuf, RecipeBookSettings> STREAM_CODEC = StreamCodec.composite(
      RecipeBookSettings.TypeSettings.STREAM_CODEC,
      p_421342_ -> p_421342_.crafting,
      RecipeBookSettings.TypeSettings.STREAM_CODEC,
      p_421338_ -> p_421338_.furnace,
      RecipeBookSettings.TypeSettings.STREAM_CODEC,
      p_421337_ -> p_421337_.blastFurnace,
      RecipeBookSettings.TypeSettings.STREAM_CODEC,
      p_421340_ -> p_421340_.smoker,
      CommonHooks.MODDED_RECIPE_BOOK_TYPES_SETTINGS_STREAM_CODEC,
      settings -> settings.moddedSettings,
      RecipeBookSettings::new
   );
   public static final MapCodec<RecipeBookSettings> MAP_CODEC = RecordCodecBuilder.mapCodec(
      p_421339_ -> p_421339_.group(
            RecipeBookSettings.TypeSettings.CRAFTING_MAP_CODEC.forGetter(p_421343_ -> p_421343_.crafting),
            RecipeBookSettings.TypeSettings.FURNACE_MAP_CODEC.forGetter(p_421344_ -> p_421344_.furnace),
            RecipeBookSettings.TypeSettings.BLAST_FURNACE_MAP_CODEC.forGetter(p_421336_ -> p_421336_.blastFurnace),
            RecipeBookSettings.TypeSettings.SMOKER_MAP_CODEC.forGetter(p_421341_ -> p_421341_.smoker),
            CommonHooks.makeModdedRecipeBookTypesSettingsCodec().forGetter(settings -> settings.moddedSettings)
         )
         .apply(p_421339_, RecipeBookSettings::new)
   );
   private RecipeBookSettings.TypeSettings crafting;
   private RecipeBookSettings.TypeSettings furnace;
   private RecipeBookSettings.TypeSettings blastFurnace;
   private RecipeBookSettings.TypeSettings smoker;
   private final Map<RecipeBookType, RecipeBookSettings.TypeSettings> moddedSettings;

   public RecipeBookSettings() {
      this(
         RecipeBookSettings.TypeSettings.DEFAULT,
         RecipeBookSettings.TypeSettings.DEFAULT,
         RecipeBookSettings.TypeSettings.DEFAULT,
         RecipeBookSettings.TypeSettings.DEFAULT
      );
   }

   private RecipeBookSettings(
      RecipeBookSettings.TypeSettings p_422389_,
      RecipeBookSettings.TypeSettings p_422071_,
      RecipeBookSettings.TypeSettings p_421838_,
      RecipeBookSettings.TypeSettings p_421582_
   ) {
      this(p_422389_, p_422071_, p_421838_, p_421582_, new EnumMap<>(RecipeBookType.class));
   }

   private RecipeBookSettings(
      RecipeBookSettings.TypeSettings p_422389_,
      RecipeBookSettings.TypeSettings p_422071_,
      RecipeBookSettings.TypeSettings p_421838_,
      RecipeBookSettings.TypeSettings p_421582_,
      Map<RecipeBookType, RecipeBookSettings.TypeSettings> moddedSettings
   ) {
      this.crafting = p_422389_;
      this.furnace = p_422071_;
      this.blastFurnace = p_421838_;
      this.smoker = p_421582_;
      this.moddedSettings = moddedSettings;
   }

   @VisibleForTesting
   public RecipeBookSettings.TypeSettings getSettings(RecipeBookType p_380129_) {
      return switch (p_380129_) {
         case CRAFTING -> this.crafting;
         case FURNACE -> this.furnace;
         case BLAST_FURNACE -> this.blastFurnace;
         case SMOKER -> this.smoker;
         default -> (RecipeBookSettings.TypeSettings)this.moddedSettings.getOrDefault(p_380129_, RecipeBookSettings.TypeSettings.DEFAULT);
      };
   }

   private void updateSettings(RecipeBookType p_379381_, UnaryOperator<RecipeBookSettings.TypeSettings> p_379491_) {
      switch (p_379381_) {
         case CRAFTING:
            this.crafting = p_379491_.apply(this.crafting);
            break;
         case FURNACE:
            this.furnace = p_379491_.apply(this.furnace);
            break;
         case BLAST_FURNACE:
            this.blastFurnace = p_379491_.apply(this.blastFurnace);
            break;
         case SMOKER:
            this.smoker = p_379491_.apply(this.smoker);
            break;
         default:
            this.moddedSettings.put(p_379381_, p_379491_.apply(this.moddedSettings.getOrDefault(p_379381_, RecipeBookSettings.TypeSettings.DEFAULT)));
      }
   }

   public boolean isOpen(RecipeBookType p_12735_) {
      return this.getSettings(p_12735_).open;
   }

   public void setOpen(RecipeBookType p_12737_, boolean p_12738_) {
      this.updateSettings(p_12737_, p_379063_ -> p_379063_.setOpen(p_12738_));
   }

   public boolean isFiltering(RecipeBookType p_12755_) {
      return this.getSettings(p_12755_).filtering;
   }

   public void setFiltering(RecipeBookType p_12757_, boolean p_12758_) {
      this.updateSettings(p_12757_, p_379058_ -> p_379058_.setFiltering(p_12758_));
   }

   public RecipeBookSettings copy() {
      return new RecipeBookSettings(this.crafting, this.furnace, this.blastFurnace, this.smoker, new EnumMap<>(this.moddedSettings));
   }

   public void replaceFrom(RecipeBookSettings p_12733_) {
      this.crafting = p_12733_.crafting;
      this.furnace = p_12733_.furnace;
      this.blastFurnace = p_12733_.blastFurnace;
      this.smoker = p_12733_.smoker;
      this.moddedSettings.clear();
      this.moddedSettings.putAll(p_12733_.moddedSettings);
   }

   public record TypeSettings(boolean open, boolean filtering) {
      public static final RecipeBookSettings.TypeSettings DEFAULT = new RecipeBookSettings.TypeSettings(false, false);
      public static final MapCodec<RecipeBookSettings.TypeSettings> CRAFTING_MAP_CODEC = codec("isGuiOpen", "isFilteringCraftable");
      public static final MapCodec<RecipeBookSettings.TypeSettings> FURNACE_MAP_CODEC = codec("isFurnaceGuiOpen", "isFurnaceFilteringCraftable");
      public static final MapCodec<RecipeBookSettings.TypeSettings> BLAST_FURNACE_MAP_CODEC = codec(
         "isBlastingFurnaceGuiOpen", "isBlastingFurnaceFilteringCraftable"
      );
      public static final MapCodec<RecipeBookSettings.TypeSettings> SMOKER_MAP_CODEC = codec("isSmokerGuiOpen", "isSmokerFilteringCraftable");
      public static final StreamCodec<ByteBuf, RecipeBookSettings.TypeSettings> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.BOOL,
         RecipeBookSettings.TypeSettings::open,
         ByteBufCodecs.BOOL,
         RecipeBookSettings.TypeSettings::filtering,
         RecipeBookSettings.TypeSettings::new
      );

      @Override
      public String toString() {
         return "[open=" + this.open + ", filtering=" + this.filtering + "]";
      }

      public RecipeBookSettings.TypeSettings setOpen(boolean p_379785_) {
         return new RecipeBookSettings.TypeSettings(p_379785_, this.filtering);
      }

      public RecipeBookSettings.TypeSettings setFiltering(boolean p_380039_) {
         return new RecipeBookSettings.TypeSettings(this.open, p_380039_);
      }

      public static MapCodec<RecipeBookSettings.TypeSettings> codec(String p_422385_, String p_422603_) {
         return RecordCodecBuilder.mapCodec(
            p_422649_ -> p_422649_.group(
                  Codec.BOOL.optionalFieldOf(p_422385_, false).forGetter(RecipeBookSettings.TypeSettings::open),
                  Codec.BOOL.optionalFieldOf(p_422603_, false).forGetter(RecipeBookSettings.TypeSettings::filtering)
               )
               .apply(p_422649_, RecipeBookSettings.TypeSettings::new)
         );
      }
   }
}
