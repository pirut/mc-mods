package net.neoforged.neoforge.entity;

import java.util.Optional;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import org.jspecify.annotations.Nullable;

public class XpOrbTargetingEvent extends Event {
   private final ExperienceOrb xpOrb;
   private final double scanDistance;
   @Nullable
   private Optional<Player> followingPlayer = null;

   public XpOrbTargetingEvent(ExperienceOrb xpOrb, double scanDistance) {
      this.xpOrb = xpOrb;
      this.scanDistance = scanDistance;
   }

   @Nullable
   public Player getFollowingPlayer() {
      return this.followingPlayer != null ? this.followingPlayer.orElse(null) : this.xpOrb.level().getNearestPlayer(this.xpOrb, this.scanDistance);
   }

   public void setFollowingPlayer(@Nullable Player newFollowingPlayer) {
      this.followingPlayer = Optional.ofNullable(newFollowingPlayer);
   }

   public ExperienceOrb getXpOrb() {
      return this.xpOrb;
   }

   public double getScanDistance() {
      return this.scanDistance;
   }
}
