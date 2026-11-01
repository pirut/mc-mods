package net.minecraft.client.gui.narration;

import com.google.common.collect.Maps;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class ScreenNarrationCollector {
   int generation;
   final Map<ScreenNarrationCollector.EntryKey, ScreenNarrationCollector.NarrationEntry> entries = Maps.newTreeMap(
      Comparator.<ScreenNarrationCollector.EntryKey, NarratedElementType>comparing(entryKey -> entryKey.type).thenComparing(entryKey -> entryKey.depth)
   );

   public void update(Consumer<NarrationElementOutput> updater) {
      this.generation++;
      updater.accept(new ScreenNarrationCollector.Output(0));
   }

   public String collectNarrationText(boolean collectAll) {
      final StringBuilder stringBuilder = new StringBuilder();
      Consumer<String> consumer = new Consumer<String>() {
         private boolean firstEntry = true;

         public void accept(String str) {
            if (!this.firstEntry) {
               stringBuilder.append(". ");
            }

            this.firstEntry = false;
            stringBuilder.append(str);
         }
      };
      this.entries.forEach((entryKey, narrationEntry) -> {
         if (narrationEntry.generation == this.generation && (collectAll || !narrationEntry.alreadyNarrated)) {
            narrationEntry.contents.getText(consumer);
            narrationEntry.alreadyNarrated = true;
         }
      });
      return stringBuilder.toString();
   }

   @Environment(EnvType.CLIENT)
   record EntryKey(NarratedElementType type, int depth) {
   }

   @Environment(EnvType.CLIENT)
   static class NarrationEntry {
      NarrationThunk<?> contents = NarrationThunk.EMPTY;
      int generation = -1;
      boolean alreadyNarrated;

      public ScreenNarrationCollector.NarrationEntry update(int generation, NarrationThunk<?> contents) {
         if (!this.contents.equals(contents)) {
            this.contents = contents;
            this.alreadyNarrated = false;
         } else if (this.generation + 1 != generation) {
            this.alreadyNarrated = false;
         }

         this.generation = generation;
         return this;
      }
   }

   @Environment(EnvType.CLIENT)
   class Output implements NarrationElementOutput {
      private final int depth;

      Output(final int depth) {
         this.depth = depth;
      }

      @Override
      public void add(NarratedElementType type, NarrationThunk<?> contents) {
         ScreenNarrationCollector.this.entries
            .computeIfAbsent(new ScreenNarrationCollector.EntryKey(type, this.depth), entryKey -> new ScreenNarrationCollector.NarrationEntry())
            .update(ScreenNarrationCollector.this.generation, contents);
      }

      @Override
      public NarrationElementOutput nest() {
         return ScreenNarrationCollector.this.new Output(this.depth + 1);
      }
   }
}
