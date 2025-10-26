# 🧭 WebStorm Sequelize Plugin — Changelog

All notable changes to this project will be documented in this file.

---

## [1.1.1] — 2025-10-26
### 🚀 Highlights
- **Verbose Sequelize Runner Terminal**  
  Improved Sequelize Runner Terminal experience by adding logs and commands into the shell.

---

---

## [1.1.0] — 2025-10-20
### 🚀 Highlights
- **Unified Terminal Experience**  
  A single shared terminal tab now handles all Sequelize operations (migrations, seeds, status, etc.).  
  Cleaner UI, no duplicate sessions, and better performance inside WebStorm.

- **ESM/CJS-Aware Migration Generation**  
  The plugin now automatically detects whether your project uses **ESM** or **CommonJS** and generates the correct migration format:
    - `export` syntax + `.js` / `.mjs` for ESM projects
    - `module.exports` syntax + `.js` / `.cjs` for CommonJS projects  
      This feature removes the need for `sequelize-cli` when generating migrations.

---

### ✨ Added
- **Native migration generation system**
    - Accessible via `Tools → Create Migration`
    - Context menu action when right-clicking inside `/migrations`
    - Uses template files stored under `/resources/templates/`
    - Automatically refreshes the IDE’s VFS so new files appear instantly

- **Enhanced Tool Window**
    - Added a native “Generate” button for migrations
    - Removed the old CLI generation toggle
    - Unified terminal execution for all commands
    - Contextual notifications for environment and migration actions

---

### 🧱 Refactored
- `core/ModuleKindDetector.kt` → detects module kind from `package.json` and migration history
- `core/MigrationScaffolder.kt` → centralizes template rendering and timestamped filenames
- `core/TerminalRunner.kt` → rewritten to use one persistent terminal session
- `ui/SequelizeToolWindowPanel.kt` → simplified UI and improved migration UX
- `actions/CreateMigrationAction.kt` and `GenerateMigrationContextAction.kt` → now use native scaffolding

---

### 🧰 Developer Experience
- Added full English KDoc documentation across all `core/` and `actions/` files
- Improved logging and error messages
- Consistent template naming and versioning
- Reduced dependency on `sequelize-cli` for all generation features

---

### 🧪 QA
- ✅ Verified migration generation in **ESM** projects (`"type": "module"`)
- ✅ Verified migration generation in **CommonJS** projects
- ✅ Verified unified terminal reuse across all actions
- ✅ Verified VFS refresh after generation
- ✅ Verified environment switching and notification display

---

## [1.0.0] — 2025-09-10
Initial release of the **WebStorm Sequelize Plugin**.  
Provides a full Sequelize workflow inside the IDE:
- Run and undo migrations
- Run and undo seeds
- View migration status
- Environment selector and CLI integration

---

### 📄 License
This project is licensed under the **MIT License**.
