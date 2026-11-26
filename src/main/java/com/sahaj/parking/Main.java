package com.sahaj.parking;

import com.sahaj.parking.calendar.FixedPublicHolidayCalendar;
import com.sahaj.parking.domain.Fare;
import com.sahaj.parking.domain.LoyaltyTier;
import com.sahaj.parking.domain.ParkingSession;
import com.sahaj.parking.domain.ParkingTicket;
import com.sahaj.parking.domain.VehicleType;
import com.sahaj.parking.pricing.ParkingFeeCalculator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        int exitCode = run(
                args,
                new InputStreamReader(System.in, StandardCharsets.UTF_8),
                new PrintWriter(System.out, true),
                new PrintWriter(System.err, true));
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    static int run(String[] args, Reader input, PrintWriter output, PrintWriter error) {
        if (args.length > 0) {
            error.println("This application uses interactive prompts and does not accept arguments.");
            return 2;
        }

        BufferedReader reader = new BufferedReader(input);
        try {
            VehicleType vehicle = readEnum(
                    reader,
                    output,
                    "Vehicle type (MOTORCYCLE, CAR, BUS): ",
                    VehicleType.class,
                    "vehicle type");
            LocalDateTime entry = readDateTime(
                    reader,
                    output,
                    "Entry time (ISO local date-time, no timezone, e.g. 2026-11-26T06:30): ",
                    "entry time");
            LocalDateTime exit = readDateTime(
                    reader,
                    output,
                    "Exit time (ISO local date-time, no timezone, e.g. 2026-11-26T16:00): ",
                    "exit time");
            LoyaltyTier loyaltyTier = readEnum(
                    reader,
                    output,
                    "Loyalty tier (NONE, SILVER, GOLD, PLATINUM): ",
                    LoyaltyTier.class,
                    "loyalty tier");
            Set<LocalDate> holidays = readHolidays(reader, output);

            ParkingTicket ticket = new ParkingTicket(vehicle, entry, loyaltyTier);
            ParkingSession session;
            try {
                session = new ParkingSession(ticket, exit);
            } catch (IllegalArgumentException exception) {
                throw new InputException("Exit time must be after entry time.");
            }

            Fare fare = new ParkingFeeCalculator(new FixedPublicHolidayCalendar(holidays))
                    .calculate(session);
            output.printf(
                    Locale.ROOT,
                    "Fare: $%d.%02d%nPolicy: %s%n",
                    fare.amount().cents() / 100,
                    fare.amount().cents() % 100,
                    fare.selectedPolicy());
            output.flush();
            return 0;
        } catch (InputException exception) {
            error.println("Invalid input: " + exception.getMessage());
            error.flush();
            return 2;
        } catch (IOException exception) {
            error.println("Unable to read input: " + exception.getMessage());
            error.flush();
            return 1;
        }
    }

    private static <E extends Enum<E>> E readEnum(
            BufferedReader reader,
            PrintWriter output,
            String prompt,
            Class<E> enumType,
            String fieldName) throws IOException, InputException {
        String input = readLine(reader, output, prompt, fieldName);
        try {
            return Enum.valueOf(enumType, input.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new InputException("Unsupported " + fieldName + ": " + input);
        }
    }

    private static LocalDateTime readDateTime(
            BufferedReader reader,
            PrintWriter output,
            String prompt,
            String fieldName) throws IOException, InputException {
        String input = readLine(reader, output, prompt, fieldName);
        try {
            return LocalDateTime.parse(input.trim());
        } catch (DateTimeParseException exception) {
            throw new InputException(
                    "Invalid " + fieldName + "; use ISO local date-time, such as 2026-11-26T06:30.");
        }
    }

    private static Set<LocalDate> readHolidays(
            BufferedReader reader,
            PrintWriter output) throws IOException, InputException {
        String input = readLine(
                reader,
                output,
                "Public holidays (comma-separated ISO dates, or blank if none): ",
                "public holiday dates");
        if (input.isBlank()) {
            return Set.of();
        }

        try {
            return Arrays.stream(input.split(",", -1))
                    .map(String::trim)
                    .map(LocalDate::parse)
                    .collect(Collectors.toUnmodifiableSet());
        } catch (DateTimeParseException exception) {
            throw new InputException(
                    "Invalid public holiday date; use ISO dates such as 2026-12-25.");
        }
    }

    private static String readLine(
            BufferedReader reader,
            PrintWriter output,
            String prompt,
            String fieldName) throws IOException, InputException {
        output.print(prompt);
        output.flush();
        String input = reader.readLine();
        if (input == null) {
            throw new InputException("No value provided for " + fieldName + ".");
        }
        return input;
    }

    private static final class InputException extends Exception {
        private InputException(String message) {
            super(message);
        }
    }
}
