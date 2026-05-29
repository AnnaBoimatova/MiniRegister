# AI-REPORT — MiniRegister

Проект: **MiniRegister** — веб-приложение бронирования переговорных комнат.  
Стек: Spring Boot 3.3, Jakarta Persistence, Thymeleaf, H2, springdoc-openapi, JUnit 5 / MockMvc.

---

## Шаг 1. JPA-сущность RecordEntity

**Что сделано до обращения к ИИ:**  
Создала пакет `ru.fa.miniregister.domain`, определила перечисление `RecordStatus` (NEW, IN_PROGRESS, DONE).

**Промт:**
```
Сгенерируй JPA entity RecordEntity на Jakarta Persistence (@Entity, @Table) с полями:
- id (Long, @Id, @GeneratedValue IDENTITY)
- title (String, @NotBlank, @Column nullable=false)
- description (String, @Column length=2000)
- status (RecordStatus, @Enumerated STRING, nullable=false, default NEW)
- createdAt (LocalDateTime, nullable=false)

Добавь @PrePersist, который проставляет createdAt и status, если они null.
Добавь @Schema аннотации springdoc для каждого поля (description, example).
Конструктор без аргументов и конструктор (title, description, status). Только геттеры + сеттеры для изменяемых полей.
```

**Что изменила после генерации:**  
- Поправила пример в `@Schema` для поля `title` — заменила дефолтный "string" на "Бронь переговорной B-101".  
- Убедилась, что `id` и `createdAt` помечены `accessMode = READ_ONLY`, чтобы Swagger не предлагал их указывать при POST.

**Результат `mvn test`:** все тесты прошли.

---

## Шаг 2. JPA-репозиторий RecordRepository

**Что сделано до обращения к ИИ:**  
Создала пакет `ru.fa.miniregister.repo`.

**Промт:**
```
Сгенерируй Spring Data JPA репозиторий RecordRepository для сущности RecordEntity.
Интерфейс должен расширять JpaRepository<RecordEntity, Long>.
Дополнительных методов не нужно — стандартных CRUD-методов достаточно.
```

**Что изменила после генерации:**  
Изменений не потребовалось — интерфейс тривиальный.

**Результат `mvn test`:** тесты скомпилировались и прошли.

---

## Шаг 3. Сервисный слой RecordService

**Что сделано до обращения к ИИ:**  
Создала пакет `ru.fa.miniregister.service`.

**Промт:**
```
Сгенерируй @Service RecordService для RecordEntity.
Зависимость RecordRepository — через конструктор (не @Autowired на поле).
Методы:
- findAll() → List<RecordEntity>
- findById(long id) → RecordEntity, бросает NoSuchElementException если не найдена
- create(RecordEntity) → RecordEntity, @Transactional
- update(long id, RecordEntity data) → RecordEntity, @Transactional, обновляет title/description/status
- delete(long id) → void, @Transactional, бросает NoSuchElementException если не найдена
```

**Что изменила после генерации:**  
В методе `update` ИИ использовал `findById(id)` через репозиторий напрямую — переделала на вызов собственного метода `findById`, чтобы не дублировать обработку `orElseThrow`.

**Результат `mvn test`:** все тесты прошли.

---

## Шаг 4. REST-контроллер RecordRestController

**Что сделано до обращения к ИИ:**  
Создала пакет `ru.fa.miniregister.web`, определила маппинг `/api/records`.

**Промт:**
```
Сгенерируй @RestController RecordRestController с маппингом /api/records.
Зависимость RecordService — через конструктор.
Эндпоинты:
- GET /api/records → list(), возвращает List<RecordEntity>
- GET /api/records/{id} → get(@PathVariable long id), 200 или 404
- POST /api/records → create(@Valid @RequestBody RecordEntity), возвращает 201 с Location header
- DELETE /api/records/{id} → delete(@PathVariable long id), возвращает 204

Добавь @ExceptionHandler(NoSuchElementException.class) → 404.
Добавь @Operation, @ApiResponse, @Tag аннотации springdoc для каждого метода.
```

**Что изменила после генерации:**  
- В `create` ИИ вернул `ResponseEntity.ok()` вместо `ResponseEntity.created(URI...)` — исправила на правильный 201 с заголовком `Location: /api/records/{id}`.  
- Добавила `@Parameter(description = ..., example = "1")` к параметрам `{id}` — ИИ их пропустил.

**Результат `mvn test`:** все тесты прошли.

---

## Шаг 5. Thymeleaf-страница списка броней (records.html)

**Что сделано до обращения к ИИ:**  
Создала директорию `src/main/resources/templates`, определила, какие данные нужны в модели.

**Промт:**
```
Сгенерируй Thymeleaf страницу records.html — список броней переговорных.
Модель содержит: records (List<RecordEntity>), опциональные flash-атрибуты error и info.

Требования:
- Таблица с колонками: ID, Название, Комментарий, Статус, Создано, Действия
- Кнопки в строке: Просмотр (GET /records/{id}), Редактировать (GET /records/{id}/edit),
  Удалить (POST /records/{id}/delete через form method=post с confirm)
- Кнопка "+ Создать новую бронь" ссылкой на /records/new
- Секция CSV: форма POST /records/export и форма POST /records/import с <input type=file accept=.csv>
- Если список пуст — строка-заглушка "Броней пока нет"
- Ссылки на Swagger UI (/swagger-ui.html) и H2 Console (/h2-console) в подвале
- Встроенный CSS: минимальный, без внешних зависимостей
```

**Что изменила после генерации:**  
- ИИ использовал `th:href="@{/records/{id}}"` без передачи переменной — исправила на `th:href="@{/records/{id}(id=${r.id})}"`.  
- Добавила `onsubmit="return confirm(...)"` на форму удаления — ИИ это пропустил.

**Результат `mvn test`:** тесты прошли; проверила страницу вручную через браузер — отображение корректное.

---

## Шаг 6. MockMvc-тесты RecordRestControllerTest

**Что сделано до обращения к ИИ:**  
Убедилась, что `@SpringBootTest` и `@AutoConfigureMockMvc` подключены в pom.xml через `spring-boot-starter-test`.

**Промт:**
```
Сгенерируй MockMvc тест RecordRestControllerTest для RecordRestController.
Аннотации: @SpringBootTest, @AutoConfigureMockMvc, @Autowired MockMvc.

Тест-методы:
1. createThenList — POST /api/records с валидным JSON {title, description, status},
   ожидаем 201 и заголовок Location содержит /api/records/;
   затем GET /api/records, ожидаем 200 и тело содержит title.
2. getNotFound — GET /api/records/999999, ожидаем 404.
3. createWithBlankTitleReturns400 — POST с title:"", ожидаем 400.

Использовать MockMvcRequestBuilders, MockMvcResultMatchers, Matchers.containsString.
```

**Что изменила после генерации:**  
- ИИ сгенерировал `andExpect(jsonPath("$[0].title").value(...))` — переписала на `containsString(...)` чтобы тест не зависел от порядка записей в базе (база общая для всех тестов).

**Результат `mvn test`:** все 3 теста прошли.

---

## Шаг 7. @DataJpaTest — RecordRepositoryTest

**Что сделано до обращения к ИИ:**  
Уже была настроена зависимость H2 in-memory в pom.xml.

**Промт:**
```
Сгенерируй @DataJpaTest тест RecordRepositoryTest для RecordRepository.

Тест-методы:
1. saveAndFind — сохранить RecordEntity(title, description, NEW), проверить assertNotNull(id),
   затем findById и assertEquals по title, description, status.
2. deleteRecord — сохранить запись, deleteById, assertFalse(existsById).
```

**Что изменила после генерации:**  
Изменений не потребовалось.

**Результат `mvn test`:** оба теста прошли.

---

## Шаг 8. CsvJobService — фоновый экспорт/импорт CSV

**Что сделано до обращения к ИИ:**  
Определила интерфейс: `JobInfo`, `JobStatus` (RUNNING, DONE, FAILED), `JobType` (EXPORT_CSV, IMPORT_CSV).

**Промт:**
```
Сгенерируй @Service CsvJobService для фонового экспорта и импорта RecordEntity в CSV.

Требования:
- ExecutorService newFixedThreadPool(2), завершается в @PreDestroy
- ConcurrentHashMap<UUID, JobInfo> для хранения статусов заданий
- Путь к папке хранения берётся из @Value("${app.storageDir:./storage}")
- startExportCsv() → UUID: в потоке записать все записи в файл records-B-{uuid}.csv,
  формат: title;description;status;createdAt, разделитель ";", экранировать ";" в данных как "\;"
- startImportCsv(String fileName) → UUID: в потоке читать CSV из storageDir,
  создавать RecordEntity через recordService.create()
- getJob(UUID) → JobInfo, NoSuchElementException если не найдено
- Защита от path traversal: проверять что результат normalize() начинается с storageDir
```

**Что изменила после генерации:**  
- ИИ не добавил защиту от path traversal в `startImportCsv` — добавила проверку `if (!file.startsWith(storageDir.normalize()))` вручную.  
- ИИ забыл вызвать `Files.createDirectories(storageDir)` перед записью — добавила.

**Результат `mvn test`:** все тесты прошли.

---

## Шаг 9. OpenAPI-конфигурация

**Что сделано до обращения к ИИ:**  
Добавила зависимость `springdoc-openapi-starter-webmvc-ui` версии 2.6.0 в pom.xml.

**Промт:**
```
Сгенерируй @Configuration класс OpenApiConfig с аннотацией @OpenAPIDefinition.
Info: title "MiniRegister API — Бронь переговорной", version "1.0",
description с кратким описанием REST API и упоминанием CRUD и CSV.
Tags: "records" и "jobs" с описаниями.
```

**Что изменила после генерации:**  
Добавила `contact = @Contact(name = "MiniRegister")` — ИИ поле пропустил.

**Результат `mvn test`:** все тесты прошли; Swagger UI доступен по `/swagger-ui.html`.

---

## Итоговый результат тестов

```
mvn test
...
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

| Класс теста               | Тестов | Статус |
|---------------------------|--------|--------|
| RecordRepositoryTest      | 2      | PASS   |
| RecordRestControllerTest  | 3      | PASS   |
