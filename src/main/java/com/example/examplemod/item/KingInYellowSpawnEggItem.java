package com.example.examplemod.item;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.entity.KingInYellowEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class KingInYellowSpawnEggItem extends Item {
    public KingInYellowSpawnEggItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();

        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        BlockPos spawnPos = context.getClickedPos().relative(context.getClickedFace());
        KingInYellowEntity king = new KingInYellowEntity(ExampleMod.KING_IN_YELLOW.get(), serverLevel);
        Vec3 centered = Vec3.atBottomCenterOf(spawnPos);

        king.snapTo(centered.x, centered.y, centered.z, context.getRotation(), 0.0F);
        king.preservePresence();

        if (!serverLevel.addFreshEntity(king)) {
            return InteractionResult.FAIL;
        }

        if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }

        return InteractionResult.SUCCESS_SERVER;
    }
}
