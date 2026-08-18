<p align="center">
  <img src="docs/aerosync-logo.png" alt="AeroSync" width="320">
</p>

# AeroSync

[English](#aerosync) · [Русский](#aerosync-на-русском)

**AeroSync** is a client-side Fabric and NeoForge mod for transferring selected Minecraft data between two players through a single Iroh P2P address. It does not require a dedicated server, a public IP address, manual port forwarding, or direct IP entry.

> **Pre-release:** `0.0.9` is intended for early testing. Back up important Minecraft data before using it.

## Scope

AeroSync transfers selected files between Minecraft installations. It is not a multiplayer tunnel, server host, cloud storage service, or continuous synchronization tool.

Supported categories:

| Category | Source/target |
|---|---|
| World | Selected directory under `saves/` |
| Configs | `config/` |
| Shader packs | `shaderpacks/` |
| Mods | Manifest generated from `mods/`; matching files downloaded from Modrinth |
| Resource packs | `resourcepacks/` |
| Screenshots | `screenshots/` |
| Game settings | `options.txt` and `optionsof.txt` |

Nothing is selected by default. World synchronization requires explicitly enabling the category and choosing a save from the built-in searchable world list.

## Compatibility

| Component | Supported version |
|---|---|
| Minecraft Java Edition | `1.21.1`–`1.21.11`, `26.1`, `26.2` |
| Mod loader | Fabric Loader `0.19.3` or newer; NeoForge matching the selected Minecraft version |
| Required mod dependency | Fabric API on Fabric; none on NeoForge |
| Java | `21` for Minecraft `1.21.x`; `25` for Minecraft `26.x` |
| Environment | Client |

AeroSync must be installed on both computers. Both players should use matching Minecraft, mod loader, and AeroSync versions. Fabric installations also require matching Fabric API builds.

Each loader has one universal JAR containing internal compatibility layers. Download the Fabric or NeoForge file for your loader; the selected file supports every Minecraft version listed above.


### Release files

| Loader | Download file | Extra dependency |
|---|---|---|
| Fabric | `aerosync-fabric-mc1.21.1-26.2-v0.0.9.jar` | Fabric API matching Minecraft |
| NeoForge | `aerosync-neoforge-mc1.21.1-26.2-v0.0.9.jar` | None |



## Main features

- One copyable `AEROSYNC:...` connection address
- NAT traversal through Iroh without manual router configuration
- Optional relay connectivity when a direct path is unavailable
- Explicit category selection before packaging
- Searchable vanilla-style world selector
- Packaging progress with the current file name
- Transfer and extraction progress
- Receiver acknowledgement after extraction completes
- Automatic backup before overwriting an existing world directory
- Zip Slip protection during package extraction
- Mod installation through the Modrinth API with SHA-1 verification
- Cancellation and connection timeout handling

## Architecture

AeroSync is divided into four main layers:

- **Client UI:** screens for category selection, world selection, sending, receiving, progress, and errors. Fabric uses mixins and NeoForge uses screen events to add menu buttons.
- **Packaging:** category archivers create a temporary ZIP containing only explicitly selected data. Existing target worlds are copied to timestamped backup directories before overwrite.
- **Transport:** the bundled Iroh client provides endpoint discovery, NAT traversal, encrypted connectivity, and relay fallback. AeroSync uses the application protocol identifier `aerosync/package/3` and a complete address containing a session code plus an Iroh endpoint ticket.
- **Mod resolution:** mod JAR files are excluded from the P2P archive. AeroSync sends a manifest containing filenames, sizes, and SHA-1 hashes; the receiver resolves matching files through the Modrinth API and verifies each download.

The sender serves one package to one receiver per transfer screen. The address becomes copyable only after both packaging and endpoint initialization have completed.

## Runtime dependencies

| Dependency | Version | Packaging |
|---|---:|---|
| Fabric API | Version matching the target Minecraft release | Required on Fabric only |
| Iroh Java bindings | `1.1.0` | Included in AeroSync JAR |
| Kotlin standard library | `2.2.21` | Included in AeroSync JAR |
| Kotlin coroutines | `1.9.0` | Included in AeroSync JAR |
| JNA | Loader-provided on NeoForge; `5.15.0` on Fabric | Included only in the Fabric JAR |

## Installation

1. Install a supported Minecraft version and either Fabric Loader with Fabric API, or NeoForge.
2. Download the AeroSync JAR matching that loader from the table above.
3. Place the JAR in the instance's `mods/` directory.
4. Install the same AeroSync release on the sender's and receiver's computers.

## Usage

Open **AeroSync P2P** from the title screen or pause menu. The sender selects the required categories and optionally chooses a world, then shares the generated private address after packaging finishes. The receiver opens **Receive**, pastes the complete address, and waits for extraction to finish.

Restart Minecraft before using newly installed mods or configuration files. Keep the sender screen open until the receiver confirms completion.

## Modrinth integration

When **Mods** is selected, AeroSync scans regular JAR files in `mods/`, excludes AeroSync itself, and creates a manifest instead of copying the JAR files into the transfer package.

The receiver queries Modrinth by SHA-1 hash. Existing matching files are skipped; downloaded files are verified before being moved into `mods/`. Files that are unavailable on Modrinth or cannot be matched are listed in the Minecraft log and must be installed manually.

## Data safety and privacy

- Existing worlds with matching directory names receive timestamped backups in `saves/` before overwrite.
- Other selected files can be replaced without an automatic backup; keep a separate copy of important data.
- Archive entries are normalized and checked to prevent extraction outside the Minecraft game directory.
- The generated connection address grants access to the active transfer. Share it privately only with the intended receiver.
- AeroSync does not provide persistent remote storage. Transfer traffic may use Iroh relay infrastructure when a direct connection cannot be established.
- Selecting **Mods** contacts the Modrinth API and CDN from the receiver's computer.

## Known limitations

- Only the exact Minecraft versions listed in the compatibility table are supported.
- The in-game interface is currently Russian-only.
- Dedicated servers are not supported.
- One sender screen handles one receiver and one package.
- AeroSync does not merge worlds or configuration files; received files replace matching targets.
- Only mods available through Modrinth and matched by hash are installed automatically.
- Restarting Minecraft is recommended after receiving mods, configs, resource packs, shader packs, or settings.
- This project is still in early testing and may contain data-transfer or compatibility bugs.

## Building from source

Requirements:

- JDK `21` and JDK `25`
- Internet access for Gradle dependency resolution

Build every supported release on Windows:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\build-all.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\build-neoforge.ps1
```

The release scripts require JDK `21` and JDK `25` at the paths configured near their top. They compile the internal compatibility layers and write one Fabric JAR, one NeoForge JAR, and `SHA256SUMS.txt` to `releases/0.0.9/`.

Build the default `1.21.11` development target only:

```powershell
.\gradlew.bat clean build
```

The single-target JAR and sources JAR are written to `build/libs/`.

## Project structure

```text
src/main/java/com/aerosync/
├── archiver/    Package creation, extraction, backups, categories
├── gui/         Sender, receiver, setup, and world-selection screens
├── mixin/       Title-screen and pause-menu integration
├── modrinth/    Manifest generation and verified downloads
├── network/     Java sender/receiver API
└── util/        Client notifications

src/main/kotlin/com/aerosync/network/
└── AeroIrohTransport.kt    Iroh endpoint and transfer protocol

src/versions/
├── common121/              Shared Minecraft 1.21.x client UI
├── legacy121/              Minecraft 1.21.1–1.21.8 adapter
└── modern121/              Minecraft 1.21.9–1.21.11 adapter

versions/26.2/
├── src/main/               Shared Minecraft 26.x client UI
└── src/versions/           Minecraft 26.1 and 26.2 bridges

versions/neoforge/          NeoForge loader, UI adapters, and exact-version builds
versions/neoforge-smoke/    Isolated universal-JAR launch checks
```

## Logs

Transfer status, missing Modrinth files, backups, and failures are written to the standard Minecraft log:

```text
.minecraft/logs/latest.log
```

Search for `AeroSync` when reporting a problem. Include the relevant log section, Minecraft version, loader version, AeroSync version, selected categories, and Fabric API version when using Fabric. Remove connection addresses or other private information before publishing logs.

## Authors

- **StandoffKitty75**
- **K2 Studio**

---

# AeroSync на русском

**AeroSync** — клиентский мод для Fabric и NeoForge, предназначенный для передачи выбранных данных Minecraft между двумя игроками через единый P2P-адрес Iroh. Для работы не нужны выделенный сервер, белый IP-адрес, ручной проброс портов или ввод IP напрямую.

> **Предварительная версия:** `0.0.9` предназначена для раннего тестирования. Перед использованием создайте резервную копию важных данных Minecraft.

## Назначение

AeroSync передаёт выбранные файлы между установками Minecraft. Это не туннель для мультиплеера, не хостинг сервера, не облачное хранилище и не средство постоянной синхронизации.

Поддерживаемые категории:

| Категория | Источник и место установки |
|---|---|
| Мир | Выбранная папка внутри `saves/` |
| Конфиги | `config/` |
| Шейдерпаки | `shaderpacks/` |
| Моды | Список из `mods/`; совпадающие файлы загружаются с Modrinth |
| Ресурспаки | `resourcepacks/` |
| Скриншоты | `screenshots/` |
| Настройки игры | `options.txt` и `optionsof.txt` |

По умолчанию ничего не выбрано. Для передачи мира нужно отдельно включить эту категорию и выбрать сохранение во встроенном списке с поиском.

## Совместимость

| Компонент | Поддерживаемая версия |
|---|---|
| Minecraft Java Edition | `1.21.1`–`1.21.11`, `26.1`, `26.2` |
| Загрузчик модов | Fabric Loader `0.19.3` или новее; NeoForge для выбранной версии Minecraft |
| Обязательная зависимость | Fabric API для Fabric; для NeoForge дополнительных модов не требуется |
| Java | `21` для Minecraft `1.21.x`; `25` для Minecraft `26.x` |
| Среда | Клиент |

AeroSync должен быть установлен на обоих компьютерах. У обоих игроков должны совпадать версии Minecraft, загрузчика и AeroSync. Для Fabric также должна совпадать версия Fabric API.

Для каждого загрузчика выпускается один универсальный JAR со встроенными слоями совместимости. Скачайте Fabric- или NeoForge-файл; выбранный JAR поддерживает все перечисленные выше версии Minecraft.


### Файлы релиза

| Загрузчик | Какой файл скачивать | Дополнительная зависимость |
|---|---|---|
| Fabric | `aerosync-fabric-mc1.21.1-26.2-v0.0.9.jar` | Fabric API для своей версии Minecraft |
| NeoForge | `aerosync-neoforge-mc1.21.1-26.2-v0.0.9.jar` | Не требуется |



## Основные возможности

- Единый копируемый адрес `AEROSYNC:...`
- Обход NAT через Iroh без ручной настройки роутера
- Возможность соединения через ретранслятор, если прямой маршрут недоступен
- Явный выбор категорий перед упаковкой
- Список миров с поиском в стиле стандартного интерфейса Minecraft
- Прогресс упаковки с названием текущего файла
- Прогресс передачи и распаковки
- Подтверждение получателя после окончания распаковки
- Автоматическая резервная копия перед перезаписью существующего мира
- Защита от Zip Slip при распаковке
- Загрузка модов через Modrinth API с проверкой SHA-1
- Отмена операции и обработка тайм-аутов соединения

## Архитектура

AeroSync разделён на четыре основных слоя:

- **Клиентский интерфейс:** экраны выбора категорий и мира, отправки, получения, прогресса и ошибок. Миксины добавляют кнопки AeroSync в главное меню и меню паузы.
- **Упаковка:** архиваторы категорий создают временный ZIP только с явно выбранными данными. Перед перезаписью существующего мира его папка копируется в резервную директорию с меткой времени.
- **Транспорт:** встроенный клиент Iroh обеспечивает обнаружение конечных точек, обход NAT, шифрованное соединение и резервный маршрут через ретранслятор. AeroSync использует идентификатор протокола `aerosync/package/3` и полный адрес с кодом сессии и билетом конечной точки Iroh.
- **Получение модов:** JAR-файлы модов не добавляются в P2P-архив. Вместо них передаётся список имён, размеров и SHA-1; получатель ищет совпадающие файлы через Modrinth API и проверяет каждую загрузку.

Один экран отправки обслуживает один пакет и одного получателя. Кнопка копирования адреса становится доступна только после завершения упаковки и инициализации P2P-точки.

## Зависимости времени выполнения

| Зависимость | Версия | Способ поставки |
|---|---:|---|
| Fabric API | Версия для целевого релиза Minecraft | Обязателен только для Fabric |
| Java-биндинги Iroh | `1.1.0` | Включены в JAR AeroSync |
| Kotlin standard library | `2.2.21` | Включена в JAR AeroSync |
| Kotlin coroutines | `1.9.0` | Включены в JAR AeroSync |
| JNA | Системная в NeoForge; `5.15.0` в Fabric | Включена только в Fabric JAR |

## Установка

1. Установите поддерживаемую версию Minecraft и либо Fabric Loader с Fabric API, либо NeoForge.
2. Скачайте JAR AeroSync для выбранного загрузчика из таблицы выше.
3. Поместите JAR в папку `mods/` нужной сборки Minecraft.
4. Установите одинаковый релиз AeroSync на компьютеры отправителя и получателя.

## Использование

Откройте **AeroSync P2P** в главном меню или меню паузы. Отправитель выбирает нужные категории и при необходимости мир, после чего передаёт созданный приватный адрес. Получатель открывает раздел **Принять**, вставляет полный адрес и ждёт окончания распаковки.

Перед использованием новых модов или конфигурационных файлов перезапустите Minecraft. Не закрывайте экран отправителя до подтверждения завершения передачи.

## Интеграция с Modrinth

При выборе категории **Моды** AeroSync сканирует обычные JAR-файлы в `mods/`, исключает сам AeroSync и создаёт список вместо добавления JAR-файлов в передаваемый пакет.

Получатель выполняет поиск на Modrinth по SHA-1. Уже существующие совпадающие файлы пропускаются, а загруженные проверяются перед перемещением в `mods/`. Файлы, которых нет на Modrinth или которые не удалось найти, записываются в лог Minecraft и устанавливаются вручную.

## Безопасность данных и приватность

- Перед перезаписью мира с совпадающим именем папки в `saves/` создаётся резервная копия с меткой времени.
- Остальные выбранные файлы могут заменяться без автоматического бэкапа; важные данные следует сохранять отдельно.
- Пути элементов архива нормализуются и проверяются, чтобы исключить распаковку за пределы папки игры.
- Созданный адрес предоставляет доступ к активной передаче. Отправляйте его только нужному получателю в личном сообщении.
- AeroSync не предоставляет постоянного удалённого хранилища. Если прямое соединение невозможно, трафик может проходить через инфраструктуру ретрансляторов Iroh.
- При выборе категории **Моды** компьютер получателя обращается к API и CDN Modrinth.

## Известные ограничения

- Поддерживаются только точные версии Minecraft из таблицы совместимости.
- Игровой интерфейс доступен на английском и русском языках.
- Выделенные серверы не поддерживаются.
- Один экран отправителя работает с одним получателем и одним пакетом.
- AeroSync не объединяет миры и конфиги: полученные файлы заменяют совпадающие цели.
- Автоматически устанавливаются только моды, доступные на Modrinth и найденные по хешу.
- После получения модов, конфигов, ресурспаков, шейдерпаков или настроек рекомендуется перезапустить Minecraft.
- Проект находится на стадии раннего тестирования и может содержать ошибки передачи или совместимости.

## Сборка из исходного кода

Требования:

- JDK `21` и JDK `25`
- Доступ в интернет для загрузки зависимостей Gradle

Сборка всех поддерживаемых релизов на Windows:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\build-all.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\build-neoforge.ps1
```

Release-скриптам нужны JDK `21` и JDK `25` по путям, указанным в их начале. Они создают в `releases/0.0.9/` универсальные JAR для Fabric и NeoForge вместе с `SHA256SUMS.txt`.

Сборка только стандартной цели разработки `1.21.11`:

```powershell
.\gradlew.bat clean build
```

JAR одной версии и JAR с исходным кодом создаются в `build/libs/`.

## Структура проекта

```text
src/main/java/com/aerosync/
├── archiver/    Упаковка, распаковка, резервные копии и категории
├── gui/         Экраны настройки, отправки, получения и выбора мира
├── mixin/       Интеграция в главное меню и меню паузы
├── modrinth/    Создание списка модов и проверенные загрузки
├── network/     Java API отправителя и получателя
└── util/        Клиентские уведомления

src/main/kotlin/com/aerosync/network/
└── AeroIrohTransport.kt    Конечные точки Iroh и протокол передачи

src/versions/
├── common121/              Общий клиентский UI Minecraft 1.21.x
├── legacy121/              Адаптер Minecraft 1.21.1–1.21.8
└── modern121/              Адаптер Minecraft 1.21.9–1.21.11

versions/26.2/
├── src/main/               Общий клиентский UI Minecraft 26.x
└── src/versions/           Bridge-классы Minecraft 26.1 и 26.2

versions/neoforge/          Загрузчик NeoForge, UI-адаптеры и сборки точных версий
versions/neoforge-smoke/    Изолированные проверки готового универсального JAR
```

## Логи

Состояние передачи, отсутствующие на Modrinth файлы, резервные копии и ошибки записываются в стандартный лог Minecraft:

```text
.minecraft/logs/latest.log
```

При отправке сообщения об ошибке приложите строки с `AeroSync`, версии Minecraft, загрузчика и AeroSync, список выбранных категорий, а для Fabric ещё версию Fabric API. Перед публикацией удалите адреса соединения и другую приватную информацию.

## Авторы

- **StandoffKitty75**
- **K2 Studio**
