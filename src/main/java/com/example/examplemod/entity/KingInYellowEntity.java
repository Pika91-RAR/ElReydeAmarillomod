package com.example.examplemod.entity;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;

public final class KingInYellowEntity extends Zombie {
    public KingInYellowEntity(EntityType<? extends KingInYellowEntity> entityType, Level level) {
        super(entityType, level);
        this.setSilent(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
            .add(Attributes.MAX_HEALTH, 72.0D)
            .add(Attributes.FOLLOW_RANGE, 64.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.31D)
            .add(Attributes.ATTACK_DAMAGE, 12.0D)
            .add(Attributes.ARMOR, 6.0D)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    public boolean gazesInto(ServerPlayer player) {
        return this.isAlive()
            && player.isAlive()
            && this.isLookingAtMe(player, 0.025D, true, false, this.getEyeY(), this.getY() + 1.15D, this.getY() + 0.35D);
    }

    public void preservePresence() {
        this.setSilent(true);

        if (!this.hasEffect(MobEffects.FIRE_RESISTANCE)) {
            this.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false, false));
        }

        if (!this.hasEffect(MobEffects.RESISTANCE)) {
            this.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, Integer.MAX_VALUE, 1, false, false, false));
        }

        if (!this.hasEffect(MobEffects.SPEED)) {
            this.addEffect(new MobEffectInstance(MobEffects.SPEED, Integer.MAX_VALUE, 0, false, false, false));
        }

        if (this.isOnFire()) {
            this.clearFire();
        }
    }
}
