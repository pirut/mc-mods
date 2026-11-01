package net.minecraft.world.item;

import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

public class HangingEntityItem extends Item {
   private static final Component TOOLTIP_RANDOM_VARIANT = Component.translatable("painting.random").withStyle(ChatFormatting.GRAY);
   private final EntityType<? extends HangingEntity> type;

   public HangingEntityItem(EntityType<? extends HangingEntity> p_41324_, Item.Properties p_41325_) {
      super(p_41325_);
      this.type = p_41324_;
   }

   @Override
   public InteractionResult useOn(UseOnContext p_41331_) {
      BlockPos $$1 = p_41331_.getClickedPos();
      Direction $$2 = p_41331_.getClickedFace();
      BlockPos $$3 = $$1.relative($$2);
      Player $$4 = p_41331_.getPlayer();
      ItemStack $$5 = p_41331_.getItemInHand();
      if ($$4 != null && !this.mayPlace($$4, $$2, $$5, $$3)) {
         return InteractionResult.FAIL;
      } else {
         Level $$6 = p_41331_.getLevel();
         HangingEntity $$8;
         if (this.type == EntityType.PAINTING) {
            Optional<Painting> $$7 = Painting.create($$6, $$3, $$2);
            if ($$7.isEmpty()) {
               return InteractionResult.CONSUME;
            }

            $$8 = $$7.get();
         } else if (this.type == EntityType.ITEM_FRAME) {
            $$8 = new ItemFrame($$6, $$3, $$2);
         } else {
            if (this.type != EntityType.GLOW_ITEM_FRAME) {
               return InteractionResult.sidedSuccess($$6.isClientSide);
            }

            $$8 = new GlowItemFrame($$6, $$3, $$2);
         }

         CustomData $$12 = $$5.getOrDefault(DataComponents.ENTITY_DATA, CustomData.EMPTY);
         if (!$$12.isEmpty()) {
            EntityType.updateCustomEntityTag($$6, $$4, $$8, $$12);
         }

         if ($$8.survives()) {
            if (!$$6.isClientSide) {
               $$8.playPlacementSound();
               $$6.gameEvent($$4, GameEvent.ENTITY_PLACE, $$8.position());
               $$6.addFreshEntity($$8);
            }

            $$5.shrink(1);
            return InteractionResult.sidedSuccess($$6.isClientSide);
         } else {
            return InteractionResult.CONSUME;
         }
      }
   }

   protected boolean mayPlace(Player p_41326_, Direction p_41327_, ItemStack p_41328_, BlockPos p_41329_) {
      return !p_41327_.getAxis().isVertical() && p_41326_.mayUseItemAt(p_41329_, p_41327_, p_41328_);
   }

   @Override
   public void appendHoverText(ItemStack p_270235_, Item.TooltipContext p_339644_, List<Component> p_270630_, TooltipFlag p_270170_) {
      super.appendHoverText(p_270235_, p_339644_, p_270630_, p_270170_);
      HolderLookup.Provider $$4 = p_339644_.registries();
      if ($$4 != null && this.type == EntityType.PAINTING) {
         CustomData $$5 = p_270235_.getOrDefault(DataComponents.ENTITY_DATA, CustomData.EMPTY);
         if (!$$5.isEmpty()) {
            $$5.read($$4.createSerializationContext(NbtOps.INSTANCE), Painting.VARIANT_MAP_CODEC)
               .result()
               .ifPresentOrElse(
                  p_344401_ -> {
                     p_344401_.unwrapKey().ifPresent(p_270217_ -> {
                        p_270630_.add(Component.translatable(p_270217_.location().toLanguageKey("painting", "title")).withStyle(ChatFormatting.YELLOW));
                        p_270630_.add(Component.translatable(p_270217_.location().toLanguageKey("painting", "author")).withStyle(ChatFormatting.GRAY));
                     });
                     p_270630_.add(
                        Component.translatable(
                           "painting.dimensions", ((PaintingVariant)p_344401_.value()).width(), ((PaintingVariant)p_344401_.value()).height()
                        )
                     );
                  },
                  () -> p_270630_.add(TOOLTIP_RANDOM_VARIANT)
               );
         } else if (p_270170_.isCreative()) {
            p_270630_.add(TOOLTIP_RANDOM_VARIANT);
         }
      }
   }
}
