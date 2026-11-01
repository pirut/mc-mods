package net.neoforged.neoforge.server.command;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameRules;

class TimeSpeedCommand {
   static ArgumentBuilder<CommandSourceStack, ?> register() {
      return ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("day")
               .then(
                  ((LiteralArgumentBuilder)Commands.literal("speed")
                        .then(
                           ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("set")
                                       .requires(cs -> cs.hasPermission(2)))
                                    .then(Commands.literal("default").executes(context -> setDefault((CommandSourceStack)context.getSource()))))
                                 .then(Commands.literal("realtime").executes(context -> setDaylength((CommandSourceStack)context.getSource(), 1440))))
                              .then(
                                 Commands.argument("speed", FloatArgumentType.floatArg(0.0F, 1000.0F))
                                    .executes(context -> setSpeed((CommandSourceStack)context.getSource(), FloatArgumentType.getFloat(context, "speed")))
                              )
                        ))
                     .executes(context -> query((CommandSourceStack)context.getSource()))
               ))
            .then(
               ((LiteralArgumentBuilder)Commands.literal("length")
                     .then(
                        ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("set").requires(cs -> cs.hasPermission(2)))
                                 .then(Commands.literal("default").executes(context -> setDefault((CommandSourceStack)context.getSource()))))
                              .then(Commands.literal("realtime").executes(context -> setDaylength((CommandSourceStack)context.getSource(), 1440))))
                           .then(
                              Commands.argument("minutes", IntegerArgumentType.integer(1, 1440))
                                 .executes(context -> setDaylength((CommandSourceStack)context.getSource(), IntegerArgumentType.getInteger(context, "minutes")))
                           )
                     ))
                  .executes(context -> query((CommandSourceStack)context.getSource()))
            ))
         .executes(context -> query((CommandSourceStack)context.getSource()));
   }

   private static int query(CommandSourceStack source) {
      float speed = source.getLevel().getDayTimePerTick();
      if (speed < 0.0F) {
         source.sendSuccess(() -> CommandUtils.makeTranslatableWithFallback("commands.neoforge.timespeed.query.default", levelName(source)), true);
      } else {
         source.sendSuccess(
            () -> CommandUtils.makeTranslatableWithFallback("commands.neoforge.timespeed.query", levelName(source), speed, minutes(speed)), true
         );
      }

      return 1;
   }

   private static Component levelName(CommandSourceStack source) {
      return source.getLevel().getDescription();
   }

   private static float minutes(float speed) {
      return (int)(200.0F / speed) / 10.0F;
   }

   private static int setSpeed(CommandSourceStack source, float speed) {
      GameRules.BooleanValue rule = source.getLevel().getGameRules().getRule(GameRules.RULE_DAYLIGHT);
      if (!rule.get() && speed > 0.0F) {
         rule.set(true, null);
         source.sendSuccess(() -> CommandUtils.makeTranslatableWithFallback("commands.gamerule.set", GameRules.RULE_DAYLIGHT.getId(), rule.toString()), true);
      } else if (rule.get() && speed == 0.0F) {
         rule.set(false, null);
         source.sendSuccess(() -> CommandUtils.makeTranslatableWithFallback("commands.gamerule.set", GameRules.RULE_DAYLIGHT.getId(), rule.toString()), true);
         return 1;
      }

      source.getLevel().setDayTimePerTick(speed);
      source.sendSuccess(() -> CommandUtils.makeTranslatableWithFallback("commands.neoforge.timespeed.set", levelName(source), speed, minutes(speed)), true);
      return 1;
   }

   private static int setDaylength(CommandSourceStack source, int minutes) {
      return minutes == 20 ? setDefault(source) : setSpeed(source, 20.0F / minutes);
   }

   private static int setDefault(CommandSourceStack source) {
      source.getLevel().setDayTimePerTick(-1.0F);
      source.sendSuccess(() -> CommandUtils.makeTranslatableWithFallback("commands.neoforge.timespeed.set.default", levelName(source)), true);
      return 1;
   }
}
