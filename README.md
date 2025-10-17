# VulcanCrates

A modern, feature-rich crates plugin for Minecraft servers. VulcanCrates is a configurable crate system with holographic displays, a clean preview GUI, and persistent player data.

## What is this?

VulcanCrates lets you set up physical crates in your Minecraft world that players can open using keys. Think of it like a loot box system, but for your server. Each crate can contain different prizes with customizable chances, and players can preview what's inside before opening.

## Features

- **Physical Crate Placement** - Place crates anywhere in your world as actual chest blocks
- **Hologram Support** - Floating text above each crate using DecentHolograms
- **Preview GUI** - Let players see what's inside a crate before they open it
- **Weighted Prize System** - Control how rare each prize is with customizable chances
- **Key Management** - Give keys to individual players or broadcast them to everyone online
- **Persistent Data** - Player key counts are saved automatically, even through server restarts
- **PlaceholderAPI Integration** - Use crate data in other plugins
- **Configurable Everything** - Messages, GUI layouts, hologram text - it's all customizable

## Requirements

This plugin needs a few things to work:

- **Spigot/Paper** 1.8.8 (WineSpigot compatible)
- **VulcanLib** (included in libs folder)
- **ProtocolLib** (included in libs folder)
- **DecentHolograms** (for the floating text above crates)
- **PlaceholderAPI** (optional, for placeholder support)

## Commands

All commands start with `/crate` (or `/crates` if you prefer):

| Command | Description | Permission |
|---------|-------------|------------|
| `/crate give <player> <crate> [amount]` | Give keys to a specific player | - |
| `/crate giveall <crate> [amount]` | Give keys to all online players | - |
| `/crate place <crate>` | Place a crate chest in the world | - |
| `/crate remove <crate>` | Remove a placed crate | - |
| `/crate preview <crate>` | Preview the prizes in a crate | - |
| `/crate list` | List all available crates | - |
| `/crate reload` | Reload the plugin configuration | - |

## Installation

This plugin is free through the Vulcan Loader found in the client panel [Here](https://vulcandev.net/).

## Setting Up Your First Crate

Here's a quick walkthrough:

1. **Define your crate** in the config file with its prizes and chances
2. **Place the crate** using `/crate place <crate-name>`
3. **Give yourself a key** with `/crate give <your-name> <crate-name>`
4. **Right-click the crate** to open it!

The hologram will appear automatically above the crate, and all locations are saved so they persist through restarts.

## How It Works

When you place a crate, it's stored as a physical chest block in the world with a hologram floating above it. The plugin tracks which chest belongs to which crate type. When a player interacts with it, the plugin checks if they have a key for that specific crate. If they do, it selects a random prize based on the weighted chances you configured and gives it to them.
Player key counts are tracked separately and saved to JSON files, so if a player has 5 keys and the server restarts, they'll still have those 5 keys when they come back.

The compiled JAR will be in the `target` folder. Make sure you have the required libraries in the `libs` folder before building.

**Dependencies:**
- [DecentHolograms](https://github.com/DecentSoftware-eu/DecentHolograms) - For hologram displays
- [ProtocolLib](https://github.com/dmulloy2/ProtocolLib) - Packet handling
- [PlaceholderAPI](https://github.com/PlaceholderAPI/PlaceholderAPI) - Placeholder support
- VulcanLib - Custom utility library

## Need Help?
If you have questions or need help, just message xanthard001 on Discord. I will be happy to help.