package com.telemedicina.shared.util;

import java.time.LocalDate;
import java.time.Period;

public class DateUtils {

    private DateUtils() {}

    public static int calculateAge(LocalDate birthDate) {
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    public static String getAgeCategory(LocalDate birthDate) {
        int age = calculateAge(birthDate);
        if (age < 18) return "CHILD";
        if (age <= 65) return "ADULT";
        return "SENIOR";
    }
}