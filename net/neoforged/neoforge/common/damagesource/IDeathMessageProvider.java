package net.neoforged.neoforge.common.damagesource;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.world.damagesource.CombatEntry;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DeathMessageType;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public interface IDeathMessageProvider {
   IDeathMessageProvider DEFAULT = (entity, lastEntry, sigFall) -> {
      DamageSource dmgSrc = lastEntry.source();
      DeathMessageType msgType = dmgSrc.type().deathMessageType();
      if (msgType == DeathMessageType.FALL_VARIANTS && sigFall != null) {
         return entity.getCombatTracker().getFallMessage(sigFall, dmgSrc.getEntity());
      } else if (msgType == DeathMessageType.INTENTIONAL_GAME_DESIGN) {
         String s = "death.attack." + dmgSrc.getMsgId();
         Component component = ComponentUtils.wrapInSquareBrackets(Component.translatable(s + ".link")).withStyle(CombatTracker.INTENTIONAL_GAME_DESIGN_STYLE);
         return Component.translatable(s + ".message", entity.getDisplayName(), component);
      } else {
         return dmgSrc.getLocalizedDeathMessage(entity);
      }
   };

   Component getDeathMessage(LivingEntity var1, CombatEntry var2, @Nullable CombatEntry var3);
}
