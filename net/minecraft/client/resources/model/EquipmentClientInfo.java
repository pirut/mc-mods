package net.minecraft.client.resources.model;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.common.asm.enumextension.ExtensionInfo;
import net.neoforged.fml.common.asm.enumextension.IExtensibleEnum;
import net.neoforged.fml.common.asm.enumextension.NamedEnum;

@OnlyIn(Dist.CLIENT)
public record EquipmentClientInfo(Map<EquipmentClientInfo.LayerType, List<EquipmentClientInfo.Layer>> layers) {
   private static final Codec<List<EquipmentClientInfo.Layer>> LAYER_LIST_CODEC = ExtraCodecs.nonEmptyList(EquipmentClientInfo.Layer.CODEC.listOf());
   public static final Codec<EquipmentClientInfo> CODEC = RecordCodecBuilder.create(
      p_388468_ -> p_388468_.group(
            ExtraCodecs.nonEmptyMap(Codec.unboundedMap(EquipmentClientInfo.LayerType.CODEC, LAYER_LIST_CODEC))
               .fieldOf("layers")
               .forGetter(EquipmentClientInfo::layers)
         )
         .apply(p_388468_, EquipmentClientInfo::new)
   );

   public static EquipmentClientInfo.Builder builder() {
      return new EquipmentClientInfo.Builder();
   }

   public List<EquipmentClientInfo.Layer> getLayers(EquipmentClientInfo.LayerType p_387923_) {
      return this.layers.getOrDefault(p_387923_, List.of());
   }

   @OnlyIn(Dist.CLIENT)
   public static class Builder {
      private final Map<EquipmentClientInfo.LayerType, List<EquipmentClientInfo.Layer>> layersByType = new EnumMap<>(EquipmentClientInfo.LayerType.class);

      Builder() {
      }

      public EquipmentClientInfo.Builder addHumanoidLayers(Identifier p_468366_) {
         return this.addHumanoidLayers(p_468366_, false);
      }

      public EquipmentClientInfo.Builder addHumanoidLayers(Identifier p_469073_, boolean p_387092_) {
         this.addLayers(EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS, EquipmentClientInfo.Layer.leatherDyeable(p_469073_, p_387092_));
         this.addMainHumanoidLayer(p_469073_, p_387092_);
         return this;
      }

      public EquipmentClientInfo.Builder addMainHumanoidLayer(Identifier p_468639_, boolean p_388218_) {
         return this.addLayers(EquipmentClientInfo.LayerType.HUMANOID, EquipmentClientInfo.Layer.leatherDyeable(p_468639_, p_388218_));
      }

      public EquipmentClientInfo.Builder addLayers(EquipmentClientInfo.LayerType p_388688_, EquipmentClientInfo.Layer... p_388509_) {
         Collections.addAll(this.layersByType.computeIfAbsent(p_388688_, p_387338_ -> new ArrayList<>()), p_388509_);
         return this;
      }

      public EquipmentClientInfo build() {
         return new EquipmentClientInfo(
            this.layersByType
               .entrySet()
               .stream()
               .collect(ImmutableMap.toImmutableMap(Entry::getKey, p_386826_ -> List.copyOf((Collection)p_386826_.getValue())))
         );
      }
   }

   @OnlyIn(Dist.CLIENT)
   public record Dyeable(Optional<Integer> colorWhenUndyed) {
      public static final Codec<EquipmentClientInfo.Dyeable> CODEC = RecordCodecBuilder.create(
         p_387846_ -> p_387846_.group(ExtraCodecs.RGB_COLOR_CODEC.optionalFieldOf("color_when_undyed").forGetter(EquipmentClientInfo.Dyeable::colorWhenUndyed))
            .apply(p_387846_, EquipmentClientInfo.Dyeable::new)
      );
   }

   @OnlyIn(Dist.CLIENT)
   public record Layer(Identifier textureId, Optional<EquipmentClientInfo.Dyeable> dyeable, boolean usePlayerTexture) {
      public static final Codec<EquipmentClientInfo.Layer> CODEC = RecordCodecBuilder.create(
         p_465775_ -> p_465775_.group(
               Identifier.CODEC.fieldOf("texture").forGetter(EquipmentClientInfo.Layer::textureId),
               EquipmentClientInfo.Dyeable.CODEC.optionalFieldOf("dyeable").forGetter(EquipmentClientInfo.Layer::dyeable),
               Codec.BOOL.optionalFieldOf("use_player_texture", false).forGetter(EquipmentClientInfo.Layer::usePlayerTexture)
            )
            .apply(p_465775_, EquipmentClientInfo.Layer::new)
      );

      public Layer(Identifier p_468646_) {
         this(p_468646_, Optional.empty(), false);
      }

      public static EquipmentClientInfo.Layer leatherDyeable(Identifier p_468148_, boolean p_388540_) {
         return new EquipmentClientInfo.Layer(
            p_468148_, p_388540_ ? Optional.of(new EquipmentClientInfo.Dyeable(Optional.of(-6265536))) : Optional.empty(), false
         );
      }

      public static EquipmentClientInfo.Layer onlyIfDyed(Identifier p_469334_, boolean p_386457_) {
         return new EquipmentClientInfo.Layer(p_469334_, p_386457_ ? Optional.of(new EquipmentClientInfo.Dyeable(Optional.empty())) : Optional.empty(), false);
      }

      public Identifier getTextureLocation(EquipmentClientInfo.LayerType p_386578_) {
         return this.textureId.withPath(p_386699_ -> "textures/entity/equipment/" + p_386578_.getSerializedName() + "/" + p_386699_ + ".png");
      }
   }

   @OnlyIn(Dist.CLIENT)
   @NamedEnum
   public static enum LayerType implements StringRepresentable, IExtensibleEnum {
      HUMANOID("humanoid"),
      HUMANOID_LEGGINGS("humanoid_leggings"),
      WINGS("wings"),
      WOLF_BODY("wolf_body"),
      HORSE_BODY("horse_body"),
      LLAMA_BODY("llama_body"),
      PIG_SADDLE("pig_saddle"),
      STRIDER_SADDLE("strider_saddle"),
      CAMEL_SADDLE("camel_saddle"),
      CAMEL_HUSK_SADDLE("camel_husk_saddle"),
      HORSE_SADDLE("horse_saddle"),
      DONKEY_SADDLE("donkey_saddle"),
      MULE_SADDLE("mule_saddle"),
      ZOMBIE_HORSE_SADDLE("zombie_horse_saddle"),
      SKELETON_HORSE_SADDLE("skeleton_horse_saddle"),
      HAPPY_GHAST_BODY("happy_ghast_body"),
      NAUTILUS_SADDLE("nautilus_saddle"),
      NAUTILUS_BODY("nautilus_body");

      public static final Codec<EquipmentClientInfo.LayerType> CODEC = StringRepresentable.fromEnum(EquipmentClientInfo.LayerType::values);
      private final String id;

      private LayerType(String p_387059_) {
         this.id = p_387059_.replaceAll(":", "/");
      }

      @Override
      public String getSerializedName() {
         return this.id;
      }

      public String trimAssetPrefix() {
         return "trims/entity/" + this.id;
      }

      public static ExtensionInfo getExtensionInfo() {
         return ExtensionInfo.nonExtended(EquipmentClientInfo.LayerType.class);
      }
   }
}
