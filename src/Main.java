import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.HashMap;
import java.util.Map;

public class Main {

    public static void main(String[] args) {

        Random random = new Random(System.currentTimeMillis());
        double balance = 1000000;

        String[] devices = {"Android Phone", "iPhone", "Laptop", "ATM"};
        String[] locations = {"Hyderabad", "Bangalore", "Mumbai", "Delhi"};
        String[] authTypes = {"PIN", "OTP", "Biometric"};
        String[] modes = {"UPI", "IMPS", "NEFT", "RTGS"};
        String[] types = {"Credit", "Debit"};

        // Track sender activity
        Map<String, Integer> transactionCount = new HashMap<>();
        Map<String, String> lastLocation = new HashMap<>();

        try {
            Connection con = MyJDBC.getConnection();

            String sql = """
                    
                            INSERT INTO transactions (
                        transaction_id,
                        transaction_type,
                        payment_mode,
                        amount,
                        transaction_time,
                        sender_id,
                        sender_account,
                        sender_mobile,
                        sender_device,
                        sender_location,
                        sender_ip,
                        auth_type,
                        receiver_id,
                        receiver_account,
                        receiver_mobile,
                        fraud_score
                    ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    
                    """;

            PreparedStatement ps = con.prepareStatement(sql);

            for (int i = 1; i <= 10; i++) {

                long transactionId = 1000000 + random.nextInt(900000);
                String transactionType = types[random.nextInt(types.length)];
                String transactionMode = modes[random.nextInt(modes.length)];
                double amount = 1000 + random.nextInt(90000);

                String senderId = "U" + (1000 + random.nextInt(9000));
                String senderAccount = "ACC" + (100000 + random.nextInt(900000));
                String senderMobile = "9" + (100000000 + random.nextInt(900000000));
                String senderDevice = devices[random.nextInt(devices.length)];
                String senderLocation = locations[random.nextInt(locations.length)];
                String senderIP = "192.168." + random.nextInt(255) + "." + random.nextInt(255);
                String authType = authTypes[random.nextInt(authTypes.length)];

                String receiverId = "U" + (1000 + random.nextInt(9000));
                String receiverAccount = "ACC" + (100000 + random.nextInt(900000));
                String receiverMobile = "9" + (100000000 + random.nextInt(900000000));

                int fraudScore = 0;
                String failureReason = "NA";

                // ---------- FRAUD RULES ----------
                if (amount > 50000) fraudScore += 30;
                if (!senderDevice.equals("Android Phone")) fraudScore += 20;
                if (!senderLocation.equals("Hyderabad")) fraudScore += 20;


                // Invalid mobile number
                if (!senderMobile.matches("[6-9][0-9]{9}")) {
                    fraudScore += 30;
                    failureReason = "Invalid mobile number";
                }

                // Huge amount
                if (amount > 75000) {
                    fraudScore += 25;
                    failureReason = "Unusually high transaction amount";
                }

                // Location change
                if (lastLocation.containsKey(senderId)
                        && !lastLocation.get(senderId).equals(senderLocation)) {
                    fraudScore += 25;
                    failureReason = "Sender location changed";
                }

                lastLocation.put(senderId, senderLocation);

                // Multiple transactions
                transactionCount.put(senderId,
                        transactionCount.getOrDefault(senderId, 0) + 1);

                if (transactionCount.get(senderId) > 3) {
                    fraudScore += 30;
                    failureReason = "Multiple rapid transactions";
                }

                // ---------- FINAL STATUS ----------
                String status;

                if (fraudScore >= 70) {
                    status = "FRAUD - BLOCKED";
                    failureReason = "High fraud score due to suspicious activity";
                } else if (transactionType.equals("Debit") && amount > balance) {
                    status = "FAILED";
                    failureReason = "Insufficient balance";
                } else {
                    status = "SUCCESS";

                    if (fraudScore >= 40) {
                        failureReason = "High-risk indicators present but allowed";
                    } else if (amount > 50000) {
                        failureReason = "High amount transaction verified";
                    } else if (!senderLocation.equals("Hyderabad")) {
                        failureReason = "Location change verified";
                    } else {
                        failureReason = "Normal transaction";
                    }

                    if (transactionType.equals("Debit")) balance -= amount;
                    else balance += amount;
                }


                LocalDateTime time = LocalDateTime.now();
                System.out.println(
                        transactionId + " | " +
                                transactionType + " | " +
                                amount + " | FraudScore: " +
                                fraudScore + " | " + status
                );


                // ---------- INSERT ----------
                ps.setLong(1, transactionId);
                ps.setString(2, transactionType);
                ps.setString(3, transactionMode);
                ps.setDouble(4, amount);
                ps.setObject(5, time);

                ps.setString(6, senderId);
                ps.setString(7, senderAccount);
                ps.setString(8, senderMobile);
                ps.setString(9, senderDevice);
                ps.setString(10, senderLocation);
                ps.setString(11, senderIP);
                ps.setString(12, authType);

                ps.setString(13, receiverId);
                ps.setString(14, receiverAccount);
                ps.setString(15, receiverMobile);

                ps.setInt(16, fraudScore);

                ps.executeUpdate();

                // ---------- ALERT ----------
                if (status.contains("FRAUD") || status.equals("FAILED")) {
                    System.out.println("🚨 ALERT: " + transactionId + " | " + failureReason);
                }
            }

            con.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}