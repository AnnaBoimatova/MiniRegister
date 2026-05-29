package ru.fa.miniregister.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.fa.miniregister.domain.RecordEntity;
import ru.fa.miniregister.service.RecordService;

import java.net.URI;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * REST-контроллер для управления бронями переговорных (CRUD).
 */
@Tag(name = "records", description = "CRUD-операции над бронями переговорных")
@RestController
@RequestMapping("/api/records")
public class RecordRestController {

    private final RecordService service;

    public RecordRestController(RecordService service) {
        this.service = service;
    }

    @Operation(summary = "Получить список всех броней",
               responses = @ApiResponse(responseCode = "200", description = "Список броней"))
    @GetMapping
    public List<RecordEntity> list() {
        return service.findAll();
    }

    @Operation(summary = "Получить бронь по ID",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Бронь найдена"),
                   @ApiResponse(responseCode = "404", description = "Бронь не найдена",
                                content = @Content(schema = @Schema(type = "string")))
               })
    @GetMapping("/{id}")
    public RecordEntity get(
            @Parameter(description = "ID брони", example = "1") @PathVariable long id) {
        return service.findById(id);
    }

    @Operation(summary = "Создать новую бронь",
               responses = {
                   @ApiResponse(responseCode = "201", description = "Бронь создана"),
                   @ApiResponse(responseCode = "400", description = "Ошибка валидации",
                                content = @Content(schema = @Schema(type = "string")))
               })
    @PostMapping
    public ResponseEntity<RecordEntity> create(@Valid @RequestBody RecordEntity record) {
        RecordEntity created = service.create(record);
        return ResponseEntity
                .created(URI.create("/api/records/" + created.getId()))
                .body(created);
    }

    @Operation(summary = "Удалить бронь по ID",
               responses = {
                   @ApiResponse(responseCode = "204", description = "Бронь удалена"),
                   @ApiResponse(responseCode = "404", description = "Бронь не найдена",
                                content = @Content(schema = @Schema(type = "string")))
               })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID брони", example = "1") @PathVariable long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<String> notFound(NoSuchElementException e) {
        return ResponseEntity.status(404).body(e.getMessage());
    }
}
