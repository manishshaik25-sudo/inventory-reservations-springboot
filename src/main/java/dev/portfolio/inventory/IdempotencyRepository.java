package dev.portfolio.inventory;
import org.springframework.data.jpa.repository.JpaRepository;
public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord,String> {}
