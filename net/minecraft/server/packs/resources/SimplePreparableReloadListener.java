package net.minecraft.server.packs.resources;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.resource.ContextAwareReloadListener;

public abstract class SimplePreparableReloadListener<T> extends ContextAwareReloadListener implements PreparableReloadListener {
   @Override
   public final CompletableFuture<Void> reload(
      PreparableReloadListener.PreparationBarrier p_10780_,
      ResourceManager p_10781_,
      ProfilerFiller p_10782_,
      ProfilerFiller p_10783_,
      Executor p_10784_,
      Executor p_10785_
   ) {
      return CompletableFuture.<T>supplyAsync(() -> this.prepare(p_10781_, p_10782_), p_10784_)
         .thenCompose(p_10780_::wait)
         .thenAcceptAsync(p_10792_ -> this.apply((T)p_10792_, p_10781_, p_10783_), p_10785_);
   }

   protected abstract T prepare(ResourceManager var1, ProfilerFiller var2);

   protected abstract void apply(T var1, ResourceManager var2, ProfilerFiller var3);
}
