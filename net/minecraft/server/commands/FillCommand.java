package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jspecify.annotations.Nullable;

public class FillCommand {
   private static final Dynamic2CommandExceptionType ERROR_AREA_TOO_LARGE = new Dynamic2CommandExceptionType(
      (object, object2) -> Component.translatableEscape("commands.fill.toobig", object, object2)
   );
   static final BlockInput HOLLOW_CORE = new BlockInput(Blocks.AIR.defaultBlockState(), Collections.emptySet(), null);
   private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.fill.failed"));

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("fill").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
            .then(
               Commands.argument("from", BlockPosArgument.blockPos())
                  .then(
                     Commands.argument("to", BlockPosArgument.blockPos())
                        .then(
                           wrapWithMode(
                                 buildContext,
                                 Commands.argument("block", BlockStateArgument.block(buildContext)),
                                 commandContext -> BlockPosArgument.getLoadedBlockPos(commandContext, "from"),
                                 commandContext -> BlockPosArgument.getLoadedBlockPos(commandContext, "to"),
                                 commandContext -> BlockStateArgument.getBlock(commandContext, "block"),
                                 commandContext -> null
                              )
                              .then(
                                 ((LiteralArgumentBuilder)Commands.literal("replace")
                                       .executes(
                                          commandContext -> fillBlocks(
                                             (CommandSourceStack)commandContext.getSource(),
                                             BoundingBox.fromCorners(
                                                BlockPosArgument.getLoadedBlockPos(commandContext, "from"),
                                                BlockPosArgument.getLoadedBlockPos(commandContext, "to")
                                             ),
                                             BlockStateArgument.getBlock(commandContext, "block"),
                                             FillCommand.Mode.REPLACE,
                                             null,
                                             false
                                          )
                                       ))
                                    .then(
                                       wrapWithMode(
                                          buildContext,
                                          Commands.argument("filter", BlockPredicateArgument.blockPredicate(buildContext)),
                                          commandContext -> BlockPosArgument.getLoadedBlockPos(commandContext, "from"),
                                          commandContext -> BlockPosArgument.getLoadedBlockPos(commandContext, "to"),
                                          commandContext -> BlockStateArgument.getBlock(commandContext, "block"),
                                          commandContext -> BlockPredicateArgument.getBlockPredicate(commandContext, "filter")
                                       )
                                    )
                              )
                              .then(
                                 Commands.literal("keep")
                                    .executes(
                                       commandContext -> fillBlocks(
                                          (CommandSourceStack)commandContext.getSource(),
                                          BoundingBox.fromCorners(
                                             BlockPosArgument.getLoadedBlockPos(commandContext, "from"),
                                             BlockPosArgument.getLoadedBlockPos(commandContext, "to")
                                          ),
                                          BlockStateArgument.getBlock(commandContext, "block"),
                                          FillCommand.Mode.REPLACE,
                                          blockInWorld -> blockInWorld.getLevel().isEmptyBlock(blockInWorld.getPos()),
                                          false
                                       )
                                    )
                              )
                        )
                  )
            )
      );
   }

   private static ArgumentBuilder<CommandSourceStack, ?> wrapWithMode(
      CommandBuildContext buildContext,
      ArgumentBuilder<CommandSourceStack, ?> argumentBuilder,
      InCommandFunction<CommandContext<CommandSourceStack>, BlockPos> from,
      InCommandFunction<CommandContext<CommandSourceStack>, BlockPos> to,
      InCommandFunction<CommandContext<CommandSourceStack>, BlockInput> block,
      FillCommand.NullableCommandFunction<CommandContext<CommandSourceStack>, Predicate<BlockInWorld>> filter
   ) {
      return argumentBuilder.executes(
            commandContext -> fillBlocks(
               (CommandSourceStack)commandContext.getSource(),
               BoundingBox.fromCorners(from.apply(commandContext), to.apply(commandContext)),
               block.apply(commandContext),
               FillCommand.Mode.REPLACE,
               filter.apply(commandContext),
               false
            )
         )
         .then(
            Commands.literal("outline")
               .executes(
                  commandContext -> fillBlocks(
                     (CommandSourceStack)commandContext.getSource(),
                     BoundingBox.fromCorners(from.apply(commandContext), to.apply(commandContext)),
                     block.apply(commandContext),
                     FillCommand.Mode.OUTLINE,
                     filter.apply(commandContext),
                     false
                  )
               )
         )
         .then(
            Commands.literal("hollow")
               .executes(
                  commandContext -> fillBlocks(
                     (CommandSourceStack)commandContext.getSource(),
                     BoundingBox.fromCorners(from.apply(commandContext), to.apply(commandContext)),
                     block.apply(commandContext),
                     FillCommand.Mode.HOLLOW,
                     filter.apply(commandContext),
                     false
                  )
               )
         )
         .then(
            Commands.literal("destroy")
               .executes(
                  commandContext -> fillBlocks(
                     (CommandSourceStack)commandContext.getSource(),
                     BoundingBox.fromCorners(from.apply(commandContext), to.apply(commandContext)),
                     block.apply(commandContext),
                     FillCommand.Mode.DESTROY,
                     filter.apply(commandContext),
                     false
                  )
               )
         )
         .then(
            Commands.literal("strict")
               .executes(
                  commandContext -> fillBlocks(
                     (CommandSourceStack)commandContext.getSource(),
                     BoundingBox.fromCorners(from.apply(commandContext), to.apply(commandContext)),
                     block.apply(commandContext),
                     FillCommand.Mode.REPLACE,
                     filter.apply(commandContext),
                     true
                  )
               )
         );
   }

   private static int fillBlocks(
      CommandSourceStack source, BoundingBox box, BlockInput block, FillCommand.Mode mode, @Nullable Predicate<BlockInWorld> filter, boolean strict
   ) throws CommandSyntaxException {
      int i = box.getXSpan() * box.getYSpan() * box.getZSpan();
      int j = source.getLevel().getGameRules().get(GameRules.MAX_BLOCK_MODIFICATIONS);
      if (i > j) {
         throw ERROR_AREA_TOO_LARGE.create(j, i);
      } else {
         record UpdatedPosition(BlockPos pos, BlockState oldState) {
         }

         List<UpdatedPosition> list = Lists.newArrayList();
         ServerLevel serverLevel = source.getLevel();
         if (serverLevel.isDebug()) {
            throw ERROR_FAILED.create();
         } else {
            int k = 0;

            for (BlockPos blockPos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
               if (filter == null || filter.test(new BlockInWorld(serverLevel, blockPos, true))) {
                  BlockState blockState = serverLevel.getBlockState(blockPos);
                  boolean bl = false;
                  if (mode.affector.affect(serverLevel, blockPos)) {
                     bl = true;
                  }

                  BlockInput blockInput = mode.filter.filter(box, blockPos, block, serverLevel);
                  if (blockInput == null) {
                     if (bl) {
                        k++;
                     }
                  } else if (!blockInput.place(serverLevel, blockPos, 2 | (strict ? 816 : 256))) {
                     if (bl) {
                        k++;
                     }
                  } else {
                     if (!strict) {
                        list.add(new UpdatedPosition(blockPos.immutable(), blockState));
                     }

                     k++;
                  }
               }
            }

            for (UpdatedPosition lv : list) {
               serverLevel.updateNeighboursOnBlockSet(lv.pos, lv.oldState);
            }

            if (k == 0) {
               throw ERROR_FAILED.create();
            } else {
               int l = k;
               source.sendSuccess(() -> Component.translatable("commands.fill.success", l), true);
               return k;
            }
         }
      }
   }

   @FunctionalInterface
   public interface Affector {
      FillCommand.Affector NOOP = (serverLevel, blockPos) -> false;

      boolean affect(ServerLevel serverLevel, BlockPos blockPos);
   }

   @FunctionalInterface
   public interface Filter {
      FillCommand.Filter NOOP = (boundingBox, blockPos, blockInput, serverLevel) -> blockInput;

      @Nullable
      BlockInput filter(BoundingBox boundingBox, BlockPos blockPos, BlockInput blockInput, ServerLevel serverLevel);
   }

   static enum Mode {
      REPLACE(FillCommand.Affector.NOOP, FillCommand.Filter.NOOP),
      OUTLINE(
         FillCommand.Affector.NOOP,
         (boundingBox, blockPos, blockInput, serverLevel) -> blockPos.getX() != boundingBox.minX()
               && blockPos.getX() != boundingBox.maxX()
               && blockPos.getY() != boundingBox.minY()
               && blockPos.getY() != boundingBox.maxY()
               && blockPos.getZ() != boundingBox.minZ()
               && blockPos.getZ() != boundingBox.maxZ()
            ? null
            : blockInput
      ),
      HOLLOW(
         FillCommand.Affector.NOOP,
         (boundingBox, blockPos, blockInput, serverLevel) -> blockPos.getX() != boundingBox.minX()
               && blockPos.getX() != boundingBox.maxX()
               && blockPos.getY() != boundingBox.minY()
               && blockPos.getY() != boundingBox.maxY()
               && blockPos.getZ() != boundingBox.minZ()
               && blockPos.getZ() != boundingBox.maxZ()
            ? FillCommand.HOLLOW_CORE
            : blockInput
      ),
      DESTROY((serverLevel, blockPos) -> serverLevel.destroyBlock(blockPos, true), FillCommand.Filter.NOOP);

      public final FillCommand.Filter filter;
      public final FillCommand.Affector affector;

      private Mode(final FillCommand.Affector affector, final FillCommand.Filter filter) {
         this.affector = affector;
         this.filter = filter;
      }
   }

   @FunctionalInterface
   interface NullableCommandFunction<T, R> {
      @Nullable
      R apply(T object) throws CommandSyntaxException;
   }
}
