package com.cinema.repository;

import com.cinema.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByShowtimeId(Long showtimeId);

    boolean existsByShowtimeIdAndSeatId(Long showtimeId, Long seatId);

    List<Ticket> findByShowtimeIdAndSeatIdIn(Long showtimeId, List<Long> seatIds);
}
