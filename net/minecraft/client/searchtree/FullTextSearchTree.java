package net.minecraft.client.searchtree;

import com.google.common.collect.ImmutableList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
public class FullTextSearchTree<T> extends IdSearchTree<T> {
   private final SearchTree<T> plainTextSearchTree;

   public FullTextSearchTree(Function<T, Stream<String>> filter, Function<T, Stream<Identifier>> idGetter, List<T> contents) {
      super(idGetter, contents);
      this.plainTextSearchTree = SearchTree.plainText(contents, filter);
   }

   @Override
   protected List<T> searchPlainText(String query) {
      return this.plainTextSearchTree.search(query);
   }

   @Override
   protected List<T> searchIdentifier(String namespace, String path) {
      List<T> list = this.identifierSearchTree.searchNamespace(namespace);
      List<T> list2 = this.identifierSearchTree.searchPath(path);
      List<T> list3 = this.plainTextSearchTree.search(path);
      Iterator<T> iterator = new MergingUniqueIterator<T>(list2.iterator(), list3.iterator(), this.additionOrder);
      return ImmutableList.copyOf(new IntersectionIterator<T>(list.iterator(), iterator, this.additionOrder));
   }
}
