package com.cinema.service;

import com.cinema.entity.Showtime;

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
}
