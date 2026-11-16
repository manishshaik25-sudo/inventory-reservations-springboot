package dev.portfolio.inventory;
import jakarta.persistence.*;
@Entity @Table(name="reservations")
public class Reservation {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,length=64) private String sku;
 @Column(nullable=false) private int quantity;
 protected Reservation(){}
 public Reservation(String sku,int quantity){this.sku=sku;this.quantity=quantity;}
 public Long getId(){return id;}
 public String getSku(){return sku;}
 public int getQuantity(){return quantity;}
}
