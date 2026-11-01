package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class EquipmentLayerRenderer {
   private static final int NO_LAYER_COLOR = 0;
   private final EquipmentAssetManager equipmentAssets;
   private final Function<EquipmentLayerRenderer.LayerTextureKey, Identifier> layerTextureLookup;
   private final Function<EquipmentLayerRenderer.TrimSpriteKey, TextureAtlasSprite> trimSpriteLookup;

   public EquipmentLayerRenderer(EquipmentAssetManager equipmentAssets, TextureAtlas atlas) {
      this.equipmentAssets = equipmentAssets;
      this.layerTextureLookup = Util.memoize(layerTextureKey -> layerTextureKey.layer.getTextureLocation(layerTextureKey.layerType));
      this.trimSpriteLookup = Util.memoize(trimSpriteKey -> atlas.getSprite(trimSpriteKey.spriteId()));
   }

   public <S> void renderLayers(
      EquipmentClientInfo.LayerType layerType,
      ResourceKey<EquipmentAsset> equipmentAsset,
      Model<? super S> armorModel,
      S renderState,
      ItemStack item,
      PoseStack poseStack,
      SubmitNodeCollector nodeCollector,
      int packedLight,
      int outlineColor
   ) {
      this.renderLayers(layerType, equipmentAsset, armorModel, renderState, item, poseStack, nodeCollector, packedLight, null, outlineColor, 1);
   }

   public <S> void renderLayers(
      EquipmentClientInfo.LayerType layerType,
      ResourceKey<EquipmentAsset> equipmentAsset,
      Model<? super S> armorModel,
      S renderState,
      ItemStack item,
      PoseStack poseStack,
      SubmitNodeCollector nodeCollector,
      int packedLight,
      @Nullable Identifier texture,
      int outlineColor,
      int key
   ) {
      List<EquipmentClientInfo.Layer> list = this.equipmentAssets.get(equipmentAsset).getLayers(layerType);
      if (!list.isEmpty()) {
         int i = DyedItemColor.getOrDefault(item, 0);
         boolean bl = item.hasFoil();
         int j = key;

         for (EquipmentClientInfo.Layer layer : list) {
            int k = getColorForLayer(layer, i);
            if (k != 0) {
               Identifier identifier = layer.usePlayerTexture() && texture != null
                  ? texture
                  : this.layerTextureLookup.apply(new EquipmentLayerRenderer.LayerTextureKey(layerType, layer));
               nodeCollector.order(j++)
                  .submitModel(
                     armorModel,
                     renderState,
                     poseStack,
                     RenderTypes.armorCutoutNoCull(identifier),
                     packedLight,
                     OverlayTexture.NO_OVERLAY,
                     k,
                     null,
                     outlineColor,
                     null
                  );
               if (bl) {
                  nodeCollector.order(j++)
                     .submitModel(
                        armorModel, renderState, poseStack, RenderTypes.armorEntityGlint(), packedLight, OverlayTexture.NO_OVERLAY, k, null, outlineColor, null
                     );
               }

               bl = false;
            }
         }

         ArmorTrim armorTrim = item.get(DataComponents.TRIM);
         if (armorTrim != null) {
            TextureAtlasSprite textureAtlasSprite = this.trimSpriteLookup.apply(new EquipmentLayerRenderer.TrimSpriteKey(armorTrim, layerType, equipmentAsset));
            RenderType renderType = Sheets.armorTrimsSheet(armorTrim.pattern().value().decal());
            nodeCollector.order(j++)
               .submitModel(armorModel, renderState, poseStack, renderType, packedLight, OverlayTexture.NO_OVERLAY, -1, textureAtlasSprite, outlineColor, null);
         }
      }
   }

   private static int getColorForLayer(EquipmentClientInfo.Layer layer, int color) {
      Optional<EquipmentClientInfo.Dyeable> optional = layer.dyeable();
      if (optional.isPresent()) {
         int i = optional.get().colorWhenUndyed().map(ARGB::opaque).orElse(0);
         return color != 0 ? color : i;
      } else {
         return -1;
      }
   }

   @Environment(EnvType.CLIENT)
   record LayerTextureKey(EquipmentClientInfo.LayerType layerType, EquipmentClientInfo.Layer layer) {
   }

   @Environment(EnvType.CLIENT)
   record TrimSpriteKey(ArmorTrim trim, EquipmentClientInfo.LayerType layerType, ResourceKey<EquipmentAsset> equipmentAssetId) {
      public Identifier spriteId() {
         return this.trim.layerAssetId(this.layerType.trimAssetPrefix(), this.equipmentAssetId);
      }
   }
}
