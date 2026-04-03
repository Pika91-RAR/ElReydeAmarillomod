package com.example.examplemod;

import com.example.examplemod.client.KingInYellowClient;
import com.example.examplemod.entity.KingInYellowEntity;
import com.example.examplemod.item.KingInYellowSpawnEggItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(ExampleMod.MODID)
public final class ExampleMod {
    public static final String MODID = "elreyamarillo";

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final RegistryObject<EntityType<KingInYellowEntity>> KING_IN_YELLOW = ENTITY_TYPES.register(
        "king_in_yellow",
        () -> EntityType.Builder.of(KingInYellowEntity::new, MobCategory.MONSTER)
            .sized(0.6F, 1.95F)
            .eyeHeight(1.74F)
            .setTrackingRange(10)
            .setUpdateInterval(2)
            .build(ENTITY_TYPES.key("king_in_yellow"))
    );
    public static final RegistryObject<Item> KING_IN_YELLOW_SPAWN_EGG = ITEMS.register(
        "king_in_yellow_spawn_egg",
        () -> new KingInYellowSpawnEggItem(new Item.Properties().setId(ITEMS.key("king_in_yellow_spawn_egg")))
    );
    public static final RegistryObject<CreativeModeTab> KING_IN_YELLOW_TAB = CREATIVE_MODE_TABS.register(
        "king_in_yellow_tab",
        () -> CreativeModeTab.builder()
            .title(net.minecraft.network.chat.Component.translatable("itemGroup.elreyamarillo"))
            .icon(() -> new ItemStack(KING_IN_YELLOW_SPAWN_EGG.get()))
            .displayItems((parameters, output) -> output.accept(KING_IN_YELLOW_SPAWN_EGG.get()))
            .build()
    );

    public ExampleMod(FMLJavaModLoadingContext context) {
        var modBusGroup = context.getModBusGroup();

        ENTITY_TYPES.register(modBusGroup);
        ITEMS.register(modBusGroup);
        CREATIVE_MODE_TABS.register(modBusGroup);

        EntityAttributeCreationEvent.BUS.addListener(ExampleMod::registerEntityAttributes);
        TickEvent.PlayerTickEvent.Post.BUS.addListener(KingInYellowEvents::onPlayerTick);
        EntityJoinLevelEvent.BUS.addListener(KingInYellowEvents::onEntityJoinLevel);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            KingInYellowClient.register();
        }
    }

    private static void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(KING_IN_YELLOW.get(), KingInYellowEntity.createAttributes().build());
    }
}
