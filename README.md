# MiniRegister — Бронь переговорной (B-records)

Учебное веб-приложение на Java 17 + Spring Boot: реестр броней переговорных комнат.

## Технологии

| Технология | Роль |
|------------|------|
| Java 17, Maven | Язык и система сборки |
| Spring Boot 3 | Основной фреймворк (IoC/DI, Web MVC) |
| Spring Data JPA + Hibernate | ORM, работа с базой данных |
| H2 (in-memory) | Встроенная БД |
| Thymeleaf | Серверный шаблонизатор (веб-страницы) |
| springdoc-openapi | Swagger UI / OpenAPI документация |
| Swing + HttpClient | Десктопный GUI-клиент |

## Быстрый старт

**Требования:** Java 17+, Maven 3.6+

```bash
# Запуск сервера
cd ~/Desktop/MiniRegister
mvn spring-boot:run

# Тесты
mvn test

# Сборка JAR
mvn package
```

## Веб-интерфейс (Thymeleaf)

| URL | Описание |
|-----|----------|
| `/records` | Список всех броней (таблица с кнопками) |
| `/records/new` | Форма создания брони |
| `/records/{id}` | Карточка брони (просмотр) |
| `/records/{id}/edit` | Форма редактирования брони |
| `/records/jobs/{jobId}` | Статус фонового задания CSV |
| `/swagger-ui.html` | Swagger UI — интерактивная документация API |
| `/h2-console` | Консоль H2 (JDBC URL: `jdbc:h2:mem:miniregister`) |

## REST API

### Брони (`/api/records`)

| Метод | URL | Описание | HTTP-код |
|-------|-----|----------|----------|
| `GET` | `/api/records` | Список всех броней | 200 |
| `GET` | `/api/records/{id}` | Бронь по ID | 200 / 404 |
| `POST` | `/api/records` | Создать бронь (JSON) | 201 + Location |
| `DELETE` | `/api/records/{id}` | Удалить бронь | 204 / 404 |

#### Пример создания

```bash
curl -X POST http://localhost:8080/api/records \
  -H "Content-Type: application/json" \
  -d '{"title":"Бронь B-101","description":"3 этаж","status":"NEW"}'
```

### Фоновые задания CSV (`/api/jobs`)

| Метод | URL | Описание | HTTP-код |
|-------|-----|----------|----------|
| `POST` | `/api/jobs/export` | Запустить экспорт всех броней в CSV | 202 |
| `POST` | `/api/jobs/import?fileName=...` | Запустить импорт из CSV | 202 |
| `GET` | `/api/jobs/{jobId}` | Статус задания (RUNNING / DONE / FAILED) | 200 |

```bash
# Экспорт
curl -X POST http://localhost:8080/api/jobs/export
# → {"jobId":"uuid","status":"RUNNING",...}

# Статус
curl http://localhost:8080/api/jobs/{jobId}
# → {"status":"DONE","resultFile":"./storage/records-B-uuid.csv",...}

# Импорт (файл должен лежать в ./storage/)
curl -X POST "http://localhost:8080/api/jobs/import?fileName=records-B-uuid.csv"
```

## Десктопный клиент (Swing)

Запускается отдельно от сервера:

```bash
# Терминал 1 — сервер
mvn spring-boot:run

# Терминал 2 — GUI
mvn exec:java
```

Кнопки: **Загрузить брони** · **Создать тестовую бронь** · **Экспорт CSV** · **Импорт CSV...**

> Spring Boot переупаковывает JAR в формат BOOT-INF, поэтому
> `java -cp target/...jar` не сработает — используйте `mvn exec:java`.

## Структура проекта

```
src/main/java/ru/fa/miniregister/
  config/       — OpenApiConfig (Swagger)
  domain/       — RecordEntity, RecordStatus
  repo/         — RecordRepository (Spring Data JPA)
  service/      — RecordService, CsvJobService, JobInfo
  web/          — RecordRestController, JobRestController, RecordWebController
  desktop/      — DesktopClientApp (Swing)
src/main/resources/
  application.yaml
  templates/    — records.html, record-new.html, record-detail.html,
                  record-edit.html, job-status.html
src/test/java/  — RecordRepositoryTest, RecordRestControllerTest
```

## JavaDoc

```bash
mvn javadoc:javadoc        # → target/reports/apidocs/index.html
mvn site                   # → target/site/index.html (полный отчёт)
```
