package net.minecraft.world.item;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ArmorStandItem extends Item {
   public ArmorStandItem(Item.Properties p_40503_) {
      super(p_40503_);
   }

   @Override
   public InteractionResult useOn(UseOnContext p_40510_) {
      Direction $$1 = p_40510_.getClickedFace();
      if ($$1 == Direction.DOWN) {
         return InteractionResult.FAIL;
      } else {
         Level $$2 = p_40510_.getLevel();
         BlockPlaceContext $$3 = new BlockPlaceContext(p_40510_);
         BlockPos $$4 = $$3.getClickedPos();
         ItemStack $$5 = p_40510_.getItemInHand();
         Vec3 $$6 = Vec3.atBottomCenterOf($$4);
         AABB $$7 = EntityType.ARMOR_STAND.getDimensions().makeBoundingBox($$6.x(), $$6.y(), $$6.z());
         if ($$2.noCollision(null, $$7) && $$2.getEntities(null, $$7).isEmpty()) {
            if ($$2 instanceof ServerLevel $$8) {
               Consumer<ArmorStand> $$9 = EntityType.createDefaultStackConfig($$8, $$5, p_40510_.getPlayer());
               ArmorStand $$10 = EntityType.ARMOR_STAND.create($$8, $$9, $$4, MobSpawnType.SPAWN_EGG, true, true);
               if ($$10 == null) {
                  return InteractionResult.FAIL;
               }

               float $$11 = Mth.floor((Mth.wrapDegrees(p_40510_.getRotation() - 180.0F) + 22.5F) / 45.0F) * 45.0F;
               $$10.moveTo($$10.getX(), $$10.getY(), $$10.getZ(), $$11, 0.0F);
               $$8.addFreshEntityWithPassengers($$10);
               $$2.playSound(null, $$10.getX(), $$10.getY(), $$10.getZ(), SoundEvents.ARMOR_STAND_PLACE, SoundSource.BLOCKS, 0.75F, 0.8F);
               $$10.gameEvent(GameEvent.ENTITY_PLACE, p_40510_.getPlayer());
            }

            $$5.shrink(1);
            return InteractionResult.sidedSuccess($$2.isClientSide);
         } else {
            return InteractionResult.FAIL;
         }
      }
   }
}
