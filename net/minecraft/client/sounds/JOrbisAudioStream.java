package net.minecraft.client.sounds;

import com.jcraft.jogg.Packet;
import com.jcraft.jogg.Page;
import com.jcraft.jogg.StreamState;
import com.jcraft.jogg.SyncState;
import com.jcraft.jorbis.Block;
import com.jcraft.jorbis.Comment;
import com.jcraft.jorbis.DspState;
import com.jcraft.jorbis.Info;
import it.unimi.dsi.fastutil.floats.FloatConsumer;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Nullable;
import javax.sound.sampled.AudioFormat;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class JOrbisAudioStream implements FloatSampleSource {
   private static final int BUFSIZE = 8192;
   private static final int PAGEOUT_RECAPTURE = -1;
   private static final int PAGEOUT_NEED_MORE_DATA = 0;
   private static final int PAGEOUT_OK = 1;
   private static final int PACKETOUT_ERROR = -1;
   private static final int PACKETOUT_NEED_MORE_DATA = 0;
   private static final int PACKETOUT_OK = 1;
   private final SyncState syncState = new SyncState();
   private final Page page = new Page();
   private final StreamState streamState = new StreamState();
   private final Packet packet = new Packet();
   private final Info info = new Info();
   private final DspState dspState = new DspState();
   private final Block block = new Block(this.dspState);
   private final AudioFormat audioFormat;
   private final InputStream input;
   private long samplesWritten;
   private long totalSamplesInStream = Long.MAX_VALUE;

   public JOrbisAudioStream(InputStream p_341241_) throws IOException {
      this.input = p_341241_;
      Comment $$1 = new Comment();
      Page $$2 = this.readPage();
      if ($$2 == null) {
         throw new IOException("Invalid Ogg file - can't find first page");
      } else {
         Packet $$3 = this.readIdentificationPacket($$2);
         if (isError(this.info.synthesis_headerin($$1, $$3))) {
            throw new IOException("Invalid Ogg identification packet");
         } else {
            for (int $$4 = 0; $$4 < 2; $$4++) {
               $$3 = this.readPacket();
               if ($$3 == null) {
                  throw new IOException("Unexpected end of Ogg stream");
               }

               if (isError(this.info.synthesis_headerin($$1, $$3))) {
                  throw new IOException("Invalid Ogg header packet " + $$4);
               }
            }

            this.dspState.synthesis_init(this.info);
            this.block.init(this.dspState);
            this.audioFormat = new AudioFormat(this.info.rate, 16, this.info.channels, true, false);
         }
      }
   }

   private static boolean isError(int p_340842_) {
      return p_340842_ < 0;
   }

   @Override
   public AudioFormat getFormat() {
      return this.audioFormat;
   }

   private boolean readToBuffer() throws IOException {
      int $$0 = this.syncState.buffer(8192);
      byte[] $$1 = this.syncState.data;
      int $$2 = this.input.read($$1, $$0, 8192);
      if ($$2 == -1) {
         return false;
      } else {
         this.syncState.wrote($$2);
         return true;
      }
   }

   @Nullable
   private Page readPage() throws IOException {
      while (true) {
         int $$0 = this.syncState.pageout(this.page);
         switch ($$0) {
            case -1:
               throw new IllegalStateException("Corrupt or missing data in bitstream");
            case 0:
               if (this.readToBuffer()) {
                  break;
               }

               return null;
            case 1:
               if (this.page.eos() != 0) {
                  this.totalSamplesInStream = this.page.granulepos();
               }

               return this.page;
            default:
               throw new IllegalStateException("Unknown page decode result: " + $$0);
         }
      }
   }

   private Packet readIdentificationPacket(Page p_340874_) throws IOException {
      this.streamState.init(p_340874_.serialno());
      if (isError(this.streamState.pagein(p_340874_))) {
         throw new IOException("Failed to parse page");
      } else {
         int $$1 = this.streamState.packetout(this.packet);
         if ($$1 != 1) {
            throw new IOException("Failed to read identification packet: " + $$1);
         } else {
            return this.packet;
         }
      }
   }

   @Nullable
   private Packet readPacket() throws IOException {
      while (true) {
         int $$0 = this.streamState.packetout(this.packet);
         switch ($$0) {
            case -1:
               throw new IOException("Failed to parse packet");
            case 0:
               Page $$1 = this.readPage();
               if ($$1 == null) {
                  return null;
               }

               if (!isError(this.streamState.pagein($$1))) {
                  break;
               }

               throw new IOException("Failed to parse page");
            case 1:
               return this.packet;
            default:
               throw new IllegalStateException("Unknown packet decode result: " + $$0);
         }
      }
   }

   private long getSamplesToWrite(int p_341351_) {
      long $$1 = this.samplesWritten + p_341351_;
      long $$2;
      if ($$1 > this.totalSamplesInStream) {
         $$2 = this.totalSamplesInStream - this.samplesWritten;
         this.samplesWritten = this.totalSamplesInStream;
      } else {
         this.samplesWritten = $$1;
         $$2 = p_341351_;
      }

      return $$2;
   }

   @Override
   public boolean readChunk(FloatConsumer p_341270_) throws IOException {
      float[][][] $$1 = new float[1][][];
      int[] $$2 = new int[this.info.channels];
      Packet $$3 = this.readPacket();
      if ($$3 == null) {
         return false;
      } else if (isError(this.block.synthesis($$3))) {
         throw new IOException("Can't decode audio packet");
      } else {
         this.dspState.synthesis_blockin(this.block);

         int $$4;
         while (($$4 = this.dspState.synthesis_pcmout($$1, $$2)) > 0) {
            float[][] $$5 = $$1[0];
            long $$6 = this.getSamplesToWrite($$4);
            switch (this.info.channels) {
               case 1:
                  copyMono($$5[0], $$2[0], $$6, p_341270_);
                  break;
               case 2:
                  copyStereo($$5[0], $$2[0], $$5[1], $$2[1], $$6, p_341270_);
                  break;
               default:
                  copyAnyChannels($$5, this.info.channels, $$2, $$6, p_341270_);
            }

            this.dspState.synthesis_read($$4);
         }

         return true;
      }
   }

   private static void copyAnyChannels(float[][] p_340927_, int p_341332_, int[] p_341269_, long p_341072_, FloatConsumer p_341382_) {
      for (int $$5 = 0; $$5 < p_341072_; $$5++) {
         for (int $$6 = 0; $$6 < p_341332_; $$6++) {
            int $$7 = p_341269_[$$6];
            float $$8 = p_340927_[$$6][$$7 + $$5];
            p_341382_.accept($$8);
         }
      }
   }

   private static void copyMono(float[] p_340838_, int p_341058_, long p_341397_, FloatConsumer p_340852_) {
      for (int $$4 = p_341058_; $$4 < p_341058_ + p_341397_; $$4++) {
         p_340852_.accept(p_340838_[$$4]);
      }
   }

   private static void copyStereo(float[] p_341142_, int p_341000_, float[] p_340922_, int p_341132_, long p_341389_, FloatConsumer p_341076_) {
      for (int $$6 = 0; $$6 < p_341389_; $$6++) {
         p_341076_.accept(p_341142_[p_341000_ + $$6]);
         p_341076_.accept(p_340922_[p_341132_ + $$6]);
      }
   }

   @Override
   public void close() throws IOException {
      this.input.close();
   }
}
