import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class GameAction {
   static Connection conn;
   static String mode, username = "Guest";
   static int height, width, mines, numMode;

   public static void setupFrame(final JFrame frame, final String title, final Dimension d) {
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.setTitle(title);
      frame.setSize(d);
      frame.setLocationRelativeTo(null);
      frame.setVisible(true);
   }

   public static void setupFrame(final JFrame frame, final String title) {
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.setTitle(title);
      frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
      frame.setVisible(true);
   }

   public static String hashPassword(final String password) {
      try {
         final MessageDigest md = MessageDigest.getInstance("SHA-256");
         md.update(password.getBytes());
         final byte[] bytes = md.digest();
         final StringBuilder sb = new StringBuilder();
         for (final byte b : bytes)
            sb.append(String.format("%02x", b));
         return sb.toString();
      } catch (final NoSuchAlgorithmException e) {
         return null;
      }
   }

   public static void addManyComponent(final Container cp, final Component... components) {
      for (final Component component : components)
         cp.add(component);
   }

   public static void setupManyComponent(final boolean needToSetFont, final Font f, final JComponent... components) {
      for (final JComponent component : components) {
         if (needToSetFont) {
            component.setFont(f);
         }
         if (component instanceof JRadioButton)
            ((JRadioButton) component).setHorizontalAlignment(SwingConstants.CENTER);
         else if (component instanceof JTextField) {
            final JTextField tmp = (JTextField) component;
            tmp.setHorizontalAlignment(SwingConstants.CENTER);
            tmp.setEditable(false);
         }
      }
   }

   public static void writeFile(final String fileName, final String text) {
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
         writer.write(text);
      } catch (final IOException ioe) {
         System.err.println("Error writing to file: " + ioe.getMessage());
      }
   }

   public static ActionListener setupButton(final JFrame frame, final Container cp, final Font f) {
      return e -> {
         frame.dispose();
         cp.removeAll();
         cp.setLayout(new GridLayout(4, 2));
         final JTextField heightText = new JTextField("Height:"), widthText = new JTextField("Width:"),
               mineText = new JTextField("Mines:"), heightAns = new JTextField(), widthAns = new JTextField(),
               mineAns = new JTextField();
         final JButton playBtn = new JButton("Play"), backBtn = new JButton("Go back");
         playBtn.addActionListener(e1 -> {
            boolean error = false;
            try {
               height = Integer.parseInt(heightAns.getText());
               width = Integer.parseInt(widthAns.getText());
               mines = Integer.parseInt(mineAns.getText());
            } catch (final Exception e2) {
               error = true;
               JOptionPane.showMessageDialog(null, "ERROR:Input is not an positive integer");
            }
            if (!error) {
               if (mines < height * width && width < 39 && height < 51 && width > 1 && height > 1) {
                  frame.dispose();
                  mode = "Custom";
                  numMode = 3;
                  SwingUtilities.invokeLater(() -> new Minesweeper(false));
               } else
                  JOptionPane.showMessageDialog(null, "Cannot make board " + height + " x " + width + "/" + mines);
            }
         });
         backBtn.addActionListener(e1 -> {
            frame.dispose();
            SwingUtilities.invokeLater(() -> new Minesweeper(true));
         });
         setupManyComponent(true, f, heightText, widthText, mineText, playBtn, backBtn);
         addManyComponent(cp, heightText, heightAns, widthText, widthAns, mineText, mineAns, playBtn, backBtn);
         frame.setSize(1100, 500);
         frame.setTitle("Custom");
         frame.setVisible(true);
      };
   }

   public static ActionListener setupButton(final JFrame f, final int numMode) {
      return e -> {
         if (!Minesweeper.firstClick) {
            if (JOptionPane.showConfirmDialog(null, "Do you want to start a new game?", "Notification", 0) == 0) {
               if (Minesweeper.firstClick == false) {
                  final Minesweeper m = new Minesweeper(false);
                  m.dispose();
                  m.updateStreak(false);
               }
               setupBoard(f, numMode);
            }
         } else
            setupBoard(f, numMode);
      };
   }

   public static void executeUpdate(PreparedStatement s, final String sql, final Object... params) {
      try {
         s = conn.prepareStatement(sql);
         setParameters(s, params);
         s.executeUpdate();
      } catch (final SQLException e) {
         System.out.println(sql);
         e.printStackTrace();
      }
   }

   public static ResultSet executeQuery(PreparedStatement s, final String sql, final Object... params) {
      try {
         s = conn.prepareStatement(sql);
         setParameters(s, params);
         return s.executeQuery();
      } catch (final SQLException e) {
         System.out.println(sql);
         e.printStackTrace();
         return null;
      }
   }

   public static void setParameters(final PreparedStatement stmt, final Object... parameters) throws SQLException {
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

   public static boolean isInteger(final String str) {
      if (str == null)
         return false;
      final int length = str.length();
      if (length == 0)
         return false;
      int i = 0;
      if (str.charAt(0) == '-') {
         if (length == 1)
            return false;
         i = 1;
      }
      for (; i < length; i++) {
         final char c = str.charAt(i);
         if (c < '0' || c > '9')
            return false;
      }
      return true;
   }

   public static int[][] getAroundCells(final int i, final int j) {
      if (i == 0 && j == 0)
         return new int[][] { { 1, 0 }, { 0, 1 }, { 1, 1 } };
      else if (i == 0 && j == width - 1)
         return new int[][] { { 0, -1 }, { 1, -1 }, { 1, 0 } };
      else if (i == height - 1 && j == 0)
         return new int[][] { { -1, 0 }, { -1, 1 }, { 0, 1 } };
      else if (i == height - 1 && j == width - 1)
         return new int[][] { { -1, -1 }, { 0, -1 }, { -1, 0 } };
      else if (i == 0 && j < width - 1)
         return new int[][] { { 0, -1 }, { 0, 1 }, { 1, -1 }, { 1, 0 }, { 1, 1 } };
      else if (i == height - 1 && j < width - 1)
         return new int[][] { { 0, -1 }, { 0, 1 }, { -1, -1 }, { -1, 0 }, { -1, 1 } };
      else if (j == 0 && i < height - 1)
         return new int[][] { { -1, 0 }, { 1, 0 }, { -1, 1 }, { 0, 1 }, { 1, 1 } };
      else if (j == width - 1 && i < height - 1)
         return new int[][] { { -1, 0 }, { 1, 0 }, { -1, -1 }, { 0, -1 }, { 1, -1 } };
      else
         return new int[][] { { -1, -1 }, { -1, 0 }, { -1, 1 }, { 0, -1 }, { 0, 1 }, { 1, -1 }, { 1, 0 }, { 1, 1 } };
   }

   public static void countMine(final int[][] board, final int[][] answer) {
      for (int i = 0; i < height; i++)
         for (int j = 0; j < width; j++) {
            answer[i][j] = 0;
            final int[][] aroundCells = getAroundCells(i, j);
            for (final int[] aroundCell : aroundCells)
               answer[i][j] += board[i + aroundCell[0]][j + aroundCell[1]];
         }
   }

   public static int calculateMastery(final int modeIndex) {
      int win = 0;
      try {
         final ResultSet tmp = executeQuery(Minesweeper.statement,
               "SELECT * FROM game_data WHERE username=? AND game_mode=? ORDER BY date_time DESC LIMIT 100", username,
               modeIndex);
         while (tmp.next())
            if (tmp.getBoolean("is_win"))
               win++;
      } catch (final SQLException e) {
         e.printStackTrace();
      }
      return win;
   }

   private static void setupBoard(final JFrame f, final int numMode) {
      height = numMode == 0 ? 9 : 16;
      width = numMode == 0 ? 9 : numMode == 1 ? 16 : 30;
      mines = numMode == 0 ? 10 : numMode == 1 ? 40 : 99;
      mode = numMode == 0 ? "Beginner" : numMode == 1 ? "Intermediate" : "Expert";
      GameAction.numMode = numMode;
      f.dispose();
      SwingUtilities.invokeLater(() -> new Minesweeper(false));
   }
}