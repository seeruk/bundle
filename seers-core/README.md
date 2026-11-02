# Seer's Core

Shared core library mod for my mods.

## Features

- **Command Registration Helpers**: Provides utilities for registering commands along with a 
  namespaced alias, ensuring commands remain accessible even if another mod takes the original name.
- **Scoreboard Stat Helpers**: Utilities for registering and interacting with scoreboard objectives.
  Keeps names consistent, and mod code compact.

This mod must also be installed on servers using any mods depending on it.

## Usage

You'll probably need to dive into the code for this one, or look at my other mods for examples of 
how this library is used. Everything in here is used somewhere, and generally just serves the 
purpose of making my other mods easier to write and maintain.

There are no promises of backwards compatibility with this library, so if you're using it in your 
own mods, you should be aware that it may change and break your code at any time...
