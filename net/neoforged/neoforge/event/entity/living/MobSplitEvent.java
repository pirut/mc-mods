package net.neoforged.neoforge.event.entity.living;

import java.util.List;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

public class MobSplitEvent extends Event implements ICancellableEvent {
   protected final Mob parent;
   protected final List<Mob> children;

   @Internal
   public MobSplitEvent(Mob parent, List<Mob> children) {
      this.parent = parent;
      this.children = children;
   }

   public Mob getParent() {
      return this.parent;
   }

   public List<Mob> getChildren() {
      return this.children;
   }

   public void setCanceled(boolean canceled) {
      super.setCanceled(canceled);
   }
}
