package net.minecraft.client.sounds;

import it.unimi.dsi.fastutil.floats.FloatConsumer;
import java.io.IOException;
import java.nio.ByteBuffer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface FloatSampleSource extends FiniteAudioStream {
   int EXPECTED_MAX_FRAME_SIZE = 8192;

   boolean readChunk(FloatConsumer var1) throws IOException;

   @Override
   default ByteBuffer read(int p_341345_) throws IOException {
      ChunkedSampleByteBuf $$1 = new ChunkedSampleByteBuf(p_341345_ + 8192);

      while (this.readChunk($$1) && $$1.size() < p_341345_) {
      }

      return $$1.get();
   }

   @Override
   default ByteBuffer readAll() throws IOException {
      ChunkedSampleByteBuf $$0 = new ChunkedSampleByteBuf(16384);

      while (this.readChunk($$0)) {
      }

      return $$0.get();
   }
}
