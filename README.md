# Smart Parking Management System

A console-based parking lot manager written in **pure Core Java**, built to make every core OOP
concept earn its place. No database, no Spring, no GUI — just classes, interfaces, exceptions
and a `Scanner`.

The domain is small but real: vehicles arrive, get a slot and a ticket, are charged by how long
they stayed, pay through a chosen method, and the slot becomes free again.

---

## Features

| # | Menu option | What it does |
|---|-------------|--------------|
| 1 | Park Vehicle | Validates the vehicle, finds a free compatible slot, issues a ticket |
| 2 | Remove Vehicle | Shows who/what/where/how long for a given ticket |
| 3 | Find Vehicle | Search by registration number, with a fallback search by vehicle type |
| 4 | View Available Slots | Free slot count per type and the full slot board |
| 5 | View Parked Vehicles | Table of every active ticket |
| 6 | Calculate Parking Fee | Duration, grace period, rate plan and total |
| 7 | Make Payment | Cash / Card / UPI checkout, then frees the slot |
| 8 | Exit | Closes the lot |

Billing rules that are actually implemented:

- Bike **₹20/hour**, Car **₹40/hour**, Truck **₹80/hour**
- First **15 minutes are free** (`GRACE_PERIOD_MINUTES`)
- Remaining minutes are **rounded up to the next full hour**
- Slot capacity: 5 bike (`B-01`…`B-05`), 8 car (`C-01`…`C-08`), 3 large (`L-01`…`L-03`)

---

## OOP concepts and where they live

| Concept | In this project |
|---------|-----------------|
| Class & Object | `Vehicle`, `ParkingSlot`, `ParkingTicket`, `ParkingLot` |
| Encapsulation | `ParkingSlot.vehicle` is private — you call `occupy()` / `release()`, never a setter |
| Constructor validation | `new Car("MH", "")` throws `InvalidVehicleException` before the object exists |
| Inheritance | `Bike` / `Car` / `Truck` extend `Vehicle` (IS-A) |
| Abstraction | `abstract class Vehicle`, `interface Payment` |
| Polymorphism | `getVehicleType()`, `getHourlyRate()`, `pay()` behave per runtime object |
| Composition | `ParkingLot` **HAS-A** list of slots and a map of tickets |
| Method overloading | `findVehicle(String)` and `findVehicle(String, SlotType)` |
| `static` | `ParkingTicket.lastTicketNumber` generates `T1001`, `T1002`, … |
| `final` | `ParkingLot.BIKE_SLOTS`, `GRACE_PERIOD_MINUTES` — the lot's fixed rules |
| Exception handling | `InvalidVehicleException`, `ParkingFullException`, `InvalidTicketException` |

---

## Class design

```
                    abstract Vehicle
                    ┌───────────────┐
                    │ registration  │
                    │ owner         │
                    │ hourlyRate    │
                    └───────┬───────┘
            ┌───────────┬───┴───────┬────────────┐
          Bike         Car        Truck      (abstract getVehicleType,
        ₹20/h        ₹40/h       ₹80/h       getRequiredSlot)


   interface Payment ──────────┬──────────────┬──────────────┐
        boolean pay(double)   CashPayment   CardPayment   UPIPayment
        String  getMethodName()


   ParkingSlot  ←── has-a ──  ParkingTicket
   (slotId, type,              (T1001, vehicle,
    private vehicle)            slot, entry/exit, amountDue)


   ParkingLot
     ├── final rules (BIKE_SLOTS, CAR_SLOTS, LARGE_SLOTS, GRACE_PERIOD_MINUTES)
     ├── List<ParkingSlot> slots
     ├── Map<String, ParkingTicket> activeTickets
     └── Map<String, ParkingTicket> closedTickets
```

---

## Two design decisions worth reading the code for

**1. No `instanceof` anywhere in the billing path.**

`ParkingTicket.calculateFee()` simply asks the vehicle what it costs:

```java
public double calculateFee() {
    long minutes = getDuration().toMinutes() - ParkingLot.GRACE_PERIOD_MINUTES;
    if (minutes <= 0) {
        amountDue = 0;
    } else {
        long hours = (minutes + 59) / 60;      // round up
        amountDue = hours * vehicle.getHourlyRate();
    }
    return amountDue;
}
```

Add a `Bus` class later and nothing in `ParkingTicket`, `ParkingLot` or the menu has to change.
That is the payoff of polymorphism over `if (vehicle instanceof Car)`.

**2. Payment happens before the slot is released.**

```java
double fee = ticket.calculateFee();
if (!payment.pay(fee)) {                 // declined?
    throw new ParkingException(...);     // vehicle stays parked
}
ticket.close(LocalDateTime.now());
ticket.getSlot().release();              // only now is the slot free
```

A failed checkout never loses the vehicle, which is how a real system behaves.

---

## How to run

Requires JDK 17 or newer (uses switch expressions, `Duration`, `LocalDateTime`).

```bash
# compile
javac -d out Smart_Parking_Management_System.java

# run
java -cp out Smart_Parking_Management_System
```

On Windows PowerShell:

```powershell
javac -d out Smart_Parking_Management_System.java
java -cp out Smart_Parking_Management_System
```

---

## Sample run

```
==============================================
       SMART PARKING SYSTEM
==============================================
Slots: 16  (5 bike, 8 car, 3 large)
Free grace period: 15 minutes

1. Park Vehicle
...
Enter choice: 1

1. Bike  (Rs 20/hour)
2. Car   (Rs 40/hour)
3. Truck (Rs 80/hour)
Enter vehicle type: 2
Registration Number: MH12AB1234
Owner Name: Krushna

==============================================
  VEHICLE PARKED SUCCESSFULLY
==============================================
Vehicle      : Car
Registration : MH12AB1234
Owner        : Krushna
Slot         : C-01
Ticket ID    : T1001
Entry Time   : 29-09-2026 16:10
Rate         : Rs 40 per hour
```

Exceptions surface as readable messages instead of stack traces:

```
Could not park: Registration number must be at least 6 characters.
Cannot remove: Ticket T1003 does not exist.
Could not park: MH12AB1234 is already parked.
Checkout failed: Payment declined by UPI.
The vehicle is still parked. Try another payment method.
```

---

## Project layout

```
Smart_Parking_Management_System/
├── README.md
├── .gitignore
└── Smart_Parking_Management_System.java   ← all classes, one file
```

The whole app lives in a single file on purpose — it keeps the OOP structure visible without
package/import noise while you are still learning the concepts themselves.

---

## Ideas to extend

1. **Multiple parking lots** — a `List<ParkingLot>` with a lot-selection menu.
2. **A `Bus` / `EV` vehicle** — add the subclass and a new `SlotType`; nothing else changes.
3. **Day passes** — a `PricingStrategy` interface so `FlatRate` and `DayPass` can be swapped.
4. **File persistence** — write `activeTickets` to a text file and reload on start.
5. **Slot preferences** — let a driver request a slot number instead of "any free one".
6. **Tests** — `park()` / `calculateFee()` are pure enough to unit test without the console.

---

Built as a Core Java OOP exercise.
