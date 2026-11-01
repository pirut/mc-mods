package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.datafixers.util.Either;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.ResourceOrTagArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.apache.commons.lang3.mutable.MutableInt;

public class FillBiomeCommand {
   public static final SimpleCommandExceptionType ERROR_NOT_LOADED = new SimpleCommandExceptionType(Component.translatable("argument.pos.unloaded"));
   private static final Dynamic2CommandExceptionType ERROR_VOLUME_TOO_LARGE = new Dynamic2CommandExceptionType(
      (p_304216_, p_304217_) -> Component.translatableEscape("commands.fillbiome.toobig", p_304216_, p_304217_)
   );

   public static void register(CommandDispatcher<CommandSourceStack> p_261867_, CommandBuildContext p_262155_) {
      p_261867_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("fillbiome").requires(p_261890_ -> p_261890_.hasPermission(2)))
            .then(
               Commands.argument("from", BlockPosArgument.blockPos())
                  .then(
                     Commands.argument("to", BlockPosArgument.blockPos())
                        .then(
                           ((RequiredArgumentBuilder)Commands.argument("biome", ResourceArgument.resource(p_262155_, Registries.BIOME))
                                 .executes(
                                    p_262554_ -> fill(
                                       (CommandSourceStack)p_262554_.getSource(),
                                       BlockPosArgument.getLoadedBlockPos(p_262554_, "from"),
                                       BlockPosArgument.getLoadedBlockPos(p_262554_, "to"),
                                       ResourceArgument.getResource(p_262554_, "biome", Registries.BIOME),
                                       p_313492_ -> true
                                    )
                                 ))
                              .then(
                                 Commands.literal("replace")
                                    .then(
                                       Commands.argument("filter", ResourceOrTagArgument.resourceOrTag(p_262155_, Registries.BIOME))
                                          .executes(
                                             p_262544_ -> fill(
                                                (CommandSourceStack)p_262544_.getSource(),
                                                BlockPosArgument.getLoadedBlockPos(p_262544_, "from"),
                                                BlockPosArgument.getLoadedBlockPos(p_262544_, "to"),
                                                ResourceArgument.getResource(p_262544_, "biome", Registries.BIOME),
                                                ResourceOrTagArgument.getResourceOrTag(p_262544_, "filter", Registries.BIOME)::test
                                             )
                                          )
                                    )
                              )
                        )
                  )
            )
      );
   }

   private static int quantize(int p_261998_) {
      return QuartPos.toBlock(QuartPos.fromBlock(p_261998_));
   }

   private static BlockPos quantize(BlockPos p_262148_) {
      return new BlockPos(quantize(p_262148_.getX()), quantize(p_262148_.getY()), quantize(p_262148_.getZ()));
   }

   private static BiomeResolver makeResolver(
      MutableInt p_262615_, ChunkAccess p_262698_, BoundingBox p_262622_, Holder<Biome> p_262705_, Predicate<Holder<Biome>> p_262695_
   ) {
      return (p_262550_, p_262551_, p_262552_, p_262553_) -> {
         int $$9 = QuartPos.toBlock(p_262550_);
         int $$10 = QuartPos.toBlock(p_262551_);
         int $$11 = QuartPos.toBlock(p_262552_);
         Holder<Biome> $$12 = p_262698_.getNoiseBiome(p_262550_, p_262551_, p_262552_);
         if (p_262622_.isInside($$9, $$10, $$11) && p_262695_.test($$12)) {
            p_262615_.increment();
            return p_262705_;
         } else {
            return $$12;
         }
      };
   }

   public static Either<Integer, CommandSyntaxException> fill(ServerLevel p_313898_, BlockPos p_313859_, BlockPos p_313868_, Holder<Biome> p_313728_) {
      return fill(p_313898_, p_313859_, p_313868_, p_313728_, p_262543_ -> true, p_313489_ -> {});
   }

   public static Either<Integer, CommandSyntaxException> fill(
      ServerLevel p_313779_,
      BlockPos p_313726_,
      BlockPos p_313895_,
      Holder<Biome> p_313905_,
      Predicate<Holder<Biome>> p_313945_,
      Consumer<Supplier<Component>> p_313787_
   ) {
      BlockPos $$6 = quantize(p_313726_);
      BlockPos $$7 = quantize(p_313895_);
      BoundingBox $$8 = BoundingBox.fromCorners($$6, $$7);
      int $$9 = $$8.getXSpan() * $$8.getYSpan() * $$8.getZSpan();
      int $$10 = p_313779_.getGameRules().getInt(GameRules.RULE_COMMAND_MODIFICATION_BLOCK_LIMIT);
      if ($$9 > $$10) {
         return Either.right(ERROR_VOLUME_TOO_LARGE.create($$10, $$9));
      } else {
         List<ChunkAccess> $$11 = new ArrayList<>();

         for (int $$12 = SectionPos.blockToSectionCoord($$8.minZ()); $$12 <= SectionPos.blockToSectionCoord($$8.maxZ()); $$12++) {
            for (int $$13 = SectionPos.blockToSectionCoord($$8.minX()); $$13 <= SectionPos.blockToSectionCoord($$8.maxX()); $$13++) {
               ChunkAccess $$14 = p_313779_.getChunk($$13, $$12, ChunkStatus.FULL, false);
               if ($$14 == null) {
                  return Either.right(ERROR_NOT_LOADED.create());
               }

               $$11.add($$14);
            }
         }

         MutableInt $$15 = new MutableInt(0);

         for (ChunkAccess $$16 : $$11) {
            $$16.fillBiomesFromNoise(makeResolver($$15, $$16, $$8, p_313905_, p_313945_), p_313779_.getChunkSource().randomState().sampler());
            $$16.setUnsaved(true);
         }

         p_313779_.getChunkSource().chunkMap.resendBiomesForChunks($$11);
         p_313787_.accept(
            () -> Component.translatable(
               "commands.fillbiome.success.count", $$15.getValue(), $$8.minX(), $$8.minY(), $$8.minZ(), $$8.maxX(), $$8.maxY(), $$8.maxZ()
            )
         );
         return Either.left($$15.getValue());
      }
   }

   private static int fill(
      CommandSourceStack p_262664_, BlockPos p_262651_, BlockPos p_262678_, Holder.Reference<Biome> p_262612_, Predicate<Holder<Biome>> p_262697_
   ) throws CommandSyntaxException {
      Either<Integer, CommandSyntaxException> $$5 = fill(
         p_262664_.getLevel(), p_262651_, p_262678_, p_262612_, p_262697_, p_313491_ -> p_262664_.sendSuccess(p_313491_, true)
      );
      Optional<CommandSyntaxException> $$6 = $$5.right();
      if ($$6.isPresent()) {
         throw (CommandSyntaxException)$$6.get();
      } else {
         return (Integer)$$5.left().get();
      }
   }
}
