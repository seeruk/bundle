# Seer's Core

Shared core library mod for my mods.

## Features

- **Command Registration Helpers**: Provides utilities for registering commands along with a 
  namespaced alias, ensuring commands remain accessible even if another mod takes the original name.
- **Custom Actions**: Lets mods handle Vanilla's custom click actions (e.g. a dialog's buttons),
  along with the player who sent them, which Vanilla doesn't provide.
- **Config**: Utilities for loading and parsing config, with opinionated behaviour around filenames,
  and merging defaults with overrides, etc.
- **Scoreboard Stat Helpers**: Utilities for registering and interacting with scoreboard objectives.
  Keeps names consistent, and mod code compact.
- **Tab List Names**: Shared functionality to allow several mods change how players are shown in the
  tab list (e.g. adding a nickname, or an AFK marker), and where they're shown, without overriding 
  each other.

This mod must also be installed on servers using any mods depending on it.

## Usage

You'll probably need to dive into the code for this one, or look at my other mods for examples of 
how this library is used. Everything in here is used somewhere, and generally just serves the 
purpose of making my other mods easier to write and maintain.

There are no promises of backwards compatibility with this library, so if you're using it in your 
own mods, you should be aware that it may change and break your code at any time...
