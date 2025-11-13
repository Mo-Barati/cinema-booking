package com.cinema.service;

import com.cinema.entity.Cinema;
import com.cinema.entity.Showtime;
import com.cinema.entity.Ticket;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ShowtimeService {

    Showtime create(Showtime showtime);
    Showtime update(Long id, Showtime update);
    boolean delete(Long id);

    Optional<Showtime> findById(Long id);
    List<Showtime> findByCinema(Long cinemaId);
    List<Showtime> searchByTitle(String query);
    List<Showtime> findInWindow(Long cinemaId, LocalDateTime from, LocalDateTime to);
    List<Showtime> filter(String q, Long cinemaId, LocalDateTime from, LocalDateTime to);


    // ✨ Add these two:
    List<Showtime> findAll();
    Optional<Cinema> findCinemaByName(String name);

    /**
     * Atomically books the given seats for a showtime.
     * Fails if any seat is invalid or already booked.
     */
    void bookSeats(Long showtimeId, List<Long> seatIds);


}
