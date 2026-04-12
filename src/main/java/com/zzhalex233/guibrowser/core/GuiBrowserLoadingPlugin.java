package com.zzhalex233.guibrowser.core;

import com.zzhalex233.guibrowser.Reference;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

import java.util.Map;

@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.Name(Reference.MOD_NAME + " Core")
@IFMLLoadingPlugin.TransformerExclusions({"com.zzhalex233.guibrowser.core"})
public final class GuiBrowserLoadingPlugin implements IFMLLoadingPlugin {
    private static boolean bootstrapped;

    public GuiBrowserLoadingPlugin() {
        if (bootstrapped) {
            return;
        }

        MixinBootstrap.init();
        Mixins.addConfiguration("mixins.guibrowser.json");
        MixinEnvironment.getDefaultEnvironment().setObfuscationContext("searge");
        bootstrapped = true;
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
