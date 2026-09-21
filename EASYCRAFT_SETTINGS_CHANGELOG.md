# EasyCraft settings-card support

This source update adds an optional `settings` array to a mod's `Info.json`.
The manager shows those settings only when the user expands that mod's card.

Supported controls are `SWITCH`, `INTEGER`, and `CHOICE`. Values are saved to:

`mods/<loader-id>/private/<mod-pkg-id>/config.json`

The path is the mod private directory, allowing a native KernelLoader mod to read
the same file without Android storage permissions. Existing mods are unchanged:
an absent `settings` array renders no settings section.
