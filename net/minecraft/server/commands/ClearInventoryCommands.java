package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Predicate;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.item.ItemPredicateArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class ClearInventoryCommands {
   private static final DynamicCommandExceptionType ERROR_SINGLE = new DynamicCommandExceptionType(
      p_304193_ -> Component.translatableEscape("clear.failed.single", p_304193_)
   );
   private static final DynamicCommandExceptionType ERROR_MULTIPLE = new DynamicCommandExceptionType(
      p_304192_ -> Component.translatableEscape("clear.failed.multiple", p_304192_)
   );

   public static void register(CommandDispatcher<CommandSourceStack> p_214421_, CommandBuildContext p_214422_) {
      p_214421_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("clear").requires(p_136704_ -> p_136704_.hasPermission(2)))
               .executes(
                  p_332568_ -> clearUnlimited(
                     (CommandSourceStack)p_332568_.getSource(),
                     Collections.singleton(((CommandSourceStack)p_332568_.getSource()).getPlayerOrException()),
                     p_180029_ -> true
                  )
               ))
            .then(
               ((RequiredArgumentBuilder)Commands.argument("targets", EntityArgument.players())
                     .executes(
                        p_332566_ -> clearUnlimited(
                           (CommandSourceStack)p_332566_.getSource(), EntityArgument.getPlayers(p_332566_, "targets"), p_180027_ -> true
                        )
                     ))
                  .then(
                     ((RequiredArgumentBuilder)Commands.argument("item", ItemPredicateArgument.itemPredicate(p_214422_))
                           .executes(
                              p_332567_ -> clearUnlimited(
                                 (CommandSourceStack)p_332567_.getSource(),
                                 EntityArgument.getPlayers(p_332567_, "targets"),
                                 ItemPredicateArgument.getItemPredicate(p_332567_, "item")
                              )
                           ))
                        .then(
                           Commands.argument("maxCount", IntegerArgumentType.integer(0))
                              .executes(
                                 p_323185_ -> clearInventory(
                                    (CommandSourceStack)p_323185_.getSource(),
                                    EntityArgument.getPlayers(p_323185_, "targets"),
                                    ItemPredicateArgument.getItemPredicate(p_323185_, "item"),
                                    IntegerArgumentType.getInteger(p_323185_, "maxCount")
                                 )
                              )
                        )
                  )
            )
      );
   }

   private static int clearUnlimited(CommandSourceStack p_332708_, Collection<ServerPlayer> p_332807_, Predicate<ItemStack> p_332684_) throws CommandSyntaxException {
      return clearInventory(p_332708_, p_332807_, p_332684_, -1);
   }

   private static int clearInventory(CommandSourceStack p_136706_, Collection<ServerPlayer> p_136707_, Predicate<ItemStack> p_136708_, int p_136709_) throws CommandSyntaxException {
      int $$4 = 0;

      for (ServerPlayer $$5 : p_136707_) {
         $$4 += $$5.getInventory().clearOrCountMatchingItems(p_136708_, p_136709_, $$5.inventoryMenu.getCraftSlots());
         $$5.containerMenu.broadcastChanges();
         $$5.inventoryMenu.slotsChanged($$5.getInventory());
      }

      if ($$4 == 0) {
         if (p_136707_.size() == 1) {
            throw ERROR_SINGLE.create(p_136707_.iterator().next().getName());
         } else {
            throw ERROR_MULTIPLE.create(p_136707_.size());
         }
      } else {
         int $$6 = $$4;
         if (p_136709_ == 0) {
            if (p_136707_.size() == 1) {
               p_136706_.sendSuccess(() -> Component.translatable("commands.clear.test.single", $$6, p_136707_.iterator().next().getDisplayName()), true);
            } else {
               p_136706_.sendSuccess(() -> Component.translatable("commands.clear.test.multiple", $$6, p_136707_.size()), true);
            }
         } else if (p_136707_.size() == 1) {
            p_136706_.sendSuccess(() -> Component.translatable("commands.clear.success.single", $$6, p_136707_.iterator().next().getDisplayName()), true);
         } else {
            p_136706_.sendSuccess(() -> Component.translatable("commands.clear.success.multiple", $$6, p_136707_.size()), true);
         }

         return $$4;
      }
   }
}
