package com.example.project;

import java.util.*;
import java.text.SimpleDateFormat;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.*;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class User {
    @Id
    private String id = UUID.randomUUID().toString();
    @Column(nullable = false, unique = true)
    private String username;
    @Column(nullable = false)
    private String hashedPassword;
    private String phoneNumber;
    private Date birthDate;
    private Date registrationDate = new Date();
    private double walletBalance;
    @ElementCollection
    private List<BankAccount> bankAccounts = new ArrayList<>();
    private String subscriptionLevel = "Bronze";
    @ElementCollection
    private List<String> transactionLog = new ArrayList<>();
    @ElementCollection
    private List<String> reservedSessions = new ArrayList<>();

    public User() {
        // Constructor for JPA
    }

    public User(String username, String password, String phoneNumber, Date birthDate) throws Exception {
        validatePassword(password);
        this.username = username;
        this.hashedPassword = hashPassword(password);
        this.phoneNumber = phoneNumber;
        this.birthDate = birthDate;
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) hexString.append(String.format("%02x", b));
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password.", e);
        }
    }

    public boolean verifyPassword(String password) {
        return this.hashedPassword.equals(hashPassword(password));
    }

    public void setPassword(String newPassword) throws Exception {
        validatePassword(newPassword);
        this.hashedPassword = hashPassword(newPassword);
    }

    private void validatePassword(String password) throws Exception {
        if (password.length() < 4) throw new Exception("Password must be at least 4 characters long.");
    }

    public String depositToWallet(String accountNumber, double amount) {
        return bankAccounts.stream()
                .filter(account -> account.getAccountNumber().equals(accountNumber))
                .findFirst()
                .map(account -> {
                    if (account.getBalance() < amount) return "Insufficient balance.";
                    account.setBalance(account.getBalance() - amount);
                    walletBalance += amount;
                    transactionLog.add("Deposited " + amount + " from account " + accountNumber);
                    return "Deposit successful.";
                })
                .orElse("Invalid account.");
    }

    public String reserveCinemaSession(String sessionId, double sessionPrice, int ageLimit, int userAge) {
        if (userAge < ageLimit) return "You do not meet the age requirement for this session.";
        if (walletBalance < sessionPrice) return "Insufficient balance in your wallet.";
        walletBalance -= sessionPrice;
        reservedSessions.add(sessionId);
        transactionLog.add("Reserved session " + sessionId + " for " + sessionPrice);
        return "Session reserved successfully.";
    }

    public void saveToFile() {
        try (Writer writer = new FileWriter(username + ".json")) {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.writeValue(writer, this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static User loadFromFile(String username) {
        try (Reader reader = new FileReader(username + ".json")) {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(reader, User.class);
        } catch (IOException e) {
            System.out.println("User file not found.");
            return null;
        }
    }
}

@Embeddable
class BankAccount {
    private String accountNumber;
    private double balance;

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }
}

class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        List<User> allUsers = new ArrayList<>();

        while (true) {
            System.out.println("\nMain Menu:\n1 - Register a new user\n2 - Login\n3 - Change password\n4 - Delete user account\n5 - Save user to file\n6 - Load user from file\n0 - Exit");
            System.out.print("Enter your choice: ");

            switch (scanner.nextLine()) {
                case "0" -> {
                    System.out.println("Exiting the program. Goodbye!");
                    return;
                }
                case "1" -> registerUser(scanner, allUsers);
                case "2" -> loginUser(scanner, allUsers);
                case "3" -> changePassword(scanner, allUsers);
                case "4" -> deleteUser(scanner, allUsers);
                case "5" -> saveUserToFile(scanner, allUsers);
                case "6" -> loadUserFromFile(scanner, allUsers);
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private static void registerUser(Scanner scanner, List<User> allUsers) {
        try {
            System.out.print("Enter username: ");
            String username = scanner.nextLine();
            System.out.print("Enter password: ");
            String password = scanner.nextLine();
            System.out.print("Enter phone number (optional): ");
            String phoneNumber = scanner.nextLine();
            System.out.print("Enter birth date (optional, yyyy-mm-dd): ");
            String birthDateInput = scanner.nextLine();
            Date birthDate = birthDateInput.isEmpty() ? null : new SimpleDateFormat("yyyy-MM-dd").parse(birthDateInput);

            allUsers.add(new User(username, password, phoneNumber, birthDate));
            System.out.println("User registered successfully!");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void loginUser(Scanner scanner, List<User> allUsers) {
        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        allUsers.stream()
                .filter(user -> user.getUsername().equals(username) && user.verifyPassword(password))
                .findFirst()
                .ifPresentOrElse(
                        user -> System.out.println("Welcome back, " + username + "!"),
                        () -> System.out.println("Invalid username or password!"));
    }

    private static void changePassword(Scanner scanner, List<User> allUsers) {
        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        System.out.print("Enter current password: ");
        String oldPassword = scanner.nextLine();
        System.out.print("Enter new password: ");
        String newPassword = scanner.nextLine();

        allUsers.stream()
                .filter(user -> user.getUsername().equals(username) && user.verifyPassword(oldPassword))
                .findFirst()
                .ifPresentOrElse(user -> {
                    try {
                        user.setPassword(newPassword);
                        System.out.println("Password changed successfully!");
                    } catch (Exception e) {
                        System.out.println("Error: " + e.getMessage());
                    }
                }, () -> System.out.println("Invalid username or password!"));
    }

    private static void deleteUser(Scanner scanner, List<User> allUsers) {
        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        if (allUsers.removeIf(user -> user.getUsername().equals(username) && user.verifyPassword(password))) {
            System.out.println("User account deleted successfully!");
        } else {
            System.out.println("Invalid username or password!");
        }
    }

    private static void saveUserToFile(Scanner scanner, List<User> allUsers) {
        System.out.print("Enter username: ");
        String username = scanner.nextLine();

        allUsers.stream()
                .filter(user -> user.getUsername().equals(username))
                .findFirst()
                .ifPresentOrElse(user -> {
                    user.saveToFile();
                    System.out.println("User saved to file successfully!");
                }, () -> System.out.println("User not found!"));
    }

    private static void loadUserFromFile(Scanner scanner, List<User> allUsers) {
        System.out.print("Enter username: ");
        String username = scanner.nextLine();

        User user = User.loadFromFile(username);
        if (user != null) {
            allUsers.add(user);
            System.out.println("User loaded from file successfully!");
        } else {
            System.out.println("Failed to load user from file.");
        }
    }
}
