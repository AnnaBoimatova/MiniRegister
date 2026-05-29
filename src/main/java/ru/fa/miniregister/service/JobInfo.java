package ru.fa.miniregister.service;

import java.time.Instant;
import java.util.UUID;

/**
 * Информация о фоновом задании импорта/экспорта CSV.
 */
public class JobInfo {
    private UUID jobId;
    private JobType type;
    private JobStatus status;
    private String message;
    private String resultFile;
    private Instant startedAt;
    private Instant finishedAt;

    public JobInfo(UUID jobId, JobType type) {
        this.jobId = jobId;
        this.type = type;
        this.status = JobStatus.RUNNING;
        this.startedAt = Instant.now();
    }

    public UUID getJobId() { return jobId; }
    public JobType getType() { return type; }
    public JobStatus getStatus() { return status; }
    public String getMessage() { return message; }
    public String getResultFile() { return resultFile; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }

    public void markDone(String resultFile) {
        this.status = JobStatus.DONE;
        this.resultFile = resultFile;
        this.finishedAt = Instant.now();
    }

    public void markFailed(String message) {
        this.status = JobStatus.FAILED;
        this.message = message;
        this.finishedAt = Instant.now();
    }

    /** Возвращает только имя файла из полного пути (без директории). */
    public String getResultFileName() {
        if (resultFile == null) return null;
        int slash = Math.max(resultFile.lastIndexOf('/'), resultFile.lastIndexOf('\\'));
        return slash >= 0 ? resultFile.substring(slash + 1) : resultFile;
    }
}
