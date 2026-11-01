package net.minecraft.util;

import com.mojang.authlib.yggdrasil.ServicesKeyInfo;
import com.mojang.authlib.yggdrasil.ServicesKeySet;
import com.mojang.authlib.yggdrasil.ServicesKeyType;
import com.mojang.logging.LogUtils;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Collection;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public interface SignatureValidator {
   SignatureValidator NO_VALIDATION = (signatureUpdater, bs) -> true;
   Logger LOGGER = LogUtils.getLogger();

   boolean validate(SignatureUpdater signatureUpdater, byte[] bs);

   default boolean validate(byte[] digest, byte[] signature) {
      return this.validate(output -> output.update(digest), signature);
   }

   private static boolean verifySignature(SignatureUpdater updater, byte[] signatureBytes, Signature signature) throws SignatureException {
      updater.update(signature::update);
      return signature.verify(signatureBytes);
   }

   static SignatureValidator from(PublicKey publicKey, String algorithm) {
      return (signatureUpdater, bs) -> {
         try {
            Signature signature = Signature.getInstance(algorithm);
            signature.initVerify(publicKey);
            return verifySignature(signatureUpdater, bs, signature);
         } catch (Exception var5) {
            LOGGER.error("Failed to verify signature", var5);
            return false;
         }
      };
   }

   @Nullable
   static SignatureValidator from(ServicesKeySet serviceKeySet, ServicesKeyType serviceKeyType) {
      Collection<ServicesKeyInfo> collection = serviceKeySet.keys(serviceKeyType);
      return collection.isEmpty() ? null : (signatureUpdater, bs) -> collection.stream().anyMatch(servicesKeyInfo -> {
         Signature signature = servicesKeyInfo.signature();

         try {
            return verifySignature(signatureUpdater, bs, signature);
         } catch (SignatureException var5) {
            LOGGER.error("Failed to verify Services signature", var5);
            return false;
         }
      });
   }
}
