package net.neoforged.neoforge.server.console;

import net.minecraft.server.dedicated.DedicatedServer;
import net.minecrell.terminalconsole.TerminalConsoleAppender;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.LineReader.Option;
import org.jline.terminal.Terminal;

public final class TerminalHandler {
   private TerminalHandler() {
   }

   public static boolean handleCommands(DedicatedServer server) {
      Terminal terminal = TerminalConsoleAppender.getTerminal();
      if (terminal == null) {
         return false;
      } else {
         LineReader reader = LineReaderBuilder.builder().appName("NeoForge").terminal(terminal).completer(new ConsoleCommandCompleter(server)).build();
         reader.setOpt(Option.DISABLE_EVENT_EXPANSION);
         reader.unsetOpt(Option.INSERT_TAB);
         TerminalConsoleAppender.setReader(reader);

         try {
            while (!server.isStopped() && server.isRunning()) {
               String line;
               try {
                  line = reader.readLine("> ");
               } catch (EndOfFileException var9) {
                  continue;
               }

               if (line == null) {
                  break;
               }

               line = line.trim();
               if (!line.isEmpty()) {
                  server.handleConsoleInput(line, server.createCommandSourceStack());
               }
            }
         } catch (UserInterruptException var10) {
            server.halt(true);
         } finally {
            TerminalConsoleAppender.setReader(null);
         }

         return true;
      }
   }
}
