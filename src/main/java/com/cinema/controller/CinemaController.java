package com.cinema.controller;

import com.cinema.entity.Cinema;
import com.cinema.service.CinemaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/cinemas")
@CrossOrigin(origins = "http://localhost:5173")
// @CrossOrigin(origins = "http://localhost:5173")
public class CinemaController {

    private final CinemaService cinemaService;

    public CinemaController(CinemaService cinemaService) {
        this.cinemaService = cinemaService;
    }

    @GetMapping
    public List<Cinema> getAllCinemas() {
        return cinemaService.getAllCinemas();
    }

    @PostMapping
    public ResponseEntity<Cinema> createCinema(@RequestBody Cinema cinema) {
        Cinema saved = cinemaService.createCinema(cinema);
        return ResponseEntity.created(URI.create("/api/cinemas/" + saved.getId())).body(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCinema(@PathVariable Long id) {
        cinemaService.deleteCinema(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Cinema> updateCinema(
            @PathVariable Long id,
            @RequestBody Cinema cinema
    ) {
        Cinema updated = cinemaService.updateCinema(id, cinema);
        return ResponseEntity.ok(updated);
    }

}
