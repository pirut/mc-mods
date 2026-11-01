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
import javax.annotation.Nullable;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.RangeArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomSequences;

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
               ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("reset").requires(p_295133_ -> p_295133_.hasPermission(2)))
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
                     ((RequiredArgumentBuilder)Commands.argument("sequence", ResourceLocationArgument.id())
                           .suggests(RandomCommand::suggestRandomSequence)
                           .executes(
                              p_295053_ -> resetSequence((CommandSourceStack)p_295053_.getSource(), ResourceLocationArgument.getId(p_295053_, "sequence"))
                           ))
                        .then(
                           ((RequiredArgumentBuilder)Commands.argument("seed", IntegerArgumentType.integer())
                                 .executes(
                                    p_295487_ -> resetSequence(
                                       (CommandSourceStack)p_295487_.getSource(),
                                       ResourceLocationArgument.getId(p_295487_, "sequence"),
                                       IntegerArgumentType.getInteger(p_295487_, "seed"),
                                       true,
                                       true
                                    )
                                 ))
                              .then(
                                 ((RequiredArgumentBuilder)Commands.argument("includeWorldSeed", BoolArgumentType.bool())
                                       .executes(
                                          p_294832_ -> resetSequence(
                                             (CommandSourceStack)p_294832_.getSource(),
                                             ResourceLocationArgument.getId(p_294832_, "sequence"),
                                             IntegerArgumentType.getInteger(p_294832_, "seed"),
                                             BoolArgumentType.getBool(p_294832_, "includeWorldSeed"),
                                             true
                                          )
                                       ))
                                    .then(
                                       Commands.argument("includeSequenceId", BoolArgumentType.bool())
                                          .executes(
                                             p_295213_ -> resetSequence(
                                                (CommandSourceStack)p_295213_.getSource(),
                                                ResourceLocationArgument.getId(p_295213_, "sequence"),
                                                IntegerArgumentType.getInteger(p_295213_, "seed"),
                                                BoolArgumentType.getBool(p_295213_, "includeWorldSeed"),
                                                BoolArgumentType.getBool(p_295213_, "includeSequenceId")
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
                     p_294702_ -> randomSample((CommandSourceStack)p_294702_.getSource(), RangeArgument.Ints.getRange(p_294702_, "range"), null, p_295785_)
                  ))
               .then(
                  ((RequiredArgumentBuilder)Commands.argument("sequence", ResourceLocationArgument.id())
                        .suggests(RandomCommand::suggestRandomSequence)
                        .requires(p_296287_ -> p_296287_.hasPermission(2)))
                     .executes(
                        p_295226_ -> randomSample(
                           (CommandSourceStack)p_295226_.getSource(),
                           RangeArgument.Ints.getRange(p_295226_, "range"),
                           ResourceLocationArgument.getId(p_295226_, "sequence"),
                           p_295785_
                        )
                     )
               )
         );
   }

   private static CompletableFuture<Suggestions> suggestRandomSequence(CommandContext<CommandSourceStack> p_296223_, SuggestionsBuilder p_295797_) {
      List<String> $$2 = Lists.newArrayList();
      ((CommandSourceStack)p_296223_.getSource()).getLevel().getRandomSequences().forAllSequences((p_294879_, p_294337_) -> $$2.add(p_294879_.toString()));
      return SharedSuggestionProvider.suggest($$2, p_295797_);
   }

   private static int randomSample(CommandSourceStack p_295774_, MinMaxBounds.Ints p_295453_, @Nullable ResourceLocation p_294336_, boolean p_296222_) throws CommandSyntaxException {
      RandomSource $$4;
      if (p_294336_ != null) {
         $$4 = p_295774_.getLevel().getRandomSequence(p_294336_);
      } else {
         $$4 = p_295774_.getLevel().getRandom();
      }

      int $$6 = p_295453_.min().orElse(Integer.MIN_VALUE);
      int $$7 = p_295453_.max().orElse(Integer.MAX_VALUE);
      long $$8 = (long)$$7 - $$6;
      if ($$8 == 0L) {
         throw ERROR_RANGE_TOO_SMALL.create();
      } else if ($$8 >= 2147483647L) {
         throw ERROR_RANGE_TOO_LARGE.create();
      } else {
         int $$9 = Mth.randomBetweenInclusive($$4, $$6, $$7);
         if (p_296222_) {
            p_295774_.getServer()
               .getPlayerList()
               .broadcastSystemMessage(Component.translatable("commands.random.roll", p_295774_.getDisplayName(), $$9, $$6, $$7), false);
         } else {
            p_295774_.sendSuccess(() -> Component.translatable("commands.random.sample.success", $$9), false);
         }

         return $$9;
      }
   }

   private static int resetSequence(CommandSourceStack p_295984_, ResourceLocation p_296220_) throws CommandSyntaxException {
      p_295984_.getLevel().getRandomSequences().reset(p_296220_);
      p_295984_.sendSuccess(() -> Component.translatable("commands.random.reset.success", Component.translationArg(p_296220_)), false);
      return 1;
   }

   private static int resetSequence(CommandSourceStack p_296416_, ResourceLocation p_294611_, int p_295199_, boolean p_295241_, boolean p_294844_) throws CommandSyntaxException {
      p_296416_.getLevel().getRandomSequences().reset(p_294611_, p_295199_, p_295241_, p_294844_);
      p_296416_.sendSuccess(() -> Component.translatable("commands.random.reset.success", Component.translationArg(p_294611_)), false);
      return 1;
   }

   private static int resetAllSequences(CommandSourceStack p_294291_) {
      int $$1 = p_294291_.getLevel().getRandomSequences().clear();
      p_294291_.sendSuccess(() -> Component.translatable("commands.random.reset.all.success", $$1), false);
      return $$1;
   }

   private static int resetAllSequencesAndSetNewDefaults(CommandSourceStack p_294151_, int p_295754_, boolean p_294405_, boolean p_294396_) {
      RandomSequences $$4 = p_294151_.getLevel().getRandomSequences();
      $$4.setSeedDefaults(p_295754_, p_294405_, p_294396_);
      int $$5 = $$4.clear();
      p_294151_.sendSuccess(() -> Component.translatable("commands.random.reset.all.success", $$5), false);
      return $$5;
   }
}
