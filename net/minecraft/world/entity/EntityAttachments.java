package net.minecraft.world.entity;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class EntityAttachments {
   private final Map<EntityAttachment, List<Vec3>> attachments;

   EntityAttachments(Map<EntityAttachment, List<Vec3>> p_316675_) {
      this.attachments = p_316675_;
   }

   public static EntityAttachments createDefault(float p_316906_, float p_316905_) {
      return builder().build(p_316906_, p_316905_);
   }

   public static EntityAttachments.Builder builder() {
      return new EntityAttachments.Builder();
   }

   public EntityAttachments scale(float p_316378_, float p_316683_, float p_316463_) {
      Map<EntityAttachment, List<Vec3>> $$3 = new EnumMap<>(EntityAttachment.class);

      for (Entry<EntityAttachment, List<Vec3>> $$4 : this.attachments.entrySet()) {
         $$3.put($$4.getKey(), scalePoints($$4.getValue(), p_316378_, p_316683_, p_316463_));
      }

      return new EntityAttachments($$3);
   }

   private static List<Vec3> scalePoints(List<Vec3> p_316879_, float p_316759_, float p_316371_, float p_316711_) {
      List<Vec3> $$4 = new ArrayList<>(p_316879_.size());

      for (Vec3 $$5 : p_316879_) {
         $$4.add($$5.multiply(p_316759_, p_316371_, p_316711_));
      }

      return $$4;
   }

   @Nullable
   public Vec3 getNullable(EntityAttachment p_316263_, int p_316709_, float p_316113_) {
      List<Vec3> $$3 = this.attachments.get(p_316263_);
      return p_316709_ >= 0 && p_316709_ < $$3.size() ? transformPoint($$3.get(p_316709_), p_316113_) : null;
   }

   public Vec3 get(EntityAttachment p_316290_, int p_316517_, float p_316296_) {
      Vec3 $$3 = this.getNullable(p_316290_, p_316517_, p_316296_);
      if ($$3 == null) {
         throw new IllegalStateException("Had no attachment point of type: " + p_316290_ + " for index: " + p_316517_);
      } else {
         return $$3;
      }
   }

   public Vec3 getClamped(EntityAttachment p_316117_, int p_316379_, float p_316900_) {
      List<Vec3> $$3 = this.attachments.get(p_316117_);
      if ($$3.isEmpty()) {
         throw new IllegalStateException("Had no attachment points of type: " + p_316117_);
      } else {
         Vec3 $$4 = $$3.get(Mth.clamp(p_316379_, 0, $$3.size() - 1));
         return transformPoint($$4, p_316900_);
      }
   }

   private static Vec3 transformPoint(Vec3 p_316742_, float p_316708_) {
      return p_316742_.yRot(-p_316708_ * (float) (Math.PI / 180.0));
   }

   public static class Builder {
      private final Map<EntityAttachment, List<Vec3>> attachments = new EnumMap<>(EntityAttachment.class);

      Builder() {
      }

      public EntityAttachments.Builder attach(EntityAttachment p_316395_, float p_316627_, float p_316510_, float p_316313_) {
         return this.attach(p_316395_, new Vec3(p_316627_, p_316510_, p_316313_));
      }

      public EntityAttachments.Builder attach(EntityAttachment p_316903_, Vec3 p_316337_) {
         this.attachments.computeIfAbsent(p_316903_, p_316616_ -> new ArrayList<>(1)).add(p_316337_);
         return this;
      }

      public EntityAttachments build(float p_316746_, float p_316254_) {
         Map<EntityAttachment, List<Vec3>> $$2 = new EnumMap<>(EntityAttachment.class);

         for (EntityAttachment $$3 : EntityAttachment.values()) {
            List<Vec3> $$4 = this.attachments.get($$3);
            $$2.put($$3, $$4 != null ? List.copyOf($$4) : $$3.createFallbackPoints(p_316746_, p_316254_));
         }

         return new EntityAttachments($$2);
      }
   }
}
