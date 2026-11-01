package net.minecraft.client.resources.model;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.List;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.QuadCollectionBuilderExtension;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class QuadCollection {
   public static final QuadCollection EMPTY = new QuadCollection(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
   private final List<BakedQuad> all;
   private final List<BakedQuad> unculled;
   private final List<BakedQuad> north;
   private final List<BakedQuad> south;
   private final List<BakedQuad> east;
   private final List<BakedQuad> west;
   private final List<BakedQuad> up;
   private final List<BakedQuad> down;

   QuadCollection(
      List<BakedQuad> p_405148_,
      List<BakedQuad> p_404733_,
      List<BakedQuad> p_405535_,
      List<BakedQuad> p_404824_,
      List<BakedQuad> p_405136_,
      List<BakedQuad> p_404979_,
      List<BakedQuad> p_404989_,
      List<BakedQuad> p_405866_
   ) {
      this.all = p_405148_;
      this.unculled = p_404733_;
      this.north = p_405535_;
      this.south = p_404824_;
      this.east = p_405136_;
      this.west = p_404979_;
      this.up = p_404989_;
      this.down = p_405866_;
   }

   public List<BakedQuad> getQuads(@Nullable Direction p_405514_) {
      return switch (p_405514_) {
         case null -> this.unculled;
         case NORTH -> this.north;
         case SOUTH -> this.south;
         case EAST -> this.east;
         case WEST -> this.west;
         case UP -> this.up;
         case DOWN -> this.down;
      };
   }

   public List<BakedQuad> getAll() {
      return this.all;
   }

   @OnlyIn(Dist.CLIENT)
   public static class Builder implements QuadCollectionBuilderExtension {
      private final com.google.common.collect.ImmutableList.Builder<BakedQuad> unculledFaces = ImmutableList.builder();
      private final Multimap<Direction, BakedQuad> culledFaces = ArrayListMultimap.create();

      public QuadCollection.Builder addCulledFace(Direction p_405416_, BakedQuad p_405678_) {
         this.culledFaces.put(p_405416_, p_405678_);
         return this;
      }

      public QuadCollection.Builder addUnculledFace(BakedQuad p_405329_) {
         this.unculledFaces.add(p_405329_);
         return this;
      }

      private static QuadCollection createFromSublists(
         List<BakedQuad> p_405742_, int p_405661_, int p_405665_, int p_405121_, int p_405725_, int p_404700_, int p_405215_, int p_404662_
      ) {
         int i = 0;
         int j;
         List<BakedQuad> list = p_405742_.subList(i, j = i + p_405661_);
         List<BakedQuad> list1 = p_405742_.subList(j, i = j + p_405665_);
         int k;
         List<BakedQuad> list2 = p_405742_.subList(i, k = i + p_405121_);
         List<BakedQuad> list3 = p_405742_.subList(k, i = k + p_405725_);
         int l;
         List<BakedQuad> list4 = p_405742_.subList(i, l = i + p_404700_);
         List<BakedQuad> list5 = p_405742_.subList(l, i = l + p_405215_);
         List<BakedQuad> list6 = p_405742_.subList(i, i + p_404662_);
         return new QuadCollection(p_405742_, list, list1, list2, list3, list4, list5, list6);
      }

      public QuadCollection build() {
         ImmutableList<BakedQuad> immutablelist = this.unculledFaces.build();
         if (this.culledFaces.isEmpty()) {
            return immutablelist.isEmpty()
               ? QuadCollection.EMPTY
               : new QuadCollection(immutablelist, immutablelist, List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
         } else {
            com.google.common.collect.ImmutableList.Builder<BakedQuad> builder = ImmutableList.builder();
            builder.addAll(immutablelist);
            Collection<BakedQuad> collection = this.culledFaces.get(Direction.NORTH);
            builder.addAll(collection);
            Collection<BakedQuad> collection1 = this.culledFaces.get(Direction.SOUTH);
            builder.addAll(collection1);
            Collection<BakedQuad> collection2 = this.culledFaces.get(Direction.EAST);
            builder.addAll(collection2);
            Collection<BakedQuad> collection3 = this.culledFaces.get(Direction.WEST);
            builder.addAll(collection3);
            Collection<BakedQuad> collection4 = this.culledFaces.get(Direction.UP);
            builder.addAll(collection4);
            Collection<BakedQuad> collection5 = this.culledFaces.get(Direction.DOWN);
            builder.addAll(collection5);
            return createFromSublists(
               builder.build(),
               immutablelist.size(),
               collection.size(),
               collection1.size(),
               collection2.size(),
               collection3.size(),
               collection4.size(),
               collection5.size()
            );
         }
      }
   }
}
