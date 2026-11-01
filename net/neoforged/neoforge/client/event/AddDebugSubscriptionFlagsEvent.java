package net.neoforged.neoforge.client.event;

import it.unimi.dsi.fastutil.objects.ObjectBooleanBiConsumer;
import java.util.function.Supplier;
import net.minecraft.util.debug.DebugSubscription;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.ApiStatus.Internal;

public final class AddDebugSubscriptionFlagsEvent extends Event {
   private final ObjectBooleanBiConsumer<DebugSubscription<?>> registrar;

   @Internal
   public AddDebugSubscriptionFlagsEvent(ObjectBooleanBiConsumer<DebugSubscription<?>> registrar) {
      this.registrar = registrar;
   }

   public void addFlag(DebugSubscription<?> subscription, boolean flag) {
      this.registrar.accept(subscription, flag);
   }

   public void addFlag(Supplier<? extends DebugSubscription<?>> subscription, boolean flag) {
      this.addFlag((DebugSubscription<?>)subscription.get(), flag);
   }

   public void addActiveInDev(DebugSubscription<?> subscription) {
      this.addFlag(subscription, !FMLEnvironment.isProduction());
   }

   public void addActiveInDev(Supplier<? extends DebugSubscription<?>> subscription) {
      this.addActiveInDev((DebugSubscription<?>)subscription.get());
   }
}
