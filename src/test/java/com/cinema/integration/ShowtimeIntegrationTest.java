package com.cinema.integration;

import com.cinema.entity.Cinema;
import com.cinema.entity.Showtime;
import com.cinema.repository.CinemaRepository;
import com.cinema.repository.ShowtimeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional // roll back DB between tests
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ShowtimeIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private CinemaRepository cinemaRepo;
    @Autowired private ShowtimeRepository showtimeRepo;

    private final ObjectMapper om = new ObjectMapper().registerModule(new JavaTimeModule());

    private Cinema cinema;
    private Showtime existing;

    @BeforeEach
    void seed() {
        cinema = new Cinema();
        cinema.setName("Central Cinema");
        cinema.setAddressLine("123 Test St");
        cinema.setCity("Test City");
        cinema.setTotalScreens(5);
        cinema = cinemaRepo.save(cinema);

        existing = new Showtime();
        existing.setMovieTitle("Inception");
        existing.setScreenNumber(1);
        existing.setStartTime(LocalDateTime.of(2030, 1, 1, 19, 30));
        existing.setEndTime(LocalDateTime.of(2030, 1, 1, 22, 0));
        existing.setTicketPrice(12.50);
        existing.setLanguage("EN");
        existing.setFormat("2D");
        existing.setCinema(cinema);
        existing = showtimeRepo.save(existing);
    }

    // --- DTO mirrors your controller's inner class ---
    static class ShowtimeRequest {
        public String movieTitle;
        public int screenNumber;
        public LocalDateTime startTime;
        public LocalDateTime endTime;
        public double ticketPrice;
        public String language;
        public String format;
        public Long cinemaId;
    }

    private ShowtimeRequest req(String title, int screen, LocalDateTime from, LocalDateTime to, double price) {
        ShowtimeRequest r = new ShowtimeRequest();
        r.movieTitle = title;
        r.screenNumber = screen;
        r.startTime = from;
        r.endTime = to;
        r.ticketPrice = price;
        r.language = "EN";
        r.format = "2D";
        r.cinemaId = cinema.getId();
        return r;
    }

    private String json(Object o) throws Exception {
        return om.writeValueAsString(o);
    }

    @Test
    @DisplayName("POST /api/showtimes -> 201 Created + Location header")
    void create_showtime_201() throws Exception {
        var body = req("Dune", 2,
                LocalDateTime.of(2030,1,2,18,0),
                LocalDateTime.of(2030,1,2,20,30), 11.0);

        var mvcRes = mockMvc.perform(post("/api/showtimes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/showtimes/\\d+")))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.movieTitle").value("Dune"))
                .andReturn();

        // sanity: persisted
        assertThat(showtimeRepo.findByMovieTitle("Dune")).isNotEmpty();
    }

    @Test
    @DisplayName("GET /api/showtimes/{id} -> 200 OK")
    void get_showtime_200() throws Exception {
        mockMvc.perform(get("/api/showtimes/{id}", existing.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movieTitle").value("Inception"))
                .andExpect(jsonPath("$.screenNumber").value(1));
    }

    @Test
    @DisplayName("PUT /api/showtimes/{id} -> 200 OK updates fields")
    void update_showtime_200() throws Exception {
        var body = req("Inception (IMAX)", 1,
                LocalDateTime.of(2030,1,1,20,0),
                LocalDateTime.of(2030,1,1,22,30), 15.0);
        body.format = "IMAX";

        mockMvc.perform(put("/api/showtimes/{id}", existing.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.format").value("IMAX"))
                .andExpect(jsonPath("$.ticketPrice").value(15.0));
    }

    @Test
    @DisplayName("DELETE /api/showtimes/{id} -> 204 No Content, then GET -> 404")
    void delete_showtime_204() throws Exception {
        mockMvc.perform(delete("/api/showtimes/{id}", existing.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/showtimes/{id}", existing.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/showtimes/by-cinema/{id} -> 200 OK with list")
    void by_cinema_200() throws Exception {
        mockMvc.perform(get("/api/showtimes/by-cinema/{cid}", cinema.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].movieTitle").value("Inception"))
                .andExpect(jsonPath("$[0].cinema").doesNotExist()); // suppressed by @JsonBackReference
    }

    @Test
    @DisplayName("GET /api/showtimes/search?q=... -> 200 OK contains title")
    void search_200() throws Exception {
        mockMvc.perform(get("/api/showtimes/search").param("q", "Inception"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].movieTitle").value("Inception"));
    }

    @Test
    @DisplayName("GET /api/showtimes/window -> 200 OK within ISO window")
    void window_200() throws Exception {
        mockMvc.perform(get("/api/showtimes/window")
                        .param("cinemaId", String.valueOf(cinema.getId()))
                        .param("from", "2030-01-01T18:00:00")
                        .param("to",   "2030-01-01T23:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].movieTitle").value("Inception"));
    }

    @Test
    @DisplayName("POST overlap -> 409 Conflict from GlobalExceptionHandler")
    void create_overlap_409() throws Exception {
        // overlaps existing 19:30–22:00 on screen 1
        var overlap = req("Clash", 1,
                LocalDateTime.of(2030,1,1,21,0),
                LocalDateTime.of(2030,1,1,22,30), 9.5);

        mockMvc.perform(post("/api/showtimes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(overlap)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST invalid times -> 400 Bad Request")
    void create_bad_times_400() throws Exception {
        var bad = req("Bad", 2,
                LocalDateTime.of(2030,1,3,20,0),
                LocalDateTime.of(2030,1,3,19,0), 8.0);

        mockMvc.perform(post("/api/showtimes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(bad)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET missing id -> 404 Not Found")
    void get_404() throws Exception {
        mockMvc.perform(get("/api/showtimes/{id}", 999_999L))
                .andExpect(status().isNotFound());
    }
}
