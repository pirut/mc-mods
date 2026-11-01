package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
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
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class EquipmentLayerRenderer {
   private static final int NO_LAYER_COLOR = 0;
   private final EquipmentAssetManager equipmentAssets;
   private final Function<EquipmentLayerRenderer.LayerTextureKey, Identifier> layerTextureLookup;
   private final Function<EquipmentLayerRenderer.TrimSpriteKey, TextureAtlasSprite> trimSpriteLookup;

   public EquipmentLayerRenderer(EquipmentAssetManager p_388621_, TextureAtlas p_371221_) {
      this.equipmentAssets = p_388621_;
      this.layerTextureLookup = Util.memoize(p_465655_ -> p_465655_.layer.getTextureLocation(p_465655_.layerType));
      this.trimSpriteLookup = Util.memoize(p_465654_ -> p_371221_.getSprite(p_465654_.spriteId()));
   }

   public <S> void renderLayers(
      EquipmentClientInfo.LayerType p_388694_,
      ResourceKey<EquipmentAsset> p_386937_,
      Model<? super S> p_371498_,
      S p_435837_,
      ItemStack p_371902_,
      PoseStack p_371937_,
      SubmitNodeCollector p_434165_,
      int p_371495_,
      int p_434452_
   ) {
      this.renderLayers(p_388694_, p_386937_, p_371498_, p_435837_, p_371902_, p_371937_, p_434165_, p_371495_, null, p_434452_, 1);
   }

   public <S> void renderLayers(
      EquipmentClientInfo.LayerType p_387484_,
      ResourceKey<EquipmentAsset> p_387603_,
      Model<? super S> p_371731_,
      S p_435806_,
      ItemStack p_371670_,
      PoseStack p_371767_,
      SubmitNodeCollector p_435795_,
      int p_371309_,
      @Nullable Identifier p_469270_,
      int p_435821_,
      int p_436591_
   ) {
      IClientItemExtensions extensions = IClientItemExtensions.of(p_371670_);
      p_371731_ = extensions.getGenericArmorModel(p_371670_, p_387484_, p_371731_);
      List<EquipmentClientInfo.Layer> list = this.equipmentAssets.get(p_387603_).getLayers(p_387484_);
      if (!list.isEmpty()) {
         int i = extensions.getDefaultDyeColor(p_371670_);
         boolean flag = p_371670_.hasFoil();
         int j = p_436591_;
         int idx = 0;

         for (EquipmentClientInfo.Layer equipmentclientinfo$layer : list) {
            int k = extensions.getArmorLayerTintColor(p_371670_, equipmentclientinfo$layer, idx, i);
            if (k != 0) {
               Identifier identifier = equipmentclientinfo$layer.usePlayerTexture() && p_469270_ != null
                  ? p_469270_
                  : this.layerTextureLookup.apply(new EquipmentLayerRenderer.LayerTextureKey(p_387484_, equipmentclientinfo$layer));
               identifier = ClientHooks.getArmorTexture(p_371670_, p_387484_, equipmentclientinfo$layer, identifier);
               p_435795_.order(j++)
                  .submitModel(
                     p_371731_, p_435806_, p_371767_, RenderTypes.armorCutoutNoCull(identifier), p_371309_, OverlayTexture.NO_OVERLAY, k, null, p_435821_, null
                  );
               if (flag) {
                  p_435795_.order(j++)
                     .submitModel(
                        p_371731_, p_435806_, p_371767_, RenderTypes.armorEntityGlint(), p_371309_, OverlayTexture.NO_OVERLAY, k, null, p_435821_, null
                     );
               }

               flag = false;
            }

            idx++;
         }

         ArmorTrim armortrim = p_371670_.get(DataComponents.TRIM);
         if (armortrim != null) {
            TextureAtlasSprite textureatlassprite = this.trimSpriteLookup.apply(new EquipmentLayerRenderer.TrimSpriteKey(armortrim, p_387484_, p_387603_));
            RenderType rendertype = Sheets.armorTrimsSheet(armortrim.pattern().value().decal());
            p_435795_.order(j++)
               .submitModel(p_371731_, p_435806_, p_371767_, rendertype, p_371309_, OverlayTexture.NO_OVERLAY, -1, textureatlassprite, p_435821_, null);
         }
      }
   }

   public static int getColorForLayer(EquipmentClientInfo.Layer p_386482_, int p_371443_) {
      Optional<EquipmentClientInfo.Dyeable> optional = p_386482_.dyeable();
      if (optional.isPresent()) {
         int i = optional.get().colorWhenUndyed().map(ARGB::opaque).orElse(0);
         return p_371443_ != 0 ? p_371443_ : i;
      } else {
         return -1;
      }
   }

   @OnlyIn(Dist.CLIENT)
   record LayerTextureKey(EquipmentClientInfo.LayerType layerType, EquipmentClientInfo.Layer layer) {
   }

   @OnlyIn(Dist.CLIENT)
   record TrimSpriteKey(ArmorTrim trim, EquipmentClientInfo.LayerType layerType, ResourceKey<EquipmentAsset> equipmentAssetId) {
      public Identifier spriteId() {
         return this.trim.layerAssetId(this.layerType.trimAssetPrefix(), this.equipmentAssetId);
      }
   }
}
