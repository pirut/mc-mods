package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Stopwatch;
import net.minecraft.world.Stopwatches;

public class StopwatchCommand {
   private static final DynamicCommandExceptionType ERROR_ALREADY_EXISTS = new DynamicCommandExceptionType(
      p_454664_ -> Component.translatableEscape("commands.stopwatch.already_exists", p_454664_)
   );
   public static final DynamicCommandExceptionType ERROR_DOES_NOT_EXIST = new DynamicCommandExceptionType(
      p_456143_ -> Component.translatableEscape("commands.stopwatch.does_not_exist", p_456143_)
   );
   public static final SuggestionProvider<CommandSourceStack> SUGGEST_STOPWATCHES = (p_454878_, p_455100_) -> SharedSuggestionProvider.suggestResource(
      ((CommandSourceStack)p_454878_.getSource()).getServer().getStopwatches().ids(), p_455100_
   );

   public static void register(CommandDispatcher<CommandSourceStack> p_455850_) {
      p_455850_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(
                           "stopwatch"
                        )
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
                     .then(
                        Commands.literal("create")
                           .then(
                              Commands.argument("id", IdentifierArgument.id())
                                 .executes(p_466313_ -> createStopwatch((CommandSourceStack)p_466313_.getSource(), IdentifierArgument.getId(p_466313_, "id")))
                           )
                     ))
                  .then(
                     Commands.literal("query")
                        .then(
                           ((RequiredArgumentBuilder)Commands.argument("id", IdentifierArgument.id())
                                 .suggests(SUGGEST_STOPWATCHES)
                                 .then(
                                    Commands.argument("scale", DoubleArgumentType.doubleArg())
                                       .executes(
                                          p_466308_ -> queryStopwatch(
                                             (CommandSourceStack)p_466308_.getSource(),
                                             IdentifierArgument.getId(p_466308_, "id"),
                                             DoubleArgumentType.getDouble(p_466308_, "scale")
                                          )
                                       )
                                 ))
                              .executes(p_466311_ -> queryStopwatch((CommandSourceStack)p_466311_.getSource(), IdentifierArgument.getId(p_466311_, "id"), 1.0))
                        )
                  ))
               .then(
                  Commands.literal("restart")
                     .then(
                        Commands.argument("id", IdentifierArgument.id())
                           .suggests(SUGGEST_STOPWATCHES)
                           .executes(p_466310_ -> restartStopwatch((CommandSourceStack)p_466310_.getSource(), IdentifierArgument.getId(p_466310_, "id")))
                     )
               ))
            .then(
               Commands.literal("remove")
                  .then(
                     Commands.argument("id", IdentifierArgument.id())
                        .suggests(SUGGEST_STOPWATCHES)
                        .executes(p_466307_ -> removeStopwatch((CommandSourceStack)p_466307_.getSource(), IdentifierArgument.getId(p_466307_, "id")))
                  )
            )
      );
   }

   private static int createStopwatch(CommandSourceStack p_455513_, Identifier p_468252_) throws CommandSyntaxException {
      MinecraftServer minecraftserver = p_455513_.getServer();
      Stopwatches stopwatches = minecraftserver.getStopwatches();
      Stopwatch stopwatch = new Stopwatch(Stopwatches.currentTime());
      if (!stopwatches.add(p_468252_, stopwatch)) {
         throw ERROR_ALREADY_EXISTS.create(p_468252_);
      } else {
         p_455513_.sendSuccess(() -> Component.translatable("commands.stopwatch.create.success", Component.translationArg(p_468252_)), true);
         return 1;
      }
   }

   private static int queryStopwatch(CommandSourceStack p_455007_, Identifier p_467660_, double p_457655_) throws CommandSyntaxException {
      MinecraftServer minecraftserver = p_455007_.getServer();
      Stopwatches stopwatches = minecraftserver.getStopwatches();
      Stopwatch stopwatch = stopwatches.get(p_467660_);
      if (stopwatch == null) {
         throw ERROR_DOES_NOT_EXIST.create(p_467660_);
      } else {
         long i = Stopwatches.currentTime();
         double d0 = stopwatch.elapsedSeconds(i);
         p_455007_.sendSuccess(() -> Component.translatable("commands.stopwatch.query", Component.translationArg(p_467660_), d0), true);
         return (int)(d0 * p_457655_);
      }
   }

   private static int restartStopwatch(CommandSourceStack p_455551_, Identifier p_469130_) throws CommandSyntaxException {
      MinecraftServer minecraftserver = p_455551_.getServer();
      Stopwatches stopwatches = minecraftserver.getStopwatches();
      if (!stopwatches.update(p_469130_, p_456125_ -> new Stopwatch(Stopwatches.currentTime()))) {
         throw ERROR_DOES_NOT_EXIST.create(p_469130_);
      } else {
         p_455551_.sendSuccess(() -> Component.translatable("commands.stopwatch.restart.success", Component.translationArg(p_469130_)), true);
         return 1;
      }
   }

   private static int removeStopwatch(CommandSourceStack p_455826_, Identifier p_469245_) throws CommandSyntaxException {
      MinecraftServer minecraftserver = p_455826_.getServer();
      Stopwatches stopwatches = minecraftserver.getStopwatches();
      if (!stopwatches.remove(p_469245_)) {
         throw ERROR_DOES_NOT_EXIST.create(p_469245_);
      } else {
         p_455826_.sendSuccess(() -> Component.translatable("commands.stopwatch.remove.success", Component.translationArg(p_469245_)), true);
         return 1;
      }
   }
}
