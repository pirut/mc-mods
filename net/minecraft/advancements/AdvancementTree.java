package net.minecraft.advancements;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

public class AdvancementTree {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Map<ResourceLocation, AdvancementNode> nodes = new Object2ObjectOpenHashMap();
   private final Set<AdvancementNode> roots = new ObjectLinkedOpenHashSet();
   private final Set<AdvancementNode> tasks = new ObjectLinkedOpenHashSet();
   @Nullable
   private AdvancementTree.Listener listener;

   private void remove(AdvancementNode p_301231_) {
      for (AdvancementNode $$1 : p_301231_.children()) {
         this.remove($$1);
      }

      LOGGER.info("Forgot about advancement {}", p_301231_.holder());
      this.nodes.remove(p_301231_.holder().id());
      if (p_301231_.parent() == null) {
         this.roots.remove(p_301231_);
         if (this.listener != null) {
            this.listener.onRemoveAdvancementRoot(p_301231_);
         }
      } else {
         this.tasks.remove(p_301231_);
         if (this.listener != null) {
            this.listener.onRemoveAdvancementTask(p_301231_);
         }
      }
   }

   public void remove(Set<ResourceLocation> p_300917_) {
      for (ResourceLocation $$1 : p_300917_) {
         AdvancementNode $$2 = this.nodes.get($$1);
         if ($$2 == null) {
            LOGGER.warn("Told to remove advancement {} but I don't know what that is", $$1);
         } else {
            this.remove($$2);
         }
      }
   }

   public void addAll(Collection<AdvancementHolder> p_301192_) {
      List<AdvancementHolder> $$1 = new ArrayList<>(p_301192_);

      while (!$$1.isEmpty()) {
         if (!$$1.removeIf(this::tryInsert)) {
            LOGGER.error("Couldn't load advancements: {}", $$1);
            break;
         }
      }

      LOGGER.info("Loaded {} advancements", this.nodes.size());
   }

   private boolean tryInsert(AdvancementHolder p_301290_) {
      Optional<ResourceLocation> $$1 = p_301290_.value().parent();
      AdvancementNode $$2 = $$1.map(this.nodes::get).orElse(null);
      if ($$2 == null && $$1.isPresent()) {
         return false;
      } else {
         AdvancementNode $$3 = new AdvancementNode(p_301290_, $$2);
         if ($$2 != null) {
            $$2.addChild($$3);
         }

         this.nodes.put(p_301290_.id(), $$3);
         if ($$2 == null) {
            this.roots.add($$3);
            if (this.listener != null) {
               this.listener.onAddAdvancementRoot($$3);
            }
         } else {
            this.tasks.add($$3);
            if (this.listener != null) {
               this.listener.onAddAdvancementTask($$3);
            }
         }

         return true;
      }
   }

   public void clear() {
      this.nodes.clear();
      this.roots.clear();
      this.tasks.clear();
      if (this.listener != null) {
         this.listener.onAdvancementsCleared();
      }
   }

   public Iterable<AdvancementNode> roots() {
      return this.roots;
   }

   public Collection<AdvancementNode> nodes() {
      return this.nodes.values();
   }

   @Nullable
   public AdvancementNode get(ResourceLocation p_301217_) {
      return this.nodes.get(p_301217_);
   }

   @Nullable
   public AdvancementNode get(AdvancementHolder p_301246_) {
      return this.nodes.get(p_301246_.id());
   }

   public void setListener(@Nullable AdvancementTree.Listener p_301114_) {
      this.listener = p_301114_;
      if (p_301114_ != null) {
         for (AdvancementNode $$1 : this.roots) {
            p_301114_.onAddAdvancementRoot($$1);
         }

         for (AdvancementNode $$2 : this.tasks) {
            p_301114_.onAddAdvancementTask($$2);
         }
      }
   }

   public interface Listener {
      void onAddAdvancementRoot(AdvancementNode var1);

      void onRemoveAdvancementRoot(AdvancementNode var1);

      void onAddAdvancementTask(AdvancementNode var1);

      void onRemoveAdvancementTask(AdvancementNode var1);

      void onAdvancementsCleared();
   }
}
