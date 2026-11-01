package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class AdvancementCommands {
   private static final DynamicCommandExceptionType ERROR_NO_ACTION_PERFORMED = new DynamicCommandExceptionType(p_311534_ -> (Component)p_311534_);
   private static final Dynamic2CommandExceptionType ERROR_CRITERION_NOT_FOUND = new Dynamic2CommandExceptionType(
      (p_351729_, p_351730_) -> Component.translatableEscape("commands.advancement.criterionNotFound", p_351729_, p_351730_)
   );
   private static final SuggestionProvider<CommandSourceStack> SUGGEST_ADVANCEMENTS = (p_136344_, p_136345_) -> {
      Collection<AdvancementHolder> $$2 = ((CommandSourceStack)p_136344_.getSource()).getServer().getAdvancements().getAllAdvancements();
      return SharedSuggestionProvider.suggestResource($$2.stream().map(AdvancementHolder::id), p_136345_);
   };

   public static void register(CommandDispatcher<CommandSourceStack> p_136311_) {
      p_136311_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("advancement")
                  .requires(p_136318_ -> p_136318_.hasPermission(2)))
               .then(
                  Commands.literal("grant")
                     .then(
                        ((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument(
                                          "targets", EntityArgument.players()
                                       )
                                       .then(
                                          Commands.literal("only")
                                             .then(
                                                ((RequiredArgumentBuilder)Commands.argument("advancement", ResourceLocationArgument.id())
                                                      .suggests(SUGGEST_ADVANCEMENTS)
                                                      .executes(
                                                         p_300759_ -> perform(
                                                            (CommandSourceStack)p_300759_.getSource(),
                                                            EntityArgument.getPlayers(p_300759_, "targets"),
                                                            AdvancementCommands.Action.GRANT,
                                                            getAdvancements(
                                                               p_300759_,
                                                               ResourceLocationArgument.getAdvancement(p_300759_, "advancement"),
                                                               AdvancementCommands.Mode.ONLY
                                                            )
                                                         )
                                                      ))
                                                   .then(
                                                      Commands.argument("criterion", StringArgumentType.greedyString())
                                                         .suggests(
                                                            (p_300739_, p_300740_) -> SharedSuggestionProvider.suggest(
                                                               ResourceLocationArgument.getAdvancement(p_300739_, "advancement").value().criteria().keySet(),
                                                               p_300740_
                                                            )
                                                         )
                                                         .executes(
                                                            p_300748_ -> performCriterion(
                                                               (CommandSourceStack)p_300748_.getSource(),
                                                               EntityArgument.getPlayers(p_300748_, "targets"),
                                                               AdvancementCommands.Action.GRANT,
                                                               ResourceLocationArgument.getAdvancement(p_300748_, "advancement"),
                                                               StringArgumentType.getString(p_300748_, "criterion")
                                                            )
                                                         )
                                                   )
                                             )
                                       ))
                                    .then(
                                       Commands.literal("from")
                                          .then(
                                             Commands.argument("advancement", ResourceLocationArgument.id())
                                                .suggests(SUGGEST_ADVANCEMENTS)
                                                .executes(
                                                   p_300760_ -> perform(
                                                      (CommandSourceStack)p_300760_.getSource(),
                                                      EntityArgument.getPlayers(p_300760_, "targets"),
                                                      AdvancementCommands.Action.GRANT,
                                                      getAdvancements(
                                                         p_300760_,
                                                         ResourceLocationArgument.getAdvancement(p_300760_, "advancement"),
                                                         AdvancementCommands.Mode.FROM
                                                      )
                                                   )
                                                )
                                          )
                                    ))
                                 .then(
                                    Commands.literal("until")
                                       .then(
                                          Commands.argument("advancement", ResourceLocationArgument.id())
                                             .suggests(SUGGEST_ADVANCEMENTS)
                                             .executes(
                                                p_300749_ -> perform(
                                                   (CommandSourceStack)p_300749_.getSource(),
                                                   EntityArgument.getPlayers(p_300749_, "targets"),
                                                   AdvancementCommands.Action.GRANT,
                                                   getAdvancements(
                                                      p_300749_,
                                                      ResourceLocationArgument.getAdvancement(p_300749_, "advancement"),
                                                      AdvancementCommands.Mode.UNTIL
                                                   )
                                                )
                                             )
                                       )
                                 ))
                              .then(
                                 Commands.literal("through")
                                    .then(
                                       Commands.argument("advancement", ResourceLocationArgument.id())
                                          .suggests(SUGGEST_ADVANCEMENTS)
                                          .executes(
                                             p_300741_ -> perform(
                                                (CommandSourceStack)p_300741_.getSource(),
                                                EntityArgument.getPlayers(p_300741_, "targets"),
                                                AdvancementCommands.Action.GRANT,
                                                getAdvancements(
                                                   p_300741_,
                                                   ResourceLocationArgument.getAdvancement(p_300741_, "advancement"),
                                                   AdvancementCommands.Mode.THROUGH
                                                )
                                             )
                                          )
                                    )
                              ))
                           .then(
                              Commands.literal("everything")
                                 .executes(
                                    p_136353_ -> perform(
                                       (CommandSourceStack)p_136353_.getSource(),
                                       EntityArgument.getPlayers(p_136353_, "targets"),
                                       AdvancementCommands.Action.GRANT,
                                       ((CommandSourceStack)p_136353_.getSource()).getServer().getAdvancements().getAllAdvancements()
                                    )
                                 )
                           )
                     )
               ))
            .then(
               Commands.literal("revoke")
                  .then(
                     ((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument(
                                       "targets", EntityArgument.players()
                                    )
                                    .then(
                                       Commands.literal("only")
                                          .then(
                                             ((RequiredArgumentBuilder)Commands.argument("advancement", ResourceLocationArgument.id())
                                                   .suggests(SUGGEST_ADVANCEMENTS)
                                                   .executes(
                                                      p_300745_ -> perform(
                                                         (CommandSourceStack)p_300745_.getSource(),
                                                         EntityArgument.getPlayers(p_300745_, "targets"),
                                                         AdvancementCommands.Action.REVOKE,
                                                         getAdvancements(
                                                            p_300745_,
                                                            ResourceLocationArgument.getAdvancement(p_300745_, "advancement"),
                                                            AdvancementCommands.Mode.ONLY
                                                         )
                                                      )
                                                   ))
                                                .then(
                                                   Commands.argument("criterion", StringArgumentType.greedyString())
                                                      .suggests(
                                                         (p_300746_, p_300747_) -> SharedSuggestionProvider.suggest(
                                                            ResourceLocationArgument.getAdvancement(p_300746_, "advancement").value().criteria().keySet(),
                                                            p_300747_
                                                         )
                                                      )
                                                      .executes(
                                                         p_300737_ -> performCriterion(
                                                            (CommandSourceStack)p_300737_.getSource(),
                                                            EntityArgument.getPlayers(p_300737_, "targets"),
                                                            AdvancementCommands.Action.REVOKE,
                                                            ResourceLocationArgument.getAdvancement(p_300737_, "advancement"),
                                                            StringArgumentType.getString(p_300737_, "criterion")
                                                         )
                                                      )
                                                )
                                          )
                                    ))
                                 .then(
                                    Commands.literal("from")
                                       .then(
                                          Commands.argument("advancement", ResourceLocationArgument.id())
                                             .suggests(SUGGEST_ADVANCEMENTS)
                                             .executes(
                                                p_300758_ -> perform(
                                                   (CommandSourceStack)p_300758_.getSource(),
                                                   EntityArgument.getPlayers(p_300758_, "targets"),
                                                   AdvancementCommands.Action.REVOKE,
                                                   getAdvancements(
                                                      p_300758_,
                                                      ResourceLocationArgument.getAdvancement(p_300758_, "advancement"),
                                                      AdvancementCommands.Mode.FROM
                                                   )
                                                )
                                             )
                                       )
                                 ))
                              .then(
                                 Commands.literal("until")
                                    .then(
                                       Commands.argument("advancement", ResourceLocationArgument.id())
                                          .suggests(SUGGEST_ADVANCEMENTS)
                                          .executes(
                                             p_300738_ -> perform(
                                                (CommandSourceStack)p_300738_.getSource(),
                                                EntityArgument.getPlayers(p_300738_, "targets"),
                                                AdvancementCommands.Action.REVOKE,
                                                getAdvancements(
                                                   p_300738_, ResourceLocationArgument.getAdvancement(p_300738_, "advancement"), AdvancementCommands.Mode.UNTIL
                                                )
                                             )
                                          )
                                    )
                              ))
                           .then(
                              Commands.literal("through")
                                 .then(
                                    Commands.argument("advancement", ResourceLocationArgument.id())
                                       .suggests(SUGGEST_ADVANCEMENTS)
                                       .executes(
                                          p_300764_ -> perform(
                                             (CommandSourceStack)p_300764_.getSource(),
                                             EntityArgument.getPlayers(p_300764_, "targets"),
                                             AdvancementCommands.Action.REVOKE,
                                             getAdvancements(
                                                p_300764_, ResourceLocationArgument.getAdvancement(p_300764_, "advancement"), AdvancementCommands.Mode.THROUGH
                                             )
                                          )
                                       )
                                 )
                           ))
                        .then(
                           Commands.literal("everything")
                              .executes(
                                 p_136313_ -> perform(
                                    (CommandSourceStack)p_136313_.getSource(),
                                    EntityArgument.getPlayers(p_136313_, "targets"),
                                    AdvancementCommands.Action.REVOKE,
                                    ((CommandSourceStack)p_136313_.getSource()).getServer().getAdvancements().getAllAdvancements()
                                 )
                              )
                        )
                  )
            )
      );
   }

   private static int perform(
      CommandSourceStack p_136320_, Collection<ServerPlayer> p_136321_, AdvancementCommands.Action p_136322_, Collection<AdvancementHolder> p_136323_
   ) throws CommandSyntaxException {
      int $$4 = 0;

      for (ServerPlayer $$5 : p_136321_) {
         $$4 += p_136322_.perform($$5, p_136323_);
      }

      if ($$4 == 0) {
         if (p_136323_.size() == 1) {
            if (p_136321_.size() == 1) {
               throw ERROR_NO_ACTION_PERFORMED.create(
                  Component.translatable(
                     p_136322_.getKey() + ".one.to.one.failure", Advancement.name(p_136323_.iterator().next()), p_136321_.iterator().next().getDisplayName()
                  )
               );
            } else {
               throw ERROR_NO_ACTION_PERFORMED.create(
                  Component.translatable(p_136322_.getKey() + ".one.to.many.failure", Advancement.name(p_136323_.iterator().next()), p_136321_.size())
               );
            }
         } else if (p_136321_.size() == 1) {
            throw ERROR_NO_ACTION_PERFORMED.create(
               Component.translatable(p_136322_.getKey() + ".many.to.one.failure", p_136323_.size(), p_136321_.iterator().next().getDisplayName())
            );
         } else {
            throw ERROR_NO_ACTION_PERFORMED.create(Component.translatable(p_136322_.getKey() + ".many.to.many.failure", p_136323_.size(), p_136321_.size()));
         }
      } else {
         if (p_136323_.size() == 1) {
            if (p_136321_.size() == 1) {
               p_136320_.sendSuccess(
                  () -> Component.translatable(
                     p_136322_.getKey() + ".one.to.one.success", Advancement.name(p_136323_.iterator().next()), p_136321_.iterator().next().getDisplayName()
                  ),
                  true
               );
            } else {
               p_136320_.sendSuccess(
                  () -> Component.translatable(p_136322_.getKey() + ".one.to.many.success", Advancement.name(p_136323_.iterator().next()), p_136321_.size()),
                  true
               );
            }
         } else if (p_136321_.size() == 1) {
            p_136320_.sendSuccess(
               () -> Component.translatable(p_136322_.getKey() + ".many.to.one.success", p_136323_.size(), p_136321_.iterator().next().getDisplayName()), true
            );
         } else {
            p_136320_.sendSuccess(() -> Component.translatable(p_136322_.getKey() + ".many.to.many.success", p_136323_.size(), p_136321_.size()), true);
         }

         return $$4;
      }
   }

   private static int performCriterion(
      CommandSourceStack p_136325_, Collection<ServerPlayer> p_136326_, AdvancementCommands.Action p_136327_, AdvancementHolder p_301324_, String p_136329_
   ) throws CommandSyntaxException {
      int $$5 = 0;
      Advancement $$6 = p_301324_.value();
      if (!$$6.criteria().containsKey(p_136329_)) {
         throw ERROR_CRITERION_NOT_FOUND.create(Advancement.name(p_301324_), p_136329_);
      } else {
         for (ServerPlayer $$7 : p_136326_) {
            if (p_136327_.performCriterion($$7, p_301324_, p_136329_)) {
               $$5++;
            }
         }

         if ($$5 == 0) {
            if (p_136326_.size() == 1) {
               throw ERROR_NO_ACTION_PERFORMED.create(
                  Component.translatable(
                     p_136327_.getKey() + ".criterion.to.one.failure", p_136329_, Advancement.name(p_301324_), p_136326_.iterator().next().getDisplayName()
                  )
               );
            } else {
               throw ERROR_NO_ACTION_PERFORMED.create(
                  Component.translatable(p_136327_.getKey() + ".criterion.to.many.failure", p_136329_, Advancement.name(p_301324_), p_136326_.size())
               );
            }
         } else {
            if (p_136326_.size() == 1) {
               p_136325_.sendSuccess(
                  () -> Component.translatable(
                     p_136327_.getKey() + ".criterion.to.one.success", p_136329_, Advancement.name(p_301324_), p_136326_.iterator().next().getDisplayName()
                  ),
                  true
               );
            } else {
               p_136325_.sendSuccess(
                  () -> Component.translatable(p_136327_.getKey() + ".criterion.to.many.success", p_136329_, Advancement.name(p_301324_), p_136326_.size()),
                  true
               );
            }

            return $$5;
         }
      }
   }

   private static List<AdvancementHolder> getAdvancements(
      CommandContext<CommandSourceStack> p_300960_, AdvancementHolder p_301220_, AdvancementCommands.Mode p_136335_
   ) {
      AdvancementTree $$3 = ((CommandSourceStack)p_300960_.getSource()).getServer().getAdvancements().tree();
      AdvancementNode $$4 = $$3.get(p_301220_);
      if ($$4 == null) {
         return List.of(p_301220_);
      } else {
         List<AdvancementHolder> $$5 = new ArrayList<>();
         if (p_136335_.parents) {
            for (AdvancementNode $$6 = $$4.parent(); $$6 != null; $$6 = $$6.parent()) {
               $$5.add($$6.holder());
            }
         }

         $$5.add(p_301220_);
         if (p_136335_.children) {
            addChildren($$4, $$5);
         }

         return $$5;
      }
   }

   private static void addChildren(AdvancementNode p_300871_, List<AdvancementHolder> p_136332_) {
      for (AdvancementNode $$2 : p_300871_.children()) {
         p_136332_.add($$2.holder());
         addChildren($$2, p_136332_);
      }
   }

   static enum Action {
      GRANT("grant") {
         @Override
         protected boolean perform(ServerPlayer p_136395_, AdvancementHolder p_301029_) {
            AdvancementProgress $$2 = p_136395_.getAdvancements().getOrStartProgress(p_301029_);
            if ($$2.isDone()) {
               return false;
            } else {
               for (String $$3 : $$2.getRemainingCriteria()) {
                  p_136395_.getAdvancements().award(p_301029_, $$3);
               }

               return true;
            }
         }

         @Override
         protected boolean performCriterion(ServerPlayer p_136398_, AdvancementHolder p_301300_, String p_136400_) {
            return p_136398_.getAdvancements().award(p_301300_, p_136400_);
         }
      },
      REVOKE("revoke") {
         @Override
         protected boolean perform(ServerPlayer p_136406_, AdvancementHolder p_301273_) {
            AdvancementProgress $$2 = p_136406_.getAdvancements().getOrStartProgress(p_301273_);
            if (!$$2.hasProgress()) {
               return false;
            } else {
               for (String $$3 : $$2.getCompletedCriteria()) {
                  p_136406_.getAdvancements().revoke(p_301273_, $$3);
               }

               return true;
            }
         }

         @Override
         protected boolean performCriterion(ServerPlayer p_136409_, AdvancementHolder p_301094_, String p_136411_) {
            return p_136409_.getAdvancements().revoke(p_301094_, p_136411_);
         }
      };

      private final String key;

      Action(String p_136372_) {
         this.key = "commands.advancement." + p_136372_;
      }

      public int perform(ServerPlayer p_136380_, Iterable<AdvancementHolder> p_136381_) {
         int $$2 = 0;

         for (AdvancementHolder $$3 : p_136381_) {
            if (this.perform(p_136380_, $$3)) {
               $$2++;
            }
         }

         return $$2;
      }

      protected abstract boolean perform(ServerPlayer var1, AdvancementHolder var2);

      protected abstract boolean performCriterion(ServerPlayer var1, AdvancementHolder var2, String var3);

      protected String getKey() {
         return this.key;
      }
   }

   static enum Mode {
      ONLY(false, false),
      THROUGH(true, true),
      FROM(false, true),
      UNTIL(true, false),
      EVERYTHING(true, true);

      final boolean parents;
      final boolean children;

      private Mode(boolean p_136424_, boolean p_136425_) {
         this.parents = p_136424_;
         this.children = p_136425_;
      }
   }
}
