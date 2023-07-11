import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class Setting extends JFrame {
   public static void showSettingFrame(JFrame parent, final Container cp, final Font f36, final JButton exitButton) {
      parent.dispose();
      final Font myFont = new Font("Dialog", 0, 25);
      final JFrame settingFrame = new JFrame();
      settingFrame.setLayout(new BorderLayout());
      final JTextField usernameLeft = new JTextField("Username"),
            usernameRight = new JTextField(Minesweeper.getUsername()),
            showNewGame = new JTextField("Show 'New game' buttons"), passwordLeft = new JTextField("Password");
      final JRadioButton showNewGameButton = new JRadioButton("Show", Minesweeper.showNewGameEnabled),
            notShowNewGameButton = new JRadioButton("Do not show", !Minesweeper.showNewGameEnabled);
      final JButton changeUsernameButton = new JButton("Change username"),
            changePasswordButton = new JButton("Change password"), newGameButton = new JButton("New game");
      final ButtonGroup g = new ButtonGroup();
      changeUsernameButton.addActionListener(e1 -> {
         settingFrame.dispose();
         final JFrame changeUsernameFrame = new JFrame();
         changeUsernameFrame.setLayout(new BorderLayout());
         final JPanel center = new JPanel(new GridLayout(1, 2)), bottom = new JPanel();
         final JButton setUsernameButton = new JButton("Change username"), goBackButton = new JButton("Go back");
         final JTextField newUsernameLeft = new JTextField("New username"), newUsernameRight = new JTextField();
         setUsernameButton.addActionListener(e2 -> {
            final String newUsername = newUsernameRight.getText().trim();
            if (newUsername.equals(Minesweeper.getUsername())) {
               JOptionPane.showMessageDialog(null, "New username must not the same as old username!!");
            } else {
               final ResultSet rs = QueryHandler.executeQuery("SELECT * FROM accounts where user_id = ?",
                     newUsername);
               try {
                  if (rs.next()) {
                     JOptionPane.showMessageDialog(null, "Username existed! Please choose another username!");
                  } else {
                     GameAction.writeFile("cookie", newUsername);
                     QueryHandler.executeUpdate("UPDATE accounts SET user_id=? where user_id=?", newUsername, Minesweeper.getUsername());
                     QueryHandler.executeUpdate("UPDATE beginner_db SET username=? where username=?", newUsername, Minesweeper.getUsername());
                     QueryHandler.executeUpdate("UPDATE intermediate_db SET username=? where username=?", newUsername, Minesweeper.getUsername());
                     QueryHandler.executeUpdate("UPDATE expert_db SET username=? where username=?", newUsername, Minesweeper.getUsername());
                     QueryHandler.executeUpdate("UPDATE custom_db SET username=? where username=?", newUsername, Minesweeper.getUsername());
                     QueryHandler.executeUpdate("UPDATE game_data SET username=? where username=?", newUsername, Minesweeper.getUsername());
                     Minesweeper.setUsername(newUsername);
                     JOptionPane.showMessageDialog(null, "Username changed " + newUsername + " !");
                     changeUsernameFrame.dispose();
                     new Minesweeper(true);
                  }
               } catch (final SQLException e) {
                  e.printStackTrace();
               }
            }
         });
         goBackButton.addActionListener(e2 -> {
            changeUsernameFrame.dispose();
            showSettingFrame(parent, cp, f36, exitButton);
         });
         SetupController.setupManyComponent(true, myFont, newUsernameLeft, setUsernameButton, goBackButton);
         SetupController.addManyComponent(center, newUsernameLeft, newUsernameRight);
         SetupController.addManyComponent(bottom, setUsernameButton, goBackButton);
         changeUsernameFrame.add(center, BorderLayout.CENTER);
         changeUsernameFrame.add(bottom, BorderLayout.SOUTH);
         SetupController.setupFrame(changeUsernameFrame, "Change username", new Dimension(500, 200));
      });
      changePasswordButton.addActionListener(e1 -> {
         settingFrame.dispose();
         final JFrame tmpFrame = new JFrame();
         tmpFrame.setLayout(new GridLayout(4, 2));
         final JTextField oldPasswordLeft = new JTextField("Old password"),
               newPasswordLeft = new JTextField("New password"),
               confirmPassWordLeft = new JTextField("Confirm password");
         final JPasswordField oldPasswordRight = new JPasswordField(), newPasswordRight = new JPasswordField(),
               confirmPasswordRight = new JPasswordField();
         final JButton setPasswordButton = new JButton("Change password"),
               goBackButton = new JButton("Go back");
         goBackButton.addActionListener(e2 -> {
            tmpFrame.dispose();
            showSettingFrame(parent, cp, f36, exitButton);
         });
         setPasswordButton.addActionListener(e2 -> {
            try {
               final ResultSet rs = QueryHandler.executeQuery("SELECT ps FROM accounts where user_id=?",
                     Minesweeper.getUsername());
               if (rs.next()) {
                  final String newPassword = new String(newPasswordRight.getPassword()),
                        oldPassword = GameAction.hashPassword(new String(oldPasswordRight.getPassword()));
                  if (newPassword.equals(""))
                     JOptionPane.showMessageDialog(null, "Please fill on 'New password' field!");
                  else if (rs.getString("ps").equals(oldPassword)) {
                     if (newPassword.equals(new String(confirmPasswordRight.getPassword()))) {
                        QueryHandler.executeUpdate("UPDATE accounts SET ps=? WHERE user_id=?",
                              GameAction.hashPassword(newPassword), Minesweeper.getUsername());
                        JOptionPane.showMessageDialog(null, "Password changed!");
                        tmpFrame.dispose();
                        SwingUtilities.invokeLater(() -> new Login());
                     } else
                        JOptionPane.showMessageDialog(null,
                              "'New password' field is not the same as 'Confirm password' field!");
                  } else
                     JOptionPane.showMessageDialog(null, "Old password did not match!");
               }
            } catch (final SQLException sql) {
               sql.printStackTrace();
            }
         });
         SetupController.setupManyComponent(true, myFont, oldPasswordLeft, newPasswordLeft, confirmPassWordLeft,
               goBackButton, setPasswordButton);
         SetupController.addManyComponent(tmpFrame, oldPasswordLeft, oldPasswordRight, newPasswordLeft,
               newPasswordRight,
               confirmPassWordLeft, confirmPasswordRight, setPasswordButton, goBackButton);
         SetupController.setupFrame(tmpFrame, "Change password", new Dimension(800, 400));
      });
      final JPanel bottom = new JPanel(), center = new JPanel(new GridLayout(3, 2)),
            showNewGamePanel = new JPanel(new GridLayout(2, 1)), changeUsernamePanel = new JPanel(new GridLayout(2, 1));
      g.add(showNewGameButton);
      g.add(notShowNewGameButton);
      showNewGameButton.addActionListener(e1 -> Minesweeper.showNewGameEnabled = true);
      notShowNewGameButton.addActionListener(e1 -> Minesweeper.showNewGameEnabled = false);
      newGameButton.addActionListener(e1 -> {
         settingFrame.dispose();
         new Minesweeper(true);
      });
      SetupController.setupManyComponent(true, f36, usernameLeft, newGameButton, usernameRight, changePasswordButton,
            showNewGame, showNewGameButton, changeUsernameButton, notShowNewGameButton, passwordLeft);
      SetupController.addManyComponent(bottom, newGameButton, exitButton);
      SetupController.addManyComponent(showNewGamePanel, showNewGameButton, notShowNewGameButton);
      SetupController.addManyComponent(changeUsernamePanel, usernameRight, changeUsernameButton);
      SetupController.addManyComponent(center, usernameLeft, changeUsernamePanel, passwordLeft, changePasswordButton,
            showNewGame, showNewGamePanel);
      settingFrame.add(center, BorderLayout.CENTER);
      settingFrame.add(bottom, BorderLayout.SOUTH);
      SetupController.setupFrame(settingFrame, "Setting", new Dimension(1000, 500));
   }
}
