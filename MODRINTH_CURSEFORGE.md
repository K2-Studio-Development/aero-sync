# AeroSync

[![Discord](https://img.shields.io/badge/DISCORD-5865F2?style=for-the-badge&logo=discord&logoColor=white)](https://discord.gg/jkjdnQMbFq)
[![Telegram](https://img.shields.io/badge/TELEGRAM-26A5E4?style=for-the-badge&logo=telegram&logoColor=white)](https://t.me/K2Studio_Dev)
[![GitHub](https://img.shields.io/badge/GITHUB-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/K2-Studio-Development)

## English

**AeroSync** lets you send selected Minecraft data to a friend through one P2P address—without a dedicated server, a public IP address, manual port forwarding, or direct IP entry.

Select a world, configs, shader packs, resource packs, screenshots, settings, or mods. AeroSync packages the selected data and creates a private `AEROSYNC:...` address for the receiver. Mods are resolved through the Modrinth API instead of sending JAR files over P2P.

### Features

- Transfer a selected singleplayer world
- Transfer `config/`, `shaderpacks/`, `resourcepacks/`, and `screenshots/`
- Transfer `options.txt` and `optionsof.txt`
- Recreate matching mod files through the Modrinth API
- Connect through one copyable `AEROSYNC:...` address
- Use NAT traversal without opening router ports
- See packaging, current-file, transfer, and extraction progress
- Search and select a world through a vanilla-style list
- Automatically back up an existing world before overwrite
- Choose every category manually; nothing is selected by default

### Installation


| Loader | Download file | Extra dependency |
|---|---|---|
| Fabric | `aerosync-fabric-mc1.21.1-26.2-v0.0.9.jar` | Fabric API matching Minecraft |
| NeoForge | `aerosync-neoforge-mc1.21.1-26.2-v0.0.9.jar` | None |



1. Install Fabric Loader with Fabric API, or install NeoForge.
2. Download the matching AeroSync JAR from the table and put it in `mods/`.
3. Install the same AeroSync release on both computers.

### Usage

Open **AeroSync P2P** from the title screen or pause menu. The sender chooses the required categories and shares the generated address after packaging. The receiver opens **Receive**, pastes the complete address, and waits for extraction to finish.

Keep the sender screen open until the transfer is complete. Restart Minecraft before using received mods or configuration files.

### Requirements

- Minecraft Java Edition `1.21.1`–`1.21.11`, `26.1`, or `26.2`
- Fabric Loader `0.19.3` or newer with Fabric API, or NeoForge
- Java `21` for Minecraft `1.21.x`; Java `25` for Minecraft `26.x`
- AeroSync on both computers
- Internet access for Iroh connectivity and Modrinth downloads

### Mod synchronization

AeroSync does not send mod JAR files through P2P. It creates a manifest from the sender's mods and downloads matching files through Modrinth on the receiver's computer. Mods unavailable on Modrinth or not matched by hash must be installed manually.

### Known limitations

- Separate universal JARs for Fabric and NeoForge
- Russian-only in-game interface in the current release
- Client-side transfers only; dedicated servers are unsupported
- One receiver per transfer screen
- This is file transfer, not multiplayer hosting or continuous cloud synchronization
- Non-world files may be overwritten without an automatic backup

**AeroSync `0.0.9` is a pre-release build for early testing. Back up important data before using it.**

Created by **StandoffKitty75** and **K2 Studio**.

---

## Русский

**AeroSync** позволяет передавать другу выбранные данные Minecraft через единый P2P-адрес — без выделенного сервера, белого IP, ручного проброса портов и ввода IP-адреса.

Можно выбрать мир, конфиги, шейдерпаки, ресурспаки, скриншоты, настройки или моды. AeroSync упакует выбранные данные и создаст приватный адрес `AEROSYNC:...` для получателя. Моды загружаются через Modrinth API вместо передачи JAR-файлов по P2P.

### Возможности

- Передача выбранного одиночного мира
- Передача `config/`, `shaderpacks/`, `resourcepacks/` и `screenshots/`
- Передача `options.txt` и `optionsof.txt`
- Загрузка совпадающих модов через Modrinth API
- Подключение по одному копируемому адресу `AEROSYNC:...`
- Соединение через NAT без открытия портов на роутере
- Прогресс упаковки, текущего файла, передачи и распаковки
- Поиск и выбор мира в списке, похожем на стандартное меню Minecraft
- Автоматическая резервная копия существующего мира перед перезаписью
- Полностью ручной выбор категорий; по умолчанию ничего не отмечено

### Установка


| Загрузчик | Какой файл скачивать | Дополнительная зависимость |
|---|---|---|
| Fabric | `aerosync-fabric-mc1.21.1-26.2-v0.0.9.jar` | Fabric API для своей версии Minecraft |
| NeoForge | `aerosync-neoforge-mc1.21.1-26.2-v0.0.9.jar` | Не требуется |



1. Установите Fabric Loader с Fabric API либо NeoForge.
2. Скачайте подходящий JAR AeroSync из таблицы и поместите его в `mods/`.
3. Установите одинаковый релиз AeroSync на оба компьютера.

### Использование

Откройте **AeroSync P2P** в главном меню или меню паузы. Отправитель выбирает нужные категории и после упаковки передаёт созданный адрес. Получатель нажимает **Принять**, вставляет полный адрес и ждёт завершения распаковки.

Не закрывайте экран отправителя до завершения передачи. Перед использованием полученных модов или конфигурационных файлов перезапустите Minecraft.

### Требования

- Minecraft Java Edition `1.21.1`–`1.21.11`, `26.1` или `26.2`
- Fabric Loader `0.19.3` или новее с Fabric API либо NeoForge
- Java `21` для Minecraft `1.21.x`; Java `25` для Minecraft `26.x`
- AeroSync на обоих компьютерах
- Интернет для соединения через Iroh и загрузки модов с Modrinth

### Синхронизация модов

AeroSync не отправляет JAR-файлы модов по P2P. Мод создаёт список модов отправителя и загружает совпадающие файлы через Modrinth на компьютере получателя. Моды, которых нет на Modrinth или которые не удалось найти по хешу, нужно установить вручную.

### Известные ограничения

- Отдельные универсальные JAR для Fabric и NeoForge
- Интерфейс текущей версии только на русском языке
- Только клиентская передача; выделенные серверы не поддерживаются
- Один получатель на один экран передачи
- Это передача файлов, а не хостинг мультиплеера или постоянная облачная синхронизация
- Файлы вне мира могут быть перезаписаны без автоматической резервной копии

**AeroSync `0.0.9` — предварительная версия для раннего тестирования. Перед использованием сохраните резервную копию важных данных.**

Авторы: **StandoffKitty75** и **K2 Studio**.
