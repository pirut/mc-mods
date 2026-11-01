package net.neoforged.neoforge.common.damagesource;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import net.minecraft.world.damagesource.DamageSource;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

public class DamageContainer {
   private final EnumMap<DamageContainer.Reduction, List<IReductionFunction>> reductionFunctions = new EnumMap<>(DamageContainer.Reduction.class);
   private final float originalDamage;
   private final DamageSource source;
   private float newDamage;
   private final EnumMap<DamageContainer.Reduction, Float> reductions = new EnumMap<>(DamageContainer.Reduction.class);
   private float blockedDamage = 0.0F;
   private float shieldDamage = 0.0F;
   private int invulnerabilityTicksAfterAttack = 20;

   public DamageContainer(DamageSource source, float originalDamage) {
      this.source = source;
      this.originalDamage = originalDamage;
      this.newDamage = originalDamage;
   }

   public float getOriginalDamage() {
      return this.originalDamage;
   }

   public DamageSource getSource() {
      return this.source;
   }

   public void setNewDamage(float damage) {
      this.newDamage = damage;
   }

   public float getNewDamage() {
      return this.newDamage;
   }

   public void addModifier(DamageContainer.Reduction type, IReductionFunction reductionFunction) {
      this.reductionFunctions.computeIfAbsent(type, a -> new ArrayList<>()).add(reductionFunction);
   }

   public float getBlockedDamage() {
      return this.blockedDamage;
   }

   public float getShieldDamage() {
      return this.shieldDamage;
   }

   public void setPostAttackInvulnerabilityTicks(int ticks) {
      this.invulnerabilityTicksAfterAttack = ticks;
   }

   public int getPostAttackInvulnerabilityTicks() {
      return this.invulnerabilityTicksAfterAttack;
   }

   public float getReduction(DamageContainer.Reduction type) {
      return this.reductions.getOrDefault(type, 0.0F);
   }

   @Internal
   public void setBlockedDamage(LivingShieldBlockEvent event) {
      if (event.getBlocked()) {
         this.blockedDamage = event.getBlockedDamage();
         this.shieldDamage = event.shieldDamage();
         this.newDamage = this.newDamage - this.blockedDamage;
      }
   }

   @Internal
   public void setReduction(DamageContainer.Reduction reduction, float amount) {
      float modifiedReduction = this.modifyReduction(reduction, amount);
      this.reductions.put(reduction, modifiedReduction);
      this.newDamage -= modifiedReduction;
   }

   private float modifyReduction(DamageContainer.Reduction type, float reduction) {
      for (IReductionFunction func : this.reductionFunctions.getOrDefault(type, List.of())) {
         reduction = func.modify(this, reduction);
      }

      return reduction;
   }

   public static enum Reduction {
      INVULNERABILITY,
      ARMOR,
      ENCHANTMENTS,
      MOB_EFFECTS,
      ABSORPTION,
      INNATE_RESISTANCE;
   }
}
