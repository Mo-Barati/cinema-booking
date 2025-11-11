package com.cinema.controller;

import com.cinema.entity.Cinema;
import com.cinema.entity.Showtime;
import com.cinema.exception.ResourceNotFoundException;
import com.cinema.service.ShowtimeService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for showtime management.
 * Supports both full payload creation and a simplified creation
 * used by the current frontend form.
 */
@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/showtimes")
public class ShowtimeController {

    private final ShowtimeService service;

    public ShowtimeController(ShowtimeService service) {
        this.service = service;
    }

    // ========== LIST / GET ==========
    /** Allow GET /api/showtimes (needed by the frontend table) */
    @GetMapping
    public List<Showtime> getAll() {
        return service.findAll(); // implement in service: return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Showtime> get(@PathVariable Long id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found"));
    }

    @GetMapping("/by-cinema/{cinemaId}")
    public List<Showtime> byCinema(@PathVariable Long cinemaId) {
        return service.findByCinema(cinemaId);
    }

    // accept q OR movieTitle OR query
    @GetMapping("/search")
    public List<Showtime> search(@RequestParam(required = false, name = "q") String q,
                                 @RequestParam(required = false, name = "movieTitle") String movieTitle,
                                 @RequestParam(required = false, name = "query") String query) {
        String term = q != null ? q : (movieTitle != null ? movieTitle : query);
        if (term == null) term = "";
        return service.searchByTitle(term);
    }

    @GetMapping("/window")
    public List<Showtime> window(@RequestParam Long cinemaId,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return service.findInWindow(cinemaId, from, to);
    }

    @GetMapping("/filter")
    public List<Showtime> filter(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long cinemaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        return service.filter(q, cinemaId, from, to);
    }


    // ========== CREATE ==========
    /**
     * Full create – expects the complete structure used by your domain.
     * Keep this for API clients or future admin forms that send all fields.
     */
    @PostMapping
    public ResponseEntity<Showtime> create(@RequestBody ShowtimeRequest req) {
        Showtime created = service.create(toEntity(req));
        return ResponseEntity
                .created(URI.create("/api/showtimes/" + created.getId()))
                .body(created);
    }

    /**
     * Simple create – matches the current frontend form (movieTitle, cinemaName, start/end).
     * Looks up the cinema by name and fills safe defaults for missing fields.
     */
    @PostMapping("/simple")
    public ResponseEntity<Showtime> createSimple(@RequestBody SimpleShowtimeRequest r) {
        // You need a helper in the service to find a cinema by name (case-insensitive)
        // e.g., service.findCinemaByName(String) -> Optional<Cinema>
        Optional<Cinema> cinemaOpt = service.findCinemaByName(r.cinemaName);
        Cinema cinema = cinemaOpt.orElseThrow(() ->
                new ResourceNotFoundException("Cinema not found: " + r.cinemaName));

        Showtime s = new Showtime();
        s.setMovieTitle(r.movieTitle);
        s.setCinema(cinema);

        // Sensible defaults (adjust as your domain evolves)
        s.setScreenNumber(1);
        s.setTicketPrice(10.0);
        s.setLanguage("EN");
        s.setFormat("2D");

        s.setStartTime(r.startTime);
        s.setEndTime(r.endTime);

        Showtime saved = service.create(s);
        return ResponseEntity
                .created(URI.create("/api/showtimes/" + saved.getId()))
                .body(saved);
    }

    // ========== UPDATE / DELETE ==========
    @PutMapping("/{id}")
    public ResponseEntity<Showtime> update(@PathVariable Long id, @RequestBody ShowtimeRequest req) {
        return ResponseEntity.ok(service.update(id, toEntity(req)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        boolean removed = service.delete(id);
        return removed ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    // ========== Mapping helpers / DTOs ==========

    private Showtime toEntity(ShowtimeRequest r) {
        Showtime s = new Showtime();
        s.setMovieTitle(r.movieTitle);
        s.setScreenNumber(r.screenNumber);
        s.setStartTime(r.startTime);
        s.setEndTime(r.endTime);
        s.setTicketPrice(r.ticketPrice);
        s.setLanguage(r.language);
        s.setFormat(r.format);

        Cinema c = new Cinema();
        c.setId(r.cinemaId);
        s.setCinema(c);
        return s;
    }

    /** Full payload DTO used by POST / and PUT /{id} */
    public static class ShowtimeRequest {
        public String movieTitle;
        public int screenNumber;
        public LocalDateTime startTime;
        public LocalDateTime endTime;
        public double ticketPrice;
        public String language;
        public String format;
        public Long cinemaId;
    }

    /** Simple payload DTO used by POST /simple (current frontend form) */
    public static class SimpleShowtimeRequest {
        public String movieTitle;
        public String cinemaName;
        public LocalDateTime startTime;
        public LocalDateTime endTime;
    }
}
