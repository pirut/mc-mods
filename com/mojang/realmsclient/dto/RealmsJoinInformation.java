package com.mojang.realmsclient.dto;

import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public record RealmsJoinInformation(
   @Nullable @SerializedName("address") String address,
   @Nullable @SerializedName("resourcePackUrl") String resourcePackUrl,
   @Nullable @SerializedName("resourcePackHash") String resourcePackHash,
   @Nullable @SerializedName("sessionRegionData") RealmsJoinInformation.RegionData regionData
) implements ReflectionBasedSerialization {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final RealmsJoinInformation EMPTY = new RealmsJoinInformation(null, null, null, null);

   public static RealmsJoinInformation parse(GuardedSerializer p_426148_, String p_425511_) {
      try {
         RealmsJoinInformation realmsjoininformation = p_426148_.fromJson(p_425511_, RealmsJoinInformation.class);
         if (realmsjoininformation == null) {
            LOGGER.error("Could not parse RealmsServerAddress: {}", p_425511_);
            return EMPTY;
         } else {
            return realmsjoininformation;
         }
      } catch (Exception var3) {
         LOGGER.error("Could not parse RealmsServerAddress", var3);
         return EMPTY;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public record RegionData(
      @Nullable @SerializedName("regionName") RealmsRegion region, @Nullable @SerializedName("serviceQuality") ServiceQuality serviceQuality
   ) implements ReflectionBasedSerialization {
   }
}
