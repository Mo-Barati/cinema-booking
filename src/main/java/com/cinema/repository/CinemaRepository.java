package com.cinema.repository;


import com.cinema.entity.Cinema;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CinemaRepository extends JpaRepository<Cinema, Long> {

    // Find cinemas in a specific city (case-insensitive)
    List<Cinema> findByCityIgnoreCase(String city);

    Optional<Cinema> findByNameIgnoreCase(String name);

    // Check if a cinema with the same name and address already exists
    boolean existsByNameAndAddressLine(String name, String addressLine);

}
