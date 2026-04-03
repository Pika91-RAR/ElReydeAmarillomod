package com.example.examplemod;

import com.example.examplemod.entity.KingInYellowEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.fml.LogicalSide;

public final class KingInYellowEvents {
    private static final double GAZE_RADIUS = 64.0D;
    private static final double PRESENCE_RADIUS = 96.0D;
    private static final double STALK_RADIUS = 40.0D;
    private static final double UNIQUE_SEARCH_RADIUS = 512.0D;
    private static final int GAZE_CHECK_INTERVAL = 4;
    private static final int TERROR_CHECK_INTERVAL = 20;
    private static final int STALK_CHECK_INTERVAL = 10;
    private static final int BASE_TERROR_CHANCE = 240;
    private static final int NEARBY_TERROR_CHANCE = 18;
    private static final int DAY_SPAWN_CHANCE = 280;
    private static final int NIGHT_SPAWN_CHANCE = 96;
    private static final int AMBIENCE_CHECK_INTERVAL = 40;

    private KingInYellowEvents() {
    }

    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        if (event.getEntity() instanceof KingInYellowEntity kingInYellow) {
            collapseDuplicates((ServerLevel)event.getLevel(), kingInYellow);
            kingInYellow.preservePresence();
        }
    }

    public static void onPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        if (event.side() != LogicalSide.SERVER || !(event.player() instanceof ServerPlayer player)) {
            return;
        }

        if (!player.isAlive() || player.isSpectator() || player.getAbilities().invulnerable || player.getAbilities().instabuild) {
            return;
        }

        ServerLevel level = player.level();
        List<KingInYellowEntity> nearbyKings = level.getEntitiesOfClass(KingInYellowEntity.class, player.getBoundingBox().inflate(PRESENCE_RADIUS));

        for (KingInYellowEntity king : nearbyKings) {
            king.preservePresence();
        }

        long pulse = level.getGameTime() + player.getId();

        if (pulse % GAZE_CHECK_INTERVAL == 0L) {
            double maxDistanceSquared = GAZE_RADIUS * GAZE_RADIUS;

            for (KingInYellowEntity king : nearbyKings) {
                if (player.distanceToSqr(king) <= maxDistanceSquared && king.gazesInto(player)) {
                    killInstantly(player, king);
                    return;
                }
            }
        }

        if (pulse % TERROR_CHECK_INTERVAL != 0L) {
            return;
        }

        boolean kingNearby = nearbyKings.stream().anyMatch(king -> player.distanceToSqr(king) <= GAZE_RADIUS * GAZE_RADIUS);
        RandomSource random = level.getRandom();

        if (random.nextInt(kingNearby ? NEARBY_TERROR_CHANCE : BASE_TERROR_CHANCE) == 0) {
            applyTerror(player, kingNearby);
        }

        if (kingNearby) {
            deepenPresence(player, nearbyKings);
        }

        if (pulse % AMBIENCE_CHECK_INTERVAL == 0L && kingNearby) {
            playPresenceSounds(level, player, nearbyKings, random);
        }

        if (pulse % STALK_CHECK_INTERVAL == 0L && kingNearby) {
            stalkPlayer(level, player, nearbyKings, random);
        }

        int spawnChance = isNight(level) ? NIGHT_SPAWN_CHANCE : DAY_SPAWN_CHANCE;
        if (hasAnyKingInLevel(level, player) || random.nextInt(spawnChance) != 0) {
            return;
        }

        trySpawnKing(level, player);
    }

    private static void applyTerror(ServerPlayer player, boolean intensified) {
        int nauseaDuration = intensified ? 260 : 160;
        int darknessDuration = intensified ? 220 : 100;
        int weaknessDuration = intensified ? 180 : 100;
        int slownessDuration = intensified ? 120 : 60;

        player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, nauseaDuration, 0, false, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, darknessDuration, intensified ? 1 : 0, false, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, weaknessDuration, intensified ? 1 : 0, false, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, slownessDuration, 0, false, false, false));
    }

    private static void deepenPresence(ServerPlayer player, List<KingInYellowEntity> nearbyKings) {
        boolean veryClose = nearbyKings.stream().anyMatch(king -> player.distanceToSqr(king) <= 14.0D * 14.0D);
        boolean close = veryClose || nearbyKings.stream().anyMatch(king -> player.distanceToSqr(king) <= 28.0D * 28.0D);

        if (!close) {
            return;
        }

        if (!player.hasEffect(MobEffects.DARKNESS)) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, veryClose ? 1 : 0, false, false, false));
        }

        if (!player.hasEffect(MobEffects.NAUSEA)) {
            player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 80, 0, false, false, false));
        }

        if (veryClose && !player.hasEffect(MobEffects.BLINDNESS)) {
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false, false));
        }
    }

    private static void playPresenceSounds(ServerLevel level, ServerPlayer player, List<KingInYellowEntity> nearbyKings, RandomSource random) {
        KingInYellowEntity nearestKing = nearbyKings.stream()
            .min((left, right) -> Double.compare(player.distanceToSqr(left), player.distanceToSqr(right)))
            .orElse(null);

        if (nearestKing == null) {
            return;
        }

        double distanceSquared = player.distanceToSqr(nearestKing);
        boolean veryClose = distanceSquared <= 14.0D * 14.0D;
        boolean close = distanceSquared <= 28.0D * 28.0D;

        if (!close) {
            return;
        }

        if (random.nextInt(veryClose ? 2 : 4) == 0) {
            playSoundAroundPlayer(level, player, nearestKing, random, SoundEvents.STONE_STEP, veryClose ? 0.85F : 0.55F, veryClose ? 0.7F : 0.9F);
        }

        if (random.nextInt(veryClose ? 3 : 5) == 0) {
            SoundEvent caveSound = random.nextBoolean() ? SoundEvents.AMBIENT_CAVE.value() : SoundEvents.AMBIENT_BASALT_DELTAS_MOOD.value();
            playSoundAroundPlayer(level, player, nearestKing, random, caveSound, veryClose ? 1.2F : 0.9F, veryClose ? 0.65F : 0.85F);
        }
    }

    private static void playSoundAroundPlayer(ServerLevel level, ServerPlayer player, KingInYellowEntity king, RandomSource random, SoundEvent sound, float volume, float pitch) {
        double offsetX = (random.nextDouble() - 0.5D) * 3.0D;
        double offsetZ = (random.nextDouble() - 0.5D) * 3.0D;

        level.playSound(
            null,
            king.getX() + offsetX,
            Math.max(player.getY(), king.getY()),
            king.getZ() + offsetZ,
            sound,
            SoundSource.AMBIENT,
            volume,
            pitch + (float)((random.nextDouble() - 0.5D) * 0.15D)
        );
    }

    private static void stalkPlayer(ServerLevel level, ServerPlayer player, List<KingInYellowEntity> nearbyKings, RandomSource random) {
        KingInYellowEntity nearestKing = nearbyKings.stream()
            .min((left, right) -> Double.compare(player.distanceToSqr(left), player.distanceToSqr(right)))
            .orElse(null);

        if (nearestKing == null) {
            return;
        }

        double distanceSquared = player.distanceToSqr(nearestKing);
        if (distanceSquared > STALK_RADIUS * STALK_RADIUS || nearestKing.gazesInto(player)) {
            return;
        }

        if (distanceSquared < 8.0D * 8.0D || random.nextInt(4) == 0) {
            relocateOutOfSight(level, player, nearestKing, random);
        }
    }

    private static void relocateOutOfSight(ServerLevel level, ServerPlayer player, KingInYellowEntity king, RandomSource random) {
        Vec3 look = player.getLookAngle().normalize();
        double distance = 12.0D + random.nextInt(14);
        double sideOffset = (random.nextBoolean() ? -1.0D : 1.0D) * (5.0D + random.nextDouble() * 8.0D);
        double x = player.getX() - look.x * distance - look.z * sideOffset;
        double z = player.getZ() - look.z * distance + look.x * sideOffset;

        BlockPos surface = level.getHeightmapPos(
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            new BlockPos((int)Math.floor(x), player.getBlockY(), (int)Math.floor(z))
        );
        BlockPos aboveSurface = surface.above();
        BlockPos twoAboveSurface = aboveSurface.above();

        if (!level.isEmptyBlock(surface) || !level.isEmptyBlock(aboveSurface) || !level.isEmptyBlock(twoAboveSurface)) {
            return;
        }

        king.snapTo(surface.getX() + 0.5D, surface.getY(), surface.getZ() + 0.5D, player.getYRot() + 180.0F, 0.0F);
        king.preservePresence();
    }

    private static void killInstantly(ServerPlayer player, KingInYellowEntity king) {
        triggerDeathEffects(player, king);
        DamageSource damageSource = player.damageSources().noAggroMobAttack(king);

        player.hurtServer(player.level(), damageSource, Float.MAX_VALUE);
        if (player.isAlive()) {
            player.setHealth(0.0F);
            player.die(damageSource);
        }
    }

    private static void triggerDeathEffects(ServerPlayer player, KingInYellowEntity king) {
        ServerLevel level = player.level();

        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200, 1, false, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0, false, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 220, 1, false, false, false));

        level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1.0D, player.getZ(), 40, 0.35D, 0.6D, 0.35D, 0.02D);
        level.sendParticles(ParticleTypes.SOUL, player.getX(), player.getY() + 1.0D, player.getZ(), 24, 0.4D, 0.7D, 0.4D, 0.01D);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1.0D, player.getZ(), 28, 0.3D, 0.6D, 0.3D, 0.05D);
        level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 1.2F, 0.6F);
        level.playSound(null, king.blockPosition(), SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 1.0F, 0.5F);
    }

    private static boolean isNight(ServerLevel level) {
        long dayTime = level.getDayTime() % 24000L;
        return dayTime >= 13000L && dayTime <= 23000L;
    }

    private static boolean hasAnyKingInLevel(ServerLevel level, ServerPlayer player) {
        return !level.getEntitiesOfClass(KingInYellowEntity.class, player.getBoundingBox().inflate(UNIQUE_SEARCH_RADIUS)).isEmpty();
    }

    private static void collapseDuplicates(ServerLevel level, KingInYellowEntity currentKing) {
        List<KingInYellowEntity> loadedKings = level.getEntitiesOfClass(
            KingInYellowEntity.class,
            currentKing.getBoundingBox().inflate(UNIQUE_SEARCH_RADIUS)
        );

        for (KingInYellowEntity otherKing : loadedKings) {
            if (otherKing != currentKing) {
                otherKing.discard();
            }
        }
    }

    private static void trySpawnKing(ServerLevel level, ServerPlayer player) {
        Vec3 look = player.getLookAngle().normalize();
        RandomSource random = level.getRandom();
        double distance = 16.0D + random.nextInt(24);
        double sideOffset = (random.nextDouble() - 0.5D) * 16.0D;
        double x = player.getX() - look.x * distance - look.z * sideOffset;
        double z = player.getZ() - look.z * distance + look.x * sideOffset;

        BlockPos surface = level.getHeightmapPos(
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            new BlockPos((int)Math.floor(x), player.getBlockY(), (int)Math.floor(z))
        );
        BlockPos aboveSurface = surface.above();
        BlockPos twoAboveSurface = aboveSurface.above();

        if (!level.isEmptyBlock(surface) || !level.isEmptyBlock(aboveSurface) || !level.isEmptyBlock(twoAboveSurface)) {
            return;
        }

        KingInYellowEntity king = new KingInYellowEntity(ExampleMod.KING_IN_YELLOW.get(), level);
        king.snapTo(
            surface.getX() + 0.5D,
            surface.getY(),
            surface.getZ() + 0.5D,
            (float)(random.nextDouble() * 360.0D),
            0.0F
        );
        king.preservePresence();
        level.addFreshEntity(king);
    }
}
