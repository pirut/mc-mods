package net.minecraft.world.item.armortrim;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

public class ArmorTrim implements TooltipProvider {
   public static final Codec<ArmorTrim> CODEC = RecordCodecBuilder.create(
      p_337943_ -> p_337943_.group(
            TrimMaterial.CODEC.fieldOf("material").forGetter(ArmorTrim::material),
            TrimPattern.CODEC.fieldOf("pattern").forGetter(ArmorTrim::pattern),
            Codec.BOOL.optionalFieldOf("show_in_tooltip", true).forGetter(p_330108_ -> p_330108_.showInTooltip)
         )
         .apply(p_337943_, ArmorTrim::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, ArmorTrim> STREAM_CODEC = StreamCodec.composite(
      TrimMaterial.STREAM_CODEC,
      ArmorTrim::material,
      TrimPattern.STREAM_CODEC,
      ArmorTrim::pattern,
      ByteBufCodecs.BOOL,
      p_330107_ -> p_330107_.showInTooltip,
      ArmorTrim::new
   );
   private static final Component UPGRADE_TITLE = Component.translatable(
         Util.makeDescriptionId("item", ResourceLocation.withDefaultNamespace("smithing_template.upgrade"))
      )
      .withStyle(ChatFormatting.GRAY);
   private final Holder<TrimMaterial> material;
   private final Holder<TrimPattern> pattern;
   private final boolean showInTooltip;
   private final Function<Holder<ArmorMaterial>, ResourceLocation> innerTexture;
   private final Function<Holder<ArmorMaterial>, ResourceLocation> outerTexture;

   private ArmorTrim(
      Holder<TrimMaterial> p_336069_,
      Holder<TrimPattern> p_335896_,
      boolean p_335621_,
      Function<Holder<ArmorMaterial>, ResourceLocation> p_335613_,
      Function<Holder<ArmorMaterial>, ResourceLocation> p_335818_
   ) {
      this.material = p_336069_;
      this.pattern = p_335896_;
      this.showInTooltip = p_335621_;
      this.innerTexture = p_335613_;
      this.outerTexture = p_335818_;
   }

   public ArmorTrim(Holder<TrimMaterial> p_331108_, Holder<TrimPattern> p_331731_, boolean p_331871_) {
      this.material = p_331108_;
      this.pattern = p_331731_;
      this.innerTexture = Util.memoize(p_335286_ -> {
         ResourceLocation $$3 = p_331731_.value().assetId();
         String $$4 = getColorPaletteSuffix(p_331108_, p_335286_);
         return $$3.withPath(p_266737_ -> "trims/models/armor/" + p_266737_ + "_leggings_" + $$4);
      });
      this.outerTexture = Util.memoize(p_335283_ -> {
         ResourceLocation $$3 = p_331731_.value().assetId();
         String $$4 = getColorPaletteSuffix(p_331108_, p_335283_);
         return $$3.withPath(p_266864_ -> "trims/models/armor/" + p_266864_ + "_" + $$4);
      });
      this.showInTooltip = p_331871_;
   }

   public ArmorTrim(Holder<TrimMaterial> p_267249_, Holder<TrimPattern> p_267212_) {
      this(p_267249_, p_267212_, true);
   }

   private static String getColorPaletteSuffix(Holder<TrimMaterial> p_323989_, Holder<ArmorMaterial> p_335566_) {
      Map<Holder<ArmorMaterial>, String> $$2 = p_323989_.value().overrideArmorMaterials();
      String $$3 = $$2.get(p_335566_);
      return $$3 != null ? $$3 : p_323989_.value().assetName();
   }

   public boolean hasPatternAndMaterial(Holder<TrimPattern> p_266942_, Holder<TrimMaterial> p_267247_) {
      return p_266942_.equals(this.pattern) && p_267247_.equals(this.material);
   }

   public Holder<TrimPattern> pattern() {
      return this.pattern;
   }

   public Holder<TrimMaterial> material() {
      return this.material;
   }

   public ResourceLocation innerTexture(Holder<ArmorMaterial> p_324361_) {
      return this.innerTexture.apply(p_324361_);
   }

   public ResourceLocation outerTexture(Holder<ArmorMaterial> p_323530_) {
      return this.outerTexture.apply(p_323530_);
   }

   @Override
   public boolean equals(Object p_267123_) {
      return !(p_267123_ instanceof ArmorTrim $$1)
         ? false
         : this.showInTooltip == $$1.showInTooltip && this.pattern.equals($$1.pattern) && this.material.equals($$1.material);
   }

   @Override
   public int hashCode() {
      int $$0 = this.material.hashCode();
      $$0 = 31 * $$0 + this.pattern.hashCode();
      return 31 * $$0 + (this.showInTooltip ? 1 : 0);
   }

   @Override
   public void addToTooltip(Item.TooltipContext p_341366_, Consumer<Component> p_331480_, TooltipFlag p_330344_) {
      if (this.showInTooltip) {
         p_331480_.accept(UPGRADE_TITLE);
         p_331480_.accept(CommonComponents.space().append(this.pattern.value().copyWithStyle(this.material)));
         p_331480_.accept(CommonComponents.space().append(this.material.value().description()));
      }
   }

   public ArmorTrim withTooltip(boolean p_335391_) {
      return new ArmorTrim(this.material, this.pattern, p_335391_, this.innerTexture, this.outerTexture);
   }
}
