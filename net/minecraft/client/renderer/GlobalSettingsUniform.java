package net.minecraft.client.renderer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.system.MemoryStack;

@Environment(EnvType.CLIENT)
public class GlobalSettingsUniform implements AutoCloseable {
   public static final int UBO_SIZE = new Std140SizeCalculator().putIVec3().putVec3().putVec2().putFloat().putFloat().putInt().putInt().get();
   private final GpuBuffer buffer = RenderSystem.getDevice().createBuffer(() -> "Global Settings UBO", 136, UBO_SIZE);

   public void update(
      int width,
      int height,
      double glintStrength,
      long gameTime,
      DeltaTracker deltaTracker,
      int menuBackgroundBlurriness,
      Camera camera,
      boolean useRgssFiltering
   ) {
      Vec3 vec3 = camera.position();
      MemoryStack memoryStack = MemoryStack.stackPush();

      try {
         int i = Mth.floor(vec3.x);
         int j = Mth.floor(vec3.y);
         int k = Mth.floor(vec3.z);
         ByteBuffer byteBuffer = Std140Builder.onStack(memoryStack, UBO_SIZE)
            .putIVec3(i, j, k)
            .putVec3((float)(i - vec3.x), (float)(j - vec3.y), (float)(k - vec3.z))
            .putVec2(width, height)
            .putFloat((float)glintStrength)
            .putFloat(((float)(gameTime % 24000L) + deltaTracker.getGameTimeDeltaPartialTick(false)) / 24000.0F)
            .putInt(menuBackgroundBlurriness)
            .putInt(useRgssFiltering ? 1 : 0)
            .get();
         RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.buffer.slice(), byteBuffer);
      } catch (Throwable var18) {
         if (memoryStack != null) {
            try {
               memoryStack.close();
            } catch (Throwable var17) {
               var18.addSuppressed(var17);
            }
         }

         throw var18;
      }

      if (memoryStack != null) {
         memoryStack.close();
      }

      RenderSystem.setGlobalSettingsUniform(this.buffer);
   }

   @Override
   public void close() {
      this.buffer.close();
   }
}
