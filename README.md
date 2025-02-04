Short-link-service

Данный проект позволяет пользователю получить короткие ссылки, переходить по ним, устанавливать лимит жизни и переходов.

Требования:
Java 11 или выше
IDE (например, IntelliJ IDEA) или консоль для запуска приложения

Установка:
Склонируйте репозиторий или загрузите проект.=
Убедитесь, что файл config.properties находится в корневой папке проекта или рядом с исполняемым файлом.
Убедитесь, что в файле config.properties указаны следующие параметры:
default_ttl=3600
default_visit_limit=100

default_ttl — время жизни ссылки в секундах по умолчанию.
default_visit_limit — лимит переходов по ссылке по умолчанию.

Запуск:
Скомпилируйте и запустите проект из IDE или с помощью командной строки:
javac ShortUrlService.java
java ShortUrlService
При запуске программы введите свой UUID (или введите exit, чтобы завершить выполнение).

Основные функции
Главное меню
После ввода UUID пользователь увидит следующее меню:
1. Сократить URL-адрес
2. Перейти по короткой ссылке
3. Обновить лимит URL
4. Удалить URL
5. Список ссылок
6. Выход
  
1. Сократить URL-адрес
Позволяет сократить URL:
Введите URL для сокращения.м
Укажите время жизни (TTL) ссылки в секундах (или 0 для значения по умолчанию).
Укажите лимит переходов (или 0 для значения по умолчанию).

2. Перейти по короткой ссылке
Позволяет перейти по короткой ссылке. Введите сокращённый URL, чтобы увидеть перенаправление.

3. Обновить лимит URL
Позволяет изменить лимит переходов по существующей короткой ссылке.

4. Удалить URL
Удаляет указанную короткую ссылку.

5. Список ссылок
Отображает список всех коротких ссылок пользователя.

6. Выход
Возвращает к вводу UUID. Если пользователь введёт тот же UUID при повторном входе, его ссылки сохранятся.

Завершение программы
При полном выходе из программы все ссылки будут удалены, а задачи очистки отменены. Чтобы полностью завершить сервис, введите exit на этапе ввода UUID.

Примечания:
Каждая ссылка уникальна для пользователя.
Истёкшие или превышенные по лимиту ссылки автоматически удаляются с уведомлением пользователя.
Если файл config.properties отсутствует, программа завершится с ошибкой загрузки.
