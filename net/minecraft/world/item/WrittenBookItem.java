package net.minecraft.world.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.Level;

public class WrittenBookItem extends Item {
   public WrittenBookItem(Item.Properties p_43455_) {
      super(p_43455_);
   }

   @Override
   public Component getName(ItemStack p_43480_) {
      WrittenBookContent $$1 = p_43480_.get(DataComponents.WRITTEN_BOOK_CONTENT);
      if ($$1 != null) {
         String $$2 = $$1.title().raw();
         if (!StringUtil.isBlank($$2)) {
            return Component.literal($$2);
         }
      }

      return super.getName(p_43480_);
   }

   @Override
   public void appendHoverText(ItemStack p_43457_, Item.TooltipContext p_339643_, List<Component> p_43459_, TooltipFlag p_43460_) {
      WrittenBookContent $$4 = p_43457_.get(DataComponents.WRITTEN_BOOK_CONTENT);
      if ($$4 != null) {
         if (!StringUtil.isBlank($$4.author())) {
            p_43459_.add(Component.translatable("book.byAuthor", $$4.author()).withStyle(ChatFormatting.GRAY));
         }

         p_43459_.add(Component.translatable("book.generation." + $$4.generation()).withStyle(ChatFormatting.GRAY));
      }
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level p_43468_, Player p_43469_, InteractionHand p_43470_) {
      ItemStack $$3 = p_43469_.getItemInHand(p_43470_);
      p_43469_.openItemGui($$3, p_43470_);
      p_43469_.awardStat(Stats.ITEM_USED.get(this));
      return InteractionResultHolder.sidedSuccess($$3, p_43468_.isClientSide());
   }

   public static boolean resolveBookComponents(ItemStack p_43462_, CommandSourceStack p_43463_, @Nullable Player p_43464_) {
      WrittenBookContent $$3 = p_43462_.get(DataComponents.WRITTEN_BOOK_CONTENT);
      if ($$3 != null && !$$3.resolved()) {
         WrittenBookContent $$4 = $$3.resolve(p_43463_, p_43464_);
         if ($$4 != null) {
            p_43462_.set(DataComponents.WRITTEN_BOOK_CONTENT, $$4);
            return true;
         }

         p_43462_.set(DataComponents.WRITTEN_BOOK_CONTENT, $$3.markResolved());
      }

      return false;
   }
}
