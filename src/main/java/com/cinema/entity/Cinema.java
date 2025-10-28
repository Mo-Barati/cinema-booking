package com.cinema.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonManagedReference;


@Entity
@Table(name = "cinema")
public class Cinema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name ="address_Line", nullable = false, length = 180)
    private String addressLine;

    @Column(nullable = false, length = 80)
    private String city;

    @Column(length = 80)
    private String stateOrProvince;

    @Column(length = 16)
    private String postcode;

    @Column(length = 80)
    private String country;

    // Capacity
    @jakarta.validation.constraints.Min(1)
    @jakarta.validation.constraints.Max(50)
    @Column(nullable = false)
    private int totalScreens;

    // Contact
    @jakarta.validation.constraints.Size(max = 32)
    @Column(length = 32)
    private String phone;


    @jakarta.validation.constraints.Email
    @jakarta.validation.constraints.Size(max = 120)
    @Column(length = 120)
    private String email;

    // --- Audit timestamps ---
    @Column(nullable = false, updatable = false)
    private java.time.OffsetDateTime createdAt;

    @Column(nullable = false)
    private java.time.OffsetDateTime updatedAt;


    // --- Relationships ---
    @OneToMany(mappedBy = "cinema", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private java.util.List<Showtime> showtimes = new java.util.ArrayList<>();


    // --- Constructors ---
    public Cinema() {
        // Default constructor required by JPA
    }


    public Cinema(String name, String addressLine, String city, String stateOrProvince,
                  String postcode, String country, int totalScreens,
                  String phone, String email) {
        this.name = name;
        this.addressLine = addressLine;
        this.city = city;
        this.stateOrProvince = stateOrProvince;
        this.postcode = postcode;
        this.country = country;
        this.totalScreens = totalScreens;
        this.phone = phone;
        this.email = email;
    }


    // --- Getters and Setters ---
    public Long getId() { return id; }
    public void setId(Long id) {
        this.id = id;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddressLine() { return addressLine; }
    public void setAddressLine(String addressLine) { this.addressLine = addressLine; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getStateOrProvince() { return stateOrProvince; }
    public void setStateOrProvince(String stateOrProvince) { this.stateOrProvince = stateOrProvince; }

    public String getPostcode() { return postcode; }
    public void setPostcode(String postcode) { this.postcode = postcode; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public int getTotalScreens() { return totalScreens; }
    public void setTotalScreens(int totalScreens) { this.totalScreens = totalScreens; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public java.time.OffsetDateTime getCreatedAt() { return createdAt; }
    public java.time.OffsetDateTime getUpdatedAt() { return updatedAt; }

    public java.util.List<Showtime> getShowtimes() { return showtimes; }
    public void setShowtimes(java.util.List<Showtime> showtimes) { this.showtimes = showtimes; }


    // --- Lifecycle callbacks to manage timestamps automatically ---
    @PrePersist
    protected void onCreate() {
        java.time.OffsetDateTime now = java.time.OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = java.time.OffsetDateTime.now();
    }

    // --- Helpers for bidirectional relationship ---
    public void addShowtime(Showtime s) {
        if (s == null) return;
        if (!this.showtimes.contains(s)) {
            this.showtimes.add(s);
            s.setCinema(this); // will resolve once Showtime entity exists
        }
    }

    public void removeShowtime(Showtime s) {
        if (s == null) return;
        if (this.showtimes.remove(s)) {
            s.setCinema(null); // will resolve once Showtime entity exists
        }
    }

    // --- Equality based on persistent identity ---
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cinema)) return false;
        Cinema other = (Cinema) o;
        return id != null && id.equals(other.id);
    }


    @Override
    public int hashCode() {
        // constant hash until persisted; avoids issues before id is assigned
        return 31;
    }


    @Override
    public String toString() {
        return "Cinema{id=" + id + ", name='" + name + "', city='" + city + "'}";
    }


}


























