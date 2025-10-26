# 🤝 Contributing to Sequelize Runner

So… you want to help make Sequelize Runner even better?  
That’s awesome. Here’s how to do it **without breaking everything** 😅

---

## 🧰 Requirements

* JDK 17+ (because Java 8 is so 2015)
* Gradle (comes with the wrapper)
* WebStorm or IntelliJ IDEA Community Edition
* Basic knowledge of Kotlin (don’t worry, it’s just Java with fewer semicolons)

---

## 🛠️ Getting Started

1. Fork this repo  
2. Clone your fork:
```bash
   git clone https://github.com/raphaelsfeir/Webstorm-Sequelize-Plugin
   cd webstorm-sequelize-plugin
````

3. Open it in IntelliJ/WebStorm
4. Run the sandbox IDE:

```bash
./gradlew runIde
```

This launches a “test IDE” where you can mess around without hurting your main setup. Like a virtual machine… but for your bad code.

---

## 🧪 Testing Your Changes

We use JUnit 5. You can run tests with:

```bash
./gradlew test
```

Make sure everything is green ✅ before opening a PR.
If something’s red, don’t panic — take a coffee ☕, then fix it.

---

## 🧙‍♂️ Commit Guidelines

Try to write commits that future-you will understand. Examples:

✅ `feat: add context menu for seeder undo`\
✅ `fix: prevent terminal crash when migration name is empty`\
❌ `update stuff lol`

---

## 🌿 Branch Workflow (a.k.a. “Don’t push on `main`, bro”)

We use a classic two-branch flow to keep the plugin stable:

* **`main`** – the sacred branch. Only stable, release-ready code lands here. You **can’t push directly** to it (and if you try... Just don't try it).
* **`dev`** – the playground for development. This is where all the fun (and occasional chaos) happens.

👉 Here’s how we roll:

1. Always start from `dev`:

   ```bash
   git checkout dev
   git pull
   git checkout -b feature/your-amazing-feature
   ```
2. Do your magic ✨, commit, and push:

   ```bash
   git push origin feature/your-amazing-feature
   ```
3. Open a **PR from your feature branch → `dev`**.
4. When `dev` is stable and tested, we’ll open a **PR from `dev` → `main`** for the next release.

This keeps `main` shiny and production-ready, while `dev` gets to be the chaotic sandbox it was always meant to be 🏖️

---

## 🚀 Submitting a Pull Request

1. Push your branch to your fork.
2. Open a Pull Request on the main repo.
3. Add a short description explaining **what** and **why**.
4. Cross your fingers and wait for a review 🤞

---

## 🧠 Tips

* **Keep it modular**: if your code looks like spaghetti, split it into smaller classes.
* **Use the sandbox**: breaking the real IDE is embarrassing. Breaking the sandbox is just Tuesday.
* **Have fun**: if you’re not having fun, you’re probably writing Java.

---

## 🥇 Final Words

**Thanks for contributing!** Every little feature, typo fix, or doc update makes this plugin better.
And who knows — maybe your PR will save someone 3 seconds of typing one day.
That’s basically a lifetime in dev time ⏱️
