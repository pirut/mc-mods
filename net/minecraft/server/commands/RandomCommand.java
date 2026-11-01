package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.advancements.criterion.MinMaxBounds;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.RangeArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomSequences;
import org.jspecify.annotations.Nullable;

public class RandomCommand {
   private static final SimpleCommandExceptionType ERROR_RANGE_TOO_LARGE = new SimpleCommandExceptionType(
      Component.translatable("commands.random.error.range_too_large")
   );
   private static final SimpleCommandExceptionType ERROR_RANGE_TOO_SMALL = new SimpleCommandExceptionType(
      Component.translatable("commands.random.error.range_too_small")
   );

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("random").then(drawRandomValueTree("value", false)))
               .then(drawRandomValueTree("roll", true)))
            .then(
               ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("reset").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
                     .then(
                        ((LiteralArgumentBuilder)Commands.literal("*")
                              .executes(commandContext -> resetAllSequences((CommandSourceStack)commandContext.getSource())))
                           .then(
                              ((RequiredArgumentBuilder)Commands.argument("seed", IntegerArgumentType.integer())
                                    .executes(
                                       commandContext -> resetAllSequencesAndSetNewDefaults(
                                          (CommandSourceStack)commandContext.getSource(), IntegerArgumentType.getInteger(commandContext, "seed"), true, true
                                       )
                                    ))
                                 .then(
                                    ((RequiredArgumentBuilder)Commands.argument("includeWorldSeed", BoolArgumentType.bool())
                                          .executes(
                                             commandContext -> resetAllSequencesAndSetNewDefaults(
                                                (CommandSourceStack)commandContext.getSource(),
                                                IntegerArgumentType.getInteger(commandContext, "seed"),
                                                BoolArgumentType.getBool(commandContext, "includeWorldSeed"),
                                                true
                                             )
                                          ))
                                       .then(
                                          Commands.argument("includeSequenceId", BoolArgumentType.bool())
                                             .executes(
                                                commandContext -> resetAllSequencesAndSetNewDefaults(
                                                   (CommandSourceStack)commandContext.getSource(),
                                                   IntegerArgumentType.getInteger(commandContext, "seed"),
                                                   BoolArgumentType.getBool(commandContext, "includeWorldSeed"),
                                                   BoolArgumentType.getBool(commandContext, "includeSequenceId")
                                                )
                                             )
                                       )
                                 )
                           )
                     ))
                  .then(
                     ((RequiredArgumentBuilder)Commands.argument("sequence", IdentifierArgument.id())
                           .suggests(RandomCommand::suggestRandomSequence)
                           .executes(
                              commandContext -> resetSequence(
                                 (CommandSourceStack)commandContext.getSource(), IdentifierArgument.getId(commandContext, "sequence")
                              )
                           ))
                        .then(
                           ((RequiredArgumentBuilder)Commands.argument("seed", IntegerArgumentType.integer())
                                 .executes(
                                    commandContext -> resetSequence(
                                       (CommandSourceStack)commandContext.getSource(),
                                       IdentifierArgument.getId(commandContext, "sequence"),
                                       IntegerArgumentType.getInteger(commandContext, "seed"),
                                       true,
                                       true
                                    )
                                 ))
                              .then(
                                 ((RequiredArgumentBuilder)Commands.argument("includeWorldSeed", BoolArgumentType.bool())
                                       .executes(
                                          commandContext -> resetSequence(
                                             (CommandSourceStack)commandContext.getSource(),
                                             IdentifierArgument.getId(commandContext, "sequence"),
                                             IntegerArgumentType.getInteger(commandContext, "seed"),
                                             BoolArgumentType.getBool(commandContext, "includeWorldSeed"),
                                             true
                                          )
                                       ))
                                    .then(
                                       Commands.argument("includeSequenceId", BoolArgumentType.bool())
                                          .executes(
                                             commandContext -> resetSequence(
                                                (CommandSourceStack)commandContext.getSource(),
                                                IdentifierArgument.getId(commandContext, "sequence"),
                                                IntegerArgumentType.getInteger(commandContext, "seed"),
                                                BoolArgumentType.getBool(commandContext, "includeWorldSeed"),
                                                BoolArgumentType.getBool(commandContext, "includeSequenceId")
                                             )
                                          )
                                    )
                              )
                        )
                  )
            )
      );
   }

   private static LiteralArgumentBuilder<CommandSourceStack> drawRandomValueTree(String subcommand, boolean displayResult) {
      return (LiteralArgumentBuilder<CommandSourceStack>)Commands.literal(subcommand)
         .then(
            ((RequiredArgumentBuilder)Commands.argument("range", RangeArgument.intRange())
                  .executes(
                     commandContext -> randomSample(
                        (CommandSourceStack)commandContext.getSource(), RangeArgument.Ints.getRange(commandContext, "range"), null, displayResult
                     )
                  ))
               .then(
                  ((RequiredArgumentBuilder)Commands.argument("sequence", IdentifierArgument.id())
                        .suggests(RandomCommand::suggestRandomSequence)
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
                     .executes(
                        commandContext -> randomSample(
                           (CommandSourceStack)commandContext.getSource(),
                           RangeArgument.Ints.getRange(commandContext, "range"),
                           IdentifierArgument.getId(commandContext, "sequence"),
                           displayResult
                        )
                     )
               )
         );
   }

   private static CompletableFuture<Suggestions> suggestRandomSequence(CommandContext<CommandSourceStack> context, SuggestionsBuilder suggestionsBuilder) {
      List<String> list = Lists.newArrayList();
      ((CommandSourceStack)context.getSource())
         .getLevel()
         .getRandomSequences()
         .forAllSequences((identifier, randomSequence) -> list.add(identifier.toString()));
      return SharedSuggestionProvider.suggest(list, suggestionsBuilder);
   }

   private static int randomSample(CommandSourceStack source, MinMaxBounds.Ints range, @Nullable Identifier sequence, boolean displayResult) throws CommandSyntaxException {
      RandomSource randomSource;
      if (sequence != null) {
         randomSource = source.getLevel().getRandomSequence(sequence);
      } else {
         randomSource = source.getLevel().getRandom();
      }

      int i = range.min().orElse(Integer.MIN_VALUE);
      int j = range.max().orElse(Integer.MAX_VALUE);
      long l = (long)j - i;
      if (l == 0L) {
         throw ERROR_RANGE_TOO_SMALL.create();
      } else if (l >= 2147483647L) {
         throw ERROR_RANGE_TOO_LARGE.create();
      } else {
         int k = Mth.randomBetweenInclusive(randomSource, i, j);
         if (displayResult) {
            source.getServer().getPlayerList().broadcastSystemMessage(Component.translatable("commands.random.roll", source.getDisplayName(), k, i, j), false);
         } else {
            source.sendSuccess(() -> Component.translatable("commands.random.sample.success", k), false);
         }

         return k;
      }
   }

   private static int resetSequence(CommandSourceStack source, Identifier sequence) throws CommandSyntaxException {
      ServerLevel serverLevel = source.getLevel();
      serverLevel.getRandomSequences().reset(sequence, serverLevel.getSeed());
      source.sendSuccess(() -> Component.translatable("commands.random.reset.success", Component.translationArg(sequence)), false);
      return 1;
   }

   private static int resetSequence(CommandSourceStack source, Identifier sequence, int seed, boolean includeWorldSeed, boolean includeSequenceId) throws CommandSyntaxException {
      ServerLevel serverLevel = source.getLevel();
      serverLevel.getRandomSequences().reset(sequence, serverLevel.getSeed(), seed, includeWorldSeed, includeSequenceId);
      source.sendSuccess(() -> Component.translatable("commands.random.reset.success", Component.translationArg(sequence)), false);
      return 1;
   }

   private static int resetAllSequences(CommandSourceStack source) {
      int i = source.getLevel().getRandomSequences().clear();
      source.sendSuccess(() -> Component.translatable("commands.random.reset.all.success", i), false);
      return i;
   }

   private static int resetAllSequencesAndSetNewDefaults(CommandSourceStack source, int seed, boolean includeWorldSeed, boolean includeSequenceId) {
      RandomSequences randomSequences = source.getLevel().getRandomSequences();
      randomSequences.setSeedDefaults(seed, includeWorldSeed, includeSequenceId);
      int i = randomSequences.clear();
      source.sendSuccess(() -> Component.translatable("commands.random.reset.all.success", i), false);
      return i;
   }
}
