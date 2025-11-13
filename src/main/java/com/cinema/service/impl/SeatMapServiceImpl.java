package com.cinema.service.impl;

import com.cinema.entity.Cinema;
import com.cinema.entity.Seat;
import com.cinema.entity.Showtime;
import com.cinema.entity.Ticket;
import com.cinema.exception.ResourceNotFoundException;
import com.cinema.repository.SeatRepository;
import com.cinema.repository.ShowtimeRepository;
import com.cinema.repository.TicketRepository;
import com.cinema.service.SeatMapService;
import com.cinema.web.dto.SeatStatus;
import com.cinema.web.dto.SeatStatusDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Service
public class SeatMapServiceImpl implements SeatMapService {


    private final ShowtimeRepository showtimeRepo;
    private final SeatRepository seatRepo;
    private final TicketRepository ticketRepo;

    public SeatMapServiceImpl(ShowtimeRepository showtimeRepo,
                              SeatRepository seatRepo,
                              TicketRepository ticketRepo) {
        this.showtimeRepo = showtimeRepo;
        this.seatRepo = seatRepo;
        this.ticketRepo = ticketRepo;
    }

    @Override
    @Transactional
    public List<SeatStatusDto> getSeatMapForShowtime(Long showtimeId) {
        Showtime showtime = showtimeRepo.findById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found: " + showtimeId));

        Long cinemaId = showtime.getCinema().getId();
        int screenNumber = showtime.getScreenNumber();

        // Try to load seats for this cinema + screen
        List<Seat> seats = seatRepo
                .findByCinemaIdAndScreenNumberOrderByRowLabelAscSeatNumberAsc(cinemaId, screenNumber);

        // If none exist yet, auto-generate a simple seating plan (A–E, 1–10)
        if (seats.isEmpty()) {
            seats = createDefaultSeatsForScreen(showtime.getCinema(), screenNumber);
        }

        // All tickets for that showtime (booked seats)
        List<Ticket> tickets = ticketRepo.findByShowtimeId(showtimeId);
        Set<Long> bookedSeatIds = tickets.stream()
                .map(t -> t.getSeat().getId())
                .collect(Collectors.toCollection(HashSet::new));

        // Map to DTOs
        return seats.stream()
                .map(seat -> {
                    SeatStatus status = bookedSeatIds.contains(seat.getId())
                            ? SeatStatus.BOOKED
                            : SeatStatus.FREE;
                    return new SeatStatusDto(
                            seat.getId(),
                            seat.getRowLabel(),
                            seat.getSeatNumber(),
                            status
                    );
                })
                .toList();
    }

    private List<Seat> createDefaultSeatsForScreen(Cinema cinema, int screenNumber) {
        List<Seat> newSeats = new ArrayList<>();

        // Rows A–E, seats 1–10
        for (char row = 'A'; row <= 'E'; row++) {
            String rowLabel = String.valueOf(row);
            for (int num = 1; num <= 10; num++) {
                Seat seat = new Seat(cinema, screenNumber, rowLabel, num);
                newSeats.add(seat);
            }
        }

        return seatRepo.saveAll(newSeats);
    }


}
