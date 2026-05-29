package ru.fa.miniregister.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.fa.miniregister.domain.RecordEntity;

/**
 * Spring Data JPA репозиторий для CRUD-операций над {@link RecordEntity}.
 * Реализация генерируется Spring Data автоматически.
 */
public interface RecordRepository extends JpaRepository<RecordEntity, Long> {
}
