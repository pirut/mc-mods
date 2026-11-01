package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;

public class OpCommand {
   private static final SimpleCommandExceptionType ERROR_ALREADY_OP = new SimpleCommandExceptionType(Component.translatable("commands.op.failed"));

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("op").requires(Commands.hasPermission(Commands.LEVEL_ADMINS)))
            .then(
               Commands.argument("targets", GameProfileArgument.gameProfile())
                  .suggests(
                     (commandContext, suggestionsBuilder) -> {
                        PlayerList playerList = ((CommandSourceStack)commandContext.getSource()).getServer().getPlayerList();
                        return SharedSuggestionProvider.suggest(
                           playerList.getPlayers()
                              .stream()
                              .filter(serverPlayer -> !playerList.isOp(serverPlayer.nameAndId()))
                              .map(serverPlayer -> serverPlayer.getGameProfile().name()),
                           suggestionsBuilder
                        );
                     }
                  )
                  .executes(
                     commandContext -> opPlayers((CommandSourceStack)commandContext.getSource(), GameProfileArgument.getGameProfiles(commandContext, "targets"))
                  )
            )
      );
   }

   private static int opPlayers(CommandSourceStack source, Collection<NameAndId> gameProfiles) throws CommandSyntaxException {
      PlayerList playerList = source.getServer().getPlayerList();
      int i = 0;

      for (NameAndId nameAndId : gameProfiles) {
         if (!playerList.isOp(nameAndId)) {
            playerList.op(nameAndId);
            i++;
            source.sendSuccess(() -> Component.translatable("commands.op.success", nameAndId.name()), true);
         }
      }

      if (i == 0) {
         throw ERROR_ALREADY_OP.create();
      } else {
         return i;
      }
   }
}
