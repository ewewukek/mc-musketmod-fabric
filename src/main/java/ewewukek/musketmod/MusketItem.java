package ewewukek.musketmod;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.tag.FluidTags;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class MusketItem extends Item {
    public static final int DURABILITY = 250;
    public static final int LOADING_STAGE_1 = 5;
    public static final int LOADING_STAGE_2 = 10;
    public static final int LOADING_STAGE_3 = 20;
    public static final int RELOAD_DURATION = 30;

    public static float bulletStdDev;
    public static double bulletSpeed;

    public MusketItem(Settings settings) {
        super(settings.maxDamage(DURABILITY));
    }

    @Override
    public TypedActionResult<ItemStack> use(World worldIn, PlayerEntity player, Hand hand) {
        if (hand != Hand.MAIN_HAND) return super.use(worldIn, player, hand);

        ItemStack stack = player.getStackInHand(hand);
        boolean creative = player.abilities.creativeMode;

        if (player.isSubmergedIn(FluidTags.WATER) && !creative) {
            return TypedActionResult.fail(stack);
        }

        boolean haveAmmo = !findAmmo(player).isEmpty() || creative;
        boolean loaded = isLoaded(stack);

        if (loaded && isReady(stack)) {
            if (!worldIn.isClient) {
                fireBullet(worldIn, player);
            }
            player.playSound(MusketMod.SOUND_MUSKET_FIRE, 1.5f, 1);

            damageItem(stack, player);
            setReady(stack, false);
            setLoaded(stack, false);

            return TypedActionResult.consume(stack);

        } else if (loaded || haveAmmo) {
            if (!loaded) {
                setLoadingStage(stack, 0);
            }
            player.setCurrentHand(hand);
            return TypedActionResult.consume(stack);

        } else {
            return TypedActionResult.fail(stack);
        }
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World worldIn, LivingEntity entityLiving, int timeLeft) {
        if (isLoaded(stack)) setReady(stack, true);
    }

    @Override
    public void usageTick(World world, LivingEntity entity, ItemStack stack, int timeLeft) {
        if (world.isClient || !(entity instanceof PlayerEntity)) return;

        PlayerEntity player = (PlayerEntity) entity;
        int usingDuration = getMaxUseTime(stack) - timeLeft;
        int loadingStage = getLoadingStage(stack);

        double posX = player.getX();
        double posY = player.getY();
        double posZ = player.getZ();

        if (loadingStage == 0 && usingDuration >= LOADING_STAGE_1) {
            world.playSound(null, posX, posY, posZ, MusketMod.SOUND_MUSKET_LOAD_0, SoundCategory.PLAYERS, 0.5f, 1);
            setLoadingStage(stack, 1);

        } else if (loadingStage == 1 && usingDuration >= LOADING_STAGE_2) {
            world.playSound(null, posX, posY, posZ, MusketMod.SOUND_MUSKET_LOAD_1, SoundCategory.PLAYERS, 0.5f, 1);
            setLoadingStage(stack, 2);

        } else if (loadingStage == 2 && usingDuration >= LOADING_STAGE_3) {
            world.playSound(null, posX, posY, posZ, MusketMod.SOUND_MUSKET_LOAD_2, SoundCategory.PLAYERS, 0.5f, 1);
            setLoadingStage(stack, 3);
        }

        if (usingDuration >= RELOAD_DURATION && !isLoaded(stack)) {
            if (!player.abilities.creativeMode) {
                ItemStack ammoStack = findAmmo(player);
                if (ammoStack.isEmpty()) return;

                ammoStack.decrement(1);
                if (ammoStack.isEmpty()) player.inventory.removeOne(ammoStack);
            }

            world.playSound(null, posX, posY, posZ, MusketMod.SOUND_MUSKET_READY, SoundCategory.PLAYERS, 0.5f, 1);
            setLoaded(stack, true);
        }
   }

    @Override
    public boolean postMine(ItemStack stack, World worldIn, BlockState state, BlockPos pos, LivingEntity entityLiving) {
        if (!worldIn.isClient && entityLiving instanceof PlayerEntity && state.getHardness(worldIn, pos) != 0.0f) {
            damageItem(stack, (PlayerEntity) entityLiving);
        }
        return false;
    }

    public static void damageItem(ItemStack stack, PlayerEntity player) {
        stack.damage(1, player, (entity) -> {
            entity.sendToolBreakStatus(player.getActiveHand());
        });
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return 72000;
    }

    public static boolean isLoaded(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getByte("loaded") == 1;
    }

    public static boolean isReady(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getByte("ready") == 1;
    }

    private boolean isAmmo(ItemStack stack) {
        return stack.getItem() == MusketMod.CARTRIDGE;
    }

    private ItemStack findAmmo(PlayerEntity player) {
        if (isAmmo(player.getStackInHand(Hand.OFF_HAND))) {
            return player.getStackInHand(Hand.OFF_HAND);

        } else if (isAmmo(player.getStackInHand(Hand.MAIN_HAND))) {
            return player.getStackInHand(Hand.MAIN_HAND);

        } else {
            for (int i = 0; i != player.inventory.main.size(); ++i) {
                ItemStack itemstack = player.inventory.main.get(i);
                if (isAmmo(itemstack)) return itemstack;
            }

            return ItemStack.EMPTY;
        }
    }

    private void fireBullet(World worldIn, PlayerEntity player) {
        final float deg2rad = 0.017453292f;
        Vec3d front = new Vec3d(0, 0, 1).rotateX(-deg2rad * player.pitch).rotateY(-deg2rad * player.yaw);

        Vec3d pos = new Vec3d(player.getX(), player.getEyeY(), player.getZ());
        pos.add(front.multiply(0.2));

        float angle = (float) Math.PI * 2 * RANDOM.nextFloat();
        float gaussian = Math.abs((float) RANDOM.nextGaussian());
        if (gaussian > 4) gaussian = 4;

        front = front.rotateX(bulletStdDev * gaussian * MathHelper.sin(angle))
                .rotateY(bulletStdDev * gaussian * MathHelper.cos(angle));

        Vec3d motion = front.multiply(bulletSpeed);

        Vec3d playerMotion = player.getVelocity();
        motion.add(playerMotion.x, player.isOnGround() ? 0 : playerMotion.y, playerMotion.z);

        BulletEntity bullet = new BulletEntity(worldIn);
        bullet.setOwner(player);
        bullet.updatePosition(pos.x, pos.y, pos.z);
        bullet.setVelocity(motion);
        bullet.doFireParticles = true;

        PacketByteBuf buf = PacketByteBufs.create();
        bullet.writeSpawnData(buf);
        ServerPlayNetworking.send((ServerPlayerEntity) player, MusketMod.SPAWN_BULLET_PACKET_ID, buf);

        worldIn.spawnEntity(bullet);
    }

    private void setLoaded(ItemStack stack, boolean loaded) {
        stack.getOrCreateTag().putByte("loaded", (byte) (loaded ? 1 : 0));
    }

    private void setReady(ItemStack stack, boolean ready) {
        stack.getOrCreateTag().putByte("ready", (byte) (ready ? 1 : 0));
    }

    private void setLoadingStage(ItemStack stack, int loadingStage) {
        stack.getOrCreateTag().putInt("loadingStage", loadingStage);
    }

    private int getLoadingStage(ItemStack stack) {
        return stack.getOrCreateTag().getInt("loadingStage");
    }
}
