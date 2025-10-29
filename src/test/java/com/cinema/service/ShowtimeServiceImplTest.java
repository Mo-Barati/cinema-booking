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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShowtimeServiceImplTest {

    @Mock private ShowtimeRepository showtimeRepo;
    @Mock private CinemaRepository cinemaRepo;

    @InjectMocks
    private ShowtimeServiceImpl service;

    private Cinema cinema;
    private Showtime base;

    @BeforeEach
    void setupData() {
        cinema = new Cinema();
        cinema.setId(100L);

        base = new Showtime();
        base.setMovieTitle("The Batman");
        base.setScreenNumber(1);
        base.setStartTime(LocalDateTime.now().plusDays(1));
        base.setEndTime(base.getStartTime().plusHours(2));
        base.setTicketPrice(10.0);
        base.setLanguage("English");
        base.setFormat("2D");
        base.setCinema(cinema);
    }

    // -------- CREATE --------
    @Test
    @DisplayName("create: saves when valid, cinema exists, and no overlap")
    void create_success() {
        when(cinemaRepo.existsById(100L)).thenReturn(true);
        when(showtimeRepo.existsOverlappingShowtimeExcludingId(100L, 1, base.getStartTime(), base.getEndTime(), null))
                .thenReturn(false);
        when(showtimeRepo.save(any(Showtime.class))).thenAnswer(inv -> inv.getArgument(0));

        Showtime saved = service.create(base);

        assertEquals("The Batman", saved.getMovieTitle());
        verify(showtimeRepo).save(any(Showtime.class));
    }

    @Test
    @DisplayName("create: throws when end <= start")
    void create_throwsWhenEndBeforeStart() {
        base.setEndTime(base.getStartTime().minusMinutes(10));

        BusinessRuleViolationException ex =
                assertThrows(BusinessRuleViolationException.class, () -> service.create(base));
        assertTrue(ex.getMessage().toLowerCase().contains("end"));
        verifyNoInteractions(cinemaRepo);
        verify(showtimeRepo, never()).save(any());
    }

    @Test
    @DisplayName("create: throws when cinema null")
    void create_throwsWhenCinemaMissing() {
        base.setCinema(null);

        BusinessRuleViolationException ex =
                assertThrows(BusinessRuleViolationException.class, () -> service.create(base));
        assertTrue(ex.getMessage().toLowerCase().contains("cinema"));
        verifyNoInteractions(cinemaRepo);
        verify(showtimeRepo, never()).save(any());
    }

    @Test
    @DisplayName("create: throws when cinema not found")
    void create_throwsWhenCinemaNotFound() {
        when(cinemaRepo.existsById(100L)).thenReturn(false);

        ResourceNotFoundException ex =
                assertThrows(ResourceNotFoundException.class, () -> service.create(base));
        assertTrue(ex.getMessage().toLowerCase().contains("cinema"));
        verify(showtimeRepo, never()).save(any());
    }

    @Test
    @DisplayName("create: throws overlap when repo says true")
    void create_throwsWhenOverlap() {
        when(cinemaRepo.existsById(100L)).thenReturn(true);
        when(showtimeRepo.existsOverlappingShowtimeExcludingId(100L, 1, base.getStartTime(), base.getEndTime(), null))
                .thenReturn(true);

        assertThrows(OverlappingShowtimeException.class, () -> service.create(base));
        verify(showtimeRepo, never()).save(any());
    }

    // -------- UPDATE --------
    @Test
    @DisplayName("update: excludes self in overlap check and saves")
    void update_excludesSelfInOverlapCheck() {
        Showtime existing = copy(base);
        existing.setId(1L);

        when(showtimeRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(cinemaRepo.existsById(100L)).thenReturn(true);
        when(showtimeRepo.existsOverlappingShowtimeExcludingId(
                100L, 1, existing.getStartTime(), existing.getEndTime(), 1L)).thenReturn(false);
        when(showtimeRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Showtime updated = service.update(1L, copy(existing)); // reuse values
        assertEquals(1, updated.getScreenNumber());
        verify(showtimeRepo).existsOverlappingShowtimeExcludingId(
                100L, 1, existing.getStartTime(), existing.getEndTime(), 1L);
    }

    @Test
    @DisplayName("update: throws not found when id missing")
    void update_notFound() {
        when(showtimeRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.update(999L, base));
        verify(showtimeRepo, never()).save(any());
    }

    @Test
    @DisplayName("update: throws when end <= start")
    void update_throwsBadTimes() {
        Showtime existing = copy(base);
        existing.setId(1L);
        when(showtimeRepo.findById(1L)).thenReturn(Optional.of(existing));

        Showtime bad = copy(existing);
        bad.setEndTime(bad.getStartTime().minusMinutes(1));

        assertThrows(BusinessRuleViolationException.class, () -> service.update(1L, bad));
        verify(showtimeRepo, never()).save(any());
    }

    @Test
    @DisplayName("update: throws overlap when repo says true")
    void update_throwsOverlap() {
        Showtime existing = copy(base);
        existing.setId(1L);
        when(showtimeRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(cinemaRepo.existsById(100L)).thenReturn(true);

        when(showtimeRepo.existsOverlappingShowtimeExcludingId(
                100L, 1, existing.getStartTime(), existing.getEndTime(), 1L)).thenReturn(true);

        assertThrows(OverlappingShowtimeException.class, () -> service.update(1L, existing));
        verify(showtimeRepo, never()).save(any());
    }

    // -------- DELETE --------
    @Test
    @DisplayName("delete: returns true when present and deleteById called")
    void delete_true() {
        Showtime existing = copy(base);
        existing.setId(10L);
        when(showtimeRepo.findById(10L)).thenReturn(Optional.of(existing));
        doNothing().when(showtimeRepo).deleteById(10L);

        boolean removed = service.delete(10L);

        assertTrue(removed);
        verify(showtimeRepo).deleteById(10L);
    }

    @Test
    @DisplayName("delete: returns false when not found")
    void delete_false() {
        when(showtimeRepo.findById(999L)).thenReturn(Optional.empty());

        boolean removed = service.delete(999L);

        assertFalse(removed);
        verify(showtimeRepo, never()).deleteById(anyLong());
    }

    // -------- FINDS --------
    @Test
    @DisplayName("findById: returns Optional present when found")
    void findById_present() {
        Showtime found = copy(base);
        found.setId(5L);
        when(showtimeRepo.findById(5L)).thenReturn(Optional.of(found));

        Optional<Showtime> got = service.findById(5L);

        assertTrue(got.isPresent());
        assertEquals(5L, got.get().getId());
    }

    @Test
    @DisplayName("findByCinema: delegates to repo")
    void findByCinema_ok() {
        when(showtimeRepo.findByCinemaId(100L)).thenReturn(List.of(copy(base)));

        var list = service.findByCinema(100L);

        assertEquals(1, list.size());
        verify(showtimeRepo).findByCinemaId(100L);
    }

    @Test
    @DisplayName("searchByTitle: delegates to repo")
    void searchByTitle_ok() {
        when(showtimeRepo.findByMovieTitle("The Batman")).thenReturn(List.of(copy(base)));

        var list = service.searchByTitle("The Batman");

        assertEquals(1, list.size());
        verify(showtimeRepo).findByMovieTitle("The Batman");
    }

    // ---- helper ----
    private Showtime copy(Showtime src) {
        Showtime s = new Showtime();
        s.setId(src.getId());
        s.setMovieTitle(src.getMovieTitle());
        s.setScreenNumber(src.getScreenNumber());
        s.setStartTime(src.getStartTime());
        s.setEndTime(src.getEndTime());
        s.setTicketPrice(src.getTicketPrice());
        s.setLanguage(src.getLanguage());
        s.setFormat(src.getFormat());
        s.setCinema(src.getCinema());
        return s;
    }
}
