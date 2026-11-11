package net.vsh33.notextwall;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.vsh33.notextwall.MessageFilter.Kind;
import net.vsh33.notextwall.MessageFilter.Reason;
import net.vsh33.notextwall.config.NoTextWallConfig;
import org.junit.jupiter.api.Test;

class MessageFilterTest {
	private static NoTextWallConfig config() {
		return new NoTextWallConfig();
	}

	@Test
	void defaultsMatchOriginalMod() {
		NoTextWallConfig c = config();
		assertEquals(100, c.maxMessageLength);
		assertEquals(true, c.showCancelMessage);
		assertEquals(true, c.enabled);
		assertEquals(0, c.maxLines);
	}

	@Test
	void shortMessagePasses() {
		assertEquals(Reason.NONE, MessageFilter.check("hello", Kind.SYSTEM, config()));
		assertEquals(Reason.NONE, MessageFilter.check("x".repeat(100), Kind.PLAYER_CHAT, config()));
	}

	@Test
	void longMessageIsHidden() {
		assertEquals(Reason.TOO_LONG, MessageFilter.check("x".repeat(101), Kind.SYSTEM, config()));
		assertEquals(Reason.TOO_LONG, MessageFilter.check("x".repeat(101), Kind.PLAYER_CHAT, config()));
	}

	@Test
	void lengthCountsCodePointsNotUtf16Units() {
		String emoji = "\uD83E\uDD95"; // sauropod emoji, 2 UTF-16 chars
		assertEquals(Reason.NONE, MessageFilter.check(emoji.repeat(100), Kind.SYSTEM, config()));
		assertEquals(Reason.TOO_LONG, MessageFilter.check(emoji.repeat(101), Kind.SYSTEM, config()));
	}

	@Test
	void customLength() {
		NoTextWallConfig c = config();
		c.maxMessageLength = 10;
		assertEquals(Reason.TOO_LONG, MessageFilter.check("12345678901", Kind.SYSTEM, c));
		assertEquals(Reason.NONE, MessageFilter.check("1234567890", Kind.SYSTEM, c));
	}

	@Test
	void lineLimit() {
		NoTextWallConfig c = config();
		String text = "a\nb\nc\nd";
		assertEquals(Reason.NONE, MessageFilter.check(text, Kind.SYSTEM, c), "line limit off by default");
		c.maxLines = 3;
		assertEquals(Reason.TOO_MANY_LINES, MessageFilter.check(text, Kind.SYSTEM, c));
		c.maxLines = 4;
		assertEquals(Reason.NONE, MessageFilter.check(text, Kind.SYSTEM, c));
	}

	@Test
	void lineCount() {
		assertEquals(0, MessageFilter.lineCount(""));
		assertEquals(1, MessageFilter.lineCount("abc"));
		assertEquals(3, MessageFilter.lineCount("a\r\nb\nc"));
		assertEquals(2, MessageFilter.lineCount("a\rb"));
	}

	@Test
	void switches() {
		NoTextWallConfig c = config();
		String wall = "x".repeat(500);
		c.enabled = false;
		assertEquals(Reason.NONE, MessageFilter.check(wall, Kind.SYSTEM, c));
		c.enabled = true;
		c.filterPlayerChat = false;
		assertEquals(Reason.NONE, MessageFilter.check(wall, Kind.PLAYER_CHAT, c));
		assertEquals(Reason.TOO_LONG, MessageFilter.check(wall, Kind.SYSTEM, c));
		c.filterPlayerChat = true;
		c.filterSystemMessages = false;
		assertEquals(Reason.NONE, MessageFilter.check(wall, Kind.SYSTEM, c));
		assertEquals(Reason.TOO_LONG, MessageFilter.check(wall, Kind.PLAYER_CHAT, c));
	}

	@Test
	void validateClamps() {
		NoTextWallConfig c = config();
		c.maxMessageLength = -5;
		c.maxLines = 1000;
		c.validate();
		assertEquals(NoTextWallConfig.MIN_LENGTH, c.maxMessageLength);
		assertEquals(NoTextWallConfig.MAX_LINES_LIMIT, c.maxLines);
	}

	@Test
	void preview() {
		assertEquals("abc", MessageFilter.preview("abc", 5));
		assertEquals("ab...", MessageFilter.preview("abcdef", 2));
	}
}
