package net.neoforged.neoforge.common.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jspecify.annotations.Nullable;

public interface AttributeTooltipContext extends Item.TooltipContext {
   @Nullable
   Player player();

   TooltipDisplay tooltipDisplay();

   TooltipFlag flag();

   static AttributeTooltipContext of(
      @Nullable final Player player, final Item.TooltipContext itemCtx, final TooltipDisplay tooltipDisplay, final TooltipFlag flag
   ) {
      return new AttributeTooltipContext() {
         @Nullable
         @Override
         public HolderLookup.Provider registries() {
            return itemCtx.registries();
         }

         @Override
         public float tickRate() {
            return itemCtx.tickRate();
         }

         @Nullable
         @Override
         public MapItemSavedData mapData(MapId id) {
            return itemCtx.mapData(id);
         }

         @Nullable
         @Override
         public Level level() {
            return itemCtx.level();
         }

         @Override
         public boolean isPeaceful() {
            return itemCtx.isPeaceful();
         }

         @Nullable
         @Override
         public Player player() {
            return player;
         }

         @Override
         public TooltipDisplay tooltipDisplay() {
            return tooltipDisplay;
         }

         @Override
         public TooltipFlag flag() {
            return flag;
         }
      };
   }
}
