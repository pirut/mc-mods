package net.neoforged.neoforge.server.command;

import com.google.common.math.Stats;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.text.DecimalFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.TimeUtil;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.level.dimension.DimensionType;
import org.joml.Math;
import org.jspecify.annotations.Nullable;

class TPSCommand {
   private static final DecimalFormat TIME_FORMATTER = new DecimalFormat("########0.000");
   private static final long[] UNLOADED = new long[]{0L};

   static ArgumentBuilder<CommandSourceStack, ?> register() {
      return ((LiteralArgumentBuilder)Commands.literal("tps")
            .then(
               Commands.argument("dimension", DimensionArgument.dimension()).executes(ctx -> sendTime(ctx, DimensionArgument.getDimension(ctx, "dimension")))
            ))
         .executes(ctx -> {
            for (ServerLevel dimension : ((CommandSourceStack)ctx.getSource()).getServer().getAllLevels()) {
               sendTime(ctx, dimension);
            }

            sendTime(ctx, null);
            return 1;
         });
   }

   private static int sendTime(CommandContext<CommandSourceStack> context, @Nullable ServerLevel dimension) throws CommandSyntaxException {
      CommandSourceStack src = (CommandSourceStack)context.getSource();
      src.sendSuccess(() -> createComponent(src.getServer(), dimension), false);
      return 1;
   }

   private static Component createComponent(MinecraftServer server, @Nullable ServerLevel dimension) {
      long[] times;
      TickRateManager tickRateManager;
      if (dimension == null) {
         times = server.getTickTimesNanos();
         tickRateManager = server.tickRateManager();
      } else {
         long[] dimensionTimes = server.getTickTime(dimension.dimension());
         times = dimensionTimes == null ? UNLOADED : dimensionTimes;
         tickRateManager = dimension.tickRateManager();
      }

      double tickTime = Stats.meanOf(times) / TimeUtil.NANOSECONDS_PER_MILLISECOND;
      double tps = TimeUtil.MILLISECONDS_PER_SECOND / Math.max(tickTime, tickRateManager.millisecondsPerTick());
      MutableComponent tickTimeComponent = Component.literal(TIME_FORMATTER.format(tickTime)).withColor(-6250336);
      MutableComponent tpsComponent = Component.literal(TIME_FORMATTER.format(tps)).withColor(calculateTPSColor(tickRateManager, tps));
      MutableComponent component;
      if (dimension == null) {
         component = CommandUtils.makeTranslatableWithFallback("commands.neoforge.tps.overall", tpsComponent, tickTimeComponent);
      } else {
         Holder<DimensionType> dimensionType = dimension.dimensionTypeRegistration();
         MutableComponent dimensionName = Component.empty()
            .append(dimension.getDescription())
            .withStyle(
               style -> style.withColor(ChatFormatting.GREEN)
                  .withHoverEvent(
                     new HoverEvent.ShowText(
                        CommandUtils.makeTranslatableWithFallback(
                           "commands.neoforge.tps.dimension.tooltip", dimension.dimension().identifier().toString(), dimensionType.getRegisteredName()
                        )
                     )
                  )
            );
         component = CommandUtils.makeTranslatableWithFallback("commands.neoforge.tps.dimension", dimensionName, tpsComponent, tickTimeComponent);
      }

      return component.withStyle(
         style -> style.withHoverEvent(
            new HoverEvent.ShowText(CommandUtils.makeTranslatableWithFallback("commands.neoforge.tps.tooltip", tickRateManager.tickrate()))
         )
      );
   }

   private static int calculateTPSColor(TickRateManager tickRateManager, double tps) {
      float maxTPS = (float)TimeUtil.MILLISECONDS_PER_SECOND / tickRateManager.millisecondsPerTick();
      return Mth.hsvToRgb((float)(Mth.inverseLerp(tps, 0.0, (double)maxTPS) * 0.33F), 1.0F, 1.0F);
   }
}
