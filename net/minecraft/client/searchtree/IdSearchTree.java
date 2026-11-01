package net.minecraft.client.searchtree;

import com.google.common.collect.ImmutableList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class IdSearchTree<T> implements SearchTree<T> {
   protected final Comparator<T> additionOrder;
   protected final ResourceLocationSearchTree<T> resourceLocationSearchTree;

   public IdSearchTree(Function<T, Stream<ResourceLocation>> p_235167_, List<T> p_235168_) {
      ToIntFunction<T> $$2 = Util.createIndexLookup(p_235168_);
      this.additionOrder = Comparator.comparingInt($$2);
      this.resourceLocationSearchTree = ResourceLocationSearchTree.create(p_235168_, p_235167_);
   }

   @Override
   public List<T> search(String p_235173_) {
      int $$1 = p_235173_.indexOf(58);
      return $$1 == -1 ? this.searchPlainText(p_235173_) : this.searchResourceLocation(p_235173_.substring(0, $$1).trim(), p_235173_.substring($$1 + 1).trim());
   }

   protected List<T> searchPlainText(String p_235169_) {
      return this.resourceLocationSearchTree.searchPath(p_235169_);
   }

   protected List<T> searchResourceLocation(String p_235170_, String p_235171_) {
      List<T> $$2 = this.resourceLocationSearchTree.searchNamespace(p_235170_);
      List<T> $$3 = this.resourceLocationSearchTree.searchPath(p_235171_);
      return ImmutableList.copyOf(new IntersectionIterator<T>($$2.iterator(), $$3.iterator(), this.additionOrder));
   }
}
