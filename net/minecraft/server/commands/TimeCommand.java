package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

public class TimeCommand {
   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("time")
                     .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
                  .then(
                     ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("set")
                                    .then(Commands.literal("day").executes(commandContext -> setTime((CommandSourceStack)commandContext.getSource(), 1000))))
                                 .then(Commands.literal("noon").executes(commandContext -> setTime((CommandSourceStack)commandContext.getSource(), 6000))))
                              .then(Commands.literal("night").executes(commandContext -> setTime((CommandSourceStack)commandContext.getSource(), 13000))))
                           .then(Commands.literal("midnight").executes(commandContext -> setTime((CommandSourceStack)commandContext.getSource(), 18000))))
                        .then(
                           Commands.argument("time", TimeArgument.time())
                              .executes(
                                 commandContext -> setTime(
                                    (CommandSourceStack)commandContext.getSource(), IntegerArgumentType.getInteger(commandContext, "time")
                                 )
                              )
                        )
                  ))
               .then(
                  Commands.literal("add")
                     .then(
                        Commands.argument("time", TimeArgument.time())
                           .executes(
                              commandContext -> addTime((CommandSourceStack)commandContext.getSource(), IntegerArgumentType.getInteger(commandContext, "time"))
                           )
                     )
               ))
            .then(
               ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("query")
                        .then(
                           Commands.literal("daytime")
                              .executes(
                                 commandContext -> queryTime(
                                    (CommandSourceStack)commandContext.getSource(), getDayTime(((CommandSourceStack)commandContext.getSource()).getLevel())
                                 )
                              )
                        ))
                     .then(
                        Commands.literal("gametime")
                           .executes(
                              commandContext -> queryTime(
                                 (CommandSourceStack)commandContext.getSource(),
                                 (int)(((CommandSourceStack)commandContext.getSource()).getLevel().getGameTime() % 2147483647L)
                              )
                           )
                     ))
                  .then(
                     Commands.literal("day")
                        .executes(
                           commandContext -> queryTime(
                              (CommandSourceStack)commandContext.getSource(),
                              (int)(((CommandSourceStack)commandContext.getSource()).getLevel().getDayCount() % 2147483647L)
                           )
                        )
                  )
            )
      );
   }

   private static int getDayTime(ServerLevel level) {
      return (int)(level.getDayTime() % 24000L);
   }

   private static int queryTime(CommandSourceStack source, int time) {
      source.sendSuccess(() -> Component.translatable("commands.time.query", time), false);
      return time;
   }

   public static int setTime(CommandSourceStack source, int time) {
      for (ServerLevel serverLevel : source.getServer().getAllLevels()) {
         serverLevel.setDayTime(time);
      }

      source.getServer().forceTimeSynchronization();
      source.sendSuccess(() -> Component.translatable("commands.time.set", time), true);
      return getDayTime(source.getLevel());
   }

   public static int addTime(CommandSourceStack source, int amount) {
      for (ServerLevel serverLevel : source.getServer().getAllLevels()) {
         serverLevel.setDayTime(serverLevel.getDayTime() + amount);
      }

      source.getServer().forceTimeSynchronization();
      int i = getDayTime(source.getLevel());
      source.sendSuccess(() -> Component.translatable("commands.time.set", i), true);
      return i;
   }
}
