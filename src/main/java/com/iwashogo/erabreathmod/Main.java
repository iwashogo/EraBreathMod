package com.iwashogo.erabreathmod;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBucketMilk;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.potion.PotionEffect;
import net.minecraft.stats.StatList;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.PotionEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.event.ItemEvent;
import java.util.stream.Collectors;
@Mod("erabreathmod")
public class Main {
    //counter for tickEvent
    public int tickCount = 0;
    //counter for POISON
    public int poisonCount = 0;
    // Directly reference a log4j logger.
    private static final Logger LOGGER = LogManager.getLogger();

    public Main() {
        // Register the setup method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        // Register the enqueueIMC method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::enqueueIMC);
        // Register the processIMC method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::processIMC);
        // Register the doClientStuff method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event)
    {
        // some preinit code
        LOGGER.info("HELLO FROM PREINIT");
        LOGGER.info("DIRT BLOCK >> {}", Blocks.DIRT.getRegistryName());
    }

    private void doClientStuff(final FMLClientSetupEvent event) {
        // do something that can only be done on the client
        LOGGER.info("Got game settings {}", event.getMinecraftSupplier().get().gameSettings);
    }

    private void enqueueIMC(final InterModEnqueueEvent event)
    {
        // some example code to dispatch IMC to another mod
        InterModComms.sendTo("examplemod", "helloworld", () -> { LOGGER.info("Hello world from the MDK"); return "Hello world";});
    }

    private void processIMC(final InterModProcessEvent event)
    {
        // some example code to receive and process InterModComms from other mods
        LOGGER.info("Got IMC {}", event.getIMCStream().
                map(m->m.getMessageSupplier().get()).
                collect(Collectors.toList()));
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) {
        // do something when the server starts
        LOGGER.info("HELLO from server starting");
    }
    @SubscribeEvent
    public void onRespawnEffect(PlayerEvent.PlayerRespawnEvent event){
        event.getPlayer().addPotionEffect(new PotionEffect(MobEffects.POISON, 1000,0));//reset poisonCount
    }

    @SubscribeEvent
    public void onDrinkMilk(PlayerInteractEvent.RightClickItem event){
        if(event.getItemStack().getDisplayName().getFormattedText().equals("Milk Bucket")){
            LOGGER.info("Milk!!");
            poisonCount = 30;
        }
    }

    @SubscribeEvent
    public void onWaterEffects(TickEvent.PlayerTickEvent event) {

        if(++tickCount % 50 == 0){
            if(!event.player.getEntityWorld().isRemote()){
                // Add PotionEffect "WATER_BREATHING" when Player is in the water
                if(event.player.isInWater()){
                    LOGGER.info("YOU ARE IN WATER!");
                    //add good potion for living in water
                    event.player.addPotionEffect(new PotionEffect(MobEffects.WATER_BREATHING,100000,0));
                    event.player.addPotionEffect(new PotionEffect(MobEffects.CONDUIT_POWER,10000,0));
                    //cure the POISON
                    if(event.player.isPotionActive(MobEffects.POISON)){
                        event.player.removePotionEffect(MobEffects.POISON);
                    }
                    poisonCount = 1; //Player will have poison just after go out from the water
                }else{ //Add PotionEffect "POISON" when Player is in the air
                    if(poisonCount < 1){
                        if(event.player.isPotionActive(MobEffects.POISON)){
                            event.player.removePotionEffect(MobEffects.POISON);
                        }
                        LOGGER.info("POISON result: "+event.player.addPotionEffect(new PotionEffect(MobEffects.POISON, 1000,0)));
                        poisonCount = 50;
                    }else{
                        --poisonCount;
                        LOGGER.info("poison:" + poisonCount);
                    }
                }
                if(event.player.getHealth() <= 1.0){
                    event.player.attackEntityFrom(DamageSource.GENERIC,2);
                }
            }
        }
        if(tickCount == Integer.MAX_VALUE || tickCount < 0) tickCount = 0;
    }
    //remove expired potion effects(in this case, the POISON effect will be removed)
    @SubscribeEvent
    public void onPotionExpiry(PotionEvent.PotionExpiryEvent event){
        LOGGER.info("potion expiry!!");

        if(event.getEntityLiving().isPotionActive(MobEffects.POISON)){
            event.getEntityLiving().removePotionEffect(MobEffects.POISON);
        }
    }

    // You can use EventBusSubscriber to automatically subscribe events on the contained class (this is subscribing to the MOD
    // Event bus for receiving Registry Events)
    @Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.MOD)
    public static class RegistryEvents {
        @SubscribeEvent
        public static void onBlocksRegistry(final RegistryEvent.Register<Block> blockRegistryEvent) {
            // register a new block here
            LOGGER.info("HELLO from Register Block");
        }
    }
}
