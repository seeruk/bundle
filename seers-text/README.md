# Seer's Text

Shared text formatting library mod for my mods.

## Features

This mod makes heavy use of the excellent [Text Placeholder API][1] by Patbox, which is a dependency
of this mod, and drastically simplifies the code here!

- **Predefined Text Formatting Pipelines**: Provides various ready-to-use text formatting pipelines
  for other mods to consume, for various use-cases; for example, player-safe formatting, extended
  functionality for ops or very trusted players, or formatting that can be used by mods or in
  configuration.
- **Text Templates**: Trusted text with named placeholders (e.g. `${player}`), parsed once and
  filled in each time it's used. Useful for configurable messages.
- **Component Helpers**: Utilities for working with `Component` trees, such as splitting styled
  text into lines, or wrapping it, while keeping its styling.
- **Text Colours**: Shared colours for text the mods put into messages that can be hovered over
  (light purple) or clicked (aqua), so they mean the same thing in every mod.

Planned:

- **Text Conversion**: Conversion of Minecraft text into other formats, such as Markdown, ready for
  use in other contexts (e.g. Discord).
- **More Component Helpers**: e.g. merging two separately formatted `Component` trees into one.

[1]: https://github.com/Patbox/TextPlaceholderAPI
