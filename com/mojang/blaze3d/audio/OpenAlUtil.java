package com.mojang.blaze3d.audio;

import com.mojang.logging.LogUtils;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC10;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class OpenAlUtil {
   private static final Logger LOGGER = LogUtils.getLogger();

   private static String alErrorToString(int p_83783_) {
      switch (p_83783_) {
         case 40961:
            return "Invalid name parameter.";
         case 40962:
            return "Invalid enumerated parameter value.";
         case 40963:
            return "Invalid parameter parameter value.";
         case 40964:
            return "Invalid operation.";
         case 40965:
            return "Unable to allocate memory.";
         default:
            return "An unrecognized error occurred.";
      }
   }

   static boolean checkALError(String p_83788_) {
      int $$1 = AL10.alGetError();
      if ($$1 != 0) {
         LOGGER.error("{}: {}", p_83788_, alErrorToString($$1));
         return true;
      } else {
         return false;
      }
   }

   private static String alcErrorToString(int p_83792_) {
      switch (p_83792_) {
         case 40961:
            return "Invalid device.";
         case 40962:
            return "Invalid context.";
         case 40963:
            return "Illegal enum.";
         case 40964:
            return "Invalid value.";
         case 40965:
            return "Unable to allocate memory.";
         default:
            return "An unrecognized error occurred.";
      }
   }

   static boolean checkALCError(long p_83785_, String p_83786_) {
      int $$2 = ALC10.alcGetError(p_83785_);
      if ($$2 != 0) {
         LOGGER.error("{} ({}): {}", new Object[]{p_83786_, p_83785_, alcErrorToString($$2)});
         return true;
      } else {
         return false;
      }
   }

   static int audioFormatToOpenAl(AudioFormat p_83790_) {
      Encoding $$1 = p_83790_.getEncoding();
      int $$2 = p_83790_.getChannels();
      int $$3 = p_83790_.getSampleSizeInBits();
      if ($$1.equals(Encoding.PCM_UNSIGNED) || $$1.equals(Encoding.PCM_SIGNED)) {
         if ($$2 == 1) {
            if ($$3 == 8) {
               return 4352;
            }

            if ($$3 == 16) {
               return 4353;
            }
         } else if ($$2 == 2) {
            if ($$3 == 8) {
               return 4354;
            }

            if ($$3 == 16) {
               return 4355;
            }
         }
      }

      throw new IllegalArgumentException("Invalid audio format: " + p_83790_);
   }
}
