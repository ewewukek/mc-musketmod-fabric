package ewewukek.musketmod;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;

public class MusketMod implements ModInitializer {
    public static final String MODID = "musketmod";
    public static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("musketmod.txt");

    public static final Item CARTRIDGE = new Item(new Item.Properties().tab(CreativeModeTab.TAB_COMBAT));
    public static final Item MUSKET = new MusketItem(new Item.Properties().tab(CreativeModeTab.TAB_COMBAT));
    public static final Item MUSKET_WITH_BAYONET = new MusketItem(3, new Item.Properties().tab(CreativeModeTab.TAB_COMBAT));

    public static final SoundEvent SOUND_MUSKET_LOAD_0 = new SoundEvent(new ResourceLocation(MODID, "musket_load0"));
    public static final SoundEvent SOUND_MUSKET_LOAD_1 = new SoundEvent(new ResourceLocation(MODID, "musket_load1"));
    public static final SoundEvent SOUND_MUSKET_LOAD_2 = new SoundEvent(new ResourceLocation(MODID, "musket_load2"));
    public static final SoundEvent SOUND_MUSKET_READY = new SoundEvent(new ResourceLocation(MODID, "musket_ready"));
    public static final SoundEvent SOUND_MUSKET_FIRE = new SoundEvent(new ResourceLocation(MODID, "musket_fire"));

    public static final EntityType<BulletEntity> BULLET_ENTITY_TYPE = FabricEntityTypeBuilder.<BulletEntity>create(MobCategory.MISC, BulletEntity::new)
            .dimensions(EntityDimensions.fixed(0.5F, 0.5F))
            .trackRangeBlocks(64).trackedUpdateRate(5)
            .forceTrackedVelocityUpdates(false)
            .build();

    @Override
    public void onInitialize() {
        Registry.register(Registry.ITEM, new ResourceLocation(MODID, "cartridge"), CARTRIDGE);
        Registry.register(Registry.ITEM, new ResourceLocation(MODID, "musket"), MUSKET);
        Registry.register(Registry.ITEM, new ResourceLocation(MODID, "musket_with_bayonet"), MUSKET_WITH_BAYONET);

        Registry.register(Registry.ENTITY_TYPE, new ResourceLocation(MODID, "bullet"), BULLET_ENTITY_TYPE);

        Registry.register(Registry.SOUND_EVENT, new ResourceLocation(MODID, "musket_load0"), SOUND_MUSKET_LOAD_0);
        Registry.register(Registry.SOUND_EVENT, new ResourceLocation(MODID, "musket_load1"), SOUND_MUSKET_LOAD_1);
        Registry.register(Registry.SOUND_EVENT, new ResourceLocation(MODID, "musket_load2"), SOUND_MUSKET_LOAD_2);
        Registry.register(Registry.SOUND_EVENT, new ResourceLocation(MODID, "musket_ready"), SOUND_MUSKET_READY);
        Registry.register(Registry.SOUND_EVENT, new ResourceLocation(MODID, "musket_fire"), SOUND_MUSKET_FIRE);

        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new IdentifiableResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return new ResourceLocation(MODID, "reload");
            }

            @Override
            public CompletableFuture<Void> reload(PreparationBarrier stage, ResourceManager resourceManager,
                ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler, Executor backgroundExecutor,
                Executor gameExecutor) {

                return stage.wait(Unit.INSTANCE).thenRunAsync(() -> {
                    Config.reload();
                }, gameExecutor);
            }
        });
    }

    public static final ResourceLocation SMOKE_EFFECT_PACKET_ID = new ResourceLocation(MODID, "smoke_effect");

    public static void sendSmokeEffect(Player player, Vec3 origin, Vec3 direction) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeFloat((float)origin.x);
        buf.writeFloat((float)origin.y);
        buf.writeFloat((float)origin.z);
        buf.writeFloat((float)direction.x);
        buf.writeFloat((float)direction.y);
        buf.writeFloat((float)direction.z);
        BlockPos blockPos = new BlockPos(origin);
        for (ServerPlayer serverPlayer : PlayerLookup.tracking((ServerLevel)player.level, blockPos)) {
            ServerPlayNetworking.send(serverPlayer, SMOKE_EFFECT_PACKET_ID, buf);
        }

    }
}
