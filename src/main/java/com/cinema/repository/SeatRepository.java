package com.cinema.repository;

import com.cinema.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    // All seats for a given cinema + screen, ordered nicely for a grid
    List<Seat> findByCinemaIdAndScreenNumberOrderByRowLabelAscSeatNumberAsc(
            Long cinemaId,
            int screenNumber
    );
}
