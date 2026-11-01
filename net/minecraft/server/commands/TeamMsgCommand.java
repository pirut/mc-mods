package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.PlayerTeam;

public class TeamMsgCommand {
   private static final Style SUGGEST_STYLE = Style.EMPTY
      .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.type.team.hover")))
      .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/teammsg "));
   private static final SimpleCommandExceptionType ERROR_NOT_ON_TEAM = new SimpleCommandExceptionType(Component.translatable("commands.teammsg.failed.noteam"));

   public static void register(CommandDispatcher<CommandSourceStack> p_139000_) {
      LiteralCommandNode<CommandSourceStack> $$1 = p_139000_.register(
         (LiteralArgumentBuilder)Commands.literal("teammsg")
            .then(
               Commands.argument("message", MessageArgument.message())
                  .executes(
                     p_248184_ -> {
                        CommandSourceStack $$1x = (CommandSourceStack)p_248184_.getSource();
                        Entity $$2 = $$1x.getEntityOrException();
                        PlayerTeam $$3 = $$2.getTeam();
                        if ($$3 == null) {
                           throw ERROR_NOT_ON_TEAM.create();
                        } else {
                           List<ServerPlayer> $$4 = $$1x.getServer()
                              .getPlayerList()
                              .getPlayers()
                              .stream()
                              .filter(p_349955_ -> p_349955_ == $$2 || p_349955_.getTeam() == $$3)
                              .toList();
                           if (!$$4.isEmpty()) {
                              MessageArgument.resolveChatMessage(p_248184_, "message", p_248180_ -> sendMessage($$1x, $$2, $$3, $$4, p_248180_));
                           }

                           return $$4.size();
                        }
                     }
                  )
            )
      );
      p_139000_.register((LiteralArgumentBuilder)Commands.literal("tm").redirect($$1));
   }

   private static void sendMessage(
      CommandSourceStack p_248778_, Entity p_248891_, PlayerTeam p_250504_, List<ServerPlayer> p_249706_, PlayerChatMessage p_249707_
   ) {
      Component $$5 = p_250504_.getFormattedDisplayName().withStyle(SUGGEST_STYLE);
      ChatType.Bound $$6 = ChatType.bind(ChatType.TEAM_MSG_COMMAND_INCOMING, p_248778_).withTargetName($$5);
      ChatType.Bound $$7 = ChatType.bind(ChatType.TEAM_MSG_COMMAND_OUTGOING, p_248778_).withTargetName($$5);
      OutgoingChatMessage $$8 = OutgoingChatMessage.create(p_249707_);
      boolean $$9 = false;

      for (ServerPlayer $$10 : p_249706_) {
         ChatType.Bound $$11 = $$10 == p_248891_ ? $$7 : $$6;
         boolean $$12 = p_248778_.shouldFilterMessageTo($$10);
         $$10.sendChatMessage($$8, $$12, $$11);
         $$9 |= $$12 && p_249707_.isFullyFiltered();
      }

      if ($$9) {
         p_248778_.sendSystemMessage(PlayerList.CHAT_FILTERED_FULL);
      }
   }
}
