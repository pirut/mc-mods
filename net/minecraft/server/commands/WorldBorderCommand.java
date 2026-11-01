package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.commands.arguments.coordinates.Vec2Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.Vec2;

public class WorldBorderCommand {
   private static final SimpleCommandExceptionType ERROR_SAME_CENTER = new SimpleCommandExceptionType(
      Component.translatable("commands.worldborder.center.failed")
   );
   private static final SimpleCommandExceptionType ERROR_SAME_SIZE = new SimpleCommandExceptionType(
      Component.translatable("commands.worldborder.set.failed.nochange")
   );
   private static final SimpleCommandExceptionType ERROR_TOO_SMALL = new SimpleCommandExceptionType(
      Component.translatable("commands.worldborder.set.failed.small")
   );
   private static final SimpleCommandExceptionType ERROR_TOO_BIG = new SimpleCommandExceptionType(
      Component.translatable("commands.worldborder.set.failed.big", 5.999997E7F)
   );
   private static final SimpleCommandExceptionType ERROR_TOO_FAR_OUT = new SimpleCommandExceptionType(
      Component.translatable("commands.worldborder.set.failed.far", 2.9999984E7)
   );
   private static final SimpleCommandExceptionType ERROR_SAME_WARNING_TIME = new SimpleCommandExceptionType(
      Component.translatable("commands.worldborder.warning.time.failed")
   );
   private static final SimpleCommandExceptionType ERROR_SAME_WARNING_DISTANCE = new SimpleCommandExceptionType(
      Component.translatable("commands.worldborder.warning.distance.failed")
   );
   private static final SimpleCommandExceptionType ERROR_SAME_DAMAGE_BUFFER = new SimpleCommandExceptionType(
      Component.translatable("commands.worldborder.damage.buffer.failed")
   );
   private static final SimpleCommandExceptionType ERROR_SAME_DAMAGE_AMOUNT = new SimpleCommandExceptionType(
      Component.translatable("commands.worldborder.damage.amount.failed")
   );

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(
                                 "worldborder"
                              )
                              .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
                           .then(
                              Commands.literal("add")
                                 .then(
                                    ((RequiredArgumentBuilder)Commands.argument("distance", DoubleArgumentType.doubleArg(-5.999997E7F, 5.999997E7F))
                                          .executes(
                                             commandContext -> setSize(
                                                (CommandSourceStack)commandContext.getSource(),
                                                ((CommandSourceStack)commandContext.getSource()).getLevel().getWorldBorder().getSize()
                                                   + DoubleArgumentType.getDouble(commandContext, "distance"),
                                                0L
                                             )
                                          ))
                                       .then(
                                          Commands.argument("time", TimeArgument.time(0))
                                             .executes(
                                                commandContext -> setSize(
                                                   (CommandSourceStack)commandContext.getSource(),
                                                   ((CommandSourceStack)commandContext.getSource()).getLevel().getWorldBorder().getSize()
                                                      + DoubleArgumentType.getDouble(commandContext, "distance"),
                                                   ((CommandSourceStack)commandContext.getSource()).getLevel().getWorldBorder().getLerpTime()
                                                      + IntegerArgumentType.getInteger(commandContext, "time")
                                                )
                                             )
                                       )
                                 )
                           ))
                        .then(
                           Commands.literal("set")
                              .then(
                                 ((RequiredArgumentBuilder)Commands.argument("distance", DoubleArgumentType.doubleArg(-5.999997E7F, 5.999997E7F))
                                       .executes(
                                          commandContext -> setSize(
                                             (CommandSourceStack)commandContext.getSource(), DoubleArgumentType.getDouble(commandContext, "distance"), 0L
                                          )
                                       ))
                                    .then(
                                       Commands.argument("time", TimeArgument.time(0))
                                          .executes(
                                             commandContext -> setSize(
                                                (CommandSourceStack)commandContext.getSource(),
                                                DoubleArgumentType.getDouble(commandContext, "distance"),
                                                IntegerArgumentType.getInteger(commandContext, "time")
                                             )
                                          )
                                    )
                              )
                        ))
                     .then(
                        Commands.literal("center")
                           .then(
                              Commands.argument("pos", Vec2Argument.vec2())
                                 .executes(
                                    commandContext -> setCenter((CommandSourceStack)commandContext.getSource(), Vec2Argument.getVec2(commandContext, "pos"))
                                 )
                           )
                     ))
                  .then(
                     ((LiteralArgumentBuilder)Commands.literal("damage")
                           .then(
                              Commands.literal("amount")
                                 .then(
                                    Commands.argument("damagePerBlock", FloatArgumentType.floatArg(0.0F))
                                       .executes(
                                          commandContext -> setDamageAmount(
                                             (CommandSourceStack)commandContext.getSource(), FloatArgumentType.getFloat(commandContext, "damagePerBlock")
                                          )
                                       )
                                 )
                           ))
                        .then(
                           Commands.literal("buffer")
                              .then(
                                 Commands.argument("distance", FloatArgumentType.floatArg(0.0F))
                                    .executes(
                                       commandContext -> setDamageBuffer(
                                          (CommandSourceStack)commandContext.getSource(), FloatArgumentType.getFloat(commandContext, "distance")
                                       )
                                    )
                              )
                        )
                  ))
               .then(Commands.literal("get").executes(commandContext -> getSize((CommandSourceStack)commandContext.getSource()))))
            .then(
               ((LiteralArgumentBuilder)Commands.literal("warning")
                     .then(
                        Commands.literal("distance")
                           .then(
                              Commands.argument("distance", IntegerArgumentType.integer(0))
                                 .executes(
                                    commandContext -> setWarningDistance(
                                       (CommandSourceStack)commandContext.getSource(), IntegerArgumentType.getInteger(commandContext, "distance")
                                    )
                                 )
                           )
                     ))
                  .then(
                     Commands.literal("time")
                        .then(
                           Commands.argument("time", TimeArgument.time(0))
                              .executes(
                                 commandContext -> setWarningTime(
                                    (CommandSourceStack)commandContext.getSource(), IntegerArgumentType.getInteger(commandContext, "time")
                                 )
                              )
                        )
                  )
            )
      );
   }

   private static int setDamageBuffer(CommandSourceStack source, float distance) throws CommandSyntaxException {
      WorldBorder worldBorder = source.getLevel().getWorldBorder();
      if (worldBorder.getSafeZone() == distance) {
         throw ERROR_SAME_DAMAGE_BUFFER.create();
      } else {
         worldBorder.setSafeZone(distance);
         source.sendSuccess(() -> Component.translatable("commands.worldborder.damage.buffer.success", String.format(Locale.ROOT, "%.2f", distance)), true);
         return (int)distance;
      }
   }

   private static int setDamageAmount(CommandSourceStack source, float damagePerBlock) throws CommandSyntaxException {
      WorldBorder worldBorder = source.getLevel().getWorldBorder();
      if (worldBorder.getDamagePerBlock() == damagePerBlock) {
         throw ERROR_SAME_DAMAGE_AMOUNT.create();
      } else {
         worldBorder.setDamagePerBlock(damagePerBlock);
         source.sendSuccess(
            () -> Component.translatable("commands.worldborder.damage.amount.success", String.format(Locale.ROOT, "%.2f", damagePerBlock)), true
         );
         return (int)damagePerBlock;
      }
   }

   private static int setWarningTime(CommandSourceStack source, int time) throws CommandSyntaxException {
      WorldBorder worldBorder = source.getLevel().getWorldBorder();
      if (worldBorder.getWarningTime() == time) {
         throw ERROR_SAME_WARNING_TIME.create();
      } else {
         worldBorder.setWarningTime(time);
         source.sendSuccess(() -> Component.translatable("commands.worldborder.warning.time.success", formatTicksToSeconds(time)), true);
         return time;
      }
   }

   private static int setWarningDistance(CommandSourceStack source, int distance) throws CommandSyntaxException {
      WorldBorder worldBorder = source.getLevel().getWorldBorder();
      if (worldBorder.getWarningBlocks() == distance) {
         throw ERROR_SAME_WARNING_DISTANCE.create();
      } else {
         worldBorder.setWarningBlocks(distance);
         source.sendSuccess(() -> Component.translatable("commands.worldborder.warning.distance.success", distance), true);
         return distance;
      }
   }

   private static int getSize(CommandSourceStack source) {
      double d = source.getLevel().getWorldBorder().getSize();
      source.sendSuccess(() -> Component.translatable("commands.worldborder.get", String.format(Locale.ROOT, "%.0f", d)), false);
      return Mth.floor(d + 0.5);
   }

   private static int setCenter(CommandSourceStack source, Vec2 pos) throws CommandSyntaxException {
      WorldBorder worldBorder = source.getLevel().getWorldBorder();
      if (worldBorder.getCenterX() == pos.x && worldBorder.getCenterZ() == pos.y) {
         throw ERROR_SAME_CENTER.create();
      } else if (!(Math.abs(pos.x) > 2.9999984E7) && !(Math.abs(pos.y) > 2.9999984E7)) {
         worldBorder.setCenter(pos.x, pos.y);
         source.sendSuccess(
            () -> Component.translatable(
               "commands.worldborder.center.success", String.format(Locale.ROOT, "%.2f", pos.x), String.format(Locale.ROOT, "%.2f", pos.y)
            ),
            true
         );
         return 0;
      } else {
         throw ERROR_TOO_FAR_OUT.create();
      }
   }

   private static int setSize(CommandSourceStack source, double newSize, long time) throws CommandSyntaxException {
      ServerLevel serverLevel = source.getLevel();
      WorldBorder worldBorder = serverLevel.getWorldBorder();
      double d = worldBorder.getSize();
      if (d == newSize) {
         throw ERROR_SAME_SIZE.create();
      } else if (newSize < 1.0) {
         throw ERROR_TOO_SMALL.create();
      } else if (newSize > 5.999997E7F) {
         throw ERROR_TOO_BIG.create();
      } else {
         String string = String.format(Locale.ROOT, "%.1f", newSize);
         if (time > 0L) {
            worldBorder.lerpSizeBetween(d, newSize, time, serverLevel.getGameTime());
            if (newSize > d) {
               source.sendSuccess(() -> Component.translatable("commands.worldborder.set.grow", string, formatTicksToSeconds(time)), true);
            } else {
               source.sendSuccess(() -> Component.translatable("commands.worldborder.set.shrink", string, formatTicksToSeconds(time)), true);
            }
         } else {
            worldBorder.setSize(newSize);
            source.sendSuccess(() -> Component.translatable("commands.worldborder.set.immediate", string), true);
         }

         return (int)(newSize - d);
      }
   }

   private static String formatTicksToSeconds(long ticks) {
      return String.format(Locale.ROOT, "%.2f", ticks / 20.0);
   }
}
