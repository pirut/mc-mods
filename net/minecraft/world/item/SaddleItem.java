package net.minecraft.world.item;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Saddleable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gameevent.GameEvent;

public class SaddleItem extends Item {
   public SaddleItem(Item.Properties p_43053_) {
      super(p_43053_);
   }

   @Override
   public InteractionResult interactLivingEntity(ItemStack p_43055_, Player p_43056_, LivingEntity p_43057_, InteractionHand p_43058_) {
      if (p_43057_ instanceof Saddleable $$4 && p_43057_.isAlive() && !$$4.isSaddled() && $$4.isSaddleable()) {
         if (!p_43056_.level().isClientSide) {
            $$4.equipSaddle(p_43055_.split(1), SoundSource.NEUTRAL);
            p_43057_.level().gameEvent(p_43057_, GameEvent.EQUIP, p_43057_.position());
         }

         return InteractionResult.sidedSuccess(p_43056_.level().isClientSide);
      } else {
         return InteractionResult.PASS;
      }
   }
}
