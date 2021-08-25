package ewewukek.musketmod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.object.builder.v1.client.model.FabricModelPredicateProviderRegistry;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.resources.ResourceLocation;

public class ClientSetup implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.INSTANCE.register(MusketMod.BULLET_ENTITY_TYPE, (ctx) -> new BulletRenderer(ctx));

        ClampedItemPropertyFunction loaded = (stack, world, player, seed) -> {
            return MusketItem.isLoaded(stack) ? 1 : 0;
        };
        FabricModelPredicateProviderRegistry.register(MusketMod.MUSKET, new ResourceLocation("loaded"), loaded);
        FabricModelPredicateProviderRegistry.register(MusketMod.MUSKET_WITH_BAYONET, new ResourceLocation("loaded"), loaded);
    }
}
