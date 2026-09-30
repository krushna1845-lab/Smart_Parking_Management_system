import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/* ===================================================================
   SMART PARKING MANAGEMENT SYSTEM
   Core Java + OOP. No database, no framework, no GUI.

   OOP used here
     Inheritance    Car / Bike / Truck  IS-A  Vehicle
     Abstraction    abstract Vehicle, interface Payment
     Encapsulation  slot.occupy(), ticket.markPaid()
     Composition    ParkingLot HAS-A slots and tickets
     Polymorphism   getVehicleType(), getHourlyRate(), pay()
     Overloading    findVehicle(...) with 1 and 2 arguments
     static         ticket id generation
     final          fixed parking rules in ParkingLot
     Exceptions     invalid vehicle, lot full, invalid ticket
   =================================================================== */


/* --------------------------- SLOT TYPES --------------------------- */

enum SlotType {
    BIKE("B", "Bike"),
    CAR("C", "Car"),
    LARGE("L", "Large");

    private final String code;
    private final String label;

    SlotType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }
}


/* ------------------------ CUSTOM EXCEPTIONS ----------------------- */

class ParkingException extends Exception {
    public ParkingException(String message) {
        super(message);
    }
}

class InvalidVehicleException extends ParkingException {
    public InvalidVehicleException(String message) {
        super(message);
    }
}

class ParkingFullException extends ParkingException {
    public ParkingFullException(String message) {
        super(message);
    }
}

class InvalidTicketException extends ParkingException {
    public InvalidTicketException(String message) {
        super(message);
    }
}


/* ----------------------------- VEHICLE ---------------------------- */
/* Car / Bike / Truck extend this class. Each one is free to behave    */
/* differently - that is polymorphism.                                 */

abstract class Vehicle {

    private final String registrationNumber;
    private final String ownerName;
    private final double hourlyRate;

    public Vehicle(String registrationNumber, String ownerName, double hourlyRate)
            throws InvalidVehicleException {

        if (registrationNumber == null || registrationNumber.trim().length() < 6) {
            throw new InvalidVehicleException("Registration number must be at least 6 characters.");
        }
        if (ownerName == null || ownerName.trim().isEmpty()) {
            throw new InvalidVehicleException("Owner name cannot be empty.");
        }
        if (hourlyRate <= 0) {
            throw new InvalidVehicleException("Hourly rate must be greater than 0.");
        }

        this.registrationNumber = registrationNumber.trim().toUpperCase();
        this.ownerName = ownerName.trim();
        this.hourlyRate = hourlyRate;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public double getHourlyRate() {
        return hourlyRate;
    }

    public abstract String getVehicleType();

    public abstract SlotType getRequiredSlot();

    @Override
    public String toString() {
        return getVehicleType() + " [" + registrationNumber + "] owned by " + ownerName;
    }
}

class Bike extends Vehicle {

    public Bike(String registrationNumber, String ownerName) throws InvalidVehicleException {
        super(registrationNumber, ownerName, 20);
    }

    @Override
    public String getVehicleType() {
        return "Bike";
    }

    @Override
    public SlotType getRequiredSlot() {
        return SlotType.BIKE;
    }
}

class Car extends Vehicle {

    public Car(String registrationNumber, String ownerName) throws InvalidVehicleException {
        super(registrationNumber, ownerName, 40);
    }

    @Override
    public String getVehicleType() {
        return "Car";
    }

    @Override
    public SlotType getRequiredSlot() {
        return SlotType.CAR;
    }
}

class Truck extends Vehicle {

    public Truck(String registrationNumber, String ownerName) throws InvalidVehicleException {
        super(registrationNumber, ownerName, 80);
    }

    @Override
    public String getVehicleType() {
        return "Truck";
    }

    @Override
    public SlotType getRequiredSlot() {
        return SlotType.LARGE;
    }
}


/* -------------------------- PARKING SLOT -------------------------- */
/* Encapsulation: nobody sets "occupied" directly. They call occupy()  */
/* or release(), and the slot decides if the change is allowed.         */

class ParkingSlot {

    private final String slotId;
    private final SlotType type;
    private Vehicle vehicle;

    public ParkingSlot(SlotType type, int number) {
        this.type = type;
        this.slotId = type.getCode() + "-" + String.format("%02d", number);
    }

    public String getSlotId() {
        return slotId;
    }

    public SlotType getType() {
        return type;
    }

    public boolean isFree() {
        return vehicle == null;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void occupy(Vehicle incoming) throws InvalidVehicleException {
        if (incoming.getRequiredSlot() != type) {
            throw new InvalidVehicleException(incoming.getVehicleType() + " cannot use slot " + slotId + ".");
        }
        if (!isFree()) {
            throw new InvalidVehicleException("Slot " + slotId + " is already occupied.");
        }
        this.vehicle = incoming;
    }

    public void release() {
        this.vehicle = null;
    }

    @Override
    public String toString() {
        return slotId + " [" + type.getLabel() + "] -> "
                + (isFree() ? "AVAILABLE" : vehicle.getVehicleType() + " " + vehicle.getRegistrationNumber());
    }
}


/* ------------------------- PARKING TICKET ------------------------- */

class ParkingTicket {

    /* static - every ticket gets a unique id, no central service needed */
    private static int lastTicketNumber = 1000;

    private final String ticketId;
    private final Vehicle vehicle;
    private final ParkingSlot slot;
    private final LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private double amountDue;

    public ParkingTicket(Vehicle vehicle, ParkingSlot slot, LocalDateTime entryTime) {
        this.ticketId = "T" + (++lastTicketNumber);
        this.vehicle = vehicle;
        this.slot = slot;
        this.entryTime = entryTime;
    }

    public String getTicketId() {
        return ticketId;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public ParkingSlot getSlot() {
        return slot;
    }

    public LocalDateTime getEntryTime() {
        return entryTime;
    }

    public LocalDateTime getExitTime() {
        return exitTime;
    }

    public double getAmountDue() {
        return amountDue;
    }

    public boolean isClosed() {
        return exitTime != null;
    }

    public Duration getDuration() {
        return Duration.between(entryTime, exitTime != null ? exitTime : LocalDateTime.now());
    }

    /* Polymorphism: we never ask "is this a Truck?".                    */
    /* The vehicle itself tells us what to charge.                       */
    public double calculateFee() {
        long minutes = getDuration().toMinutes() - ParkingLot.GRACE_PERIOD_MINUTES;
        if (minutes <= 0) {
            amountDue = 0;
        } else {
            long hours = (minutes + 59) / 60;
            amountDue = hours * vehicle.getHourlyRate();
        }
        return amountDue;
    }

    public void close(LocalDateTime time) {
        this.exitTime = time;
        if (amountDue == 0) {
            calculateFee();
        }
    }
}


/* ----------------------------- PAYMENT ---------------------------- */
/* The lot only knows that it can pay. It does not care how.          */

interface Payment {

    boolean pay(double amount);

    String getMethodName();
}

class CashPayment implements Payment {

    @Override
    public boolean pay(double amount) {
        System.out.println("   Collected Rs " + (long) amount + " in cash.");
        return true;
    }

    @Override
    public String getMethodName() {
        return "Cash";
    }
}

class CardPayment implements Payment {

    private final String cardHolder;

    public CardPayment(String cardHolder) {
        this.cardHolder = cardHolder;
    }

    @Override
    public boolean pay(double amount) {
        System.out.println("   Swiped " + cardHolder + "'s card for Rs " + (long) amount + ".");
        return true;
    }

    @Override
    public String getMethodName() {
        return "Card";
    }
}

class UPIPayment implements Payment {

    private final String upiId;

    public UPIPayment(String upiId) {
        this.upiId = upiId;
    }

    @Override
    public boolean pay(double amount) {
        System.out.println("   Payment request sent to " + upiId + " for Rs " + (long) amount + ".");
        return true;
    }

    @Override
    public String getMethodName() {
        return "UPI";
    }
}


/* -------------------------- PARKING LOT --------------------------- */
/* Composition - the lot HAS-A slots and tickets. It does not           */
/* inherit them.                                                       */

class ParkingLot {

    /* final - fixed rules of this parking lot */
    public static final int BIKE_SLOTS = 5;
    public static final int CAR_SLOTS = 8;
    public static final int LARGE_SLOTS = 3;
    public static final int GRACE_PERIOD_MINUTES = 15;

    private final List<ParkingSlot> slots = new ArrayList<>();
    private final Map<String, ParkingTicket> activeTickets = new LinkedHashMap<>();
    private final Map<String, ParkingTicket> closedTickets = new LinkedHashMap<>();

    public ParkingLot() {
        addSlots(SlotType.BIKE, BIKE_SLOTS);
        addSlots(SlotType.CAR, CAR_SLOTS);
        addSlots(SlotType.LARGE, LARGE_SLOTS);
    }

    private void addSlots(SlotType type, int count) {
        for (int i = 1; i <= count; i++) {
            slots.add(new ParkingSlot(type, i));
        }
    }

    /* -------------------------- park / exit ---------------------- */

    public ParkingTicket park(Vehicle vehicle) throws ParkingException {
        if (vehicle == null) {
            throw new InvalidVehicleException("Vehicle cannot be null.");
        }
        if (findVehicle(vehicle.getRegistrationNumber()) != null) {
            throw new InvalidVehicleException(vehicle.getRegistrationNumber() + " is already parked.");
        }

        ParkingSlot slot = findFreeSlot(vehicle);
        if (slot == null) {
            throw new ParkingFullException("No " + vehicle.getRequiredSlot().getLabel().toLowerCase()
                    + " slot is free right now.");
        }
        slot.occupy(vehicle);

        ParkingTicket ticket = new ParkingTicket(vehicle, slot, LocalDateTime.now());
        activeTickets.put(ticket.getTicketId(), ticket);
        return ticket;
    }

    public ParkingSlot findFreeSlot(Vehicle vehicle) {
        for (ParkingSlot slot : slots) {
            if (slot.isFree() && slot.getType() == vehicle.getRequiredSlot()) {
                return slot;
            }
        }
        return null;
    }

    /* Pay first, then free the slot. If payment fails the vehicle     */
    /* stays parked, exactly like a real system.                        */
    public ParkingTicket remove(String ticketId, Payment payment) throws ParkingException {
        ParkingTicket ticket = getActiveTicket(ticketId);
        if (payment == null) {
            throw new ParkingException("Choose a payment method first.");
        }

        double fee = ticket.calculateFee();
        if (!payment.pay(fee)) {
            throw new ParkingException("Payment declined by " + payment.getMethodName() + ".");
        }

        ticket.close(LocalDateTime.now());
        ticket.getSlot().release();
        activeTickets.remove(ticketId.toUpperCase());
        closedTickets.put(ticket.getTicketId(), ticket);
        return ticket;
    }

    /* --------------------------- lookups ------------------------- */

    public ParkingTicket getActiveTicket(String ticketId) throws InvalidTicketException {
        String key = ticketId == null ? "" : ticketId.trim().toUpperCase();
        ParkingTicket ticket = activeTickets.get(key);
        if (ticket == null) {
            throw new InvalidTicketException("Ticket " + ticketId + " does not exist.");
        }
        return ticket;
    }

    /* Overloading - two ways to search for a parked vehicle. */
    public ParkingTicket findVehicle(String registrationNumber) {
        if (registrationNumber == null) {
            return null;
        }
        String key = registrationNumber.trim().toUpperCase();
        for (ParkingTicket ticket : activeTickets.values()) {
            if (ticket.getVehicle().getRegistrationNumber().equals(key)) {
                return ticket;
            }
        }
        return null;
    }

    public List<ParkingTicket> findVehicle(String registrationNumber, SlotType type) {
        List<ParkingTicket> matches = new ArrayList<>();
        for (ParkingTicket ticket : activeTickets.values()) {
            if (ticket.getVehicle().getRequiredSlot() == type
                    && ticket.getVehicle().getRegistrationNumber()
                            .toUpperCase().contains(registrationNumber.toUpperCase().trim())) {
                matches.add(ticket);
            }
        }
        return matches;
    }

    public List<ParkingSlot> getAvailableSlots() {
        List<ParkingSlot> free = new ArrayList<>();
        for (ParkingSlot slot : slots) {
            if (slot.isFree()) {
                free.add(slot);
            }
        }
        return free;
    }

    public int getAvailableCount(SlotType type) {
        int count = 0;
        for (ParkingSlot slot : slots) {
            if (slot.isFree() && slot.getType() == type) {
                count++;
            }
        }
        return count;
    }

    public List<ParkingTicket> getActiveTickets() {
        return new ArrayList<>(activeTickets.values());
    }

    public List<ParkingTicket> getClosedTickets() {
        return new ArrayList<>(closedTickets.values());
    }
}


/* ------------------------------ APP ------------------------------- */

public class Smart_Parking_Management_System {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private final ParkingLot lot = new ParkingLot();
    private final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        Smart_Parking_Management_System app = new Smart_Parking_Management_System();
        app.showWelcome();
        app.run();
    }

    private void showWelcome() {
        System.out.println();
        System.out.println("==============================================");
        System.out.println("       SMART PARKING SYSTEM");
        System.out.println("==============================================");
        System.out.println("Slots: " + (ParkingLot.BIKE_SLOTS + ParkingLot.CAR_SLOTS + ParkingLot.LARGE_SLOTS)
                + "  (" + ParkingLot.BIKE_SLOTS + " bike, " + ParkingLot.CAR_SLOTS + " car, "
                + ParkingLot.LARGE_SLOTS + " large)");
        System.out.println("Free grace period: " + ParkingLot.GRACE_PERIOD_MINUTES + " minutes");
    }

    private void run() {
        boolean running = true;
        while (running) {
            printMenu();
            int choice = readInt("Enter choice: ");
            switch (choice) {
                case 1 -> parkVehicle();
                case 2 -> removeVehicle();
                case 3 -> findVehicle();
                case 4 -> viewAvailableSlots();
                case 5 -> viewParkedVehicles();
                case 6 -> calculateFee();
                case 7 -> makePayment();
                case 8 -> {
                    running = false;
                    System.out.println("Parking lot closed. Goodbye!");
                }
                default -> System.out.println("Invalid option. Please choose 1 to 8.");
            }
        }
        scanner.close();
    }

    private void printMenu() {
        System.out.println();
        System.out.println("1. Park Vehicle");
        System.out.println("2. Remove Vehicle");
        System.out.println("3. Find Vehicle");
        System.out.println("4. View Available Slots");
        System.out.println("5. View Parked Vehicles");
        System.out.println("6. Calculate Parking Fee");
        System.out.println("7. Make Payment");
        System.out.println("8. Exit");
    }

    /* -------------------------- 1. park -------------------------- */

    private void parkVehicle() {
        try {
            Vehicle vehicle = readVehicle();
            ParkingTicket ticket = lot.park(vehicle);

            System.out.println();
            System.out.println("==============================================");
            System.out.println("  VEHICLE PARKED SUCCESSFULLY");
            System.out.println("==============================================");
            System.out.println("Vehicle      : " + vehicle.getVehicleType());
            System.out.println("Registration : " + vehicle.getRegistrationNumber());
            System.out.println("Owner        : " + vehicle.getOwnerName());
            System.out.println("Slot         : " + ticket.getSlot().getSlotId());
            System.out.println("Ticket ID    : " + ticket.getTicketId());
            System.out.println("Entry Time   : " + format(ticket.getEntryTime()));
            System.out.println("Rate         : Rs " + (long) vehicle.getHourlyRate() + " per hour");
        } catch (ParkingException e) {
            System.out.println("Could not park: " + e.getMessage());
        }
    }

    private Vehicle readVehicle() throws InvalidVehicleException {
        System.out.println();
        System.out.println("1. Bike  (Rs 20/hour)");
        System.out.println("2. Car   (Rs 40/hour)");
        System.out.println("3. Truck (Rs 80/hour)");
        int type = readInt("Enter vehicle type: ");
        String registration = readText("Registration Number: ");
        String owner = readText("Owner Name: ");

        return switch (type) {
            case 1 -> new Bike(registration, owner);
            case 2 -> new Car(registration, owner);
            case 3 -> new Truck(registration, owner);
            default -> throw new InvalidVehicleException("Unknown vehicle type: " + type);
        };
    }

    /* ------------------------- 2. remove ------------------------- */

    private void removeVehicle() {
        String ticketId = readText("Enter Ticket ID: ");
        try {
            ParkingTicket ticket = lot.getActiveTicket(ticketId);
            Duration parked = ticket.getDuration();
            System.out.println();
            System.out.println("Vehicle   : " + ticket.getVehicle().getVehicleType() + " "
                    + ticket.getVehicle().getRegistrationNumber());
            System.out.println("Owner     : " + ticket.getVehicle().getOwnerName());
            System.out.println("Slot      : " + ticket.getSlot().getSlotId());
            System.out.println("Parked    : " + format(ticket.getEntryTime()));
            System.out.println("Parked for: " + parked.toHours() + "h " + parked.toMinutesPart() + "m");
            System.out.println("Use option 7 to pay and free the slot.");
        } catch (ParkingException e) {
            System.out.println("Cannot remove: " + e.getMessage());
        }
    }

    /* -------------------------- 3. find -------------------------- */

    private void findVehicle() {
        String registration = readText("Enter Registration Number: ");
        ParkingTicket exact = lot.findVehicle(registration);
        if (exact != null) {
            System.out.println("Found:");
            System.out.println("  " + exact.getTicketId() + " | " + exact.getSlot().getSlotId() + " | "
                    + exact.getVehicle().getVehicleType() + " " + exact.getVehicle().getRegistrationNumber()
                    + " | owner " + exact.getVehicle().getOwnerName());
            return;
        }

        System.out.println("No exact match. Search all parked vehicles of a type instead?");
        System.out.println("1. Bike   2. Car   3. Truck");
        SlotType type = switch (readInt("Enter type: ")) {
            case 1 -> SlotType.BIKE;
            case 2 -> SlotType.CAR;
            case 3 -> SlotType.LARGE;
            default -> null;
        };

        List<ParkingTicket> matches = type == null ? List.of() : lot.findVehicle(registration, type);
        if (matches.isEmpty()) {
            System.out.println("No parked vehicle found for " + registration + ".");
        } else {
            for (ParkingTicket ticket : matches) {
                System.out.println("  " + ticket.getTicketId() + " | " + ticket.getSlot().getSlotId() + " | "
                        + ticket.getVehicle().getRegistrationNumber() + " | owner "
                        + ticket.getVehicle().getOwnerName());
            }
        }
    }

    /* ------------------------- 4. slots -------------------------- */

    private void viewAvailableSlots() {
        List<ParkingSlot> free = lot.getAvailableSlots();
        System.out.println();
        System.out.println("Free: " + lot.getAvailableCount(SlotType.BIKE) + " bike, "
                + lot.getAvailableCount(SlotType.CAR) + " car, "
                + lot.getAvailableCount(SlotType.LARGE) + " large");
        if (free.isEmpty()) {
            System.out.println("Parking lot is full.");
            return;
        }
        for (ParkingSlot slot : free) {
            System.out.println("  " + slot);
        }
    }

    /* ------------------------ 5. parked -------------------------- */

    private void viewParkedVehicles() {
        List<ParkingTicket> active = lot.getActiveTickets();
        System.out.println();
        if (active.isEmpty()) {
            System.out.println("No vehicles parked.");
            return;
        }
        System.out.printf("%-8s %-7s %-16s %-18s %s%n", "TICKET", "SLOT", "REGISTRATION", "OWNER", "PARKED FOR");
        for (ParkingTicket ticket : active) {
            Duration parked = ticket.getDuration();
            System.out.printf("%-8s %-7s %-16s %-18s %dh %02dm%n",
                    ticket.getTicketId(),
                    ticket.getSlot().getSlotId(),
                    ticket.getVehicle().getRegistrationNumber(),
                    ticket.getVehicle().getOwnerName(),
                    parked.toHours(),
                    parked.toMinutesPart());
        }
    }

    /* -------------------------- 6. fee --------------------------- */

    private void calculateFee() {
        String ticketId = readText("Enter Ticket ID: ");
        try {
            ParkingTicket ticket = lot.getActiveTicket(ticketId);
            Duration parked = ticket.getDuration();
            System.out.println();
            System.out.println("Ticket      : " + ticket.getTicketId());
            System.out.println("Vehicle     : " + ticket.getVehicle().getVehicleType() + " "
                    + ticket.getVehicle().getRegistrationNumber());
            System.out.println("Entry Time  : " + format(ticket.getEntryTime()));
            System.out.println("Duration    : " + parked.toHours() + "h " + parked.toMinutesPart() + "m");
            System.out.println("Grace       : " + ParkingLot.GRACE_PERIOD_MINUTES + " min free");
            System.out.println("Rate        : Rs " + (long) ticket.getVehicle().getHourlyRate() + " per hour");
            System.out.println("Parking Fee : Rs " + (long) ticket.calculateFee());
        } catch (ParkingException e) {
            System.out.println("Cannot calculate fee: " + e.getMessage());
        }
    }

    /* ------------------------ 7. payment ------------------------- */

    private void makePayment() {
        String ticketId = readText("Enter Ticket ID: ");
        try {
            ParkingTicket ticket = lot.getActiveTicket(ticketId);
            System.out.println("Amount due: Rs " + (long) ticket.calculateFee());
            System.out.println("1. Cash");
            System.out.println("2. Card");
            System.out.println("3. UPI");
            int method = readInt("Select payment method: ");
            Payment payment = switch (method) {
                case 1 -> new CashPayment();
                case 2 -> new CardPayment(readText("Card holder name: "));
                case 3 -> new UPIPayment(readText("UPI ID: "));
                default -> null;
            };
            if (payment == null) {
                System.out.println("Invalid payment method.");
                return;
            }

            ParkingTicket closed = lot.remove(ticketId, payment);
            Duration parked = closed.getDuration();
            System.out.println();
            System.out.println("==============================================");
            System.out.println("  PAYMENT SUCCESSFUL - THANK YOU");
            System.out.println("==============================================");
            System.out.println("Ticket     : " + closed.getTicketId());
            System.out.println("Paid with  : " + payment.getMethodName());
            System.out.println("Vehicle    : " + closed.getVehicle().getVehicleType() + " "
                    + closed.getVehicle().getRegistrationNumber());
            System.out.println("Parked from: " + format(closed.getEntryTime()));
            System.out.println("Parked to  : " + format(closed.getExitTime()));
            System.out.println("Duration   : " + parked.toHours() + "h " + parked.toMinutesPart() + "m");
            System.out.println("Total paid : Rs " + (long) closed.getAmountDue());
            System.out.println("Slot " + closed.getSlot().getSlotId() + " is free again.");
        } catch (ParkingException e) {
            System.out.println("Checkout failed: " + e.getMessage());
            System.out.println("The vehicle is still parked. Try another payment method.");
        }
    }

    /* ------------------------ input helpers ---------------------- */

    private String format(LocalDateTime time) {
        return time.format(TIME);
    }

    private int readInt(String prompt) {
        while (true) {
            String line = readText(prompt);
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a number.");
            }
        }
    }

    private String readText(String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) {
            System.out.println("\nNo more input. Closing the parking lot.");
            scanner.close();
            System.exit(0);
        }
        return scanner.nextLine().trim();
    }
}
