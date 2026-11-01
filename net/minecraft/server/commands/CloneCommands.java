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
import java.util.Deque;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
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
import net.minecraft.world.Clearable;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class CloneCommands {
   private static final SimpleCommandExceptionType ERROR_OVERLAP = new SimpleCommandExceptionType(Component.translatable("commands.clone.overlap"));
   private static final Dynamic2CommandExceptionType ERROR_AREA_TOO_LARGE = new Dynamic2CommandExceptionType(
      (p_304194_, p_304195_) -> Component.translatableEscape("commands.clone.toobig", p_304194_, p_304195_)
   );
   private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.clone.failed"));
   public static final Predicate<BlockInWorld> FILTER_AIR = p_284652_ -> !p_284652_.getState().isAir();

   public static void register(CommandDispatcher<CommandSourceStack> p_214424_, CommandBuildContext p_214425_) {
      p_214424_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("clone").requires(p_136734_ -> p_136734_.hasPermission(2)))
               .then(beginEndDestinationAndModeSuffix(p_214425_, p_264757_ -> ((CommandSourceStack)p_264757_.getSource()).getLevel())))
            .then(
               Commands.literal("from")
                  .then(
                     Commands.argument("sourceDimension", DimensionArgument.dimension())
                        .then(beginEndDestinationAndModeSuffix(p_214425_, p_264743_ -> DimensionArgument.getDimension(p_264743_, "sourceDimension")))
                  )
            )
      );
   }

   private static ArgumentBuilder<CommandSourceStack, ?> beginEndDestinationAndModeSuffix(
      CommandBuildContext p_265681_, CloneCommands.CommandFunction<CommandContext<CommandSourceStack>, ServerLevel> p_265514_
   ) {
      return Commands.argument("begin", BlockPosArgument.blockPos())
         .then(
            ((RequiredArgumentBuilder)Commands.argument("end", BlockPosArgument.blockPos())
                  .then(destinationAndModeSuffix(p_265681_, p_265514_, p_264751_ -> ((CommandSourceStack)p_264751_.getSource()).getLevel())))
               .then(
                  Commands.literal("to")
                     .then(
                        Commands.argument("targetDimension", DimensionArgument.dimension())
                           .then(destinationAndModeSuffix(p_265681_, p_265514_, p_264756_ -> DimensionArgument.getDimension(p_264756_, "targetDimension")))
                     )
               )
         );
   }

   private static CloneCommands.DimensionAndPosition getLoadedDimensionAndPosition(
      CommandContext<CommandSourceStack> p_265513_, ServerLevel p_265183_, String p_265511_
   ) throws CommandSyntaxException {
      BlockPos $$3 = BlockPosArgument.getLoadedBlockPos(p_265513_, p_265183_, p_265511_);
      return new CloneCommands.DimensionAndPosition(p_265183_, $$3);
   }

   private static ArgumentBuilder<CommandSourceStack, ?> destinationAndModeSuffix(
      CommandBuildContext p_265238_,
      CloneCommands.CommandFunction<CommandContext<CommandSourceStack>, ServerLevel> p_265621_,
      CloneCommands.CommandFunction<CommandContext<CommandSourceStack>, ServerLevel> p_265296_
   ) {
      CloneCommands.CommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> $$3 = p_264737_ -> getLoadedDimensionAndPosition(
         p_264737_, p_265621_.apply(p_264737_), "begin"
      );
      CloneCommands.CommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> $$4 = p_264735_ -> getLoadedDimensionAndPosition(
         p_264735_, p_265621_.apply(p_264735_), "end"
      );
      CloneCommands.CommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> $$5 = p_264768_ -> getLoadedDimensionAndPosition(
         p_264768_, p_265296_.apply(p_264768_), "destination"
      );
      return ((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("destination", BlockPosArgument.blockPos())
                  .executes(
                     p_264761_ -> clone(
                        (CommandSourceStack)p_264761_.getSource(),
                        $$3.apply(p_264761_),
                        $$4.apply(p_264761_),
                        $$5.apply(p_264761_),
                        p_180033_ -> true,
                        CloneCommands.Mode.NORMAL
                     )
                  ))
               .then(
                  wrapWithCloneMode(
                     $$3,
                     $$4,
                     $$5,
                     p_264738_ -> p_180041_ -> true,
                     Commands.literal("replace")
                        .executes(
                           p_264755_ -> clone(
                              (CommandSourceStack)p_264755_.getSource(),
                              $$3.apply(p_264755_),
                              $$4.apply(p_264755_),
                              $$5.apply(p_264755_),
                              p_180039_ -> true,
                              CloneCommands.Mode.NORMAL
                           )
                        )
                  )
               ))
            .then(
               wrapWithCloneMode(
                  $$3,
                  $$4,
                  $$5,
                  p_264744_ -> FILTER_AIR,
                  Commands.literal("masked")
                     .executes(
                        p_264742_ -> clone(
                           (CommandSourceStack)p_264742_.getSource(),
                           $$3.apply(p_264742_),
                           $$4.apply(p_264742_),
                           $$5.apply(p_264742_),
                           FILTER_AIR,
                           CloneCommands.Mode.NORMAL
                        )
                     )
               )
            ))
         .then(
            Commands.literal("filtered")
               .then(
                  wrapWithCloneMode(
                     $$3,
                     $$4,
                     $$5,
                     p_264745_ -> BlockPredicateArgument.getBlockPredicate(p_264745_, "filter"),
                     Commands.argument("filter", BlockPredicateArgument.blockPredicate(p_265238_))
                        .executes(
                           p_264733_ -> clone(
                              (CommandSourceStack)p_264733_.getSource(),
                              $$3.apply(p_264733_),
                              $$4.apply(p_264733_),
                              $$5.apply(p_264733_),
                              BlockPredicateArgument.getBlockPredicate(p_264733_, "filter"),
                              CloneCommands.Mode.NORMAL
                           )
                        )
                  )
               )
         );
   }

   private static ArgumentBuilder<CommandSourceStack, ?> wrapWithCloneMode(
      CloneCommands.CommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> p_265374_,
      CloneCommands.CommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> p_265134_,
      CloneCommands.CommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> p_265546_,
      CloneCommands.CommandFunction<CommandContext<CommandSourceStack>, Predicate<BlockInWorld>> p_265798_,
      ArgumentBuilder<CommandSourceStack, ?> p_265069_
   ) {
      return p_265069_.then(
            Commands.literal("force")
               .executes(
                  p_264773_ -> clone(
                     (CommandSourceStack)p_264773_.getSource(),
                     p_265374_.apply(p_264773_),
                     p_265134_.apply(p_264773_),
                     p_265546_.apply(p_264773_),
                     p_265798_.apply(p_264773_),
                     CloneCommands.Mode.FORCE
                  )
               )
         )
         .then(
            Commands.literal("move")
               .executes(
                  p_264766_ -> clone(
                     (CommandSourceStack)p_264766_.getSource(),
                     p_265374_.apply(p_264766_),
                     p_265134_.apply(p_264766_),
                     p_265546_.apply(p_264766_),
                     p_265798_.apply(p_264766_),
                     CloneCommands.Mode.MOVE
                  )
               )
         )
         .then(
            Commands.literal("normal")
               .executes(
                  p_264750_ -> clone(
                     (CommandSourceStack)p_264750_.getSource(),
                     p_265374_.apply(p_264750_),
                     p_265134_.apply(p_264750_),
                     p_265546_.apply(p_264750_),
                     p_265798_.apply(p_264750_),
                     CloneCommands.Mode.NORMAL
                  )
               )
         );
   }

   private static int clone(
      CommandSourceStack p_265047_,
      CloneCommands.DimensionAndPosition p_265232_,
      CloneCommands.DimensionAndPosition p_265188_,
      CloneCommands.DimensionAndPosition p_265594_,
      Predicate<BlockInWorld> p_265585_,
      CloneCommands.Mode p_265530_
   ) throws CommandSyntaxException {
      BlockPos $$6 = p_265232_.position();
      BlockPos $$7 = p_265188_.position();
      BoundingBox $$8 = BoundingBox.fromCorners($$6, $$7);
      BlockPos $$9 = p_265594_.position();
      BlockPos $$10 = $$9.offset($$8.getLength());
      BoundingBox $$11 = BoundingBox.fromCorners($$9, $$10);
      ServerLevel $$12 = p_265232_.dimension();
      ServerLevel $$13 = p_265594_.dimension();
      if (!p_265530_.canOverlap() && $$12 == $$13 && $$11.intersects($$8)) {
         throw ERROR_OVERLAP.create();
      } else {
         int $$14 = $$8.getXSpan() * $$8.getYSpan() * $$8.getZSpan();
         int $$15 = p_265047_.getLevel().getGameRules().getInt(GameRules.RULE_COMMAND_MODIFICATION_BLOCK_LIMIT);
         if ($$14 > $$15) {
            throw ERROR_AREA_TOO_LARGE.create($$15, $$14);
         } else if ($$12.hasChunksAt($$6, $$7) && $$13.hasChunksAt($$9, $$10)) {
            List<CloneCommands.CloneBlockInfo> $$16 = Lists.newArrayList();
            List<CloneCommands.CloneBlockInfo> $$17 = Lists.newArrayList();
            List<CloneCommands.CloneBlockInfo> $$18 = Lists.newArrayList();
            Deque<BlockPos> $$19 = Lists.newLinkedList();
            BlockPos $$20 = new BlockPos($$11.minX() - $$8.minX(), $$11.minY() - $$8.minY(), $$11.minZ() - $$8.minZ());

            for (int $$21 = $$8.minZ(); $$21 <= $$8.maxZ(); $$21++) {
               for (int $$22 = $$8.minY(); $$22 <= $$8.maxY(); $$22++) {
                  for (int $$23 = $$8.minX(); $$23 <= $$8.maxX(); $$23++) {
                     BlockPos $$24 = new BlockPos($$23, $$22, $$21);
                     BlockPos $$25 = $$24.offset($$20);
                     BlockInWorld $$26 = new BlockInWorld($$12, $$24, false);
                     BlockState $$27 = $$26.getState();
                     if (p_265585_.test($$26)) {
                        BlockEntity $$28 = $$12.getBlockEntity($$24);
                        if ($$28 != null) {
                           CloneCommands.CloneBlockEntityInfo $$29 = new CloneCommands.CloneBlockEntityInfo(
                              $$28.saveCustomOnly(p_265047_.registryAccess()), $$28.components()
                           );
                           $$17.add(new CloneCommands.CloneBlockInfo($$25, $$27, $$29));
                           $$19.addLast($$24);
                        } else if (!$$27.isSolidRender($$12, $$24) && !$$27.isCollisionShapeFullBlock($$12, $$24)) {
                           $$18.add(new CloneCommands.CloneBlockInfo($$25, $$27, null));
                           $$19.addFirst($$24);
                        } else {
                           $$16.add(new CloneCommands.CloneBlockInfo($$25, $$27, null));
                           $$19.addLast($$24);
                        }
                     }
                  }
               }
            }

            if (p_265530_ == CloneCommands.Mode.MOVE) {
               for (BlockPos $$30 : $$19) {
                  BlockEntity $$31 = $$12.getBlockEntity($$30);
                  Clearable.tryClear($$31);
                  $$12.setBlock($$30, Blocks.BARRIER.defaultBlockState(), 2);
               }

               for (BlockPos $$32 : $$19) {
                  $$12.setBlock($$32, Blocks.AIR.defaultBlockState(), 3);
               }
            }

            List<CloneCommands.CloneBlockInfo> $$33 = Lists.newArrayList();
            $$33.addAll($$16);
            $$33.addAll($$17);
            $$33.addAll($$18);
            List<CloneCommands.CloneBlockInfo> $$34 = Lists.reverse($$33);

            for (CloneCommands.CloneBlockInfo $$35 : $$34) {
               BlockEntity $$36 = $$13.getBlockEntity($$35.pos);
               Clearable.tryClear($$36);
               $$13.setBlock($$35.pos, Blocks.BARRIER.defaultBlockState(), 2);
            }

            int $$37 = 0;

            for (CloneCommands.CloneBlockInfo $$38 : $$33) {
               if ($$13.setBlock($$38.pos, $$38.state, 2)) {
                  $$37++;
               }
            }

            for (CloneCommands.CloneBlockInfo $$39 : $$17) {
               BlockEntity $$40 = $$13.getBlockEntity($$39.pos);
               if ($$39.blockEntityInfo != null && $$40 != null) {
                  $$40.loadCustomOnly($$39.blockEntityInfo.tag, $$13.registryAccess());
                  $$40.setComponents($$39.blockEntityInfo.components);
                  $$40.setChanged();
               }

               $$13.setBlock($$39.pos, $$39.state, 2);
            }

            for (CloneCommands.CloneBlockInfo $$41 : $$34) {
               $$13.blockUpdated($$41.pos, $$41.state.getBlock());
            }

            $$13.getBlockTicks().copyAreaFrom($$12.getBlockTicks(), $$8, $$20);
            if ($$37 == 0) {
               throw ERROR_FAILED.create();
            } else {
               int $$42 = $$37;
               p_265047_.sendSuccess(() -> Component.translatable("commands.clone.success", $$42), true);
               return $$37;
            }
         } else {
            throw BlockPosArgument.ERROR_NOT_LOADED.create();
         }
      }
   }

   record CloneBlockEntityInfo(CompoundTag tag, DataComponentMap components) {
   }

   record CloneBlockInfo(BlockPos pos, BlockState state, @Nullable CloneCommands.CloneBlockEntityInfo blockEntityInfo) {
   }

   @FunctionalInterface
   interface CommandFunction<T, R> {
      R apply(T var1) throws CommandSyntaxException;
   }

   record DimensionAndPosition(ServerLevel dimension, BlockPos position) {
   }

   static enum Mode {
      FORCE(true),
      MOVE(true),
      NORMAL(false);

      private final boolean canOverlap;

      private Mode(boolean p_136795_) {
         this.canOverlap = p_136795_;
      }

      public boolean canOverlap() {
         return this.canOverlap;
      }
   }
}
