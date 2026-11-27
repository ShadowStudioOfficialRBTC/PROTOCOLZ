# ProtocolZ

**A last-resort emergency containment plugin for Paper servers.**

ProtocolZ provides commands to freeze server ticks, ban player names, remove entities, and clear selected blocks from loaded chunks. It is intentionally powerful and destructive; it is not a routine moderation or server-management plugin.

## Important warning

Use ProtocolZ only when you understand and accept the consequences. Take a verified backup first and test on a disposable copy of your world.

- `/protocolz cpt` attempts to freeze ticks, permanently bans every player name Bukkit reports (including offline players), kicks online players, and additionally bans the hard-coded name `DEVLOPER__`. It removes **all non-player entities** from every world and replaces selected blocks with air in **currently loaded chunks**.
- These world and entity changes have no undo or restore command. Lost entities and block contents are not recovered by `/protocolz release`.
- `/protocolz cptr` removes **all name bans**, including bans that existed before ProtocolZ was installed.
- `/protocolz release` attempts to unfreeze ticks and unbans only the names in `unban-on-release`. It does not undo the containment changes or unban everyone.

Do not run these commands to test on a live server.

## Requirements

- Paper server matching the Paper API version declared by the project (`26.3.build.263-stable`)
- Java 25
- Maven to build from source

## Build

From the project root, run:

```shell
mvn clean package
```

The plugin JAR will be created at:

```text
target/ProtocolZ-1.2.0.jar
```

Paper API dependencies are provided by the server and are not bundled in the plugin JAR.

## Install

1. Build the JAR, or obtain a trusted build of it.
2. Copy `target/ProtocolZ-1.2.0.jar` into your server's `plugins` directory.
3. Start or restart the server.
4. Review `plugins/ProtocolZ/config.yml` before using any command.

## Commands

All commands require `protocolz.use`, which defaults to server operators.

| Command | Effect |
| --- | --- |
| `/protocolz cpt` | Runs the destructive containment actions described above. |
| `/protocolz cptr` | Unbans every name on the server. |
| `/protocolz relese` or `/protocolz release` | Attempts to unfreeze ticks and unbans configured names. |
| `/protocolz reload` | Reloads `config.yml`. |

## Configuration

`unban-on-release` controls which names `/protocolz release` pardons. The shipped default is:

```yaml
unban-on-release:
  - DEVLOPER__
```

Edit this list to match your recovery plan. The `DEVLOPER__` name is also hard-coded into the `cpt` action, so changing this setting does not change that additional ban.

## License

This project is released under the MIT License. See [LICENSE](LICENSE).
