package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

public class MsgCommand {
   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      LiteralCommandNode<CommandSourceStack> literalCommandNode = dispatcher.register(
         (LiteralArgumentBuilder)Commands.literal("msg")
            .then(
               Commands.argument("targets", EntityArgument.players())
                  .then(
                     Commands.argument("message", MessageArgument.message())
                        .executes(
                           commandContext -> {
                              Collection<ServerPlayer> collection = EntityArgument.getPlayers(commandContext, "targets");
                              if (!collection.isEmpty()) {
                                 MessageArgument.resolveChatMessage(
                                    commandContext,
                                    "message",
                                    playerChatMessage -> sendMessage((CommandSourceStack)commandContext.getSource(), collection, playerChatMessage)
                                 );
                              }

                              return collection.size();
                           }
                        )
                  )
            )
      );
      dispatcher.register((LiteralArgumentBuilder)Commands.literal("tell").redirect(literalCommandNode));
      dispatcher.register((LiteralArgumentBuilder)Commands.literal("w").redirect(literalCommandNode));
   }

   private static void sendMessage(CommandSourceStack source, Collection<ServerPlayer> targets, PlayerChatMessage message) {
      ChatType.Bound bound = ChatType.bind(ChatType.MSG_COMMAND_INCOMING, source);
      OutgoingChatMessage outgoingChatMessage = OutgoingChatMessage.create(message);
      boolean bl = false;

      for (ServerPlayer serverPlayer : targets) {
         ChatType.Bound bound2 = ChatType.bind(ChatType.MSG_COMMAND_OUTGOING, source).withTargetName(serverPlayer.getDisplayName());
         source.sendChatMessage(outgoingChatMessage, false, bound2);
         boolean bl2 = source.shouldFilterMessageTo(serverPlayer);
         serverPlayer.sendChatMessage(outgoingChatMessage, bl2, bound);
         bl |= bl2 && message.isFullyFiltered();
      }

      if (bl) {
         source.sendSystemMessage(PlayerList.CHAT_FILTERED_FULL);
      }
   }
}
