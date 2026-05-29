package ru.fa.miniregister.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.fa.miniregister.domain.RecordEntity;
import ru.fa.miniregister.repo.RecordRepository;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Сервисный слой для работы с записями реестра.
 * Внедрение зависимостей через конструктор (IoC/DI).
 */
@Service
public class RecordService {

    private final RecordRepository repo;

    public RecordService(RecordRepository repo) {
        this.repo = repo;
    }

    /** Возвращает список всех записей. */
    public List<RecordEntity> findAll() {
        return repo.findAll();
    }

    /**
     * Возвращает запись по идентификатору.
     * @throws NoSuchElementException если запись не найдена
     */
    public RecordEntity findById(long id) {
        return repo.findById(id).orElseThrow(() ->
                new NoSuchElementException("Record not found: id=" + id));
    }

    /** Сохраняет новую запись в БД и возвращает её с присвоенным id. */
    @Transactional
    public RecordEntity create(RecordEntity record) {
        return repo.save(record);
    }

    /**
     * Обновляет поля существующей записи.
     * @throws NoSuchElementException если запись не найдена
     */
    @Transactional
    public RecordEntity update(long id, RecordEntity data) {
        RecordEntity existing = findById(id);
        existing.setTitle(data.getTitle());
        existing.setDescription(data.getDescription());
        existing.setStatus(data.getStatus());
        return repo.save(existing);
    }

    /**
     * Удаляет запись по идентификатору.
     * @throws NoSuchElementException если запись не найдена
     */
    @Transactional
    public void delete(long id) {
        if (!repo.existsById(id)) {
            throw new NoSuchElementException("Record not found: id=" + id);
        }
        repo.deleteById(id);
    }
}
