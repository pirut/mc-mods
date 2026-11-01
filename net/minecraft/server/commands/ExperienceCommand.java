package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.ToIntFunction;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

public class ExperienceCommand {
   private static final SimpleCommandExceptionType ERROR_SET_POINTS_INVALID = new SimpleCommandExceptionType(
      Component.translatable("commands.experience.set.points.invalid")
   );

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      LiteralCommandNode<CommandSourceStack> literalCommandNode = dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("experience")
                     .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
                  .then(
                     Commands.literal("add")
                        .then(
                           Commands.argument("target", EntityArgument.players())
                              .then(
                                 ((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("amount", IntegerArgumentType.integer())
                                          .executes(
                                             commandContext -> addExperience(
                                                (CommandSourceStack)commandContext.getSource(),
                                                EntityArgument.getPlayers(commandContext, "target"),
                                                IntegerArgumentType.getInteger(commandContext, "amount"),
                                                ExperienceCommand.Type.POINTS
                                             )
                                          ))
                                       .then(
                                          Commands.literal("points")
                                             .executes(
                                                commandContext -> addExperience(
                                                   (CommandSourceStack)commandContext.getSource(),
                                                   EntityArgument.getPlayers(commandContext, "target"),
                                                   IntegerArgumentType.getInteger(commandContext, "amount"),
                                                   ExperienceCommand.Type.POINTS
                                                )
                                             )
                                       ))
                                    .then(
                                       Commands.literal("levels")
                                          .executes(
                                             commandContext -> addExperience(
                                                (CommandSourceStack)commandContext.getSource(),
                                                EntityArgument.getPlayers(commandContext, "target"),
                                                IntegerArgumentType.getInteger(commandContext, "amount"),
                                                ExperienceCommand.Type.LEVELS
                                             )
                                          )
                                    )
                              )
                        )
                  ))
               .then(
                  Commands.literal("set")
                     .then(
                        Commands.argument("target", EntityArgument.players())
                           .then(
                              ((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("amount", IntegerArgumentType.integer(0))
                                       .executes(
                                          commandContext -> setExperience(
                                             (CommandSourceStack)commandContext.getSource(),
                                             EntityArgument.getPlayers(commandContext, "target"),
                                             IntegerArgumentType.getInteger(commandContext, "amount"),
                                             ExperienceCommand.Type.POINTS
                                          )
                                       ))
                                    .then(
                                       Commands.literal("points")
                                          .executes(
                                             commandContext -> setExperience(
                                                (CommandSourceStack)commandContext.getSource(),
                                                EntityArgument.getPlayers(commandContext, "target"),
                                                IntegerArgumentType.getInteger(commandContext, "amount"),
                                                ExperienceCommand.Type.POINTS
                                             )
                                          )
                                    ))
                                 .then(
                                    Commands.literal("levels")
                                       .executes(
                                          commandContext -> setExperience(
                                             (CommandSourceStack)commandContext.getSource(),
                                             EntityArgument.getPlayers(commandContext, "target"),
                                             IntegerArgumentType.getInteger(commandContext, "amount"),
                                             ExperienceCommand.Type.LEVELS
                                          )
                                       )
                                 )
                           )
                     )
               ))
            .then(
               Commands.literal("query")
                  .then(
                     ((RequiredArgumentBuilder)Commands.argument("target", EntityArgument.player())
                           .then(
                              Commands.literal("points")
                                 .executes(
                                    commandContext -> queryExperience(
                                       (CommandSourceStack)commandContext.getSource(),
                                       EntityArgument.getPlayer(commandContext, "target"),
                                       ExperienceCommand.Type.POINTS
                                    )
                                 )
                           ))
                        .then(
                           Commands.literal("levels")
                              .executes(
                                 commandContext -> queryExperience(
                                    (CommandSourceStack)commandContext.getSource(),
                                    EntityArgument.getPlayer(commandContext, "target"),
                                    ExperienceCommand.Type.LEVELS
                                 )
                              )
                        )
                  )
            )
      );
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("xp").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
            .redirect(literalCommandNode)
      );
   }

   private static int queryExperience(CommandSourceStack source, ServerPlayer player, ExperienceCommand.Type type) {
      int i = type.query.applyAsInt(player);
      source.sendSuccess(() -> Component.translatable("commands.experience.query." + type.name, player.getDisplayName(), i), false);
      return i;
   }

   private static int addExperience(CommandSourceStack source, Collection<? extends ServerPlayer> targets, int amount, ExperienceCommand.Type type) {
      for (ServerPlayer serverPlayer : targets) {
         type.add.accept(serverPlayer, amount);
      }

      if (targets.size() == 1) {
         source.sendSuccess(
            () -> Component.translatable("commands.experience.add." + type.name + ".success.single", amount, targets.iterator().next().getDisplayName()), true
         );
      } else {
         source.sendSuccess(() -> Component.translatable("commands.experience.add." + type.name + ".success.multiple", amount, targets.size()), true);
      }

      return targets.size();
   }

   private static int setExperience(CommandSourceStack source, Collection<? extends ServerPlayer> targets, int amount, ExperienceCommand.Type type) throws CommandSyntaxException {
      int i = 0;

      for (ServerPlayer serverPlayer : targets) {
         if (type.set.test(serverPlayer, amount)) {
            i++;
         }
      }

      if (i == 0) {
         throw ERROR_SET_POINTS_INVALID.create();
      } else {
         if (targets.size() == 1) {
            source.sendSuccess(
               () -> Component.translatable("commands.experience.set." + type.name + ".success.single", amount, targets.iterator().next().getDisplayName()),
               true
            );
         } else {
            source.sendSuccess(() -> Component.translatable("commands.experience.set." + type.name + ".success.multiple", amount, targets.size()), true);
         }

         return targets.size();
      }
   }

   static enum Type {
      POINTS("points", Player::giveExperiencePoints, (serverPlayer, integer) -> {
         if (integer >= serverPlayer.getXpNeededForNextLevel()) {
            return false;
         } else {
            serverPlayer.setExperiencePoints(integer);
            return true;
         }
      }, serverPlayer -> Mth.floor(serverPlayer.experienceProgress * serverPlayer.getXpNeededForNextLevel())),
      LEVELS("levels", ServerPlayer::giveExperienceLevels, (serverPlayer, integer) -> {
         serverPlayer.setExperienceLevels(integer);
         return true;
      }, serverPlayer -> serverPlayer.experienceLevel);

      public final BiConsumer<ServerPlayer, Integer> add;
      public final BiPredicate<ServerPlayer, Integer> set;
      public final String name;
      final ToIntFunction<ServerPlayer> query;

      private Type(
         final String name, final BiConsumer<ServerPlayer, Integer> add, final BiPredicate<ServerPlayer, Integer> set, final ToIntFunction<ServerPlayer> query
      ) {
         this.add = add;
         this.name = name;
         this.set = set;
         this.query = query;
      }
   }
}
