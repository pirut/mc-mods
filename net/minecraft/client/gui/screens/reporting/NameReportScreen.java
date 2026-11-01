package net.minecraft.client.gui.screens.reporting;

import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.CommonLayouts;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.chat.report.NameReport;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class NameReportScreen extends AbstractReportScreen<NameReport.Builder> {
   private static final Component TITLE = Component.translatable("gui.abuseReport.name.title");
   private static final Component COMMENT_BOX_LABEL = Component.translatable("gui.abuseReport.name.comment_box_label");
   @Nullable
   private MultiLineEditBox commentBox;

   private NameReportScreen(Screen p_299832_, ReportingContext p_300026_, NameReport.Builder p_299866_) {
      super(TITLE, p_299832_, p_300026_, p_299866_);
   }

   public NameReportScreen(Screen p_299947_, ReportingContext p_299966_, UUID p_299912_, String p_299935_) {
      this(p_299947_, p_299966_, new NameReport.Builder(p_299912_, p_299935_, p_299966_.sender().reportLimits()));
   }

   public NameReportScreen(Screen p_299861_, ReportingContext p_300015_, NameReport p_299994_) {
      this(p_299861_, p_300015_, new NameReport.Builder(p_299994_, p_300015_.sender().reportLimits()));
   }

   @Override
   protected void addContent() {
      Component component = Component.literal(this.reportBuilder.report().getReportedName()).withStyle(ChatFormatting.YELLOW);
      this.layout
         .addChild(
            new StringWidget(Component.translatable("gui.abuseReport.name.reporting", component), this.font),
            p_359100_ -> p_359100_.alignHorizontallyCenter().padding(0, 8)
         );
      this.commentBox = this.createCommentBox(280, 72, p_436489_ -> {
         this.reportBuilder.setComments(p_436489_);
         this.onReportChanged();
      });
      this.layout.addChild(CommonLayouts.labeledElement(this.font, this.commentBox, COMMENT_BOX_LABEL, p_299902_ -> p_299902_.paddingBottom(12)));
   }

   @Override
   public boolean mouseReleased(MouseButtonEvent p_446271_) {
      if (super.mouseReleased(p_446271_)) {
         return true;
      } else {
         return this.commentBox != null ? this.commentBox.mouseReleased(p_446271_) : false;
      }
   }
}
