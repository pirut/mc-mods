package net.minecraft.server.commands;

import com.google.common.base.Stopwatch;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import java.time.Duration;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceOrTagArgument;
import net.minecraft.commands.arguments.ResourceOrTagKeyArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.slf4j.Logger;

public class LocateCommand {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final DynamicCommandExceptionType ERROR_STRUCTURE_NOT_FOUND = new DynamicCommandExceptionType(
      p_304261_ -> Component.translatableEscape("commands.locate.structure.not_found", p_304261_)
   );
   private static final DynamicCommandExceptionType ERROR_STRUCTURE_INVALID = new DynamicCommandExceptionType(
      p_304258_ -> Component.translatableEscape("commands.locate.structure.invalid", p_304258_)
   );
   private static final DynamicCommandExceptionType ERROR_BIOME_NOT_FOUND = new DynamicCommandExceptionType(
      p_304259_ -> Component.translatableEscape("commands.locate.biome.not_found", p_304259_)
   );
   private static final DynamicCommandExceptionType ERROR_POI_NOT_FOUND = new DynamicCommandExceptionType(
      p_304260_ -> Component.translatableEscape("commands.locate.poi.not_found", p_304260_)
   );
   private static final int MAX_STRUCTURE_SEARCH_RADIUS = 100;
   private static final int MAX_BIOME_SEARCH_RADIUS = 6400;
   private static final int BIOME_SAMPLE_RESOLUTION_HORIZONTAL = 32;
   private static final int BIOME_SAMPLE_RESOLUTION_VERTICAL = 64;
   private static final int POI_SEARCH_RADIUS = 256;

   public static void register(CommandDispatcher<CommandSourceStack> p_249870_, CommandBuildContext p_248936_) {
      p_249870_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("locate")
                     .requires(p_214470_ -> p_214470_.hasPermission(2)))
                  .then(
                     Commands.literal("structure")
                        .then(
                           Commands.argument("structure", ResourceOrTagKeyArgument.resourceOrTagKey(Registries.STRUCTURE))
                              .executes(
                                 p_258233_ -> locateStructure(
                                    (CommandSourceStack)p_258233_.getSource(),
                                    ResourceOrTagKeyArgument.getResourceOrTagKey(p_258233_, "structure", Registries.STRUCTURE, ERROR_STRUCTURE_INVALID)
                                 )
                              )
                        )
                  ))
               .then(
                  Commands.literal("biome")
                     .then(
                        Commands.argument("biome", ResourceOrTagArgument.resourceOrTag(p_248936_, Registries.BIOME))
                           .executes(
                              p_258232_ -> locateBiome(
                                 (CommandSourceStack)p_258232_.getSource(), ResourceOrTagArgument.getResourceOrTag(p_258232_, "biome", Registries.BIOME)
                              )
                           )
                     )
               ))
            .then(
               Commands.literal("poi")
                  .then(
                     Commands.argument("poi", ResourceOrTagArgument.resourceOrTag(p_248936_, Registries.POINT_OF_INTEREST_TYPE))
                        .executes(
                           p_258234_ -> locatePoi(
                              (CommandSourceStack)p_258234_.getSource(),
                              ResourceOrTagArgument.getResourceOrTag(p_258234_, "poi", Registries.POINT_OF_INTEREST_TYPE)
                           )
                        )
                  )
            )
      );
   }

   private static Optional<? extends HolderSet.ListBacked<Structure>> getHolders(
      ResourceOrTagKeyArgument.Result<Structure> p_251212_, Registry<Structure> p_249691_
   ) {
      return (Optional<? extends HolderSet.ListBacked<Structure>>)p_251212_.unwrap()
         .map(p_258231_ -> p_249691_.getHolder(p_258231_).map(p_214491_ -> HolderSet.direct(p_214491_)), p_249691_::getTag);
   }

   private static int locateStructure(CommandSourceStack p_214472_, ResourceOrTagKeyArgument.Result<Structure> p_249893_) throws CommandSyntaxException {
      Registry<Structure> $$2 = p_214472_.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE);
      HolderSet<Structure> $$3 = (HolderSet<Structure>)getHolders(p_249893_, $$2).orElseThrow(() -> ERROR_STRUCTURE_INVALID.create(p_249893_.asPrintable()));
      BlockPos $$4 = BlockPos.containing(p_214472_.getPosition());
      ServerLevel $$5 = p_214472_.getLevel();
      Stopwatch $$6 = Stopwatch.createStarted(Util.TICKER);
      Pair<BlockPos, Holder<Structure>> $$7 = $$5.getChunkSource().getGenerator().findNearestMapStructure($$5, $$3, $$4, 100, false);
      $$6.stop();
      if ($$7 == null) {
         throw ERROR_STRUCTURE_NOT_FOUND.create(p_249893_.asPrintable());
      } else {
         return showLocateResult(p_214472_, p_249893_, $$4, $$7, "commands.locate.structure.success", false, $$6.elapsed());
      }
   }

   private static int locateBiome(CommandSourceStack p_252062_, ResourceOrTagArgument.Result<Biome> p_249756_) throws CommandSyntaxException {
      BlockPos $$2 = BlockPos.containing(p_252062_.getPosition());
      Stopwatch $$3 = Stopwatch.createStarted(Util.TICKER);
      Pair<BlockPos, Holder<Biome>> $$4 = p_252062_.getLevel().findClosestBiome3d(p_249756_, $$2, 6400, 32, 64);
      $$3.stop();
      if ($$4 == null) {
         throw ERROR_BIOME_NOT_FOUND.create(p_249756_.asPrintable());
      } else {
         return showLocateResult(p_252062_, p_249756_, $$2, $$4, "commands.locate.biome.success", true, $$3.elapsed());
      }
   }

   private static int locatePoi(CommandSourceStack p_252013_, ResourceOrTagArgument.Result<PoiType> p_249480_) throws CommandSyntaxException {
      BlockPos $$2 = BlockPos.containing(p_252013_.getPosition());
      ServerLevel $$3 = p_252013_.getLevel();
      Stopwatch $$4 = Stopwatch.createStarted(Util.TICKER);
      Optional<Pair<Holder<PoiType>, BlockPos>> $$5 = $$3.getPoiManager().findClosestWithType(p_249480_, $$2, 256, PoiManager.Occupancy.ANY);
      $$4.stop();
      if ($$5.isEmpty()) {
         throw ERROR_POI_NOT_FOUND.create(p_249480_.asPrintable());
      } else {
         return showLocateResult(p_252013_, p_249480_, $$2, $$5.get().swap(), "commands.locate.poi.success", false, $$4.elapsed());
      }
   }

   public static int showLocateResult(
      CommandSourceStack p_263098_,
      ResourceOrTagArgument.Result<?> p_262956_,
      BlockPos p_262917_,
      Pair<BlockPos, ? extends Holder<?>> p_263074_,
      String p_262937_,
      boolean p_263051_,
      Duration p_263028_
   ) {
      String $$7 = (String)p_262956_.unwrap()
         .map(p_248147_ -> p_262956_.asPrintable(), p_315917_ -> p_262956_.asPrintable() + " (" + ((Holder)p_263074_.getSecond()).getRegisteredName() + ")");
      return showLocateResult(p_263098_, p_262917_, p_263074_, p_262937_, p_263051_, $$7, p_263028_);
   }

   public static int showLocateResult(
      CommandSourceStack p_263019_,
      ResourceOrTagKeyArgument.Result<?> p_263031_,
      BlockPos p_262989_,
      Pair<BlockPos, ? extends Holder<?>> p_262959_,
      String p_263045_,
      boolean p_262934_,
      Duration p_262960_
   ) {
      String $$7 = (String)p_263031_.unwrap()
         .map(
            p_214498_ -> p_214498_.location().toString(),
            p_339434_ -> "#" + p_339434_.location() + " (" + ((Holder)p_262959_.getSecond()).getRegisteredName() + ")"
         );
      return showLocateResult(p_263019_, p_262989_, p_262959_, p_263045_, p_262934_, $$7, p_262960_);
   }

   private static int showLocateResult(
      CommandSourceStack p_262983_,
      BlockPos p_263016_,
      Pair<BlockPos, ? extends Holder<?>> p_262941_,
      String p_263083_,
      boolean p_263010_,
      String p_263048_,
      Duration p_263040_
   ) {
      BlockPos $$7 = (BlockPos)p_262941_.getFirst();
      int $$8 = p_263010_ ? Mth.floor(Mth.sqrt((float)p_263016_.distSqr($$7))) : Mth.floor(dist(p_263016_.getX(), p_263016_.getZ(), $$7.getX(), $$7.getZ()));
      String $$9 = p_263010_ ? String.valueOf($$7.getY()) : "~";
      Component $$10 = ComponentUtils.wrapInSquareBrackets(Component.translatable("chat.coordinates", $$7.getX(), $$9, $$7.getZ()))
         .withStyle(
            p_214489_ -> p_214489_.withColor(ChatFormatting.GREEN)
               .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp @s " + $$7.getX() + " " + $$9 + " " + $$7.getZ()))
               .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.coordinates.tooltip")))
         );
      p_262983_.sendSuccess(() -> Component.translatable(p_263083_, p_263048_, $$10, $$8), false);
      LOGGER.info("Locating element " + p_263048_ + " took " + p_263040_.toMillis() + " ms");
      return $$8;
   }

   private static float dist(int p_137854_, int p_137855_, int p_137856_, int p_137857_) {
      int $$4 = p_137856_ - p_137854_;
      int $$5 = p_137857_ - p_137855_;
      return Mth.sqrt($$4 * $$4 + $$5 * $$5);
   }
}
