package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Collection;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceOrIdArgument;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundClearDialogPacket;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.level.ServerPlayer;

public class DialogCommand {
   public static void register(CommandDispatcher<CommandSourceStack> p_426198_, CommandBuildContext p_426290_) {
      p_426198_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("dialog")
                  .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
               .then(
                  Commands.literal("show")
                     .then(
                        Commands.argument("targets", EntityArgument.players())
                           .then(
                              Commands.argument("dialog", ResourceOrIdArgument.dialog(p_426290_))
                                 .executes(
                                    p_426297_ -> showDialog(
                                       (CommandSourceStack)p_426297_.getSource(),
                                       EntityArgument.getPlayers(p_426297_, "targets"),
                                       ResourceOrIdArgument.getDialog(p_426297_, "dialog")
                                    )
                                 )
                           )
                     )
               ))
            .then(
               Commands.literal("clear")
                  .then(
                     Commands.argument("targets", EntityArgument.players())
                        .executes(p_425811_ -> clearDialog((CommandSourceStack)p_425811_.getSource(), EntityArgument.getPlayers(p_425811_, "targets")))
                  )
            )
      );
   }

   private static int showDialog(CommandSourceStack p_425549_, Collection<ServerPlayer> p_425743_, Holder<Dialog> p_426279_) {
      for (ServerPlayer serverplayer : p_425743_) {
         serverplayer.openDialog(p_426279_);
      }

      if (p_425743_.size() == 1) {
         p_425549_.sendSuccess(() -> Component.translatable("commands.dialog.show.single", p_425743_.iterator().next().getDisplayName()), true);
      } else {
         p_425549_.sendSuccess(() -> Component.translatable("commands.dialog.show.multiple", p_425743_.size()), true);
      }

      return p_425743_.size();
   }

   private static int clearDialog(CommandSourceStack p_426304_, Collection<ServerPlayer> p_425553_) {
      for (ServerPlayer serverplayer : p_425553_) {
         serverplayer.connection.send(ClientboundClearDialogPacket.INSTANCE);
      }

      if (p_425553_.size() == 1) {
         p_426304_.sendSuccess(() -> Component.translatable("commands.dialog.clear.single", p_425553_.iterator().next().getDisplayName()), true);
      } else {
         p_426304_.sendSuccess(() -> Component.translatable("commands.dialog.clear.multiple", p_425553_.size()), true);
      }

      return p_425553_.size();
   }
}
