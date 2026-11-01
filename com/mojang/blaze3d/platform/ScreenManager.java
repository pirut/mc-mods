package com.mojang.blaze3d.platform;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import javax.annotation.Nullable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWMonitorCallback;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ScreenManager {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Long2ObjectMap<Monitor> monitors = new Long2ObjectOpenHashMap();
   private final MonitorCreator monitorCreator;

   public ScreenManager(MonitorCreator p_85265_) {
      this.monitorCreator = p_85265_;
      GLFW.glfwSetMonitorCallback(this::onMonitorChange);
      PointerBuffer $$1 = GLFW.glfwGetMonitors();
      if ($$1 != null) {
         for (int $$2 = 0; $$2 < $$1.limit(); $$2++) {
            long $$3 = $$1.get($$2);
            this.monitors.put($$3, p_85265_.createMonitor($$3));
         }
      }
   }

   private void onMonitorChange(long p_85274_, int p_85275_) {
      RenderSystem.assertOnRenderThread();
      if (p_85275_ == 262145) {
         this.monitors.put(p_85274_, this.monitorCreator.createMonitor(p_85274_));
         LOGGER.debug("Monitor {} connected. Current monitors: {}", p_85274_, this.monitors);
      } else if (p_85275_ == 262146) {
         this.monitors.remove(p_85274_);
         LOGGER.debug("Monitor {} disconnected. Current monitors: {}", p_85274_, this.monitors);
      }
   }

   @Nullable
   public Monitor getMonitor(long p_85272_) {
      return (Monitor)this.monitors.get(p_85272_);
   }

   @Nullable
   public Monitor findBestMonitor(Window p_85277_) {
      long $$1 = GLFW.glfwGetWindowMonitor(p_85277_.getWindow());
      if ($$1 != 0L) {
         return this.getMonitor($$1);
      } else {
         int $$2 = p_85277_.getX();
         int $$3 = $$2 + p_85277_.getScreenWidth();
         int $$4 = p_85277_.getY();
         int $$5 = $$4 + p_85277_.getScreenHeight();
         int $$6 = -1;
         Monitor $$7 = null;
         long $$8 = GLFW.glfwGetPrimaryMonitor();
         LOGGER.debug("Selecting monitor - primary: {}, current monitors: {}", $$8, this.monitors);
         ObjectIterator var12 = this.monitors.values().iterator();

         while (var12.hasNext()) {
            Monitor $$9 = (Monitor)var12.next();
            int $$10 = $$9.getX();
            int $$11 = $$10 + $$9.getCurrentMode().getWidth();
            int $$12 = $$9.getY();
            int $$13 = $$12 + $$9.getCurrentMode().getHeight();
            int $$14 = clamp($$2, $$10, $$11);
            int $$15 = clamp($$3, $$10, $$11);
            int $$16 = clamp($$4, $$12, $$13);
            int $$17 = clamp($$5, $$12, $$13);
            int $$18 = Math.max(0, $$15 - $$14);
            int $$19 = Math.max(0, $$17 - $$16);
            int $$20 = $$18 * $$19;
            if ($$20 > $$6) {
               $$7 = $$9;
               $$6 = $$20;
            } else if ($$20 == $$6 && $$8 == $$9.getMonitor()) {
               LOGGER.debug("Primary monitor {} is preferred to monitor {}", $$9, $$7);
               $$7 = $$9;
            }
         }

         LOGGER.debug("Selected monitor: {}", $$7);
         return $$7;
      }
   }

   public static int clamp(int p_85268_, int p_85269_, int p_85270_) {
      if (p_85268_ < p_85269_) {
         return p_85269_;
      } else {
         return p_85268_ > p_85270_ ? p_85270_ : p_85268_;
      }
   }

   public void shutdown() {
      RenderSystem.assertOnRenderThread();
      GLFWMonitorCallback $$0 = GLFW.glfwSetMonitorCallback(null);
      if ($$0 != null) {
         $$0.free();
      }
   }
}
