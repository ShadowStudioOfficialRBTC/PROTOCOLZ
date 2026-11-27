# ProtocolZ 1.3.0

**A last-resort emergency containment plugin for Paper servers.**

ProtocolZ can freeze server ticks, ban player names, remove entities and selected blocks, disable command blocks, and suppress TNT explosions. It is deliberately destructive and is not intended for routine server administration.

## Read before installing

Back up your worlds and ban list, and test on a disposable server first. ProtocolZ does not provide an undo for removed entities or blocks.

- `/protocolz cpt` activates and persists containment, attempts to freeze ticks, bans online and known offline player names, kicks online players, and additionally bans the names listed in `unban-on-release`. It removes non-player entities and selected blocks from loaded chunks.
- CPT sets the `commandBlocksEnabled` and `tntExplodes` gamerules to `false` in every world and deletes command blocks from loaded chunks. Command blocks in chunks loaded later are also deleted. Command-block commands and TNT priming/explosions are suppressed while containment is active.
- `/protocolz cptr` unbans **all** name bans, attempts to unfreeze ticks, and sets both gamerules to `true`. Deleted command blocks, entities, and blocks are **not restored**.
- `/protocolz releaseud`, `/protocolz releseud`, `/protocolz release`, and `/protocolz relese` attempt to unfreeze ticks and pardon only names in `unban-on-release`. They deliberately leave containment active: command blocks and TNT remain disabled until `/protocolz cptr`.

The saved containment state is reapplied at startup: the plugin resets both gamerules, removes command blocks from loaded and subsequently loaded chunks, and attempts to freeze ticks again. Do not use these commands to test on a live server.

## Requirements

- Paper server matching the API version declared by the project (`26.3.build.263-stable`)
- Java 25
- Maven to build from source

## Build

From the project root:

```shell
mvn clean package
```

The plugin JAR is created at `target/ProtocolZ-1.3.0.jar`. Paper API dependencies are provided by the server and are not bundled in the plugin.

## Install

1. Copy `target/ProtocolZ-1.3.0.jar` into the server's `plugins` directory.
2. Start or restart the server.
3. Review `plugins/ProtocolZ/config.yml` before using any command.

## Commands

All commands require `protocolz.use`, which defaults to server operators.

| Command | Effect |
| --- | --- |
| `/protocolz cpt` | Activate emergency containment. |
| `/protocolz cptr` | Unban all names, unfreeze ticks, and re-enable command blocks and TNT. |
| `/protocolz releaseud` or `/protocolz releseud` | Unfreeze ticks and unban configured names; containment remains active. |
| `/protocolz release` or `/protocolz relese` | Backward-compatible aliases for the temporary release. |
| `/protocolz reload` | Reload `config.yml`; the plugin-managed containment state is unchanged. |

## Configuration

```yaml
containment-active: false
unban-on-release:
  - DEVLOPER__
```

`containment-active` is managed by the plugin to preserve containment across restarts. Add or remove names in `unban-on-release` to control which names CPT additionally bans and which the temporary release commands pardon. CPT still bans all online and known offline names; use `/protocolz cptr` to clear every name ban.

## License

MIT. See [LICENSE](LICENSE).
