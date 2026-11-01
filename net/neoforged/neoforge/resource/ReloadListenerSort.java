package net.neoforged.neoforge.resource;

import com.google.common.graph.Graph;
import com.google.common.graph.Graphs;
import com.google.common.graph.MutableGraph;
import com.google.common.graph.Traverser;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.neoforged.fml.loading.toposort.CyclePresentException;
import net.neoforged.fml.loading.toposort.TopologicalSort;
import net.neoforged.neoforge.event.SortedReloadListenerEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public class ReloadListenerSort {
   public static List<PreparableReloadListener> sort(SortedReloadListenerEvent event) {
      return sortListeners(event.getNameLookup(), (MutableGraph<PreparableReloadListener>)event.getGraph(), event.getRegistry(), event.getLastVanillaListener());
   }

   public static List<PreparableReloadListener> sortListeners(
      SortedReloadListenerEvent.NameLookup lookup,
      MutableGraph<PreparableReloadListener> graph,
      Map<Identifier, PreparableReloadListener> registry,
      PreparableReloadListener lastVanilla
   ) {
      for (Entry<Identifier, PreparableReloadListener> entry : registry.entrySet()) {
         if (needsToBeLinkedToVanilla(lookup, graph, entry.getValue())) {
            graph.putEdge(lastVanilla, entry.getValue());
         }
      }

      Object2IntMap<PreparableReloadListener> insertionOrder = new Object2IntOpenHashMap();
      int idx = 0;

      for (PreparableReloadListener listener : registry.values()) {
         insertionOrder.put(listener, idx++);
      }

      try {
         List<PreparableReloadListener> sorted = TopologicalSort.topologicalSort(graph, Comparator.comparingInt(insertionOrder::getInt));
         return Collections.unmodifiableList(sorted);
      } catch (CyclePresentException var15) {
         Set<Set<PreparableReloadListener>> cycles = var15.getCycles();
         Set<Set<Identifier>> keyedCycles = cycles.stream()
            .map(set -> set.stream().map(lookup::apply).collect(Collectors.toCollection(LinkedHashSet::new)))
            .collect(Collectors.toSet());
         StringBuilder sb = new StringBuilder();
         sb.append("Cycles were detected during reload listener sorting:").append('\n');
         idx = 0;

         for (Set<Identifier> cycle : keyedCycles) {
            StringBuilder msg = new StringBuilder();
            msg.append(idx++).append(": ");

            for (Identifier key : cycle) {
               msg.append(key).append("->");
            }

            msg.append(cycle.iterator().next());
            sb.append((CharSequence)msg);
            sb.append('\n');
         }

         throw new IllegalArgumentException(sb.toString());
      }
   }

   private static boolean needsToBeLinkedToVanilla(
      SortedReloadListenerEvent.NameLookup lookup, Graph<PreparableReloadListener> graph, PreparableReloadListener listener
   ) {
      if (isVanilla(lookup, listener)) {
         return false;
      } else {
         for (PreparableReloadListener node : Traverser.forGraph(graph).depthFirstPreOrder(listener)) {
            if (isVanilla(lookup, node)) {
               return false;
            }
         }

         for (PreparableReloadListener nodex : Traverser.forGraph(Graphs.transpose(graph)).depthFirstPreOrder(listener)) {
            if (isVanilla(lookup, nodex)) {
               return false;
            }
         }

         return true;
      }
   }

   private static boolean isVanilla(SortedReloadListenerEvent.NameLookup lookup, PreparableReloadListener listener) {
      return "minecraft".equals(lookup.apply(listener).getNamespace());
   }
}
