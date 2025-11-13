package com.cinema.service;

import com.cinema.web.dto.SeatStatusDto;

import java.util.List;

public interface SeatMapService {

    /**
     * Returns the seat map for a given showtime:
     * every seat for the showtime's cinema + screen,
     * marked FREE or BOOKED.
     */
    List<SeatStatusDto> getSeatMapForShowtime(Long showtimeId);
}
