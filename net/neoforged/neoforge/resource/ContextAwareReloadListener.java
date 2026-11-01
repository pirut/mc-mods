package net.neoforged.neoforge.resource;

import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.neoforged.neoforge.common.conditions.ConditionalOps;
import net.neoforged.neoforge.common.conditions.ICondition;
import org.jetbrains.annotations.ApiStatus.Internal;

public abstract class ContextAwareReloadListener implements PreparableReloadListener {
   private ICondition.IContext conditionContext = ICondition.IContext.EMPTY;
   private HolderLookup.Provider registryLookup = RegistryAccess.EMPTY;

   @Internal
   public void injectContext(ICondition.IContext context, HolderLookup.Provider registryLookup) {
      this.conditionContext = context;
      this.registryLookup = registryLookup;
   }

   protected final ICondition.IContext getContext() {
      return this.conditionContext;
   }

   protected final HolderLookup.Provider getRegistryLookup() {
      return this.registryLookup;
   }

   protected final ConditionalOps<JsonElement> makeConditionalOps() {
      return new ConditionalOps<>(this.getRegistryLookup().createSerializationContext(JsonOps.INSTANCE), this.getContext());
   }

   protected final ConditionalOps<JsonElement> makeConditionalOps(DynamicOps<JsonElement> ops) {
      if (ops instanceof ConditionalOps<JsonElement> conditionalOps) {
         return conditionalOps;
      } else {
         return ops instanceof RegistryOps<JsonElement> registryOps ? new ConditionalOps<>(registryOps, this.getContext()) : this.makeConditionalOps();
      }
   }
}
