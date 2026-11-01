package net.minecraft.client.gui.screens.inventory;

import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.FittingMultiLineTextWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ServerboundTestInstanceBlockActionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.TestInstanceBlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class TestInstanceBlockEditScreen extends Screen {
   private static final Component ID_LABEL = Component.translatable("test_instance_block.test_id");
   private static final Component SIZE_LABEL = Component.translatable("test_instance_block.size");
   private static final Component INCLUDE_ENTITIES_LABEL = Component.translatable("test_instance_block.entities");
   private static final Component ROTATION_LABEL = Component.translatable("test_instance_block.rotation");
   private static final int BUTTON_PADDING = 8;
   private static final int WIDTH = 316;
   private final TestInstanceBlockEntity blockEntity;
   @Nullable
   private EditBox idEdit;
   @Nullable
   private EditBox sizeXEdit;
   @Nullable
   private EditBox sizeYEdit;
   @Nullable
   private EditBox sizeZEdit;
   @Nullable
   private FittingMultiLineTextWidget infoWidget;
   @Nullable
   private Button saveButton;
   @Nullable
   private Button exportButton;
   @Nullable
   private CycleButton<Boolean> includeEntitiesButton;
   @Nullable
   private CycleButton<Rotation> rotationButton;

   public TestInstanceBlockEditScreen(TestInstanceBlockEntity p_397992_) {
      super(p_397992_.getBlockState().getBlock().getName());
      this.blockEntity = p_397992_;
   }

   @Override
   protected void init() {
      int i = this.width / 2 - 158;
      boolean flag = SharedConstants.IS_RUNNING_IN_IDE;
      int j = flag ? 3 : 2;
      int k = widgetSize(j);
      this.idEdit = new EditBox(this.font, i, 40, 316, 20, Component.translatable("test_instance_block.test_id"));
      this.idEdit.setMaxLength(128);
      Optional<ResourceKey<GameTestInstance>> optional = this.blockEntity.test();
      if (optional.isPresent()) {
         this.idEdit.setValue(optional.get().identifier().toString());
      }

      this.idEdit.setResponder(p_397657_ -> this.updateTestInfo(false));
      this.addRenderableWidget(this.idEdit);
      this.infoWidget = new FittingMultiLineTextWidget(i, 70, 316, 72, Component.literal(""), this.font);
      this.addRenderableWidget(this.infoWidget);
      Vec3i vec3i = this.blockEntity.getSize();
      int l = 0;
      this.sizeXEdit = new EditBox(this.font, this.widgetX(l++, 5), 160, widgetSize(5), 20, Component.translatable("structure_block.size.x"));
      this.sizeXEdit.setMaxLength(15);
      this.addRenderableWidget(this.sizeXEdit);
      this.sizeYEdit = new EditBox(this.font, this.widgetX(l++, 5), 160, widgetSize(5), 20, Component.translatable("structure_block.size.y"));
      this.sizeYEdit.setMaxLength(15);
      this.addRenderableWidget(this.sizeYEdit);
      this.sizeZEdit = new EditBox(this.font, this.widgetX(l++, 5), 160, widgetSize(5), 20, Component.translatable("structure_block.size.z"));
      this.sizeZEdit.setMaxLength(15);
      this.addRenderableWidget(this.sizeZEdit);
      this.setSize(vec3i);
      this.rotationButton = this.addRenderableWidget(
         CycleButton.builder(TestInstanceBlockEditScreen::rotationDisplay, this.blockEntity.getRotation())
            .withValues(Rotation.values())
            .displayOnlyValue()
            .create(this.widgetX(l++, 5), 160, widgetSize(5), 20, ROTATION_LABEL, (p_397117_, p_397434_) -> this.updateSaveState())
      );
      this.includeEntitiesButton = this.addRenderableWidget(
         CycleButton.onOffBuilder(!this.blockEntity.ignoreEntities())
            .displayOnlyValue()
            .create(this.widgetX(l++, 5), 160, widgetSize(5), 20, INCLUDE_ENTITIES_LABEL)
      );
      l = 0;
      this.addRenderableWidget(Button.builder(Component.translatable("test_instance.action.reset"), p_397794_ -> {
         this.sendToServer(ServerboundTestInstanceBlockActionPacket.Action.RESET);
         this.minecraft.setScreen(null);
      }).bounds(this.widgetX(l++, j), 185, k, 20).build());
      this.saveButton = this.addRenderableWidget(Button.builder(Component.translatable("test_instance.action.save"), p_397643_ -> {
         this.sendToServer(ServerboundTestInstanceBlockActionPacket.Action.SAVE);
         this.minecraft.setScreen(null);
      }).bounds(this.widgetX(l++, j), 185, k, 20).build());
      if (flag) {
         this.exportButton = this.addRenderableWidget(Button.builder(Component.literal("Export Structure"), p_397603_ -> {
            this.sendToServer(ServerboundTestInstanceBlockActionPacket.Action.EXPORT);
            this.minecraft.setScreen(null);
         }).bounds(this.widgetX(l++, j), 185, k, 20).build());
      }

      this.addRenderableWidget(Button.builder(Component.translatable("test_instance.action.run"), p_397205_ -> {
         this.sendToServer(ServerboundTestInstanceBlockActionPacket.Action.RUN);
         this.minecraft.setScreen(null);
      }).bounds(this.widgetX(0, 3), 210, widgetSize(3), 20).build());
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, p_397628_ -> this.onDone()).bounds(this.widgetX(1, 3), 210, widgetSize(3), 20).build());
      this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_CANCEL, p_397964_ -> this.onCancel()).bounds(this.widgetX(2, 3), 210, widgetSize(3), 20).build()
      );
      this.updateTestInfo(true);
   }

   private void updateSaveState() {
      boolean flag = this.rotationButton.getValue() == Rotation.NONE && Identifier.tryParse(this.idEdit.getValue()) != null;
      this.saveButton.active = flag;
      if (this.exportButton != null) {
         this.exportButton.active = flag;
      }
   }

   private static Component rotationDisplay(Rotation p_398001_) {
      return Component.literal(switch (p_398001_) {
         case NONE -> "0";
         case CLOCKWISE_90 -> "90";
         case CLOCKWISE_180 -> "180";
         case COUNTERCLOCKWISE_90 -> "270";
      });
   }

   private void setSize(Vec3i p_397406_) {
      this.sizeXEdit.setValue(Integer.toString(p_397406_.getX()));
      this.sizeYEdit.setValue(Integer.toString(p_397406_.getY()));
      this.sizeZEdit.setValue(Integer.toString(p_397406_.getZ()));
   }

   private int widgetX(int p_397341_, int p_397626_) {
      int i = this.width / 2 - 158;
      float f = exactWidgetSize(p_397626_);
      return (int)(i + p_397341_ * (8.0F + f));
   }

   private static int widgetSize(int p_397220_) {
      return (int)exactWidgetSize(p_397220_);
   }

   private static float exactWidgetSize(int p_397807_) {
      return (float)(316 - (p_397807_ - 1) * 8) / p_397807_;
   }

   @Override
   public void render(GuiGraphics p_397620_, int p_397635_, int p_397823_, float p_397214_) {
      super.render(p_397620_, p_397635_, p_397823_, p_397214_);
      int i = this.width / 2 - 158;
      p_397620_.drawCenteredString(this.font, this.title, this.width / 2, 10, -1);
      p_397620_.drawString(this.font, ID_LABEL, i, 30, -6250336);
      p_397620_.drawString(this.font, SIZE_LABEL, i, 150, -6250336);
      p_397620_.drawString(this.font, ROTATION_LABEL, this.rotationButton.getX(), 150, -6250336);
      p_397620_.drawString(this.font, INCLUDE_ENTITIES_LABEL, this.includeEntitiesButton.getX(), 150, -6250336);
   }

   private void updateTestInfo(boolean p_397030_) {
      boolean flag = this.sendToServer(p_397030_ ? ServerboundTestInstanceBlockActionPacket.Action.INIT : ServerboundTestInstanceBlockActionPacket.Action.QUERY);
      if (!flag) {
         this.infoWidget.setMessage(Component.translatable("test_instance.description.invalid_id").withStyle(ChatFormatting.RED));
      }

      this.updateSaveState();
   }

   private void onDone() {
      this.sendToServer(ServerboundTestInstanceBlockActionPacket.Action.SET);
      this.onClose();
   }

   private boolean sendToServer(ServerboundTestInstanceBlockActionPacket.Action p_397612_) {
      Optional<Identifier> optional = Optional.ofNullable(Identifier.tryParse(this.idEdit.getValue()));
      Optional<ResourceKey<GameTestInstance>> optional1 = optional.map(p_465532_ -> ResourceKey.create(Registries.TEST_INSTANCE, p_465532_));
      Vec3i vec3i = new Vec3i(parseSize(this.sizeXEdit.getValue()), parseSize(this.sizeYEdit.getValue()), parseSize(this.sizeZEdit.getValue()));
      boolean flag = !this.includeEntitiesButton.getValue();
      this.minecraft
         .getConnection()
         .send(new ServerboundTestInstanceBlockActionPacket(this.blockEntity.getBlockPos(), p_397612_, optional1, vec3i, this.rotationButton.getValue(), flag));
      return optional.isPresent();
   }

   public void setStatus(Component p_397990_, Optional<Vec3i> p_397675_) {
      MutableComponent mutablecomponent = Component.empty();
      this.blockEntity
         .errorMessage()
         .ifPresent(
            p_397370_ -> mutablecomponent.append(
                  Component.translatable("test_instance.description.failed", Component.empty().withStyle(ChatFormatting.RED).append(p_397370_))
               )
               .append("\n\n")
         );
      mutablecomponent.append(p_397990_);
      this.infoWidget.setMessage(mutablecomponent);
      p_397675_.ifPresent(this::setSize);
   }

   private void onCancel() {
      this.onClose();
   }

   private static int parseSize(String p_397084_) {
      try {
         return Mth.clamp(Integer.parseInt(p_397084_), 1, 48);
      } catch (NumberFormatException var2) {
         return 1;
      }
   }

   @Override
   public boolean isInGameUi() {
      return true;
   }
}
