package net.neoforged.neoforge.event;

import com.google.common.graph.ElementOrder;
import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.ApiStatus.Internal;

public abstract class SortedReloadListenerEvent extends Event {
   private final Map<Identifier, PreparableReloadListener> registry = new LinkedHashMap<>();
   private final Map<PreparableReloadListener, Identifier> keys = new IdentityHashMap<>();
   private final MutableGraph<PreparableReloadListener> graph = GraphBuilder.directed().nodeOrder(ElementOrder.insertion()).build();
   private final PreparableReloadListener lastVanilla;

   @Internal
   protected SortedReloadListenerEvent(List<PreparableReloadListener> vanillaListeners, SortedReloadListenerEvent.NameLookup lookup) {
      for (PreparableReloadListener listener : vanillaListeners) {
         Identifier key = lookup.apply(listener);
         this.addListener(key, listener);
      }

      for (int i = 1; i < vanillaListeners.size(); i++) {
         PreparableReloadListener prev = vanillaListeners.get(i - 1);
         PreparableReloadListener listener = vanillaListeners.get(i);
         this.graph.putEdge(prev, listener);
      }

      this.lastVanilla = vanillaListeners.getLast();
   }

   public void addListener(Identifier key, PreparableReloadListener listener) {
      if (!this.registry.containsKey(key) && !this.registry.containsValue(listener)) {
         this.registry.put(key, listener);
         this.keys.put(listener, key);
         this.graph.addNode(listener);
      } else {
         throw new IllegalArgumentException("Attempted to register two reload listeners for the same key: " + key);
      }
   }

   public void addDependency(Identifier first, Identifier second) {
      this.graph.putEdge(this.getOrThrow(first), this.getOrThrow(second));
   }

   public Graph<PreparableReloadListener> getGraph() {
      return this.graph;
   }

   public Map<Identifier, PreparableReloadListener> getRegistry() {
      return Collections.unmodifiableMap(this.registry);
   }

   public SortedReloadListenerEvent.NameLookup getNameLookup() {
      return this::getOrThrow;
   }

   @Internal
   public PreparableReloadListener getLastVanillaListener() {
      return this.lastVanilla;
   }

   private PreparableReloadListener getOrThrow(Identifier key) {
      PreparableReloadListener listener = this.registry.get(key);
      if (listener == null) {
         throw new IllegalArgumentException("Unknown reload listener: " + key);
      } else {
         return listener;
      }
   }

   private Identifier getOrThrow(PreparableReloadListener listener) {
      Identifier key = this.keys.get(listener);
      if (key == null) {
         throw new IllegalArgumentException("Unknown reload listener: " + listener);
      } else {
         return key;
      }
   }

   @FunctionalInterface
   public interface NameLookup extends Function<PreparableReloadListener, Identifier> {
      Identifier apply(PreparableReloadListener var1);
   }
}
