package net.minecraft.client.resources.model;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
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
      List<BakedQuad> all,
      List<BakedQuad> unculled,
      List<BakedQuad> north,
      List<BakedQuad> south,
      List<BakedQuad> east,
      List<BakedQuad> west,
      List<BakedQuad> up,
      List<BakedQuad> down
   ) {
      this.all = all;
      this.unculled = unculled;
      this.north = north;
      this.south = south;
      this.east = east;
      this.west = west;
      this.up = up;
      this.down = down;
   }

   public List<BakedQuad> getQuads(@Nullable Direction direction) {
      return switch (direction) {
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

   @Environment(EnvType.CLIENT)
   public static class Builder {
      private final com.google.common.collect.ImmutableList.Builder<BakedQuad> unculledFaces = ImmutableList.builder();
      private final Multimap<Direction, BakedQuad> culledFaces = ArrayListMultimap.create();

      public QuadCollection.Builder addCulledFace(Direction direction, BakedQuad quad) {
         this.culledFaces.put(direction, quad);
         return this;
      }

      public QuadCollection.Builder addUnculledFace(BakedQuad quad) {
         this.unculledFaces.add(quad);
         return this;
      }

      private static QuadCollection createFromSublists(
         List<BakedQuad> quads, int unculledSize, int northSize, int southSize, int eastSize, int westSize, int upSize, int downSize
      ) {
         int i = 0;
         int var16;
         List<BakedQuad> list = quads.subList(i, var16 = i + unculledSize);
         List<BakedQuad> list2 = quads.subList(var16, i = var16 + northSize);
         int var18;
         List<BakedQuad> list3 = quads.subList(i, var18 = i + southSize);
         List<BakedQuad> list4 = quads.subList(var18, i = var18 + eastSize);
         int var20;
         List<BakedQuad> list5 = quads.subList(i, var20 = i + westSize);
         List<BakedQuad> list6 = quads.subList(var20, i = var20 + upSize);
         List<BakedQuad> list7 = quads.subList(i, i + downSize);
         return new QuadCollection(quads, list, list2, list3, list4, list5, list6, list7);
      }

      public QuadCollection build() {
         ImmutableList<BakedQuad> immutableList = this.unculledFaces.build();
         if (this.culledFaces.isEmpty()) {
            return immutableList.isEmpty()
               ? QuadCollection.EMPTY
               : new QuadCollection(immutableList, immutableList, List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
         } else {
            com.google.common.collect.ImmutableList.Builder<BakedQuad> builder = ImmutableList.builder();
            builder.addAll(immutableList);
            Collection<BakedQuad> collection = this.culledFaces.get(Direction.NORTH);
            builder.addAll(collection);
            Collection<BakedQuad> collection2 = this.culledFaces.get(Direction.SOUTH);
            builder.addAll(collection2);
            Collection<BakedQuad> collection3 = this.culledFaces.get(Direction.EAST);
            builder.addAll(collection3);
            Collection<BakedQuad> collection4 = this.culledFaces.get(Direction.WEST);
            builder.addAll(collection4);
            Collection<BakedQuad> collection5 = this.culledFaces.get(Direction.UP);
            builder.addAll(collection5);
            Collection<BakedQuad> collection6 = this.culledFaces.get(Direction.DOWN);
            builder.addAll(collection6);
            return createFromSublists(
               builder.build(),
               immutableList.size(),
               collection.size(),
               collection2.size(),
               collection3.size(),
               collection4.size(),
               collection5.size(),
               collection6.size()
            );
         }
      }
   }
}
