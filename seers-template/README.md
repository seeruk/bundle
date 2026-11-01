# Seer's Template

A template mod, based on the [Fabric Example Mod][1], adapted for use in this monorepo. Serves as a
base that can be copied to create new mods quickly.

## Usage

This template mod can be copied to create a new mod. A helper task is defined in the shared
`build-logic` module. Simply run:

```bash
./gradlew scaffoldMod --name="New Mod Name"
```

The name provided is the display name, this allows the scaffold task to generate accurate names. For
example, if a particularly stylized name is provided (e.g. "PropSwap"), the task will properly use
"PropSwap" as one word instead of splitting it.

[1]: https://github.com/FabricMC/fabric-example-mod
