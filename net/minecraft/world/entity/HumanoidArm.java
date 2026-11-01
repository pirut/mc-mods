package net.minecraft.world.entity;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.function.IntFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

public enum HumanoidArm implements StringRepresentable {
   LEFT(0, "left", "options.mainHand.left"),
   RIGHT(1, "right", "options.mainHand.right");

   public static final Codec<HumanoidArm> CODEC = StringRepresentable.fromEnum(HumanoidArm::values);
   private static final IntFunction<HumanoidArm> BY_ID = ByIdMap.continuous(p_467072_ -> p_467072_.id, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
   public static final StreamCodec<ByteBuf, HumanoidArm> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, p_468712_ -> p_468712_.id);
   private final int id;
   private final String name;
   private final Component caption;

   private HumanoidArm(int p_217028_, String p_217029_, String p_295547_) {
      this.id = p_217028_;
      this.name = p_217029_;
      this.caption = Component.translatable(p_295547_);
   }

   public HumanoidArm getOpposite() {
      return switch (this) {
         case LEFT -> RIGHT;
         case RIGHT -> LEFT;
      };
   }

   public Component caption() {
      return this.caption;
   }

   @Override
   public String getSerializedName() {
      return this.name;
   }
}
