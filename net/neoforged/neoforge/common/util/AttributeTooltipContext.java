package net.neoforged.neoforge.common.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;

public interface AttributeTooltipContext extends Item.TooltipContext {
   @Nullable
   Player player();

   TooltipFlag flag();

   static AttributeTooltipContext of(@Nullable final Player player, final Item.TooltipContext itemCtx, final TooltipFlag flag) {
      return new AttributeTooltipContext() {
         @Override
         public HolderLookup.Provider registries() {
            return itemCtx.registries();
         }

         @Override
         public float tickRate() {
            return itemCtx.tickRate();
         }

         @Override
         public MapItemSavedData mapData(MapId id) {
            return itemCtx.mapData(id);
         }

         @Override
         public Level level() {
            return itemCtx.level();
         }

         @Nullable
         @Override
         public Player player() {
            return player;
         }

         @Override
         public TooltipFlag flag() {
            return flag;
         }
      };
   }
}
