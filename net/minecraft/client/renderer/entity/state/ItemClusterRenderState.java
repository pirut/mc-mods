package net.minecraft.client.renderer.entity.state;

import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ItemClusterRenderState extends EntityRenderState {
   public final ItemStackRenderState item = new ItemStackRenderState();
   public int count;
   public int seed;
   public boolean shouldSpread;

   public void extractItemGroupRenderState(Entity p_386526_, ItemStack p_386486_, ItemModelResolver p_387036_) {
      p_387036_.updateForNonLiving(this.item, p_386486_, ItemDisplayContext.GROUND, p_386526_);
      this.count = getRenderedAmount(p_386486_.getCount());
      this.seed = getSeedForItemStack(p_386486_);
   }

   public static int getSeedForItemStack(ItemStack p_387411_) {
      return p_387411_.isEmpty() ? 187 : Item.getId(p_387411_.getItem()) + p_387411_.getDamageValue();
   }

   public static int getRenderedAmount(int p_387955_) {
      if (p_387955_ <= 1) {
         return 1;
      } else if (p_387955_ <= 16) {
         return 2;
      } else if (p_387955_ <= 32) {
         return 3;
      } else {
         return p_387955_ <= 48 ? 4 : 5;
      }
   }
}
