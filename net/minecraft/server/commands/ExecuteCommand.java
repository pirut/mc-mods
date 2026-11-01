package net.minecraft.server.commands;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.RedirectModifier;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandResultCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.ExecutionCommandSource;
import net.minecraft.commands.FunctionInstantiationException;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.HeightmapTypeArgument;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.commands.arguments.ObjectiveArgument;
import net.minecraft.commands.arguments.RangeArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.ResourceOrIdArgument;
import net.minecraft.commands.arguments.ResourceOrTagArgument;
import net.minecraft.commands.arguments.ScoreHolderArgument;
import net.minecraft.commands.arguments.SlotsArgument;
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.SwizzleArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.arguments.item.FunctionArgument;
import net.minecraft.commands.arguments.item.ItemPredicateArgument;
import net.minecraft.commands.execution.ChainModifiers;
import net.minecraft.commands.execution.CustomModifierExecutor;
import net.minecraft.commands.execution.ExecutionControl;
import net.minecraft.commands.execution.tasks.BuildContexts;
import net.minecraft.commands.execution.tasks.CallFunction;
import net.minecraft.commands.execution.tasks.FallthroughTask;
import net.minecraft.commands.execution.tasks.IsolatedCall;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.commands.functions.InstantiatedFunction;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.ReloadableServerRegistries;
import net.minecraft.server.bossevents.CustomBossEvent;
import net.minecraft.server.commands.data.DataAccessor;
import net.minecraft.server.commands.data.DataCommands;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Attackable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.Targeting;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.inventory.SlotRange;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;

public class ExecuteCommand {
   private static final int MAX_TEST_AREA = 32768;
   private static final Dynamic2CommandExceptionType ERROR_AREA_TOO_LARGE = new Dynamic2CommandExceptionType(
      (p_304211_, p_304212_) -> Component.translatableEscape("commands.execute.blocks.toobig", p_304211_, p_304212_)
   );
   private static final SimpleCommandExceptionType ERROR_CONDITIONAL_FAILED = new SimpleCommandExceptionType(
      Component.translatable("commands.execute.conditional.fail")
   );
   private static final DynamicCommandExceptionType ERROR_CONDITIONAL_FAILED_COUNT = new DynamicCommandExceptionType(
      p_304210_ -> Component.translatableEscape("commands.execute.conditional.fail_count", p_304210_)
   );
   @VisibleForTesting
   public static final Dynamic2CommandExceptionType ERROR_FUNCTION_CONDITION_INSTANTATION_FAILURE = new Dynamic2CommandExceptionType(
      (p_305676_, p_305677_) -> Component.translatableEscape("commands.execute.function.instantiationFailure", p_305676_, p_305677_)
   );
   private static final SuggestionProvider<CommandSourceStack> SUGGEST_PREDICATE = (p_335218_, p_335219_) -> {
      ReloadableServerRegistries.Holder $$2 = ((CommandSourceStack)p_335218_.getSource()).getServer().reloadableRegistries();
      return SharedSuggestionProvider.suggestResource($$2.getKeys(Registries.PREDICATE), p_335219_);
   };

   public static void register(CommandDispatcher<CommandSourceStack> p_214435_, CommandBuildContext p_214436_) {
      LiteralCommandNode<CommandSourceStack> $$2 = p_214435_.register(
         (LiteralArgumentBuilder)Commands.literal("execute").requires(p_137197_ -> p_137197_.hasPermission(2))
      );
      p_214435_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(
                                                         "execute"
                                                      )
                                                      .requires(p_137103_ -> p_137103_.hasPermission(2)))
                                                   .then(Commands.literal("run").redirect(p_214435_.getRoot())))
                                                .then(addConditionals($$2, Commands.literal("if"), true, p_214436_)))
                                             .then(addConditionals($$2, Commands.literal("unless"), false, p_214436_)))
                                          .then(Commands.literal("as").then(Commands.argument("targets", EntityArgument.entities()).fork($$2, p_137299_ -> {
                                             List<CommandSourceStack> $$1 = Lists.newArrayList();

                                             for (Entity $$2x : EntityArgument.getOptionalEntities(p_137299_, "targets")) {
                                                $$1.add(((CommandSourceStack)p_137299_.getSource()).withEntity($$2x));
                                             }

                                             return $$1;
                                          }))))
                                       .then(
                                          Commands.literal("at")
                                             .then(
                                                Commands.argument("targets", EntityArgument.entities())
                                                   .fork(
                                                      $$2,
                                                      p_284653_ -> {
                                                         List<CommandSourceStack> $$1 = Lists.newArrayList();

                                                         for (Entity $$2x : EntityArgument.getOptionalEntities(p_284653_, "targets")) {
                                                            $$1.add(
                                                               ((CommandSourceStack)p_284653_.getSource())
                                                                  .withLevel((ServerLevel)$$2x.level())
                                                                  .withPosition($$2x.position())
                                                                  .withRotation($$2x.getRotationVector())
                                                            );
                                                         }

                                                         return $$1;
                                                      }
                                                   )
                                             )
                                       ))
                                    .then(
                                       ((LiteralArgumentBuilder)Commands.literal("store").then(wrapStores($$2, Commands.literal("result"), true)))
                                          .then(wrapStores($$2, Commands.literal("success"), false))
                                    ))
                                 .then(
                                    ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("positioned")
                                             .then(
                                                Commands.argument("pos", Vec3Argument.vec3())
                                                   .redirect(
                                                      $$2,
                                                      p_137295_ -> ((CommandSourceStack)p_137295_.getSource())
                                                         .withPosition(Vec3Argument.getVec3(p_137295_, "pos"))
                                                         .withAnchor(EntityAnchorArgument.Anchor.FEET)
                                                   )
                                             ))
                                          .then(Commands.literal("as").then(Commands.argument("targets", EntityArgument.entities()).fork($$2, p_137293_ -> {
                                             List<CommandSourceStack> $$1 = Lists.newArrayList();

                                             for (Entity $$2x : EntityArgument.getOptionalEntities(p_137293_, "targets")) {
                                                $$1.add(((CommandSourceStack)p_137293_.getSource()).withPosition($$2x.position()));
                                             }

                                             return $$1;
                                          }))))
                                       .then(
                                          Commands.literal("over")
                                             .then(Commands.argument("heightmap", HeightmapTypeArgument.heightmap()).redirect($$2, p_274814_ -> {
                                                Vec3 $$1 = ((CommandSourceStack)p_274814_.getSource()).getPosition();
                                                ServerLevel $$2x = ((CommandSourceStack)p_274814_.getSource()).getLevel();
                                                double $$3 = $$1.x();
                                                double $$4 = $$1.z();
                                                if (!$$2x.hasChunk(SectionPos.blockToSectionCoord($$3), SectionPos.blockToSectionCoord($$4))) {
                                                   throw BlockPosArgument.ERROR_NOT_LOADED.create();
                                                } else {
                                                   int $$5 = $$2x.getHeight(
                                                      HeightmapTypeArgument.getHeightmap(p_274814_, "heightmap"), Mth.floor($$3), Mth.floor($$4)
                                                   );
                                                   return ((CommandSourceStack)p_274814_.getSource()).withPosition(new Vec3($$3, $$5, $$4));
                                                }
                                             }))
                                       )
                                 ))
                              .then(
                                 ((LiteralArgumentBuilder)Commands.literal("rotated")
                                       .then(
                                          Commands.argument("rot", RotationArgument.rotation())
                                             .redirect(
                                                $$2,
                                                p_137291_ -> ((CommandSourceStack)p_137291_.getSource())
                                                   .withRotation(
                                                      RotationArgument.getRotation(p_137291_, "rot").getRotation((CommandSourceStack)p_137291_.getSource())
                                                   )
                                             )
                                       ))
                                    .then(Commands.literal("as").then(Commands.argument("targets", EntityArgument.entities()).fork($$2, p_137289_ -> {
                                       List<CommandSourceStack> $$1 = Lists.newArrayList();

                                       for (Entity $$2x : EntityArgument.getOptionalEntities(p_137289_, "targets")) {
                                          $$1.add(((CommandSourceStack)p_137289_.getSource()).withRotation($$2x.getRotationVector()));
                                       }

                                       return $$1;
                                    })))
                              ))
                           .then(
                              ((LiteralArgumentBuilder)Commands.literal("facing")
                                    .then(
                                       Commands.literal("entity")
                                          .then(
                                             Commands.argument("targets", EntityArgument.entities())
                                                .then(Commands.argument("anchor", EntityAnchorArgument.anchor()).fork($$2, p_137287_ -> {
                                                   List<CommandSourceStack> $$1 = Lists.newArrayList();
                                                   EntityAnchorArgument.Anchor $$2x = EntityAnchorArgument.getAnchor(p_137287_, "anchor");

                                                   for (Entity $$3 : EntityArgument.getOptionalEntities(p_137287_, "targets")) {
                                                      $$1.add(((CommandSourceStack)p_137287_.getSource()).facing($$3, $$2x));
                                                   }

                                                   return $$1;
                                                }))
                                          )
                                    ))
                                 .then(
                                    Commands.argument("pos", Vec3Argument.vec3())
                                       .redirect($$2, p_137285_ -> ((CommandSourceStack)p_137285_.getSource()).facing(Vec3Argument.getVec3(p_137285_, "pos")))
                                 )
                           ))
                        .then(
                           Commands.literal("align")
                              .then(
                                 Commands.argument("axes", SwizzleArgument.swizzle())
                                    .redirect(
                                       $$2,
                                       p_137283_ -> ((CommandSourceStack)p_137283_.getSource())
                                          .withPosition(
                                             ((CommandSourceStack)p_137283_.getSource()).getPosition().align(SwizzleArgument.getSwizzle(p_137283_, "axes"))
                                          )
                                    )
                              )
                        ))
                     .then(
                        Commands.literal("anchored")
                           .then(
                              Commands.argument("anchor", EntityAnchorArgument.anchor())
                                 .redirect(
                                    $$2,
                                    p_137281_ -> ((CommandSourceStack)p_137281_.getSource()).withAnchor(EntityAnchorArgument.getAnchor(p_137281_, "anchor"))
                                 )
                           )
                     ))
                  .then(
                     Commands.literal("in")
                        .then(
                           Commands.argument("dimension", DimensionArgument.dimension())
                              .redirect(
                                 $$2,
                                 p_137279_ -> ((CommandSourceStack)p_137279_.getSource()).withLevel(DimensionArgument.getDimension(p_137279_, "dimension"))
                              )
                        )
                  ))
               .then(
                  Commands.literal("summon")
                     .then(
                        Commands.argument("entity", ResourceArgument.resource(p_214436_, Registries.ENTITY_TYPE))
                           .suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                           .redirect(
                              $$2,
                              p_269759_ -> spawnEntityAndRedirect(
                                 (CommandSourceStack)p_269759_.getSource(), ResourceArgument.getSummonableEntityType(p_269759_, "entity")
                              )
                           )
                     )
               ))
            .then(createRelationOperations($$2, Commands.literal("on")))
      );
   }

   private static ArgumentBuilder<CommandSourceStack, ?> wrapStores(
      LiteralCommandNode<CommandSourceStack> p_137094_, LiteralArgumentBuilder<CommandSourceStack> p_137095_, boolean p_137096_
   ) {
      p_137095_.then(
         Commands.literal("score")
            .then(
               Commands.argument("targets", ScoreHolderArgument.scoreHolders())
                  .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                  .then(
                     Commands.argument("objective", ObjectiveArgument.objective())
                        .redirect(
                           p_137094_,
                           p_137271_ -> storeValue(
                              (CommandSourceStack)p_137271_.getSource(),
                              ScoreHolderArgument.getNamesWithDefaultWildcard(p_137271_, "targets"),
                              ObjectiveArgument.getObjective(p_137271_, "objective"),
                              p_137096_
                           )
                        )
                  )
            )
      );
      p_137095_.then(
         Commands.literal("bossbar")
            .then(
               ((RequiredArgumentBuilder)Commands.argument("id", ResourceLocationArgument.id())
                     .suggests(BossBarCommands.SUGGEST_BOSS_BAR)
                     .then(
                        Commands.literal("value")
                           .redirect(
                              p_137094_,
                              p_137259_ -> storeValue((CommandSourceStack)p_137259_.getSource(), BossBarCommands.getBossBar(p_137259_), true, p_137096_)
                           )
                     ))
                  .then(
                     Commands.literal("max")
                        .redirect(
                           p_137094_,
                           p_137247_ -> storeValue((CommandSourceStack)p_137247_.getSource(), BossBarCommands.getBossBar(p_137247_), false, p_137096_)
                        )
                  )
            )
      );

      for (DataCommands.DataProvider $$3 : DataCommands.TARGET_PROVIDERS) {
         $$3.wrap(
            p_137095_,
            p_137101_ -> p_137101_.then(
               ((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument(
                                    "path", NbtPathArgument.nbtPath()
                                 )
                                 .then(
                                    Commands.literal("int")
                                       .then(
                                          Commands.argument("scale", DoubleArgumentType.doubleArg())
                                             .redirect(
                                                p_137094_,
                                                p_180216_ -> storeData(
                                                   (CommandSourceStack)p_180216_.getSource(),
                                                   $$3.access(p_180216_),
                                                   NbtPathArgument.getPath(p_180216_, "path"),
                                                   p_180219_ -> IntTag.valueOf((int)(p_180219_ * DoubleArgumentType.getDouble(p_180216_, "scale"))),
                                                   p_137096_
                                                )
                                             )
                                       )
                                 ))
                              .then(
                                 Commands.literal("float")
                                    .then(
                                       Commands.argument("scale", DoubleArgumentType.doubleArg())
                                          .redirect(
                                             p_137094_,
                                             p_180209_ -> storeData(
                                                (CommandSourceStack)p_180209_.getSource(),
                                                $$3.access(p_180209_),
                                                NbtPathArgument.getPath(p_180209_, "path"),
                                                p_180212_ -> FloatTag.valueOf((float)(p_180212_ * DoubleArgumentType.getDouble(p_180209_, "scale"))),
                                                p_137096_
                                             )
                                          )
                                    )
                              ))
                           .then(
                              Commands.literal("short")
                                 .then(
                                    Commands.argument("scale", DoubleArgumentType.doubleArg())
                                       .redirect(
                                          p_137094_,
                                          p_180199_ -> storeData(
                                             (CommandSourceStack)p_180199_.getSource(),
                                             $$3.access(p_180199_),
                                             NbtPathArgument.getPath(p_180199_, "path"),
                                             p_180202_ -> ShortTag.valueOf((short)(p_180202_ * DoubleArgumentType.getDouble(p_180199_, "scale"))),
                                             p_137096_
                                          )
                                       )
                                 )
                           ))
                        .then(
                           Commands.literal("long")
                              .then(
                                 Commands.argument("scale", DoubleArgumentType.doubleArg())
                                    .redirect(
                                       p_137094_,
                                       p_180189_ -> storeData(
                                          (CommandSourceStack)p_180189_.getSource(),
                                          $$3.access(p_180189_),
                                          NbtPathArgument.getPath(p_180189_, "path"),
                                          p_180192_ -> LongTag.valueOf((long)(p_180192_ * DoubleArgumentType.getDouble(p_180189_, "scale"))),
                                          p_137096_
                                       )
                                    )
                              )
                        ))
                     .then(
                        Commands.literal("double")
                           .then(
                              Commands.argument("scale", DoubleArgumentType.doubleArg())
                                 .redirect(
                                    p_137094_,
                                    p_180179_ -> storeData(
                                       (CommandSourceStack)p_180179_.getSource(),
                                       $$3.access(p_180179_),
                                       NbtPathArgument.getPath(p_180179_, "path"),
                                       p_180182_ -> DoubleTag.valueOf(p_180182_ * DoubleArgumentType.getDouble(p_180179_, "scale")),
                                       p_137096_
                                    )
                                 )
                           )
                     ))
                  .then(
                     Commands.literal("byte")
                        .then(
                           Commands.argument("scale", DoubleArgumentType.doubleArg())
                              .redirect(
                                 p_137094_,
                                 p_180156_ -> storeData(
                                    (CommandSourceStack)p_180156_.getSource(),
                                    $$3.access(p_180156_),
                                    NbtPathArgument.getPath(p_180156_, "path"),
                                    p_180165_ -> ByteTag.valueOf((byte)(p_180165_ * DoubleArgumentType.getDouble(p_180156_, "scale"))),
                                    p_137096_
                                 )
                              )
                        )
                  )
            )
         );
      }

      return p_137095_;
   }

   private static CommandSourceStack storeValue(CommandSourceStack p_137108_, Collection<ScoreHolder> p_137109_, Objective p_137110_, boolean p_137111_) {
      Scoreboard $$4 = p_137108_.getServer().getScoreboard();
      return p_137108_.withCallback((p_137137_, p_137138_) -> {
         for (ScoreHolder $$6 : p_137109_) {
            ScoreAccess $$7 = $$4.getOrCreatePlayerScore($$6, p_137110_);
            int $$8 = p_137111_ ? p_137138_ : (p_137137_ ? 1 : 0);
            $$7.set($$8);
         }
      }, CommandResultCallback::chain);
   }

   private static CommandSourceStack storeValue(CommandSourceStack p_137113_, CustomBossEvent p_137114_, boolean p_137115_, boolean p_137116_) {
      return p_137113_.withCallback((p_137186_, p_137187_) -> {
         int $$5 = p_137116_ ? p_137187_ : (p_137186_ ? 1 : 0);
         if (p_137115_) {
            p_137114_.setValue($$5);
         } else {
            p_137114_.setMax($$5);
         }
      }, CommandResultCallback::chain);
   }

   private static CommandSourceStack storeData(
      CommandSourceStack p_137118_, DataAccessor p_137119_, NbtPathArgument.NbtPath p_137120_, IntFunction<Tag> p_137121_, boolean p_137122_
   ) {
      return p_137118_.withCallback((p_137154_, p_137155_) -> {
         try {
            CompoundTag $$6 = p_137119_.getData();
            int $$7 = p_137122_ ? p_137155_ : (p_137154_ ? 1 : 0);
            p_137120_.set($$6, p_137121_.apply($$7));
            p_137119_.setData($$6);
         } catch (CommandSyntaxException var8) {
         }
      }, CommandResultCallback::chain);
   }

   private static boolean isChunkLoaded(ServerLevel p_265261_, BlockPos p_265260_) {
      ChunkPos $$2 = new ChunkPos(p_265260_);
      LevelChunk $$3 = p_265261_.getChunkSource().getChunkNow($$2.x, $$2.z);
      return $$3 == null ? false : $$3.getFullStatus() == FullChunkStatus.ENTITY_TICKING && p_265261_.areEntitiesLoaded($$2.toLong());
   }

   private static ArgumentBuilder<CommandSourceStack, ?> addConditionals(
      CommandNode<CommandSourceStack> p_214438_, LiteralArgumentBuilder<CommandSourceStack> p_214439_, boolean p_214440_, CommandBuildContext p_214441_
   ) {
      ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)p_214439_.then(
                                    Commands.literal("block")
                                       .then(
                                          Commands.argument("pos", BlockPosArgument.blockPos())
                                             .then(
                                                addConditional(
                                                   p_214438_,
                                                   Commands.argument("block", BlockPredicateArgument.blockPredicate(p_214441_)),
                                                   p_214440_,
                                                   p_137277_ -> BlockPredicateArgument.getBlockPredicate(p_137277_, "block")
                                                      .test(
                                                         new BlockInWorld(
                                                            ((CommandSourceStack)p_137277_.getSource()).getLevel(),
                                                            BlockPosArgument.getLoadedBlockPos(p_137277_, "pos"),
                                                            true
                                                         )
                                                      )
                                                )
                                             )
                                       )
                                 ))
                                 .then(
                                    Commands.literal("biome")
                                       .then(
                                          Commands.argument("pos", BlockPosArgument.blockPos())
                                             .then(
                                                addConditional(
                                                   p_214438_,
                                                   Commands.argument("biome", ResourceOrTagArgument.resourceOrTag(p_214441_, Registries.BIOME)),
                                                   p_214440_,
                                                   p_313477_ -> ResourceOrTagArgument.getResourceOrTag(p_313477_, "biome", Registries.BIOME)
                                                      .test(
                                                         ((CommandSourceStack)p_313477_.getSource())
                                                            .getLevel()
                                                            .getBiome(BlockPosArgument.getLoadedBlockPos(p_313477_, "pos"))
                                                      )
                                                )
                                             )
                                       )
                                 ))
                              .then(
                                 Commands.literal("loaded")
                                    .then(
                                       addConditional(
                                          p_214438_,
                                          Commands.argument("pos", BlockPosArgument.blockPos()),
                                          p_214440_,
                                          p_269757_ -> isChunkLoaded(
                                             ((CommandSourceStack)p_269757_.getSource()).getLevel(), BlockPosArgument.getBlockPos(p_269757_, "pos")
                                          )
                                       )
                                    )
                              ))
                           .then(
                              Commands.literal("dimension")
                                 .then(
                                    addConditional(
                                       p_214438_,
                                       Commands.argument("dimension", DimensionArgument.dimension()),
                                       p_214440_,
                                       p_264789_ -> DimensionArgument.getDimension(p_264789_, "dimension")
                                          == ((CommandSourceStack)p_264789_.getSource()).getLevel()
                                    )
                                 )
                           ))
                        .then(
                           Commands.literal("score")
                              .then(
                                 Commands.argument("target", ScoreHolderArgument.scoreHolder())
                                    .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                    .then(
                                       ((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument(
                                                            "targetObjective", ObjectiveArgument.objective()
                                                         )
                                                         .then(
                                                            Commands.literal("=")
                                                               .then(
                                                                  Commands.argument("source", ScoreHolderArgument.scoreHolder())
                                                                     .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                                                     .then(
                                                                        addConditional(
                                                                           p_214438_,
                                                                           Commands.argument("sourceObjective", ObjectiveArgument.objective()),
                                                                           p_214440_,
                                                                           p_313481_ -> checkScore(p_313481_, (p_313485_, p_313486_) -> p_313485_ == p_313486_)
                                                                        )
                                                                     )
                                                               )
                                                         ))
                                                      .then(
                                                         Commands.literal("<")
                                                            .then(
                                                               Commands.argument("source", ScoreHolderArgument.scoreHolder())
                                                                  .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                                                  .then(
                                                                     addConditional(
                                                                        p_214438_,
                                                                        Commands.argument("sourceObjective", ObjectiveArgument.objective()),
                                                                        p_214440_,
                                                                        p_313476_ -> checkScore(p_313476_, (p_313478_, p_313479_) -> p_313478_ < p_313479_)
                                                                     )
                                                                  )
                                                            )
                                                      ))
                                                   .then(
                                                      Commands.literal("<=")
                                                         .then(
                                                            Commands.argument("source", ScoreHolderArgument.scoreHolder())
                                                               .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                                               .then(
                                                                  addConditional(
                                                                     p_214438_,
                                                                     Commands.argument("sourceObjective", ObjectiveArgument.objective()),
                                                                     p_214440_,
                                                                     p_313482_ -> checkScore(p_313482_, (p_313473_, p_313474_) -> p_313473_ <= p_313474_)
                                                                  )
                                                               )
                                                         )
                                                   ))
                                                .then(
                                                   Commands.literal(">")
                                                      .then(
                                                         Commands.argument("source", ScoreHolderArgument.scoreHolder())
                                                            .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                                            .then(
                                                               addConditional(
                                                                  p_214438_,
                                                                  Commands.argument("sourceObjective", ObjectiveArgument.objective()),
                                                                  p_214440_,
                                                                  p_313475_ -> checkScore(p_313475_, (p_313487_, p_313488_) -> p_313487_ > p_313488_)
                                                               )
                                                            )
                                                      )
                                                ))
                                             .then(
                                                Commands.literal(">=")
                                                   .then(
                                                      Commands.argument("source", ScoreHolderArgument.scoreHolder())
                                                         .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                                         .then(
                                                            addConditional(
                                                               p_214438_,
                                                               Commands.argument("sourceObjective", ObjectiveArgument.objective()),
                                                               p_214440_,
                                                               p_313480_ -> checkScore(p_313480_, (p_313483_, p_313484_) -> p_313483_ >= p_313484_)
                                                            )
                                                         )
                                                   )
                                             ))
                                          .then(
                                             Commands.literal("matches")
                                                .then(
                                                   addConditional(
                                                      p_214438_,
                                                      Commands.argument("range", RangeArgument.intRange()),
                                                      p_214440_,
                                                      p_137216_ -> checkScore(p_137216_, RangeArgument.Ints.getRange(p_137216_, "range"))
                                                   )
                                                )
                                          )
                                    )
                              )
                        ))
                     .then(
                        Commands.literal("blocks")
                           .then(
                              Commands.argument("start", BlockPosArgument.blockPos())
                                 .then(
                                    Commands.argument("end", BlockPosArgument.blockPos())
                                       .then(
                                          ((RequiredArgumentBuilder)Commands.argument("destination", BlockPosArgument.blockPos())
                                                .then(addIfBlocksConditional(p_214438_, Commands.literal("all"), p_214440_, false)))
                                             .then(addIfBlocksConditional(p_214438_, Commands.literal("masked"), p_214440_, true))
                                       )
                                 )
                           )
                     ))
                  .then(
                     Commands.literal("entity")
                        .then(
                           ((RequiredArgumentBuilder)Commands.argument("entities", EntityArgument.entities())
                                 .fork(
                                    p_214438_, p_137232_ -> expect(p_137232_, p_214440_, !EntityArgument.getOptionalEntities(p_137232_, "entities").isEmpty())
                                 ))
                              .executes(
                                 createNumericConditionalHandler(p_214440_, p_137189_ -> EntityArgument.getOptionalEntities(p_137189_, "entities").size())
                              )
                        )
                  ))
               .then(
                  Commands.literal("predicate")
                     .then(
                        addConditional(
                           p_214438_,
                           Commands.argument("predicate", ResourceOrIdArgument.lootPredicate(p_214441_)).suggests(SUGGEST_PREDICATE),
                           p_214440_,
                           p_335217_ -> checkCustomPredicate(
                              (CommandSourceStack)p_335217_.getSource(), ResourceOrIdArgument.getLootPredicate(p_335217_, "predicate")
                           )
                        )
                     )
               ))
            .then(
               Commands.literal("function")
                  .then(
                     Commands.argument("name", FunctionArgument.functions())
                        .suggests(FunctionCommand.SUGGEST_FUNCTION)
                        .fork(p_214438_, new ExecuteCommand.ExecuteIfFunctionCustomModifier(p_214440_))
                  )
            ))
         .then(
            ((LiteralArgumentBuilder)Commands.literal("items")
                  .then(
                     Commands.literal("entity")
                        .then(
                           Commands.argument("entities", EntityArgument.entities())
                              .then(
                                 Commands.argument("slots", SlotsArgument.slots())
                                    .then(
                                       ((RequiredArgumentBuilder)Commands.argument("item_predicate", ItemPredicateArgument.itemPredicate(p_214441_))
                                             .fork(
                                                p_214438_,
                                                p_332574_ -> expect(
                                                   p_332574_,
                                                   p_214440_,
                                                   countItems(
                                                         EntityArgument.getEntities(p_332574_, "entities"),
                                                         SlotsArgument.getSlots(p_332574_, "slots"),
                                                         ItemPredicateArgument.getItemPredicate(p_332574_, "item_predicate")
                                                      )
                                                      > 0
                                                )
                                             ))
                                          .executes(
                                             createNumericConditionalHandler(
                                                p_214440_,
                                                p_332569_ -> countItems(
                                                   EntityArgument.getEntities(p_332569_, "entities"),
                                                   SlotsArgument.getSlots(p_332569_, "slots"),
                                                   ItemPredicateArgument.getItemPredicate(p_332569_, "item_predicate")
                                                )
                                             )
                                          )
                                    )
                              )
                        )
                  ))
               .then(
                  Commands.literal("block")
                     .then(
                        Commands.argument("pos", BlockPosArgument.blockPos())
                           .then(
                              Commands.argument("slots", SlotsArgument.slots())
                                 .then(
                                    ((RequiredArgumentBuilder)Commands.argument("item_predicate", ItemPredicateArgument.itemPredicate(p_214441_))
                                          .fork(
                                             p_214438_,
                                             p_332571_ -> expect(
                                                p_332571_,
                                                p_214440_,
                                                countItems(
                                                      (CommandSourceStack)p_332571_.getSource(),
                                                      BlockPosArgument.getLoadedBlockPos(p_332571_, "pos"),
                                                      SlotsArgument.getSlots(p_332571_, "slots"),
                                                      ItemPredicateArgument.getItemPredicate(p_332571_, "item_predicate")
                                                   )
                                                   > 0
                                             )
                                          ))
                                       .executes(
                                          createNumericConditionalHandler(
                                             p_214440_,
                                             p_332572_ -> countItems(
                                                (CommandSourceStack)p_332572_.getSource(),
                                                BlockPosArgument.getLoadedBlockPos(p_332572_, "pos"),
                                                SlotsArgument.getSlots(p_332572_, "slots"),
                                                ItemPredicateArgument.getItemPredicate(p_332572_, "item_predicate")
                                             )
                                          )
                                       )
                                 )
                           )
                     )
               )
         );

      for (DataCommands.DataProvider $$4 : DataCommands.SOURCE_PROVIDERS) {
         p_214439_.then(
            $$4.wrap(
               Commands.literal("data"),
               p_137092_ -> p_137092_.then(
                  ((RequiredArgumentBuilder)Commands.argument("path", NbtPathArgument.nbtPath())
                        .fork(
                           p_214438_,
                           p_180175_ -> expect(p_180175_, p_214440_, checkMatchingData($$4.access(p_180175_), NbtPathArgument.getPath(p_180175_, "path")) > 0)
                        ))
                     .executes(
                        createNumericConditionalHandler(
                           p_214440_, p_180152_ -> checkMatchingData($$4.access(p_180152_), NbtPathArgument.getPath(p_180152_, "path"))
                        )
                     )
               )
            )
         );
      }

      return p_214439_;
   }

   private static int countItems(Iterable<? extends Entity> p_332642_, SlotRange p_332729_, Predicate<ItemStack> p_332803_) {
      int $$3 = 0;

      for (Entity $$4 : p_332642_) {
         IntList $$5 = p_332729_.slots();

         for (int $$6 = 0; $$6 < $$5.size(); $$6++) {
            int $$7 = $$5.getInt($$6);
            SlotAccess $$8 = $$4.getSlot($$7);
            ItemStack $$9 = $$8.get();
            if (p_332803_.test($$9)) {
               $$3 += $$9.getCount();
            }
         }
      }

      return $$3;
   }

   private static int countItems(CommandSourceStack p_332730_, BlockPos p_332679_, SlotRange p_332656_, Predicate<ItemStack> p_332801_) throws CommandSyntaxException {
      int $$4 = 0;
      Container $$5 = ItemCommands.getContainer(p_332730_, p_332679_, ItemCommands.ERROR_SOURCE_NOT_A_CONTAINER);
      int $$6 = $$5.getContainerSize();
      IntList $$7 = p_332656_.slots();

      for (int $$8 = 0; $$8 < $$7.size(); $$8++) {
         int $$9 = $$7.getInt($$8);
         if ($$9 >= 0 && $$9 < $$6) {
            ItemStack $$10 = $$5.getItem($$9);
            if (p_332801_.test($$10)) {
               $$4 += $$10.getCount();
            }
         }
      }

      return $$4;
   }

   private static Command<CommandSourceStack> createNumericConditionalHandler(boolean p_137167_, ExecuteCommand.CommandNumericPredicate p_137168_) {
      return p_137167_ ? p_288391_ -> {
         int $$2 = p_137168_.test(p_288391_);
         if ($$2 > 0) {
            ((CommandSourceStack)p_288391_.getSource()).sendSuccess(() -> Component.translatable("commands.execute.conditional.pass_count", $$2), false);
            return $$2;
         } else {
            throw ERROR_CONDITIONAL_FAILED.create();
         }
      } : p_288393_ -> {
         int $$2 = p_137168_.test(p_288393_);
         if ($$2 == 0) {
            ((CommandSourceStack)p_288393_.getSource()).sendSuccess(() -> Component.translatable("commands.execute.conditional.pass"), false);
            return 1;
         } else {
            throw ERROR_CONDITIONAL_FAILED_COUNT.create($$2);
         }
      };
   }

   private static int checkMatchingData(DataAccessor p_137146_, NbtPathArgument.NbtPath p_137147_) throws CommandSyntaxException {
      return p_137147_.countMatching(p_137146_.getData());
   }

   private static boolean checkScore(CommandContext<CommandSourceStack> p_137065_, ExecuteCommand.IntBiPredicate p_313900_) throws CommandSyntaxException {
      ScoreHolder $$2 = ScoreHolderArgument.getName(p_137065_, "target");
      Objective $$3 = ObjectiveArgument.getObjective(p_137065_, "targetObjective");
      ScoreHolder $$4 = ScoreHolderArgument.getName(p_137065_, "source");
      Objective $$5 = ObjectiveArgument.getObjective(p_137065_, "sourceObjective");
      Scoreboard $$6 = ((CommandSourceStack)p_137065_.getSource()).getServer().getScoreboard();
      ReadOnlyScoreInfo $$7 = $$6.getPlayerScoreInfo($$2, $$3);
      ReadOnlyScoreInfo $$8 = $$6.getPlayerScoreInfo($$4, $$5);
      return $$7 != null && $$8 != null ? p_313900_.test($$7.value(), $$8.value()) : false;
   }

   private static boolean checkScore(CommandContext<CommandSourceStack> p_137059_, MinMaxBounds.Ints p_137060_) throws CommandSyntaxException {
      ScoreHolder $$2 = ScoreHolderArgument.getName(p_137059_, "target");
      Objective $$3 = ObjectiveArgument.getObjective(p_137059_, "targetObjective");
      Scoreboard $$4 = ((CommandSourceStack)p_137059_.getSource()).getServer().getScoreboard();
      ReadOnlyScoreInfo $$5 = $$4.getPlayerScoreInfo($$2, $$3);
      return $$5 == null ? false : p_137060_.matches($$5.value());
   }

   private static boolean checkCustomPredicate(CommandSourceStack p_137105_, Holder<LootItemCondition> p_335673_) {
      ServerLevel $$2 = p_137105_.getLevel();
      LootParams $$3 = new LootParams.Builder($$2)
         .withParameter(LootContextParams.ORIGIN, p_137105_.getPosition())
         .withOptionalParameter(LootContextParams.THIS_ENTITY, p_137105_.getEntity())
         .create(LootContextParamSets.COMMAND);
      LootContext $$4 = new LootContext.Builder($$3).create(Optional.empty());
      $$4.pushVisitedElement(LootContext.createVisitedEntry(p_335673_.value()));
      return p_335673_.value().test($$4);
   }

   private static Collection<CommandSourceStack> expect(CommandContext<CommandSourceStack> p_137071_, boolean p_137072_, boolean p_137073_) {
      return (Collection<CommandSourceStack>)(p_137073_ == p_137072_
         ? Collections.singleton((CommandSourceStack)p_137071_.getSource())
         : Collections.emptyList());
   }

   private static ArgumentBuilder<CommandSourceStack, ?> addConditional(
      CommandNode<CommandSourceStack> p_137075_, ArgumentBuilder<CommandSourceStack, ?> p_137076_, boolean p_137077_, ExecuteCommand.CommandPredicate p_137078_
   ) {
      return p_137076_.fork(p_137075_, p_137214_ -> expect(p_137214_, p_137077_, p_137078_.test(p_137214_))).executes(p_288396_ -> {
         if (p_137077_ == p_137078_.test(p_288396_)) {
            ((CommandSourceStack)p_288396_.getSource()).sendSuccess(() -> Component.translatable("commands.execute.conditional.pass"), false);
            return 1;
         } else {
            throw ERROR_CONDITIONAL_FAILED.create();
         }
      });
   }

   private static ArgumentBuilder<CommandSourceStack, ?> addIfBlocksConditional(
      CommandNode<CommandSourceStack> p_137080_, ArgumentBuilder<CommandSourceStack, ?> p_137081_, boolean p_137082_, boolean p_137083_
   ) {
      return p_137081_.fork(p_137080_, p_137180_ -> expect(p_137180_, p_137082_, checkRegions(p_137180_, p_137083_).isPresent()))
         .executes(p_137082_ ? p_137210_ -> checkIfRegions(p_137210_, p_137083_) : p_137165_ -> checkUnlessRegions(p_137165_, p_137083_));
   }

   private static int checkIfRegions(CommandContext<CommandSourceStack> p_137068_, boolean p_137069_) throws CommandSyntaxException {
      OptionalInt $$2 = checkRegions(p_137068_, p_137069_);
      if ($$2.isPresent()) {
         ((CommandSourceStack)p_137068_.getSource())
            .sendSuccess(() -> Component.translatable("commands.execute.conditional.pass_count", $$2.getAsInt()), false);
         return $$2.getAsInt();
      } else {
         throw ERROR_CONDITIONAL_FAILED.create();
      }
   }

   private static int checkUnlessRegions(CommandContext<CommandSourceStack> p_137194_, boolean p_137195_) throws CommandSyntaxException {
      OptionalInt $$2 = checkRegions(p_137194_, p_137195_);
      if ($$2.isPresent()) {
         throw ERROR_CONDITIONAL_FAILED_COUNT.create($$2.getAsInt());
      } else {
         ((CommandSourceStack)p_137194_.getSource()).sendSuccess(() -> Component.translatable("commands.execute.conditional.pass"), false);
         return 1;
      }
   }

   private static OptionalInt checkRegions(CommandContext<CommandSourceStack> p_137221_, boolean p_137222_) throws CommandSyntaxException {
      return checkRegions(
         ((CommandSourceStack)p_137221_.getSource()).getLevel(),
         BlockPosArgument.getLoadedBlockPos(p_137221_, "start"),
         BlockPosArgument.getLoadedBlockPos(p_137221_, "end"),
         BlockPosArgument.getLoadedBlockPos(p_137221_, "destination"),
         p_137222_
      );
   }

   private static OptionalInt checkRegions(ServerLevel p_137037_, BlockPos p_137038_, BlockPos p_137039_, BlockPos p_137040_, boolean p_137041_) throws CommandSyntaxException {
      BoundingBox $$5 = BoundingBox.fromCorners(p_137038_, p_137039_);
      BoundingBox $$6 = BoundingBox.fromCorners(p_137040_, p_137040_.offset($$5.getLength()));
      BlockPos $$7 = new BlockPos($$6.minX() - $$5.minX(), $$6.minY() - $$5.minY(), $$6.minZ() - $$5.minZ());
      int $$8 = $$5.getXSpan() * $$5.getYSpan() * $$5.getZSpan();
      if ($$8 > 32768) {
         throw ERROR_AREA_TOO_LARGE.create(32768, $$8);
      } else {
         RegistryAccess $$9 = p_137037_.registryAccess();
         int $$10 = 0;

         for (int $$11 = $$5.minZ(); $$11 <= $$5.maxZ(); $$11++) {
            for (int $$12 = $$5.minY(); $$12 <= $$5.maxY(); $$12++) {
               for (int $$13 = $$5.minX(); $$13 <= $$5.maxX(); $$13++) {
                  BlockPos $$14 = new BlockPos($$13, $$12, $$11);
                  BlockPos $$15 = $$14.offset($$7);
                  BlockState $$16 = p_137037_.getBlockState($$14);
                  if (!p_137041_ || !$$16.is(Blocks.AIR)) {
                     if ($$16 != p_137037_.getBlockState($$15)) {
                        return OptionalInt.empty();
                     }

                     BlockEntity $$17 = p_137037_.getBlockEntity($$14);
                     BlockEntity $$18 = p_137037_.getBlockEntity($$15);
                     if ($$17 != null) {
                        if ($$18 == null) {
                           return OptionalInt.empty();
                        }

                        if ($$18.getType() != $$17.getType()) {
                           return OptionalInt.empty();
                        }

                        if (!$$17.components().equals($$18.components())) {
                           return OptionalInt.empty();
                        }

                        CompoundTag $$19 = $$17.saveCustomOnly($$9);
                        CompoundTag $$20 = $$18.saveCustomOnly($$9);
                        if (!$$19.equals($$20)) {
                           return OptionalInt.empty();
                        }
                     }

                     $$10++;
                  }
               }
            }
         }

         return OptionalInt.of($$10);
      }
   }

   private static RedirectModifier<CommandSourceStack> expandOneToOneEntityRelation(Function<Entity, Optional<Entity>> p_265114_) {
      return p_264786_ -> {
         CommandSourceStack $$2 = (CommandSourceStack)p_264786_.getSource();
         Entity $$3 = $$2.getEntity();
         return (Collection)($$3 == null
            ? List.of()
            : p_265114_.apply($$3).filter(p_264783_ -> !p_264783_.isRemoved()).map(p_264775_ -> List.of($$2.withEntity(p_264775_))).orElse(List.of()));
      };
   }

   private static RedirectModifier<CommandSourceStack> expandOneToManyEntityRelation(Function<Entity, Stream<Entity>> p_265496_) {
      return p_264780_ -> {
         CommandSourceStack $$2 = (CommandSourceStack)p_264780_.getSource();
         Entity $$3 = $$2.getEntity();
         return $$3 == null ? List.of() : p_265496_.apply($$3).filter(p_264784_ -> !p_264784_.isRemoved()).map($$2::withEntity).toList();
      };
   }

   private static LiteralArgumentBuilder<CommandSourceStack> createRelationOperations(
      CommandNode<CommandSourceStack> p_265189_, LiteralArgumentBuilder<CommandSourceStack> p_265783_
   ) {
      return (LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)p_265783_.then(
                              Commands.literal("owner")
                                 .fork(
                                    p_265189_,
                                    expandOneToOneEntityRelation(
                                       p_269758_ -> p_269758_ instanceof OwnableEntity $$1 ? Optional.ofNullable($$1.getOwner()) : Optional.empty()
                                    )
                                 )
                           ))
                           .then(
                              Commands.literal("leasher")
                                 .fork(
                                    p_265189_,
                                    expandOneToOneEntityRelation(
                                       p_353021_ -> p_353021_ instanceof Leashable $$1 ? Optional.ofNullable($$1.getLeashHolder()) : Optional.empty()
                                    )
                                 )
                           ))
                        .then(
                           Commands.literal("target")
                              .fork(
                                 p_265189_,
                                 expandOneToOneEntityRelation(
                                    p_272389_ -> p_272389_ instanceof Targeting $$1 ? Optional.ofNullable($$1.getTarget()) : Optional.empty()
                                 )
                              )
                        ))
                     .then(
                        Commands.literal("attacker")
                           .fork(
                              p_265189_,
                              expandOneToOneEntityRelation(
                                 p_272388_ -> p_272388_ instanceof Attackable $$1 ? Optional.ofNullable($$1.getLastAttacker()) : Optional.empty()
                              )
                           )
                     ))
                  .then(Commands.literal("vehicle").fork(p_265189_, expandOneToOneEntityRelation(p_264776_ -> Optional.ofNullable(p_264776_.getVehicle())))))
               .then(
                  Commands.literal("controller")
                     .fork(p_265189_, expandOneToOneEntityRelation(p_274815_ -> Optional.ofNullable(p_274815_.getControllingPassenger())))
               ))
            .then(
               Commands.literal("origin")
                  .fork(
                     p_265189_,
                     expandOneToOneEntityRelation(
                        p_266631_ -> p_266631_ instanceof TraceableEntity $$1 ? Optional.ofNullable($$1.getOwner()) : Optional.empty()
                     )
                  )
            ))
         .then(Commands.literal("passengers").fork(p_265189_, expandOneToManyEntityRelation(p_264777_ -> p_264777_.getPassengers().stream())));
   }

   private static CommandSourceStack spawnEntityAndRedirect(CommandSourceStack p_270320_, Holder.Reference<EntityType<?>> p_270344_) throws CommandSyntaxException {
      Entity $$2 = SummonCommand.createEntity(p_270320_, p_270344_, p_270320_.getPosition(), new CompoundTag(), true);
      return p_270320_.withEntity($$2);
   }

   public static <T extends ExecutionCommandSource<T>> void scheduleFunctionConditionsAndTest(
      T p_309624_,
      List<T> p_306057_,
      Function<T, T> p_306045_,
      IntPredicate p_306343_,
      ContextChain<T> p_306308_,
      @Nullable CompoundTag p_306076_,
      ExecutionControl<T> p_306202_,
      ExecuteCommand.CommandGetter<T, Collection<CommandFunction<T>>> p_305836_,
      ChainModifiers p_309703_
   ) {
      List<T> $$9 = new ArrayList<>(p_306057_.size());

      Collection<CommandFunction<T>> $$10;
      try {
         $$10 = p_305836_.get(p_306308_.getTopContext().copyFor(p_309624_));
      } catch (CommandSyntaxException var18) {
         p_309624_.handleError(var18, p_309703_.isForked(), p_306202_.tracer());
         return;
      }

      int $$13 = $$10.size();
      if ($$13 != 0) {
         List<InstantiatedFunction<T>> $$14 = new ArrayList<>($$13);

         try {
            for (CommandFunction<T> $$15 : $$10) {
               try {
                  $$14.add($$15.instantiate(p_306076_, p_309624_.dispatcher()));
               } catch (FunctionInstantiationException var17) {
                  throw ERROR_FUNCTION_CONDITION_INSTANTATION_FAILURE.create($$15.id(), var17.messageComponent());
               }
            }
         } catch (CommandSyntaxException var19) {
            p_309624_.handleError(var19, p_309703_.isForked(), p_306202_.tracer());
         }

         for (T $$18 : p_306057_) {
            T $$19 = (T)p_306045_.apply($$18.clearCallbacks());
            CommandResultCallback $$20 = (p_309685_, p_305691_) -> {
               if (p_306343_.test(p_305691_)) {
                  $$9.add($$18);
               }
            };
            p_306202_.queueNext(new IsolatedCall<>(p_309456_ -> {
               for (InstantiatedFunction<T> $$3 : $$14) {
                  p_309456_.queueNext(new CallFunction<>($$3, p_309456_.currentFrame().returnValueConsumer(), true).bind($$19));
               }

               p_309456_.queueNext(FallthroughTask.instance());
            }, $$20));
         }

         ContextChain<T> $$21 = p_306308_.nextStage();
         String $$22 = p_306308_.getTopContext().getInput();
         p_306202_.queueNext(new BuildContexts.Continuation<>($$22, $$21, p_309703_, p_309624_, $$9));
      }
   }

   @FunctionalInterface
   public interface CommandGetter<T, R> {
      R get(CommandContext<T> var1) throws CommandSyntaxException;
   }

   @FunctionalInterface
   interface CommandNumericPredicate {
      int test(CommandContext<CommandSourceStack> var1) throws CommandSyntaxException;
   }

   @FunctionalInterface
   interface CommandPredicate {
      boolean test(CommandContext<CommandSourceStack> var1) throws CommandSyntaxException;
   }

   static class ExecuteIfFunctionCustomModifier implements CustomModifierExecutor.ModifierAdapter<CommandSourceStack> {
      private final IntPredicate check;

      ExecuteIfFunctionCustomModifier(boolean p_305979_) {
         this.check = p_305979_ ? p_305777_ -> p_305777_ != 0 : p_306070_ -> p_306070_ == 0;
      }

      public void apply(
         CommandSourceStack p_309582_,
         List<CommandSourceStack> p_309642_,
         ContextChain<CommandSourceStack> p_309682_,
         ChainModifiers p_309694_,
         ExecutionControl<CommandSourceStack> p_309646_
      ) {
         ExecuteCommand.scheduleFunctionConditionsAndTest(
            p_309582_,
            p_309642_,
            FunctionCommand::modifySenderForExecution,
            this.check,
            p_309682_,
            null,
            p_309646_,
            p_305997_ -> FunctionArgument.getFunctions(p_305997_, "name"),
            p_309694_
         );
      }
   }

   @FunctionalInterface
   interface IntBiPredicate {
      boolean test(int var1, int var2);
   }
}
