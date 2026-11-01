package net.minecraft.server.players;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import org.jspecify.annotations.Nullable;

public record NameAndId(UUID id, String name) {
   public static final Codec<NameAndId> CODEC = RecordCodecBuilder.create(
      p_443190_ -> p_443190_.group(UUIDUtil.STRING_CODEC.fieldOf("id").forGetter(NameAndId::id), Codec.STRING.fieldOf("name").forGetter(NameAndId::name))
         .apply(p_443190_, NameAndId::new)
   );

   public NameAndId(GameProfile p_433336_) {
      this(p_433336_.id(), p_433336_.name());
   }

   public NameAndId(com.mojang.authlib.yggdrasil.response.NameAndId p_442601_) {
      this(p_442601_.id(), p_442601_.name());
   }

   @Nullable
   public static NameAndId fromJson(JsonObject p_433854_) {
      if (p_433854_.has("uuid") && p_433854_.has("name")) {
         String s = p_433854_.get("uuid").getAsString();

         UUID uuid;
         try {
            uuid = UUID.fromString(s);
         } catch (Throwable var4) {
            return null;
         }

         return new NameAndId(uuid, p_433854_.get("name").getAsString());
      } else {
         return null;
      }
   }

   public void appendTo(JsonObject p_433096_) {
      p_433096_.addProperty("uuid", this.id().toString());
      p_433096_.addProperty("name", this.name());
   }

   public static NameAndId createOffline(String p_433923_) {
      UUID uuid = UUIDUtil.createOfflinePlayerUUID(p_433923_);
      return new NameAndId(uuid, p_433923_);
   }
}
