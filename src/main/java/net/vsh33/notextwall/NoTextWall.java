package net.vsh33.notextwall;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.PlayerChatMessage;
import net.vsh33.notextwall.command.NoTextWallCommand;
import net.vsh33.notextwall.config.NoTextWallConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Client-only: hides incoming chat / system messages that are too long ("walls of text"). */
public class NoTextWall implements ClientModInitializer {
	public static final String MOD_ID = "notextwall";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final int PREVIEW_LENGTH = 300;

	/** Set while this mod itself prints a message, so revealed messages and notices are never filtered. */
	private static boolean bypass;

	@Override
	public void onInitializeClient() {
		NoTextWallConfig.load();

		ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
			// Like the original mod: for signed player chat check the text the player typed (without the "<name>" decoration).
			String text = signedMessage != null ? rawContent(signedMessage) : message.getString();
			return allow(message, text, MessageFilter.Kind.PLAYER_CHAT);
		});
		ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
			if (overlay) {
				return true; // action bar text is a single short line, never a wall
			}
			return allow(message, message.getString(), MessageFilter.Kind.SYSTEM);
		});
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> NoTextWallCommand.register(dispatcher));
		LOGGER.info("NoTextWall loaded (max {} characters)", NoTextWallConfig.get().maxMessageLength);
	}

	private static String rawContent(PlayerChatMessage signedMessage) {
		Component unsigned = signedMessage.unsignedContent();
		return unsigned != null ? unsigned.getString() : signedMessage.signedContent();
	}

	private static boolean allow(Component message, String text, MessageFilter.Kind kind) {
		if (bypass) {
			return true;
		}
		NoTextWallConfig config = NoTextWallConfig.get();
		MessageFilter.Reason reason = MessageFilter.check(text, kind, config);
		if (reason == MessageFilter.Reason.NONE) {
			return true;
		}
		HiddenMessages.Entry entry = HiddenMessages.add(message);
		if (config.showCancelMessage) {
			print(notice(entry, text, reason, config));
		}
		return false;
	}

	private static Component notice(HiddenMessages.Entry entry, String text, MessageFilter.Reason reason, NoTextWallConfig config) {
		int length = text.codePointCount(0, text.length());
		Component why = reason == MessageFilter.Reason.TOO_MANY_LINES
				? Component.translatable("notextwall.notice.lines", MessageFilter.lineCount(text))
				: Component.translatable("notextwall.notice.chars", length);
		MutableComponent notice = prefix().append(Component.translatable("notextwall.notice", why).withStyle(ChatFormatting.GRAY));
		if (config.showPreview) {
			Component hover = Component.literal(MessageFilter.preview(text, PREVIEW_LENGTH))
					.append(Component.literal("\n\n"))
					.append(Component.translatable("notextwall.notice.click").withStyle(ChatFormatting.YELLOW));
			notice.append(Component.literal(" "))
					.append(Component.translatable("notextwall.notice.show").withStyle(style -> style
							.withColor(ChatFormatting.AQUA)
							.withUnderlined(true)
							.withHoverEvent(new HoverEvent.ShowText(hover))
							.withClickEvent(new ClickEvent.RunCommand("/notextwall show " + entry.id()))));
		}
		return notice;
	}

	public static MutableComponent prefix() {
		return Component.literal("[NoTextWall] ").withStyle(ChatFormatting.DARK_AQUA);
	}

	/** Prints a message into the local chat without it being filtered by this mod. */
	public static void print(Component message) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null) {
			return;
		}
		boolean previous = bypass;
		bypass = true;
		try {
			minecraft.player.sendSystemMessage(message);
		} finally {
			bypass = previous;
		}
	}
}
