package net.minecraft.client.renderer.item;

import java.util.function.Function;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ItemModelResolver {
   private final Function<Identifier, ItemModel> modelGetter;
   private final Function<Identifier, ClientItem.Properties> clientProperties;

   public ItemModelResolver(ModelManager p_387246_) {
      this.modelGetter = p_387246_::getItemModel;
      this.clientProperties = p_387246_::getItemProperties;
   }

   public void updateForLiving(ItemStackRenderState p_387100_, ItemStack p_387635_, ItemDisplayContext p_388107_, LivingEntity p_388201_) {
      this.updateForTopItem(p_387100_, p_387635_, p_388107_, p_388201_.level(), p_388201_, p_388201_.getId() + p_388107_.ordinal());
   }

   public void updateForNonLiving(ItemStackRenderState p_386914_, ItemStack p_388286_, ItemDisplayContext p_387479_, Entity p_386766_) {
      this.updateForTopItem(p_386914_, p_388286_, p_387479_, p_386766_.level(), null, p_386766_.getId());
   }

   public void updateForTopItem(
      ItemStackRenderState p_387014_,
      ItemStack p_388693_,
      ItemDisplayContext p_388835_,
      @Nullable Level p_388064_,
      @Nullable ItemOwner p_434877_,
      int p_388137_
   ) {
      p_387014_.clear();
      if (!p_388693_.isEmpty()) {
         p_387014_.displayContext = p_388835_;
         this.appendItemLayers(p_387014_, p_388693_, p_388835_, p_388064_, p_434877_, p_388137_);
      }
   }

   public void appendItemLayers(
      ItemStackRenderState p_387962_,
      ItemStack p_388009_,
      ItemDisplayContext p_387722_,
      @Nullable Level p_388449_,
      @Nullable ItemOwner p_435150_,
      int p_388206_
   ) {
      Identifier identifier = p_388009_.get(DataComponents.ITEM_MODEL);
      if (identifier != null) {
         p_387962_.setOversizedInGui(this.clientProperties.apply(identifier).oversizedInGui());
         this.modelGetter
            .apply(identifier)
            .update(p_387962_, p_388009_, this, p_387722_, p_388449_ instanceof ClientLevel clientlevel ? clientlevel : null, p_435150_, p_388206_);
      }
   }

   public boolean shouldPlaySwapAnimation(ItemStack p_390529_) {
      Identifier identifier = p_390529_.get(DataComponents.ITEM_MODEL);
      return identifier == null ? true : this.clientProperties.apply(identifier).handAnimationOnSwap();
   }

   public float swapAnimationScale(ItemStack p_455446_) {
      Identifier identifier = p_455446_.get(DataComponents.ITEM_MODEL);
      return identifier == null ? 1.0F : this.clientProperties.apply(identifier).swapAnimationScale();
   }
}
