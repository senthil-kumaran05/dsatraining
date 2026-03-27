
// PaymentMethod Interface
interface PaymentMethod {
    void processPayment(double amount);
}

// Credit Card Payment
class CreditCardPayment implements PaymentMethod {
    public void processPayment(double amount) {
        System.out.println("Processing Credit Card payment of Rs." + amount);
    }
}

// PayPal Payment
class PayPalPayment implements PaymentMethod {
    public void processPayment(double amount) {
        System.out.println("Processing PayPal payment of Rs." + amount);
    }
}

// UPI Payment
class UPIPayment implements PaymentMethod {
    public void processPayment(double amount) {
        System.out.println("Processing UPI payment of Rs." + amount);
    }
}

// Service Class (DIP)
class PaymentProcessor {
    private PaymentMethod paymentMethod;

    public PaymentProcessor(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public void process(double amount) {
        paymentMethod.processPayment(amount);
    }
}


// Abstract Base Class
abstract class Device {
    private String deviceName;
    private boolean powerStatus; // true = ON, false = OFF

    // Constructor
    public Device(String deviceName) {
        this.deviceName = deviceName;
        this.powerStatus = false;
    }

    // Getter and Setter
    public String getDeviceName() {
        return deviceName;
    }

    public boolean isPowerOn() {
        return powerStatus;
    }

    public void setPowerStatus(boolean powerStatus) {
        this.powerStatus = powerStatus;
    }

    // Common Methods
    public void turnOn() {
        powerStatus = true;
        System.out.println(deviceName + " is turned ON");
    }

    public void turnOff() {
        powerStatus = false;
        System.out.println(deviceName + " is turned OFF");
    }

    // Abstract Method (Polymorphism)
    public abstract void displayStatus();
}

// Derived Class: Light
class Light extends Device {
    private int brightness;

    public Light(String name, int brightness) {
        super(name);
        this.brightness = brightness;
    }

    // Getter & Setter
    public int getBrightness() {
        return brightness;
    }

    public void setBrightness(int brightness) {
        this.brightness = brightness;
    }

    // Override method
    @Override
    public void displayStatus() {
        System.out.println("Light: " + getDeviceName() +
                " | Status: " + (isPowerOn() ? "ON" : "OFF") +
                " | Brightness: " + brightness);
    }
}

// Derived Class: Thermostat
class Thermostat extends Device {
    private double temperature;

    public Thermostat(String name, double temperature) {
        super(name);
        this.temperature = temperature;
    }

    // Getter & Setter
    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    // Override method
    @Override
    public void displayStatus() {
        System.out.println("Thermostat: " + getDeviceName() +
                " | Status: " + (isPowerOn() ? "ON" : "OFF") +
                " | Temperature: " + temperature + "°C");
    }
}

// Email Interface
interface EmailSender {
    void sendEmail(String message);
}

// SMS Interface
interface SMSSender {
    void sendSMS(String message);
}

// Push Notification Interface
interface PushNotificationSender {
    void sendPushNotification(String message);
}

// Email Notification Class
class EmailNotification implements EmailSender {
    public void sendEmail(String message) {
        System.out.println("Sending Email: " + message);
    }
}

// SMS Notification Class
class SMSNotification implements SMSSender {
    public void sendSMS(String message) {
        System.out.println("Sending SMS: " + message);
    }
}

// Mobile App Notification Class
class MobileAppNotification implements PushNotificationSender {
    public void sendPushNotification(String message) {
        System.out.println("Sending Push Notification: " + message);
    }
}

// Main Class
public class week3solution {
    public static void main(String[] args) {
        PaymentMethod credit = new CreditCardPayment();
        PaymentMethod paypal = new PayPalPayment();
        PaymentMethod upi = new UPIPayment();

        PaymentProcessor processor1 = new PaymentProcessor(credit);
        processor1.process(1000);

        PaymentProcessor processor2 = new PaymentProcessor(paypal);
        processor2.process(2000);

        PaymentProcessor processor3 = new PaymentProcessor(upi);
        processor3.process(500);

        // Polymorphism (Base class reference)
        Device light = new Light("Living Room Light", 75);
        Device thermostat = new Thermostat("Bedroom Thermostat", 24.5);

        // Turn ON devices
        light.turnOn();
        thermostat.turnOn();

        // Display Status
        light.displayStatus();
        thermostat.displayStatus();

        // Turn OFF devices
        light.turnOff();
        thermostat.turnOff();

        // Display again
        light.displayStatus();
        thermostat.displayStatus();

         EmailSender email = new EmailNotification();
        SMSSender sms = new SMSNotification();
        PushNotificationSender push = new MobileAppNotification();

        email.sendEmail("Hello via Email!");
        sms.sendSMS("Hello via SMS!");
        push.sendPushNotification("Hello via Push Notification!");
           }
}

        