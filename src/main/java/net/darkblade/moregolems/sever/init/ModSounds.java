package net.darkblade.moregolems.sever.init;

import net.darkblade.moregolems.MoreGolems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MoreGolems.MODID);


    public static final RegistryObject<SoundEvent> CACTUS_ATTACK = registerSoundEvent("cactus_golem_attack");
    public static final RegistryObject<SoundEvent> CACTUS_HURT = registerSoundEvent("cactus_golem_hurt");
    public static final RegistryObject<SoundEvent> CACTUS_DEATH = registerSoundEvent("cactus_golem_death");
    public static final RegistryObject<SoundEvent> CACTUS_WALK = registerSoundEvent("cactus_golem_walk");

    public static final RegistryObject<SoundEvent> GOLD_GOLEM_REFLECT = registerSoundEvent("gold_golem_reflect");
    public static final RegistryObject<SoundEvent> GOLD_SWORD_TRANSFORM = registerSoundEvent("gold_sword_transform");

    public static final RegistryObject<SoundEvent> CASTLE_DEATH = registerSoundEvent("castle_golem_death");
    public static final RegistryObject<SoundEvent> CASTLE_HURT = registerSoundEvent("castle_golem_hurt");
    public static final RegistryObject<SoundEvent> CASTLE_WALK = registerSoundEvent("castle_golem_walk");
    public static final RegistryObject<SoundEvent> CASTLE_HEARTBEAT = registerSoundEvent("castle_golem_heartbeat");
    public static final RegistryObject<SoundEvent> CASTLE_SMASH = registerSoundEvent("castle_smash");

    public static final RegistryObject<SoundEvent> BLOWGUN_SHOOT = registerSoundEvent("blowgun_shoot");
    public static final RegistryObject<SoundEvent> CONCRETE_SHIELD = registerSoundEvent("concrete_shield");


    private static RegistryObject<SoundEvent> registerSoundEvent(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MoreGolems.MODID, name)));
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}