package net.vsh33.notextwall.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;

/** Mod Menu entry point: opens the Cloth Config screen when Cloth Config is installed, otherwise no config button. */
public class ModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		if (FabricLoader.getInstance().isModLoaded("cloth-config2") || FabricLoader.getInstance().isModLoaded("cloth-config")) {
			return NoTextWallConfigScreen::create;
		}
		return parent -> null;
	}
}
