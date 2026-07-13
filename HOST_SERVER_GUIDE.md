# Полное Руководство: Запуск домашнего сервера NetAuth в Termux с туннелированием через Ngrok

Ты абсолютно прав! Запуск сервера внутри клиентского приложения на телефоне — это костыль. Настоящий домашний хостинг должен работать независимо. **Termux** — это полноценная среда Linux на твоем Android-устройстве, которая идеально подходит для запуска нашего Python-сервера баз данных. А **Ngrok** позволит тебе получить бесплатный публичный статический или динамический адрес с HTTPS, чтобы подключаться к своему домашнему серверу с любого устройства из любой точки мира (через мобильный интернет или любой Wi-Fi) абсолютно бесплатно!

Ниже представлено пошаговое руководство на русском языке со всеми командами, которые нужно просто скопировать и вставить в Termux.

---

## Шаг 1. Установка Termux на телефон-сервер

> **ВАЖНО:** Не устанавливай Termux из Google Play! Версия там давно устарела и репозитории в ней не работают.
1. Скачай актуальный APK-файл Termux из **F-Droid** или напрямую с GitHub:
   * Ссылка на F-Droid: [https://f-droid.org/packages/com.termux/](https://f-droid.org/packages/com.termux/)
   * Ссылка на GitHub Releases: [https://github.com/termux/termux-app/releases](https://github.com/termux/termux-app/releases) (выбирай файл `termux-app_v0.118.1+github-debug_universal.apk` или новее).
2. Установи скачанный APK на телефон, который будет выступать в роли сервера.

---

## Шаг 2. Обновление репозиториев и установка окружения

Запусти Termux и поочередно выполни следующие команды для обновления системы и установки Python, SQLite и необходимых инструментов:

```bash
# 1. Обновляем пакетную базу и системные утилиты (на все запросы нажимаем Enter или Y)
pkg update && pkg upgrade -y

# 2. Разрешаем доступ к хранилищу телефона (появится системный запрос — подтверди его)
termux-setup-storage

# 3. Устанавливаем Python, SQLite, Git и wget
pkg install python git sqlite wget -y

# 4. Устанавливаем Flask (веб-фреймворк для нашего сервера)
pip install flask
```

---

## Шаг 3. Создание и запуск Python-сервера

Создадим отдельную папку для нашего сервера баз данных и запишем туда скрипт `server.py`:

```bash
# 1. Создаем папку netauth-server и переходим в нее
mkdir -p ~/netauth-server && cd ~/netauth-server
```

Теперь создай файл `server.py`. Для этого введи команду:
```bash
cat << 'EOF' > server.py
import os
import sys
import json
import socket
import threading
import time
import sqlite3
from flask import Flask, request, jsonify

app = Flask(__name__)

@app.after_request
def after_request(response):
    response.headers.add('Access-Control-Allow-Origin', '*')
    response.headers.add('Access-Control-Allow-Headers', 'Content-Type,Authorization')
    response.headers.add('Access-Control-Allow-Methods', 'GET,PUT,POST,DELETE,OPTIONS')
    return response

DB_FILE = "netauth.db"
MAX_QUOTA_BYTES = 512 * 1024 * 1024  # Лимит хранилища на пользователя: 512 MB

def get_db_connection():
    conn = sqlite3.connect(DB_FILE)
    conn.row_factory = sqlite3.Row
    return conn

def init_db():
    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS server_users (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            email TEXT UNIQUE NOT NULL,
            passwordHash TEXT NOT NULL,
            firstName TEXT,
            lastName TEXT,
            birthDate TEXT,
            gender TEXT,
            avatarColor INTEGER,
            phoneNumber TEXT,
            recoveryEmail TEXT,
            createdAt INTEGER
        )
    """)
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS user_files (
            fileId INTEGER PRIMARY KEY AUTOINCREMENT,
            userId INTEGER NOT NULL,
            fileName TEXT NOT NULL,
            fileData BLOB,
            fileSize INTEGER,
            lastUpdated INTEGER,
            FOREIGN KEY(userId) REFERENCES server_users(id) ON DELETE CASCADE,
            UNIQUE(userId, fileName)
        )
    """)
    conn.commit()
    conn.close()
    print(f"[*] База данных успешно инициализирована: {DB_FILE}")

# UDP-ответчик для автоматического обнаружения в локальной сети Wi-Fi
def start_udp_responder():
    udp_port = 8888
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        sock.bind(('0.0.0.0', udp_port))
        print(f"[*] UDP-слушатель автообнаружения запущен на порту {udp_port}")
    except Exception as e:
        print(f"[!] Ошибка запуска UDP-слушателя: {e}")
        return

    while True:
        try:
            data, addr = sock.recvfrom(1024)
            message = data.decode('utf-8', errors='ignore').strip()
            if message == "NETAUTH_DISCOVER":
                s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
                try:
                    s.connect(('8.8.8.8', 80))
                    local_ip = s.getsockname()[0]
                except Exception:
                    local_ip = "127.0.0.1"
                finally:
                    s.close()
                
                response = f"NETAUTH_SERVER:http://{local_ip}:8080/"
                sock.sendto(response.encode('utf-8'), addr)
                print(f"[UDP] -> Отправлен ответ автообнаружения для {addr[0]}:{addr[1]}")
        except Exception as e:
            print(f"[!] Ошибка UDP сокета: {e}")
            time.sleep(1)

# API Эндпоинты
@app.route('/api/status', methods=['GET'])
def get_status():
    return jsonify({
        "status": "ok",
        "message": "NetAuth Termux Server is active",
        "serverTime": int(time.time() * 1000)
    }), 200

@app.route('/api/register', methods=['POST'])
def register_user():
    data = request.json or {}
    email = data.get("email", "").strip().lower()
    password_hash = data.get("passwordHash", "")
    first_name = data.get("firstName", "")
    last_name = data.get("lastName", "")
    birth_date = data.get("birthDate", "")
    gender = data.get("gender", "")
    avatar_color = data.get("avatarColor", -12543232)
    phone_number = data.get("phoneNumber", "")
    recovery_email = data.get("recoveryEmail", "")

    if not email or not password_hash:
        return jsonify({"status": "error", "message": "Email и пароль обязательны"}), 400

    conn = get_db_connection()
    cursor = conn.cursor()
    try:
        cursor.execute("""
            INSERT INTO server_users (
                email, passwordHash, firstName, lastName, birthDate, gender, avatarColor, phoneNumber, recoveryEmail, createdAt
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, (
            email, password_hash, first_name, last_name, birth_date, gender, avatar_color, phone_number, recovery_email, int(time.time() * 1000)
        ))
        conn.commit()
        user_id = cursor.lastrowid
        
        cursor.execute("SELECT * FROM server_users WHERE id = ?", (user_id,))
        user = cursor.fetchone()
        
        user_data = dict(user)
        user_data.pop("passwordHash", None)
        return jsonify(user_data), 201
    except sqlite3.IntegrityError:
        return jsonify({"status": "error", "message": "Этот email уже зарегистрирован"}), 409
    except Exception as e:
        return jsonify({"status": "error", "message": str(e)}), 500
    finally:
        conn.close()

@app.route('/api/login', methods=['POST'])
def login_user():
    data = request.json or {}
    email = data.get("email", "").strip().lower()
    password_hash = data.get("passwordHash", "")

    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute("SELECT * FROM server_users WHERE email = ?", (email,))
    user = cursor.fetchone()
    conn.close()

    if user is None:
        return jsonify({"status": "error", "message": "Пользователь не найден"}), 404
    
    if user["passwordHash"] != password_hash:
        return jsonify({"status": "error", "message": "Неверный пароль"}), 401

    user_data = dict(user)
    user_data.pop("passwordHash", None)
    return jsonify(user_data), 200

@app.route('/api/users/<int:user_id>', methods=['PUT'])
def update_profile(user_id):
    data = request.json or {}
    first_name = data.get("firstName")
    last_name = data.get("lastName")
    birth_date = data.get("birthDate")
    gender = data.get("gender")
    phone_number = data.get("phoneNumber")
    recovery_email = data.get("recoveryEmail")

    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute("SELECT * FROM server_users WHERE id = ?", (user_id,))
    user = cursor.fetchone()

    if user is None:
        conn.close()
        return jsonify({"status": "error", "message": "Пользователь не найден"}), 404

    first_name = first_name if first_name is not None else user["firstName"]
    last_name = last_name if last_name is not None else user["lastName"]
    birth_date = birth_date if birth_date is not None else user["birthDate"]
    gender = gender if gender is not None else user["gender"]
    phone_number = phone_number if phone_number is not None else user["phoneNumber"]
    recovery_email = recovery_email if recovery_email is not None else user["recoveryEmail"]

    cursor.execute("""
        UPDATE server_users SET
            firstName = ?, lastName = ?, birthDate = ?, gender = ?, phoneNumber = ?, recoveryEmail = ?
        WHERE id = ?
    """, (first_name, last_name, birth_date, gender, phone_number, recovery_email, user_id))
    conn.commit()

    cursor.execute("SELECT * FROM server_users WHERE id = ?", (user_id,))
    updated_user = cursor.fetchone()
    conn.close()

    user_data = dict(updated_user)
    user_data.pop("passwordHash", None)
    return jsonify(user_data), 200

@app.route('/api/users/<int:user_id>/password', methods=['PUT'])
def update_password(user_id):
    data = request.json or {}
    password_hash = data.get("passwordHash")

    if not password_hash:
        return jsonify({"status": "error", "message": "passwordHash обязателен"}), 400

    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute("SELECT id FROM server_users WHERE id = ?", (user_id,))
    user = cursor.fetchone()

    if user is None:
        conn.close()
        return jsonify({"status": "error", "message": "Пользователь не найден"}), 404

    cursor.execute("UPDATE server_users SET passwordHash = ? WHERE id = ?", (password_hash, user_id))
    conn.commit()
    conn.close()

    return jsonify({"status": "ok", "message": "Пароль успешно обновлен"}), 200

@app.route('/api/users/<int:user_id>', methods=['DELETE'])
def delete_user(user_id):
    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute("SELECT id FROM server_users WHERE id = ?", (user_id,))
    user = cursor.fetchone()

    if user is None:
        conn.close()
        return jsonify({"status": "error", "message": "Пользователь не найден"}), 404

    cursor.execute("DELETE FROM user_files WHERE userId = ?", (user_id,))
    cursor.execute("DELETE FROM server_users WHERE id = ?", (user_id,))
    conn.commit()
    conn.close()

    return jsonify({"status": "ok", "message": "Пользователь и его облачные файлы успешно удалены"}), 200

# Облачное Хранилище Игровых Файлов и Прогресса (Лимит 512MB)
@app.route('/api/users/<int:user_id>/storage', methods=['POST'])
def upload_file(user_id):
    data = request.json or {}
    file_name = data.get("fileName", "").strip()
    file_data_b64 = data.get("fileData", "")
    
    if not file_name or not file_data_b64:
        return jsonify({"status": "error", "message": "fileName и fileData обязательны"}), 400

    try:
        import base64
        file_bytes = base64.b64decode(file_data_b64)
        file_size = len(file_bytes)
    except Exception as e:
        return jsonify({"status": "error", "message": f"Ошибка декодирования Base64: {e}"}), 400

    conn = get_db_connection()
    cursor = conn.cursor()

    cursor.execute("SELECT id FROM server_users WHERE id = ?", (user_id,))
    if cursor.fetchone() is None:
        conn.close()
        return jsonify({"status": "error", "message": "Пользователь не найден"}), 404

    cursor.execute("SELECT SUM(fileSize) FROM user_files WHERE userId = ? AND fileName != ?", (user_id, file_name))
    row = cursor.fetchone()
    current_total = row[0] or 0

    if current_total + file_size > MAX_QUOTA_BYTES:
        conn.close()
        return jsonify({"status": "error", "message": "Превышена квота облачного хранилища в 512MB для этого аккаунта"}), 413

    try:
        cursor.execute("""
            INSERT OR REPLACE INTO user_files (userId, fileName, fileData, fileSize, lastUpdated)
            VALUES (?, ?, ?, ?, ?)
        """, (user_id, file_name, sqlite3.Binary(file_bytes), file_size, int(time.time() * 1000)))
        conn.commit()

        remaining = MAX_QUOTA_BYTES - (current_total + file_size)
        return jsonify({
            "status": "ok",
            "message": "Файл успешно синхронизирован с облаком",
            "fileSize": file_size,
            "remainingQuotaBytes": remaining
        }), 200
    except Exception as e:
        return jsonify({"status": "error", "message": str(e)}), 500
    finally:
        conn.close()

@app.route('/api/users/<int:user_id>/storage/<string:file_name>', methods=['GET'])
def download_file(user_id, file_name):
    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute("SELECT fileData, fileSize FROM user_files WHERE userId = ? AND fileName = ?", (user_id, file_name))
    row = cursor.fetchone()
    conn.close()

    if row is None:
        return jsonify({"status": "error", "message": "Файл не найден в облаке"}), 404

    import base64
    file_bytes = row["fileData"]
    file_data_b64 = base64.b64encode(file_bytes).decode('utf-8')

    return jsonify({
        "status": "ok",
        "fileName": file_name,
        "fileSize": row["fileSize"],
        "fileData": file_data_b64
    }), 200

if __name__ == '__main__':
    init_db()
    udp_thread = threading.Thread(target=start_udp_responder, daemon=True)
    udp_thread.start()
    print("[*] HTTP Сервер запускается на порту 8080...")
    app.run(host='0.0.0.0', port=8080, debug=False, threaded=True)
EOF
```

Отлично! Файл создан. Теперь давай настроим туннель.

---

## Шаг 4. Установка и настройка Ngrok в Termux

**Ngrok** создаст безопасный зашифрованный HTTPS-туннель к твоему телефону.

1. Зарегистрируйся бесплатно на официальном сайте [https://ngrok.com](https://ngrok.com).
2. Зайди в свой личный кабинет Ngrok и скопируй свой персональный токен авторизации (Auth Token) в разделе **Your Authtoken**.
3. Установи Ngrok в Termux с помощью официального скрипта или скачав пакет напрямую:

```bash
# Скачиваем официальный стабильный бинарник ngrok для архитектуры ARM64 (большинство современных Android устройств)
cd ~
wget https://bin.equinox.io/c/b34edq8Zmqn/ngrok-v3-stable-linux-arm64.tgz

# Распаковываем архив
tar -xvzf ngrok-v3-stable-linux-arm64.tgz

# Переносим бинарник в системную директорию Termux для удобного запуска из любого места
mv ngrok ~/../usr/bin/
chmod +x ~/../usr/bin/ngrok

# Удаляем архив, чтобы не занимать место
rm ngrok-v3-stable-linux-arm64.tgz
```

4. Добавь свой скопированный токен авторизации в Ngrok:
```bash
ngrok config add-authtoken ТВОЙ_СКОПИРОВАННЫЙ_ТОКЕН_ИЗ_ЛИЧНОГО_КАБИНЕТА
```

---

## Шаг 5. Запуск сервера и туннеля (Две сессии Termux)

Для одновременного запуска сервера и туннеля нам понадобятся две сессии Termux:

### Сессия 1: Запуск Python HTTP-сервера
В первой открытой вкладке Termux введи:
```bash
cd ~/netauth-server
python server.py
```
Ты увидишь логи инициализации SQLite базы данных и сообщение: `[*] HTTP Сервер запускается на порту 8080...`.

### Сессия 2: Запуск туннеля Ngrok
1. Сделай свайп от левого края экрана Termux вправо и нажми кнопку **"New Session"** (или зажми экран в любом месте и выбери "More..." -> "New session").
2. Во второй открывшейся вкладке запусти туннель:
```bash
ngrok http 8080
```
3. На экране отобразится статус Ngrok. Найди строку **Forwarding**. Она будет выглядеть примерно так:
   `https://a8bc-123-45-67-89.ngrok-free.app`
4. **Скопируй этот адрес!** Это твой глобальный, бесплатный, безопасный HTTPS URL сервера, который работает прямо на твоем телефоне.

---

## Шаг 6. Подключение приложения-клиента

1. Открой клиентское приложение **NetAuth** на основном телефоне.
2. Перейди в раздел настроек подключения или выбери ручную настройку сервера.
3. Вставь скопированный Ngrok-адрес (например, `https://a8bc-123-45-67-89.ngrok-free.app`) в поле ввода адреса сервера и нажми "Подключиться" / "Сохранить".
4. Теперь твои мобильные клиенты, игры и сервисы будут мгновенно авторизовываться, создавать профили и сохранять игровой прогресс прямо в SQLite базу данных, хранящуюся в Termux на твоем домашнем телефоне-сервере, совершенно бесплатно!
