package net.minecraft.client;

import com.google.common.base.Charsets;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import net.minecraft.util.ArrayListDeque;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class CommandHistory {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int MAX_PERSISTED_COMMAND_HISTORY = 50;
   private static final String PERSISTED_COMMANDS_FILE_NAME = "command_history.txt";
   private final Path commandsPath;
   private final ArrayListDeque<String> lastCommands = new ArrayListDeque<>(50);

   public CommandHistory(Path p_294191_) {
      this.commandsPath = p_294191_.resolve("command_history.txt");
      if (Files.exists(this.commandsPath)) {
         try (BufferedReader $$1 = Files.newBufferedReader(this.commandsPath, Charsets.UTF_8)) {
            this.lastCommands.addAll($$1.lines().toList());
         } catch (Exception var7) {
            LOGGER.error("Failed to read {}, command history will be missing", "command_history.txt", var7);
         }
      }
   }

   public void addCommand(String p_294362_) {
      if (!p_294362_.equals(this.lastCommands.peekLast())) {
         if (this.lastCommands.size() >= 50) {
            this.lastCommands.removeFirst();
         }

         this.lastCommands.addLast(p_294362_);
         this.save();
      }
   }

   private void save() {
      try (BufferedWriter $$0 = Files.newBufferedWriter(this.commandsPath, Charsets.UTF_8)) {
         for (String $$1 : this.lastCommands) {
            $$0.write($$1);
            $$0.newLine();
         }
      } catch (IOException var6) {
         LOGGER.error("Failed to write {}, command history will be missing", "command_history.txt", var6);
      }
   }

   public Collection<String> history() {
      return this.lastCommands;
   }
}
