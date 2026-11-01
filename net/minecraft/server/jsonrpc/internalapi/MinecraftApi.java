package net.minecraft.server.jsonrpc.internalapi;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.jsonrpc.JsonRpcLogger;
import net.minecraft.server.notifications.NotificationManager;

public class MinecraftApi {
   private final NotificationManager notificationManager;
   private final MinecraftAllowListService allowListService;
   private final MinecraftBanListService banListService;
   private final MinecraftPlayerListService minecraftPlayerListService;
   private final MinecraftGameRuleService gameRuleService;
   private final MinecraftOperatorListService minecraftOperatorListService;
   private final MinecraftServerSettingsService minecraftServerSettingsService;
   private final MinecraftServerStateService minecraftServerStateService;
   private final MinecraftExecutorService executorService;

   public MinecraftApi(
      NotificationManager p_449655_,
      MinecraftAllowListService p_443286_,
      MinecraftBanListService p_443467_,
      MinecraftPlayerListService p_442735_,
      MinecraftGameRuleService p_442736_,
      MinecraftOperatorListService p_443566_,
      MinecraftServerSettingsService p_443225_,
      MinecraftServerStateService p_443354_,
      MinecraftExecutorService p_449378_
   ) {
      this.notificationManager = p_449655_;
      this.allowListService = p_443286_;
      this.banListService = p_443467_;
      this.minecraftPlayerListService = p_442735_;
      this.gameRuleService = p_442736_;
      this.minecraftOperatorListService = p_443566_;
      this.minecraftServerSettingsService = p_443225_;
      this.minecraftServerStateService = p_443354_;
      this.executorService = p_449378_;
   }

   public <V> CompletableFuture<V> submit(Supplier<V> p_442544_) {
      return this.executorService.submit(p_442544_);
   }

   public CompletableFuture<Void> submit(Runnable p_443556_) {
      return this.executorService.submit(p_443556_);
   }

   public MinecraftAllowListService allowListService() {
      return this.allowListService;
   }

   public MinecraftBanListService banListService() {
      return this.banListService;
   }

   public MinecraftPlayerListService playerListService() {
      return this.minecraftPlayerListService;
   }

   public MinecraftGameRuleService gameRuleService() {
      return this.gameRuleService;
   }

   public MinecraftOperatorListService operatorListService() {
      return this.minecraftOperatorListService;
   }

   public MinecraftServerSettingsService serverSettingsService() {
      return this.minecraftServerSettingsService;
   }

   public MinecraftServerStateService serverStateService() {
      return this.minecraftServerStateService;
   }

   public NotificationManager notificationManager() {
      return this.notificationManager;
   }

   public static MinecraftApi of(DedicatedServer p_449765_) {
      JsonRpcLogger jsonrpclogger = new JsonRpcLogger();
      MinecraftAllowListServiceImpl minecraftallowlistserviceimpl = new MinecraftAllowListServiceImpl(p_449765_, jsonrpclogger);
      MinecraftBanListServiceImpl minecraftbanlistserviceimpl = new MinecraftBanListServiceImpl(p_449765_, jsonrpclogger);
      MinecraftPlayerListServiceImpl minecraftplayerlistserviceimpl = new MinecraftPlayerListServiceImpl(p_449765_, jsonrpclogger);
      MinecraftGameRuleServiceImpl minecraftgameruleserviceimpl = new MinecraftGameRuleServiceImpl(p_449765_, jsonrpclogger);
      MinecraftOperatorListServiceImpl minecraftoperatorlistserviceimpl = new MinecraftOperatorListServiceImpl(p_449765_, jsonrpclogger);
      MinecraftServerSettingsServiceImpl minecraftserversettingsserviceimpl = new MinecraftServerSettingsServiceImpl(p_449765_, jsonrpclogger);
      MinecraftServerStateServiceImpl minecraftserverstateserviceimpl = new MinecraftServerStateServiceImpl(p_449765_, jsonrpclogger);
      MinecraftExecutorService minecraftexecutorservice = new MinecraftExecutorServiceImpl(p_449765_);
      return new MinecraftApi(
         p_449765_.notificationManager(),
         minecraftallowlistserviceimpl,
         minecraftbanlistserviceimpl,
         minecraftplayerlistserviceimpl,
         minecraftgameruleserviceimpl,
         minecraftoperatorlistserviceimpl,
         minecraftserversettingsserviceimpl,
         minecraftserverstateserviceimpl,
         minecraftexecutorservice
      );
   }
}
