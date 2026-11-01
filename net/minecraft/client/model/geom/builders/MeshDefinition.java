package net.minecraft.client.model.geom.builders;

import com.google.common.collect.ImmutableList;
import java.util.function.UnaryOperator;
import net.minecraft.client.model.geom.PartPose;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MeshDefinition {
   private final PartDefinition root;

   public MeshDefinition() {
      this(new PartDefinition(ImmutableList.of(), PartPose.ZERO));
   }

   private MeshDefinition(PartDefinition p_364076_) {
      this.root = p_364076_;
   }

   public PartDefinition getRoot() {
      return this.root;
   }

   public MeshDefinition transformed(UnaryOperator<PartPose> p_360385_) {
      return new MeshDefinition(this.root.transformed(p_360385_));
   }

   public MeshDefinition apply(MeshTransformer p_397270_) {
      return p_397270_.apply(this);
   }
}
