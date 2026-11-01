package net.minecraft.client.data.models;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

@Environment(EnvType.CLIENT)
public class EquipmentAssetProvider implements DataProvider {
   private final PackOutput.PathProvider pathProvider;

   public EquipmentAssetProvider(PackOutput output) {
      this.pathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "equipment");
   }

   private static void bootstrap(BiConsumer<ResourceKey<EquipmentAsset>, EquipmentClientInfo> output) {
      output.accept(
         EquipmentAssets.LEATHER,
         EquipmentClientInfo.builder()
            .addHumanoidLayers(Identifier.withDefaultNamespace("leather"), true)
            .addHumanoidLayers(Identifier.withDefaultNamespace("leather_overlay"), false)
            .addLayers(
               EquipmentClientInfo.LayerType.HORSE_BODY,
               EquipmentClientInfo.Layer.leatherDyeable(Identifier.withDefaultNamespace("leather"), true),
               EquipmentClientInfo.Layer.leatherDyeable(Identifier.withDefaultNamespace("leather_overlay"), false)
            )
            .build()
      );
      output.accept(EquipmentAssets.CHAINMAIL, onlyHumanoid("chainmail"));
      output.accept(EquipmentAssets.COPPER, humanoidAndMountArmor("copper"));
      output.accept(EquipmentAssets.IRON, humanoidAndMountArmor("iron"));
      output.accept(EquipmentAssets.GOLD, humanoidAndMountArmor("gold"));
      output.accept(EquipmentAssets.DIAMOND, humanoidAndMountArmor("diamond"));
      output.accept(
         EquipmentAssets.TURTLE_SCUTE, EquipmentClientInfo.builder().addMainHumanoidLayer(Identifier.withDefaultNamespace("turtle_scute"), false).build()
      );
      output.accept(EquipmentAssets.NETHERITE, humanoidAndMountArmor("netherite"));
      output.accept(
         EquipmentAssets.ARMADILLO_SCUTE,
         EquipmentClientInfo.builder()
            .addLayers(EquipmentClientInfo.LayerType.WOLF_BODY, EquipmentClientInfo.Layer.onlyIfDyed(Identifier.withDefaultNamespace("armadillo_scute"), false))
            .addLayers(
               EquipmentClientInfo.LayerType.WOLF_BODY, EquipmentClientInfo.Layer.onlyIfDyed(Identifier.withDefaultNamespace("armadillo_scute_overlay"), true)
            )
            .build()
      );
      output.accept(
         EquipmentAssets.ELYTRA,
         EquipmentClientInfo.builder()
            .addLayers(EquipmentClientInfo.LayerType.WINGS, new EquipmentClientInfo.Layer(Identifier.withDefaultNamespace("elytra"), Optional.empty(), true))
            .build()
      );
      EquipmentClientInfo.Layer layer = new EquipmentClientInfo.Layer(Identifier.withDefaultNamespace("saddle"));
      output.accept(
         EquipmentAssets.SADDLE,
         EquipmentClientInfo.builder()
            .addLayers(EquipmentClientInfo.LayerType.PIG_SADDLE, layer)
            .addLayers(EquipmentClientInfo.LayerType.STRIDER_SADDLE, layer)
            .addLayers(EquipmentClientInfo.LayerType.CAMEL_SADDLE, layer)
            .addLayers(EquipmentClientInfo.LayerType.CAMEL_HUSK_SADDLE, layer)
            .addLayers(EquipmentClientInfo.LayerType.HORSE_SADDLE, layer)
            .addLayers(EquipmentClientInfo.LayerType.DONKEY_SADDLE, layer)
            .addLayers(EquipmentClientInfo.LayerType.MULE_SADDLE, layer)
            .addLayers(EquipmentClientInfo.LayerType.SKELETON_HORSE_SADDLE, layer)
            .addLayers(EquipmentClientInfo.LayerType.ZOMBIE_HORSE_SADDLE, layer)
            .addLayers(EquipmentClientInfo.LayerType.NAUTILUS_SADDLE, layer)
            .build()
      );

      for (Entry<DyeColor, ResourceKey<EquipmentAsset>> entry : EquipmentAssets.HARNESSES.entrySet()) {
         DyeColor dyeColor = entry.getKey();
         ResourceKey<EquipmentAsset> resourceKey = entry.getValue();
         output.accept(
            resourceKey,
            EquipmentClientInfo.builder()
               .addLayers(
                  EquipmentClientInfo.LayerType.HAPPY_GHAST_BODY,
                  EquipmentClientInfo.Layer.onlyIfDyed(Identifier.withDefaultNamespace(dyeColor.getSerializedName() + "_harness"), false)
               )
               .build()
         );
      }

      for (Entry<DyeColor, ResourceKey<EquipmentAsset>> entry : EquipmentAssets.CARPETS.entrySet()) {
         DyeColor dyeColor = entry.getKey();
         ResourceKey<EquipmentAsset> resourceKey = entry.getValue();
         output.accept(
            resourceKey,
            EquipmentClientInfo.builder()
               .addLayers(
                  EquipmentClientInfo.LayerType.LLAMA_BODY, new EquipmentClientInfo.Layer(Identifier.withDefaultNamespace(dyeColor.getSerializedName()))
               )
               .build()
         );
      }

      output.accept(
         EquipmentAssets.TRADER_LLAMA,
         EquipmentClientInfo.builder()
            .addLayers(EquipmentClientInfo.LayerType.LLAMA_BODY, new EquipmentClientInfo.Layer(Identifier.withDefaultNamespace("trader_llama")))
            .build()
      );
   }

   private static EquipmentClientInfo onlyHumanoid(String name) {
      return EquipmentClientInfo.builder().addHumanoidLayers(Identifier.withDefaultNamespace(name)).build();
   }

   private static EquipmentClientInfo humanoidAndMountArmor(String name) {
      return EquipmentClientInfo.builder()
         .addHumanoidLayers(Identifier.withDefaultNamespace(name))
         .addLayers(EquipmentClientInfo.LayerType.HORSE_BODY, EquipmentClientInfo.Layer.leatherDyeable(Identifier.withDefaultNamespace(name), false))
         .addLayers(EquipmentClientInfo.LayerType.NAUTILUS_BODY, EquipmentClientInfo.Layer.leatherDyeable(Identifier.withDefaultNamespace(name), false))
         .build();
   }

   @Override
   public CompletableFuture<?> run(CachedOutput output) {
      Map<ResourceKey<EquipmentAsset>, EquipmentClientInfo> map = new HashMap<>();
      bootstrap((resourceKey, equipmentClientInfo) -> {
         if (map.putIfAbsent(resourceKey, equipmentClientInfo) != null) {
            throw new IllegalStateException("Tried to register equipment asset twice for id: " + resourceKey);
         }
      });
      return DataProvider.saveAll(output, EquipmentClientInfo.CODEC, this.pathProvider::json, map);
   }

   @Override
   public String getName() {
      return "Equipment Asset Definitions";
   }
}
