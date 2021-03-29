package ewewukek.musketmod;

import net.fabricmc.api.ModInitializer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class MusketMod implements ModInitializer {
    public static final String MODID = "musketmod";

    public static final Item BARREL = new Item(new Item.Settings().group(ItemGroup.MISC));
    public static final Item STOCK = new Item(new Item.Settings().group(ItemGroup.MISC));
    public static final Item CARTRIDGE = new Item(new Item.Settings().group(ItemGroup.COMBAT));
    public static final Item MUSKET = new Item(new Item.Settings().group(ItemGroup.COMBAT));

    @Override
    public void onInitialize() {
        Registry.register(Registry.ITEM, new Identifier(MODID, "barrel"), BARREL);
        Registry.register(Registry.ITEM, new Identifier(MODID, "stock"), STOCK);
        Registry.register(Registry.ITEM, new Identifier(MODID, "cartridge"), CARTRIDGE);
        Registry.register(Registry.ITEM, new Identifier(MODID, "musket"), MUSKET);
    }
}
