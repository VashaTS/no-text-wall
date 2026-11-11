package net.vsh33.notextwall.test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.vsh33.notextwall.config.NoTextWallConfig;

/**
 * End-to-end test in a real client + singleplayer world (xvfb-run -a ./gradlew runClientGameTest):
 * short messages are shown, walls of text are hidden and replaced by the notice, /notextwall changes the
 * config at runtime and /notextwall show reveals the hidden message.
 */
public class NoTextWallClientTest implements FabricClientGameTest {
	private static final List<String> SHOWN = new CopyOnWriteArrayList<>();

	@Override
	public void runTest(ClientGameTestContext context) {
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> SHOWN.add(message.getString()));
		ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, time) -> SHOWN.add(message.getString()));
		context.runOnClient(mc -> NoTextWallConfig.reset());

		String wall = "WALL " + "lorem ipsum ".repeat(30);
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			var server = world.getServer();
			context.waitTicks(20);
			SHOWN.clear();

			// 1. short system message passes, long one is hidden and replaced by the notice
			server.runCommand("tellraw @a \"short hello\"");
			server.runCommand("tellraw @a \"" + wall + "\"");
			context.waitTicks(10);
			check(SHOWN.contains("short hello"), "short message should be shown");
			check(SHOWN.stream().noneMatch(s -> s.startsWith("WALL")), "wall of text must be hidden");
			check(SHOWN.stream().anyMatch(s -> s.startsWith("[NoTextWall]")), "notice should be shown, got " + SHOWN);
			server.runCommand("tellraw @a \"[Discord] Ania: did you see the castle?\"");
			server.runCommand("tellraw @a \"[Discord] Bob: " + "Castles are fortified structures built during the Middle Ages by predominantly the nobility or royalty and by military orders. ".repeat(4) + "\"");
			server.runCommand("tellraw @a \"[Discord] Ania: ...ok Bob\"");
			context.waitTicks(10);
			context.runOnClient(mc -> mc.gui.hud.getChat().openScreen(net.minecraft.client.gui.components.ChatComponent.ChatMethod.MESSAGE, net.minecraft.client.gui.screens.ChatScreen::new));
			context.waitTicks(5);
			context.takeScreenshot("notextwall_hidden");
			context.setScreen(() -> null);

			// 2. /say goes through player-chat path (signed chat type) - also filtered
			SHOWN.clear();
			server.runCommand("say WALL " + "abc ".repeat(50)); // /say itself is limited to 256 characters
			context.waitTicks(10);
			check(SHOWN.stream().noneMatch(s -> s.contains("WALL")), "/say wall must be hidden, got " + SHOWN);
			check(SHOWN.stream().anyMatch(s -> s.startsWith("[NoTextWall]")), "notice for /say should be shown, got " + SHOWN);

			// 3. client command reveals the hidden message
			SHOWN.clear();
			context.runOnClient(mc -> mc.player.connection.sendCommand("notextwall show 1"));
			context.waitTicks(10);
			check(SHOWN.stream().anyMatch(s -> s.startsWith("WALL")), "show 1 should reveal the wall, got " + SHOWN);

			// 4. client command changes the config (planned 0.3 feature) and it applies immediately
			context.runOnClient(mc -> mc.player.connection.sendCommand("notextwall maxLength 5"));
			context.waitTicks(5);
			check(NoTextWallConfig.get().maxMessageLength == 5, "maxLength command should set 5, is " + NoTextWallConfig.get().maxMessageLength);
			SHOWN.clear();
			server.runCommand("tellraw @a \"short hello\"");
			context.waitTicks(10);
			check(!SHOWN.contains("short hello"), "with maxLength 5 the short message is hidden too");

			context.runOnClient(mc -> mc.player.connection.sendCommand("notextwall off"));
			context.waitTicks(5);
			SHOWN.clear();
			server.runCommand("tellraw @a \"" + wall + "\"");
			context.waitTicks(10);
			check(SHOWN.stream().anyMatch(s -> s.startsWith("WALL")), "with the filter off everything is shown");
			context.takeScreenshot("notextwall_chat");
		}
		context.runOnClient(mc -> NoTextWallConfig.reset());

		// Cloth Config screen (only when run with -PwithConfigMods=true)
		if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("cloth-config")) {
			context.setScreen(() -> net.vsh33.notextwall.compat.NoTextWallConfigScreen.create(null));
			context.waitTicks(10);
			context.takeScreenshot("notextwall_config");
			context.setScreen(() -> null);
		}
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
