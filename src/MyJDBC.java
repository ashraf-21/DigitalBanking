import java.sql.Connection;
import java.sql.DriverManager;

public class MyJDBC {

    public static Connection getConnection() {

        Connection con = null;

        try {
            String url = "jdbc:mysql://localhost:3306/transaction_schema";
            String user = "root";
            String password = "Ashr@f2115";

            con = DriverManager.getConnection(url, user, password);
            System.out.println("Database connected!");

        } catch (Exception e) {
            e.printStackTrace();
        }

        return con;
    }
}