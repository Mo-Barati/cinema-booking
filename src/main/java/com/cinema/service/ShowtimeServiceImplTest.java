package com.cinema.service;

import com.cinema.entity.Cinema;
import com.cinema.entity.Showtime;
import com.cinema.exception.BusinessRuleViolationException;
import com.cinema.exception.OverlappingShowtimeException;
import com.cinema.exception.ResourceNotFoundException;
import com.cinema.repository.CinemaRepository;
import com.cinema.repository.ShowtimeRepository;
import com.cinema.service.impl.ShowtimeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ShowtimeServiceImplTest {

    ShowtimeRepository showtimeRepo;
    CinemaRepository cinemaRepo;
    ShowtimeService service;

    @BeforeEach
    void setup() {
        showtimeRepo = mock(ShowtimeRepository.class);
        cinemaRepo = mock(CinemaRepository.class);
        service = new ShowtimeServiceImpl(showtimeRepo, cinemaRepo);
    }


    private Showtime baseShowtime(Long cinemaId) {
        Cinema c = new Cinema();
        c.setId(cinemaId);
        Showtime s = new Showtime();
        s.setMovieTitle("The Batman");
        s.setScreenNumber(1);
        s.setStartTime(LocalDateTime.now().plusDays(1));
        s.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));
        s.setTicketPrice(10.0);
        s.setLanguage("English");
        s.setFormat("2D");
        s.setCinema(c);
        return s;
    }

    @Test
    void create_success() {
        Showtime s = baseShowtime(100L);

        when(cinemaRepo.existsById(100L)).thenReturn(true);
        when(showtimeRepo.existsOverlappingShowtimeExcludingId(100L, 1, s.getStartTime(), s.getEndTime(), null))
                .thenReturn(false);
        when(showtimeRepo.save(any(Showtime.class))).thenAnswer(inv -> inv.getArgument(0));

        Showtime saved = service.create(s);

        assertEquals("The Batman", saved.getMovieTitle());
        verify(showtimeRepo).save(any(Showtime.class));
    }

    @Test
    void create_throwsWhenEndBeforeStart() {
        Showtime s = baseShowtime(100L);
        s.setEndTime(s.getStartTime().minusMinutes(10));

        BusinessRuleViolationException ex =
                assertThrows(BusinessRuleViolationException.class, () -> service.create(s));
        assertTrue(ex.getMessage().contains("endTime must be after startTime"));
        verifyNoInteractions(cinemaRepo);
        verify(showtimeRepo, never()).save(any());
    }

    @Test
    void create_throwsWhenCinemaMissing() {
        Showtime s = baseShowtime(null);
        s.setCinema(null);

        BusinessRuleViolationException ex =
                assertThrows(BusinessRuleViolationException.class, () -> service.create(s));
        assertTrue(ex.getMessage().contains("cinema is required"));
    }

    @Test
    void create_throwsWhenCinemaNotFound() {
        Showtime s = baseShowtime(200L);
        when(cinemaRepo.existsById(200L)).thenReturn(false);

        ResourceNotFoundException ex =
                assertThrows(ResourceNotFoundException.class, () -> service.create(s));
        assertTrue(ex.getMessage().contains("Cinema not found"));
    }

    @Test
    void create_throwsWhenOverlap() {
        Showtime s = baseShowtime(300L);
        when(cinemaRepo.existsById(300L)).thenReturn(true);
        when(showtimeRepo.existsOverlappingShowtimeExcludingId(300L, 1, s.getStartTime(), s.getEndTime(), null))
                .thenReturn(true);

        assertThrows(OverlappingShowtimeException.class, () -> service.create(s));
        verify(showtimeRepo, never()).save(any());
    }

    void update_excludesSelfInOverlapCheck() {
        Showtime existing = baseShowtime(400L);
        existing.setScreenNumber(2);

        when(showtimeRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(cinemaRepo.existsById(400L)).thenReturn(true);
        when(showtimeRepo.existsOverlappingShowtimeExcludingId(400L, 2,
                existing.getStartTime(), existing.getEndTime(), 1L)).thenReturn(false);
        when(showtimeRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Showtime updated = service.update(1L, new Showtime());
        assertEquals(2, updated.getScreenNumber());
        verify(showtimeRepo).existsOverlappingShowtimeExcludingId(400L, 2,
                existing.getStartTime(), existing.getEndTime(), 1L);
    }


}
