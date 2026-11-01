package net.neoforged.neoforge.server.command.generation;

import java.text.DecimalFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.neoforged.neoforge.server.command.CommandUtils;

public class GenerationBar implements AutoCloseable {
   private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("#.00");
   private final ServerBossEvent bar = new ServerBossEvent(
      CommandUtils.makeTranslatableWithFallback("commands.neoforge.chunkgen.progress_bar_title"),
      BossEvent.BossBarColor.YELLOW,
      BossEvent.BossBarOverlay.PROGRESS
   );

   public GenerationBar() {
      this.bar.setPlayBossMusic(false);
      this.bar.setCreateWorldFog(false);
      this.bar.setDarkenScreen(false);
   }

   public void update(int ok, int error, int skipped, int total) {
      int count = ok + error + skipped;
      float percent = (float)count / total;
      MutableComponent title = CommandUtils.makeTranslatableWithFallback("commands.neoforge.chunkgen.progress_bar_progress", total)
         .append(CommandUtils.makeTranslatableWithFallback(PERCENT_FORMAT.format(percent * 100.0F) + "%").setStyle(Style.EMPTY.withColor(ChatFormatting.GOLD)));
      if (error > 0) {
         title = title.append(
            CommandUtils.makeTranslatableWithFallback("commands.neoforge.chunkgen.progress_bar_errors").setStyle(Style.EMPTY.withColor(ChatFormatting.RED))
         );
      }

      this.bar.setName(title);
      this.bar.setProgress(percent);
   }

   public void addPlayer(ServerPlayer player) {
      this.bar.addPlayer(player);
   }

   @Override
   public void close() {
      this.bar.setVisible(false);
      this.bar.removeAllPlayers();
   }
}
