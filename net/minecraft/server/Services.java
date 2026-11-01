package net.minecraft.server;

import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.ServicesKeySet;
import com.mojang.authlib.yggdrasil.ServicesKeyType;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import java.io.File;
import javax.annotation.Nullable;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.util.SignatureValidator;

public record Services(
   MinecraftSessionService sessionService, ServicesKeySet servicesKeySet, GameProfileRepository profileRepository, GameProfileCache profileCache
) {
   private static final String USERID_CACHE_FILE = "usercache.json";

   public static Services create(YggdrasilAuthenticationService p_214345_, File p_214346_) {
      MinecraftSessionService $$2 = p_214345_.createMinecraftSessionService();
      GameProfileRepository $$3 = p_214345_.createProfileRepository();
      GameProfileCache $$4 = new GameProfileCache($$3, new File(p_214346_, "usercache.json"));
      return new Services($$2, p_214345_.getServicesKeySet(), $$3, $$4);
   }

   @Nullable
   public SignatureValidator profileKeySignatureValidator() {
      return SignatureValidator.from(this.servicesKeySet, ServicesKeyType.PROFILE_KEY);
   }

   public boolean canValidateProfileKeys() {
      return !this.servicesKeySet.keys(ServicesKeyType.PROFILE_KEY).isEmpty();
   }
}
