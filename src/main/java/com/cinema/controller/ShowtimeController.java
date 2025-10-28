package com.cinema.controller;


import com.cinema.entity.Cinema;
import com.cinema.entity.Showtime;
import com.cinema.service.ShowtimeService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/showtimes")
public class ShowtimeController {

    private final ShowtimeService service;

    public ShowtimeController(ShowtimeService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public Showtime get(@PathVariable Long id) {
        return service.findById(id).orElse(null);
    }

    @GetMapping("/by-cinema/{cinemaId}")
    public List<Showtime> byCinema(@PathVariable Long cinemaId) {
        return service.findByCinema(cinemaId);
    }

    @GetMapping("/search")
    public List<Showtime> search(@RequestParam String q) {
        return service.searchByTitle(q);
    }

    @GetMapping("/window")
    public List<Showtime> window(@RequestParam Long cinemaId,
                                 @RequestParam LocalDateTime from,
                                 @RequestParam LocalDateTime to) {
        return service.findInWindow(cinemaId, from, to);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Showtime create(@RequestBody ShowtimeRequest req) {
        return service.create(toEntity(req));
    }

    @PutMapping("/{id}")
    public Showtime update(@PathVariable Long id, @RequestBody ShowtimeRequest req) {
        return service.update(id, toEntity(req));
    }

    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable Long id) {
        return service.delete(id);
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


    // Minimal DTO to avoid sending nested Cinema in requests
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
