package net.minecraft.gametest.framework;

import com.google.common.base.Stopwatch;
import java.io.File;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class JUnitLikeTestReporter implements TestReporter {
   private final Document document;
   private final Element testSuite;
   private final Stopwatch stopwatch;
   private final File destination;

   public JUnitLikeTestReporter(File p_177664_) throws ParserConfigurationException {
      this.destination = p_177664_;
      this.document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
      this.testSuite = this.document.createElement("testsuite");
      Element $$1 = this.document.createElement("testsuite");
      $$1.appendChild(this.testSuite);
      this.document.appendChild($$1);
      this.testSuite.setAttribute("timestamp", DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
      this.stopwatch = Stopwatch.createStarted();
   }

   private Element createTestCase(GameTestInfo p_177671_, String p_177672_) {
      Element $$2 = this.document.createElement("testcase");
      $$2.setAttribute("name", p_177672_);
      $$2.setAttribute("classname", p_177671_.getStructureName());
      $$2.setAttribute("time", String.valueOf(p_177671_.getRunTime() / 1000.0));
      this.testSuite.appendChild($$2);
      return $$2;
   }

   @Override
   public void onTestFailed(GameTestInfo p_177669_) {
      String $$1 = p_177669_.getTestName();
      String $$2 = p_177669_.getError().getMessage();
      Element $$3 = this.document.createElement(p_177669_.isRequired() ? "failure" : "skipped");
      $$3.setAttribute("message", "(" + p_177669_.getStructureBlockPos().toShortString() + ") " + $$2);
      Element $$4 = this.createTestCase(p_177669_, $$1);
      $$4.appendChild($$3);
   }

   @Override
   public void onTestSuccess(GameTestInfo p_177674_) {
      String $$1 = p_177674_.getTestName();
      this.createTestCase(p_177674_, $$1);
   }

   @Override
   public void finish() {
      this.stopwatch.stop();
      this.testSuite.setAttribute("time", String.valueOf(this.stopwatch.elapsed(TimeUnit.MILLISECONDS) / 1000.0));

      try {
         this.save(this.destination);
      } catch (TransformerException var2) {
         throw new Error("Couldn't save test report", var2);
      }
   }

   public void save(File p_177667_) throws TransformerException {
      TransformerFactory $$1 = TransformerFactory.newInstance();
      Transformer $$2 = $$1.newTransformer();
      DOMSource $$3 = new DOMSource(this.document);
      StreamResult $$4 = new StreamResult(p_177667_);
      $$2.transform($$3, $$4);
   }
}
