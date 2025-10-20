# 🚀 Sequelize Runner

[![Version](https://img.shields.io/github/v/release/raphaelsfeir/Webstorm-Sequelize-Plugin?color=brightgreen&label=plugin)](https://github.com/raphaelsfeir/Webstorm-Sequelize-Plugin/releases)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/WebStorm%20%2F%20IntelliJ-2024.2+-blue)](https://plugins.jetbrains.com/)

A WebStorm / IntelliJ IDEA plugin to make your life with [Sequelize CLI](https://sequelize.org/) 100000% easier.  
Because typing `npx sequelize-cli db:migrate` 50 times a day is not what we signed up for.

---

## ✨ Features

- 🪄 Run or undo migrations directly from the **right-click context menu**
- ⚙️ Quickly generate new migrations without touching the terminal
- 🌱 Execute or undo seed files with a single click
- 🧪 Support for multiple environments (`development`, `test`, `production`)
- 📟 A dedicated tool window with all the buttons you’re too lazy to write CLI commands for

---

## 🧑‍💻 Installation

### 😅 I'm not a geek!
No worries! Just [download the latest release](https://github.com/raphaelsfeir/Webstorm-Sequelize-Plugin/releases/latest/download/webstorm-sequelize-plugin-1.1.0.zip) — that's literally all you need to do.

---

### 🛠️ I want to build it myself!
1. Clone the repo:
   ```bash
     git clone https://github.com/raphaelsfeir/Webstorm-Sequelize-Plugin.git
     cd Webstorm-Sequelize-Plugin```

2. Build the plugin:
   ```bash
     ./gradlew buildPlugin```

3. The `.zip` will appear in `build/distributions/`

---

### 🚀 Install it

1. Go to **Settings → Plugins → ⚙ → Install plugin from disk…**
2. Select the `.zip` you just downloaded or built
3. Restart WebStorm (or IntelliJ)
4. Boom 💥 You’re ready to migrate like a boss!

---

## 🧪 Usage

* **Right-click on a migration file** → “Run this migration” or “Undo this migration”
* **Right-click on a seed file** → “Run this seed” or “Undo this seed”
* Use the **“Sequelize” tool window** to run common tasks without leaving the IDE.

It’s like having `sequelize-cli` on speed dial… without the typos.

---

## 🏗️ Development

Want to hack on this plugin? Awesome! Clone it and run:

```bash
./gradlew runIde
```

This will launch a sandbox IDE with the plugin pre-installed so you can play with it safely.

---

## 📦 Build a release

To package your own `.zip`:

```bash
./gradlew buildPlugin
```

The `.zip` will appear in `build/distributions/`.
Give it a cool name like `sequelize-runner-1.1.0.zip` and share it with the world 🌍

---

## 🤝 Contributing

Contributions are welcome — **especially if they save me from typing**.
Check out [CONTRIBUTING.md](CONTRIBUTING.md) for how to join the fun.
(⚠️ Spoiler: be nice, write tests, and don’t break everything.)

---

## 🧑‍🎤 Author

Made with ☕ (okay, hot chocolate), 💻, and mild frustration by [Raphaël Sfeir](https://github.com/raphaelsfeir)

```yaml
“A great developer is just a lazy developer with good tooling.”
— Someone... probably```
