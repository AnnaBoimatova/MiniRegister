package ru.fa.miniregister.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.fa.miniregister.domain.RecordEntity;
import ru.fa.miniregister.domain.RecordStatus;

import jakarta.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Сервис фонового импорта и экспорта записей в формате CSV.
 * Операции выполняются в пуле потоков через {@link ExecutorService}.
 */
@Service
public class CsvJobService {

    private final RecordService recordService;
    private final Path storageDir;

    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Map<UUID, JobInfo> jobs = new ConcurrentHashMap<>();

    public CsvJobService(RecordService recordService,
                         @Value("${app.storageDir:./storage}") String storageDir) {
        this.recordService = recordService;
        this.storageDir = Paths.get(storageDir);
    }

    /**
     * Запускает экспорт всех записей в CSV-файл в фоновом потоке.
     * @return идентификатор задания (jobId)
     */
    public UUID startExportCsv() {
        UUID id = UUID.randomUUID();
        JobInfo jobInfo = new JobInfo(id, JobType.EXPORT_CSV);
        jobs.put(id, jobInfo);

        executor.submit(() -> {
            try {
                Files.createDirectories(storageDir);
                Path file = storageDir.resolve("records-B-" + id + ".csv");
                exportToCsv(file);
                jobInfo.markDone(file.toString());
            } catch (Exception e) {
                jobInfo.markFailed(e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        });

        return id;
    }

    /**
     * Запускает импорт записей из CSV-файла в фоновом потоке.
     * @param fileName имя файла внутри директории storage
     * @return идентификатор задания (jobId)
     */
    public UUID startImportCsv(String fileName) {
        UUID id = UUID.randomUUID();
        JobInfo jobInfo = new JobInfo(id, JobType.IMPORT_CSV);
        jobs.put(id, jobInfo);

        executor.submit(() -> {
            try {
                Files.createDirectories(storageDir);
                Path file = storageDir.resolve(fileName).normalize();

                // Защита от path traversal (выхода за пределы storageDir через ../)
                if (!file.startsWith(storageDir.normalize())) {
                    throw new IllegalArgumentException("Invalid fileName (path traversal)");
                }

                int imported = importFromCsv(file);
                jobInfo.markDone("IMPORTED_ROWS=" + imported);
            } catch (Exception e) {
                jobInfo.markFailed(e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        });

        return id;
    }

    /** Возвращает информацию о задании по его идентификатору. */
    public JobInfo getJob(UUID id) {
        JobInfo info = jobs.get(id);
        if (info == null) throw new NoSuchElementException("Job not found: " + id);
        return info;
    }

    private void exportToCsv(Path file) {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

            writer.write("title;description;status;createdAt");
            writer.newLine();

            for (RecordEntity r : recordService.findAll()) {
                writer.write(escape(r.getTitle()));
                writer.write(';');
                writer.write(escape(r.getDescription()));
                writer.write(';');
                writer.write(r.getStatus().name());
                writer.write(';');
                writer.write(r.getCreatedAt().toString());
                writer.newLine();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private int importFromCsv(Path file) {
        int count = 0;
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String header = reader.readLine();
            if (header == null) return 0;

            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(";", -1);
                if (parts.length < 3) continue;

                String title = unescape(parts[0]);
                String description = unescape(parts[1]);
                RecordStatus status = RecordStatus.valueOf(parts[2]);

                recordService.create(new RecordEntity(title, description, status));
                count++;
            }
            return count;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace(";", "\\;");
    }

    private String unescape(String s) {
        if (s == null) return "";
        return s.replace("\\;", ";").replace("\\\\", "\\");
    }

    @PreDestroy
    public void shutdown() {
        // JavaDoc рекомендует завершать неиспользуемый ExecutorService для освобождения ресурсов
        executor.shutdown();
    }
}
