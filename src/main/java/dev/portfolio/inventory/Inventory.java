package dev.portfolio.inventory;
import jakarta.persistence.*;
@Entity @Table(name="inventory")
public class Inventory {
 @Id @Column(length=64) private String sku;
 @Column(name="available_quantity",nullable=false) private int availableQuantity;
 @Version @Column(nullable=false) private long version;
 protected Inventory(){}
 public String getSku(){return sku;}
 public int getAvailableQuantity(){return availableQuantity;}
 public long getVersion(){return version;}
}
