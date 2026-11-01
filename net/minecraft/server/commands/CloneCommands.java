package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.logging.LogUtils;
import java.util.Deque;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class CloneCommands {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final SimpleCommandExceptionType ERROR_OVERLAP = new SimpleCommandExceptionType(Component.translatable("commands.clone.overlap"));
   private static final Dynamic2CommandExceptionType ERROR_AREA_TOO_LARGE = new Dynamic2CommandExceptionType(
      (object, object2) -> Component.translatableEscape("commands.clone.toobig", object, object2)
   );
   private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.clone.failed"));
   public static final Predicate<BlockInWorld> FILTER_AIR = blockInWorld -> !blockInWorld.getState().isAir();

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("clone")
                  .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
               .then(beginEndDestinationAndModeSuffix(context, commandContext -> ((CommandSourceStack)commandContext.getSource()).getLevel())))
            .then(
               Commands.literal("from")
                  .then(
                     Commands.argument("sourceDimension", DimensionArgument.dimension())
                        .then(beginEndDestinationAndModeSuffix(context, commandContext -> DimensionArgument.getDimension(commandContext, "sourceDimension")))
                  )
            )
      );
   }

   private static ArgumentBuilder<CommandSourceStack, ?> beginEndDestinationAndModeSuffix(
      CommandBuildContext buildContext, InCommandFunction<CommandContext<CommandSourceStack>, ServerLevel> levelGetter
   ) {
      return Commands.argument("begin", BlockPosArgument.blockPos())
         .then(
            ((RequiredArgumentBuilder)Commands.argument("end", BlockPosArgument.blockPos())
                  .then(destinationAndStrictSuffix(buildContext, levelGetter, commandContext -> ((CommandSourceStack)commandContext.getSource()).getLevel())))
               .then(
                  Commands.literal("to")
                     .then(
                        Commands.argument("targetDimension", DimensionArgument.dimension())
                           .then(
                              destinationAndStrictSuffix(
                                 buildContext, levelGetter, commandContext -> DimensionArgument.getDimension(commandContext, "targetDimension")
                              )
                           )
                     )
               )
         );
   }

   private static CloneCommands.DimensionAndPosition getLoadedDimensionAndPosition(CommandContext<CommandSourceStack> context, ServerLevel level, String name) throws CommandSyntaxException {
      BlockPos blockPos = BlockPosArgument.getLoadedBlockPos(context, level, name);
      return new CloneCommands.DimensionAndPosition(level, blockPos);
   }

   private static ArgumentBuilder<CommandSourceStack, ?> destinationAndStrictSuffix(
      CommandBuildContext buildContext,
      InCommandFunction<CommandContext<CommandSourceStack>, ServerLevel> sourceLevelGetter,
      InCommandFunction<CommandContext<CommandSourceStack>, ServerLevel> destinationLevelGetter
   ) {
      InCommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> inCommandFunction = commandContext -> getLoadedDimensionAndPosition(
         commandContext, sourceLevelGetter.apply(commandContext), "begin"
      );
      InCommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> inCommandFunction2 = commandContext -> getLoadedDimensionAndPosition(
         commandContext, sourceLevelGetter.apply(commandContext), "end"
      );
      InCommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> inCommandFunction3 = commandContext -> getLoadedDimensionAndPosition(
         commandContext, destinationLevelGetter.apply(commandContext), "destination"
      );
      return modeSuffix(
            buildContext, inCommandFunction, inCommandFunction2, inCommandFunction3, false, Commands.argument("destination", BlockPosArgument.blockPos())
         )
         .then(modeSuffix(buildContext, inCommandFunction, inCommandFunction2, inCommandFunction3, true, Commands.literal("strict")));
   }

   private static ArgumentBuilder<CommandSourceStack, ?> modeSuffix(
      CommandBuildContext buildContext,
      InCommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> begin,
      InCommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> end,
      InCommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> destination,
      boolean strict,
      ArgumentBuilder<CommandSourceStack, ?> argumentBuilder
   ) {
      return argumentBuilder.executes(
            commandContext -> clone(
               (CommandSourceStack)commandContext.getSource(),
               begin.apply(commandContext),
               end.apply(commandContext),
               destination.apply(commandContext),
               blockInWorld -> true,
               CloneCommands.Mode.NORMAL,
               strict
            )
         )
         .then(wrapWithCloneMode(begin, end, destination, commandContext -> blockInWorld -> true, strict, Commands.literal("replace")))
         .then(wrapWithCloneMode(begin, end, destination, commandContext -> FILTER_AIR, strict, Commands.literal("masked")))
         .then(
            Commands.literal("filtered")
               .then(
                  wrapWithCloneMode(
                     begin,
                     end,
                     destination,
                     commandContext -> BlockPredicateArgument.getBlockPredicate(commandContext, "filter"),
                     strict,
                     Commands.argument("filter", BlockPredicateArgument.blockPredicate(buildContext))
                  )
               )
         );
   }

   private static ArgumentBuilder<CommandSourceStack, ?> wrapWithCloneMode(
      InCommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> begin,
      InCommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> end,
      InCommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> destination,
      InCommandFunction<CommandContext<CommandSourceStack>, Predicate<BlockInWorld>> filter,
      boolean strict,
      ArgumentBuilder<CommandSourceStack, ?> argumentBuilder
   ) {
      return argumentBuilder.executes(
            commandContext -> clone(
               (CommandSourceStack)commandContext.getSource(),
               begin.apply(commandContext),
               end.apply(commandContext),
               destination.apply(commandContext),
               filter.apply(commandContext),
               CloneCommands.Mode.NORMAL,
               strict
            )
         )
         .then(
            Commands.literal("force")
               .executes(
                  commandContext -> clone(
                     (CommandSourceStack)commandContext.getSource(),
                     begin.apply(commandContext),
                     end.apply(commandContext),
                     destination.apply(commandContext),
                     filter.apply(commandContext),
                     CloneCommands.Mode.FORCE,
                     strict
                  )
               )
         )
         .then(
            Commands.literal("move")
               .executes(
                  commandContext -> clone(
                     (CommandSourceStack)commandContext.getSource(),
                     begin.apply(commandContext),
                     end.apply(commandContext),
                     destination.apply(commandContext),
                     filter.apply(commandContext),
                     CloneCommands.Mode.MOVE,
                     strict
                  )
               )
         )
         .then(
            Commands.literal("normal")
               .executes(
                  commandContext -> clone(
                     (CommandSourceStack)commandContext.getSource(),
                     begin.apply(commandContext),
                     end.apply(commandContext),
                     destination.apply(commandContext),
                     filter.apply(commandContext),
                     CloneCommands.Mode.NORMAL,
                     strict
                  )
               )
         );
   }

   private static int clone(
      CommandSourceStack source,
      CloneCommands.DimensionAndPosition begin,
      CloneCommands.DimensionAndPosition end,
      CloneCommands.DimensionAndPosition destination,
      Predicate<BlockInWorld> filter,
      CloneCommands.Mode mode,
      boolean strict
   ) throws CommandSyntaxException {
      BlockPos blockPos = begin.position();
      BlockPos blockPos2 = end.position();
      BoundingBox boundingBox = BoundingBox.fromCorners(blockPos, blockPos2);
      BlockPos blockPos3 = destination.position();
      BlockPos blockPos4 = blockPos3.offset(boundingBox.getLength());
      BoundingBox boundingBox2 = BoundingBox.fromCorners(blockPos3, blockPos4);
      ServerLevel serverLevel = begin.dimension();
      ServerLevel serverLevel2 = destination.dimension();
      if (!mode.canOverlap() && serverLevel == serverLevel2 && boundingBox2.intersects(boundingBox)) {
         throw ERROR_OVERLAP.create();
      } else {
         int i = boundingBox.getXSpan() * boundingBox.getYSpan() * boundingBox.getZSpan();
         int j = source.getLevel().getGameRules().get(GameRules.MAX_BLOCK_MODIFICATIONS);
         if (i > j) {
            throw ERROR_AREA_TOO_LARGE.create(j, i);
         } else if (!serverLevel.hasChunksAt(blockPos, blockPos2) || !serverLevel2.hasChunksAt(blockPos3, blockPos4)) {
            throw BlockPosArgument.ERROR_NOT_LOADED.create();
         } else if (serverLevel2.isDebug()) {
            throw ERROR_FAILED.create();
         } else {
            List<CloneCommands.CloneBlockInfo> list = Lists.newArrayList();
            List<CloneCommands.CloneBlockInfo> list2 = Lists.newArrayList();
            List<CloneCommands.CloneBlockInfo> list3 = Lists.newArrayList();
            Deque<BlockPos> deque = Lists.newLinkedList();
            int k = 0;
            ProblemReporter.ScopedCollector scopedCollector = new ProblemReporter.ScopedCollector(LOGGER);

            try {
               BlockPos blockPos5 = new BlockPos(
                  boundingBox2.minX() - boundingBox.minX(), boundingBox2.minY() - boundingBox.minY(), boundingBox2.minZ() - boundingBox.minZ()
               );

               for (int l = boundingBox.minZ(); l <= boundingBox.maxZ(); l++) {
                  for (int m = boundingBox.minY(); m <= boundingBox.maxY(); m++) {
                     for (int n = boundingBox.minX(); n <= boundingBox.maxX(); n++) {
                        BlockPos blockPos6 = new BlockPos(n, m, l);
                        BlockPos blockPos7 = blockPos6.offset(blockPos5);
                        BlockInWorld blockInWorld = new BlockInWorld(serverLevel, blockPos6, false);
                        BlockState blockState = blockInWorld.getState();
                        if (filter.test(blockInWorld)) {
                           BlockEntity blockEntity = serverLevel.getBlockEntity(blockPos6);
                           if (blockEntity != null) {
                              TagValueOutput tagValueOutput = TagValueOutput.createWithContext(
                                 scopedCollector.forChild(blockEntity.problemPath()), source.registryAccess()
                              );
                              blockEntity.saveCustomOnly(tagValueOutput);
                              CloneCommands.CloneBlockEntityInfo cloneBlockEntityInfo = new CloneCommands.CloneBlockEntityInfo(
                                 tagValueOutput.buildResult(), blockEntity.components()
                              );
                              list2.add(new CloneCommands.CloneBlockInfo(blockPos7, blockState, cloneBlockEntityInfo, serverLevel2.getBlockState(blockPos7)));
                              deque.addLast(blockPos6);
                           } else if (!blockState.isSolidRender() && !blockState.isCollisionShapeFullBlock(serverLevel, blockPos6)) {
                              list3.add(new CloneCommands.CloneBlockInfo(blockPos7, blockState, null, serverLevel2.getBlockState(blockPos7)));
                              deque.addFirst(blockPos6);
                           } else {
                              list.add(new CloneCommands.CloneBlockInfo(blockPos7, blockState, null, serverLevel2.getBlockState(blockPos7)));
                              deque.addLast(blockPos6);
                           }
                        }
                     }
                  }
               }

               int l = 2 | (strict ? 816 : 0);
               if (mode == CloneCommands.Mode.MOVE) {
                  for (BlockPos blockPos8 : deque) {
                     serverLevel.setBlock(blockPos8, Blocks.BARRIER.defaultBlockState(), l | 816);
                  }

                  int m = strict ? l : 3;

                  for (BlockPos blockPos6 : deque) {
                     serverLevel.setBlock(blockPos6, Blocks.AIR.defaultBlockState(), m);
                  }
               }

               List<CloneCommands.CloneBlockInfo> list4 = Lists.newArrayList();
               list4.addAll(list);
               list4.addAll(list2);
               list4.addAll(list3);
               List<CloneCommands.CloneBlockInfo> list5 = Lists.reverse(list4);

               for (CloneCommands.CloneBlockInfo cloneBlockInfo : list5) {
                  serverLevel2.setBlock(cloneBlockInfo.pos, Blocks.BARRIER.defaultBlockState(), l | 816);
               }

               for (CloneCommands.CloneBlockInfo cloneBlockInfo : list4) {
                  if (serverLevel2.setBlock(cloneBlockInfo.pos, cloneBlockInfo.state, l)) {
                     k++;
                  }
               }

               for (CloneCommands.CloneBlockInfo cloneBlockInfox : list2) {
                  BlockEntity blockEntity2 = serverLevel2.getBlockEntity(cloneBlockInfox.pos);
                  if (cloneBlockInfox.blockEntityInfo != null && blockEntity2 != null) {
                     blockEntity2.loadCustomOnly(
                        TagValueInput.create(
                           scopedCollector.forChild(blockEntity2.problemPath()), serverLevel2.registryAccess(), cloneBlockInfox.blockEntityInfo.tag
                        )
                     );
                     blockEntity2.setComponents(cloneBlockInfox.blockEntityInfo.components);
                     blockEntity2.setChanged();
                  }

                  serverLevel2.setBlock(cloneBlockInfox.pos, cloneBlockInfox.state, l);
               }

               if (!strict) {
                  for (CloneCommands.CloneBlockInfo cloneBlockInfox : list5) {
                     serverLevel2.updateNeighboursOnBlockSet(cloneBlockInfox.pos, cloneBlockInfox.previousStateAtDestination);
                  }
               }

               serverLevel2.getBlockTicks().copyAreaFrom(serverLevel.getBlockTicks(), boundingBox, blockPos5);
            } catch (Throwable var35) {
               try {
                  scopedCollector.close();
               } catch (Throwable var34) {
                  var35.addSuppressed(var34);
               }

               throw var35;
            }

            scopedCollector.close();
            if (k == 0) {
               throw ERROR_FAILED.create();
            } else {
               int o = k;
               source.sendSuccess(() -> Component.translatable("commands.clone.success", o), true);
               return k;
            }
         }
      }
   }

   record CloneBlockEntityInfo(CompoundTag tag, DataComponentMap components) {
   }

   record CloneBlockInfo(BlockPos pos, BlockState state, @Nullable CloneCommands.CloneBlockEntityInfo blockEntityInfo, BlockState previousStateAtDestination) {
   }

   record DimensionAndPosition(ServerLevel dimension, BlockPos position) {
   }

   static enum Mode {
      FORCE(true),
      MOVE(true),
      NORMAL(false);

      private final boolean canOverlap;

      private Mode(final boolean canOverlap) {
         this.canOverlap = canOverlap;
      }

      public boolean canOverlap() {
         return this.canOverlap;
      }
   }
}
