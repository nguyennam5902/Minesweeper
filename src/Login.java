import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class Login extends JFrame {
   public static void main(final String[] args) throws Exception {
      SwingUtilities.invokeLater(() -> new Login());
   }

   private final String connectionString = "jdbc:postgresql://localhost/minesweeper_db?user=postgres&password=0967918502";

   public Login() {
      try {
         Class.forName("org.postgresql.Driver");
         GameAction.conn = DriverManager.getConnection(connectionString);
      } catch (final ClassNotFoundException e) {
         e.printStackTrace();
      } catch (final SQLException e1) {
         e1.printStackTrace();
      }
      boolean isLogin = false;
      String line = "NULL";
      try (BufferedReader reader = new BufferedReader(new FileReader("cookie"))) {
         line = reader.readLine();
         if (!line.equals("NULL"))
            isLogin = true;
      } catch (final IOException e) {
         System.err.println("Error reading file: " + e.getMessage());
      }
      if (isLogin) {
         GameAction.username = line;
         SwingUtilities.invokeLater(() -> new Minesweeper(true));
      } else {
         final Container cp = getContentPane();
         final Font f = new Font("Dialog", 0, 25);
         final JTextField usernameLeft = new JTextField("Username"), usernameTextField = new JTextField(),
               passwordLeft = new JTextField("Password");
         final JPasswordField passwordField = new JPasswordField();
         final JButton loginButton = new JButton("Login"), registerButton = new JButton("Register");
         loginButton.addActionListener(e -> {
            final String usernameString = usernameTextField.getText(),
                  passwordString = GameAction.hashPassword(String.valueOf(passwordField.getPassword()));
            try {
               final ResultSet rs = GameAction.executeQuery(Minesweeper.statement,
                     "SELECT * FROM accounts WHERE user_id=? AND ps=?", usernameString, passwordString);
               if (rs.next()) {
                  GameAction.writeFile("cookie", usernameString);
                  dispose();
                  GameAction.username = usernameString;
                  SwingUtilities.invokeLater(() -> new Minesweeper(true));
               } else
                  JOptionPane.showMessageDialog(null, "Login failed!");
            } catch (final SQLException e1) {
               e1.printStackTrace();
            }
         });
         registerButton.addActionListener(e -> {
            dispose();
            SwingUtilities.invokeLater(() -> new Register());
         });
         cp.setLayout(new GridLayout(3, 2));
         GameAction.setupManyComponent(true, f, usernameLeft, passwordLeft, loginButton, registerButton);
         GameAction.addManyComponent(cp, usernameLeft, usernameTextField, passwordLeft, passwordField, loginButton,
               registerButton);
         GameAction.setupFrame(this, "Login", new Dimension(600, 400));
      }
   }
}