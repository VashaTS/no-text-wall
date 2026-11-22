package net.vsh33.notextwall;

import net.vsh33.notextwall.config.NoTextWallConfig;

/** Pure decision logic (no Minecraft classes) so it can be unit tested. */
public final class MessageFilter {
	public enum Kind { PLAYER_CHAT, SYSTEM }

	public enum Reason { NONE, TOO_LONG, TOO_MANY_LINES }

	/** Style insertion Homes+ puts on its system messages so this filter can recognise them. */
	public static final String HOMESPLUS_MARKER = "homesplus";

	public static Reason check(String text, Kind kind, NoTextWallConfig config) {
		return check(text, kind, config, false);
	}

	public static Reason check(String text, Kind kind, NoTextWallConfig config, boolean homesPlusMarker) {
		if (text == null || !config.enabled) {
			return Reason.NONE;
		}
		if (kind == Kind.PLAYER_CHAT && !config.filterPlayerChat) {
			return Reason.NONE;
		}
		if (kind == Kind.SYSTEM && !config.filterSystemMessages) {
			return Reason.NONE;
		}
		if (config.allowHomesPlus && (homesPlusMarker || looksLikeHomesPlus(text))) {
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

	/**
	 * Fallback for Homes+ 1.1.1 (and earlier) which does not yet tag messages:
	 * a "/listhomes" list starts with "Your homes (N):" or "Homes for <name> (N):"
	 * and then a bullet line.
	 */
	public static boolean looksLikeHomesPlus(String text) {
		if (text == null || text.isEmpty()) {
			return false;
		}
		boolean titled = text.startsWith("Your homes (") || text.startsWith("Homes for ");
		if (!titled) {
			return false;
		}
		int headingEnd = text.indexOf("):");
		return headingEnd >= 0 && text.indexOf("\n• ", headingEnd) >= 0;
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
