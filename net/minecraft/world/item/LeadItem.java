package net.minecraft.world.item;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;

public class LeadItem extends Item {
   public LeadItem(Item.Properties p_42828_) {
      super(p_42828_);
   }

   @Override
   public InteractionResult useOn(UseOnContext p_42834_) {
      Level $$1 = p_42834_.getLevel();
      BlockPos $$2 = p_42834_.getClickedPos();
      BlockState $$3 = $$1.getBlockState($$2);
      if ($$3.is(BlockTags.FENCES)) {
         Player $$4 = p_42834_.getPlayer();
         if (!$$1.isClientSide && $$4 != null) {
            bindPlayerMobs($$4, $$1, $$2);
         }

         return InteractionResult.sidedSuccess($$1.isClientSide);
      } else {
         return InteractionResult.PASS;
      }
   }

   public static InteractionResult bindPlayerMobs(Player p_42830_, Level p_42831_, BlockPos p_42832_) {
      LeashFenceKnotEntity $$3 = null;
      List<Leashable> $$4 = leashableInArea(p_42831_, p_42832_, p_353025_ -> p_353025_.getLeashHolder() == p_42830_);

      for (Leashable $$5 : $$4) {
         if ($$3 == null) {
            $$3 = LeashFenceKnotEntity.getOrCreateKnot(p_42831_, p_42832_);
            $$3.playPlacementSound();
         }

         $$5.setLeashedTo($$3, true);
      }

      if (!$$4.isEmpty()) {
         p_42831_.gameEvent(GameEvent.BLOCK_ATTACH, p_42832_, GameEvent.Context.of(p_42830_));
         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.PASS;
      }
   }

   public static List<Leashable> leashableInArea(Level p_353033_, BlockPos p_353031_, Predicate<Leashable> p_353062_) {
      double $$3 = 7.0;
      int $$4 = p_353031_.getX();
      int $$5 = p_353031_.getY();
      int $$6 = p_353031_.getZ();
      AABB $$7 = new AABB($$4 - 7.0, $$5 - 7.0, $$6 - 7.0, $$4 + 7.0, $$5 + 7.0, $$6 + 7.0);
      return p_353033_.getEntitiesOfClass(Entity.class, $$7, p_353023_ -> p_353023_ instanceof Leashable $$2 && p_353062_.test($$2))
         .stream()
         .map(Leashable.class::cast)
         .toList();
   }
}
