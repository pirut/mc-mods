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
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.logging.LogUtils;
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
import net.minecraft.advancements.criterion.MinMaxBounds;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandResultCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.ExecutionCommandSource;
import net.minecraft.commands.FunctionInstantiationException;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.HeightmapTypeArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.commands.arguments.ObjectiveArgument;
import net.minecraft.commands.arguments.RangeArgument;
import net.minecraft.commands.arguments.ResourceArgument;
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
import net.minecraft.resources.Identifier;
import net.minecraft.server.bossevents.CustomBossEvent;
import net.minecraft.server.commands.data.DataAccessor;
import net.minecraft.server.commands.data.DataCommands;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.Stopwatch;
import net.minecraft.world.Stopwatches;
import net.minecraft.world.entity.Attackable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.SlotProvider;
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
import net.minecraft.world.level.storage.TagValueOutput;
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
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class ExecuteCommand {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int MAX_TEST_AREA = 32768;
   private static final Dynamic2CommandExceptionType ERROR_AREA_TOO_LARGE = new Dynamic2CommandExceptionType(
      (object, object2) -> Component.translatableEscape("commands.execute.blocks.toobig", object, object2)
   );
   private static final SimpleCommandExceptionType ERROR_CONDITIONAL_FAILED = new SimpleCommandExceptionType(
      Component.translatable("commands.execute.conditional.fail")
   );
   private static final DynamicCommandExceptionType ERROR_CONDITIONAL_FAILED_COUNT = new DynamicCommandExceptionType(
      object -> Component.translatableEscape("commands.execute.conditional.fail_count", object)
   );
   @VisibleForTesting
   public static final Dynamic2CommandExceptionType ERROR_FUNCTION_CONDITION_INSTANTATION_FAILURE = new Dynamic2CommandExceptionType(
      (object, object2) -> Component.translatableEscape("commands.execute.function.instantiationFailure", object, object2)
   );

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
      LiteralCommandNode<CommandSourceStack> literalCommandNode = dispatcher.register(
         (LiteralArgumentBuilder)Commands.literal("execute").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
      );
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(
                                                         "execute"
                                                      )
                                                      .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
                                                   .then(Commands.literal("run").redirect(dispatcher.getRoot())))
                                                .then(addConditionals(literalCommandNode, Commands.literal("if"), true, buildContext)))
                                             .then(addConditionals(literalCommandNode, Commands.literal("unless"), false, buildContext)))
                                          .then(
                                             Commands.literal("as")
                                                .then(Commands.argument("targets", EntityArgument.entities()).fork(literalCommandNode, commandContext -> {
                                                   List<CommandSourceStack> list = Lists.newArrayList();

                                                   for (Entity entity : EntityArgument.getOptionalEntities(commandContext, "targets")) {
                                                      list.add(((CommandSourceStack)commandContext.getSource()).withEntity(entity));
                                                   }

                                                   return list;
                                                }))
                                          ))
                                       .then(
                                          Commands.literal("at")
                                             .then(
                                                Commands.argument("targets", EntityArgument.entities())
                                                   .fork(
                                                      literalCommandNode,
                                                      commandContext -> {
                                                         List<CommandSourceStack> list = Lists.newArrayList();

                                                         for (Entity entity : EntityArgument.getOptionalEntities(commandContext, "targets")) {
                                                            list.add(
                                                               ((CommandSourceStack)commandContext.getSource())
                                                                  .withLevel((ServerLevel)entity.level())
                                                                  .withPosition(entity.position())
                                                                  .withRotation(entity.getRotationVector())
                                                            );
                                                         }

                                                         return list;
                                                      }
                                                   )
                                             )
                                       ))
                                    .then(
                                       ((LiteralArgumentBuilder)Commands.literal("store")
                                             .then(wrapStores(literalCommandNode, Commands.literal("result"), true)))
                                          .then(wrapStores(literalCommandNode, Commands.literal("success"), false))
                                    ))
                                 .then(
                                    ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("positioned")
                                             .then(
                                                Commands.argument("pos", Vec3Argument.vec3())
                                                   .redirect(
                                                      literalCommandNode,
                                                      commandContext -> ((CommandSourceStack)commandContext.getSource())
                                                         .withPosition(Vec3Argument.getVec3(commandContext, "pos"))
                                                         .withAnchor(EntityAnchorArgument.Anchor.FEET)
                                                   )
                                             ))
                                          .then(
                                             Commands.literal("as")
                                                .then(Commands.argument("targets", EntityArgument.entities()).fork(literalCommandNode, commandContext -> {
                                                   List<CommandSourceStack> list = Lists.newArrayList();

                                                   for (Entity entity : EntityArgument.getOptionalEntities(commandContext, "targets")) {
                                                      list.add(((CommandSourceStack)commandContext.getSource()).withPosition(entity.position()));
                                                   }

                                                   return list;
                                                }))
                                          ))
                                       .then(
                                          Commands.literal("over")
                                             .then(
                                                Commands.argument("heightmap", HeightmapTypeArgument.heightmap())
                                                   .redirect(literalCommandNode, commandContext -> {
                                                      Vec3 vec3 = ((CommandSourceStack)commandContext.getSource()).getPosition();
                                                      ServerLevel serverLevel = ((CommandSourceStack)commandContext.getSource()).getLevel();
                                                      double d = vec3.x();
                                                      double e = vec3.z();
                                                      if (!serverLevel.hasChunk(SectionPos.blockToSectionCoord(d), SectionPos.blockToSectionCoord(e))) {
                                                         throw BlockPosArgument.ERROR_NOT_LOADED.create();
                                                      } else {
                                                         int i = serverLevel.getHeight(
                                                            HeightmapTypeArgument.getHeightmap(commandContext, "heightmap"), Mth.floor(d), Mth.floor(e)
                                                         );
                                                         return ((CommandSourceStack)commandContext.getSource()).withPosition(new Vec3(d, i, e));
                                                      }
                                                   })
                                             )
                                       )
                                 ))
                              .then(
                                 ((LiteralArgumentBuilder)Commands.literal("rotated")
                                       .then(
                                          Commands.argument("rot", RotationArgument.rotation())
                                             .redirect(
                                                literalCommandNode,
                                                commandContext -> ((CommandSourceStack)commandContext.getSource())
                                                   .withRotation(
                                                      RotationArgument.getRotation(commandContext, "rot")
                                                         .getRotation((CommandSourceStack)commandContext.getSource())
                                                   )
                                             )
                                       ))
                                    .then(
                                       Commands.literal("as")
                                          .then(Commands.argument("targets", EntityArgument.entities()).fork(literalCommandNode, commandContext -> {
                                             List<CommandSourceStack> list = Lists.newArrayList();

                                             for (Entity entity : EntityArgument.getOptionalEntities(commandContext, "targets")) {
                                                list.add(((CommandSourceStack)commandContext.getSource()).withRotation(entity.getRotationVector()));
                                             }

                                             return list;
                                          }))
                                    )
                              ))
                           .then(
                              ((LiteralArgumentBuilder)Commands.literal("facing")
                                    .then(
                                       Commands.literal("entity")
                                          .then(
                                             Commands.argument("targets", EntityArgument.entities())
                                                .then(Commands.argument("anchor", EntityAnchorArgument.anchor()).fork(literalCommandNode, commandContext -> {
                                                   List<CommandSourceStack> list = Lists.newArrayList();
                                                   EntityAnchorArgument.Anchor anchor = EntityAnchorArgument.getAnchor(commandContext, "anchor");

                                                   for (Entity entity : EntityArgument.getOptionalEntities(commandContext, "targets")) {
                                                      list.add(((CommandSourceStack)commandContext.getSource()).facing(entity, anchor));
                                                   }

                                                   return list;
                                                }))
                                          )
                                    ))
                                 .then(
                                    Commands.argument("pos", Vec3Argument.vec3())
                                       .redirect(
                                          literalCommandNode,
                                          commandContext -> ((CommandSourceStack)commandContext.getSource())
                                             .facing(Vec3Argument.getVec3(commandContext, "pos"))
                                       )
                                 )
                           ))
                        .then(
                           Commands.literal("align")
                              .then(
                                 Commands.argument("axes", SwizzleArgument.swizzle())
                                    .redirect(
                                       literalCommandNode,
                                       commandContext -> ((CommandSourceStack)commandContext.getSource())
                                          .withPosition(
                                             ((CommandSourceStack)commandContext.getSource())
                                                .getPosition()
                                                .align(SwizzleArgument.getSwizzle(commandContext, "axes"))
                                          )
                                    )
                              )
                        ))
                     .then(
                        Commands.literal("anchored")
                           .then(
                              Commands.argument("anchor", EntityAnchorArgument.anchor())
                                 .redirect(
                                    literalCommandNode,
                                    commandContext -> ((CommandSourceStack)commandContext.getSource())
                                       .withAnchor(EntityAnchorArgument.getAnchor(commandContext, "anchor"))
                                 )
                           )
                     ))
                  .then(
                     Commands.literal("in")
                        .then(
                           Commands.argument("dimension", DimensionArgument.dimension())
                              .redirect(
                                 literalCommandNode,
                                 commandContext -> ((CommandSourceStack)commandContext.getSource())
                                    .withLevel(DimensionArgument.getDimension(commandContext, "dimension"))
                              )
                        )
                  ))
               .then(
                  Commands.literal("summon")
                     .then(
                        Commands.argument("entity", ResourceArgument.resource(buildContext, Registries.ENTITY_TYPE))
                           .suggests(SuggestionProviders.cast(SuggestionProviders.SUMMONABLE_ENTITIES))
                           .redirect(
                              literalCommandNode,
                              commandContext -> spawnEntityAndRedirect(
                                 (CommandSourceStack)commandContext.getSource(), ResourceArgument.getSummonableEntityType(commandContext, "entity")
                              )
                           )
                     )
               ))
            .then(createRelationOperations(literalCommandNode, Commands.literal("on")))
      );
   }

   private static ArgumentBuilder<CommandSourceStack, ?> wrapStores(
      LiteralCommandNode<CommandSourceStack> parent, LiteralArgumentBuilder<CommandSourceStack> literal, boolean storingResult
   ) {
      literal.then(
         Commands.literal("score")
            .then(
               Commands.argument("targets", ScoreHolderArgument.scoreHolders())
                  .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                  .then(
                     Commands.argument("objective", ObjectiveArgument.objective())
                        .redirect(
                           parent,
                           commandContext -> storeValue(
                              (CommandSourceStack)commandContext.getSource(),
                              ScoreHolderArgument.getNamesWithDefaultWildcard(commandContext, "targets"),
                              ObjectiveArgument.getObjective(commandContext, "objective"),
                              storingResult
                           )
                        )
                  )
            )
      );
      literal.then(
         Commands.literal("bossbar")
            .then(
               ((RequiredArgumentBuilder)Commands.argument("id", IdentifierArgument.id())
                     .suggests(BossBarCommands.SUGGEST_BOSS_BAR)
                     .then(
                        Commands.literal("value")
                           .redirect(
                              parent,
                              commandContext -> storeValue(
                                 (CommandSourceStack)commandContext.getSource(), BossBarCommands.getBossBar(commandContext), true, storingResult
                              )
                           )
                     ))
                  .then(
                     Commands.literal("max")
                        .redirect(
                           parent,
                           commandContext -> storeValue(
                              (CommandSourceStack)commandContext.getSource(), BossBarCommands.getBossBar(commandContext), false, storingResult
                           )
                        )
                  )
            )
      );

      for (DataCommands.DataProvider dataProvider : DataCommands.TARGET_PROVIDERS) {
         dataProvider.wrap(
            literal,
            argumentBuilder -> argumentBuilder.then(
               ((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument(
                                    "path", NbtPathArgument.nbtPath()
                                 )
                                 .then(
                                    Commands.literal("int")
                                       .then(
                                          Commands.argument("scale", DoubleArgumentType.doubleArg())
                                             .redirect(
                                                parent,
                                                commandContext -> storeData(
                                                   (CommandSourceStack)commandContext.getSource(),
                                                   dataProvider.access(commandContext),
                                                   NbtPathArgument.getPath(commandContext, "path"),
                                                   i -> IntTag.valueOf((int)(i * DoubleArgumentType.getDouble(commandContext, "scale"))),
                                                   storingResult
                                                )
                                             )
                                       )
                                 ))
                              .then(
                                 Commands.literal("float")
                                    .then(
                                       Commands.argument("scale", DoubleArgumentType.doubleArg())
                                          .redirect(
                                             parent,
                                             commandContext -> storeData(
                                                (CommandSourceStack)commandContext.getSource(),
                                                dataProvider.access(commandContext),
                                                NbtPathArgument.getPath(commandContext, "path"),
                                                i -> FloatTag.valueOf((float)(i * DoubleArgumentType.getDouble(commandContext, "scale"))),
                                                storingResult
                                             )
                                          )
                                    )
                              ))
                           .then(
                              Commands.literal("short")
                                 .then(
                                    Commands.argument("scale", DoubleArgumentType.doubleArg())
                                       .redirect(
                                          parent,
                                          commandContext -> storeData(
                                             (CommandSourceStack)commandContext.getSource(),
                                             dataProvider.access(commandContext),
                                             NbtPathArgument.getPath(commandContext, "path"),
                                             i -> ShortTag.valueOf((short)(i * DoubleArgumentType.getDouble(commandContext, "scale"))),
                                             storingResult
                                          )
                                       )
                                 )
                           ))
                        .then(
                           Commands.literal("long")
                              .then(
                                 Commands.argument("scale", DoubleArgumentType.doubleArg())
                                    .redirect(
                                       parent,
                                       commandContext -> storeData(
                                          (CommandSourceStack)commandContext.getSource(),
                                          dataProvider.access(commandContext),
                                          NbtPathArgument.getPath(commandContext, "path"),
                                          i -> LongTag.valueOf((long)(i * DoubleArgumentType.getDouble(commandContext, "scale"))),
                                          storingResult
                                       )
                                    )
                              )
                        ))
                     .then(
                        Commands.literal("double")
                           .then(
                              Commands.argument("scale", DoubleArgumentType.doubleArg())
                                 .redirect(
                                    parent,
                                    commandContext -> storeData(
                                       (CommandSourceStack)commandContext.getSource(),
                                       dataProvider.access(commandContext),
                                       NbtPathArgument.getPath(commandContext, "path"),
                                       i -> DoubleTag.valueOf(i * DoubleArgumentType.getDouble(commandContext, "scale")),
                                       storingResult
                                    )
                                 )
                           )
                     ))
                  .then(
                     Commands.literal("byte")
                        .then(
                           Commands.argument("scale", DoubleArgumentType.doubleArg())
                              .redirect(
                                 parent,
                                 commandContext -> storeData(
                                    (CommandSourceStack)commandContext.getSource(),
                                    dataProvider.access(commandContext),
                                    NbtPathArgument.getPath(commandContext, "path"),
                                    i -> ByteTag.valueOf((byte)(i * DoubleArgumentType.getDouble(commandContext, "scale"))),
                                    storingResult
                                 )
                              )
                        )
                  )
            )
         );
      }

      return literal;
   }

   private static CommandSourceStack storeValue(CommandSourceStack source, Collection<ScoreHolder> targets, Objective objective, boolean storingResult) {
      Scoreboard scoreboard = source.getServer().getScoreboard();
      return source.withCallback((bl2, i) -> {
         for (ScoreHolder scoreHolder : targets) {
            ScoreAccess scoreAccess = scoreboard.getOrCreatePlayerScore(scoreHolder, objective);
            int j = storingResult ? i : (bl2 ? 1 : 0);
            scoreAccess.set(j);
         }
      }, CommandResultCallback::chain);
   }

   private static CommandSourceStack storeValue(CommandSourceStack source, CustomBossEvent bar, boolean storingValue, boolean storingResult) {
      return source.withCallback((bl3, i) -> {
         int j = storingResult ? i : (bl3 ? 1 : 0);
         if (storingValue) {
            bar.setValue(j);
         } else {
            bar.setMax(j);
         }
      }, CommandResultCallback::chain);
   }

   private static CommandSourceStack storeData(
      CommandSourceStack source, DataAccessor accessor, NbtPathArgument.NbtPath path, IntFunction<Tag> tagConverter, boolean storingResult
   ) {
      return source.withCallback((bl2, i) -> {
         try {
            CompoundTag compoundTag = accessor.getData();
            int j = storingResult ? i : (bl2 ? 1 : 0);
            path.set(compoundTag, tagConverter.apply(j));
            accessor.setData(compoundTag);
         } catch (CommandSyntaxException var8) {
         }
      }, CommandResultCallback::chain);
   }

   private static boolean isChunkLoaded(ServerLevel level, BlockPos pos) {
      ChunkPos chunkPos = new ChunkPos(pos);
      LevelChunk levelChunk = level.getChunkSource().getChunkNow(chunkPos.x, chunkPos.z);
      return levelChunk == null ? false : levelChunk.getFullStatus() == FullChunkStatus.ENTITY_TICKING && level.areEntitiesLoaded(chunkPos.toLong());
   }

   private static ArgumentBuilder<CommandSourceStack, ?> addConditionals(
      CommandNode<CommandSourceStack> parent, LiteralArgumentBuilder<CommandSourceStack> literal, boolean isIf, CommandBuildContext buildContext
   ) {
      ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)literal.then(
                                       Commands.literal("block")
                                          .then(
                                             Commands.argument("pos", BlockPosArgument.blockPos())
                                                .then(
                                                   addConditional(
                                                      parent,
                                                      Commands.argument("block", BlockPredicateArgument.blockPredicate(buildContext)),
                                                      isIf,
                                                      commandContext -> BlockPredicateArgument.getBlockPredicate(commandContext, "block")
                                                         .test(
                                                            new BlockInWorld(
                                                               ((CommandSourceStack)commandContext.getSource()).getLevel(),
                                                               BlockPosArgument.getLoadedBlockPos(commandContext, "pos"),
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
                                                      parent,
                                                      Commands.argument("biome", ResourceOrTagArgument.resourceOrTag(buildContext, Registries.BIOME)),
                                                      isIf,
                                                      commandContext -> ResourceOrTagArgument.getResourceOrTag(commandContext, "biome", Registries.BIOME)
                                                         .test(
                                                            ((CommandSourceStack)commandContext.getSource())
                                                               .getLevel()
                                                               .getBiome(BlockPosArgument.getLoadedBlockPos(commandContext, "pos"))
                                                         )
                                                   )
                                                )
                                          )
                                    ))
                                 .then(
                                    Commands.literal("loaded")
                                       .then(
                                          addConditional(
                                             parent,
                                             Commands.argument("pos", BlockPosArgument.blockPos()),
                                             isIf,
                                             commandContext -> isChunkLoaded(
                                                ((CommandSourceStack)commandContext.getSource()).getLevel(),
                                                BlockPosArgument.getBlockPos(commandContext, "pos")
                                             )
                                          )
                                       )
                                 ))
                              .then(
                                 Commands.literal("dimension")
                                    .then(
                                       addConditional(
                                          parent,
                                          Commands.argument("dimension", DimensionArgument.dimension()),
                                          isIf,
                                          commandContext -> DimensionArgument.getDimension(commandContext, "dimension")
                                             == ((CommandSourceStack)commandContext.getSource()).getLevel()
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
                                                                              parent,
                                                                              Commands.argument("sourceObjective", ObjectiveArgument.objective()),
                                                                              isIf,
                                                                              commandContext -> checkScore(commandContext, (i, j) -> i == j)
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
                                                                           parent,
                                                                           Commands.argument("sourceObjective", ObjectiveArgument.objective()),
                                                                           isIf,
                                                                           commandContext -> checkScore(commandContext, (i, j) -> i < j)
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
                                                                        parent,
                                                                        Commands.argument("sourceObjective", ObjectiveArgument.objective()),
                                                                        isIf,
                                                                        commandContext -> checkScore(commandContext, (i, j) -> i <= j)
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
                                                                     parent,
                                                                     Commands.argument("sourceObjective", ObjectiveArgument.objective()),
                                                                     isIf,
                                                                     commandContext -> checkScore(commandContext, (i, j) -> i > j)
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
                                                                  parent,
                                                                  Commands.argument("sourceObjective", ObjectiveArgument.objective()),
                                                                  isIf,
                                                                  commandContext -> checkScore(commandContext, (i, j) -> i >= j)
                                                               )
                                                            )
                                                      )
                                                ))
                                             .then(
                                                Commands.literal("matches")
                                                   .then(
                                                      addConditional(
                                                         parent,
                                                         Commands.argument("range", RangeArgument.intRange()),
                                                         isIf,
                                                         commandContext -> checkScore(commandContext, RangeArgument.Ints.getRange(commandContext, "range"))
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
                                                   .then(addIfBlocksConditional(parent, Commands.literal("all"), isIf, false)))
                                                .then(addIfBlocksConditional(parent, Commands.literal("masked"), isIf, true))
                                          )
                                    )
                              )
                        ))
                     .then(
                        Commands.literal("entity")
                           .then(
                              ((RequiredArgumentBuilder)Commands.argument("entities", EntityArgument.entities())
                                    .fork(
                                       parent,
                                       commandContext -> expect(commandContext, isIf, !EntityArgument.getOptionalEntities(commandContext, "entities").isEmpty())
                                    ))
                                 .executes(
                                    createNumericConditionalHandler(
                                       isIf, commandContext -> EntityArgument.getOptionalEntities(commandContext, "entities").size()
                                    )
                                 )
                           )
                     ))
                  .then(
                     Commands.literal("predicate")
                        .then(
                           addConditional(
                              parent,
                              Commands.argument("predicate", ResourceOrIdArgument.lootPredicate(buildContext)),
                              isIf,
                              commandContext -> checkCustomPredicate(
                                 (CommandSourceStack)commandContext.getSource(), ResourceOrIdArgument.getLootPredicate(commandContext, "predicate")
                              )
                           )
                        )
                  ))
               .then(
                  Commands.literal("function")
                     .then(
                        Commands.argument("name", FunctionArgument.functions())
                           .suggests(FunctionCommand.SUGGEST_FUNCTION)
                           .fork(parent, new ExecuteCommand.ExecuteIfFunctionCustomModifier(isIf))
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
                                          ((RequiredArgumentBuilder)Commands.argument("item_predicate", ItemPredicateArgument.itemPredicate(buildContext))
                                                .fork(
                                                   parent,
                                                   commandContext -> expect(
                                                      commandContext,
                                                      isIf,
                                                      countItems(
                                                            EntityArgument.getEntities(commandContext, "entities"),
                                                            SlotsArgument.getSlots(commandContext, "slots"),
                                                            ItemPredicateArgument.getItemPredicate(commandContext, "item_predicate")
                                                         )
                                                         > 0
                                                   )
                                                ))
                                             .executes(
                                                createNumericConditionalHandler(
                                                   isIf,
                                                   commandContext -> countItems(
                                                      EntityArgument.getEntities(commandContext, "entities"),
                                                      SlotsArgument.getSlots(commandContext, "slots"),
                                                      ItemPredicateArgument.getItemPredicate(commandContext, "item_predicate")
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
                                       ((RequiredArgumentBuilder)Commands.argument("item_predicate", ItemPredicateArgument.itemPredicate(buildContext))
                                             .fork(
                                                parent,
                                                commandContext -> expect(
                                                   commandContext,
                                                   isIf,
                                                   countItems(
                                                         (CommandSourceStack)commandContext.getSource(),
                                                         BlockPosArgument.getLoadedBlockPos(commandContext, "pos"),
                                                         SlotsArgument.getSlots(commandContext, "slots"),
                                                         ItemPredicateArgument.getItemPredicate(commandContext, "item_predicate")
                                                      )
                                                      > 0
                                                )
                                             ))
                                          .executes(
                                             createNumericConditionalHandler(
                                                isIf,
                                                commandContext -> countItems(
                                                   (CommandSourceStack)commandContext.getSource(),
                                                   BlockPosArgument.getLoadedBlockPos(commandContext, "pos"),
                                                   SlotsArgument.getSlots(commandContext, "slots"),
                                                   ItemPredicateArgument.getItemPredicate(commandContext, "item_predicate")
                                                )
                                             )
                                          )
                                    )
                              )
                        )
                  )
            ))
         .then(
            Commands.literal("stopwatch")
               .then(
                  Commands.argument("id", IdentifierArgument.id())
                     .suggests(StopwatchCommand.SUGGEST_STOPWATCHES)
                     .then(
                        addConditional(
                           parent,
                           Commands.argument("range", RangeArgument.floatRange()),
                           isIf,
                           commandContext -> checkStopwatch(commandContext, RangeArgument.Floats.getRange(commandContext, "range"))
                        )
                     )
               )
         );

      for (DataCommands.DataProvider dataProvider : DataCommands.SOURCE_PROVIDERS) {
         literal.then(
            dataProvider.wrap(
               Commands.literal("data"),
               argumentBuilder -> argumentBuilder.then(
                  ((RequiredArgumentBuilder)Commands.argument("path", NbtPathArgument.nbtPath())
                        .fork(
                           parent,
                           commandContext -> expect(
                              commandContext, isIf, checkMatchingData(dataProvider.access(commandContext), NbtPathArgument.getPath(commandContext, "path")) > 0
                           )
                        ))
                     .executes(
                        createNumericConditionalHandler(
                           isIf, commandContext -> checkMatchingData(dataProvider.access(commandContext), NbtPathArgument.getPath(commandContext, "path"))
                        )
                     )
               )
            )
         );
      }

      return literal;
   }

   private static int countItems(Iterable<? extends SlotProvider> targets, SlotRange slotRange, Predicate<ItemStack> filter) {
      int i = 0;

      for (SlotProvider slotProvider : targets) {
         IntList intList = slotRange.slots();

         for (int j = 0; j < intList.size(); j++) {
            int k = intList.getInt(j);
            SlotAccess slotAccess = slotProvider.getSlot(k);
            if (slotAccess != null) {
               ItemStack itemStack = slotAccess.get();
               if (filter.test(itemStack)) {
                  i += itemStack.getCount();
               }
            }
         }
      }

      return i;
   }

   private static int countItems(CommandSourceStack source, BlockPos pos, SlotRange slotRange, Predicate<ItemStack> filter) throws CommandSyntaxException {
      int i = 0;
      Container container = ItemCommands.getContainer(source, pos, ItemCommands.ERROR_SOURCE_NOT_A_CONTAINER);
      int j = container.getContainerSize();
      IntList intList = slotRange.slots();

      for (int k = 0; k < intList.size(); k++) {
         int l = intList.getInt(k);
         if (l >= 0 && l < j) {
            ItemStack itemStack = container.getItem(l);
            if (filter.test(itemStack)) {
               i += itemStack.getCount();
            }
         }
      }

      return i;
   }

   private static Command<CommandSourceStack> createNumericConditionalHandler(boolean isIf, ExecuteCommand.CommandNumericPredicate predicate) {
      return isIf ? commandContext -> {
         int i = predicate.test(commandContext);
         if (i > 0) {
            ((CommandSourceStack)commandContext.getSource()).sendSuccess(() -> Component.translatable("commands.execute.conditional.pass_count", i), false);
            return i;
         } else {
            throw ERROR_CONDITIONAL_FAILED.create();
         }
      } : commandContext -> {
         int i = predicate.test(commandContext);
         if (i == 0) {
            ((CommandSourceStack)commandContext.getSource()).sendSuccess(() -> Component.translatable("commands.execute.conditional.pass"), false);
            return 1;
         } else {
            throw ERROR_CONDITIONAL_FAILED_COUNT.create(i);
         }
      };
   }

   private static int checkMatchingData(DataAccessor accessor, NbtPathArgument.NbtPath path) throws CommandSyntaxException {
      return path.countMatching(accessor.getData());
   }

   private static boolean checkScore(CommandContext<CommandSourceStack> context, ExecuteCommand.IntBiPredicate predicate) throws CommandSyntaxException {
      ScoreHolder scoreHolder = ScoreHolderArgument.getName(context, "target");
      Objective objective = ObjectiveArgument.getObjective(context, "targetObjective");
      ScoreHolder scoreHolder2 = ScoreHolderArgument.getName(context, "source");
      Objective objective2 = ObjectiveArgument.getObjective(context, "sourceObjective");
      Scoreboard scoreboard = ((CommandSourceStack)context.getSource()).getServer().getScoreboard();
      ReadOnlyScoreInfo readOnlyScoreInfo = scoreboard.getPlayerScoreInfo(scoreHolder, objective);
      ReadOnlyScoreInfo readOnlyScoreInfo2 = scoreboard.getPlayerScoreInfo(scoreHolder2, objective2);
      return readOnlyScoreInfo != null && readOnlyScoreInfo2 != null ? predicate.test(readOnlyScoreInfo.value(), readOnlyScoreInfo2.value()) : false;
   }

   private static boolean checkScore(CommandContext<CommandSourceStack> context, MinMaxBounds.Ints range) throws CommandSyntaxException {
      ScoreHolder scoreHolder = ScoreHolderArgument.getName(context, "target");
      Objective objective = ObjectiveArgument.getObjective(context, "targetObjective");
      Scoreboard scoreboard = ((CommandSourceStack)context.getSource()).getServer().getScoreboard();
      ReadOnlyScoreInfo readOnlyScoreInfo = scoreboard.getPlayerScoreInfo(scoreHolder, objective);
      return readOnlyScoreInfo == null ? false : range.matches(readOnlyScoreInfo.value());
   }

   private static boolean checkStopwatch(CommandContext<CommandSourceStack> context, MinMaxBounds.Doubles range) throws CommandSyntaxException {
      Identifier identifier = IdentifierArgument.getId(context, "id");
      Stopwatches stopwatches = ((CommandSourceStack)context.getSource()).getServer().getStopwatches();
      Stopwatch stopwatch = stopwatches.get(identifier);
      if (stopwatch == null) {
         throw StopwatchCommand.ERROR_DOES_NOT_EXIST.create(identifier);
      } else {
         long l = Stopwatches.currentTime();
         double d = stopwatch.elapsedSeconds(l);
         return range.matches(d);
      }
   }

   private static boolean checkCustomPredicate(CommandSourceStack source, Holder<LootItemCondition> condition) {
      ServerLevel serverLevel = source.getLevel();
      LootParams lootParams = new LootParams.Builder(serverLevel)
         .withParameter(LootContextParams.ORIGIN, source.getPosition())
         .withOptionalParameter(LootContextParams.THIS_ENTITY, source.getEntity())
         .create(LootContextParamSets.COMMAND);
      LootContext lootContext = new LootContext.Builder(lootParams).create(Optional.empty());
      lootContext.pushVisitedElement(LootContext.createVisitedEntry(condition.value()));
      return condition.value().test(lootContext);
   }

   private static Collection<CommandSourceStack> expect(CommandContext<CommandSourceStack> context, boolean actual, boolean expected) {
      return (Collection<CommandSourceStack>)(expected == actual ? Collections.singleton((CommandSourceStack)context.getSource()) : Collections.emptyList());
   }

   private static ArgumentBuilder<CommandSourceStack, ?> addConditional(
      CommandNode<CommandSourceStack> commandNode, ArgumentBuilder<CommandSourceStack, ?> builder, boolean value, ExecuteCommand.CommandPredicate test
   ) {
      return builder.fork(commandNode, commandContext -> expect(commandContext, value, test.test(commandContext))).executes(commandContext -> {
         if (value == test.test(commandContext)) {
            ((CommandSourceStack)commandContext.getSource()).sendSuccess(() -> Component.translatable("commands.execute.conditional.pass"), false);
            return 1;
         } else {
            throw ERROR_CONDITIONAL_FAILED.create();
         }
      });
   }

   private static ArgumentBuilder<CommandSourceStack, ?> addIfBlocksConditional(
      CommandNode<CommandSourceStack> commandNode, ArgumentBuilder<CommandSourceStack, ?> literal, boolean isIf, boolean isMasked
   ) {
      return literal.fork(commandNode, commandContext -> expect(commandContext, isIf, checkRegions(commandContext, isMasked).isPresent()))
         .executes(isIf ? commandContext -> checkIfRegions(commandContext, isMasked) : commandContext -> checkUnlessRegions(commandContext, isMasked));
   }

   private static int checkIfRegions(CommandContext<CommandSourceStack> context, boolean isMasked) throws CommandSyntaxException {
      OptionalInt optionalInt = checkRegions(context, isMasked);
      if (optionalInt.isPresent()) {
         ((CommandSourceStack)context.getSource())
            .sendSuccess(() -> Component.translatable("commands.execute.conditional.pass_count", optionalInt.getAsInt()), false);
         return optionalInt.getAsInt();
      } else {
         throw ERROR_CONDITIONAL_FAILED.create();
      }
   }

   private static int checkUnlessRegions(CommandContext<CommandSourceStack> context, boolean isMasked) throws CommandSyntaxException {
      OptionalInt optionalInt = checkRegions(context, isMasked);
      if (optionalInt.isPresent()) {
         throw ERROR_CONDITIONAL_FAILED_COUNT.create(optionalInt.getAsInt());
      } else {
         ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.translatable("commands.execute.conditional.pass"), false);
         return 1;
      }
   }

   private static OptionalInt checkRegions(CommandContext<CommandSourceStack> context, boolean isMasked) throws CommandSyntaxException {
      return checkRegions(
         ((CommandSourceStack)context.getSource()).getLevel(),
         BlockPosArgument.getLoadedBlockPos(context, "start"),
         BlockPosArgument.getLoadedBlockPos(context, "end"),
         BlockPosArgument.getLoadedBlockPos(context, "destination"),
         isMasked
      );
   }

   private static OptionalInt checkRegions(ServerLevel level, BlockPos begin, BlockPos end, BlockPos destination, boolean isMasked) throws CommandSyntaxException {
      BoundingBox boundingBox = BoundingBox.fromCorners(begin, end);
      BoundingBox boundingBox2 = BoundingBox.fromCorners(destination, destination.offset(boundingBox.getLength()));
      BlockPos blockPos = new BlockPos(
         boundingBox2.minX() - boundingBox.minX(), boundingBox2.minY() - boundingBox.minY(), boundingBox2.minZ() - boundingBox.minZ()
      );
      int i = boundingBox.getXSpan() * boundingBox.getYSpan() * boundingBox.getZSpan();
      if (i > 32768) {
         throw ERROR_AREA_TOO_LARGE.create(32768, i);
      } else {
         int j = 0;
         RegistryAccess registryAccess = level.registryAccess();

         try (ProblemReporter.ScopedCollector scopedCollector = new ProblemReporter.ScopedCollector(LOGGER)) {
            for (int k = boundingBox.minZ(); k <= boundingBox.maxZ(); k++) {
               for (int l = boundingBox.minY(); l <= boundingBox.maxY(); l++) {
                  for (int m = boundingBox.minX(); m <= boundingBox.maxX(); m++) {
                     BlockPos blockPos2 = new BlockPos(m, l, k);
                     BlockPos blockPos3 = blockPos2.offset(blockPos);
                     BlockState blockState = level.getBlockState(blockPos2);
                     if (!isMasked || !blockState.is(Blocks.AIR)) {
                        if (blockState != level.getBlockState(blockPos3)) {
                           return OptionalInt.empty();
                        }

                        BlockEntity blockEntity = level.getBlockEntity(blockPos2);
                        BlockEntity blockEntity2 = level.getBlockEntity(blockPos3);
                        if (blockEntity != null) {
                           if (blockEntity2 == null) {
                              return OptionalInt.empty();
                           }

                           if (blockEntity2.getType() != blockEntity.getType()) {
                              return OptionalInt.empty();
                           }

                           if (!blockEntity.components().equals(blockEntity2.components())) {
                              return OptionalInt.empty();
                           }

                           TagValueOutput tagValueOutput = TagValueOutput.createWithContext(scopedCollector.forChild(blockEntity.problemPath()), registryAccess);
                           blockEntity.saveCustomOnly(tagValueOutput);
                           CompoundTag compoundTag = tagValueOutput.buildResult();
                           TagValueOutput tagValueOutput2 = TagValueOutput.createWithContext(
                              scopedCollector.forChild(blockEntity2.problemPath()), registryAccess
                           );
                           blockEntity2.saveCustomOnly(tagValueOutput2);
                           CompoundTag compoundTag2 = tagValueOutput2.buildResult();
                           if (!compoundTag.equals(compoundTag2)) {
                              return OptionalInt.empty();
                           }
                        }

                        j++;
                     }
                  }
               }
            }
         }

         return OptionalInt.of(j);
      }
   }

   private static RedirectModifier<CommandSourceStack> expandOneToOneEntityRelation(Function<Entity, Optional<Entity>> relation) {
      return commandContext -> {
         CommandSourceStack commandSourceStack = (CommandSourceStack)commandContext.getSource();
         Entity entity = commandSourceStack.getEntity();
         return (Collection)(entity == null
            ? List.of()
            : relation.apply(entity).filter(entityx -> !entityx.isRemoved()).map(entityx -> List.of(commandSourceStack.withEntity(entityx))).orElse(List.of()));
      };
   }

   private static RedirectModifier<CommandSourceStack> expandOneToManyEntityRelation(Function<Entity, Stream<Entity>> relation) {
      return commandContext -> {
         CommandSourceStack commandSourceStack = (CommandSourceStack)commandContext.getSource();
         Entity entity = commandSourceStack.getEntity();
         return entity == null ? List.of() : relation.apply(entity).filter(entityx -> !entityx.isRemoved()).map(commandSourceStack::withEntity).toList();
      };
   }

   private static LiteralArgumentBuilder<CommandSourceStack> createRelationOperations(
      CommandNode<CommandSourceStack> node, LiteralArgumentBuilder<CommandSourceStack> argumentBuilder
   ) {
      return (LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)argumentBuilder.then(
                              Commands.literal("owner")
                                 .fork(
                                    node,
                                    expandOneToOneEntityRelation(
                                       entity -> entity instanceof OwnableEntity ownableEntity
                                          ? Optional.ofNullable(ownableEntity.getOwner())
                                          : Optional.empty()
                                    )
                                 )
                           ))
                           .then(
                              Commands.literal("leasher")
                                 .fork(
                                    node,
                                    expandOneToOneEntityRelation(
                                       entity -> entity instanceof Leashable leashable ? Optional.ofNullable(leashable.getLeashHolder()) : Optional.empty()
                                    )
                                 )
                           ))
                        .then(
                           Commands.literal("target")
                              .fork(
                                 node,
                                 expandOneToOneEntityRelation(
                                    entity -> entity instanceof Targeting targeting ? Optional.ofNullable(targeting.getTarget()) : Optional.empty()
                                 )
                              )
                        ))
                     .then(
                        Commands.literal("attacker")
                           .fork(
                              node,
                              expandOneToOneEntityRelation(
                                 entity -> entity instanceof Attackable attackable ? Optional.ofNullable(attackable.getLastAttacker()) : Optional.empty()
                              )
                           )
                     ))
                  .then(Commands.literal("vehicle").fork(node, expandOneToOneEntityRelation(entity -> Optional.ofNullable(entity.getVehicle())))))
               .then(Commands.literal("controller").fork(node, expandOneToOneEntityRelation(entity -> Optional.ofNullable(entity.getControllingPassenger())))))
            .then(
               Commands.literal("origin")
                  .fork(
                     node,
                     expandOneToOneEntityRelation(
                        entity -> entity instanceof TraceableEntity traceableEntity ? Optional.ofNullable(traceableEntity.getOwner()) : Optional.empty()
                     )
                  )
            ))
         .then(Commands.literal("passengers").fork(node, expandOneToManyEntityRelation(entity -> entity.getPassengers().stream())));
   }

   private static CommandSourceStack spawnEntityAndRedirect(CommandSourceStack source, Holder.Reference<EntityType<?>> entityType) throws CommandSyntaxException {
      Entity entity = SummonCommand.createEntity(source, entityType, source.getPosition(), new CompoundTag(), true);
      return source.withEntity(entity);
   }

   public static <T extends ExecutionCommandSource<T>> void scheduleFunctionConditionsAndTest(
      T originalSource,
      List<T> sources,
      Function<T, T> sourceModifier,
      IntPredicate successCheck,
      ContextChain<T> contextChain,
      @Nullable CompoundTag arguments,
      ExecutionControl<T> executionControl,
      InCommandFunction<CommandContext<T>, Collection<CommandFunction<T>>> functionsGetter,
      ChainModifiers chainModifiers
   ) {
      List<T> list = new ArrayList<>(sources.size());

      Collection<CommandFunction<T>> collection;
      try {
         collection = functionsGetter.apply(contextChain.getTopContext().copyFor(originalSource));
      } catch (CommandSyntaxException var18) {
         originalSource.handleError(var18, chainModifiers.isForked(), executionControl.tracer());
         return;
      }

      int i = collection.size();
      if (i != 0) {
         List<InstantiatedFunction<T>> list2 = new ArrayList<>(i);

         try {
            for (CommandFunction<T> commandFunction : collection) {
               try {
                  list2.add(commandFunction.instantiate(arguments, originalSource.dispatcher()));
               } catch (FunctionInstantiationException var17) {
                  throw ERROR_FUNCTION_CONDITION_INSTANTATION_FAILURE.create(commandFunction.id(), var17.messageComponent());
               }
            }
         } catch (CommandSyntaxException var19) {
            originalSource.handleError(var19, chainModifiers.isForked(), executionControl.tracer());
         }

         for (T executionCommandSource : sources) {
            T executionCommandSource2 = (T)sourceModifier.apply(executionCommandSource.clearCallbacks());
            CommandResultCallback commandResultCallback = (bl, ix) -> {
               if (successCheck.test(ix)) {
                  list.add(executionCommandSource);
               }
            };
            executionControl.queueNext(
               new IsolatedCall<>(
                  executionControlx -> {
                     for (InstantiatedFunction<T> instantiatedFunction : list2) {
                        executionControlx.queueNext(
                           new CallFunction<>(instantiatedFunction, executionControlx.currentFrame().returnValueConsumer(), true).bind(executionCommandSource2)
                        );
                     }

                     executionControlx.queueNext(FallthroughTask.instance());
                  },
                  commandResultCallback
               )
            );
         }

         ContextChain<T> contextChain2 = contextChain.nextStage();
         String string = contextChain.getTopContext().getInput();
         executionControl.queueNext(new BuildContexts.Continuation<>(string, contextChain2, chainModifiers, originalSource, list));
      }
   }

   @FunctionalInterface
   interface CommandNumericPredicate {
      int test(CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException;
   }

   @FunctionalInterface
   interface CommandPredicate {
      boolean test(CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException;
   }

   static class ExecuteIfFunctionCustomModifier implements CustomModifierExecutor.ModifierAdapter<CommandSourceStack> {
      private final IntPredicate check;

      ExecuteIfFunctionCustomModifier(boolean invert) {
         this.check = invert ? i -> i != 0 : i -> i == 0;
      }

      public void apply(
         CommandSourceStack commandSourceStack,
         List<CommandSourceStack> list,
         ContextChain<CommandSourceStack> contextChain,
         ChainModifiers chainModifiers,
         ExecutionControl<CommandSourceStack> executionControl
      ) {
         ExecuteCommand.scheduleFunctionConditionsAndTest(
            commandSourceStack,
            list,
            FunctionCommand::modifySenderForExecution,
            this.check,
            contextChain,
            null,
            executionControl,
            commandContext -> FunctionArgument.getFunctions(commandContext, "name"),
            chainModifiers
         );
      }
   }

   @FunctionalInterface
   interface IntBiPredicate {
      boolean test(int i, int j);
   }
}
