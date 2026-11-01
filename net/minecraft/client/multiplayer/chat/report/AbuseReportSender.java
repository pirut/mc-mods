package net.minecraft.client.multiplayer.chat.report;

import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.exceptions.MinecraftClientHttpException;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.minecraft.report.AbuseReport;
import com.mojang.authlib.minecraft.report.AbuseReportLimits;
import com.mojang.authlib.yggdrasil.request.AbuseReportRequest;
import com.mojang.datafixers.util.Unit;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ThrowingComponent;
import net.minecraft.util.Util;

@Environment(EnvType.CLIENT)
public interface AbuseReportSender {
   static AbuseReportSender create(ReportEnvironment environment, UserApiService userApiService) {
      return new AbuseReportSender.Services(environment, userApiService);
   }

   CompletableFuture<Unit> send(UUID id, ReportType reportType, AbuseReport report);

   boolean isEnabled();

   default AbuseReportLimits reportLimits() {
      return AbuseReportLimits.DEFAULTS;
   }

   @Environment(EnvType.CLIENT)
   public static class SendException extends ThrowingComponent {
      public SendException(Component component, Throwable throwable) {
         super(component, throwable);
      }
   }

   @Environment(EnvType.CLIENT)
   public record Services(ReportEnvironment environment, UserApiService userApiService) implements AbuseReportSender {
      private static final Component SERVICE_UNAVAILABLE_TEXT = Component.translatable("gui.abuseReport.send.service_unavailable");
      private static final Component HTTP_ERROR_TEXT = Component.translatable("gui.abuseReport.send.http_error");
      private static final Component JSON_ERROR_TEXT = Component.translatable("gui.abuseReport.send.json_error");

      @Override
      public CompletableFuture<Unit> send(UUID id, ReportType reportType, AbuseReport report) {
         return CompletableFuture.supplyAsync(
            () -> {
               AbuseReportRequest abuseReportRequest = new AbuseReportRequest(
                  1, id, report, this.environment.clientInfo(), this.environment.thirdPartyServerInfo(), this.environment.realmInfo(), reportType.backendName()
               );

               try {
                  this.userApiService.reportAbuse(abuseReportRequest);
                  return Unit.INSTANCE;
               } catch (MinecraftClientHttpException var7) {
                  Component component = this.getHttpErrorDescription(var7);
                  throw new CompletionException(new AbuseReportSender.SendException(component, var7));
               } catch (MinecraftClientException var8) {
                  Component componentx = this.getErrorDescription(var8);
                  throw new CompletionException(new AbuseReportSender.SendException(componentx, var8));
               }
            },
            Util.ioPool()
         );
      }

      @Override
      public boolean isEnabled() {
         return this.userApiService.canSendReports();
      }

      private Component getHttpErrorDescription(MinecraftClientHttpException httpException) {
         return Component.translatable("gui.abuseReport.send.error_message", httpException.getMessage());
      }

      private Component getErrorDescription(MinecraftClientException exception) {
         return switch (exception.getType()) {
            case SERVICE_UNAVAILABLE -> SERVICE_UNAVAILABLE_TEXT;
            case HTTP_ERROR -> HTTP_ERROR_TEXT;
            case JSON_ERROR -> JSON_ERROR_TEXT;
            default -> throw new MatchException(null, null);
         };
      }

      @Override
      public AbuseReportLimits reportLimits() {
         return this.userApiService.getAbuseReportLimits();
      }
   }
}
