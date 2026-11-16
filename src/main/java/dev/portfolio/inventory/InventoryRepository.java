package dev.portfolio.inventory;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface InventoryRepository extends JpaRepository<Inventory,String> {
 // Atomic compare-and-swap: both quantity and version must match expectations.
 @Modifying(flushAutomatically=true)
 @Query("update Inventory i set i.availableQuantity = i.availableQuantity - :qty, i.version = i.version + 1 where i.sku = :sku and i.version = :version and i.availableQuantity >= :qty")
 int reserveIfVersionMatches(@Param("sku") String sku,@Param("qty") int qty,@Param("version") long version);
}
