package net.minecraft.world.item;

import java.util.List;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public class PotionItem extends Item {
   private static final int DRINK_DURATION = 32;

   public PotionItem(Item.Properties p_42979_) {
      super(p_42979_);
   }

   @Override
   public ItemStack getDefaultInstance() {
      ItemStack $$0 = super.getDefaultInstance();
      $$0.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.WATER));
      return $$0;
   }

   @Override
   public ItemStack finishUsingItem(ItemStack p_42984_, Level p_42985_, LivingEntity p_42986_) {
      Player $$3 = p_42986_ instanceof Player ? (Player)p_42986_ : null;
      if ($$3 instanceof ServerPlayer) {
         CriteriaTriggers.CONSUME_ITEM.trigger((ServerPlayer)$$3, p_42984_);
      }

      if (!p_42985_.isClientSide) {
         PotionContents $$4 = p_42984_.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
         $$4.forEachEffect(p_330883_ -> {
            if (p_330883_.getEffect().value().isInstantenous()) {
               p_330883_.getEffect().value().applyInstantenousEffect($$3, $$3, p_42986_, p_330883_.getAmplifier(), 1.0);
            } else {
               p_42986_.addEffect(p_330883_);
            }
         });
      }

      if ($$3 != null) {
         $$3.awardStat(Stats.ITEM_USED.get(this));
         p_42984_.consume(1, $$3);
      }

      if ($$3 == null || !$$3.hasInfiniteMaterials()) {
         if (p_42984_.isEmpty()) {
            return new ItemStack(Items.GLASS_BOTTLE);
         }

         if ($$3 != null) {
            $$3.getInventory().add(new ItemStack(Items.GLASS_BOTTLE));
         }
      }

      p_42986_.gameEvent(GameEvent.DRINK);
      return p_42984_;
   }

   @Override
   public InteractionResult useOn(UseOnContext p_220235_) {
      Level $$1 = p_220235_.getLevel();
      BlockPos $$2 = p_220235_.getClickedPos();
      Player $$3 = p_220235_.getPlayer();
      ItemStack $$4 = p_220235_.getItemInHand();
      PotionContents $$5 = $$4.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
      BlockState $$6 = $$1.getBlockState($$2);
      if (p_220235_.getClickedFace() != Direction.DOWN && $$6.is(BlockTags.CONVERTABLE_TO_MUD) && $$5.is(Potions.WATER)) {
         $$1.playSound(null, $$2, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 1.0F, 1.0F);
         $$3.setItemInHand(p_220235_.getHand(), ItemUtils.createFilledResult($$4, $$3, new ItemStack(Items.GLASS_BOTTLE)));
         $$3.awardStat(Stats.ITEM_USED.get($$4.getItem()));
         if (!$$1.isClientSide) {
            ServerLevel $$7 = (ServerLevel)$$1;

            for (int $$8 = 0; $$8 < 5; $$8++) {
               $$7.sendParticles(
                  ParticleTypes.SPLASH, $$2.getX() + $$1.random.nextDouble(), $$2.getY() + 1, $$2.getZ() + $$1.random.nextDouble(), 1, 0.0, 0.0, 0.0, 1.0
               );
            }
         }

         $$1.playSound(null, $$2, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
         $$1.gameEvent(null, GameEvent.FLUID_PLACE, $$2);
         $$1.setBlockAndUpdate($$2, Blocks.MUD.defaultBlockState());
         return InteractionResult.sidedSuccess($$1.isClientSide);
      } else {
         return InteractionResult.PASS;
      }
   }

   @Override
   public int getUseDuration(ItemStack p_43001_, LivingEntity p_346041_) {
      return 32;
   }

   @Override
   public UseAnim getUseAnimation(ItemStack p_42997_) {
      return UseAnim.DRINK;
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level p_42993_, Player p_42994_, InteractionHand p_42995_) {
      return ItemUtils.startUsingInstantly(p_42993_, p_42994_, p_42995_);
   }

   @Override
   public String getDescriptionId(ItemStack p_43003_) {
      return Potion.getName(p_43003_.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).potion(), this.getDescriptionId() + ".effect.");
   }

   @Override
   public void appendHoverText(ItemStack p_42988_, Item.TooltipContext p_339608_, List<Component> p_42990_, TooltipFlag p_42991_) {
      PotionContents $$4 = p_42988_.get(DataComponents.POTION_CONTENTS);
      if ($$4 != null) {
         $$4.addPotionTooltip(p_42990_::add, 1.0F, p_339608_.tickRate());
      }
   }
}
