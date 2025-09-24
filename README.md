# 🚀 Sequelize Runner

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

1. Go to **Settings → Plugins → ⚙ → Install plugin from disk…**
2. Select the `.zip` you built with `./gradlew buildPlugin`
3. Restart WebStorm (or IntelliJ)
4. Boom 💥 You’re ready to migrate like a boss!

---

## 🧪 Usage

- **Right-click on a migration file** → “Run this migration” or “Undo this migration”
- **Right-click on a seed file** → “Run this seed” or “Undo this seed”
- Use the **“Sequelize” tool window** to run common tasks without leaving the IDE.

It’s like having `sequelize-cli` on speed dial… without the typos.

---

## 🛠️ Development

Want to hack on this plugin? Awesome! Clone it and run:
```bash
./gradlew runIde
```
This will launch a sandbox IDE with the plugin pre-installed so you can play with it safely.

---

## 🏗️ Build a release
To build your own `.zip` ready to install:
```bash
./gradlew buildPlugin
```
The `.zip` will appear in `build/distributions/`.
Give it a loving name like `sequelize-runner-1.0.0.zip` and share it with the world 🌍

---

## 🤝 Contributing
Contributions are welcome — **especially if they save me from typing**.
Check out [CONTRIBUTING.md](CONTRIBUTING.md) for some ground rules (spoiler: be nice, write tests, and don’t break everything).

---

## 🧑‍🎤 Author
Made with ☕ (it's hot chocolate), 💻, and mild frustration by [Raphaël Sfeir](https://github.com/raphaelsfeir)

```yaml
“A great developer is just a lazy developer with good tooling.”
— Someone... probably
```
