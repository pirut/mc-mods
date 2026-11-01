package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
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
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class AdvancementCommands {
   private static final DynamicCommandExceptionType ERROR_NO_ACTION_PERFORMED = new DynamicCommandExceptionType(p_311534_ -> (Component)p_311534_);
   private static final Dynamic2CommandExceptionType ERROR_CRITERION_NOT_FOUND = new Dynamic2CommandExceptionType(
      (p_351729_, p_351730_) -> Component.translatableEscape("commands.advancement.criterionNotFound", p_351729_, p_351730_)
   );

   public static void register(CommandDispatcher<CommandSourceStack> p_136311_) {
      p_136311_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("advancement")
                  .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
               .then(
                  Commands.literal("grant")
                     .then(
                        ((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument(
                                          "targets", EntityArgument.players()
                                       )
                                       .then(
                                          Commands.literal("only")
                                             .then(
                                                ((RequiredArgumentBuilder)Commands.argument("advancement", ResourceKeyArgument.key(Registries.ADVANCEMENT))
                                                      .executes(
                                                         p_378980_ -> perform(
                                                            (CommandSourceStack)p_378980_.getSource(),
                                                            EntityArgument.getPlayers(p_378980_, "targets"),
                                                            AdvancementCommands.Action.GRANT,
                                                            getAdvancements(
                                                               p_378980_,
                                                               ResourceKeyArgument.getAdvancement(p_378980_, "advancement"),
                                                               AdvancementCommands.Mode.ONLY
                                                            )
                                                         )
                                                      ))
                                                   .then(
                                                      Commands.argument("criterion", StringArgumentType.greedyString())
                                                         .suggests(
                                                            (p_378964_, p_378965_) -> SharedSuggestionProvider.suggest(
                                                               ResourceKeyArgument.getAdvancement(p_378964_, "advancement").value().criteria().keySet(),
                                                               p_378965_
                                                            )
                                                         )
                                                         .executes(
                                                            p_378981_ -> performCriterion(
                                                               (CommandSourceStack)p_378981_.getSource(),
                                                               EntityArgument.getPlayers(p_378981_, "targets"),
                                                               AdvancementCommands.Action.GRANT,
                                                               ResourceKeyArgument.getAdvancement(p_378981_, "advancement"),
                                                               StringArgumentType.getString(p_378981_, "criterion")
                                                            )
                                                         )
                                                   )
                                             )
                                       ))
                                    .then(
                                       Commands.literal("from")
                                          .then(
                                             Commands.argument("advancement", ResourceKeyArgument.key(Registries.ADVANCEMENT))
                                                .executes(
                                                   p_378962_ -> perform(
                                                      (CommandSourceStack)p_378962_.getSource(),
                                                      EntityArgument.getPlayers(p_378962_, "targets"),
                                                      AdvancementCommands.Action.GRANT,
                                                      getAdvancements(
                                                         p_378962_, ResourceKeyArgument.getAdvancement(p_378962_, "advancement"), AdvancementCommands.Mode.FROM
                                                      )
                                                   )
                                                )
                                          )
                                    ))
                                 .then(
                                    Commands.literal("until")
                                       .then(
                                          Commands.argument("advancement", ResourceKeyArgument.key(Registries.ADVANCEMENT))
                                             .executes(
                                                p_378973_ -> perform(
                                                   (CommandSourceStack)p_378973_.getSource(),
                                                   EntityArgument.getPlayers(p_378973_, "targets"),
                                                   AdvancementCommands.Action.GRANT,
                                                   getAdvancements(
                                                      p_378973_, ResourceKeyArgument.getAdvancement(p_378973_, "advancement"), AdvancementCommands.Mode.UNTIL
                                                   )
                                                )
                                             )
                                       )
                                 ))
                              .then(
                                 Commands.literal("through")
                                    .then(
                                       Commands.argument("advancement", ResourceKeyArgument.key(Registries.ADVANCEMENT))
                                          .executes(
                                             p_378979_ -> perform(
                                                (CommandSourceStack)p_378979_.getSource(),
                                                EntityArgument.getPlayers(p_378979_, "targets"),
                                                AdvancementCommands.Action.GRANT,
                                                getAdvancements(
                                                   p_378979_, ResourceKeyArgument.getAdvancement(p_378979_, "advancement"), AdvancementCommands.Mode.THROUGH
                                                )
                                             )
                                          )
                                    )
                              ))
                           .then(
                              Commands.literal("everything")
                                 .executes(
                                    p_392636_ -> perform(
                                       (CommandSourceStack)p_392636_.getSource(),
                                       EntityArgument.getPlayers(p_392636_, "targets"),
                                       AdvancementCommands.Action.GRANT,
                                       ((CommandSourceStack)p_392636_.getSource()).getServer().getAdvancements().getAllAdvancements(),
                                       false
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
                                             ((RequiredArgumentBuilder)Commands.argument("advancement", ResourceKeyArgument.key(Registries.ADVANCEMENT))
                                                   .executes(
                                                      p_378982_ -> perform(
                                                         (CommandSourceStack)p_378982_.getSource(),
                                                         EntityArgument.getPlayers(p_378982_, "targets"),
                                                         AdvancementCommands.Action.REVOKE,
                                                         getAdvancements(
                                                            p_378982_,
                                                            ResourceKeyArgument.getAdvancement(p_378982_, "advancement"),
                                                            AdvancementCommands.Mode.ONLY
                                                         )
                                                      )
                                                   ))
                                                .then(
                                                   Commands.argument("criterion", StringArgumentType.greedyString())
                                                      .suggests(
                                                         (p_378966_, p_378967_) -> SharedSuggestionProvider.suggest(
                                                            ResourceKeyArgument.getAdvancement(p_378966_, "advancement").value().criteria().keySet(), p_378967_
                                                         )
                                                      )
                                                      .executes(
                                                         p_378974_ -> performCriterion(
                                                            (CommandSourceStack)p_378974_.getSource(),
                                                            EntityArgument.getPlayers(p_378974_, "targets"),
                                                            AdvancementCommands.Action.REVOKE,
                                                            ResourceKeyArgument.getAdvancement(p_378974_, "advancement"),
                                                            StringArgumentType.getString(p_378974_, "criterion")
                                                         )
                                                      )
                                                )
                                          )
                                    ))
                                 .then(
                                    Commands.literal("from")
                                       .then(
                                          Commands.argument("advancement", ResourceKeyArgument.key(Registries.ADVANCEMENT))
                                             .executes(
                                                p_378963_ -> perform(
                                                   (CommandSourceStack)p_378963_.getSource(),
                                                   EntityArgument.getPlayers(p_378963_, "targets"),
                                                   AdvancementCommands.Action.REVOKE,
                                                   getAdvancements(
                                                      p_378963_, ResourceKeyArgument.getAdvancement(p_378963_, "advancement"), AdvancementCommands.Mode.FROM
                                                   )
                                                )
                                             )
                                       )
                                 ))
                              .then(
                                 Commands.literal("until")
                                    .then(
                                       Commands.argument("advancement", ResourceKeyArgument.key(Registries.ADVANCEMENT))
                                          .executes(
                                             p_378978_ -> perform(
                                                (CommandSourceStack)p_378978_.getSource(),
                                                EntityArgument.getPlayers(p_378978_, "targets"),
                                                AdvancementCommands.Action.REVOKE,
                                                getAdvancements(
                                                   p_378978_, ResourceKeyArgument.getAdvancement(p_378978_, "advancement"), AdvancementCommands.Mode.UNTIL
                                                )
                                             )
                                          )
                                    )
                              ))
                           .then(
                              Commands.literal("through")
                                 .then(
                                    Commands.argument("advancement", ResourceKeyArgument.key(Registries.ADVANCEMENT))
                                       .executes(
                                          p_378972_ -> perform(
                                             (CommandSourceStack)p_378972_.getSource(),
                                             EntityArgument.getPlayers(p_378972_, "targets"),
                                             AdvancementCommands.Action.REVOKE,
                                             getAdvancements(
                                                p_378972_, ResourceKeyArgument.getAdvancement(p_378972_, "advancement"), AdvancementCommands.Mode.THROUGH
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
      return perform(p_136320_, p_136321_, p_136322_, p_136323_, true);
   }

   private static int perform(
      CommandSourceStack p_394564_,
      Collection<ServerPlayer> p_394281_,
      AdvancementCommands.Action p_393519_,
      Collection<AdvancementHolder> p_393926_,
      boolean p_393471_
   ) throws CommandSyntaxException {
      int i = 0;

      for (ServerPlayer serverplayer : p_394281_) {
         i += p_393519_.perform(serverplayer, p_393926_, p_393471_);
      }

      if (i == 0) {
         if (p_393926_.size() == 1) {
            if (p_394281_.size() == 1) {
               throw ERROR_NO_ACTION_PERFORMED.create(
                  Component.translatable(
                     p_393519_.getKey() + ".one.to.one.failure", Advancement.name(p_393926_.iterator().next()), p_394281_.iterator().next().getDisplayName()
                  )
               );
            } else {
               throw ERROR_NO_ACTION_PERFORMED.create(
                  Component.translatable(p_393519_.getKey() + ".one.to.many.failure", Advancement.name(p_393926_.iterator().next()), p_394281_.size())
               );
            }
         } else if (p_394281_.size() == 1) {
            throw ERROR_NO_ACTION_PERFORMED.create(
               Component.translatable(p_393519_.getKey() + ".many.to.one.failure", p_393926_.size(), p_394281_.iterator().next().getDisplayName())
            );
         } else {
            throw ERROR_NO_ACTION_PERFORMED.create(Component.translatable(p_393519_.getKey() + ".many.to.many.failure", p_393926_.size(), p_394281_.size()));
         }
      } else {
         if (p_393926_.size() == 1) {
            if (p_394281_.size() == 1) {
               p_394564_.sendSuccess(
                  () -> Component.translatable(
                     p_393519_.getKey() + ".one.to.one.success", Advancement.name(p_393926_.iterator().next()), p_394281_.iterator().next().getDisplayName()
                  ),
                  true
               );
            } else {
               p_394564_.sendSuccess(
                  () -> Component.translatable(p_393519_.getKey() + ".one.to.many.success", Advancement.name(p_393926_.iterator().next()), p_394281_.size()),
                  true
               );
            }
         } else if (p_394281_.size() == 1) {
            p_394564_.sendSuccess(
               () -> Component.translatable(p_393519_.getKey() + ".many.to.one.success", p_393926_.size(), p_394281_.iterator().next().getDisplayName()), true
            );
         } else {
            p_394564_.sendSuccess(() -> Component.translatable(p_393519_.getKey() + ".many.to.many.success", p_393926_.size(), p_394281_.size()), true);
         }

         return i;
      }
   }

   private static int performCriterion(
      CommandSourceStack p_136325_, Collection<ServerPlayer> p_136326_, AdvancementCommands.Action p_136327_, AdvancementHolder p_301324_, String p_136329_
   ) throws CommandSyntaxException {
      int i = 0;
      Advancement advancement = p_301324_.value();
      if (!advancement.criteria().containsKey(p_136329_)) {
         throw ERROR_CRITERION_NOT_FOUND.create(Advancement.name(p_301324_), p_136329_);
      } else {
         for (ServerPlayer serverplayer : p_136326_) {
            if (p_136327_.performCriterion(serverplayer, p_301324_, p_136329_)) {
               i++;
            }
         }

         if (i == 0) {
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

            return i;
         }
      }
   }

   private static List<AdvancementHolder> getAdvancements(
      CommandContext<CommandSourceStack> p_300960_, AdvancementHolder p_301220_, AdvancementCommands.Mode p_136335_
   ) {
      AdvancementTree advancementtree = ((CommandSourceStack)p_300960_.getSource()).getServer().getAdvancements().tree();
      AdvancementNode advancementnode = advancementtree.get(p_301220_);
      if (advancementnode == null) {
         return List.of(p_301220_);
      } else {
         List<AdvancementHolder> list = new ArrayList<>();
         if (p_136335_.parents) {
            for (AdvancementNode advancementnode1 = advancementnode.parent(); advancementnode1 != null; advancementnode1 = advancementnode1.parent()) {
               list.add(advancementnode1.holder());
            }
         }

         list.add(p_301220_);
         if (p_136335_.children) {
            addChildren(advancementnode, list);
         }

         return list;
      }
   }

   private static void addChildren(AdvancementNode p_300871_, List<AdvancementHolder> p_136332_) {
      for (AdvancementNode advancementnode : p_300871_.children()) {
         p_136332_.add(advancementnode.holder());
         addChildren(advancementnode, p_136332_);
      }
   }

   static enum Action {
      GRANT("grant") {
         @Override
         protected boolean perform(ServerPlayer p_136395_, AdvancementHolder p_301029_) {
            AdvancementProgress advancementprogress = p_136395_.getAdvancements().getOrStartProgress(p_301029_);
            if (advancementprogress.isDone()) {
               return false;
            } else {
               for (String s : advancementprogress.getRemainingCriteria()) {
                  p_136395_.getAdvancements().award(p_301029_, s);
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
            AdvancementProgress advancementprogress = p_136406_.getAdvancements().getOrStartProgress(p_301273_);
            if (!advancementprogress.hasProgress()) {
               return false;
            } else {
               for (String s : advancementprogress.getCompletedCriteria()) {
                  p_136406_.getAdvancements().revoke(p_301273_, s);
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

      private Action(String p_136372_) {
         this.key = "commands.advancement." + p_136372_;
      }

      public int perform(ServerPlayer p_136380_, Iterable<AdvancementHolder> p_136381_, boolean p_394360_) {
         int i = 0;
         if (!p_394360_) {
            p_136380_.getAdvancements().flushDirty(p_136380_, true);
         }

         for (AdvancementHolder advancementholder : p_136381_) {
            if (this.perform(p_136380_, advancementholder)) {
               i++;
            }
         }

         if (!p_394360_) {
            p_136380_.getAdvancements().flushDirty(p_136380_, false);
         }

         return i;
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
