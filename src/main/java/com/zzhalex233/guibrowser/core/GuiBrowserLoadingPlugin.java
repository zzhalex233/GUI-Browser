package com.zzhalex233.guibrowser.core;

import com.zzhalex233.guibrowser.Reference;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import zone.rong.mixinbooter.IEarlyMixinLoader;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.Name(Reference.MOD_NAME + " Core")
@IFMLLoadingPlugin.TransformerExclusions({"com.zzhalex233.guibrowser.core"})
public final class GuiBrowserLoadingPlugin implements IFMLLoadingPlugin, IEarlyMixinLoader {
    public GuiBrowserLoadingPlugin() {
    }

    @Override
    public List<String> getMixinConfigs() {
        return Collections.singletonList("mixins.guibrowser.json");
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
