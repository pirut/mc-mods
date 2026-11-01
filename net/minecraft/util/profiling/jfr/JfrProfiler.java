package net.minecraft.util.profiling.jfr;

import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.SocketAddress;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import jdk.jfr.Configuration;
import jdk.jfr.Event;
import jdk.jfr.FlightRecorder;
import jdk.jfr.FlightRecorderListener;
import jdk.jfr.Recording;
import jdk.jfr.RecordingState;
import net.minecraft.FileUtil;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.profiling.jfr.callback.ProfiledDuration;
import net.minecraft.util.profiling.jfr.event.ChunkGenerationEvent;
import net.minecraft.util.profiling.jfr.event.ChunkRegionReadEvent;
import net.minecraft.util.profiling.jfr.event.ChunkRegionWriteEvent;
import net.minecraft.util.profiling.jfr.event.NetworkSummaryEvent;
import net.minecraft.util.profiling.jfr.event.PacketReceivedEvent;
import net.minecraft.util.profiling.jfr.event.PacketSentEvent;
import net.minecraft.util.profiling.jfr.event.ServerTickTimeEvent;
import net.minecraft.util.profiling.jfr.event.WorldLoadFinishedEvent;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.storage.RegionFileVersion;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import org.slf4j.Logger;

public class JfrProfiler implements JvmProfiler {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final String ROOT_CATEGORY = "Minecraft";
   public static final String WORLD_GEN_CATEGORY = "World Generation";
   public static final String TICK_CATEGORY = "Ticking";
   public static final String NETWORK_CATEGORY = "Network";
   public static final String STORAGE_CATEGORY = "Storage";
   private static final List<Class<? extends Event>> CUSTOM_EVENTS = List.of(
      ChunkGenerationEvent.class,
      ChunkRegionReadEvent.class,
      ChunkRegionWriteEvent.class,
      PacketReceivedEvent.class,
      PacketSentEvent.class,
      NetworkSummaryEvent.class,
      ServerTickTimeEvent.class,
      WorldLoadFinishedEvent.class
   );
   private static final String FLIGHT_RECORDER_CONFIG = "/flightrecorder-config.jfc";
   private static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
      .appendPattern("yyyy-MM-dd-HHmmss")
      .toFormatter()
      .withZone(ZoneId.systemDefault());
   private static final JfrProfiler INSTANCE = new JfrProfiler();
   @Nullable
   Recording recording;
   private float currentAverageTickTime;
   private final Map<String, NetworkSummaryEvent.SumAggregation> networkTrafficByAddress = new ConcurrentHashMap<>();

   private JfrProfiler() {
      CUSTOM_EVENTS.forEach(FlightRecorder::register);
      FlightRecorder.addPeriodicEvent(ServerTickTimeEvent.class, () -> new ServerTickTimeEvent(this.currentAverageTickTime).commit());
      FlightRecorder.addPeriodicEvent(NetworkSummaryEvent.class, () -> {
         Iterator<NetworkSummaryEvent.SumAggregation> $$0 = this.networkTrafficByAddress.values().iterator();

         while ($$0.hasNext()) {
            $$0.next().commitEvent();
            $$0.remove();
         }
      });
   }

   public static JfrProfiler getInstance() {
      return INSTANCE;
   }

   @Override
   public boolean start(Environment p_185307_) {
      URL $$1 = JfrProfiler.class.getResource("/flightrecorder-config.jfc");
      if ($$1 == null) {
         LOGGER.warn("Could not find default flight recorder config at {}", "/flightrecorder-config.jfc");
         return false;
      } else {
         try {
            boolean var4;
            try (BufferedReader $$2 = new BufferedReader(new InputStreamReader($$1.openStream()))) {
               var4 = this.start($$2, p_185307_);
            }

            return var4;
         } catch (IOException var8) {
            LOGGER.warn("Failed to start flight recorder using configuration at {}", $$1, var8);
            return false;
         }
      }
   }

   @Override
   public Path stop() {
      if (this.recording == null) {
         throw new IllegalStateException("Not currently profiling");
      } else {
         this.networkTrafficByAddress.clear();
         Path $$0 = this.recording.getDestination();
         this.recording.stop();
         return $$0;
      }
   }

   @Override
   public boolean isRunning() {
      return this.recording != null;
   }

   @Override
   public boolean isAvailable() {
      return FlightRecorder.isAvailable();
   }

   private boolean start(Reader p_185317_, Environment p_185318_) {
      if (this.isRunning()) {
         LOGGER.warn("Profiling already in progress");
         return false;
      } else {
         try {
            Configuration $$2 = Configuration.create(p_185317_);
            String $$3 = DATE_TIME_FORMATTER.format(Instant.now());
            this.recording = Util.make(new Recording($$2), p_185311_ -> {
               CUSTOM_EVENTS.forEach(p_185311_::enable);
               p_185311_.setDumpOnExit(true);
               p_185311_.setToDisk(true);
               p_185311_.setName(String.format(Locale.ROOT, "%s-%s-%s", p_185318_.getDescription(), SharedConstants.getCurrentVersion().getName(), $$3));
            });
            Path $$4 = Paths.get(String.format(Locale.ROOT, "debug/%s-%s.jfr", p_185318_.getDescription(), $$3));
            FileUtil.createDirectoriesSafe($$4.getParent());
            this.recording.setDestination($$4);
            this.recording.start();
            this.setupSummaryListener();
         } catch (ParseException | IOException var6) {
            LOGGER.warn("Failed to start jfr profiling", var6);
            return false;
         }

         LOGGER.info(
            "Started flight recorder profiling id({}):name({}) - will dump to {} on exit or stop command",
            new Object[]{this.recording.getId(), this.recording.getName(), this.recording.getDestination()}
         );
         return true;
      }
   }

   private void setupSummaryListener() {
      FlightRecorder.addListener(new FlightRecorderListener() {
         final SummaryReporter summaryReporter = new SummaryReporter(() -> JfrProfiler.this.recording = null);

         @Override
         public void recordingStateChanged(Recording p_185339_) {
            if (p_185339_ == JfrProfiler.this.recording && p_185339_.getState() == RecordingState.STOPPED) {
               this.summaryReporter.recordingStopped(p_185339_.getDestination());
               FlightRecorder.removeListener(this);
            }
         }
      });
   }

   @Override
   public void onServerTick(float p_185300_) {
      if (ServerTickTimeEvent.TYPE.isEnabled()) {
         this.currentAverageTickTime = p_185300_;
      }
   }

   @Override
   public void onPacketReceived(ConnectionProtocol p_294286_, PacketType<?> p_320639_, SocketAddress p_185304_, int p_185302_) {
      if (PacketReceivedEvent.TYPE.isEnabled()) {
         new PacketReceivedEvent(p_294286_.id(), p_320639_.flow().id(), p_320639_.id().toString(), p_185304_, p_185302_).commit();
      }

      if (NetworkSummaryEvent.TYPE.isEnabled()) {
         this.networkStatFor(p_185304_).trackReceivedPacket(p_185302_);
      }
   }

   @Override
   public void onPacketSent(ConnectionProtocol p_295940_, PacketType<?> p_320751_, SocketAddress p_185325_, int p_185323_) {
      if (PacketSentEvent.TYPE.isEnabled()) {
         new PacketSentEvent(p_295940_.id(), p_320751_.flow().id(), p_320751_.id().toString(), p_185325_, p_185323_).commit();
      }

      if (NetworkSummaryEvent.TYPE.isEnabled()) {
         this.networkStatFor(p_185325_).trackSentPacket(p_185323_);
      }
   }

   private NetworkSummaryEvent.SumAggregation networkStatFor(SocketAddress p_185320_) {
      return this.networkTrafficByAddress.computeIfAbsent(p_185320_.toString(), NetworkSummaryEvent.SumAggregation::new);
   }

   @Override
   public void onRegionFileRead(RegionStorageInfo p_326253_, ChunkPos p_326199_, RegionFileVersion p_326089_, int p_325934_) {
      if (ChunkRegionReadEvent.TYPE.isEnabled()) {
         new ChunkRegionReadEvent(p_326253_, p_326199_, p_326089_, p_325934_).commit();
      }
   }

   @Override
   public void onRegionFileWrite(RegionStorageInfo p_326009_, ChunkPos p_326210_, RegionFileVersion p_326516_, int p_326455_) {
      if (ChunkRegionWriteEvent.TYPE.isEnabled()) {
         new ChunkRegionWriteEvent(p_326009_, p_326210_, p_326516_, p_326455_).commit();
      }
   }

   @Nullable
   @Override
   public ProfiledDuration onWorldLoadedStarted() {
      if (!WorldLoadFinishedEvent.TYPE.isEnabled()) {
         return null;
      } else {
         WorldLoadFinishedEvent $$0 = new WorldLoadFinishedEvent();
         $$0.begin();
         return $$0::commit;
      }
   }

   @Nullable
   @Override
   public ProfiledDuration onChunkGenerate(ChunkPos p_185313_, ResourceKey<Level> p_185314_, String p_185315_) {
      if (!ChunkGenerationEvent.TYPE.isEnabled()) {
         return null;
      } else {
         ChunkGenerationEvent $$3 = new ChunkGenerationEvent(p_185313_, p_185314_, p_185315_);
         $$3.begin();
         return $$3::commit;
      }
   }
}
