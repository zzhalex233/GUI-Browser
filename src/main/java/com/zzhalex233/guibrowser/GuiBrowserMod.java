package com.zzhalex233.guibrowser;

import com.zzhalex233.guibrowser.proxy.IProxy;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = Reference.MOD_ID, name = Reference.MOD_NAME, version = Reference.VERSION,
        dependencies = "required-after:mixinbooter"
)

public class GuiBrowserMod {
    public static final Logger LOGGER = LogManager.getLogger(Reference.MOD_ID);

    @SidedProxy(
        modId = Reference.MOD_ID,
        clientSide = "com.zzhalex233.guibrowser.proxy.ClientProxy",
        serverSide = "com.zzhalex233.guibrowser.proxy.CommonProxy"
    )
    public static IProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("Starting {} {}", Reference.MOD_NAME, Reference.VERSION);
        proxy.preInit();
    }
}
