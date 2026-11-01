package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.Function;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class SimpleEquipmentLayer<S extends LivingEntityRenderState, RM extends EntityModel<? super S>, EM extends EntityModel<? super S>>
   extends RenderLayer<S, RM> {
   private final EquipmentLayerRenderer equipmentRenderer;
   private final EquipmentClientInfo.LayerType layer;
   private final Function<S, ItemStack> itemGetter;
   private final EM adultModel;
   @Nullable
   private final EM babyModel;
   private final int order;

   public SimpleEquipmentLayer(
      RenderLayerParent<S, RM> p_397277_,
      EquipmentLayerRenderer p_398036_,
      EquipmentClientInfo.LayerType p_397312_,
      Function<S, ItemStack> p_397526_,
      EM p_397856_,
      @Nullable EM p_436778_,
      int p_436781_
   ) {
      super(p_397277_);
      this.equipmentRenderer = p_398036_;
      this.layer = p_397312_;
      this.itemGetter = p_397526_;
      this.adultModel = p_397856_;
      this.babyModel = p_436778_;
      this.order = p_436781_;
   }

   public SimpleEquipmentLayer(
      RenderLayerParent<S, RM> p_397685_,
      EquipmentLayerRenderer p_397797_,
      EquipmentClientInfo.LayerType p_397371_,
      Function<S, ItemStack> p_397740_,
      EM p_397462_,
      @Nullable EM p_397451_
   ) {
      this(p_397685_, p_397797_, p_397371_, p_397740_, p_397462_, p_397451_, 0);
   }

   public void submit(PoseStack p_434184_, SubmitNodeCollector p_433972_, int p_434492_, S p_434386_, float p_434090_, float p_432997_) {
      ItemStack itemstack = this.itemGetter.apply(p_434386_);
      Equippable equippable = itemstack.get(DataComponents.EQUIPPABLE);
      if (equippable != null && !equippable.assetId().isEmpty() && (!p_434386_.isBaby || this.babyModel != null)) {
         EM em = p_434386_.isBaby ? this.babyModel : this.adultModel;
         this.equipmentRenderer
            .renderLayers(
               this.layer, equippable.assetId().get(), em, p_434386_, itemstack, p_434184_, p_433972_, p_434492_, null, p_434386_.outlineColor, this.order
            );
      }
   }
}
