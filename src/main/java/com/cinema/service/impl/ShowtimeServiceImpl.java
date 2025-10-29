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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ShowtimeServiceImpl implements ShowtimeService{

    private final ShowtimeRepository showtimeRepo;
    private final CinemaRepository cinemaRepo;


    public ShowtimeServiceImpl(ShowtimeRepository showtimeRepo, CinemaRepository cinemaRepo) {
        this.showtimeRepo = showtimeRepo;
        this.cinemaRepo = cinemaRepo;
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
        return showtimeRepo.findByCinemaId(cinemaId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Showtime> searchByTitle(String query) {
        return showtimeRepo.findByMovieTitle(query);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Showtime> findInWindow(Long cinemaId, LocalDateTime from, LocalDateTime to) {
        return showtimeRepo.findByCinemaId(cinemaId).stream()
                .filter(s -> s.getStartTime().isBefore(to) && s.getEndTime().isAfter(from))
                .toList();
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
