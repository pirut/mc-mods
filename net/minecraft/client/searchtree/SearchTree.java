package net.minecraft.client.searchtree;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Stream;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@FunctionalInterface
@OnlyIn(Dist.CLIENT)
public interface SearchTree<T> {
   static <T> SearchTree<T> empty() {
      return p_344720_ -> List.of();
   }

   static <T> SearchTree<T> plainText(List<T> p_346366_, Function<T, Stream<String>> p_346287_) {
      if (p_346366_.isEmpty()) {
         return empty();
      } else {
         SuffixArray<T> $$2 = new SuffixArray<>();

         for (T $$3 : p_346366_) {
            p_346287_.apply($$3).forEach(p_344960_ -> $$2.add($$3, p_344960_.toLowerCase(Locale.ROOT)));
         }

         $$2.generate();
         return $$2::search;
      }
   }

   List<T> search(String var1);
}
