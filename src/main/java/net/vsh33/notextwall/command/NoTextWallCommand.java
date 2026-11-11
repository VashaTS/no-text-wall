package net.vsh33.notextwall.command;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.vsh33.notextwall.HiddenMessages;
import net.vsh33.notextwall.NoTextWall;
import net.vsh33.notextwall.config.NoTextWallConfig;

/**
 * Client-side command {@code /notextwall} (alias {@code /ntw}) - the "/command change of config" planned for 0.3.
 * Every change is saved to config/notextwall.json right away and applies without a restart.
 */
public final class NoTextWallCommand {
	public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		dispatcher.register(build("notextwall"));
		dispatcher.register(build("ntw"));
	}

	private static LiteralArgumentBuilder<FabricClientCommandSource> build(String name) {
		return literal(name)
				.executes(NoTextWallCommand::status)
				.then(literal("status").executes(NoTextWallCommand::status))
				.then(literal("on").executes(ctx -> setBool(ctx, "enabled", true, (c, v) -> c.enabled = v)))
				.then(literal("off").executes(ctx -> setBool(ctx, "enabled", false, (c, v) -> c.enabled = v)))
				.then(literal("maxLength")
						.executes(ctx -> showInt(ctx, "maxLength", c -> c.maxMessageLength))
						.then(argument("characters", IntegerArgumentType.integer(NoTextWallConfig.MIN_LENGTH, NoTextWallConfig.MAX_LENGTH))
								.executes(ctx -> setInt(ctx, "maxLength", IntegerArgumentType.getInteger(ctx, "characters"), (c, v) -> c.maxMessageLength = v))))
				.then(literal("maxLines")
						.executes(ctx -> showInt(ctx, "maxLines", c -> c.maxLines))
						.then(argument("lines", IntegerArgumentType.integer(0, NoTextWallConfig.MAX_LINES_LIMIT))
								.executes(ctx -> setInt(ctx, "maxLines", IntegerArgumentType.getInteger(ctx, "lines"), (c, v) -> c.maxLines = v))))
				.then(boolOption("showCancelMessage", (c, v) -> c.showCancelMessage = v))
				.then(boolOption("showPreview", (c, v) -> c.showPreview = v))
				.then(boolOption("filterPlayerChat", (c, v) -> c.filterPlayerChat = v))
				.then(boolOption("filterSystemMessages", (c, v) -> c.filterSystemMessages = v))
				.then(literal("reload").executes(ctx -> {
					NoTextWallConfig.load();
					feedback(ctx, Component.translatable("notextwall.command.reloaded"));
					return status(ctx);
				}))
				.then(literal("reset").executes(ctx -> {
					NoTextWallConfig.reset();
					feedback(ctx, Component.translatable("notextwall.command.reset"));
					return status(ctx);
				}))
				.then(literal("last").executes(ctx -> show(ctx, HiddenMessages.last())))
				.then(literal("show")
						.then(argument("id", IntegerArgumentType.integer(1))
								.executes(ctx -> show(ctx, HiddenMessages.get(IntegerArgumentType.getInteger(ctx, "id"))))));
	}

	private static LiteralArgumentBuilder<FabricClientCommandSource> boolOption(String name, BiConsumer<NoTextWallConfig, Boolean> setter) {
		return literal(name)
				.then(argument("value", BoolArgumentType.bool())
						.executes(ctx -> setBool(ctx, name, BoolArgumentType.getBool(ctx, "value"), setter)));
	}

	private static int setBool(CommandContext<FabricClientCommandSource> ctx, String name, boolean value, BiConsumer<NoTextWallConfig, Boolean> setter) {
		setter.accept(NoTextWallConfig.get(), value);
		NoTextWallConfig.save();
		feedback(ctx, Component.translatable("notextwall.command.set", name, onOff(value)));
		return 1;
	}

	private static int setInt(CommandContext<FabricClientCommandSource> ctx, String name, int value, BiConsumer<NoTextWallConfig, Integer> setter) {
		setter.accept(NoTextWallConfig.get(), value);
		NoTextWallConfig.get().validate();
		NoTextWallConfig.save();
		feedback(ctx, Component.translatable("notextwall.command.set", name, Component.literal(String.valueOf(value)).withStyle(ChatFormatting.WHITE)));
		return 1;
	}

	private static int showInt(CommandContext<FabricClientCommandSource> ctx, String name, Function<NoTextWallConfig, Integer> getter) {
		feedback(ctx, Component.translatable("notextwall.command.value", name, Component.literal(String.valueOf(getter.apply(NoTextWallConfig.get()))).withStyle(ChatFormatting.WHITE)));
		return 1;
	}

	private static int status(CommandContext<FabricClientCommandSource> ctx) {
		NoTextWallConfig c = NoTextWallConfig.get();
		feedback(ctx, Component.translatable("notextwall.command.status",
				onOff(c.enabled),
				num(c.maxMessageLength),
				c.maxLines > 0 ? num(c.maxLines) : Component.translatable("notextwall.command.no_limit").withStyle(ChatFormatting.WHITE),
				onOff(c.showCancelMessage),
				onOff(c.showPreview),
				onOff(c.filterPlayerChat),
				onOff(c.filterSystemMessages),
				num(HiddenMessages.size())));
		return 1;
	}

	private static int show(CommandContext<FabricClientCommandSource> ctx, HiddenMessages.Entry entry) {
		if (entry == null) {
			ctx.getSource().sendError(Component.translatable("notextwall.command.not_found"));
			return 0;
		}
		NoTextWall.print(NoTextWall.prefix().append(Component.translatable("notextwall.command.revealed", entry.id()).withStyle(ChatFormatting.GRAY)));
		NoTextWall.print(entry.message());
		return 1;
	}

	private static Component onOff(boolean value) {
		return Component.translatable(value ? "options.on" : "options.off").withStyle(value ? ChatFormatting.GREEN : ChatFormatting.RED);
	}

	private static Component num(int value) {
		return Component.literal(String.valueOf(value)).withStyle(ChatFormatting.WHITE);
	}

	private static void feedback(CommandContext<FabricClientCommandSource> ctx, Component message) {
		// Printed through NoTextWall.print so the status (which can be long) is never hidden by the mod itself.
		NoTextWall.print(NoTextWall.prefix().append(message.copy().withStyle(ChatFormatting.GRAY)));
	}

	private NoTextWallCommand() {
	}
}
