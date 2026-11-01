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
import net.minecraft.server.players.UserWhiteList;
import net.minecraft.server.players.UserWhiteListEntry;
import net.minecraft.world.entity.player.Player;

public class WhitelistCommand {
   private static final SimpleCommandExceptionType ERROR_ALREADY_ENABLED = new SimpleCommandExceptionType(
      Component.translatable("commands.whitelist.alreadyOn")
   );
   private static final SimpleCommandExceptionType ERROR_ALREADY_DISABLED = new SimpleCommandExceptionType(
      Component.translatable("commands.whitelist.alreadyOff")
   );
   private static final SimpleCommandExceptionType ERROR_ALREADY_WHITELISTED = new SimpleCommandExceptionType(
      Component.translatable("commands.whitelist.add.failed")
   );
   private static final SimpleCommandExceptionType ERROR_NOT_WHITELISTED = new SimpleCommandExceptionType(
      Component.translatable("commands.whitelist.remove.failed")
   );

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(
                                 "whitelist"
                              )
                              .requires(Commands.hasPermission(Commands.LEVEL_ADMINS)))
                           .then(Commands.literal("on").executes(commandContext -> enableWhitelist((CommandSourceStack)commandContext.getSource()))))
                        .then(Commands.literal("off").executes(commandContext -> disableWhitelist((CommandSourceStack)commandContext.getSource()))))
                     .then(Commands.literal("list").executes(commandContext -> showList((CommandSourceStack)commandContext.getSource()))))
                  .then(
                     Commands.literal("add")
                        .then(
                           Commands.argument("targets", GameProfileArgument.gameProfile())
                              .suggests(
                                 (commandContext, suggestionsBuilder) -> {
                                    PlayerList playerList = ((CommandSourceStack)commandContext.getSource()).getServer().getPlayerList();
                                    return SharedSuggestionProvider.suggest(
                                       playerList.getPlayers()
                                          .stream()
                                          .map(Player::nameAndId)
                                          .filter(nameAndId -> !playerList.getWhiteList().isWhiteListed(nameAndId))
                                          .map(NameAndId::name),
                                       suggestionsBuilder
                                    );
                                 }
                              )
                              .executes(
                                 commandContext -> addPlayers(
                                    (CommandSourceStack)commandContext.getSource(), GameProfileArgument.getGameProfiles(commandContext, "targets")
                                 )
                              )
                        )
                  ))
               .then(
                  Commands.literal("remove")
                     .then(
                        Commands.argument("targets", GameProfileArgument.gameProfile())
                           .suggests(
                              (commandContext, suggestionsBuilder) -> SharedSuggestionProvider.suggest(
                                 ((CommandSourceStack)commandContext.getSource()).getServer().getPlayerList().getWhiteListNames(), suggestionsBuilder
                              )
                           )
                           .executes(
                              commandContext -> removePlayers(
                                 (CommandSourceStack)commandContext.getSource(), GameProfileArgument.getGameProfiles(commandContext, "targets")
                              )
                           )
                     )
               ))
            .then(Commands.literal("reload").executes(commandContext -> reload((CommandSourceStack)commandContext.getSource())))
      );
   }

   private static int reload(CommandSourceStack source) {
      source.getServer().getPlayerList().reloadWhiteList();
      source.sendSuccess(() -> Component.translatable("commands.whitelist.reloaded"), true);
      source.getServer().kickUnlistedPlayers();
      return 1;
   }

   private static int addPlayers(CommandSourceStack source, Collection<NameAndId> players) throws CommandSyntaxException {
      UserWhiteList userWhiteList = source.getServer().getPlayerList().getWhiteList();
      int i = 0;

      for (NameAndId nameAndId : players) {
         if (!userWhiteList.isWhiteListed(nameAndId)) {
            UserWhiteListEntry userWhiteListEntry = new UserWhiteListEntry(nameAndId);
            userWhiteList.add(userWhiteListEntry);
            source.sendSuccess(() -> Component.translatable("commands.whitelist.add.success", Component.literal(nameAndId.name())), true);
            i++;
         }
      }

      if (i == 0) {
         throw ERROR_ALREADY_WHITELISTED.create();
      } else {
         return i;
      }
   }

   private static int removePlayers(CommandSourceStack source, Collection<NameAndId> players) throws CommandSyntaxException {
      UserWhiteList userWhiteList = source.getServer().getPlayerList().getWhiteList();
      int i = 0;

      for (NameAndId nameAndId : players) {
         if (userWhiteList.isWhiteListed(nameAndId)) {
            UserWhiteListEntry userWhiteListEntry = new UserWhiteListEntry(nameAndId);
            userWhiteList.remove(userWhiteListEntry);
            source.sendSuccess(() -> Component.translatable("commands.whitelist.remove.success", Component.literal(nameAndId.name())), true);
            i++;
         }
      }

      if (i == 0) {
         throw ERROR_NOT_WHITELISTED.create();
      } else {
         source.getServer().kickUnlistedPlayers();
         return i;
      }
   }

   private static int enableWhitelist(CommandSourceStack source) throws CommandSyntaxException {
      if (source.getServer().isUsingWhitelist()) {
         throw ERROR_ALREADY_ENABLED.create();
      } else {
         source.getServer().setUsingWhitelist(true);
         source.sendSuccess(() -> Component.translatable("commands.whitelist.enabled"), true);
         source.getServer().kickUnlistedPlayers();
         return 1;
      }
   }

   private static int disableWhitelist(CommandSourceStack source) throws CommandSyntaxException {
      if (!source.getServer().isUsingWhitelist()) {
         throw ERROR_ALREADY_DISABLED.create();
      } else {
         source.getServer().setUsingWhitelist(false);
         source.sendSuccess(() -> Component.translatable("commands.whitelist.disabled"), true);
         return 1;
      }
   }

   private static int showList(CommandSourceStack source) {
      String[] strings = source.getServer().getPlayerList().getWhiteListNames();
      if (strings.length == 0) {
         source.sendSuccess(() -> Component.translatable("commands.whitelist.none"), false);
      } else {
         source.sendSuccess(() -> Component.translatable("commands.whitelist.list", strings.length, String.join(", ", strings)), false);
      }

      return strings.length;
   }
}
