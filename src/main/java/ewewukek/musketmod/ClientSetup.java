package ewewukek.musketmod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.object.builder.v1.client.model.FabricModelPredicateProviderRegistry;
import net.minecraft.util.Identifier;

public class ClientSetup implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.INSTANCE.register(MusketMod.BULLET_ENTITY_TYPE, (ctx) -> new BulletRenderer(ctx));

        FabricModelPredicateProviderRegistry.register(MusketMod.MUSKET, new Identifier("loaded"), (stack, world, player, seed) -> {
            return MusketItem.isLoaded(stack) ? 1 : 0;
        });
    }
}
