package com.cinema.service.impl;

import com.cinema.entity.Cinema;
import com.cinema.entity.Showtime;
import com.cinema.exception.BusinessRuleViolationException;
import com.cinema.exception.OverlappingShowtimeException;
import com.cinema.exception.ResourceNotFoundException;
import com.cinema.repository.CinemaRepository;
import com.cinema.repository.ShowtimeRepository;
import com.cinema.service.ShowtimeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.cinema.entity.Seat;
import com.cinema.entity.Ticket;
import com.cinema.repository.SeatRepository;
import com.cinema.repository.TicketRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ShowtimeServiceImpl implements ShowtimeService{

    private final ShowtimeRepository showtimeRepo;
    private final CinemaRepository cinemaRepo;
    private final SeatRepository seatRepo;
    private final TicketRepository ticketRepo;


    public ShowtimeServiceImpl(ShowtimeRepository showtimeRepo,
                               CinemaRepository cinemaRepo,
                               SeatRepository seatRepo,
                               TicketRepository ticketRepo) {
        this.showtimeRepo = showtimeRepo;
        this.cinemaRepo = cinemaRepo;
        this.seatRepo = seatRepo;
        this.ticketRepo = ticketRepo;
    }

    @Override
    @Transactional
    public void bookSeats(Long showtimeId, List<Long> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) {
            throw new BusinessRuleViolationException("seatIds must not be empty");
        }

        Showtime showtime = showtimeRepo.findById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found: " + showtimeId));

        List<Seat> seats = seatRepo.findAllById(seatIds);
        if (seats.size() != seatIds.size()) {
            throw new ResourceNotFoundException("One or more seats not found");
        }

        Long cinemaId = showtime.getCinema().getId();
        int screen = showtime.getScreenNumber();
        boolean allMatch = seats.stream()
                .allMatch(seat -> seat.getCinema().getId().equals(cinemaId)
                        && seat.getScreenNumber() == screen);
        if (!allMatch) {
            throw new BusinessRuleViolationException("One or more seats do not belong to this showtime's screen");
        }

        List<Ticket> existing = ticketRepo.findByShowtimeIdAndSeatIdIn(showtimeId, seatIds);
        if (!existing.isEmpty()) {
            throw new BusinessRuleViolationException("One or more seats are already booked");
        }

        double price = showtime.getTicketPrice();
        List<Ticket> ticketsToSave = seats.stream()
                .map(seat -> new Ticket(showtime, seat, price))
                .toList();

        ticketRepo.saveAll(ticketsToSave); // no return
    }




    @Override
    public Showtime create(Showtime s) {
        validateFields(s);
        Long cinemaId = requireCinemaId(s.getCinema());
        ensureCinemaExists(cinemaId);
        ensureNoOverlap(cinemaId, s.getScreenNumber(), s.getStartTime(), s.getEndTime(), null);
        return showtimeRepo.save(s);
    }

    @Override
    public Showtime update(Long id, Showtime u) {
        Showtime existing = showtimeRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found: " + id));

        // Apply allowed field changes
        if (u.getMovieTitle() != null) existing.setMovieTitle(u.getMovieTitle());
        if (u.getScreenNumber() != 0) existing.setScreenNumber(u.getScreenNumber());
        if (u.getStartTime() != null) existing.setStartTime(u.getStartTime());
        if (u.getEndTime() != null) existing.setEndTime(u.getEndTime());
        if (u.getTicketPrice() != 0.0) existing.setTicketPrice(u.getTicketPrice());
        if (u.getLanguage() != null) existing.setLanguage(u.getLanguage());
        if (u.getFormat() != null) existing.setFormat(u.getFormat());
        if (u.getCinema() != null) existing.setCinema(u.getCinema());

        validateFields(existing);
        Long cinemaId = requireCinemaId(existing.getCinema());
        ensureCinemaExists(cinemaId);
        ensureNoOverlap(cinemaId, existing.getScreenNumber(),
                existing.getStartTime(), existing.getEndTime(), id);

        return showtimeRepo.save(existing);
    }

    @Override
    public boolean delete(Long id) {
        return showtimeRepo.findById(id)
                .map(s -> { showtimeRepo.deleteById(id); return true; })
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Showtime> findById(Long id) {
        return showtimeRepo.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Showtime> findByCinema(Long cinemaId) {
        return showtimeRepo.findByCinema_Id(cinemaId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Showtime> searchByTitle(String query) {
        return showtimeRepo.findByMovieTitleIgnoreCaseContaining(query == null ? "" : query);
    }


    @Override
    @Transactional(readOnly = true)
    public List<Showtime> findInWindow(Long cinemaId, LocalDateTime from, LocalDateTime to) {
        return showtimeRepo.findByCinema_IdAndStartTimeBetween(cinemaId, from, to);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Showtime> findAll() {
        return showtimeRepo.findAll();
    }

    @Override
    public Optional<Cinema> findCinemaByName(String name) {
        if (name == null) return Optional.empty();
        String target = normalize(name);

        // Fallback: normalize all names and compare (robust for spaces/case differences)
        return cinemaRepo.findAll().stream()
                .filter(c -> normalize(c.getName()).equals(target))
                .findFirst();
    }

    @Override
    public List<Showtime> filter(String q, Long cinemaId, LocalDateTime from, LocalDateTime to) {
        String term = (q == null) ? "" : q.trim();
        boolean hasTitle = !term.isEmpty();
        boolean hasCinema = cinemaId != null;
        boolean hasFrom = from != null;
        boolean hasTo = to != null;

        List<Showtime> base;

        // Pick the broadest efficient query first
        if (hasCinema && hasFrom && hasTo) {
            base = showtimeRepo.findByCinema_IdAndStartTimeBetween(cinemaId, from, to);
        } else if (!hasCinema && hasFrom && hasTo) {
            base = showtimeRepo.findByStartTimeBetween(from, to);
        } else if (hasCinema && !hasFrom && !hasTo) {
            base = showtimeRepo.findByCinema_Id(cinemaId);
        } else if (!hasCinema && !hasFrom && !hasTo && hasTitle) {
            base = showtimeRepo.findByMovieTitleIgnoreCaseContaining(term);
        } else {
            base = showtimeRepo.findAll();
        }

        if (hasTitle) {
            String lc = term.toLowerCase();
            base = base.stream()
                    .filter(s -> s.getMovieTitle() != null && s.getMovieTitle().toLowerCase().contains(lc))
                    .toList();
        }

        return base;
    }


    private String normalize(String s) {
        return s == null ? "" : s.toLowerCase().trim().replaceAll("\\s+", " ");
    }

    // --- helpers ---

    private void validateFields(Showtime s) {
        if (s.getMovieTitle() == null || s.getMovieTitle().isBlank())
            throw new BusinessRuleViolationException("movieTitle is required");
        if (s.getStartTime() == null || s.getEndTime() == null)
            throw new BusinessRuleViolationException("startTime and endTime are required");
        if (!s.getEndTime().isAfter(s.getStartTime()))
            throw new BusinessRuleViolationException("endTime must be after startTime");
        if (s.getScreenNumber() <= 0)
            throw new BusinessRuleViolationException("screenNumber must be positive");
        if (s.getTicketPrice() < 0)
            throw new BusinessRuleViolationException("ticketPrice must be >= 0");
        if (s.getCinema() == null)
            throw new BusinessRuleViolationException("cinema is required");
    }

    private void ensureNoOverlap(Long cinemaId, int screen, LocalDateTime start, LocalDateTime end, Long excludeId) {
        boolean clash = showtimeRepo.existsOverlappingShowtimeExcludingId(cinemaId, screen, start, end, excludeId);
        if (clash) {
            throw new OverlappingShowtimeException(
                    "Overlapping showtime for cinema=" + cinemaId + ", screen=" + screen);
        }
    }

    private void ensureCinemaExists(Long cinemaId) {
        if (!cinemaRepo.existsById(cinemaId)) {
            throw new ResourceNotFoundException("Cinema not found: " + cinemaId);
        }
    }

    private Long requireCinemaId(Cinema c) {
        if (c.getId() == null)
            throw new BusinessRuleViolationException("cinema.id is required");
        return c.getId();
    }


}
