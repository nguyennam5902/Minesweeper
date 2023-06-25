import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class Register extends JFrame {
   public Register() {
      final Container cp = getContentPane();
      cp.setLayout(new GridLayout(4, 2));
      final Font f = new Font("Dialog", 0, 30);
      final JTextField usernameLeft = new JTextField("Username"), usernameRight = new JTextField(),
            passwordLeft = new JTextField("Password"), confirmPasswordLeft = new JTextField("Confirm");
      final JPasswordField passwordRight = new JPasswordField(), confirmPasswordRight = new JPasswordField();
      final JButton registerButton = new JButton("Register"), loginButton = new JButton("Login");
      registerButton.addActionListener(e -> {
         final String upPassword = String.valueOf(passwordRight.getPassword());
         if (!upPassword.equals(new String(confirmPasswordRight.getPassword()))
               || usernameRight.getText().equals(""))
            JOptionPane.showMessageDialog(null, "Register failed!");
         else {
            try {
               Class.forName("org.postgresql.Driver");
               final ResultSet rs = GameAction.executeQuery(Minesweeper.statement,
                     "SELECT * FROM accounts WHERE user_id=?",
                     usernameRight.getText());
               if (rs.next())
                  JOptionPane.showMessageDialog(null, "Username existed! Please choose another username");
               else {
                  GameAction.executeUpdate(Minesweeper.statement, "insert into accounts values(?,?);",
                        usernameRight.getText(), GameAction.hashPassword(upPassword));
                  GameAction.executeUpdate(Minesweeper.statement,
                        "insert into beginner_db values(?,ARRAY[]::double precision[],0,0,0);",
                        usernameRight.getText());
                  GameAction.executeUpdate(Minesweeper.statement,
                        "insert into intermediate_db values(?,ARRAY[]::double precision[],0,0,0);",
                        usernameRight.getText());
                  GameAction.executeUpdate(Minesweeper.statement,
                        "insert into expert_db values(?,ARRAY[]::double precision[],0,0,0);", usernameRight.getText());
                  GameAction.executeUpdate(Minesweeper.statement, "insert into custom_db values(?,0,'','');",
                        usernameRight.getText());
                  JOptionPane.showMessageDialog(null, "Register succeed");
                  dispose();
                  SwingUtilities.invokeLater(() -> new Login());
               }
            } catch (ClassNotFoundException | SQLException e1) {
               e1.printStackTrace();
            }
         }
      });
      loginButton.addActionListener(e -> {
         dispose();
         SwingUtilities.invokeLater(() -> new Login());
      });
      GameAction.setupManyComponent(true, f, usernameLeft, passwordLeft, confirmPasswordLeft, registerButton,
            loginButton);
      GameAction.addManyComponent(cp, usernameLeft, usernameRight, passwordLeft, passwordRight, confirmPasswordLeft,
            confirmPasswordRight, registerButton, loginButton);
      GameAction.setupFrame(this, "Register", new Dimension(600, 400));
   }
}