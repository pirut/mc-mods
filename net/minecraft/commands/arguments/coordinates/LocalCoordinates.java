package net.minecraft.commands.arguments.coordinates;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Objects;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class LocalCoordinates implements Coordinates {
   public static final char PREFIX_LOCAL_COORDINATE = '^';
   private final double left;
   private final double up;
   private final double forwards;

   public LocalCoordinates(double p_119902_, double p_119903_, double p_119904_) {
      this.left = p_119902_;
      this.up = p_119903_;
      this.forwards = p_119904_;
   }

   @Override
   public Vec3 getPosition(CommandSourceStack p_119912_) {
      Vec2 $$1 = p_119912_.getRotation();
      Vec3 $$2 = p_119912_.getAnchor().apply(p_119912_);
      float $$3 = Mth.cos(($$1.y + 90.0F) * (float) (Math.PI / 180.0));
      float $$4 = Mth.sin(($$1.y + 90.0F) * (float) (Math.PI / 180.0));
      float $$5 = Mth.cos(-$$1.x * (float) (Math.PI / 180.0));
      float $$6 = Mth.sin(-$$1.x * (float) (Math.PI / 180.0));
      float $$7 = Mth.cos((-$$1.x + 90.0F) * (float) (Math.PI / 180.0));
      float $$8 = Mth.sin((-$$1.x + 90.0F) * (float) (Math.PI / 180.0));
      Vec3 $$9 = new Vec3($$3 * $$5, $$6, $$4 * $$5);
      Vec3 $$10 = new Vec3($$3 * $$7, $$8, $$4 * $$7);
      Vec3 $$11 = $$9.cross($$10).scale(-1.0);
      double $$12 = $$9.x * this.forwards + $$10.x * this.up + $$11.x * this.left;
      double $$13 = $$9.y * this.forwards + $$10.y * this.up + $$11.y * this.left;
      double $$14 = $$9.z * this.forwards + $$10.z * this.up + $$11.z * this.left;
      return new Vec3($$2.x + $$12, $$2.y + $$13, $$2.z + $$14);
   }

   @Override
   public Vec2 getRotation(CommandSourceStack p_119915_) {
      return Vec2.ZERO;
   }

   @Override
   public boolean isXRelative() {
      return true;
   }

   @Override
   public boolean isYRelative() {
      return true;
   }

   @Override
   public boolean isZRelative() {
      return true;
   }

   public static LocalCoordinates parse(StringReader p_119907_) throws CommandSyntaxException {
      int $$1 = p_119907_.getCursor();
      double $$2 = readDouble(p_119907_, $$1);
      if (p_119907_.canRead() && p_119907_.peek() == ' ') {
         p_119907_.skip();
         double $$3 = readDouble(p_119907_, $$1);
         if (p_119907_.canRead() && p_119907_.peek() == ' ') {
            p_119907_.skip();
            double $$4 = readDouble(p_119907_, $$1);
            return new LocalCoordinates($$2, $$3, $$4);
         } else {
            p_119907_.setCursor($$1);
            throw Vec3Argument.ERROR_NOT_COMPLETE.createWithContext(p_119907_);
         }
      } else {
         p_119907_.setCursor($$1);
         throw Vec3Argument.ERROR_NOT_COMPLETE.createWithContext(p_119907_);
      }
   }

   private static double readDouble(StringReader p_119909_, int p_119910_) throws CommandSyntaxException {
      if (!p_119909_.canRead()) {
         throw WorldCoordinate.ERROR_EXPECTED_DOUBLE.createWithContext(p_119909_);
      } else if (p_119909_.peek() != '^') {
         p_119909_.setCursor(p_119910_);
         throw Vec3Argument.ERROR_MIXED_TYPE.createWithContext(p_119909_);
      } else {
         p_119909_.skip();
         return p_119909_.canRead() && p_119909_.peek() != ' ' ? p_119909_.readDouble() : 0.0;
      }
   }

   @Override
   public boolean equals(Object p_119918_) {
      if (this == p_119918_) {
         return true;
      } else {
         return !(p_119918_ instanceof LocalCoordinates $$1) ? false : this.left == $$1.left && this.up == $$1.up && this.forwards == $$1.forwards;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.left, this.up, this.forwards);
   }
}
