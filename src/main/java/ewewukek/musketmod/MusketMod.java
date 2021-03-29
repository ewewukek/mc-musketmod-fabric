package ewewukek.musketmod;

import net.fabricmc.api.ModInitializer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class MusketMod implements ModInitializer {
    public static final String MODID = "musketmod";

    public static final Item BARREL = new Item(new Item.Settings().group(ItemGroup.MISC));
    public static final Item STOCK = new Item(new Item.Settings().group(ItemGroup.MISC));
    public static final Item CARTRIDGE = new Item(new Item.Settings().group(ItemGroup.COMBAT));
    public static final Item MUSKET = new MusketItem(new Item.Settings().group(ItemGroup.COMBAT));

    public static final SoundEvent SOUND_MUSKET_LOAD_0 = new SoundEvent(new Identifier(MODID, "musket_load0"));
    public static final SoundEvent SOUND_MUSKET_LOAD_1 = new SoundEvent(new Identifier(MODID, "musket_load1"));
    public static final SoundEvent SOUND_MUSKET_LOAD_2 = new SoundEvent(new Identifier(MODID, "musket_load2"));
    public static final SoundEvent SOUND_MUSKET_READY = new SoundEvent(new Identifier(MODID, "musket_ready"));
    public static final SoundEvent SOUND_MUSKET_FIRE = new SoundEvent(new Identifier(MODID, "musket_fire"));

    @Override
    public void onInitialize() {
        Registry.register(Registry.ITEM, new Identifier(MODID, "barrel"), BARREL);
        Registry.register(Registry.ITEM, new Identifier(MODID, "stock"), STOCK);
        Registry.register(Registry.ITEM, new Identifier(MODID, "cartridge"), CARTRIDGE);
        Registry.register(Registry.ITEM, new Identifier(MODID, "musket"), MUSKET);

        Registry.register(Registry.SOUND_EVENT, new Identifier(MODID, "musket_load0"), SOUND_MUSKET_LOAD_0);
        Registry.register(Registry.SOUND_EVENT, new Identifier(MODID, "musket_load1"), SOUND_MUSKET_LOAD_1);
        Registry.register(Registry.SOUND_EVENT, new Identifier(MODID, "musket_load2"), SOUND_MUSKET_LOAD_2);
        Registry.register(Registry.SOUND_EVENT, new Identifier(MODID, "musket_ready"), SOUND_MUSKET_READY);
        Registry.register(Registry.SOUND_EVENT, new Identifier(MODID, "musket_fire"), SOUND_MUSKET_FIRE);
    }
}