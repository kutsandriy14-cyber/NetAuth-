# 📂 NetAuth Account (.af) & Chat (.chat) Reader App Specification Prompt
# 🌐 Промт для Читающей Программы Файлов Формата .af и .chat

This prompt contains the exact technical schemas, format specification, and UI guidelines to build a standalone desktop, web, or mobile viewer utility for NetAuth `.af` (Account File) and `.chat` (Chat File) data configurations. 

Give this prompt to any AI developer agent or assistant to automatically generate the reading application.

---

## 🇺🇸 ENGLISH: PORTABLE READER UTILITY SPECIFICATION PROMPT

```text
Act as an expert Software Engineer. Your goal is to build a modern, high-performance reader and editor utility (written in Python/Tkinter, Electron, or Jetpack Compose) to inspect, parse, edit, and convert NetAuth secure configuration extensions: ".af" (Account File) and ".chat" (Chat File).

========================================================================
1. ".af" (ACCOUNT FILE) TECHNICAL SCHEMA & SPECIFICATION
========================================================================
An ".af" file is a unified backup/export format of a user's entire account, files, and preferences. It can be formatted either as a single JSON object (encrypted or plaintext) or a zipped folder bundle:

- If Zipped Bundle, the folder contains:
  - `profile.af` (JSON structure representing the account data).
  - `blocklist.af` (JSON representing blocked users).
  - `files_manifest.af` (JSON representing cloud files catalog).
  - `/cloud_files/` (Subdirectory containing physical backup copies of the user's files).

- If Single JSON Object, the structure is:
  ```json
  {
    "fileFormat": "NetAuthAccountBackup",
    "version": "1.0",
    "exportTime": 1782390820000,
    "userProfile": {
      "id": 105,
      "email": "andriy@netauth.lan",
      "passwordHash": "8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918",
      "firstName": "Andriy",
      "lastName": "Kuts",
      "birthDate": "2000-01-01",
      "gender": "Male",
      "avatarColor": -12543232,
      "phoneNumber": "+380000000000",
      "recoveryEmail": "backup@netauth.lan",
      "keyProtect": "SAME_AS_PASSWORD_OR_HASH"
    },
    "blocklist": [
      "spammer@netauth.lan",
      "annoying_user@netauth.lan"
    ],
    "userFiles": [
      {
        "fileName": "document.txt",
        "fileSize": 1024,
        "contentBase64": "SGVsbG8gV29ybGQ="
      }
    ]
  }
  ```

- **Reader App Task for .af**:
  1. Parse the `.af` file (auto-detect JSON or ZIP container).
  2. Display a beautiful profile summary card (Name, Email, Phone, Recovery, Registration Time).
  3. Render active block/mute settings (Spam block status).
  4. Display a files tab allowing the user to select, preview, or extract any embedded files.
  5. Provide an interactive editor to update profile details, with an "Export to NetAuth .af" feature.

========================================================================
2. ".chat" (CHAT FILE) TECHNICAL SCHEMA & SPECIFICATION
========================================================================
A `.chat` file stores the chronological transcripts of conversation messages exchanged between two accounts.

- **Structure**:
  ```json
  {
    "fileFormat": "NetAuthChatTranscript",
    "version": "1.0",
    "participants": [
      "andriy@netauth.lan",
      "friend@netauth.lan"
    ],
    "messageCount": 3,
    "messages": [
      {
        "id": "msg_001",
        "senderEmail": "andriy@netauth.lan",
        "receiverEmail": "friend@netauth.lan",
        "text": "Hey! Are you playing Minecraft today?",
        "timestamp": 1782390830000
      },
      {
        "id": "msg_002",
        "senderEmail": "friend@netauth.lan",
        "receiverEmail": "andriy@netauth.lan",
        "text": "Yeah! Send me the LAN invite.",
        "timestamp": 1782390840000
      },
      {
        "id": "msg_003",
        "senderEmail": "andriy@netauth.lan",
        "receiverEmail": "friend@netauth.lan",
        "text": "Sure, check your launcher overlay right now!",
        "timestamp": 1782390850000
      }
    ]
  }
  ```

- **Reader App Task for .chat**:
  1. Parse the `.chat` transcript structure.
  2. Render a messaging bubble visual style interface:
     - Messages sent by `participants[0]` align to the right (with a custom blue/green bubble).
     - Messages sent by `participants[1]` align to the left (with a gray/slate bubble).
  3. Display accurate date/time headers (converting timestamp to readable format, e.g., `YYYY-MM-DD HH:MM`).
  4. Include a Search Bar to filter messages by text/keywords.
  5. Provide an Export feature (to clear HTML, PDF, or Plain Text transcripts).
```

---

## 🇷🇺 RUSSIAN: СПЕЦИФИКАЦИЯ ДЛЯ ЧИТАЮЩЕЙ ПРОГРАММЫ (ПРОМТ НА РАЗРАБОТКУ)

```text
Действуй как опытный системный и прикладной разработчик. Твоя задача — создать современную, быструю программу для чтения, редактирования и конвертации файлов резервных копий NetAuth с расширениями ".af" (Account File — Файл Аккаунта) и ".chat" (Chat File — Файл Чата).

Программа может быть написана на Python (Tkinter/PyQt), Electron (HTML/JS) или Jetpack Compose (Kotlin).

========================================================================
1. ТЕХНИЧЕСКАЯ СПЕЦИФИКАЦИЯ ФОРМАТА ".af" (ФАЙЛ АККАУНТА)
========================================================================
Файл `.af` представляет собой полный экспорт данных профиля пользователя, его настроек, черного списка и облачного хранилища файлов. Он может существовать как зашифрованный/открытый JSON-файл или ZIP-архив с внутренней структурой:

- В случае ZIP-архива файлы внутри распределены следующим образом:
  - `profile.af` (JSON с регистрационными данными и паролем).
  - `blocklist.af` (JSON со списком заблокированных контактов).
  - `files_manifest.af` (Список и размеры файлов на диске).
  - `/cloud_files/` (Директория с физическими копиями файлов пользователя).

- В случае единого JSON-файла структура выглядит так:
  ```json
  {
    "fileFormat": "NetAuthAccountBackup",
    "version": "1.0",
    "exportTime": 1782390820000,
    "userProfile": {
      "id": 105,
      "email": "andriy@netauth.lan",
      "passwordHash": "ХЭШ_ПАРОЛЯ_SHA256",
      "firstName": "Andriy",
      "lastName": "Kuts",
      "birthDate": "2000-01-01",
      "gender": "Male",
      "avatarColor": -12543232,
      "phoneNumber": "+380000000000",
      "recoveryEmail": "backup@netauth.lan",
      "keyProtect": "SAME_AS_PASSWORD_OR_HASH"
    },
    "blocklist": [
      "spammer@netauth.lan",
      "annoying_user@netauth.lan"
    ],
    "userFiles": [
      {
        "fileName": "document.txt",
        "fileSize": 1024,
        "contentBase64": "SGVsbG8gV29ybGQ="
      }
    ]
  }
  ```

- **Возможности программы-ридера для файлов .af**:
  1. Чтение и автоматическое определение типа файла `.af` (архив или сырой JSON).
  2. Отображение красивой карточки пользователя (Имя, Фамилия, Email, телефон, почта восстановления, дата регистрации).
  3. Просмотр списка блокировок (спам-фильтр).
  4. Просмотр файлов на диске с возможностью их скачивания на рабочий стол или предварительного просмотра прямо в интерфейсе.
  5. Возможность редактировать данные и сохранять обратно в файл `.af`.

========================================================================
2. ТЕХНИЧЕСКАЯ СПЕЦИФИКАЦИЯ ФОРМАТА ".chat" (ФАЙЛ ЧАТА)
========================================================================
Файл `.chat` хранит полную хронологическую историю переписки между двумя пользователями системы.

- **Структура**:
  ```json
  {
    "fileFormat": "NetAuthChatTranscript",
    "version": "1.0",
    "participants": [
      "andriy@netauth.lan",
      "friend@netauth.lan"
    ],
    "messageCount": 3,
    "messages": [
      {
        "id": "msg_001",
        "senderEmail": "andriy@netauth.lan",
        "receiverEmail": "friend@netauth.lan",
        "text": "Привет! Будешь играть в Майнкрафт сегодня?",
        "timestamp": 1782390830000
      },
      {
        "id": "msg_002",
        "senderEmail": "friend@netauth.lan",
        "receiverEmail": "andriy@netauth.lan",
        "text": "Да! Скинь приглашение.",
        "timestamp": 1782390840000
      },
      {
        "id": "msg_003",
        "senderEmail": "andriy@netauth.lan",
        "receiverEmail": "friend@netauth.lan",
        "text": "Ок, лови в лаунчере прямо сейчас!",
        "timestamp": 1782390850000
      }
    ]
  }
  ```

- **Возможности программы-ридера для файлов .chat**:
  1. Чтение файла истории переписки `.chat`.
  2. Отображение переписки в стиле мессенджера (бабблы сообщений):
     - Сообщения от автора `participants[0]` отображаются справа (в зеленом/синем блоке).
     - Сообщения от собеседника `participants[1]` отображаются слева (в сером блоке).
  3. Форматирование временных меток в понятную дату (например, `ДД.ММ.ГГГГ ЧЧ:ММ`).
  4. Встроенный поиск по тексту сообщений (фильтрация истории).
  5. Экспорт переписки в текстовый формат, HTML или PDF.
```
