package com.cinema.controller;

import com.cinema.entity.Cinema;
import com.cinema.entity.Showtime;
import com.cinema.exception.ResourceNotFoundException;
import com.cinema.service.ShowtimeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

// Allow the React dev server to call this API during development
@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/showtimes")
public class ShowtimeController {

    private final ShowtimeService service;

    public ShowtimeController(ShowtimeService service) {
        this.service = service;
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

    @PostMapping
    public ResponseEntity<Showtime> create(@RequestBody ShowtimeRequest req) {
        Showtime created = service.create(toEntity(req));
        return ResponseEntity
                .created(URI.create("/api/showtimes/" + created.getId()))
                .body(created);
    }

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
}
