package net.neoforged.neoforge.client.event;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.ApiStatus.Internal;

public class ComputeFovModifierEvent extends Event {
   private final Player player;
   private final float fovModifier;
   private final float fovScale;
   private float newFovModifier;

   @Internal
   public ComputeFovModifierEvent(Player player, float fovModifier, float fovScale) {
      this.player = player;
      this.fovModifier = fovModifier;
      this.fovScale = fovScale;
      this.setNewFovModifier(Mth.lerp(fovScale, 1.0F, fovModifier));
   }

   public Player getPlayer() {
      return this.player;
   }

   public float getFovModifier() {
      return this.fovModifier;
   }

   public float getFovScale() {
      return this.fovScale;
   }

   public float getNewFovModifier() {
      return this.newFovModifier;
   }

   public void setNewFovModifier(float newFovModifier) {
      this.newFovModifier = newFovModifier;
   }
}
