package scheduler;

import com.microsoft.sqlserver.jdbc.SQLServerException;
import scheduler.db.ConnectionManager;
import scheduler.model.Caregiver;
import scheduler.model.Patient;
import scheduler.model.Vaccine;
import scheduler.util.Util;

import javax.management.monitor.StringMonitorMBean;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;

import java.util.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;

    public static void main(String[] args) {
        // printing greetings text
        System.out.println();
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        System.out.println("*** Please enter one of the following commands ***");
        System.out.println("> create_patient <username> <password>");  //TODO: implement create_patient (Part 1)
        System.out.println("> create_caregiver <username> <password>");
        System.out.println("> login_patient <username> <password>");  // TODO: implement login_patient (Part 1)
        System.out.println("> login_caregiver <username> <password>");
        System.out.println("> search_caregiver_schedule <date>");  // TODO: implement search_caregiver_schedule (Part 2)
        System.out.println("> reserve <date> <vaccine>");  // TODO: implement reserve (Part 2)
        System.out.println("> upload_availability <date>");
        System.out.println("> cancel <appointment_id>");  // TODO: implement cancel (extra credit)
        System.out.println("> add_doses <vaccine> <number>");
        System.out.println("> show_appointments");  // TODO: implement show_appointments (Part 2)
        System.out.println("> logout");  // TODO: implement logout (Part 2)
        System.out.println("> quit");
        System.out.println();

        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String response = "";
            try {
                response = r.readLine();
            } catch (IOException e) {
                System.out.println("Please try again!");
            }
            // split the user input by spaces
            String[] tokens = response.split(" ");
            // check if input exists
            if (tokens.length == 0) {
                System.out.println("Please try again!");
                continue;
            }
            // determine which operation to perform
            String operation = tokens[0];
            if (operation.equals("create_patient")) {
                createPatient(tokens);
            } else if (operation.equals("create_caregiver")) {
                createCaregiver(tokens);
            } else if (operation.equals("login_patient")) {
                loginPatient(tokens);
            } else if (operation.equals("login_caregiver")) {
                loginCaregiver(tokens);
            } else if (operation.equals("search_caregiver_schedule")) {
                searchCaregiverSchedule(tokens);
            } else if (operation.equals("reserve")) {
                reserve(tokens);
            } else if (operation.equals("upload_availability")) {
                uploadAvailability(tokens);
            } else if (operation.equals("cancel")) {
                cancel(tokens);
            } else if (operation.equals("add_doses")) {
                addDoses(tokens);
            } else if (operation.equals("show_appointments")) {
                showAppointments(tokens);
            } else if (operation.equals("logout")) {
                logout(tokens);
            } else if (operation.equals("quit")) {
                System.out.println("Bye!");
                return;
            } else {
                System.out.println("Invalid operation name!");
            }
        }
    }

    private static void createPatient(String[] tokens) {
        // create_patient <username> <password>
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        if (usernameExistsPatient(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        if (!checkStrongPw(password)) {
            System.out.println("Password is not strong enough, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        try {
            currentPatient = new Patient.PatientBuilder(username, salt, hash).build();
            currentPatient.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsPatient(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Patients WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void createCaregiver(String[] tokens) {
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsCaregiver(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        if (!checkStrongPw(password)) {
            System.out.println("Password is not strong enough, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            currentCaregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build();
            // save to caregiver information to our database
            currentCaregiver.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsCaregiver(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Caregivers WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static boolean checkStrongPw(String pw) {
        boolean hasEight = pw.length() >= 8;
        boolean hasLower = false;
        boolean hasUpper = false;
        boolean hasNumber = false;
        boolean hasSpecial = false;
        Set<Character> set = new HashSet<>(Arrays.asList('!', '@', '#', '?'));
        for (char i : pw.toCharArray()) {
            if (Character.isLowerCase(i))
                hasLower = true;
            if (Character.isUpperCase(i))
                hasUpper = true;
            if (Character.isDigit(i))
                hasNumber = true;
            if (set.contains(i))
                hasSpecial = true;
        }
        return hasEight && hasLower && hasUpper && hasNumber && hasSpecial;
    }

    private static void loginPatient(String[] tokens) {
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Patient patient = null;
        try {
            patient = new Patient.PatientGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (patient == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentPatient = patient;
        }
    }

    private static void loginCaregiver(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    private static void searchCaregiverSchedule(String[] tokens) {
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }

        if (tokens.length != 2) {
            System.out.println("Enter a valid date, please try again!");
            return;
        }

        String date = tokens[1];
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        Date d;
        try {
            d = Date.valueOf(date);
        } catch (IllegalArgumentException e) {
            System.out.println("Please print date in the format: yyyy-mm-dd");
            return;
        }

        String getAvail = "SELECT cUsername FROM Availabilities WHERE Time = ? AND vName IS NULL AND pUsername IS NULL ORDER BY cUsername";
        try {
            PreparedStatement statement_1 = con.prepareStatement(getAvail);
            statement_1.setDate(1, d);
            ResultSet resultSet_1;
            resultSet_1 = statement_1.executeQuery();
            if (!resultSet_1.isBeforeFirst()) {
                System.out.println("No availabilities found on this date!");
                return;
            } else {
                StringBuffer availCaregivers = new StringBuffer();
                while(resultSet_1.next()) {
                    String cName = resultSet_1.getString(1);
                    availCaregivers.append(cName).append(" ");
                }
                String getDoses = "SELECT Name, Doses FROM Vaccines";
                PreparedStatement statement_2 = con.prepareStatement(getDoses);
                statement_1.setDate(1, d);
                ResultSet resultSet_2;
                resultSet_2 = statement_2.executeQuery();
                StringBuffer vaccineDoses = new StringBuffer();
                while(resultSet_2.next()) {
                    String name = resultSet_2.getString(1);
                    int doses = resultSet_2.getInt(2);
                    vaccineDoses.append("\n").append(name).append(": ").append(doses).append(" doses ");
                }
                System.out.println("Caregivers available on " + d + ": " + availCaregivers + vaccineDoses);
            }
        } catch (SQLException e) {
            System.out.println("Please try again!");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }

    }

    private static void reserve(String[] tokens) {
        if (currentCaregiver != null) {
            System.out.println("Please login as a patient!");
            return;
        } else if (currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }

        if (tokens.length != 3) {
            System.out.println("Please enter both date and vaccine!");
            return;
        }

        String date = tokens[1];
        String vName = tokens[2];
        Vaccine vaccine = null;

        Date d;
        try {
            d = Date.valueOf(date);
        } catch (IllegalArgumentException e) {
            System.out.println("Please print date in the format: yyyy-mm-dd");
            return;
        }

        try {
            vaccine = new Vaccine.VaccineGetter(tokens[2]).get();
        } catch (SQLException e) {
            System.out.println("Error occurred with chosen vaccine!");
            e.printStackTrace();
        }

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        // Checking if vaccine is in stock
        try {
            PreparedStatement getAllVaccines = con.prepareStatement("SELECT Name FROM vaccines");
            ResultSet rs = getAllVaccines.executeQuery();
            StringBuffer availVax = new StringBuffer();
            while (rs.next()) {
                availVax.append("\n").append(rs.getString(1)).append(" ");
            }
            if (availVax.indexOf(vName) == -1) {
                System.out.println(vName + " is not in our inventory. We currently have these vaccines in" +
                        " our inventory: " + availVax);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Checking if any availabilities
        String getAvail = "SELECT cUsername FROM Availabilities WHERE Time = ? AND vName IS NULL AND pUsername IS NULL ORDER BY cUsername";
        String caregiver = null;
        try {
            PreparedStatement statement_1 = con.prepareStatement(getAvail);
            statement_1.setDate(1, d);
            ResultSet resultSet_1;
            resultSet_1 = statement_1.executeQuery();
            if (!resultSet_1.isBeforeFirst()) {
                System.out.println("No caregiver is available!");
                return;
            } else {
                StringBuffer availCaregivers = new StringBuffer();
                while (resultSet_1.next()) {
                    String cName = resultSet_1.getString(1);
                    availCaregivers.append(cName).append(" ");
                }
                caregiver = availCaregivers.substring(0, availCaregivers.indexOf(" "));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        //
        String checkAvail = "SELECT aptId FROM Availabilities WHERE Time = ? AND vName IS NULL AND pUsername IS NULL ORDER BY cUsername";
        int id = 0;
        try {
            PreparedStatement statement = con.prepareStatement(checkAvail);
            statement.setDate(1, d);
            ResultSet rs;
            rs = statement.executeQuery();
            if (!rs.next()) {
                return;
            }
            if (vaccine.getAvailableDoses() == 0) {
                System.out.println("Not enough available doses!");
                return;
            } else {
                id = rs.getInt(1);
            }
        } catch(SQLException e){
            e.printStackTrace();
        }
        System.out.println("Successfully scheduled:\nAppointment ID: " + id + "\nCaregiver username: " + caregiver);

        String updateApt = "UPDATE Availabilities SET pUsername = ?, vName = ? WHERE aptId = ?;";
        try {
            PreparedStatement statement2 = con.prepareStatement(updateApt);
            statement2.setString(1, currentPatient.getUsername());
            statement2.setString(2, vName);
            statement2.setInt(3, id);
            statement2.executeUpdate();
            vaccine.decreaseAvailableDoses(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void uploadAvailability(String[] tokens) {
        // upload_availability <date>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            currentCaregiver.uploadAvailability(d);
            System.out.println("Availability uploaded!");
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when uploading availability");
            e.printStackTrace();
        }
    }

    private static void cancel(String[] tokens) {
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
        }

        if (tokens.length != 2) {
            System.out.println("Please enter the id of the appointment you want cancelled!");
            return;
        }

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String id = tokens[1];
        String updateApt = "UPDATE Availabilities SET pUsername = ?, vName = ? WHERE aptId = ?;";
        String addBackDose = "SELECT vName FROM Availabilities WHERE aptID = ?";
        try {
            PreparedStatement statement2 = con.prepareStatement(updateApt);
            statement2.setString(1, null);
            statement2.setString(2, null);
            statement2.setString(3, id);
            statement2.executeUpdate();

            PreparedStatement add = con.prepareStatement(addBackDose);
            add.setString(1, id);
            ResultSet rs = add.executeQuery();
            String vax = rs.getString(1);
            Vaccine vac = new Vaccine.VaccineGetter(vax).get();
            vac.increaseAvailableDoses(1);
        } catch (SQLException e) {
            System.out.println("Successfully cancelled");
        }
    }

    private static void addDoses(String[] tokens) {
        // add_doses <vaccine> <number>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String vaccineName = tokens[1];
        int doses = Integer.parseInt(tokens[2]);
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
            e.printStackTrace();
        }
        // check 3: if getter returns null, it means that we need to create the vaccine and insert it into the Vaccines
        //          table
        if (vaccine == null) {
            try {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        } else {
            // if the vaccine is not null, meaning that the vaccine already exists in our table
            try {
                vaccine.increaseAvailableDoses(doses);
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        }
        System.out.println("Doses updated!");
    }

    private static void showAppointments(String[] tokens) {
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }

        if (tokens.length != 1) {
            System.out.println("Please try again!");
            return;
        }
        if (currentCaregiver != null) {
            ConnectionManager cm = new ConnectionManager();
            Connection con = cm.createConnection();
            String getApt = "SELECT aptId, vName, Time, pUsername  FROM Availabilities WHERE cUsername = ? ORDER BY aptId";
            try {
                PreparedStatement statement = con.prepareStatement(getApt);
                statement.setString(1, currentCaregiver.getUsername());
                ResultSet rs;
                rs = statement.executeQuery();
                if (!rs.isBeforeFirst() ) {
                    System.out.println("You have no appointments scheduled!");
                } else {
                    while (rs.next()) {
                        System.out.println("Appointment ID: " + rs.getInt(1) + ", Vaccine Name: " +
                                rs.getString(2) + ", Date: " + rs.getDate(3) + ", Patient Name: "
                                + rs.getString(4));
                    }
                }

            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                cm.closeConnection();
            }
        } else if (currentPatient != null) {
            ConnectionManager cm = new ConnectionManager();
            Connection con = cm.createConnection();
            String getApt = "SELECT aptId, vName, Time, cUsername  FROM Availabilities WHERE pUsername = ? ORDER BY aptId";
            try {
                PreparedStatement statement = con.prepareStatement(getApt);
                statement.setString(1, currentPatient.getUsername());
                ResultSet rs;
                rs = statement.executeQuery();
                if (!rs.isBeforeFirst() ) {
                    System.out.println("You have no appointments scheduled!");
                } else {
                    while (rs.next()) {
                        System.out.println("Appointment ID: " + rs.getInt(1) + ", Vaccine Name: " +
                                rs.getString(2) + ", Date: " + rs.getDate(3) + ", Caregiver username: "
                                + rs.getString(4));
                    }
                }

            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                cm.closeConnection();
            }
        }
    }

    private static void logout(String[] tokens) {
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first.");
            return;
        }
        currentCaregiver = null;
        currentPatient = null;
        System.out.println("Successfully logged out!");
    }
}
