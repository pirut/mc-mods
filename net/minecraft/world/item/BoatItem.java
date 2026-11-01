package net.minecraft.world.item;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class BoatItem extends Item {
   private static final Predicate<Entity> ENTITY_PREDICATE = EntitySelector.NO_SPECTATORS.and(Entity::isPickable);
   private final Boat.Type type;
   private final boolean hasChest;

   public BoatItem(boolean p_220013_, Boat.Type p_220014_, Item.Properties p_220015_) {
      super(p_220015_);
      this.hasChest = p_220013_;
      this.type = p_220014_;
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level p_40622_, Player p_40623_, InteractionHand p_40624_) {
      ItemStack $$3 = p_40623_.getItemInHand(p_40624_);
      HitResult $$4 = getPlayerPOVHitResult(p_40622_, p_40623_, ClipContext.Fluid.ANY);
      if ($$4.getType() == HitResult.Type.MISS) {
         return InteractionResultHolder.pass($$3);
      } else {
         Vec3 $$5 = p_40623_.getViewVector(1.0F);
         double $$6 = 5.0;
         List<Entity> $$7 = p_40622_.getEntities(p_40623_, p_40623_.getBoundingBox().expandTowards($$5.scale(5.0)).inflate(1.0), ENTITY_PREDICATE);
         if (!$$7.isEmpty()) {
            Vec3 $$8 = p_40623_.getEyePosition();

            for (Entity $$9 : $$7) {
               AABB $$10 = $$9.getBoundingBox().inflate($$9.getPickRadius());
               if ($$10.contains($$8)) {
                  return InteractionResultHolder.pass($$3);
               }
            }
         }

         if ($$4.getType() == HitResult.Type.BLOCK) {
            Boat $$11 = this.getBoat(p_40622_, $$4, $$3, p_40623_);
            $$11.setVariant(this.type);
            $$11.setYRot(p_40623_.getYRot());
            if (!p_40622_.noCollision($$11, $$11.getBoundingBox())) {
               return InteractionResultHolder.fail($$3);
            } else {
               if (!p_40622_.isClientSide) {
                  p_40622_.addFreshEntity($$11);
                  p_40622_.gameEvent(p_40623_, GameEvent.ENTITY_PLACE, $$4.getLocation());
                  $$3.consume(1, p_40623_);
               }

               p_40623_.awardStat(Stats.ITEM_USED.get(this));
               return InteractionResultHolder.sidedSuccess($$3, p_40622_.isClientSide());
            }
         } else {
            return InteractionResultHolder.pass($$3);
         }
      }
   }

   private Boat getBoat(Level p_220017_, HitResult p_220018_, ItemStack p_305852_, Player p_306127_) {
      Vec3 $$4 = p_220018_.getLocation();
      Boat $$5 = (Boat)(this.hasChest ? new ChestBoat(p_220017_, $$4.x, $$4.y, $$4.z) : new Boat(p_220017_, $$4.x, $$4.y, $$4.z));
      if (p_220017_ instanceof ServerLevel $$6) {
         EntityType.<Boat>createDefaultStackConfig($$6, p_305852_, p_306127_).accept($$5);
      }

      return $$5;
   }
}
