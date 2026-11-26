package com.sahaj.parking;

import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainTest {
    @Test
    void calculatesFareUsingTheEnteredHolidayCalendar() {
        String input = String.join(
                System.lineSeparator(),
                "car",
                "2026-11-26T06:30",
                "2026-11-26T07:30",
                "",
                "2026-11-26");
        StringWriter output = new StringWriter();
        StringWriter error = new StringWriter();

        int exitCode = Main.run(
                new String[0],
                new StringReader(input),
                new PrintWriter(output),
                new PrintWriter(error));

        assertEquals(0, exitCode);
        assertTrue(output.toString().contains("Fare: $5.00"));
        assertTrue(output.toString().contains("Policy: STANDARD_HOURLY"));
        assertTrue(error.toString().isEmpty());
    }

    @Test
    void reportsInvalidSessionWithoutPrintingAFare() {
        String input = String.join(
                System.lineSeparator(),
                "CAR",
                "2026-11-26T10:00",
                "2026-11-26T09:00",
                "NONE",
                "",
                "");
        StringWriter output = new StringWriter();
        StringWriter error = new StringWriter();

        int exitCode = Main.run(
                new String[0],
                new StringReader(input),
                new PrintWriter(output),
                new PrintWriter(error));

        assertEquals(2, exitCode);
        assertTrue(error.toString().contains("Exit time must be after entry time."));
        assertFalse(output.toString().contains("Fare:"));
    }

    @Test
    void rejectsMalformedInputAndUnexpectedArguments() {
        StringWriter invalidInputOutput = new StringWriter();
        StringWriter invalidInputError = new StringWriter();
        int invalidInputExitCode = Main.run(
                new String[0],
                new StringReader("SCOOTER"),
                new PrintWriter(invalidInputOutput),
                new PrintWriter(invalidInputError));

        StringWriter argumentError = new StringWriter();
        int argumentExitCode = Main.run(
                new String[]{"--help"},
                new StringReader(""),
                new PrintWriter(new StringWriter()),
                new PrintWriter(argumentError));

        assertEquals(2, invalidInputExitCode);
        assertTrue(invalidInputError.toString().contains("Unsupported vehicle type"));
        assertEquals(2, argumentExitCode);
        assertTrue(argumentError.toString().contains("does not accept arguments"));
    }

    @Test
    void rejectsMalformedTimestampsAndHolidayDates() {
        StringWriter timestampError = new StringWriter();
        int timestampExitCode = Main.run(
                new String[0],
                new StringReader("CAR\nnot-a-timestamp"),
                new PrintWriter(new StringWriter()),
                new PrintWriter(timestampError));

        String holidayInput = String.join(
                System.lineSeparator(),
                "CAR",
                "2026-11-26T06:30",
                "2026-11-26T07:30",
                "NONE",
                "not-a-date");
        StringWriter holidayError = new StringWriter();
        int holidayExitCode = Main.run(
                new String[0],
                new StringReader(holidayInput),
                new PrintWriter(new StringWriter()),
                new PrintWriter(holidayError));

        assertEquals(2, timestampExitCode);
        assertTrue(timestampError.toString().contains("Invalid entry time"));
        assertEquals(2, holidayExitCode);
        assertTrue(holidayError.toString().contains("Invalid public holiday date"));
    }
}
