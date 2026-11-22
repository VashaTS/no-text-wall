package net.vsh33.notextwall.test;

import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.vsh33.notextwall.config.NoTextWallConfig;

/**
 * Real client connected to a dedicated Homes+ server. Confirms /listhomes is visible
 * with the default Homes+ exception, and hidden again when {@code allowHomesPlus} is off.
 */
public class HomesPlusCompatClientTest implements FabricClientGameTest {
	private static final Path OUT = Path.of(
		System.getProperty("ntw.realclientShot", "/workspace/sethome-26.2-out/realclient")
	);
	private static final List<String> SHOWN = new CopyOnWriteArrayList<>();

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!FabricLoader.getInstance().isModLoaded("sethome")) {
			System.out.println("[ntw-test] sethome not on the classpath, skipping Homes+ compatibility test");
			return;
		}

		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			if (!overlay) {
				SHOWN.add(message.getString());
			}
		});
		context.runOnClient(mc -> NoTextWallConfig.reset());

		Properties props = new Properties();
		props.setProperty("online-mode", "false");
		props.setProperty("spawn-protection", "0");
		props.setProperty("difficulty", "peaceful");

		try (TestDedicatedServerContext server = context.worldBuilder().createServer(props);
				TestDedicatedServerConnection connection = server.connect()) {
			connection.waitForChunksDownload();
			context.runOnClient(mc -> {
				mc.getWindow().setWindowed(1280, 720);
				mc.options.guiScale().set(3);
				int scale = mc.getWindow().calculateScale(3, mc.isEnforceUnicode());
				mc.getWindow().setGuiScale(scale);
				mc.options.chatOpacity().set(1.0);
				mc.options.chatWidth().set(1.0);
				mc.options.chatHeightFocused().set(1.0);
			});

			server.runCommand("gamerule send_command_feedback false");
			server.runCommand("gamerule announce_advancements false");
			server.runCommand("deop @a");
			context.waitTicks(10);

			// Four named homes so the /listhomes system message is well over 100 characters.
			send(context, connection, "sethome home");
			send(context, connection, "sethome ender");
			send(context, connection, "sethome nether_hub");
			send(context, connection, "sethome spawn_platform");

			context.runOnClient(mc -> mc.gui.hud.getChat().clearMessages(false));
			SHOWN.clear();
			send(context, connection, "listhomes");
			System.out.println("[ntw-test] after /listhomes (allowHomesPlus=true): " + SHOWN);
			check(SHOWN.stream().anyMatch(s -> s.contains("Your homes")), "listhomes should be visible with the default exception, got " + SHOWN);
			check(SHOWN.stream().noneMatch(s -> s.contains("[NoTextWall]")), "notice should not replace a Homes+ list, got " + SHOWN);
			context.waitTicks(10);
			context.takeScreenshot(TestScreenshotOptions.of("listhomes-allowed")
				.disableCounterPrefix()
				.withDestinationDir(OUT));

			// The same list is hidden again when the exception is turned off.
			context.runOnClient(mc -> mc.player.connection.sendCommand("notextwall allowHomesPlus false"));
			connection.waitForClientboundPackets();
			context.waitTicks(5);
			check(!NoTextWallConfig.get().allowHomesPlus, "allowHomesPlus should now be false");

			context.runOnClient(mc -> mc.gui.hud.getChat().clearMessages(false));
			SHOWN.clear();
			send(context, connection, "listhomes");
			System.out.println("[ntw-test] after /listhomes (allowHomesPlus=false): " + SHOWN);
			check(SHOWN.stream().noneMatch(s -> s.contains("Your homes")), "listhomes must be hidden when the exception is off, got " + SHOWN);
			check(SHOWN.stream().anyMatch(s -> s.contains("[NoTextWall]")), "notice should be shown when the exception is off, got " + SHOWN);
			context.waitTicks(10);
			context.takeScreenshot(TestScreenshotOptions.of("listhomes-filtered")
				.disableCounterPrefix()
				.withDestinationDir(OUT));

			// Pattern fallback: a 1.1.1-style list sent as plain tellraw (no insertion tag).
			context.runOnClient(mc -> mc.player.connection.sendCommand("notextwall allowHomesPlus true"));
			connection.waitForClientboundPackets();
			context.waitTicks(5);
			context.runOnClient(mc -> mc.gui.hud.getChat().clearMessages(false));
			SHOWN.clear();
			String fakeList = "Your homes (4):\\n• ender — The End (100, 64, 20)\\n• home — Overworld (10, 70, -30)\\n• nether — The Nether (0, 80, 0)\\n• spawn — Overworld (0, 64, 0)";
			server.runCommand("tellraw @a \"" + fakeList + "\"");
			connection.waitForClientboundPackets();
			context.waitTicks(10);
			System.out.println("[ntw-test] after tellraw 1.1.1-style list: " + SHOWN);
			check(SHOWN.stream().anyMatch(s -> s.contains("Your homes (4)")), "pattern fallback should allow an untagged 1.1.1-style list, got " + SHOWN);
			check(SHOWN.stream().noneMatch(s -> s.contains("[NoTextWall]")), "untagged Homes+ list should not be replaced by a notice, got " + SHOWN);
		} finally {
			context.runOnClient(mc -> NoTextWallConfig.reset());
		}
	}

	private static void send(ClientGameTestContext context, TestDedicatedServerConnection connection, String command) {
		context.runOnClient(mc -> mc.player.connection.sendCommand(command));
		connection.waitForServerboundPackets();
		connection.waitForClientboundPackets();
		context.waitTicks(10);
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
