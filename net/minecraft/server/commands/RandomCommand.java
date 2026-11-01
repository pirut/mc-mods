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

   public static void register(CommandDispatcher<CommandSourceStack> p_295018_) {
      p_295018_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("random").then(drawRandomValueTree("value", false)))
               .then(drawRandomValueTree("roll", true)))
            .then(
               ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("reset").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
                     .then(
                        ((LiteralArgumentBuilder)Commands.literal("*").executes(p_295916_ -> resetAllSequences((CommandSourceStack)p_295916_.getSource())))
                           .then(
                              ((RequiredArgumentBuilder)Commands.argument("seed", IntegerArgumentType.integer())
                                    .executes(
                                       p_294436_ -> resetAllSequencesAndSetNewDefaults(
                                          (CommandSourceStack)p_294436_.getSource(), IntegerArgumentType.getInteger(p_294436_, "seed"), true, true
                                       )
                                    ))
                                 .then(
                                    ((RequiredArgumentBuilder)Commands.argument("includeWorldSeed", BoolArgumentType.bool())
                                          .executes(
                                             p_295162_ -> resetAllSequencesAndSetNewDefaults(
                                                (CommandSourceStack)p_295162_.getSource(),
                                                IntegerArgumentType.getInteger(p_295162_, "seed"),
                                                BoolArgumentType.getBool(p_295162_, "includeWorldSeed"),
                                                true
                                             )
                                          ))
                                       .then(
                                          Commands.argument("includeSequenceId", BoolArgumentType.bool())
                                             .executes(
                                                p_295871_ -> resetAllSequencesAndSetNewDefaults(
                                                   (CommandSourceStack)p_295871_.getSource(),
                                                   IntegerArgumentType.getInteger(p_295871_, "seed"),
                                                   BoolArgumentType.getBool(p_295871_, "includeWorldSeed"),
                                                   BoolArgumentType.getBool(p_295871_, "includeSequenceId")
                                                )
                                             )
                                       )
                                 )
                           )
                     ))
                  .then(
                     ((RequiredArgumentBuilder)Commands.argument("sequence", IdentifierArgument.id())
                           .suggests(RandomCommand::suggestRandomSequence)
                           .executes(p_466275_ -> resetSequence((CommandSourceStack)p_466275_.getSource(), IdentifierArgument.getId(p_466275_, "sequence"))))
                        .then(
                           ((RequiredArgumentBuilder)Commands.argument("seed", IntegerArgumentType.integer())
                                 .executes(
                                    p_466280_ -> resetSequence(
                                       (CommandSourceStack)p_466280_.getSource(),
                                       IdentifierArgument.getId(p_466280_, "sequence"),
                                       IntegerArgumentType.getInteger(p_466280_, "seed"),
                                       true,
                                       true
                                    )
                                 ))
                              .then(
                                 ((RequiredArgumentBuilder)Commands.argument("includeWorldSeed", BoolArgumentType.bool())
                                       .executes(
                                          p_466287_ -> resetSequence(
                                             (CommandSourceStack)p_466287_.getSource(),
                                             IdentifierArgument.getId(p_466287_, "sequence"),
                                             IntegerArgumentType.getInteger(p_466287_, "seed"),
                                             BoolArgumentType.getBool(p_466287_, "includeWorldSeed"),
                                             true
                                          )
                                       ))
                                    .then(
                                       Commands.argument("includeSequenceId", BoolArgumentType.bool())
                                          .executes(
                                             p_466286_ -> resetSequence(
                                                (CommandSourceStack)p_466286_.getSource(),
                                                IdentifierArgument.getId(p_466286_, "sequence"),
                                                IntegerArgumentType.getInteger(p_466286_, "seed"),
                                                BoolArgumentType.getBool(p_466286_, "includeWorldSeed"),
                                                BoolArgumentType.getBool(p_466286_, "includeSequenceId")
                                             )
                                          )
                                    )
                              )
                        )
                  )
            )
      );
   }

   private static LiteralArgumentBuilder<CommandSourceStack> drawRandomValueTree(String p_295419_, boolean p_295785_) {
      return (LiteralArgumentBuilder<CommandSourceStack>)Commands.literal(p_295419_)
         .then(
            ((RequiredArgumentBuilder)Commands.argument("range", RangeArgument.intRange())
                  .executes(
                     p_466285_ -> randomSample((CommandSourceStack)p_466285_.getSource(), RangeArgument.Ints.getRange(p_466285_, "range"), null, p_295785_)
                  ))
               .then(
                  ((RequiredArgumentBuilder)Commands.argument("sequence", IdentifierArgument.id())
                        .suggests(RandomCommand::suggestRandomSequence)
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
                     .executes(
                        p_466282_ -> randomSample(
                           (CommandSourceStack)p_466282_.getSource(),
                           RangeArgument.Ints.getRange(p_466282_, "range"),
                           IdentifierArgument.getId(p_466282_, "sequence"),
                           p_295785_
                        )
                     )
               )
         );
   }

   private static CompletableFuture<Suggestions> suggestRandomSequence(CommandContext<CommandSourceStack> p_296223_, SuggestionsBuilder p_295797_) {
      List<String> list = Lists.newArrayList();
      ((CommandSourceStack)p_296223_.getSource()).getLevel().getRandomSequences().forAllSequences((p_466278_, p_466279_) -> list.add(p_466278_.toString()));
      return SharedSuggestionProvider.suggest(list, p_295797_);
   }

   private static int randomSample(CommandSourceStack p_295774_, MinMaxBounds.Ints p_467049_, @Nullable Identifier p_469730_, boolean p_296222_) throws CommandSyntaxException {
      RandomSource randomsource;
      if (p_469730_ != null) {
         randomsource = p_295774_.getLevel().getRandomSequence(p_469730_);
      } else {
         randomsource = p_295774_.getLevel().getRandom();
      }

      int i = p_467049_.min().orElse(Integer.MIN_VALUE);
      int j = p_467049_.max().orElse(Integer.MAX_VALUE);
      long k = (long)j - i;
      if (k == 0L) {
         throw ERROR_RANGE_TOO_SMALL.create();
      } else if (k >= 2147483647L) {
         throw ERROR_RANGE_TOO_LARGE.create();
      } else {
         int l = Mth.randomBetweenInclusive(randomsource, i, j);
         if (p_296222_) {
            p_295774_.getServer()
               .getPlayerList()
               .broadcastSystemMessage(Component.translatable("commands.random.roll", p_295774_.getDisplayName(), l, i, j), false);
         } else {
            p_295774_.sendSuccess(() -> Component.translatable("commands.random.sample.success", l), false);
         }

         return l;
      }
   }

   private static int resetSequence(CommandSourceStack p_296416_, Identifier p_468757_) throws CommandSyntaxException {
      ServerLevel serverlevel = p_296416_.getLevel();
      serverlevel.getRandomSequences().reset(p_468757_, serverlevel.getSeed());
      p_296416_.sendSuccess(() -> Component.translatable("commands.random.reset.success", Component.translationArg(p_468757_)), false);
      return 1;
   }

   private static int resetSequence(CommandSourceStack p_295984_, Identifier p_468294_, int p_467399_, boolean p_467853_, boolean p_467725_) throws CommandSyntaxException {
      ServerLevel serverlevel = p_295984_.getLevel();
      serverlevel.getRandomSequences().reset(p_468294_, serverlevel.getSeed(), p_467399_, p_467853_, p_467725_);
      p_295984_.sendSuccess(() -> Component.translatable("commands.random.reset.success", Component.translationArg(p_468294_)), false);
      return 1;
   }

   private static int resetAllSequences(CommandSourceStack p_294291_) {
      int i = p_294291_.getLevel().getRandomSequences().clear();
      p_294291_.sendSuccess(() -> Component.translatable("commands.random.reset.all.success", i), false);
      return i;
   }

   private static int resetAllSequencesAndSetNewDefaults(CommandSourceStack p_294151_, int p_295754_, boolean p_294405_, boolean p_294396_) {
      RandomSequences randomsequences = p_294151_.getLevel().getRandomSequences();
      randomsequences.setSeedDefaults(p_295754_, p_294405_, p_294396_);
      int i = randomsequences.clear();
      p_294151_.sendSuccess(() -> Component.translatable("commands.random.reset.all.success", i), false);
      return i;
   }
}
