import java.sql.*;
public class Check {
    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection("jdbc:mysql://mysql-16b47c6b-phongtran080809-7c70.c.aivencloud.com:26260/defaultdb?sslMode=REQUIRED", "avnadmin", "AVNS_R3e75R9Bz5pEXwuzhK6");
        // Kiểm tra tên cột thực tế
        ResultSet rs = c.createStatement().executeQuery("SHOW COLUMNS FROM users");
        while(rs.next()) System.out.println(rs.getString(1) + " | " + rs.getString(2));
        c.close();
    }
}
