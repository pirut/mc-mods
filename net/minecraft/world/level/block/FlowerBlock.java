package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class FlowerBlock extends VegetationBlock implements SuspiciousEffectHolder {
   protected static final MapCodec<SuspiciousStewEffects> EFFECTS_FIELD = SuspiciousStewEffects.CODEC.fieldOf("suspicious_stew_effects");
   public static final MapCodec<FlowerBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_432665_ -> p_432665_.group(EFFECTS_FIELD.forGetter(FlowerBlock::getSuspiciousEffects), propertiesCodec()).apply(p_432665_, FlowerBlock::new)
   );
   private static final VoxelShape SHAPE = Block.column(6.0, 0.0, 10.0);
   private final SuspiciousStewEffects suspiciousStewEffects;

   @Override
   public MapCodec<? extends FlowerBlock> codec() {
      return CODEC;
   }

   public FlowerBlock(Holder<MobEffect> p_316154_, float p_332744_, BlockBehaviour.Properties p_53514_) {
      this(makeEffectList(p_316154_, p_332744_), p_53514_);
   }

   public FlowerBlock(SuspiciousStewEffects p_330645_, BlockBehaviour.Properties p_304822_) {
      super(p_304822_);
      this.suspiciousStewEffects = p_330645_;
   }

   protected static SuspiciousStewEffects makeEffectList(Holder<MobEffect> p_316817_, float p_332817_) {
      return new SuspiciousStewEffects(List.of(new SuspiciousStewEffects.Entry(p_316817_, Mth.floor(p_332817_ * 20.0F))));
   }

   @Override
   protected VoxelShape getShape(BlockState p_53517_, BlockGetter p_53518_, BlockPos p_53519_, CollisionContext p_53520_) {
      return SHAPE.move(p_53517_.getOffset(p_53519_));
   }

   @Override
   public SuspiciousStewEffects getSuspiciousEffects() {
      return this.suspiciousStewEffects;
   }

   @Nullable
   public MobEffectInstance getBeeInteractionEffect() {
      return null;
   }
}
