package com.cinema.repository;

import com.cinema.entity.Cinema;
import com.cinema.entity.Showtime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class ShowtimeRepositoryTest {

    @Autowired private TestEntityManager em;
    @Autowired private ShowtimeRepository repo;

    private Cinema cinema(String name) {
        Cinema c = new Cinema();
        c.setName(name);
        c.setAddressLine("123 Test St");  // required
        c.setCity("Test City");           // required
        c.setTotalScreens(5);             // required: 1..50

        // Optional fields (only if you want):
        c.setStateOrProvince("Testshire");
        c.setPostcode("T35 7AA");
        c.setCountry("UK");
        c.setPhone("+1 555 0000");
        c.setEmail("boxoffice@test.example");

        return em.persistAndFlush(c);
    }

    private Showtime show(Cinema c, String title, int screen, LocalDateTime start, LocalDateTime end) {
        Showtime s = new Showtime();
        s.setMovieTitle(title);
        s.setScreenNumber(screen);
        s.setStartTime(start);
        s.setEndTime(end);
        s.setTicketPrice(10.0);
        s.setLanguage("EN");
        s.setFormat("2D");
        s.setCinema(c);
        return em.persist(s);
    }

    @Test
    @DisplayName("findByCinemaId returns only that cinema's showtimes")
    void findByCinemaId_ok() {
        Cinema a = cinema("A");
        Cinema b = cinema("B");

        show(a, "Inception", 1, t(19, 0), t(21, 30));
        show(a, "Dune",      1, t(22, 0), t(23, 59));
        show(b, "Inception", 1, t(19, 0), t(21, 30));
        em.flush();

        List<Showtime> forA = repo.findByCinema_Id(a.getId());

        assertThat(forA).hasSize(2).allMatch(s -> s.getCinema().getId().equals(a.getId()));
    }

    @Test
    @DisplayName("findByMovieTitleContainingIgnoreCase matches case-insensitively")
    void findByMovieTitleContainingIgnoreCase_ok() {
        Cinema c = cinema("C");
        show(c, "The Batman", 1, t(18, 0), t(20, 30));
        show(c, "BATMAN Begins", 2, t(21, 0), t(23, 0));
        em.flush();

        List<Showtime> hits = repo.findByMovieTitleIgnoreCaseContaining("batman");
        assertThat(hits).hasSize(2);
    }

    @Test
    @DisplayName("findByMovieTitle exact match")
    void findByMovieTitle_exact() {
        Cinema c = cinema("C2");
        show(c, "Oppenheimer", 1, t(14, 0), t(17, 0));
        show(c, "Oppenheimer Extended", 2, t(18, 0), t(21, 0));
        em.flush();

        List<Showtime> hits = repo.findByMovieTitle("Oppenheimer");
        assertThat(hits).hasSize(1).allMatch(s -> s.getMovieTitle().equals("Oppenheimer"));
    }

    @Test
    @DisplayName("findByCinemaIdAndStartTimeBetween returns only shows starting within window")
    void findByCinemaIdAndStartTimeBetween_ok() {
        Cinema c = cinema("D");
        show(c, "Early",  1, dt(2030,1,1,  8,0), dt(2030,1,1,10,0));
        show(c, "Inside", 1, dt(2030,1,1, 12,0), dt(2030,1,1,14,0));
        show(c, "Late",   1, dt(2030,1,1, 18,0), dt(2030,1,1,20,0));
        em.flush();

        List<Showtime> hits = repo.findByCinema_IdAndStartTimeBetween(
                c.getId(),
                LocalDateTime.of(2030, 1, 1, 11, 0),
                LocalDateTime.of(2030, 1, 1, 16, 0)
        );


        assertThat(hits).extracting(Showtime::getMovieTitle).containsExactly("Inside");
    }

    @Test
    @DisplayName("existsOverlappingShowtime detects overlap on same screen")
    void existsOverlappingShowtime_trueWhenOverlap() {
        Cinema c = cinema("E");
        // Base show: 12:00–14:00 on screen 3
        show(c, "Base", 3, t(12, 0), t(14, 0));
        em.flush();

        boolean clash = repo.existsOverlappingShowtime(
                c.getId(), 3, t(13, 0), t(15, 0)); // overlaps 13:00–14:00
        assertThat(clash).isTrue();
    }

    @Test
    @DisplayName("existsOverlappingShowtime is false on different screen")
    void existsOverlappingShowtime_falseDifferentScreen() {
        Cinema c = cinema("F");
        show(c, "Base", 1, t(12, 0), t(14, 0));
        em.flush();

        boolean clash = repo.existsOverlappingShowtime(
                c.getId(), 2, t(13, 0), t(15, 0)); // different screen
        assertThat(clash).isFalse();
    }

    @Test
    @DisplayName("existsOverlappingShowtimeExcludingId ignores the excluded showtime")
    void existsOverlappingShowtimeExcludingId_ignoresSelf() {
        Cinema c = cinema("G");
        Showtime self = show(c, "Self", 2, t(10, 0), t(12, 0));
        em.flush();

        boolean clash = repo.existsOverlappingShowtimeExcludingId(
                c.getId(), 2, t(11, 0), t(11, 30), self.getId());
        assertThat(clash).isFalse(); // same slot but excluded -> should be ignored
    }

    // ---- helpers ----
    private LocalDateTime today(int hour, int minute) {
        LocalDateTime now = LocalDateTime.now();
        return LocalDateTime.of(now.getYear(), now.getMonth(), now.getDayOfMonth(), hour, minute);
    }
    private LocalDateTime t(int h, int m) { return today(h, m); }
    private LocalDateTime dt(int y, int mo, int d, int h, int mi) {
        return LocalDateTime.of(y, mo, d, h, mi);
    }
}
