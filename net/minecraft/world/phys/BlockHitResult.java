package net.minecraft.world.phys;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public class BlockHitResult extends HitResult {
   private final Direction direction;
   private final BlockPos blockPos;
   private final boolean miss;
   private final boolean inside;
   private final boolean worldBorderHit;

   public static BlockHitResult miss(Vec3 p_82427_, Direction p_82428_, BlockPos p_82429_) {
      return new BlockHitResult(true, p_82427_, p_82428_, p_82429_, false, false);
   }

   public BlockHitResult(Vec3 p_82415_, Direction p_82416_, BlockPos p_82417_, boolean p_82418_) {
      this(false, p_82415_, p_82416_, p_82417_, p_82418_, false);
   }

   public BlockHitResult(Vec3 p_366888_, Direction p_366561_, BlockPos p_366409_, boolean p_366822_, boolean p_366589_) {
      this(false, p_366888_, p_366561_, p_366409_, p_366822_, p_366589_);
   }

   private BlockHitResult(boolean p_82420_, Vec3 p_82421_, Direction p_82422_, BlockPos p_82423_, boolean p_82424_, boolean p_366493_) {
      super(p_82421_);
      this.miss = p_82420_;
      this.direction = p_82422_;
      this.blockPos = p_82423_;
      this.inside = p_82424_;
      this.worldBorderHit = p_366493_;
   }

   public BlockHitResult withDirection(Direction p_82433_) {
      return new BlockHitResult(this.miss, this.location, p_82433_, this.blockPos, this.inside, this.worldBorderHit);
   }

   public BlockHitResult withPosition(BlockPos p_82431_) {
      return new BlockHitResult(this.miss, this.location, this.direction, p_82431_, this.inside, this.worldBorderHit);
   }

   public BlockHitResult hitBorder() {
      return new BlockHitResult(this.miss, this.location, this.direction, this.blockPos, this.inside, true);
   }

   public BlockPos getBlockPos() {
      return this.blockPos;
   }

   public Direction getDirection() {
      return this.direction;
   }

   @Override
   public HitResult.Type getType() {
      return this.miss ? HitResult.Type.MISS : HitResult.Type.BLOCK;
   }

   public boolean isInside() {
      return this.inside;
   }

   public boolean isWorldBorderHit() {
      return this.worldBorderHit;
   }
}
