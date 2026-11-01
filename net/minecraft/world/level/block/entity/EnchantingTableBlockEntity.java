package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class EnchantingTableBlockEntity extends BlockEntity implements Nameable {
   private static final Component DEFAULT_NAME = Component.translatable("container.enchant");
   public int time;
   public float flip;
   public float oFlip;
   public float flipT;
   public float flipA;
   public float open;
   public float oOpen;
   public float rot;
   public float oRot;
   public float tRot;
   private static final RandomSource RANDOM = RandomSource.create();
   @Nullable
   private Component name;

   public EnchantingTableBlockEntity(BlockPos p_340972_, BlockState p_340871_) {
      super(BlockEntityType.ENCHANTING_TABLE, p_340972_, p_340871_);
   }

   @Override
   protected void saveAdditional(ValueOutput p_421876_) {
      super.saveAdditional(p_421876_);
      p_421876_.storeNullable("CustomName", ComponentSerialization.CODEC, this.name);
   }

   @Override
   protected void loadAdditional(ValueInput p_422324_) {
      super.loadAdditional(p_422324_);
      this.name = parseCustomNameSafe(p_422324_, "CustomName");
   }

   public static void bookAnimationTick(Level p_341315_, BlockPos p_341271_, BlockState p_341158_, EnchantingTableBlockEntity p_341066_) {
      p_341066_.oOpen = p_341066_.open;
      p_341066_.oRot = p_341066_.rot;
      Player player = p_341315_.getNearestPlayer(p_341271_.getX() + 0.5, p_341271_.getY() + 0.5, p_341271_.getZ() + 0.5, 3.0, false);
      if (player != null) {
         double d0 = player.getX() - (p_341271_.getX() + 0.5);
         double d1 = player.getZ() - (p_341271_.getZ() + 0.5);
         p_341066_.tRot = (float)Mth.atan2(d1, d0);
         p_341066_.open += 0.1F;
         if (p_341066_.open < 0.5F || RANDOM.nextInt(40) == 0) {
            float f1 = p_341066_.flipT;

            do {
               p_341066_.flipT = p_341066_.flipT + (RANDOM.nextInt(4) - RANDOM.nextInt(4));
            } while (f1 == p_341066_.flipT);
         }
      } else {
         p_341066_.tRot += 0.02F;
         p_341066_.open -= 0.1F;
      }

      while (p_341066_.rot >= (float) Math.PI) {
         p_341066_.rot -= (float) (Math.PI * 2);
      }

      while (p_341066_.rot < (float) -Math.PI) {
         p_341066_.rot += (float) (Math.PI * 2);
      }

      while (p_341066_.tRot >= (float) Math.PI) {
         p_341066_.tRot -= (float) (Math.PI * 2);
      }

      while (p_341066_.tRot < (float) -Math.PI) {
         p_341066_.tRot += (float) (Math.PI * 2);
      }

      float f2 = p_341066_.tRot - p_341066_.rot;

      while (f2 >= (float) Math.PI) {
         f2 -= (float) (Math.PI * 2);
      }

      while (f2 < (float) -Math.PI) {
         f2 += (float) (Math.PI * 2);
      }

      p_341066_.rot += f2 * 0.4F;
      p_341066_.open = Mth.clamp(p_341066_.open, 0.0F, 1.0F);
      p_341066_.time++;
      p_341066_.oFlip = p_341066_.flip;
      float f = (p_341066_.flipT - p_341066_.flip) * 0.4F;
      float f3 = 0.2F;
      f = Mth.clamp(f, -0.2F, 0.2F);
      p_341066_.flipA = p_341066_.flipA + (f - p_341066_.flipA) * 0.9F;
      p_341066_.flip = p_341066_.flip + p_341066_.flipA;
   }

   @Override
   public Component getName() {
      return this.name != null ? this.name : DEFAULT_NAME;
   }

   public void setCustomName(@Nullable Component p_341274_) {
      this.name = p_341274_;
   }

   @Nullable
   @Override
   public Component getCustomName() {
      return this.name;
   }

   @Override
   protected void applyImplicitComponents(DataComponentGetter p_397284_) {
      super.applyImplicitComponents(p_397284_);
      this.name = p_397284_.get(DataComponents.CUSTOM_NAME);
   }

   @Override
   protected void collectImplicitComponents(DataComponentMap.Builder p_340897_) {
      super.collectImplicitComponents(p_340897_);
      p_340897_.set(DataComponents.CUSTOM_NAME, this.name);
   }

   @Override
   public void removeComponentsFromTag(ValueOutput p_422024_) {
      p_422024_.discard("CustomName");
   }
}
