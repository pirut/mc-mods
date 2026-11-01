package net.minecraft.commands.arguments.coordinates;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class WorldCoordinates implements Coordinates {
   private final WorldCoordinate x;
   private final WorldCoordinate y;
   private final WorldCoordinate z;

   public WorldCoordinates(WorldCoordinate p_120883_, WorldCoordinate p_120884_, WorldCoordinate p_120885_) {
      this.x = p_120883_;
      this.y = p_120884_;
      this.z = p_120885_;
   }

   @Override
   public Vec3 getPosition(CommandSourceStack p_120893_) {
      Vec3 $$1 = p_120893_.getPosition();
      return new Vec3(this.x.get($$1.x), this.y.get($$1.y), this.z.get($$1.z));
   }

   @Override
   public Vec2 getRotation(CommandSourceStack p_120896_) {
      Vec2 $$1 = p_120896_.getRotation();
      return new Vec2((float)this.x.get($$1.x), (float)this.y.get($$1.y));
   }

   @Override
   public boolean isXRelative() {
      return this.x.isRelative();
   }

   @Override
   public boolean isYRelative() {
      return this.y.isRelative();
   }

   @Override
   public boolean isZRelative() {
      return this.z.isRelative();
   }

   @Override
   public boolean equals(Object p_120900_) {
      if (this == p_120900_) {
         return true;
      } else if (!(p_120900_ instanceof WorldCoordinates $$1)) {
         return false;
      } else if (!this.x.equals($$1.x)) {
         return false;
      } else {
         return !this.y.equals($$1.y) ? false : this.z.equals($$1.z);
      }
   }

   public static WorldCoordinates parseInt(StringReader p_120888_) throws CommandSyntaxException {
      int $$1 = p_120888_.getCursor();
      WorldCoordinate $$2 = WorldCoordinate.parseInt(p_120888_);
      if (p_120888_.canRead() && p_120888_.peek() == ' ') {
         p_120888_.skip();
         WorldCoordinate $$3 = WorldCoordinate.parseInt(p_120888_);
         if (p_120888_.canRead() && p_120888_.peek() == ' ') {
            p_120888_.skip();
            WorldCoordinate $$4 = WorldCoordinate.parseInt(p_120888_);
            return new WorldCoordinates($$2, $$3, $$4);
         } else {
            p_120888_.setCursor($$1);
            throw Vec3Argument.ERROR_NOT_COMPLETE.createWithContext(p_120888_);
         }
      } else {
         p_120888_.setCursor($$1);
         throw Vec3Argument.ERROR_NOT_COMPLETE.createWithContext(p_120888_);
      }
   }

   public static WorldCoordinates parseDouble(StringReader p_120890_, boolean p_120891_) throws CommandSyntaxException {
      int $$2 = p_120890_.getCursor();
      WorldCoordinate $$3 = WorldCoordinate.parseDouble(p_120890_, p_120891_);
      if (p_120890_.canRead() && p_120890_.peek() == ' ') {
         p_120890_.skip();
         WorldCoordinate $$4 = WorldCoordinate.parseDouble(p_120890_, false);
         if (p_120890_.canRead() && p_120890_.peek() == ' ') {
            p_120890_.skip();
            WorldCoordinate $$5 = WorldCoordinate.parseDouble(p_120890_, p_120891_);
            return new WorldCoordinates($$3, $$4, $$5);
         } else {
            p_120890_.setCursor($$2);
            throw Vec3Argument.ERROR_NOT_COMPLETE.createWithContext(p_120890_);
         }
      } else {
         p_120890_.setCursor($$2);
         throw Vec3Argument.ERROR_NOT_COMPLETE.createWithContext(p_120890_);
      }
   }

   public static WorldCoordinates absolute(double p_175086_, double p_175087_, double p_175088_) {
      return new WorldCoordinates(new WorldCoordinate(false, p_175086_), new WorldCoordinate(false, p_175087_), new WorldCoordinate(false, p_175088_));
   }

   public static WorldCoordinates absolute(Vec2 p_175090_) {
      return new WorldCoordinates(new WorldCoordinate(false, p_175090_.x), new WorldCoordinate(false, p_175090_.y), new WorldCoordinate(true, 0.0));
   }

   public static WorldCoordinates current() {
      return new WorldCoordinates(new WorldCoordinate(true, 0.0), new WorldCoordinate(true, 0.0), new WorldCoordinate(true, 0.0));
   }

   @Override
   public int hashCode() {
      int $$0 = this.x.hashCode();
      $$0 = 31 * $$0 + this.y.hashCode();
      return 31 * $$0 + this.z.hashCode();
   }
}
