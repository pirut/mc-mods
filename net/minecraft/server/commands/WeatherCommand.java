package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.valueproviders.IntProvider;

public class WeatherCommand {
   private static final int DEFAULT_TIME = -1;

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("weather")
                     .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
                  .then(
                     ((LiteralArgumentBuilder)Commands.literal("clear")
                           .executes(commandContext -> setClear((CommandSourceStack)commandContext.getSource(), -1)))
                        .then(
                           Commands.argument("duration", TimeArgument.time(1))
                              .executes(
                                 commandContext -> setClear(
                                    (CommandSourceStack)commandContext.getSource(), IntegerArgumentType.getInteger(commandContext, "duration")
                                 )
                              )
                        )
                  ))
               .then(
                  ((LiteralArgumentBuilder)Commands.literal("rain").executes(commandContext -> setRain((CommandSourceStack)commandContext.getSource(), -1)))
                     .then(
                        Commands.argument("duration", TimeArgument.time(1))
                           .executes(
                              commandContext -> setRain(
                                 (CommandSourceStack)commandContext.getSource(), IntegerArgumentType.getInteger(commandContext, "duration")
                              )
                           )
                     )
               ))
            .then(
               ((LiteralArgumentBuilder)Commands.literal("thunder").executes(commandContext -> setThunder((CommandSourceStack)commandContext.getSource(), -1)))
                  .then(
                     Commands.argument("duration", TimeArgument.time(1))
                        .executes(
                           commandContext -> setThunder(
                              (CommandSourceStack)commandContext.getSource(), IntegerArgumentType.getInteger(commandContext, "duration")
                           )
                        )
                  )
            )
      );
   }

   private static int getDuration(CommandSourceStack source, int time, IntProvider timeProvider) {
      return time == -1 ? timeProvider.sample(source.getServer().overworld().getRandom()) : time;
   }

   private static int setClear(CommandSourceStack source, int time) {
      source.getServer().overworld().setWeatherParameters(getDuration(source, time, ServerLevel.RAIN_DELAY), 0, false, false);
      source.sendSuccess(() -> Component.translatable("commands.weather.set.clear"), true);
      return time;
   }

   private static int setRain(CommandSourceStack source, int time) {
      source.getServer().overworld().setWeatherParameters(0, getDuration(source, time, ServerLevel.RAIN_DURATION), true, false);
      source.sendSuccess(() -> Component.translatable("commands.weather.set.rain"), true);
      return time;
   }

   private static int setThunder(CommandSourceStack source, int time) {
      source.getServer().overworld().setWeatherParameters(0, getDuration(source, time, ServerLevel.THUNDER_DURATION), true, true);
      source.sendSuccess(() -> Component.translatable("commands.weather.set.thunder"), true);
      return time;
   }
}
