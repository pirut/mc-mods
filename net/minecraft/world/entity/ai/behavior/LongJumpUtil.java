package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;

public final class LongJumpUtil {
   public static Optional<Vec3> calculateJumpVectorForAngle(Mob p_311799_, Vec3 p_312876_, float p_312407_, int p_311991_, boolean p_312784_) {
      Vec3 $$5 = p_311799_.position();
      Vec3 $$6 = new Vec3(p_312876_.x - $$5.x, 0.0, p_312876_.z - $$5.z).normalize().scale(0.5);
      Vec3 $$7 = p_312876_.subtract($$6);
      Vec3 $$8 = $$7.subtract($$5);
      float $$9 = p_311991_ * (float) Math.PI / 180.0F;
      double $$10 = Math.atan2($$8.z, $$8.x);
      double $$11 = $$8.subtract(0.0, $$8.y, 0.0).lengthSqr();
      double $$12 = Math.sqrt($$11);
      double $$13 = $$8.y;
      double $$14 = p_311799_.getGravity();
      double $$15 = Math.sin(2.0F * $$9);
      double $$16 = Math.pow(Math.cos($$9), 2.0);
      double $$17 = Math.sin($$9);
      double $$18 = Math.cos($$9);
      double $$19 = Math.sin($$10);
      double $$20 = Math.cos($$10);
      double $$21 = $$11 * $$14 / ($$12 * $$15 - 2.0 * $$13 * $$16);
      if ($$21 < 0.0) {
         return Optional.empty();
      } else {
         double $$22 = Math.sqrt($$21);
         if ($$22 > p_312407_) {
            return Optional.empty();
         } else {
            double $$23 = $$22 * $$18;
            double $$24 = $$22 * $$17;
            if (p_312784_) {
               int $$25 = Mth.ceil($$12 / $$23) * 2;
               double $$26 = 0.0;
               Vec3 $$27 = null;
               EntityDimensions $$28 = p_311799_.getDimensions(Pose.LONG_JUMPING);

               for (int $$29 = 0; $$29 < $$25 - 1; $$29++) {
                  $$26 += $$12 / $$25;
                  double $$30 = $$17 / $$18 * $$26 - Math.pow($$26, 2.0) * $$14 / (2.0 * $$21 * Math.pow($$18, 2.0));
                  double $$31 = $$26 * $$20;
                  double $$32 = $$26 * $$19;
                  Vec3 $$33 = new Vec3($$5.x + $$31, $$5.y + $$30, $$5.z + $$32);
                  if ($$27 != null && !isClearTransition(p_311799_, $$28, $$27, $$33)) {
                     return Optional.empty();
                  }

                  $$27 = $$33;
               }
            }

            return Optional.of(new Vec3($$23 * $$20, $$24, $$23 * $$19).scale(0.95F));
         }
      }
   }

   private static boolean isClearTransition(Mob p_312910_, EntityDimensions p_312734_, Vec3 p_311995_, Vec3 p_312896_) {
      Vec3 $$4 = p_312896_.subtract(p_311995_);
      double $$5 = Math.min(p_312734_.width(), p_312734_.height());
      int $$6 = Mth.ceil($$4.length() / $$5);
      Vec3 $$7 = $$4.normalize();
      Vec3 $$8 = p_311995_;

      for (int $$9 = 0; $$9 < $$6; $$9++) {
         $$8 = $$9 == $$6 - 1 ? p_312896_ : $$8.add($$7.scale($$5 * 0.9F));
         if (!p_312910_.level().noCollision(p_312910_, p_312734_.makeBoundingBox($$8))) {
            return false;
         }
      }

      return true;
   }
}
