package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class SetBlockCommand {
   private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.setblock.failed"));

   public static void register(CommandDispatcher<CommandSourceStack> p_214731_, CommandBuildContext p_214732_) {
      p_214731_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("setblock").requires(p_138606_ -> p_138606_.hasPermission(2)))
            .then(
               Commands.argument("pos", BlockPosArgument.blockPos())
                  .then(
                     ((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument(
                                    "block", BlockStateArgument.block(p_214732_)
                                 )
                                 .executes(
                                    p_138618_ -> setBlock(
                                       (CommandSourceStack)p_138618_.getSource(),
                                       BlockPosArgument.getLoadedBlockPos(p_138618_, "pos"),
                                       BlockStateArgument.getBlock(p_138618_, "block"),
                                       SetBlockCommand.Mode.REPLACE,
                                       null
                                    )
                                 ))
                              .then(
                                 Commands.literal("destroy")
                                    .executes(
                                       p_138616_ -> setBlock(
                                          (CommandSourceStack)p_138616_.getSource(),
                                          BlockPosArgument.getLoadedBlockPos(p_138616_, "pos"),
                                          BlockStateArgument.getBlock(p_138616_, "block"),
                                          SetBlockCommand.Mode.DESTROY,
                                          null
                                       )
                                    )
                              ))
                           .then(
                              Commands.literal("keep")
                                 .executes(
                                    p_138614_ -> setBlock(
                                       (CommandSourceStack)p_138614_.getSource(),
                                       BlockPosArgument.getLoadedBlockPos(p_138614_, "pos"),
                                       BlockStateArgument.getBlock(p_138614_, "block"),
                                       SetBlockCommand.Mode.REPLACE,
                                       p_180517_ -> p_180517_.getLevel().isEmptyBlock(p_180517_.getPos())
                                    )
                                 )
                           ))
                        .then(
                           Commands.literal("replace")
                              .executes(
                                 p_138604_ -> setBlock(
                                    (CommandSourceStack)p_138604_.getSource(),
                                    BlockPosArgument.getLoadedBlockPos(p_138604_, "pos"),
                                    BlockStateArgument.getBlock(p_138604_, "block"),
                                    SetBlockCommand.Mode.REPLACE,
                                    null
                                 )
                              )
                        )
                  )
            )
      );
   }

   private static int setBlock(
      CommandSourceStack p_138608_, BlockPos p_138609_, BlockInput p_138610_, SetBlockCommand.Mode p_138611_, @Nullable Predicate<BlockInWorld> p_138612_
   ) throws CommandSyntaxException {
      ServerLevel $$5 = p_138608_.getLevel();
      if (p_138612_ != null && !p_138612_.test(new BlockInWorld($$5, p_138609_, true))) {
         throw ERROR_FAILED.create();
      } else {
         boolean $$6;
         if (p_138611_ == SetBlockCommand.Mode.DESTROY) {
            $$5.destroyBlock(p_138609_, true);
            $$6 = !p_138610_.getState().isAir() || !$$5.getBlockState(p_138609_).isAir();
         } else {
            BlockEntity $$7 = $$5.getBlockEntity(p_138609_);
            Clearable.tryClear($$7);
            $$6 = true;
         }

         if ($$6 && !p_138610_.place($$5, p_138609_, 2)) {
            throw ERROR_FAILED.create();
         } else {
            $$5.blockUpdated(p_138609_, p_138610_.getState().getBlock());
            p_138608_.sendSuccess(() -> Component.translatable("commands.setblock.success", p_138609_.getX(), p_138609_.getY(), p_138609_.getZ()), true);
            return 1;
         }
      }
   }

   public interface Filter {
      @Nullable
      BlockInput filter(BoundingBox var1, BlockPos var2, BlockInput var3, ServerLevel var4);
   }

   public static enum Mode {
      REPLACE,
      DESTROY;
   }
}
