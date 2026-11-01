package net.neoforged.neoforge.server.command.generation;

import java.util.Iterator;
import java.util.NoSuchElementException;
import net.minecraft.world.level.ChunkPos;

public class OnionIterator implements Iterator<ChunkPos> {
   private static final byte EAST = 0;
   private static final byte SOUTH = 1;
   private static final byte WEST = 2;
   private static final byte NORTH = 3;
   private static final byte STOP = 4;
   private final int radius;
   private int x;
   private int z;
   private int distance = 0;
   private byte state = 0;

   public OnionIterator(int radius) {
      this.radius = radius;
   }

   public ChunkPos next() {
      if (!this.hasNext()) {
         throw new NoSuchElementException();
      } else {
         ChunkPos pos = new ChunkPos(this.x, this.z);
         switch (this.state) {
            case 0:
               if (++this.x >= this.distance) {
                  this.state = 1;
                  if (this.distance > this.radius) {
                     this.state = 4;
                  }
               }
               break;
            case 1:
               if (++this.z >= this.distance) {
                  this.state = 2;
               }
               break;
            case 2:
               if (--this.x <= -this.distance) {
                  this.state = 3;
               }
               break;
            case 3:
               if (--this.z <= -this.distance) {
                  this.state = 0;
                  this.distance++;
               }
         }

         if (this.distance == 0) {
            this.distance++;
         }

         return pos;
      }
   }

   @Override
   public boolean hasNext() {
      return this.state != 4;
   }
}
