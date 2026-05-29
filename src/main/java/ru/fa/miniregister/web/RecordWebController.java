package ru.fa.miniregister.web;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.fa.miniregister.domain.RecordEntity;
import ru.fa.miniregister.domain.RecordStatus;
import ru.fa.miniregister.service.CsvJobService;
import ru.fa.miniregister.service.RecordService;

import java.io.IOException;
import java.nio.file.*;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Контроллер веб-страниц Thymeleaf: список, CRUD, экспорт/импорт CSV.
 */
@Controller
public class RecordWebController {

    private final RecordService service;
    private final CsvJobService csvJobService;
    private final Path storageDir;

    public RecordWebController(RecordService service,
                               CsvJobService csvJobService,
                               @Value("${app.storageDir:./storage}") String storageDir) {
        this.service = service;
        this.csvJobService = csvJobService;
        this.storageDir = Paths.get(storageDir);
    }

    /** GET /records — таблица броней + кнопки экспорта/импорта. */
    @GetMapping("/records")
    public String list(Model model) {
        model.addAttribute("records", service.findAll());
        return "records";
    }

    /** GET /records/new — форма создания брони. */
    @GetMapping("/records/new")
    public String newForm(Model model) {
        model.addAttribute("record", new RecordEntity());
        model.addAttribute("statuses", RecordStatus.values());
        return "record-new";
    }

    /** POST /records — сохранить бронь из формы. */
    @PostMapping("/records")
    public String create(@Valid @ModelAttribute("record") RecordEntity record,
                         BindingResult bindingResult,
                         Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("statuses", RecordStatus.values());
            return "record-new";
        }
        service.create(record);
        return "redirect:/records";
    }

    /** GET /records/{id} — страница деталей брони. */
    @GetMapping("/records/{id}")
    public String detail(@PathVariable long id, Model model) {
        model.addAttribute("record", service.findById(id));
        return "record-detail";
    }

    /** GET /records/{id}/edit — форма редактирования брони. */
    @GetMapping("/records/{id}/edit")
    public String editForm(@PathVariable long id, Model model) {
        model.addAttribute("record", service.findById(id));
        model.addAttribute("statuses", RecordStatus.values());
        return "record-edit";
    }

    /** POST /records/{id}/edit — сохранить изменения брони. */
    @PostMapping("/records/{id}/edit")
    public String update(@PathVariable long id,
                         @Valid @ModelAttribute("record") RecordEntity record,
                         BindingResult bindingResult,
                         Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("statuses", RecordStatus.values());
            return "record-edit";
        }
        service.update(id, record);
        return "redirect:/records/" + id;
    }

    /** POST /records/{id}/delete — удалить бронь и вернуться в список. */
    @PostMapping("/records/{id}/delete")
    public String delete(@PathVariable long id, RedirectAttributes ra) {
        service.delete(id);
        ra.addFlashAttribute("info", "Бронь #" + id + " удалена.");
        return "redirect:/records";
    }

    /** POST /records/export — запустить экспорт CSV, редирект на страницу статуса задания. */
    @PostMapping("/records/export")
    public String startExport() {
        UUID jobId = csvJobService.startExportCsv();
        return "redirect:/records/jobs/" + jobId;
    }

    /**
     * POST /records/import — загрузить CSV-файл и запустить импорт.
     * Файл сохраняется в ./storage, затем передаётся фоновому заданию.
     */
    @PostMapping("/records/import")
    public String startImport(@RequestParam("file") MultipartFile file,
                              RedirectAttributes ra) throws IOException {
        if (file.isEmpty()) {
            ra.addFlashAttribute("error", "Выберите CSV-файл для загрузки.");
            return "redirect:/records";
        }
        Files.createDirectories(storageDir);
        String fileName = StringUtils.cleanPath(
                java.util.Objects.requireNonNull(file.getOriginalFilename()));
        Path target = storageDir.resolve(fileName).normalize();
        if (!target.startsWith(storageDir.normalize())) {
            ra.addFlashAttribute("error", "Недопустимое имя файла.");
            return "redirect:/records";
        }
        file.transferTo(target);

        UUID jobId = csvJobService.startImportCsv(fileName);
        return "redirect:/records/jobs/" + jobId;
    }

    /** GET /records/jobs/{jobId} — страница статуса фонового задания. */
    @GetMapping("/records/jobs/{jobId}")
    public String jobStatus(@PathVariable UUID jobId, Model model) {
        model.addAttribute("job", csvJobService.getJob(jobId));
        return "job-status";
    }

    /**
     * GET /records/download?fileName=... — скачать готовый CSV-файл из ./storage.
     * Защита от path traversal: файл должен быть внутри storageDir.
     */
    @GetMapping("/records/download")
    public ResponseEntity<Resource> download(@RequestParam String fileName) {
        Path file = storageDir.resolve(fileName).normalize();
        if (!file.startsWith(storageDir.normalize()) || !Files.exists(file)) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.getFileName() + "\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(resource);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public String jobNotFound(NoSuchElementException e, Model model) {
        model.addAttribute("error", e.getMessage());
        return "redirect:/records";
    }
}
