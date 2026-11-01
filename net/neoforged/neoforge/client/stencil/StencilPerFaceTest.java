package net.neoforged.neoforge.client.stencil;

public record StencilPerFaceTest(StencilOperation fail, StencilOperation depthFail, StencilOperation pass, StencilFunction compare) {
}
