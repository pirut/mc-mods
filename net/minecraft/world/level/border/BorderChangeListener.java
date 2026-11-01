package net.minecraft.world.level.border;

public interface BorderChangeListener {
   void onBorderSizeSet(WorldBorder var1, double var2);

   void onBorderSizeLerping(WorldBorder var1, double var2, double var4, long var6);

   void onBorderCenterSet(WorldBorder var1, double var2, double var4);

   void onBorderSetWarningTime(WorldBorder var1, int var2);

   void onBorderSetWarningBlocks(WorldBorder var1, int var2);

   void onBorderSetDamagePerBlock(WorldBorder var1, double var2);

   void onBorderSetDamageSafeZOne(WorldBorder var1, double var2);

   public static class DelegateBorderChangeListener implements BorderChangeListener {
      private final WorldBorder worldBorder;

      public DelegateBorderChangeListener(WorldBorder p_61866_) {
         this.worldBorder = p_61866_;
      }

      @Override
      public void onBorderSizeSet(WorldBorder p_61868_, double p_61869_) {
         this.worldBorder.setSize(p_61869_);
      }

      @Override
      public void onBorderSizeLerping(WorldBorder p_61875_, double p_61876_, double p_61877_, long p_61878_) {
         this.worldBorder.lerpSizeBetween(p_61876_, p_61877_, p_61878_);
      }

      @Override
      public void onBorderCenterSet(WorldBorder p_61871_, double p_61872_, double p_61873_) {
         this.worldBorder.setCenter(p_61872_, p_61873_);
      }

      @Override
      public void onBorderSetWarningTime(WorldBorder p_61880_, int p_61881_) {
         this.worldBorder.setWarningTime(p_61881_);
      }

      @Override
      public void onBorderSetWarningBlocks(WorldBorder p_61886_, int p_61887_) {
         this.worldBorder.setWarningBlocks(p_61887_);
      }

      @Override
      public void onBorderSetDamagePerBlock(WorldBorder p_61883_, double p_61884_) {
         this.worldBorder.setDamagePerBlock(p_61884_);
      }

      @Override
      public void onBorderSetDamageSafeZOne(WorldBorder p_61889_, double p_61890_) {
         this.worldBorder.setDamageSafeZone(p_61890_);
      }
   }
}
