package net.minecraft.world.level.portal;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec3;

public record TeleportTransition(
   ServerLevel newLevel,
   Vec3 position,
   Vec3 deltaMovement,
   float yRot,
   float xRot,
   boolean missingRespawnBlock,
   boolean asPassenger,
   Set<Relative> relatives,
   TeleportTransition.PostTeleportTransition postTeleportTransition
) {
   public static final TeleportTransition.PostTeleportTransition DO_NOTHING = p_379662_ -> {};
   public static final TeleportTransition.PostTeleportTransition PLAY_PORTAL_SOUND = TeleportTransition::playPortalSound;
   public static final TeleportTransition.PostTeleportTransition PLACE_PORTAL_TICKET = TeleportTransition::placePortalTicket;

   public TeleportTransition(
      ServerLevel p_379776_, Vec3 p_379412_, Vec3 p_379320_, float p_380257_, float p_379610_, TeleportTransition.PostTeleportTransition p_380303_
   ) {
      this(p_379776_, p_379412_, p_379320_, p_380257_, p_379610_, Set.of(), p_380303_);
   }

   public TeleportTransition(
      ServerLevel p_380133_,
      Vec3 p_379861_,
      Vec3 p_380308_,
      float p_379941_,
      float p_380119_,
      Set<Relative> p_379959_,
      TeleportTransition.PostTeleportTransition p_379425_
   ) {
      this(p_380133_, p_379861_, p_380308_, p_379941_, p_380119_, false, false, p_379959_, p_379425_);
   }

   private static void playPortalSound(Entity p_380322_) {
      if (p_380322_ instanceof ServerPlayer serverplayer) {
         serverplayer.connection.send(new ClientboundLevelEventPacket(1032, BlockPos.ZERO, 0, false));
      }
   }

   private static void placePortalTicket(Entity p_379684_) {
      p_379684_.placePortalTicket(BlockPos.containing(p_379684_.position()));
   }

   public static TeleportTransition createDefault(ServerPlayer p_451087_, TeleportTransition.PostTeleportTransition p_451271_) {
      ServerLevel serverlevel = p_451087_.level().getServer().findRespawnDimension();
      LevelData.RespawnData leveldata$respawndata = serverlevel.getRespawnData();
      return new TeleportTransition(
         serverlevel,
         findAdjustedSharedSpawnPos(serverlevel, p_451087_),
         Vec3.ZERO,
         leveldata$respawndata.yaw(),
         leveldata$respawndata.pitch(),
         false,
         false,
         Set.of(),
         p_451271_
      );
   }

   public static TeleportTransition missingRespawnBlock(ServerPlayer p_451170_, TeleportTransition.PostTeleportTransition p_380370_) {
      ServerLevel serverlevel = p_451170_.level().getServer().findRespawnDimension();
      LevelData.RespawnData leveldata$respawndata = serverlevel.getRespawnData();
      return new TeleportTransition(
         serverlevel,
         findAdjustedSharedSpawnPos(serverlevel, p_451170_),
         Vec3.ZERO,
         leveldata$respawndata.yaw(),
         leveldata$respawndata.pitch(),
         true,
         false,
         Set.of(),
         p_380370_
      );
   }

   private static Vec3 findAdjustedSharedSpawnPos(ServerLevel p_379295_, Entity p_379825_) {
      return p_379825_.adjustSpawnLocation(p_379295_, p_379295_.getRespawnData().pos()).getBottomCenter();
   }

   public TeleportTransition withRotation(float p_380177_, float p_379582_) {
      return new TeleportTransition(
         this.newLevel(),
         this.position(),
         this.deltaMovement(),
         p_380177_,
         p_379582_,
         this.missingRespawnBlock(),
         this.asPassenger(),
         this.relatives(),
         this.postTeleportTransition()
      );
   }

   public TeleportTransition withPosition(Vec3 p_379914_) {
      return new TeleportTransition(
         this.newLevel(),
         p_379914_,
         this.deltaMovement(),
         this.yRot(),
         this.xRot(),
         this.missingRespawnBlock(),
         this.asPassenger(),
         this.relatives(),
         this.postTeleportTransition()
      );
   }

   public TeleportTransition transitionAsPassenger() {
      return new TeleportTransition(
         this.newLevel(),
         this.position(),
         this.deltaMovement(),
         this.yRot(),
         this.xRot(),
         this.missingRespawnBlock(),
         true,
         this.relatives(),
         this.postTeleportTransition()
      );
   }

   @FunctionalInterface
   public interface PostTeleportTransition {
      void onTransition(Entity var1);

      default TeleportTransition.PostTeleportTransition then(TeleportTransition.PostTeleportTransition p_380353_) {
         return p_380407_ -> {
            this.onTransition(p_380407_);
            p_380353_.onTransition(p_380407_);
         };
      }
   }
}
