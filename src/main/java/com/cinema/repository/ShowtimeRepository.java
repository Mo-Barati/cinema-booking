package com.cinema.repository;

import com.cinema.entity.Showtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;


public interface ShowtimeRepository extends JpaRepository<Showtime, Long>, JpaSpecificationExecutor<Showtime> {

    // Basic finders
    List<Showtime> findByCinemaId(Long cinemaId);
    List<Showtime> findByMovieTitleContainingIgnoreCase(String query);
    List<Showtime> findByCinemaIdAndStartTimeBetween(Long cinemaId, LocalDateTime from, LocalDateTime to);

    // Useful for validation: detect overlapping showtimes on the same screen
    @Query("""
           SELECT CASE WHEN COUNT(s) > 0 THEN TRUE ELSE FALSE END
           FROM Showtime s
           WHERE s.cinema.id = :cinemaId
             AND s.screenNumber = :screenNumber
             AND s.startTime < :endTime
             AND s.endTime   > :startTime
           """)
    boolean existsOverlappingShowtime(@Param("cinemaId") Long cinemaId,
                                      @Param("screenNumber") int screenNumber,
                                      @Param("startTime") LocalDateTime startTime,
                                      @Param("endTime") LocalDateTime endTime);


    @Query("""
       SELECT CASE WHEN COUNT(s) > 0 THEN TRUE ELSE FALSE END
       FROM Showtime s
       WHERE s.cinema.id = :cinemaId
         AND s.screenNumber = :screenNumber
         AND s.startTime < :endTime
         AND s.endTime   > :startTime
         AND (:excludeId IS NULL OR s.id <> :excludeId)
       """)
    boolean existsOverlappingShowtimeExcludingId(@Param("cinemaId") Long cinemaId,
                                                 @Param("screenNumber") int screenNumber,
                                                 @Param("startTime") LocalDateTime startTime,
                                                 @Param("endTime") LocalDateTime endTime,
                                                 @Param("excludeId") Long excludeId);
}
