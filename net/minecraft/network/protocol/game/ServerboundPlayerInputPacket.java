package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ServerboundPlayerInputPacket implements Packet<ServerGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ServerboundPlayerInputPacket> STREAM_CODEC = Packet.codec(
      ServerboundPlayerInputPacket::write, ServerboundPlayerInputPacket::new
   );
   private static final int FLAG_JUMPING = 1;
   private static final int FLAG_SHIFT_KEY_DOWN = 2;
   private final float xxa;
   private final float zza;
   private final boolean isJumping;
   private final boolean isShiftKeyDown;

   public ServerboundPlayerInputPacket(float p_134345_, float p_134346_, boolean p_134347_, boolean p_134348_) {
      this.xxa = p_134345_;
      this.zza = p_134346_;
      this.isJumping = p_134347_;
      this.isShiftKeyDown = p_134348_;
   }

   private ServerboundPlayerInputPacket(FriendlyByteBuf p_179720_) {
      this.xxa = p_179720_.readFloat();
      this.zza = p_179720_.readFloat();
      byte $$1 = p_179720_.readByte();
      this.isJumping = ($$1 & 1) > 0;
      this.isShiftKeyDown = ($$1 & 2) > 0;
   }

   private void write(FriendlyByteBuf p_134357_) {
      p_134357_.writeFloat(this.xxa);
      p_134357_.writeFloat(this.zza);
      byte $$1 = 0;
      if (this.isJumping) {
         $$1 = (byte)($$1 | 1);
      }

      if (this.isShiftKeyDown) {
         $$1 = (byte)($$1 | 2);
      }

      p_134357_.writeByte($$1);
   }

   @Override
   public PacketType<ServerboundPlayerInputPacket> type() {
      return GamePacketTypes.SERVERBOUND_PLAYER_INPUT;
   }

   public void handle(ServerGamePacketListener p_134354_) {
      p_134354_.handlePlayerInput(this);
   }

   public float getXxa() {
      return this.xxa;
   }

   public float getZza() {
      return this.zza;
   }

   public boolean isJumping() {
      return this.isJumping;
   }

   public boolean isShiftKeyDown() {
      return this.isShiftKeyDown;
   }
}
