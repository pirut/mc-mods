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
      instance -> instance.group(UUIDUtil.STRING_CODEC.fieldOf("id").forGetter(NameAndId::id), Codec.STRING.fieldOf("name").forGetter(NameAndId::name))
         .apply(instance, NameAndId::new)
   );

   public NameAndId(GameProfile profile) {
      this(profile.id(), profile.name());
   }

   public NameAndId(com.mojang.authlib.yggdrasil.response.NameAndId nameAndId) {
      this(nameAndId.id(), nameAndId.name());
   }

   @Nullable
   public static NameAndId fromJson(JsonObject json) {
      if (json.has("uuid") && json.has("name")) {
         String string = json.get("uuid").getAsString();

         UUID uUID;
         try {
            uUID = UUID.fromString(string);
         } catch (Throwable var4) {
            return null;
         }

         return new NameAndId(uUID, json.get("name").getAsString());
      } else {
         return null;
      }
   }

   public void appendTo(JsonObject json) {
      json.addProperty("uuid", this.id().toString());
      json.addProperty("name", this.name());
   }

   public static NameAndId createOffline(String username) {
      UUID uUID = UUIDUtil.createOfflinePlayerUUID(username);
      return new NameAndId(uUID, username);
   }
}
