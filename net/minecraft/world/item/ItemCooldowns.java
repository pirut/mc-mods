package net.minecraft.world.item;

import com.google.common.collect.Maps;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.component.UseCooldown;

public class ItemCooldowns {
   private final Map<Identifier, ItemCooldowns.CooldownInstance> cooldowns = Maps.newHashMap();
   private int tickCount;

   public boolean isOnCooldown(ItemStack p_366432_) {
      return this.getCooldownPercent(p_366432_, 0.0F) > 0.0F;
   }

   public float getCooldownPercent(ItemStack p_366443_, float p_41523_) {
      Identifier identifier = this.getCooldownGroup(p_366443_);
      ItemCooldowns.CooldownInstance itemcooldowns$cooldowninstance = this.cooldowns.get(identifier);
      if (itemcooldowns$cooldowninstance != null) {
         float f = itemcooldowns$cooldowninstance.endTime - itemcooldowns$cooldowninstance.startTime;
         float f1 = itemcooldowns$cooldowninstance.endTime - (this.tickCount + p_41523_);
         return Mth.clamp(f1 / f, 0.0F, 1.0F);
      } else {
         return 0.0F;
      }
   }

   public void tick() {
      this.tickCount++;
      if (!this.cooldowns.isEmpty()) {
         Iterator<Entry<Identifier, ItemCooldowns.CooldownInstance>> iterator = this.cooldowns.entrySet().iterator();

         while (iterator.hasNext()) {
            Entry<Identifier, ItemCooldowns.CooldownInstance> entry = iterator.next();
            if (entry.getValue().endTime <= this.tickCount) {
               iterator.remove();
               this.onCooldownEnded(entry.getKey());
            }
         }
      }
   }

   public Identifier getCooldownGroup(ItemStack p_366444_) {
      UseCooldown usecooldown = p_366444_.get(DataComponents.USE_COOLDOWN);
      Identifier identifier = BuiltInRegistries.ITEM.getKey(p_366444_.getItem());
      return usecooldown == null ? identifier : usecooldown.cooldownGroup().orElse(identifier);
   }

   public void addCooldown(ItemStack p_366762_, int p_41526_) {
      this.addCooldown(this.getCooldownGroup(p_366762_), p_41526_);
   }

   public void addCooldown(Identifier p_466853_, int p_366819_) {
      this.cooldowns.put(p_466853_, new ItemCooldowns.CooldownInstance(this.tickCount, this.tickCount + p_366819_));
      this.onCooldownStarted(p_466853_, p_366819_);
   }

   public void removeCooldown(Identifier p_469616_) {
      this.cooldowns.remove(p_469616_);
      this.onCooldownEnded(p_469616_);
   }

   protected void onCooldownStarted(Identifier p_467032_, int p_41530_) {
   }

   protected void onCooldownEnded(Identifier p_469860_) {
   }

   record CooldownInstance(int startTime, int endTime) {
   }
}
