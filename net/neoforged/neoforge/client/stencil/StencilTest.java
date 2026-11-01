package net.neoforged.neoforge.client.stencil;

public record StencilTest(StencilPerFaceTest front, StencilPerFaceTest back, int readMask, int writeMask, int referenceValue) {
   public static int DEFAULT_READ_MASK = -1;
   public static int DEFAULT_WRITE_MASK = -1;
   public static int DEFAULT_REFERENCE_VALUE = 0;

   public StencilTest(StencilPerFaceTest test, int readMask, int writeMask, int referenceValue) {
      this(test, test, readMask, writeMask, referenceValue);
   }
}
