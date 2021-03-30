package ewewukek.musketmod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry;
import net.minecraft.client.world.ClientWorld;

public class ClientSetup implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.INSTANCE.register(MusketMod.BULLET_ENTITY_TYPE, (dispatcher, context) -> new BulletRenderer(dispatcher));

        ClientPlayNetworking.registerGlobalReceiver(MusketMod.SPAWN_BULLET_PACKET_ID, (client, handler, buf, responseSender) -> {
            ClientWorld world = handler.getWorld();
            BulletEntity bullet = new BulletEntity(world);
            bullet.readSpawnData(buf);
            world.addEntity(bullet.getEntityId(), bullet);
        });
    }
}
