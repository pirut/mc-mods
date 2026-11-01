package net.neoforged.neoforge.common;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.graph.ElementOrder;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.fml.loading.toposort.TopologicalSort;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.slf4j.Logger;

public final class CreativeModeTabRegistry {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final ResourceLocation CREATIVE_MODE_TAB_ORDERING_JSON = ResourceLocation.fromNamespaceAndPath("neoforge", "creative_mode_tab_ordering.json");
   private static final List<CreativeModeTab> SORTED_TABS = new ArrayList<>();
   private static final List<CreativeModeTab> SORTED_TABS_VIEW = Collections.unmodifiableList(SORTED_TABS);
   private static final List<CreativeModeTab> DEFAULT_TABS = new ArrayList<>();
   private static final Multimap<ResourceLocation, ResourceLocation> edges = HashMultimap.create();

   public static List<CreativeModeTab> getSortedCreativeModeTabs() {
      return SORTED_TABS_VIEW;
   }

   public static List<CreativeModeTab> getDefaultTabs() {
      return Collections.unmodifiableList(DEFAULT_TABS);
   }

   @Nullable
   public static CreativeModeTab getTab(ResourceLocation name) {
      return BuiltInRegistries.CREATIVE_MODE_TAB.get(name);
   }

   @Nullable
   public static ResourceLocation getName(CreativeModeTab tab) {
      return BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
   }

   static PreparableReloadListener getReloadListener() {
      return new SimplePreparableReloadListener<JsonObject>() {
         final Gson gson = new GsonBuilder().create();

         protected JsonObject prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
            Optional<Resource> res = resourceManager.getResource(CreativeModeTabRegistry.CREATIVE_MODE_TAB_ORDERING_JSON);
            if (res.isEmpty()) {
               return new JsonObject();
            } else {
               try {
                  JsonObject var5;
                  try (Reader reader = res.get().openAsReader()) {
                     var5 = (JsonObject)this.gson.fromJson(reader, JsonObject.class);
                  }

                  return var5;
               } catch (IOException var9) {
                  CreativeModeTabRegistry.LOGGER
                     .error("Could not read CreativeModeTab sorting file " + CreativeModeTabRegistry.CREATIVE_MODE_TAB_ORDERING_JSON, var9);
                  return new JsonObject();
               }
            }
         }

         protected void apply(JsonObject data, ResourceManager resourceManager, ProfilerFiller p) {
            try {
               if (data.size() > 0) {
                  JsonArray order = GsonHelper.getAsJsonArray(data, "order");
                  List<CreativeModeTab> customOrder = new ArrayList<>();

                  for (JsonElement entry : order) {
                     ResourceLocation id = ResourceLocation.parse(entry.getAsString());
                     CreativeModeTab CreativeModeTab = CreativeModeTabRegistry.getTab(id);
                     if (CreativeModeTab == null) {
                        throw new IllegalStateException("CreativeModeTab not found with name " + id);
                     }

                     customOrder.add(CreativeModeTab);
                  }

                  List<CreativeModeTab> missingCreativeModeTabs = BuiltInRegistries.CREATIVE_MODE_TAB
                     .stream()
                     .filter(CreativeModeTabx -> !customOrder.contains(CreativeModeTabx))
                     .toList();
                  if (!missingCreativeModeTabs.isEmpty()) {
                     throw new IllegalStateException(
                        "CreativeModeTabs missing from the ordered list: "
                           + missingCreativeModeTabs.stream()
                              .map(CreativeModeTabx -> Objects.toString(CreativeModeTabRegistry.getName(CreativeModeTabx)))
                              .collect(Collectors.joining(", "))
                     );
                  }

                  CreativeModeTabRegistry.setCreativeModeTabOrder(customOrder);
                  return;
               }
            } catch (Exception var10) {
               CreativeModeTabRegistry.LOGGER
                  .error("Error parsing CreativeModeTab sorting file " + CreativeModeTabRegistry.CREATIVE_MODE_TAB_ORDERING_JSON, var10);
            }

            CreativeModeTabRegistry.recalculateItemCreativeModeTabs();
         }
      };
   }

   private static void recalculateItemCreativeModeTabs() {
      MutableGraph<CreativeModeTab> graph = GraphBuilder.directed().nodeOrder(ElementOrder.insertion()).build();

      for (CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
         if (!DEFAULT_TABS.contains(tab)) {
            graph.addNode(tab);
         }
      }

      edges.forEach((key, value) -> {
         CreativeModeTab keyC = getTab(key);
         CreativeModeTab valueC = getTab(value);
         if (keyC != null && valueC != null) {
            graph.putEdge(keyC, valueC);
         }
      });
      List<CreativeModeTab> tierList = TopologicalSort.topologicalSort(graph, Comparator.comparing(tabx -> getName(tabx)));
      setCreativeModeTabOrder(tierList);
   }

   private static void setCreativeModeTabOrder(List<CreativeModeTab> tierList) {
      runInServerThreadIfPossible(hasServer -> {
         SORTED_TABS.clear();
         SORTED_TABS.addAll(tierList);
      });
   }

   private static void runInServerThreadIfPossible(BooleanConsumer runnable) {
      MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
      if (server != null) {
         server.execute(() -> runnable.accept(true));
      } else {
         runnable.accept(false);
      }
   }

   @Internal
   public static void sortTabs() {
      edges.clear();
      DEFAULT_TABS.add(BuiltInRegistries.CREATIVE_MODE_TAB.get(CreativeModeTabs.HOTBAR));
      DEFAULT_TABS.add(BuiltInRegistries.CREATIVE_MODE_TAB.get(CreativeModeTabs.SEARCH));
      DEFAULT_TABS.add(BuiltInRegistries.CREATIVE_MODE_TAB.get(CreativeModeTabs.OP_BLOCKS));
      DEFAULT_TABS.add(BuiltInRegistries.CREATIVE_MODE_TAB.get(CreativeModeTabs.INVENTORY));
      List<Holder<CreativeModeTab>> indexed = new ArrayList<>();
      BuiltInRegistries.CREATIVE_MODE_TAB.holders().filter(c -> !DEFAULT_TABS.contains(c.value())).forEach(indexed::add);
      int vanillaTabs = 10;

      for (int i = 0; i < vanillaTabs; i++) {
         Holder<CreativeModeTab> value = indexed.get(i);
         CreativeModeTab tab = value.value();
         ResourceLocation name = value.unwrapKey().orElseThrow().location();
         if (tab.tabsBefore.isEmpty() && tab.tabsAfter.isEmpty()) {
            if (i != 0) {
               edges.put(indexed.get(i - 1).unwrapKey().orElseThrow().location(), name);
            }

            if (i + 1 < indexed.size()) {
               edges.put(name, indexed.get(i + 1).unwrapKey().orElseThrow().location());
            }
         } else {
            addTabOrder(tab, name);
         }
      }

      ResourceLocation lastVanilla = indexed.get(vanillaTabs - 1).unwrapKey().orElseThrow().location();

      for (int ix = vanillaTabs; ix < indexed.size(); ix++) {
         Holder<CreativeModeTab> value = indexed.get(ix);
         CreativeModeTab tab = value.value();
         ResourceLocation name = value.unwrapKey().orElseThrow().location();
         if (tab.tabsBefore.isEmpty() && tab.tabsAfter.isEmpty()) {
            edges.put(lastVanilla, name);
         } else {
            addTabOrder(tab, name);
         }
      }

      recalculateItemCreativeModeTabs();
   }

   private static void addTabOrder(CreativeModeTab tab, ResourceLocation name) {
      for (ResourceLocation after : tab.tabsAfter) {
         edges.put(name, after);
      }

      for (ResourceLocation before : tab.tabsBefore) {
         edges.put(before, name);
      }
   }
}
