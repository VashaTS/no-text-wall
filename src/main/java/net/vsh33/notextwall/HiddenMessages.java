package net.vsh33.notextwall;

import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.network.chat.Component;

/** Remembers the last few hidden messages so they can be revealed on demand ({@code /notextwall show <id>}). */
public final class HiddenMessages {
	public static final int CAPACITY = 50;

	public record Entry(int id, Component message) {
	}

	private static final Deque<Entry> ENTRIES = new ArrayDeque<>();
	private static int nextId = 1;

	public static synchronized Entry add(Component message) {
		Entry entry = new Entry(nextId++, message);
		ENTRIES.addLast(entry);
		while (ENTRIES.size() > CAPACITY) {
			ENTRIES.removeFirst();
		}
		return entry;
	}

	public static synchronized Entry get(int id) {
		for (Entry entry : ENTRIES) {
			if (entry.id() == id) {
				return entry;
			}
		}
		return null;
	}

	public static synchronized Entry last() {
		return ENTRIES.peekLast();
	}

	public static synchronized int size() {
		return ENTRIES.size();
	}

	public static synchronized void clear() {
		ENTRIES.clear();
	}

	private HiddenMessages() {
	}
}
