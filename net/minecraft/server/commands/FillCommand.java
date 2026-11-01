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
      (p_304218_, p_304219_) -> Component.translatableEscape("commands.fill.toobig", p_304218_, p_304219_)
   );
   static final BlockInput HOLLOW_CORE = new BlockInput(Blocks.AIR.defaultBlockState(), Collections.emptySet(), null);
   private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.fill.failed"));

   public static void register(CommandDispatcher<CommandSourceStack> p_214443_, CommandBuildContext p_214444_) {
      p_214443_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("fill").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
            .then(
               Commands.argument("from", BlockPosArgument.blockPos())
                  .then(
                     Commands.argument("to", BlockPosArgument.blockPos())
                        .then(
                           wrapWithMode(
                                 p_214444_,
                                 Commands.argument("block", BlockStateArgument.block(p_214444_)),
                                 p_392711_ -> BlockPosArgument.getLoadedBlockPos(p_392711_, "from"),
                                 p_392705_ -> BlockPosArgument.getLoadedBlockPos(p_392705_, "to"),
                                 p_392692_ -> BlockStateArgument.getBlock(p_392692_, "block"),
                                 p_392685_ -> null
                              )
                              .then(
                                 ((LiteralArgumentBuilder)Commands.literal("replace")
                                       .executes(
                                          p_392698_ -> fillBlocks(
                                             (CommandSourceStack)p_392698_.getSource(),
                                             BoundingBox.fromCorners(
                                                BlockPosArgument.getLoadedBlockPos(p_392698_, "from"), BlockPosArgument.getLoadedBlockPos(p_392698_, "to")
                                             ),
                                             BlockStateArgument.getBlock(p_392698_, "block"),
                                             FillCommand.Mode.REPLACE,
                                             null,
                                             false
                                          )
                                       ))
                                    .then(
                                       wrapWithMode(
                                          p_214444_,
                                          Commands.argument("filter", BlockPredicateArgument.blockPredicate(p_214444_)),
                                          p_392704_ -> BlockPosArgument.getLoadedBlockPos(p_392704_, "from"),
                                          p_392717_ -> BlockPosArgument.getLoadedBlockPos(p_392717_, "to"),
                                          p_392718_ -> BlockStateArgument.getBlock(p_392718_, "block"),
                                          p_392684_ -> BlockPredicateArgument.getBlockPredicate(p_392684_, "filter")
                                       )
                                    )
                              )
                              .then(
                                 Commands.literal("keep")
                                    .executes(
                                       p_392691_ -> fillBlocks(
                                          (CommandSourceStack)p_392691_.getSource(),
                                          BoundingBox.fromCorners(
                                             BlockPosArgument.getLoadedBlockPos(p_392691_, "from"), BlockPosArgument.getLoadedBlockPos(p_392691_, "to")
                                          ),
                                          BlockStateArgument.getBlock(p_392691_, "block"),
                                          FillCommand.Mode.REPLACE,
                                          p_180225_ -> p_180225_.getLevel().isEmptyBlock(p_180225_.getPos()),
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
      CommandBuildContext p_394423_,
      ArgumentBuilder<CommandSourceStack, ?> p_393983_,
      InCommandFunction<CommandContext<CommandSourceStack>, BlockPos> p_397131_,
      InCommandFunction<CommandContext<CommandSourceStack>, BlockPos> p_397847_,
      InCommandFunction<CommandContext<CommandSourceStack>, BlockInput> p_397185_,
      FillCommand.NullableCommandFunction<CommandContext<CommandSourceStack>, Predicate<BlockInWorld>> p_394036_
   ) {
      return p_393983_.executes(
            p_396538_ -> fillBlocks(
               (CommandSourceStack)p_396538_.getSource(),
               BoundingBox.fromCorners(p_397131_.apply(p_396538_), p_397847_.apply(p_396538_)),
               p_397185_.apply(p_396538_),
               FillCommand.Mode.REPLACE,
               p_394036_.apply(p_396538_),
               false
            )
         )
         .then(
            Commands.literal("outline")
               .executes(
                  p_396553_ -> fillBlocks(
                     (CommandSourceStack)p_396553_.getSource(),
                     BoundingBox.fromCorners(p_397131_.apply(p_396553_), p_397847_.apply(p_396553_)),
                     p_397185_.apply(p_396553_),
                     FillCommand.Mode.OUTLINE,
                     p_394036_.apply(p_396553_),
                     false
                  )
               )
         )
         .then(
            Commands.literal("hollow")
               .executes(
                  p_396548_ -> fillBlocks(
                     (CommandSourceStack)p_396548_.getSource(),
                     BoundingBox.fromCorners(p_397131_.apply(p_396548_), p_397847_.apply(p_396548_)),
                     p_397185_.apply(p_396548_),
                     FillCommand.Mode.HOLLOW,
                     p_394036_.apply(p_396548_),
                     false
                  )
               )
         )
         .then(
            Commands.literal("destroy")
               .executes(
                  p_396543_ -> fillBlocks(
                     (CommandSourceStack)p_396543_.getSource(),
                     BoundingBox.fromCorners(p_397131_.apply(p_396543_), p_397847_.apply(p_396543_)),
                     p_397185_.apply(p_396543_),
                     FillCommand.Mode.DESTROY,
                     p_394036_.apply(p_396543_),
                     false
                  )
               )
         )
         .then(
            Commands.literal("strict")
               .executes(
                  p_396558_ -> fillBlocks(
                     (CommandSourceStack)p_396558_.getSource(),
                     BoundingBox.fromCorners(p_397131_.apply(p_396558_), p_397847_.apply(p_396558_)),
                     p_397185_.apply(p_396558_),
                     FillCommand.Mode.REPLACE,
                     p_394036_.apply(p_396558_),
                     true
                  )
               )
         );
   }

   private static int fillBlocks(
      CommandSourceStack p_137386_,
      BoundingBox p_137387_,
      BlockInput p_137388_,
      FillCommand.Mode p_137389_,
      @Nullable Predicate<BlockInWorld> p_137390_,
      boolean p_393689_
   ) throws CommandSyntaxException {
      int i = p_137387_.getXSpan() * p_137387_.getYSpan() * p_137387_.getZSpan();
      int j = p_137386_.getLevel().getGameRules().get(GameRules.MAX_BLOCK_MODIFICATIONS);
      if (i > j) {
         throw ERROR_AREA_TOO_LARGE.create(j, i);
      } else {
         record UpdatedPosition(BlockPos pos, BlockState oldState) {
         }

         List<UpdatedPosition> list = Lists.newArrayList();
         ServerLevel serverlevel = p_137386_.getLevel();
         if (serverlevel.isDebug()) {
            throw ERROR_FAILED.create();
         } else {
            int k = 0;

            for (BlockPos blockpos : BlockPos.betweenClosed(
               p_137387_.minX(), p_137387_.minY(), p_137387_.minZ(), p_137387_.maxX(), p_137387_.maxY(), p_137387_.maxZ()
            )) {
               if (p_137390_ == null || p_137390_.test(new BlockInWorld(serverlevel, blockpos, true))) {
                  BlockState blockstate = serverlevel.getBlockState(blockpos);
                  boolean flag = false;
                  if (p_137389_.affector.affect(serverlevel, blockpos)) {
                     flag = true;
                  }

                  BlockInput blockinput = p_137389_.filter.filter(p_137387_, blockpos, p_137388_, serverlevel);
                  if (blockinput == null) {
                     if (flag) {
                        k++;
                     }
                  } else if (!blockinput.place(serverlevel, blockpos, 2 | (p_393689_ ? 816 : 256))) {
                     if (flag) {
                        k++;
                     }
                  } else {
                     if (!p_393689_) {
                        list.add(new UpdatedPosition(blockpos.immutable(), blockstate));
                     }

                     k++;
                  }
               }
            }

            for (UpdatedPosition fillcommand$1updatedposition : list) {
               serverlevel.updateNeighboursOnBlockSet(fillcommand$1updatedposition.pos, fillcommand$1updatedposition.oldState);
            }

            if (k == 0) {
               throw ERROR_FAILED.create();
            } else {
               int l = k;
               p_137386_.sendSuccess(() -> Component.translatable("commands.fill.success", l), true);
               return k;
            }
         }
      }
   }

   @FunctionalInterface
   public interface Affector {
      FillCommand.Affector NOOP = (p_393846_, p_393551_) -> false;

      boolean affect(ServerLevel var1, BlockPos var2);
   }

   @FunctionalInterface
   public interface Filter {
      FillCommand.Filter NOOP = (p_394191_, p_393566_, p_394115_, p_394030_) -> p_394115_;

      @Nullable
      BlockInput filter(BoundingBox var1, BlockPos var2, BlockInput var3, ServerLevel var4);
   }

   static enum Mode {
      REPLACE(FillCommand.Affector.NOOP, FillCommand.Filter.NOOP),
      OUTLINE(
         FillCommand.Affector.NOOP,
         (p_137428_, p_137429_, p_137430_, p_137431_) -> p_137429_.getX() != p_137428_.minX()
               && p_137429_.getX() != p_137428_.maxX()
               && p_137429_.getY() != p_137428_.minY()
               && p_137429_.getY() != p_137428_.maxY()
               && p_137429_.getZ() != p_137428_.minZ()
               && p_137429_.getZ() != p_137428_.maxZ()
            ? null
            : p_137430_
      ),
      HOLLOW(
         FillCommand.Affector.NOOP,
         (p_137423_, p_137424_, p_137425_, p_137426_) -> p_137424_.getX() != p_137423_.minX()
               && p_137424_.getX() != p_137423_.maxX()
               && p_137424_.getY() != p_137423_.minY()
               && p_137424_.getY() != p_137423_.maxY()
               && p_137424_.getZ() != p_137423_.minZ()
               && p_137424_.getZ() != p_137423_.maxZ()
            ? FillCommand.HOLLOW_CORE
            : p_137425_
      ),
      DESTROY((p_392719_, p_392720_) -> p_392719_.destroyBlock(p_392720_, true), FillCommand.Filter.NOOP);

      public final FillCommand.Filter filter;
      public final FillCommand.Affector affector;

      private Mode(FillCommand.Affector p_394156_, FillCommand.Filter p_393770_) {
         this.affector = p_394156_;
         this.filter = p_393770_;
      }
   }

   @FunctionalInterface
   interface NullableCommandFunction<T, R> {
      @Nullable
      R apply(T var1) throws CommandSyntaxException;
   }
}
