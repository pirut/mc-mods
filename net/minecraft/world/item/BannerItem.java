package net.minecraft.world.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import org.apache.commons.lang3.Validate;

public class BannerItem extends StandingAndWallBlockItem {
   public BannerItem(Block p_40534_, Block p_40535_, Item.Properties p_40536_) {
      super(p_40534_, p_40535_, p_40536_, Direction.DOWN);
      Validate.isInstanceOf(AbstractBannerBlock.class, p_40534_);
      Validate.isInstanceOf(AbstractBannerBlock.class, p_40535_);
   }

   public static void appendHoverTextFromBannerBlockEntityTag(ItemStack p_40543_, List<Component> p_40544_) {
      BannerPatternLayers $$2 = p_40543_.get(DataComponents.BANNER_PATTERNS);
      if ($$2 != null) {
         for (int $$3 = 0; $$3 < Math.min($$2.layers().size(), 6); $$3++) {
            BannerPatternLayers.Layer $$4 = $$2.layers().get($$3);
            p_40544_.add($$4.description().withStyle(ChatFormatting.GRAY));
         }
      }
   }

   public DyeColor getColor() {
      return ((AbstractBannerBlock)this.getBlock()).getColor();
   }

   @Override
   public void appendHoverText(ItemStack p_40538_, Item.TooltipContext p_339668_, List<Component> p_40540_, TooltipFlag p_40541_) {
      appendHoverTextFromBannerBlockEntityTag(p_40538_, p_40540_);
   }
}
