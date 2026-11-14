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

Planned:

- **Text Conversion**: Conversion of Minecraft text into other formats, such as Markdown, ready for
  use in other contexts (e.g. Discord).
- **Component Helpers**: Utilities for working with `Component` trees, such as merging two 
  separately formatted trees into one, or wrapping text, etc.

[1]: https://github.com/Patbox/TextPlaceholderAPI
