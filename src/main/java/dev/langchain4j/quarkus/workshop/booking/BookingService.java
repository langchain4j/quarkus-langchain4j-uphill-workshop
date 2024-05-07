package dev.langchain4j.quarkus.workshop.booking;

import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;

@ApplicationScoped
public class BookingService {

    public Booking getBookingDetails(String bookingNumber, String customerName, String customerSurname) {
        ensureExists(bookingNumber, customerName, customerSurname);

        // Imitating retrieval from DB
        LocalDate dateFrom = LocalDate.now().plusDays(1);
        LocalDate dateTo = LocalDate.now().plusDays(3);
        Customer customer = new Customer(customerName, customerSurname);
        return new Booking(bookingNumber, dateFrom, dateTo, customer);
    }

    public void cancelBooking(String bookingNumber, String customerName, String customerSurname) {
        ensureExists(bookingNumber, customerName, customerSurname);

        // Imitating cancellation
        throw new BookingCannotBeCancelledException(bookingNumber);
    }

    private void ensureExists(String bookingNumber, String customerName, String customerSurname) {
        // Imitating check
        if (!(bookingNumber.equals("123-456")
                && customerName.equals("Klaus")
                && customerSurname.equals("Heisler"))) {
            throw new BookingNotFoundException(bookingNumber);
        }
    }
}
