package com.cinema.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "tickets",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ticket_showtime_seat",
                columnNames = {"showtime_id", "seat_id"}
        )
)
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Which showtime this ticket is for
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "showtime_id", nullable = false)
    private Showtime showtime;

    // Which seat is booked
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    // Price actually paid – we can copy from showtime.ticketPrice when creating
    @Column(nullable = false)
    private double price;

    @Column(nullable = false, updatable = false)
    private LocalDateTime bookedAt;

    public Ticket() {
    }

    public Ticket(Showtime showtime, Seat seat, double price) {
        this.showtime = showtime;
        this.seat = seat;
        this.price = price;
    }

    @PrePersist
    protected void onCreate() {
        this.bookedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Showtime getShowtime() {
        return showtime;
    }

    public void setShowtime(Showtime showtime) {
        this.showtime = showtime;
    }

    public Seat getSeat() {
        return seat;
    }

    public void setSeat(Seat seat) {
        this.seat = seat;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public LocalDateTime getBookedAt() {
        return bookedAt;
    }
}
