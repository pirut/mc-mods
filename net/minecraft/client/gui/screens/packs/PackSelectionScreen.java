package net.minecraft.client.gui.screens.packs;

import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.NoticeWithLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackDetector;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.world.level.validation.ForbiddenSymlinkInfo;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class PackSelectionScreen extends Screen {
   static final Logger LOGGER = LogUtils.getLogger();
   private static final Component AVAILABLE_TITLE = Component.translatable("pack.available.title");
   private static final Component SELECTED_TITLE = Component.translatable("pack.selected.title");
   private static final Component OPEN_PACK_FOLDER_TITLE = Component.translatable("pack.openFolder");
   private static final int LIST_WIDTH = 200;
   private static final Component DRAG_AND_DROP = Component.translatable("pack.dropInfo").withStyle(ChatFormatting.GRAY);
   private static final Component DIRECTORY_BUTTON_TOOLTIP = Component.translatable("pack.folderInfo");
   private static final int RELOAD_COOLDOWN = 20;
   private static final ResourceLocation DEFAULT_ICON = ResourceLocation.withDefaultNamespace("textures/misc/unknown_pack.png");
   private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
   private final PackSelectionModel model;
   @Nullable
   private PackSelectionScreen.Watcher watcher;
   private long ticksToReload;
   private TransferableSelectionList availablePackList;
   private TransferableSelectionList selectedPackList;
   private final Path packDir;
   private Button doneButton;
   private final Map<String, ResourceLocation> packIcons = Maps.newHashMap();

   public PackSelectionScreen(PackRepository p_275398_, Consumer<PackRepository> p_275659_, Path p_275522_, Component p_275337_) {
      super(p_275337_);
      this.model = new PackSelectionModel(this::populateLists, this::getPackIcon, p_275398_, p_275659_);
      this.packDir = p_275522_;
      this.watcher = PackSelectionScreen.Watcher.create(p_275522_);
   }

   @Override
   public void onClose() {
      this.model.commit();
      this.closeWatcher();
   }

   private void closeWatcher() {
      if (this.watcher != null) {
         try {
            this.watcher.close();
            this.watcher = null;
         } catch (Exception var2) {
         }
      }
   }

   @Override
   protected void init() {
      LinearLayout $$0 = this.layout.addToHeader(LinearLayout.vertical().spacing(5));
      $$0.defaultCellSetting().alignHorizontallyCenter();
      $$0.addChild(new StringWidget(this.getTitle(), this.font));
      $$0.addChild(new StringWidget(DRAG_AND_DROP, this.font));
      this.availablePackList = this.addRenderableWidget(new TransferableSelectionList(this.minecraft, this, 200, this.height - 66, AVAILABLE_TITLE));
      this.selectedPackList = this.addRenderableWidget(new TransferableSelectionList(this.minecraft, this, 200, this.height - 66, SELECTED_TITLE));
      LinearLayout $$1 = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
      $$1.addChild(
         Button.builder(OPEN_PACK_FOLDER_TITLE, p_351663_ -> Util.getPlatform().openPath(this.packDir))
            .tooltip(Tooltip.create(DIRECTORY_BUTTON_TOOLTIP))
            .build()
      );
      this.doneButton = $$1.addChild(Button.builder(CommonComponents.GUI_DONE, p_100036_ -> this.onClose()).build());
      this.reload();
      this.layout.visitWidgets(p_329736_ -> {
         AbstractWidget var10000 = this.addRenderableWidget(p_329736_);
      });
      this.repositionElements();
   }

   @Override
   protected void repositionElements() {
      this.layout.arrangeElements();
      this.availablePackList.updateSize(200, this.layout);
      this.availablePackList.setX(this.width / 2 - 15 - 200);
      this.selectedPackList.updateSize(200, this.layout);
      this.selectedPackList.setX(this.width / 2 + 15);
   }

   @Override
   public void tick() {
      if (this.watcher != null) {
         try {
            if (this.watcher.pollForChanges()) {
               this.ticksToReload = 20L;
            }
         } catch (IOException var2) {
            LOGGER.warn("Failed to poll for directory {} changes, stopping", this.packDir);
            this.closeWatcher();
         }
      }

      if (this.ticksToReload > 0L && --this.ticksToReload == 0L) {
         this.reload();
      }
   }

   private void populateLists() {
      this.updateList(this.selectedPackList, this.model.getSelected());
      this.updateList(this.availablePackList, this.model.getUnselected());
      this.doneButton.active = !this.selectedPackList.children().isEmpty();
   }

   private void updateList(TransferableSelectionList p_100014_, Stream<PackSelectionModel.Entry> p_100015_) {
      p_100014_.children().clear();
      TransferableSelectionList.PackEntry $$2 = p_100014_.getSelected();
      String $$3 = $$2 == null ? "" : $$2.getPackId();
      p_100014_.setSelected(null);
      p_100015_.forEach(p_351662_ -> {
         TransferableSelectionList.PackEntry $$3x = new TransferableSelectionList.PackEntry(this.minecraft, p_100014_, p_351662_);
         p_100014_.children().add($$3x);
         if (p_351662_.getId().equals($$3)) {
            p_100014_.setSelected($$3x);
         }
      });
   }

   public void updateFocus(TransferableSelectionList p_265419_) {
      TransferableSelectionList $$1 = this.selectedPackList == p_265419_ ? this.availablePackList : this.selectedPackList;
      this.changeFocus(ComponentPath.path($$1.getFirstElement(), $$1, this));
   }

   public void clearSelected() {
      this.selectedPackList.setSelected(null);
      this.availablePackList.setSelected(null);
   }

   private void reload() {
      this.model.findNewPacks();
      this.populateLists();
      this.ticksToReload = 0L;
      this.packIcons.clear();
   }

   protected static void copyPacks(Minecraft p_100000_, List<Path> p_100001_, Path p_100002_) {
      MutableBoolean $$3 = new MutableBoolean();
      p_100001_.forEach(p_170009_ -> {
         try (Stream<Path> $$3x = Files.walk(p_170009_)) {
            $$3x.forEach(p_170005_ -> {
               try {
                  Util.copyBetweenDirs(p_170009_.getParent(), p_100002_, p_170005_);
               } catch (IOException var5) {
                  LOGGER.warn("Failed to copy datapack file  from {} to {}", new Object[]{p_170005_, p_100002_, var5});
                  $$3.setTrue();
               }
            });
         } catch (IOException var8) {
            LOGGER.warn("Failed to copy datapack file from {} to {}", p_170009_, p_100002_);
            $$3.setTrue();
         }
      });
      if ($$3.isTrue()) {
         SystemToast.onPackCopyFailure(p_100000_, p_100002_.toString());
      }
   }

   @Override
   public void onFilesDrop(List<Path> p_100029_) {
      String $$1 = extractPackNames(p_100029_).collect(Collectors.joining(", "));
      this.minecraft
         .setScreen(
            new ConfirmScreen(
               p_293606_ -> {
                  if (p_293606_) {
                     List<Path> $$2 = new ArrayList<>(p_100029_.size());
                     Set<Path> $$3 = new HashSet<>(p_100029_);
                     PackDetector<Path> $$4 = new PackDetector<Path>(this.minecraft.directoryValidator()) {
                        protected Path createZipPack(Path p_294508_) {
                           return p_294508_;
                        }

                        protected Path createDirectoryPack(Path p_296022_) {
                           return p_296022_;
                        }
                     };
                     List<ForbiddenSymlinkInfo> $$5 = new ArrayList<>();

                     for (Path $$6 : p_100029_) {
                        try {
                           Path $$7 = $$4.detectPackResources($$6, $$5);
                           if ($$7 == null) {
                              LOGGER.warn("Path {} does not seem like pack", $$6);
                           } else {
                              $$2.add($$7);
                              $$3.remove($$7);
                           }
                        } catch (IOException var10) {
                           LOGGER.warn("Failed to check {} for packs", $$6, var10);
                        }
                     }

                     if (!$$5.isEmpty()) {
                        this.minecraft.setScreen(NoticeWithLinkScreen.createPackSymlinkWarningScreen(() -> this.minecraft.setScreen(this)));
                        return;
                     }

                     if (!$$2.isEmpty()) {
                        copyPacks(this.minecraft, $$2, this.packDir);
                        this.reload();
                     }

                     if (!$$3.isEmpty()) {
                        String $$9 = extractPackNames($$3).collect(Collectors.joining(", "));
                        this.minecraft
                           .setScreen(
                              new AlertScreen(
                                 () -> this.minecraft.setScreen(this),
                                 Component.translatable("pack.dropRejected.title"),
                                 Component.translatable("pack.dropRejected.message", $$9)
                              )
                           );
                        return;
                     }
                  }

                  this.minecraft.setScreen(this);
               },
               Component.translatable("pack.dropConfirm"),
               Component.literal($$1)
            )
         );
   }

   private static Stream<String> extractPackNames(Collection<Path> p_296218_) {
      return p_296218_.stream().map(Path::getFileName).map(Path::toString);
   }

   private ResourceLocation loadPackIcon(TextureManager p_100017_, Pack p_100018_) {
      try {
         ResourceLocation var9;
         try (PackResources $$2 = p_100018_.open()) {
            IoSupplier<InputStream> $$3 = $$2.getRootResource("pack.png");
            if ($$3 == null) {
               return DEFAULT_ICON;
            }

            String $$4 = p_100018_.getId();
            ResourceLocation $$5 = ResourceLocation.withDefaultNamespace(
               "pack/" + Util.sanitizeName($$4, ResourceLocation::validPathChar) + "/" + Hashing.sha1().hashUnencodedChars($$4) + "/icon"
            );

            try (InputStream $$6 = $$3.get()) {
               NativeImage $$7 = NativeImage.read($$6);
               p_100017_.register($$5, new DynamicTexture($$7));
               var9 = $$5;
            }
         }

         return var9;
      } catch (Exception var14) {
         LOGGER.warn("Failed to load icon from pack {}", p_100018_.getId(), var14);
         return DEFAULT_ICON;
      }
   }

   private ResourceLocation getPackIcon(Pack p_99990_) {
      return this.packIcons.computeIfAbsent(p_99990_.getId(), p_280879_ -> this.loadPackIcon(this.minecraft.getTextureManager(), p_99990_));
   }

   @OnlyIn(Dist.CLIENT)
   static class Watcher implements AutoCloseable {
      private final WatchService watcher;
      private final Path packPath;

      public Watcher(Path p_250327_) throws IOException {
         this.packPath = p_250327_;
         this.watcher = p_250327_.getFileSystem().newWatchService();

         try {
            this.watchDir(p_250327_);

            try (DirectoryStream<Path> $$1 = Files.newDirectoryStream(p_250327_)) {
               for (Path $$2 : $$1) {
                  if (Files.isDirectory($$2, LinkOption.NOFOLLOW_LINKS)) {
                     this.watchDir($$2);
                  }
               }
            }
         } catch (Exception var7) {
            this.watcher.close();
            throw var7;
         }
      }

      @Nullable
      public static PackSelectionScreen.Watcher create(Path p_252119_) {
         try {
            return new PackSelectionScreen.Watcher(p_252119_);
         } catch (IOException var2) {
            PackSelectionScreen.LOGGER.warn("Failed to initialize pack directory {} monitoring", p_252119_, var2);
            return null;
         }
      }

      private void watchDir(Path p_100050_) throws IOException {
         p_100050_.register(this.watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
      }

      public boolean pollForChanges() throws IOException {
         boolean $$0 = false;

         WatchKey $$1;
         while (($$1 = this.watcher.poll()) != null) {
            for (WatchEvent<?> $$3 : $$1.pollEvents()) {
               $$0 = true;
               if ($$1.watchable() == this.packPath && $$3.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                  Path $$4 = this.packPath.resolve((Path)$$3.context());
                  if (Files.isDirectory($$4, LinkOption.NOFOLLOW_LINKS)) {
                     this.watchDir($$4);
                  }
               }
            }

            $$1.reset();
         }

         return $$0;
      }

      @Override
      public void close() throws IOException {
         this.watcher.close();
      }
   }
}
