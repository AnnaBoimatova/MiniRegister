package ru.fa.miniregister;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import ru.fa.miniregister.domain.RecordEntity;
import ru.fa.miniregister.domain.RecordStatus;
import ru.fa.miniregister.repo.RecordRepository;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тест репозитория: проверяет сохранение и чтение записи из БД.
 */
@DataJpaTest
class RecordRepositoryTest {

    @Autowired
    RecordRepository repo;

    @Test
    void saveAndFind() {
        RecordEntity saved = repo.save(new RecordEntity("Бронь B-101", "Переговорная на 3 этаже", RecordStatus.NEW));
        assertNotNull(saved.getId());

        RecordEntity found = repo.findById(saved.getId()).orElseThrow();
        assertEquals("Бронь B-101", found.getTitle());
        assertEquals("Переговорная на 3 этаже", found.getDescription());
        assertEquals(RecordStatus.NEW, found.getStatus());
    }

    @Test
    void deleteRecord() {
        RecordEntity saved = repo.save(new RecordEntity("Бронь B-999 (удалить)", "", RecordStatus.DONE));
        Long id = saved.getId();

        repo.deleteById(id);

        assertFalse(repo.existsById(id));
    }
}
