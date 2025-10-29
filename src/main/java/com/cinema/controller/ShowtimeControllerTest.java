package com.cinema.controller;

import com.cinema.entity.Showtime;
import com.cinema.exception.BusinessRuleViolationException;
import com.cinema.exception.OverlappingShowtimeException;
import com.cinema.service.ShowtimeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;

import java.time.LocalDateTime;

import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Import;
import com.cinema.web.GlobalExceptionHandler;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ShowtimeController.class)
@Import(GlobalExceptionHandler.class)
public class ShowtimeControllerTest {

    private static final String BASE = "/api/showtimes";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ShowtimeService showtimeService;

    private ObjectMapper mapper;




    private Showtime sampleShowtime(Long id) {
        Showtime s = new Showtime();
        // if you added setId(Long) for tests, use it; otherwise reflection
        s.setId(id);
        s.setMovieTitle("Inception");
        s.setScreenNumber(3);
        s.setStartTime(LocalDateTime.of(2030, 1, 1, 19, 30));
        s.setEndTime(LocalDateTime.of(2030, 1, 1, 22, 0));
        s.setTicketPrice(12.50);
        s.setLanguage("EN");
        s.setFormat("2D");
        return s;
    }

    @BeforeEach
    void setup() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
    }

    // ---------- CREATE ----------
    @Test
    @DisplayName("POST /api/showtimes -> 201 Created")
    void create_created() throws Exception {
        Showtime created = sampleShowtime(10L);
        Mockito.when(showtimeService.create(any())).thenReturn(created);

        String body = """
        {
          "movieTitle": "Inception",
          "screenNumber": 3,
          "startTime": "2030-01-01T19:30:00",
          "endTime": "2030-01-01T22:00:00",
          "ticketPrice": 12.50,
          "language": "EN",
          "format": "2D",
          "cinemaId": 1
        }
        """;

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString(BASE + "/10")))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.movieTitle").value("Inception"));
    }

    @Test
    @DisplayName("POST /api/showtimes -> 409 Conflict on overlapping show")
    void create_overlap() throws Exception {
        Mockito.when(showtimeService.create(any()))
                .thenThrow(new OverlappingShowtimeException("Overlaps with existing showtime"));

        String body = """
        {
          "movieTitle": "Inception",
          "screenNumber": 3,
          "startTime": "2030-01-01T19:30:00",
          "endTime": "2030-01-01T22:00:00",
          "ticketPrice": 12.50,
          "language": "EN",
          "format": "2D",
          "cinemaId": 1
        }
        """;

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", containsString("Overlap")));
    }

    @Test
    @DisplayName("POST /api/showtimes -> 400 Bad Request for business rule violation")
    void create_businessRule() throws Exception {
        Mockito.when(showtimeService.create(any()))
                .thenThrow(new BusinessRuleViolationException("End time must be after start time"));

        String body = """
        {
          "movieTitle": "Inception",
          "screenNumber": 3,
          "startTime": "2030-01-01T22:00:00",
          "endTime": "2030-01-01T19:30:00",
          "ticketPrice": 12.50,
          "language": "EN",
          "format": "2D",
          "cinemaId": 1
        }
        """;

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("End time must be after start time")));
    }

    // ---------- READ ----------
    @Test
    @DisplayName("GET /api/showtimes/{id} -> 200 OK")
    void getById_ok() throws Exception {
        Mockito.when(showtimeService.findById(10L))
                .thenReturn(Optional.of(sampleShowtime(10L)));

        mockMvc.perform(get(BASE + "/{id}", 10))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.movieTitle").value("Inception"));
    }

    @Test
    @DisplayName("GET /api/showtimes/{id} -> 404 Not Found")
    void getById_notFound() throws Exception {
        Mockito.when(showtimeService.findById(999L))
                .thenReturn(Optional.empty());

        mockMvc.perform(get(BASE + "/{id}", 999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", containsString("not found")));
    }

    @Test
    @DisplayName("GET /api/showtimes/by-cinema/{cinemaId} -> 200 OK")
    void getByCinema_ok() throws Exception {
        Mockito.when(showtimeService.findByCinema(1L))
                .thenReturn(List.of(sampleShowtime(1L), sampleShowtime(2L)));

        mockMvc.perform(get(BASE + "/by-cinema/{cinemaId}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("GET /api/showtimes/search?movieTitle=Inception -> 200 OK")
    void search_ok() throws Exception {
        Mockito.when(showtimeService.searchByTitle("Inception"))
                .thenReturn(List.of(sampleShowtime(5L)));

        mockMvc.perform(get(BASE + "/search").param("movieTitle", "Inception"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[0].movieTitle").value("Inception"));
    }

    @Test
    @DisplayName("GET /api/showtimes/window?cinemaId=1&from=..&to=.. -> 200 OK")
    void window_ok() throws Exception {
        LocalDateTime from = LocalDateTime.of(2030, 1, 1, 0, 0);
        LocalDateTime to   = LocalDateTime.of(2030, 1, 2, 0, 0);

        Mockito.when(showtimeService.findInWindow(1L, from, to))
                .thenReturn(List.of(sampleShowtime(7L)));

        mockMvc.perform(get(BASE + "/window")
                        .param("cinemaId", "1")
                        .param("from", "2030-01-01T00:00:00")
                        .param("to",   "2030-01-02T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7));
    }

    // ---------- UPDATE ----------
    @Test
    @DisplayName("PUT /api/showtimes/{id} -> 200 OK")
    void update_ok() throws Exception {
        Showtime updated = sampleShowtime(10L);
        updated.setFormat("IMAX");
        Mockito.when(showtimeService.update(eq(10L), any()))
                .thenReturn(updated);

        String body = """
        {
          "movieTitle": "Inception",
          "screenNumber": 3,
          "startTime": "2030-01-01T19:30:00",
          "endTime": "2030-01-01T22:00:00",
          "ticketPrice": 12.50,
          "language": "EN",
          "format": "IMAX",
          "cinemaId": 1
        }
        """;

        mockMvc.perform(put(BASE + "/{id}", 10)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.format").value("IMAX"));
    }

    // ---------- DELETE ----------
    @Test
    @DisplayName("DELETE /api/showtimes/{id} -> 204 No Content")
    void delete_noContent() throws Exception {
        Mockito.when(showtimeService.delete(10L)).thenReturn(true);

        mockMvc.perform(delete(BASE + "/{id}", 10))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/showtimes/{id} -> 404 Not Found when service returns false")
    void delete_notFound() throws Exception {
        Mockito.when(showtimeService.delete(999L)).thenReturn(false);

        mockMvc.perform(delete(BASE + "/{id}", 999))
                .andExpect(status().isNotFound());
    }
}
