import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;

public class GameAction {
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

   public static void writeFile(final String fileName, final String text) {
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
         writer.write(text);
      } catch (final IOException ioe) {
         System.err.println("Error writing to file: " + ioe.getMessage());
      }
   }

   public static void makeBoard(final Random r) {
      for (int i = 0; i < Minesweeper.getBoardMines(); i++) {
         final int x = r.nextInt(0, Minesweeper.getBoardHeight()), y = r.nextInt(0, Minesweeper.getBoardWidth());
         if (Minesweeper.getGameBoard()[x][y] == 0)
            Minesweeper.getGameBoard()[x][y] = 1;
         else
            i--;
      }
   }

   public static int[][] getAroundCells(final int i, final int j) {
      if (i == 0 && j == 0)
         return new int[][] { { 1, 0 }, { 0, 1 }, { 1, 1 } };
      else if (i == 0 && j == Minesweeper.getBoardWidth() - 1)
         return new int[][] { { 0, -1 }, { 1, -1 }, { 1, 0 } };
      else if (i == Minesweeper.getBoardHeight() - 1 && j == 0)
         return new int[][] { { -1, 0 }, { -1, 1 }, { 0, 1 } };
      else if (i == Minesweeper.getBoardHeight() - 1 && j == Minesweeper.getBoardWidth() - 1)
         return new int[][] { { -1, -1 }, { 0, -1 }, { -1, 0 } };
      else if (i == 0 && j < Minesweeper.getBoardWidth() - 1)
         return new int[][] { { 0, -1 }, { 0, 1 }, { 1, -1 }, { 1, 0 }, { 1, 1 } };
      else if (i == Minesweeper.getBoardHeight() - 1 && j < Minesweeper.getBoardWidth() - 1)
         return new int[][] { { 0, -1 }, { 0, 1 }, { -1, -1 }, { -1, 0 }, { -1, 1 } };
      else if (j == 0 && i < Minesweeper.getBoardHeight() - 1)
         return new int[][] { { -1, 0 }, { 1, 0 }, { -1, 1 }, { 0, 1 }, { 1, 1 } };
      else if (j == Minesweeper.getBoardWidth() - 1 && i < Minesweeper.getBoardHeight() - 1)
         return new int[][] { { -1, 0 }, { 1, 0 }, { -1, -1 }, { 0, -1 }, { 1, -1 } };
      else
         return new int[][] { { -1, -1 }, { -1, 0 }, { -1, 1 }, { 0, -1 }, { 0, 1 }, { 1, -1 }, { 1, 0 }, { 1, 1 } };
   }

   public static void countMine(final int[][] board, final int[][] answer) {
      for (int i = 0; i < Minesweeper.getBoardHeight(); i++)
         for (int j = 0; j < Minesweeper.getBoardWidth(); j++) {
            answer[i][j] = 0;
            final int[][] aroundCells = getAroundCells(i, j);
            for (final int[] aroundCell : aroundCells)
               answer[i][j] += board[i + aroundCell[0]][j + aroundCell[1]];
         }
   }

   public static int calculateMastery(final int modeIndex) {
      int win = 0;
      try {
         final ResultSet tmp = QueryHandler.executeQuery(
               "SELECT * FROM game_data WHERE username=? AND game_mode=? ORDER BY date_time DESC LIMIT 100",
               Minesweeper.getUsername(),
               modeIndex);
         while (tmp.next())
            if (tmp.getBoolean("is_win"))
               win++;
      } catch (final SQLException e) {
         e.printStackTrace();
      }
      return win;
   }

   public static void updateStreak(final boolean isWin) {
      if (isWin) {
         final String tmpMode = Minesweeper.getNumMode() == 3 ? "custom_" : "";
         QueryHandler.executeUpdate(
               "UPDATE " + Minesweeper.getMode().toLowerCase() + "_db SET " + tmpMode + "wins="
                     + tmpMode + "wins+1 WHERE username=?",
               Minesweeper.getUsername());
      }
      if (Minesweeper.getNumMode() == 3) {
         final String board = Minesweeper.getBoardHeight() + "x" + Minesweeper.getBoardWidth() + "x"
               + Minesweeper.getBoardMines();
         HashMap<String, Integer> currentCustomTable = Minesweeper.getCurrentCustomTable();
         Minesweeper.setCurrentCustomStreak(currentCustomTable.get(board) == null ? 0 : currentCustomTable.get(board));
         Minesweeper.setCurrentCustomStreak(isWin ? Minesweeper.getCurrentCustomStreak() + 1 : 0);
         HashMap<String, Integer> maxCustomTable = Minesweeper.getMaxcustomtable();
         if (Minesweeper.getCurrentCustomStreak() != 0) {
            currentCustomTable.put(board, Minesweeper.getCurrentCustomStreak());
            final int maxStreak = maxCustomTable.get(board) == null ? 1 : maxCustomTable.get(board);
            maxCustomTable.put(board, Math.max(Minesweeper.getCurrentCustomStreak(), maxStreak));
         } else
            currentCustomTable.remove(board);
         final Set<String> currentCustomStreakSet = currentCustomTable.keySet(),
               maxCustomStreakSet = maxCustomTable.keySet();
         final StringBuffer currentStreak = new StringBuffer(), maxStreak = new StringBuffer();
         for (final String string : currentCustomStreakSet)
            currentStreak.append(string + ":" + currentCustomTable.get(string) + ";");
         for (final String string : maxCustomStreakSet)
            maxStreak.append(string + ":" + maxCustomTable.get(string) + ";");
         QueryHandler.executeUpdate(
               "UPDATE custom_db SET max_streak=?,current_streak=? WHERE username=?", maxStreak.toString(),
               currentStreak.toString(), Minesweeper.getUsername());
      } else {
         Minesweeper.getCurrentNormalStreak()[Minesweeper.getNumMode()] = isWin
               ? Minesweeper.getCurrentNormalStreak()[Minesweeper.getNumMode()] + 1
               : 0;

         Minesweeper.getMaxNormalStreak()[Minesweeper.getNumMode()] = Math.max(
               Minesweeper.getCurrentNormalStreak()[Minesweeper.getNumMode()],
               Minesweeper.getMaxNormalStreak()[Minesweeper.getNumMode()]);
         QueryHandler.executeUpdate(
               "UPDATE " + Minesweeper.getMode().toLowerCase() + "_db SET max_ws=?,current_ws=? WHERE username=?;",
               Minesweeper.getMaxNormalStreak()[Minesweeper.getNumMode()],
               Minesweeper.getCurrentNormalStreak()[Minesweeper.getNumMode()],
               Minesweeper.getUsername());
         QueryHandler.executeUpdate("INSERT INTO game_data VALUES(?,?,?,?);", Minesweeper.getUsername(),
               Minesweeper.getNumMode(), isWin,
               LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
      }
   }

}