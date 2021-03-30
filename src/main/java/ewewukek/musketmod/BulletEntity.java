package ewewukek.musketmod;

import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.ProjectileDamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.ThrownEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

public class BulletEntity extends ThrownEntity {
    private static final Random random = new Random();
    static final double GRAVITY = 0.05;
    static final double AIR_FRICTION = 0.99;
    static final double WATER_FRICTION = 0.6;
    static final short LIFETIME = 50;

    public static float damageFactorMin;
    public static float damageFactorMax;

    public short ticksLeft;
    public boolean doFireParticles;

    public BulletEntity(EntityType<BulletEntity>entityType, World world) {
        super(entityType, world);
        ticksLeft = LIFETIME;
    }

    public BulletEntity(World world) {
        this(MusketMod.BULLET_ENTITY_TYPE, world);
    }

    public DamageSource causeMusketDamage(BulletEntity bullet, Entity attacker) {
        return (new ProjectileDamageSource("musket", bullet, attacker)).setProjectile();
    }

    @Override
    public void tick() {
        if (!world.isClient && processCollision()) {
            remove();
            return;
        }

        if (world.isClient && doFireParticles) {
            fireParticles();
            doFireParticles = false;
        }

        if (--ticksLeft <= 0) {
            remove();
            return;
        }

        checkBlockCollision();

        Vec3d motion = getVelocity();
        double posX = getX() + motion.x;
        double posY = getY() + motion.y;
        double posZ = getZ() + motion.z;

        motion = motion.subtract(0, GRAVITY, 0);

        double friction = AIR_FRICTION;
        if (isSubmergedInWater()) {
            final int count = 4;
            for (int i = 0; i != count; ++i) {
                double t = (i + 1.0) / count;
                world.addParticle(
                    ParticleTypes.BUBBLE,
                    posX - motion.x * t,
                    posY - motion.y * t,
                    posZ - motion.z * t,
                    motion.x,
                    motion.y,
                    motion.z
                );
            }
            friction = WATER_FRICTION;
        }

        setVelocity(motion.multiply(friction));
        updatePosition(posX, posY, posZ);
    }

    private void fireParticles() {
        Vec3d pos = getPos();
        Vec3d front = getVelocity().normalize();

        for (int i = 0; i != 10; ++i) {
            double t = Math.pow(random.nextFloat(), 1.5);
            Vec3d p = pos.add(front.multiply(1.25 + t));
            Vec3d v = front.multiply(0.1 * (1 - t));
            world.addParticle(ParticleTypes.POOF, p.x, p.y, p.z, v.x, v.y, v.z);
        }
    }

    private boolean processCollision() {
        Vec3d from = getPos();
        Vec3d to = from.add(getVelocity());

        HitResult collision = world.raycast(
            new RaycastContext(from, to, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this));

        // prevents hitting entities behind an obstacle
        if (collision.getType() != HitResult.Type.MISS) {
            to = collision.getPos();
        }

        Entity target = closestEntityOnPath(from, to);
        if (target != null) {
            if (target instanceof PlayerEntity) {
                Entity shooter = getOwner();
                if (shooter instanceof PlayerEntity && !((PlayerEntity)shooter).shouldDamagePlayer((PlayerEntity)target)) {

                    target = null;
                }
            }
            if (target != null) {
                hitEntity(target);
                return true;
            }
        }

        if (collision.getType() != HitResult.Type.BLOCK) return false;

        BlockPos blockPos = ((BlockHitResult)collision).getBlockPos();
        BlockState blockstate = world.getBlockState(blockPos);
        blockstate.onProjectileHit(world, blockstate, (BlockHitResult)collision, this);

        int impactParticleCount = (int)(getVelocity().lengthSquared() / 20);
        if (impactParticleCount > 0) {
            ((ServerWorld)world).spawnParticles(
                new BlockStateParticleEffect(ParticleTypes.BLOCK, blockstate),
                to.x, to.y, to.z,
                impactParticleCount,
                0, 0, 0, 0.01
            );
        }

        return true;
    }

    private void hitEntity(Entity target) {
        Entity shooter = getOwner();
        DamageSource damagesource = causeMusketDamage(this, shooter != null ? shooter : this);

        float energy = (float)getVelocity().lengthSquared();
        float factor = damageFactorMin + random.nextFloat() * (damageFactorMax - damageFactorMin);
        target.damage(damagesource, energy * factor);
    }

    private Predicate<Entity> getTargetPredicate() {
        Entity shooter = getOwner();
        return (entity) -> {
            return !entity.isSpectator() && entity.isAlive() && entity.collides() && entity != shooter;
        };
    }

    private Entity closestEntityOnPath(Vec3d start, Vec3d end) {
        Vec3d motion = getVelocity();
        Entity shooter = getOwner();

        Entity result = null;
        double result_dist = motion.lengthSquared() + 0.05;

        Box aabbSelection = getBoundingBox().stretch(motion).expand(0.5);
        for (Entity entity : world.getOtherEntities(this, aabbSelection, getTargetPredicate())) {
            if (entity != shooter) {
                Box aabb = entity.getBoundingBox();
                Optional<Vec3d> optional = aabb.raycast(start, end);
                if (optional.isPresent()) {
                    double dist = start.squaredDistanceTo(optional.get());
                    if (dist < result_dist || result == null) {
                        result = entity;
                        result_dist = dist;
                    }
                }
            }
        }

        return result;
    }

    @Override
    protected void initDataTracker() {
    }

    @Override
    protected void readCustomDataFromTag(CompoundTag compound) {
        super.readCustomDataFromTag(compound);
        ticksLeft = compound.getShort("ticksLeft");
    }

    @Override
    protected void writeCustomDataToTag(CompoundTag compound) {
        super.writeCustomDataToTag(compound);
        compound.putShort("ticksLeft", ticksLeft);
    }

    public void writeSpawnData(PacketByteBuf data) {
        data.writeVarInt(getEntityId());
        data.writeUuid(getUuid());

        data.writeDouble(getX());
        data.writeDouble(getY());
        data.writeDouble(getZ());

        Vec3d motion = getVelocity();
        data.writeFloat((float)motion.x);
        data.writeFloat((float)motion.y);
        data.writeFloat((float)motion.z);

        data.writeShort(ticksLeft);
        data.writeByte(doFireParticles ? 1 : 0);
    }

    public void readSpawnData(PacketByteBuf data) {
        setEntityId(data.readVarInt());
        setUuid(data.readUuid());

        double x = data.readDouble();
        double y = data.readDouble();
        double z = data.readDouble();
        setPos(x, y, z);
        updateTrackedPosition(x, y, z);
        refreshPositionAfterTeleport(x, y, z);

        Vec3d motion = new Vec3d(data.readFloat(), data.readFloat(), data.readFloat());
        setVelocity(motion);

        ticksLeft = data.readShort();
        doFireParticles = data.readByte() != 0;
    }
}
