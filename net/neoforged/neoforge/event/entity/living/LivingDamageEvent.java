package net.neoforged.neoforge.event.entity.living;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.stream.Collectors;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.damagesource.DamageContainer;

public abstract class LivingDamageEvent extends LivingEvent {
   private LivingDamageEvent(LivingEntity entity) {
      super(entity);
   }

   public static class Post extends LivingDamageEvent {
      private final float originalDamage;
      private final DamageSource source;
      private final float newDamage;
      private final float blockedDamage;
      private final float shieldDamage;
      private final int postAttackInvulnerabilityTicks;
      private final EnumMap<DamageContainer.Reduction, Float> reductions;

      public Post(LivingEntity entity, DamageContainer container) {
         super(entity);
         this.originalDamage = container.getOriginalDamage();
         this.source = container.getSource();
         this.newDamage = container.getNewDamage();
         this.blockedDamage = container.getBlockedDamage();
         this.shieldDamage = container.getShieldDamage();
         this.postAttackInvulnerabilityTicks = container.getPostAttackInvulnerabilityTicks();
         this.reductions = new EnumMap<>(
            Arrays.stream(DamageContainer.Reduction.values())
               .map(type -> new SimpleEntry<>(type, container.getReduction(type)))
               .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue))
         );
      }

      public float getOriginalDamage() {
         return this.originalDamage;
      }

      public DamageSource getSource() {
         return this.source;
      }

      public float getNewDamage() {
         return this.newDamage;
      }

      public float getBlockedDamage() {
         return this.blockedDamage;
      }

      public float getShieldDamage() {
         return this.shieldDamage;
      }

      public int getPostAttackInvulnerabilityTicks() {
         return this.postAttackInvulnerabilityTicks;
      }

      public float getReduction(DamageContainer.Reduction reduction) {
         return this.reductions.getOrDefault(reduction, 0.0F);
      }
   }

   public static class Pre extends LivingDamageEvent {
      private final DamageContainer container;

      public Pre(LivingEntity entity, DamageContainer container) {
         super(entity);
         this.container = container;
      }

      public DamageContainer getContainer() {
         return this.container;
      }

      public DamageSource getSource() {
         return this.container.getSource();
      }

      public float getNewDamage() {
         return this.container.getNewDamage();
      }

      public float getOriginalDamage() {
         return this.container.getOriginalDamage();
      }

      public void setNewDamage(float newDamage) {
         this.container.setNewDamage(newDamage);
      }
   }
}
