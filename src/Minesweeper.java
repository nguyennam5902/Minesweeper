import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class Minesweeper extends JFrame {
   static boolean firstClick = true, showNewGameEnabled = true;
   private static int currentCustomStreak, boardHeight, boardWidth, boardMines, numMode;
   private static final HashMap<String, Integer> currentCustomTable = new HashMap<>(), maxCustomTable = new HashMap<>();
   private static final int[] wins = new int[4], currentNormalStreak = new int[3], maxNormalStreak = new int[3];
   private static String mode, username = "Guest";
   private static final double[] fastestTime = { 999999, 999999, 999999 };
   private static int[][] gameBoard, answerBoard;

   public static void setCurrentCustomStreak(final int currentCustomStreak) {
      Minesweeper.currentCustomStreak = currentCustomStreak;
   }

   public static int[] getWins() {
      return wins;
   }

   public static int getCurrentCustomStreak() {
      return Minesweeper.currentCustomStreak;
   }

   public static HashMap<String, Integer> getMaxcustomtable() {
      return maxCustomTable;
   }

   public static HashMap<String, Integer> getCurrentCustomTable() {
      return currentCustomTable;
   }

   public static int[] getCurrentNormalStreak() {
      return currentNormalStreak;
   }

   public static int[] getMaxNormalStreak() {
      return maxNormalStreak;
   }

   public static String getMode() {
      return mode;
   }

   public static void setMode(final String mode) {
      Minesweeper.mode = mode;
   }

   public static String getUsername() {
      return username;
   }

   public static void setUsername(final String username) {
      Minesweeper.username = username;
   }

   public static int getBoardHeight() {
      return boardHeight;
   }

   public static void setBoardHeight(final int boardHeight) {
      Minesweeper.boardHeight = boardHeight;
   }

   public static int getBoardWidth() {
      return boardWidth;
   }

   public static void setBoardWidth(final int boardWidth) {
      Minesweeper.boardWidth = boardWidth;
   }

   public static int getBoardMines() {
      return boardMines;
   }

   public static void setBoardMines(final int boardMines) {
      Minesweeper.boardMines = boardMines;
   }

   public static int getNumMode() {
      return numMode;
   }

   public static void setNumMode(final int numMode) {
      Minesweeper.numMode = numMode;
   }

   public static double[] getFastestTime() {
      return fastestTime;
   }

   public static int[][] getGameBoard() {
      return gameBoard;
   }

   private boolean isSetTimer = false;

   private int openCell, clicks;

   private final List<Double>[] time = new ArrayList[3];

   private final int delayMs = 100;

   private JTextField mineCounter, timerField;

   private Cell[][] cellBoard;

   private final Timer timer = new Timer();

   Minesweeper(final boolean firstStart) {
      firstClick = true;
      isSetTimer = false;
      clicks = 0;
      for (int i = 0; i < 3; i++)
         time[i] = new ArrayList<>();
      try {
         Class.forName("org.postgresql.Driver");
         ResultSet rs = QueryHandler.executeQuery(
               "SELECT beginner_db.wins,intermediate_db.wins,expert_db.wins,beginner_db.current_ws,intermediate_db.current_ws,expert_db.current_ws,beginner_db.max_ws,intermediate_db.max_ws,expert_db.max_ws FROM((beginner_db INNER JOIN intermediate_db ON beginner_db.username=intermediate_db.username)INNER JOIN expert_db on beginner_db.username=expert_db.username)WHERE beginner_db.username=?",
               Minesweeper.getUsername());
         if (rs.next()) {
            for (int i = 0; i < 3; i++) {
               wins[i] = rs.getInt(i + 1);
               currentNormalStreak[i] = rs.getInt(4 + i);
               maxNormalStreak[i] = rs.getInt(7 + i);
            }
            for (int i = 0; i < 3; i++) {
               final String column = i == 0 ? "beginner" : i == 1 ? "intermediate" : "expert";
               int tmp = 1;
               while (true) {
                  rs = QueryHandler.executeQuery(
                        "SELECT time[?] from " + column + "_db where username=?",
                        tmp, Minesweeper.getUsername());
                  if (rs.next()) {
                     final double tmpDouble = rs.getDouble(1);
                     if (tmpDouble == 0.0)
                        break;
                     fastestTime[i] = Math.min(fastestTime[i], tmpDouble);
                  }
                  tmp++;
               }
            }
         }
         rs = QueryHandler.executeQuery("SELECT * FROM custom_db where username=?",
               Minesweeper.getUsername());
         if (rs.next()) {
            wins[3] = rs.getInt("custom_wins");
            final String[] currentStreaks = rs.getString("current_streak").split(";");
            for (final String streak : currentStreaks) {
               final int mid = streak.indexOf(":");
               if (mid != -1)
                  currentCustomTable.put(streak.substring(0, mid), Integer.parseInt(streak.substring(mid + 1)));
            }
            final String[] maxStreaks = rs.getString("max_streak").split(";");
            for (final String streak : maxStreaks) {
               final int mid = streak.indexOf(":");
               if (mid != -1)
                  maxCustomTable.put(streak.substring(0, mid), Integer.parseInt(streak.substring(mid + 1)));
            }
         }
      } catch (final Exception e) {
         e.printStackTrace();
      }
      if (firstStart) {
         final Container cp = getContentPane();
         cp.setLayout(new GridLayout(9, 1));
         final JTextField text = new JTextField("Choose a difficulty level: Beginner, Intermediate, Expert or Custom");
         final Font f36 = new Font("Dialog", 0, 36);
         final JButton beginnerButton = new JButton("Beginner - 9 x 9 / 10"),
               intermediateButton = new JButton("Intermediate - 16 x 16 / 40"),
               expertButton = new JButton("Expert - 16 x 30 / 99"), customButton = new JButton("Custom"),
               statisticsButton = new JButton("Statistics"), settingButton = new JButton("Setting"),
               logoutButton = new JButton("Logout"),
               exitButton = new JButton("Exit");
         exitButton.addActionListener(e -> System.exit(0));
         beginnerButton.addActionListener(SetupController.setupButton(this, 0));
         intermediateButton.addActionListener(SetupController.setupButton(this, 1));
         expertButton.addActionListener(SetupController.setupButton(this, 2));
         customButton.addActionListener(SetupController.setupButton(this, cp, f36));
         statisticsButton.addActionListener(e -> Statistics.setupStatisticsFrame(this, cp));
         settingButton.addActionListener(e -> Setting.showSettingFrame(this, cp, f36, exitButton));
         logoutButton.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(null, "Are you sure to log out", "Log out confirmation", 0) == 0) {
               GameAction.writeFile("cookie", "NULL");
               JOptionPane.showMessageDialog(null, "Logout successfully");
               dispose();
               SwingUtilities.invokeLater(() -> new Login());
            }
         });
         SetupController.setupManyComponent(true, f36, text, beginnerButton, intermediateButton, expertButton,
               customButton,
               statisticsButton, settingButton, logoutButton, exitButton);
         SetupController.addManyComponent(cp, text, beginnerButton, intermediateButton, expertButton, customButton,
               statisticsButton, settingButton, logoutButton, exitButton);
         SetupController.setupFrame(this, "Choose game mode", new Dimension(1100, 500));
      } else {
         final int needOpen = Minesweeper.getBoardHeight() * Minesweeper.getBoardWidth() - Minesweeper.getBoardMines();
         final Random r = new Random();
         gameBoard = new int[Minesweeper.getBoardHeight()][Minesweeper.getBoardWidth()];
         answerBoard = new int[Minesweeper.getBoardHeight()][Minesweeper.getBoardWidth()];
         cellBoard = new Cell[Minesweeper.getBoardHeight()][Minesweeper.getBoardWidth()];
         for (int i = 0; i < Minesweeper.getBoardHeight(); i++)
            for (int j = 0; j < Minesweeper.getBoardWidth(); j++) {
               final Cell tmpCell = new Cell(i, j);
               tmpCell.addMouseListener(new MouseAdapter() {
                  boolean pressed;

                  @Override
                  public void mousePressed(final MouseEvent e) {
                     pressed = true;
                  }

                  @Override
                  public void mouseReleased(final MouseEvent e) {
                     if (pressed) {
                        if (firstClick && !isSetTimer) {
                           isSetTimer = true;
                           timer.schedule(new TimerTask() {
                              @Override
                              public void run() {
                                 if (timerField != null && !firstClick)
                                    timerField.setText(String.format("%.1f",
                                          Double.parseDouble(timerField.getText()) + delayMs / 1000.0));
                              }
                           }, 0, delayMs);
                        }
                        if (SwingUtilities.isRightMouseButton(e)) {
                           if (tmpCell.getBackground().equals(Color.white)) {
                              tmpCell.setText("F");
                              tmpCell.setBackground(Color.red);
                              mineCounter.setText((Integer.parseInt(mineCounter.getText()) - 1) + "");
                           } else if (tmpCell.getBackground().equals(Color.red)) {
                              tmpCell.setText("");
                              tmpCell.setBackground(Color.white);
                              mineCounter.setText((Integer.parseInt(mineCounter.getText()) + 1) + "");
                           }
                           firstClick = false;
                        } else {
                           if (tmpCell.getBackground().equals(Color.white)) {
                              final int x = tmpCell.getI(), y = tmpCell.getJ();
                              if (gameBoard[x][y] == 1) {
                                 if (firstClick) {
                                    while (gameBoard[x][y] == 1) {
                                       for (int k = 0; k < Minesweeper.getBoardHeight(); k++)
                                          for (int l = 0; l < Minesweeper.getBoardWidth(); l++)
                                             gameBoard[k][l] = 0;
                                       GameAction.makeBoard(r);
                                    }
                                    GameAction.countMine(gameBoard, answerBoard);
                                    open(gameBoard, answerBoard, x, y);
                                    checkWin(needOpen);
                                 } else {
                                    timer.cancel();
                                    GameAction.updateStreak(false);
                                    tmpCell.setText("*");
                                    if (JOptionPane.showConfirmDialog(null, "You lose! Play again?", "Game over",
                                          0) != 0)
                                       System.exit(0);
                                    else {
                                       dispose();
                                       SwingUtilities.invokeLater(() -> new Minesweeper(true));
                                    }
                                 }
                              } else {
                                 openCell = 0;
                                 open(gameBoard, answerBoard, x, y);
                                 checkWin(needOpen);
                              }
                              firstClick = false;
                           } else if (tmpCell.getBackground().equals(GameConfig.selectedColor)) {
                              final int i = tmpCell.getI(), j = tmpCell.getJ();
                              int redFlagCounter = 0;
                              final int[][] aroundCells = GameAction.getAroundCells(i, j);
                              for (final int[] ks : aroundCells)
                                 if (cellBoard[i + ks[0]][j + ks[1]].getBackground().equals(Color.red))
                                    redFlagCounter++;
                              if (answerBoard[i][j] == redFlagCounter) {
                                 for (final int[] ks : aroundCells) {
                                    if (cellBoard[i + ks[0]][j + ks[1]].getBackground().equals(Color.white))
                                       if (gameBoard[i + ks[0]][j + ks[1]] == 1) {
                                          timer.cancel();
                                          GameAction.updateStreak(false);
                                          cellBoard[i + ks[0]][j + ks[1]].setText("*");
                                          if (JOptionPane.showConfirmDialog(null, "You lose! Play again?", "Game over",
                                                0) != 0)
                                             System.exit(0);
                                          else {
                                             dispose();
                                             SwingUtilities.invokeLater(() -> new Minesweeper(true));
                                          }
                                          System.exit(0);
                                       } else
                                          open(gameBoard, answerBoard, i + ks[0], j + ks[1]);
                                 }
                                 checkWin(needOpen);
                              }
                           }
                        }
                     }
                     clicks++;
                     pressed = false;
                  }
               });
               cellBoard[i][j] = tmpCell;
            }
         GameAction.makeBoard(r);
         GameAction.countMine(gameBoard, answerBoard);
         timerField = new JTextField("0", 15);
         mineCounter = new JTextField("" + Minesweeper.getBoardMines(), 10);
         final Container cp = getContentPane();
         cp.setLayout(new BorderLayout());
         final JPanel header = new JPanel(),
               board = new JPanel(new GridLayout(Minesweeper.getBoardHeight(), Minesweeper.getBoardWidth())),
               westPanel = new JPanel(new GridLayout(4, 1));
         final JTextField textField1 = new JTextField("Mines: ", 6), textField2 = new JTextField("Timer: ", 12);
         final JButton newGameButton = new JButton("New game?"), statisticsButton = new JButton("Statistics"),
               beginnerButton = new JButton("Beginner"), intermediateButton = new JButton("Intermediate"),
               expertButton = new JButton("Expert"), customButton = new JButton("Custom");
         newGameButton.setBackground(Color.green);
         newGameButton.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(null, "Do you want to start a new game?", "Notification", 0) == 0) {
               if (firstClick == false)
                  GameAction.updateStreak(false);
               timerField.setText("0");
               firstClick = true;
               for (int k = 0; k < Minesweeper.getBoardHeight(); k++)
                  for (int l = 0; l < Minesweeper.getBoardWidth(); l++) {
                     cellBoard[k][l].setText("");
                     cellBoard[k][l].setBackground(Color.white);
                     gameBoard[k][l] = 0;
                     answerBoard[k][l] = 0;
                  }
               GameAction.makeBoard(r);
               GameAction.countMine(gameBoard, answerBoard);
            }
         });
         beginnerButton.addActionListener(SetupController.setupButton(this, 0));
         intermediateButton.addActionListener(SetupController.setupButton(this, 1));
         expertButton.addActionListener(SetupController.setupButton(this, 2));
         customButton.addActionListener(SetupController.setupButton(this, cp, new Font("Dialog", 0, 20)));
         statisticsButton.addActionListener(e -> Statistics.setupStatisticsFrame(this, cp));
         cp.add(header, BorderLayout.NORTH);
         SetupController.setupManyComponent(false, null, textField1, textField2, timerField, mineCounter);
         if (showNewGameEnabled)
            SetupController.addManyComponent(header, newGameButton);
         SetupController.addManyComponent(header, textField1, mineCounter, textField2, timerField, statisticsButton);
         SetupController.addManyComponent(westPanel, beginnerButton, intermediateButton, expertButton, customButton);
         for (int i = 0; i < Minesweeper.getBoardHeight(); i++)
            for (int j = 0; j < Minesweeper.getBoardWidth(); j++)
               board.add(cellBoard[i][j]);
         cp.add(board, BorderLayout.CENTER);
         if (showNewGameEnabled)
            cp.add(westPanel, BorderLayout.WEST);
         SetupController.setupFrame(this, "Minesweeper");
      }
   }

   private void open(final int[][] board, final int[][] answer, final int i, final int j) {
      if (i < 0 || i >= Minesweeper.getBoardHeight() || j < 0 || j >= Minesweeper.getBoardWidth() || board[i][j] == 1)
         return;
      if (answer[i][j] != 0) {
         cellBoard[i][j].setValue(answer[i][j]);
         cellBoard[i][j].setBackground(GameConfig.selectedColor);
         return;
      }
      if (cellBoard[i][j].getText().equals(answer[i][j] + ""))
         return;
      cellBoard[i][j].setValue(answer[i][j]);
      cellBoard[i][j].setBackground(GameConfig.selectedColor);
      for (final int[] aroundCells : GameAction.getAroundCells(i, j))
         open(board, answer, i + aroundCells[0], j + aroundCells[1]);
   }

   private void checkWin(final int needOpen) {
      openCell = 0;
      for (int k = 0; k < Minesweeper.getBoardHeight(); k++)
         for (int l = 0; l < Minesweeper.getBoardWidth(); l++)
            if (GameAction.isInteger(cellBoard[k][l].getText()))
               openCell++;
      if (openCell == needOpen) {
         final double playTime = timerField.getText().equals("") ? 0.0 : Double.parseDouble(timerField.getText());
         if (Minesweeper.getNumMode() != 3)
            QueryHandler.executeUpdate(
                  "UPDATE " + Minesweeper.getMode().toLowerCase() + "_db SET time=ARRAY_APPEND(time,?)where username=?",
                  playTime, Minesweeper.getUsername());
         timer.cancel();
         GameAction.updateStreak(true);
         if (JOptionPane.showConfirmDialog(null,
               Minesweeper.getMode() + " - " + playTime + " s\nWin streak: "
                     + (Minesweeper.getNumMode() == 3 ? currentCustomStreak
                           : currentNormalStreak[Minesweeper.getNumMode()])
                     + "\nClicks: " + clicks + "\nPlay again?",
               "You win!", 0) != 0)
            System.exit(0);
         else {
            dispose();
            SwingUtilities.invokeLater(() -> new Minesweeper(true));
         }
      }
   }

   // private void makeBoard(final Random r) {
   // for (int i = 0; i < Minesweeper.getBoardMines(); i++) {
   // final int x = r.nextInt(0, Minesweeper.getBoardHeight()), y = r.nextInt(0,
   // Minesweeper.getBoardWidth());
   // if (gameBoard[x][y] == 0)
   // gameBoard[x][y] = 1;
   // else
   // i--;
   // }
   // }
}