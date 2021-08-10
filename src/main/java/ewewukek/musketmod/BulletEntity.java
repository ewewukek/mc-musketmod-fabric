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
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
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

    private Vec3d origin;

    public static float damageFactorMin;
    public static float damageFactorMax;
    public static double maxDistance;

    public short ticksLeft;

    public BulletEntity(EntityType<BulletEntity>entityType, World world) {
        super(entityType, world);
        ticksLeft = LIFETIME;
    }

    public BulletEntity(World world) {
        this(MusketMod.BULLET_ENTITY_TYPE, world);
    }

    public boolean isFirstTick() {
        return ticksLeft == LIFETIME;
    }

    public DamageSource causeMusketDamage(BulletEntity bullet, Entity attacker) {
        return (new ProjectileDamageSource("musket", bullet, attacker)).setProjectile();
    }

    @Override
    public void tick() {
        if (!world.isClient && processCollision()) {
            discard();
            return;
        }

        if (world.isClient && isFirstTick()) {
            fireParticles();
        }

        // for compatibility origin is not stored in world save
        if (origin == null) origin = getPos();
        double distanceTravelled = getPos().subtract(origin).length();

        if (--ticksLeft <= 0 || distanceTravelled > maxDistance) {
            discard();
            return;
        }

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
        checkBlockCollision();
    }

    private void fireParticles() {
        Vec3d pos = getPos();
        Vec3d front = getVelocity().normalize();

        for (int i = 0; i != 10; ++i) {
            double t = Math.pow(random.nextFloat(), 1.5);
            Vec3d p = pos.add(front.multiply(1.25 + t));
            p = p.add(new Vec3d(random.nextFloat() - 0.5, random.nextFloat() - 0.5, random.nextFloat() - 0.5).multiply(0.1));
            Vec3d v = front.multiply(0.1 * (1 - t));
            world.addParticle(ParticleTypes.POOF, p.x, p.y, p.z, v.x, v.y, v.z);
        }
    }

    private boolean processCollision() {
        Vec3d from = getPos();
        Vec3d to = from.add(getVelocity());

        BlockHitResult collision = world.raycast(
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

        BlockState blockstate = world.getBlockState(collision.getBlockPos());
        blockstate.onProjectileHit(world, blockstate, collision, this);

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

        Entity result = null;
        double result_dist = 0;

        Box aabbSelection = getBoundingBox().stretch(motion).expand(0.5);
        for (Entity entity : world.getOtherEntities(this, aabbSelection, getTargetPredicate())) {
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

        return result;
    }

    @Override
    protected void initDataTracker() {
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        ticksLeft = compound.getShort("ticksLeft");
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putShort("ticksLeft", ticksLeft);
    }

    // workaround for EntitySpawnS2CPacket.MAX_ABSOLUTE_VELOCITY
    @Override
    public Packet<?> createSpawnPacket() {
        Entity owner = getOwner();
        return new EntitySpawnS2CPacket(
            getId(), getUuid(),
            getX(), getY(), getZ(),
            getPitch(), getYaw(),
            getType(), owner != null ? owner.getId() : 0,
            getVelocity().multiply(EntitySpawnS2CPacket.MAX_ABSOLUTE_VELOCITY / MusketItem.bulletSpeed)
        );
    }

    @Override
    public void onSpawnPacket(EntitySpawnS2CPacket packet) {
        super.onSpawnPacket(packet);
        Vec3d packet_velocity = new Vec3d(packet.getVelocityX(), packet.getVelocityY(), packet.getVelocityZ());
        setVelocity(packet_velocity.multiply(MusketItem.bulletSpeed / EntitySpawnS2CPacket.MAX_ABSOLUTE_VELOCITY));
    }
}
