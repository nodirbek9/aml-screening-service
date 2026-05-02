package aml.code.screeningservice.entity;

import aml.code.screeningservice.entity.enums.ClientStatus;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "clients")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    private LocalDate birthday;

    @Column(nullable = false)
    private String passwordNumber;

    private String inn;

    private String phone;

    @Column(unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    private ClientStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
