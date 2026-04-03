package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.entity.KingInYellowEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.zombie.ZombieModel;
import net.minecraft.client.renderer.entity.AbstractZombieRenderer;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.Identifier;

public final class KingInYellowRenderer extends AbstractZombieRenderer<KingInYellowEntity, ZombieRenderState, ZombieModel<ZombieRenderState>> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(ExampleMod.MODID, "textures/entity/king_in_yellow.png");

    public KingInYellowRenderer(EntityRendererProvider.Context context) {
        super(
            context,
            new ZombieModel<>(context.bakeLayer(ModelLayers.ZOMBIE)),
            new ZombieModel<>(context.bakeLayer(ModelLayers.ZOMBIE_BABY)),
            ArmorModelSet.bake(ModelLayers.ZOMBIE_ARMOR, context.getModelSet(), ZombieModel::new),
            ArmorModelSet.bake(ModelLayers.ZOMBIE_BABY_ARMOR, context.getModelSet(), ZombieModel::new)
        );
    }

    @Override
    public ZombieRenderState createRenderState() {
        return new ZombieRenderState();
    }

    @Override
    public Identifier getTextureLocation(ZombieRenderState state) {
        return TEXTURE;
    }
}
