# Changelog

## 1.0.1 (Minecraft 26.2 / 26.3, Fabric)
- Homes+ exception: `/listhomes` lists and other Homes+ messages are no longer hidden when they exceed the length or line limit.
  Messages are recognised by a Homes+ insertion tag, and by the `/listhomes` text pattern used in Homes+ 1.1.1 and earlier.
- New option `allowHomesPlus` (default **true**) to turn that exception off: `/notextwall allowHomesPlus <true|false>`, also in Mod Menu + Cloth Config and in `config/notextwall.json`.


## 1.0.0 (Minecraft 26.2 / 26.3, Fabric)
- Port of No Text Wall 0.2 (1.20.4) to Minecraft 26.2 and 26.3.
- New: `/notextwall` client command (alias `/ntw`) to change every setting in-game (planned for 0.3).
- New: Mod Menu + Cloth Config settings screen (both optional).
- New: changes apply immediately, no restart. `/notextwall reload` reloads the file after editing it by hand. Missing options are added to an old config file.
- New: hover preview and a clickable [Show] on the "Long message hidden" notice. `/notextwall last` and `/notextwall show <id>` reveal hidden messages.
- New: optional line limit (`maxLines`), separate switches for player chat and system messages, and a master on/off switch.
- New: Polish translation.
- Action-bar messages are never hidden.
