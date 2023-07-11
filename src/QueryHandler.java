import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class QueryHandler {
   private static Connection conn;

   private static Connection getConnection() {
      if (QueryHandler.conn == null) {
         try {
            QueryHandler.conn = DriverManager
                  .getConnection("jdbc:postgresql://localhost/minesweeper_db?user=postgres&password=0967918502");
         } catch (SQLException e) {
            e.printStackTrace();
         }
      }
      return QueryHandler.conn;
   }

   public static void executeUpdate(final String sql, final Object... params) {
      try {
         PreparedStatement s = QueryHandler.getConnection().prepareStatement(sql);
         setParameters(s, params);
         s.executeUpdate();
      } catch (final SQLException e) {
         System.out.println(sql);
         e.printStackTrace();
      }
   }

   public static ResultSet executeQuery(final String sql, final Object... params) {
      try {
         PreparedStatement s = QueryHandler.getConnection().prepareStatement(sql);
         setParameters(s, params);
         return s.executeQuery();
      } catch (final SQLException e) {
         System.out.println(sql);
         e.printStackTrace();
         return null;
      }
   }

   private static void setParameters(final PreparedStatement stmt, final Object... parameters) throws SQLException {
      for (int i = 0; i < parameters.length; i++) {
         final Object param = parameters[i];
         if (param instanceof String) {
            stmt.setString(i + 1, (String) param);
         } else if (param instanceof Boolean) {
            stmt.setBoolean(i + 1, (Boolean) param);
         } else if (param instanceof Integer) {
            stmt.setInt(i + 1, (Integer) param);
         } else if (param instanceof Double) {
            stmt.setDouble(i + 1, (Double) param);
         }
      }
   }
}
