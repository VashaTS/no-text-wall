package net.vsh33.notextwall.compat;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.vsh33.notextwall.config.NoTextWallConfig;

/** Cloth Config screen for {@link NoTextWallConfig}. Only loaded when Cloth Config is present. */
public final class NoTextWallConfigScreen {
	public static Screen create(Screen parent) {
		NoTextWallConfig config = NoTextWallConfig.get();
		NoTextWallConfig defaults = new NoTextWallConfig();
		ConfigBuilder builder = ConfigBuilder.create()
				.setParentScreen(parent)
				.setTitle(Component.translatable("config.notextwall.title"))
				.setSavingRunnable(() -> {
					config.validate();
					NoTextWallConfig.save();
				});
		ConfigEntryBuilder entries = builder.entryBuilder();

		ConfigCategory filter = builder.getOrCreateCategory(Component.translatable("config.notextwall.category.filter"));
		filter.addEntry(entries.startBooleanToggle(Component.translatable("config.notextwall.enabled"), config.enabled)
				.setDefaultValue(defaults.enabled)
				.setTooltip(Component.translatable("config.notextwall.enabled.tooltip"))
				.setSaveConsumer(v -> config.enabled = v)
				.build());
		filter.addEntry(entries.startIntField(Component.translatable("config.notextwall.max_length"), config.maxMessageLength)
				.setDefaultValue(defaults.maxMessageLength)
				.setMin(NoTextWallConfig.MIN_LENGTH)
				.setMax(NoTextWallConfig.MAX_LENGTH)
				.setTooltip(Component.translatable("config.notextwall.max_length.tooltip"))
				.setSaveConsumer(v -> config.maxMessageLength = v)
				.build());
		filter.addEntry(entries.startIntField(Component.translatable("config.notextwall.max_lines"), config.maxLines)
				.setDefaultValue(defaults.maxLines)
				.setMin(0)
				.setMax(NoTextWallConfig.MAX_LINES_LIMIT)
				.setTooltip(Component.translatable("config.notextwall.max_lines.tooltip"))
				.setSaveConsumer(v -> config.maxLines = v)
				.build());
		filter.addEntry(entries.startBooleanToggle(Component.translatable("config.notextwall.filter_player_chat"), config.filterPlayerChat)
				.setDefaultValue(defaults.filterPlayerChat)
				.setTooltip(Component.translatable("config.notextwall.filter_player_chat.tooltip"))
				.setSaveConsumer(v -> config.filterPlayerChat = v)
				.build());
		filter.addEntry(entries.startBooleanToggle(Component.translatable("config.notextwall.filter_system"), config.filterSystemMessages)
				.setDefaultValue(defaults.filterSystemMessages)
				.setTooltip(Component.translatable("config.notextwall.filter_system.tooltip"))
				.setSaveConsumer(v -> config.filterSystemMessages = v)
				.build());

		ConfigCategory notice = builder.getOrCreateCategory(Component.translatable("config.notextwall.category.notice"));
		notice.addEntry(entries.startBooleanToggle(Component.translatable("config.notextwall.show_cancel_message"), config.showCancelMessage)
				.setDefaultValue(defaults.showCancelMessage)
				.setTooltip(Component.translatable("config.notextwall.show_cancel_message.tooltip"))
				.setSaveConsumer(v -> config.showCancelMessage = v)
				.build());
		notice.addEntry(entries.startBooleanToggle(Component.translatable("config.notextwall.show_preview"), config.showPreview)
				.setDefaultValue(defaults.showPreview)
				.setTooltip(Component.translatable("config.notextwall.show_preview.tooltip"))
				.setSaveConsumer(v -> config.showPreview = v)
				.build());
		return builder.build();
	}

	private NoTextWallConfigScreen() {
	}
}
