package ru.fa.miniregister.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.fa.miniregister.service.CsvJobService;
import ru.fa.miniregister.service.JobInfo;

import java.util.UUID;

/**
 * REST-контроллер для управления фоновыми заданиями импорта/экспорта CSV.
 */
@Tag(name = "jobs", description = "Фоновые задания импорта/экспорта CSV")
@RestController
@RequestMapping("/api/jobs")
public class JobRestController {

    private final CsvJobService csvJobs;

    public JobRestController(CsvJobService csvJobs) {
        this.csvJobs = csvJobs;
    }

    @Operation(summary = "Запустить экспорт всех броней в CSV",
               description = "Запускает фоновое задание. Файл сохраняется в ./storage/records-B-{jobId}.csv.",
               responses = @ApiResponse(responseCode = "202", description = "Задание принято в обработку"))
    @PostMapping("/export")
    public ResponseEntity<JobInfo> startExport() {
        UUID jobId = csvJobs.startExportCsv();
        return ResponseEntity.accepted().body(csvJobs.getJob(jobId));
    }

    @Operation(summary = "Запустить импорт броней из CSV-файла",
               description = "Файл должен находиться в папке ./storage на сервере.",
               responses = @ApiResponse(responseCode = "202", description = "Задание принято в обработку"))
    @PostMapping("/import")
    public ResponseEntity<JobInfo> startImport(
            @Parameter(description = "Имя файла в папке storage", example = "records-B-uuid.csv")
            @RequestParam String fileName) {
        UUID jobId = csvJobs.startImportCsv(fileName);
        return ResponseEntity.accepted().body(csvJobs.getJob(jobId));
    }

    @Operation(summary = "Получить статус задания",
               description = "Возвращает RUNNING, DONE или FAILED. При DONE для экспорта содержит путь к файлу.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Статус задания"),
                   @ApiResponse(responseCode = "404", description = "Задание не найдено")
               })
    @GetMapping("/{jobId}")
    public JobInfo getJob(
            @Parameter(description = "UUID задания") @PathVariable UUID jobId) {
        return csvJobs.getJob(jobId);
    }
}
