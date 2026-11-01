package net.minecraft.client.telemetry;

import com.google.common.base.Suppliers;
import com.mojang.authlib.minecraft.TelemetrySession;
import com.mojang.authlib.minecraft.UserApiService;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientTelemetryManager implements AutoCloseable {
   private static final AtomicInteger THREAD_COUNT = new AtomicInteger(1);
   private static final Executor EXECUTOR = Executors.newSingleThreadExecutor(p_261485_ -> {
      Thread $$1 = new Thread(p_261485_);
      $$1.setName("Telemetry-Sender-#" + THREAD_COUNT.getAndIncrement());
      return $$1;
   });
   private final Minecraft minecraft;
   private final UserApiService userApiService;
   private final TelemetryPropertyMap deviceSessionProperties;
   private final Path logDirectory;
   private final CompletableFuture<Optional<TelemetryLogManager>> logManager;
   private final Supplier<TelemetryEventSender> outsideSessionSender = Suppliers.memoize(this::createEventSender);

   public ClientTelemetryManager(Minecraft p_261610_, UserApiService p_261552_, User p_262159_) {
      this.minecraft = p_261610_;
      this.userApiService = p_261552_;
      TelemetryPropertyMap.Builder $$3 = TelemetryPropertyMap.builder();
      p_262159_.getXuid().ifPresent(p_261810_ -> $$3.put(TelemetryProperty.USER_ID, p_261810_));
      p_262159_.getClientId().ifPresent(p_261690_ -> $$3.put(TelemetryProperty.CLIENT_ID, p_261690_));
      $$3.put(TelemetryProperty.MINECRAFT_SESSION_ID, UUID.randomUUID());
      $$3.put(TelemetryProperty.GAME_VERSION, SharedConstants.getCurrentVersion().getId());
      $$3.put(TelemetryProperty.OPERATING_SYSTEM, Util.getPlatform().telemetryName());
      $$3.put(TelemetryProperty.PLATFORM, System.getProperty("os.name"));
      $$3.put(TelemetryProperty.CLIENT_MODDED, Minecraft.checkModStatus().shouldReportAsModified());
      $$3.putIfNotNull(TelemetryProperty.LAUNCHER_NAME, Minecraft.getLauncherBrand());
      this.deviceSessionProperties = $$3.build();
      this.logDirectory = p_261610_.gameDirectory.toPath().resolve("logs/telemetry");
      this.logManager = TelemetryLogManager.open(this.logDirectory);
   }

   public WorldSessionTelemetryManager createWorldSessionManager(boolean p_286373_, @Nullable Duration p_286752_, @Nullable String p_286568_) {
      return new WorldSessionTelemetryManager(this.createEventSender(), p_286373_, p_286752_, p_286568_);
   }

   public TelemetryEventSender getOutsideSessionSender() {
      return this.outsideSessionSender.get();
   }

   private TelemetryEventSender createEventSender() {
      if (!this.minecraft.allowsTelemetry()) {
         return TelemetryEventSender.DISABLED;
      } else {
         TelemetrySession $$0 = this.userApiService.newTelemetrySession(EXECUTOR);
         if (!$$0.isEnabled()) {
            return TelemetryEventSender.DISABLED;
         } else {
            CompletableFuture<Optional<TelemetryEventLogger>> $$1 = this.logManager
               .thenCompose(p_261737_ -> p_261737_.map(TelemetryLogManager::openLogger).orElseGet(() -> CompletableFuture.completedFuture(Optional.empty())));
            return (p_261827_, p_261818_) -> {
               if (!p_261827_.isOptIn() || Minecraft.getInstance().telemetryOptInExtra()) {
                  TelemetryPropertyMap.Builder $$4 = TelemetryPropertyMap.builder();
                  $$4.putAll(this.deviceSessionProperties);
                  $$4.put(TelemetryProperty.EVENT_TIMESTAMP_UTC, Instant.now());
                  $$4.put(TelemetryProperty.OPT_IN, p_261827_.isOptIn());
                  p_261818_.accept($$4);
                  TelemetryEventInstance $$5 = new TelemetryEventInstance(p_261827_, $$4.build());
                  $$1.thenAccept(p_262038_ -> {
                     if (!p_262038_.isEmpty()) {
                        p_262038_.get().log($$5);
                        $$5.export($$0).send();
                     }
                  });
               }
            };
         }
      }
   }

   public Path getLogDirectory() {
      return this.logDirectory;
   }

   @Override
   public void close() {
      this.logManager.thenAccept(p_261643_ -> p_261643_.ifPresent(TelemetryLogManager::close));
   }
}
