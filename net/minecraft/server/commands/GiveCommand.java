package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collection;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

public class GiveCommand {
   public static final int MAX_ALLOWED_ITEMSTACKS = 100;

   public static void register(CommandDispatcher<CommandSourceStack> p_214446_, CommandBuildContext p_214447_) {
      p_214446_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("give").requires(p_137777_ -> p_137777_.hasPermission(2)))
            .then(
               Commands.argument("targets", EntityArgument.players())
                  .then(
                     ((RequiredArgumentBuilder)Commands.argument("item", ItemArgument.item(p_214447_))
                           .executes(
                              p_137784_ -> giveItem(
                                 (CommandSourceStack)p_137784_.getSource(),
                                 ItemArgument.getItem(p_137784_, "item"),
                                 EntityArgument.getPlayers(p_137784_, "targets"),
                                 1
                              )
                           ))
                        .then(
                           Commands.argument("count", IntegerArgumentType.integer(1))
                              .executes(
                                 p_137775_ -> giveItem(
                                    (CommandSourceStack)p_137775_.getSource(),
                                    ItemArgument.getItem(p_137775_, "item"),
                                    EntityArgument.getPlayers(p_137775_, "targets"),
                                    IntegerArgumentType.getInteger(p_137775_, "count")
                                 )
                              )
                        )
                  )
            )
      );
   }

   private static int giveItem(CommandSourceStack p_137779_, ItemInput p_137780_, Collection<ServerPlayer> p_137781_, int p_137782_) throws CommandSyntaxException {
      ItemStack $$4 = p_137780_.createItemStack(1, false);
      int $$5 = $$4.getMaxStackSize();
      int $$6 = $$5 * 100;
      if (p_137782_ > $$6) {
         p_137779_.sendFailure(Component.translatable("commands.give.failed.toomanyitems", $$6, $$4.getDisplayName()));
         return 0;
      } else {
         for (ServerPlayer $$7 : p_137781_) {
            int $$8 = p_137782_;

            while ($$8 > 0) {
               int $$9 = Math.min($$5, $$8);
               $$8 -= $$9;
               ItemStack $$10 = p_137780_.createItemStack($$9, false);
               boolean $$11 = $$7.getInventory().add($$10);
               if ($$11 && $$10.isEmpty()) {
                  ItemEntity $$13 = $$7.drop($$4, false);
                  if ($$13 != null) {
                     $$13.makeFakeItem();
                  }

                  $$7.level()
                     .playSound(
                        null,
                        $$7.getX(),
                        $$7.getY(),
                        $$7.getZ(),
                        SoundEvents.ITEM_PICKUP,
                        SoundSource.PLAYERS,
                        0.2F,
                        (($$7.getRandom().nextFloat() - $$7.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F
                     );
                  $$7.containerMenu.broadcastChanges();
               } else {
                  ItemEntity $$12 = $$7.drop($$10, false);
                  if ($$12 != null) {
                     $$12.setNoPickUpDelay();
                     $$12.setTarget($$7.getUUID());
                  }
               }
            }
         }

         if (p_137781_.size() == 1) {
            p_137779_.sendSuccess(
               () -> Component.translatable("commands.give.success.single", p_137782_, $$4.getDisplayName(), p_137781_.iterator().next().getDisplayName()),
               true
            );
         } else {
            p_137779_.sendSuccess(() -> Component.translatable("commands.give.success.single", p_137782_, $$4.getDisplayName(), p_137781_.size()), true);
         }

         return p_137781_.size();
      }
   }
}
