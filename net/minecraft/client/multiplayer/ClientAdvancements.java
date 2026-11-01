package net.minecraft.client.multiplayer;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.client.telemetry.WorldSessionTelemetryManager;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.network.protocol.game.ServerboundSeenAdvancementsPacket;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ClientAdvancements {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Minecraft minecraft;
   private final WorldSessionTelemetryManager telemetryManager;
   private final AdvancementTree tree = new AdvancementTree();
   private final Map<AdvancementHolder, AdvancementProgress> progress = new Object2ObjectOpenHashMap();
   @Nullable
   private ClientAdvancements.Listener listener;
   @Nullable
   private AdvancementHolder selectedTab;

   public ClientAdvancements(Minecraft p_286782_, WorldSessionTelemetryManager p_286391_) {
      this.minecraft = p_286782_;
      this.telemetryManager = p_286391_;
   }

   public void update(ClientboundUpdateAdvancementsPacket p_104400_) {
      if (p_104400_.shouldReset()) {
         this.tree.clear();
         this.progress.clear();
      }

      this.tree.remove(p_104400_.getRemoved());
      this.tree.addAll(p_104400_.getAdded());

      for (Entry<ResourceLocation, AdvancementProgress> $$1 : p_104400_.getProgress().entrySet()) {
         AdvancementNode $$2 = this.tree.get($$1.getKey());
         if ($$2 != null) {
            AdvancementProgress $$3 = $$1.getValue();
            $$3.update($$2.advancement().requirements());
            this.progress.put($$2.holder(), $$3);
            if (this.listener != null) {
               this.listener.onUpdateAdvancementProgress($$2, $$3);
            }

            if (!p_104400_.shouldReset() && $$3.isDone()) {
               if (this.minecraft.level != null) {
                  this.telemetryManager.onAdvancementDone(this.minecraft.level, $$2.holder());
               }

               Optional<DisplayInfo> $$4 = $$2.advancement().display();
               if ($$4.isPresent() && $$4.get().shouldShowToast()) {
                  this.minecraft.getToasts().addToast(new AdvancementToast($$2.holder()));
               }
            }
         } else {
            LOGGER.warn("Server informed client about progress for unknown advancement {}", $$1.getKey());
         }
      }
   }

   public AdvancementTree getTree() {
      return this.tree;
   }

   public void setSelectedTab(@Nullable AdvancementHolder p_301030_, boolean p_104403_) {
      ClientPacketListener $$2 = this.minecraft.getConnection();
      if ($$2 != null && p_301030_ != null && p_104403_) {
         $$2.send(ServerboundSeenAdvancementsPacket.openedTab(p_301030_));
      }

      if (this.selectedTab != p_301030_) {
         this.selectedTab = p_301030_;
         if (this.listener != null) {
            this.listener.onSelectedTabChanged(p_301030_);
         }
      }
   }

   public void setListener(@Nullable ClientAdvancements.Listener p_104398_) {
      this.listener = p_104398_;
      this.tree.setListener(p_104398_);
      if (p_104398_ != null) {
         this.progress.forEach((p_300950_, p_301173_) -> {
            AdvancementNode $$3 = this.tree.get(p_300950_);
            if ($$3 != null) {
               p_104398_.onUpdateAdvancementProgress($$3, p_301173_);
            }
         });
         p_104398_.onSelectedTabChanged(this.selectedTab);
      }
   }

   @Nullable
   public AdvancementHolder get(ResourceLocation p_301015_) {
      AdvancementNode $$1 = this.tree.get(p_301015_);
      return $$1 != null ? $$1.holder() : null;
   }

   @OnlyIn(Dist.CLIENT)
   public interface Listener extends AdvancementTree.Listener {
      void onUpdateAdvancementProgress(AdvancementNode var1, AdvancementProgress var2);

      void onSelectedTabChanged(@Nullable AdvancementHolder var1);
   }
}
