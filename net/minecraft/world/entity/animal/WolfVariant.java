package net.minecraft.world.entity.animal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

public final class WolfVariant {
   public static final Codec<WolfVariant> DIRECT_CODEC = RecordCodecBuilder.create(
      p_332779_ -> p_332779_.group(
            ResourceLocation.CODEC.fieldOf("wild_texture").forGetter(p_335261_ -> p_335261_.wildTexture),
            ResourceLocation.CODEC.fieldOf("tame_texture").forGetter(p_335263_ -> p_335263_.tameTexture),
            ResourceLocation.CODEC.fieldOf("angry_texture").forGetter(p_335264_ -> p_335264_.angryTexture),
            RegistryCodecs.homogeneousList(Registries.BIOME).fieldOf("biomes").forGetter(WolfVariant::biomes)
         )
         .apply(p_332779_, WolfVariant::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, WolfVariant> DIRECT_STREAM_CODEC = StreamCodec.composite(
      ResourceLocation.STREAM_CODEC,
      WolfVariant::wildTexture,
      ResourceLocation.STREAM_CODEC,
      WolfVariant::tameTexture,
      ResourceLocation.STREAM_CODEC,
      WolfVariant::angryTexture,
      ByteBufCodecs.holderSet(Registries.BIOME),
      WolfVariant::biomes,
      WolfVariant::new
   );
   public static final Codec<Holder<WolfVariant>> CODEC = RegistryFileCodec.create(Registries.WOLF_VARIANT, DIRECT_CODEC);
   public static final StreamCodec<RegistryFriendlyByteBuf, Holder<WolfVariant>> STREAM_CODEC = ByteBufCodecs.holder(
      Registries.WOLF_VARIANT, DIRECT_STREAM_CODEC
   );
   private final ResourceLocation wildTexture;
   private final ResourceLocation tameTexture;
   private final ResourceLocation angryTexture;
   private final ResourceLocation wildTextureFull;
   private final ResourceLocation tameTextureFull;
   private final ResourceLocation angryTextureFull;
   private final HolderSet<Biome> biomes;

   public WolfVariant(ResourceLocation p_332712_, ResourceLocation p_332714_, ResourceLocation p_332788_, HolderSet<Biome> p_332717_) {
      this.wildTexture = p_332712_;
      this.wildTextureFull = fullTextureId(p_332712_);
      this.tameTexture = p_332714_;
      this.tameTextureFull = fullTextureId(p_332714_);
      this.angryTexture = p_332788_;
      this.angryTextureFull = fullTextureId(p_332788_);
      this.biomes = p_332717_;
   }

   private static ResourceLocation fullTextureId(ResourceLocation p_336042_) {
      return p_336042_.withPath(p_335262_ -> "textures/" + p_335262_ + ".png");
   }

   public ResourceLocation wildTexture() {
      return this.wildTextureFull;
   }

   public ResourceLocation tameTexture() {
      return this.tameTextureFull;
   }

   public ResourceLocation angryTexture() {
      return this.angryTextureFull;
   }

   public HolderSet<Biome> biomes() {
      return this.biomes;
   }

   @Override
   public boolean equals(Object p_332811_) {
      if (p_332811_ == this) {
         return true;
      } else {
         return !(p_332811_ instanceof WolfVariant $$1)
            ? false
            : Objects.equals(this.wildTexture, $$1.wildTexture)
               && Objects.equals(this.tameTexture, $$1.tameTexture)
               && Objects.equals(this.angryTexture, $$1.angryTexture)
               && Objects.equals(this.biomes, $$1.biomes);
      }
   }

   @Override
   public int hashCode() {
      int $$0 = 1;
      $$0 = 31 * $$0 + this.wildTexture.hashCode();
      $$0 = 31 * $$0 + this.tameTexture.hashCode();
      $$0 = 31 * $$0 + this.angryTexture.hashCode();
      return 31 * $$0 + this.biomes.hashCode();
   }
}
