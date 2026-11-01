package net.minecraft.client.renderer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.system.MemoryStack;

@OnlyIn(Dist.CLIENT)
public class GlobalSettingsUniform implements AutoCloseable {
   public static final int UBO_SIZE = new Std140SizeCalculator().putIVec3().putVec3().putVec2().putFloat().putFloat().putInt().putInt().get();
   private final GpuBuffer buffer = RenderSystem.getDevice().createBuffer(() -> "Global Settings UBO", 136, UBO_SIZE);

   public void update(
      int p_418423_, int p_418065_, double p_418173_, long p_418308_, DeltaTracker p_418331_, int p_418075_, Camera p_460873_, boolean p_478902_
   ) {
      Vec3 vec3 = p_460873_.position();
      MemoryStack memorystack = MemoryStack.stackPush();

      try {
         int i = Mth.floor(vec3.x);
         int j = Mth.floor(vec3.y);
         int k = Mth.floor(vec3.z);
         ByteBuffer bytebuffer = Std140Builder.onStack(memorystack, UBO_SIZE)
            .putIVec3(i, j, k)
            .putVec3((float)(i - vec3.x), (float)(j - vec3.y), (float)(k - vec3.z))
            .putVec2(p_418423_, p_418065_)
            .putFloat((float)p_418173_)
            .putFloat(((float)(p_418308_ % 24000L) + p_418331_.getGameTimeDeltaPartialTick(false)) / 24000.0F)
            .putInt(p_418075_)
            .putInt(p_478902_ ? 1 : 0)
            .get();
         RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.buffer.slice(), bytebuffer);
      } catch (Throwable var18) {
         if (memorystack != null) {
            try {
               memorystack.close();
            } catch (Throwable var17) {
               var18.addSuppressed(var17);
            }
         }

         throw var18;
      }

      if (memorystack != null) {
         memorystack.close();
      }

      RenderSystem.setGlobalSettingsUniform(this.buffer);
   }

   @Override
   public void close() {
      this.buffer.close();
   }
}
