package com.telemedicina.appointment.model;

public enum AppointmentStatus {
    SCHEDULED,    // programata, asteapta sa inceapa
    IN_PROGRESS,  // consultatia e in desfasurare
    COMPLETED,    // finalizata de doctor
    CANCELLED,    // anulata
    NO_SHOW       // pacientul nu s-a prezentat
}