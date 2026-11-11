package net.vsh33.notextwall;

import net.vsh33.notextwall.config.NoTextWallConfig;

/** Pure decision logic (no Minecraft classes) so it can be unit tested. */
public final class MessageFilter {
	public enum Kind { PLAYER_CHAT, SYSTEM }

	public enum Reason { NONE, TOO_LONG, TOO_MANY_LINES }

	public static Reason check(String text, Kind kind, NoTextWallConfig config) {
		if (text == null || !config.enabled) {
			return Reason.NONE;
		}
		if (kind == Kind.PLAYER_CHAT && !config.filterPlayerChat) {
			return Reason.NONE;
		}
		if (kind == Kind.SYSTEM && !config.filterSystemMessages) {
			return Reason.NONE;
		}
		if (text.codePointCount(0, text.length()) > config.maxMessageLength) {
			return Reason.TOO_LONG;
		}
		if (config.maxLines > 0 && lineCount(text) > config.maxLines) {
			return Reason.TOO_MANY_LINES;
		}
		return Reason.NONE;
	}

	public static int lineCount(String text) {
		if (text.isEmpty()) {
			return 0;
		}
		int lines = 1;
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c == '\n') {
				lines++;
			} else if (c == '\r') {
				lines++;
				if (i + 1 < text.length() && text.charAt(i + 1) == '\n') {
					i++;
				}
			}
		}
		return lines;
	}

	/** First {@code max} code points of the text, with "..." appended when it was cut. */
	public static String preview(String text, int max) {
		if (text.codePointCount(0, text.length()) <= max) {
			return text;
		}
		return text.substring(0, text.offsetByCodePoints(0, max)) + "...";
	}

	private MessageFilter() {
	}
}
