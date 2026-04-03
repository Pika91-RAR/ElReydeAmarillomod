package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import net.minecraftforge.client.event.EntityRenderersEvent;

public final class KingInYellowClient {
    private KingInYellowClient() {
    }

    public static void register() {
        EntityRenderersEvent.RegisterRenderers.BUS.addListener(KingInYellowClient::registerRenderers);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ExampleMod.KING_IN_YELLOW.get(), KingInYellowRenderer::new);
    }
}
