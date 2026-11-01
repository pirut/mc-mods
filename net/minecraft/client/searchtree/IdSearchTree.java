package net.minecraft.client.searchtree;

import com.google.common.collect.ImmutableList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

@Environment(EnvType.CLIENT)
public class IdSearchTree<T> implements SearchTree<T> {
   protected final Comparator<T> additionOrder;
   protected final IdentifierSearchTree<T> identifierSearchTree;

   public IdSearchTree(Function<T, Stream<Identifier>> idGetter, List<T> contents) {
      ToIntFunction<T> toIntFunction = Util.createIndexLookup(contents);
      this.additionOrder = Comparator.comparingInt(toIntFunction);
      this.identifierSearchTree = IdentifierSearchTree.create(contents, idGetter);
   }

   @Override
   public List<T> search(String string) {
      int i = string.indexOf(58);
      return i == -1 ? this.searchPlainText(string) : this.searchIdentifier(string.substring(0, i).trim(), string.substring(i + 1).trim());
   }

   protected List<T> searchPlainText(String query) {
      return this.identifierSearchTree.searchPath(query);
   }

   protected List<T> searchIdentifier(String namespace, String path) {
      List<T> list = this.identifierSearchTree.searchNamespace(namespace);
      List<T> list2 = this.identifierSearchTree.searchPath(path);
      return ImmutableList.copyOf(new IntersectionIterator<T>(list.iterator(), list2.iterator(), this.additionOrder));
   }
}
