package net.minecraft.client.searchtree;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
public interface IdentifierSearchTree<T> {
   static <T> IdentifierSearchTree<T> empty() {
      return new IdentifierSearchTree<T>() {
         @Override
         public List<T> searchNamespace(String namespace) {
            return List.of();
         }

         @Override
         public List<T> searchPath(String path) {
            return List.of();
         }
      };
   }

   static <T> IdentifierSearchTree<T> create(List<T> values, Function<T, Stream<Identifier>> idGetter) {
      if (values.isEmpty()) {
         return empty();
      } else {
         final SuffixArray<T> suffixArray = new SuffixArray<>();
         final SuffixArray<T> suffixArray2 = new SuffixArray<>();

         for (T object : values) {
            idGetter.apply(object).forEach(identifier -> {
               suffixArray.add(object, identifier.getNamespace().toLowerCase(Locale.ROOT));
               suffixArray2.add(object, identifier.getPath().toLowerCase(Locale.ROOT));
            });
         }

         suffixArray.generate();
         suffixArray2.generate();
         return new IdentifierSearchTree<T>() {
            @Override
            public List<T> searchNamespace(String namespace) {
               return suffixArray.search(namespace);
            }

            @Override
            public List<T> searchPath(String path) {
               return suffixArray2.search(path);
            }
         };
      }
   }

   List<T> searchNamespace(String namespace);

   List<T> searchPath(String path);
}
