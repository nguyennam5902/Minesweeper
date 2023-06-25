import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class Minesweeper extends JFrame {
   static boolean firstClick = true;
   static PreparedStatement statement;
   static boolean showNewGameEnabled = true;
   private boolean isSetTimer = false;
   private final Color selectedColor = Color.green;
   private int currentCustomStreak, openCell, clicks;
   private final List<Double>[] time = new ArrayList[3];
   private final HashMap<String, Integer> currentCustomTable = new HashMap<>(), maxCustomTable = new HashMap<>();
   private final int delayMs = 100;
   private final int[] wins = new int[4], currentNormalStreak = new int[3], maxNormalStreak = new int[3];
   private final double[] fastestTime = { 999999, 999999, 999999 };
   private JTextField mineCounter, timerField;
   private int[][] gameBoard, answerBoard;
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
         ResultSet rs = GameAction.executeQuery(statement,
               "SELECT beginner_db.wins,intermediate_db.wins,expert_db.wins,beginner_db.current_ws,intermediate_db.current_ws,expert_db.current_ws,beginner_db.max_ws,intermediate_db.max_ws,expert_db.max_ws FROM((beginner_db INNER JOIN intermediate_db ON beginner_db.username=intermediate_db.username)INNER JOIN expert_db on beginner_db.username=expert_db.username)WHERE beginner_db.username=?",
               GameAction.username);
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
                  rs = GameAction.executeQuery(statement,
                        "SELECT time[?] from " + column + "_db where username=?",
                        tmp, GameAction.username);
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
         rs = GameAction.executeQuery(statement, "SELECT * FROM custom_db where username=?",
               GameAction.username);
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
         beginnerButton.addActionListener(GameAction.setupButton(this, 0));
         intermediateButton.addActionListener(GameAction.setupButton(this, 1));
         expertButton.addActionListener(GameAction.setupButton(this, 2));
         customButton.addActionListener(GameAction.setupButton(this, cp, f36));
         statisticsButton.addActionListener(e -> statisticsFrame(cp));
         settingButton.addActionListener(e -> showSettingFrame(cp, f36, exitButton));
         logoutButton.addActionListener(e -> {
            GameAction.writeFile("cookie", "NULL");
            JOptionPane.showMessageDialog(null, "Logout successfully");
            dispose();
            SwingUtilities.invokeLater(() -> new Login());
         });
         GameAction.setupManyComponent(true, f36, text, beginnerButton, intermediateButton, expertButton, customButton,
               statisticsButton, settingButton, logoutButton, exitButton);
         GameAction.addManyComponent(cp, text, beginnerButton, intermediateButton, expertButton, customButton,
               statisticsButton, settingButton, logoutButton, exitButton);
         GameAction.setupFrame(this, "Choose game mode", new Dimension(1100, 500));
      } else {
         final int needOpen = GameAction.height * GameAction.width - GameAction.mines;
         final Random r = new Random();
         gameBoard = new int[GameAction.height][GameAction.width];
         answerBoard = new int[GameAction.height][GameAction.width];
         cellBoard = new Cell[GameAction.height][GameAction.width];
         for (int i = 0; i < GameAction.height; i++)
            for (int j = 0; j < GameAction.width; j++) {
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
                                       for (int k = 0; k < GameAction.height; k++)
                                          for (int l = 0; l < GameAction.width; l++)
                                             gameBoard[k][l] = 0;
                                       makeBoard(r);
                                    }
                                    GameAction.countMine(gameBoard, answerBoard);
                                    open(gameBoard, answerBoard, x, y);
                                    checkWin(needOpen);
                                 } else {
                                    timer.cancel();
                                    updateStreak(false);
                                    tmpCell.setText("*");
                                    final int result = JOptionPane.showConfirmDialog(null, "You lose! Play again?",
                                          "Game over", 0);
                                    if (result != 0)
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
                           } else if (tmpCell.getBackground().equals(selectedColor)) {
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
                                          updateStreak(false);
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
         makeBoard(r);
         GameAction.countMine(gameBoard, answerBoard);
         timerField = new JTextField("0", 15);
         mineCounter = new JTextField("" + GameAction.mines, 10);
         final Container cp = getContentPane();
         cp.setLayout(new BorderLayout());
         final JPanel header = new JPanel(), board = new JPanel(new GridLayout(GameAction.height, GameAction.width)),
               westPanel = new JPanel(new GridLayout(4, 1));
         final JTextField textField1 = new JTextField("Mines: ", 6), textField2 = new JTextField("Timer: ", 12);
         final JButton newGameButton = new JButton("New game?"), statisticsButton = new JButton("Statistics"),
               beginnerButton = new JButton("Beginner"), intermediateButton = new JButton("Intermediate"),
               expertButton = new JButton("Expert"), customButton = new JButton("Custom");
         newGameButton.setBackground(Color.green);
         newGameButton.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(null, "Do you want to start a new game?", "Notification", 0) == 0) {
               if (firstClick == false)
                  updateStreak(false);
               timerField.setText("0");
               firstClick = true;
               for (int k = 0; k < GameAction.height; k++)
                  for (int l = 0; l < GameAction.width; l++) {
                     cellBoard[k][l].setText("");
                     cellBoard[k][l].setBackground(Color.white);
                     gameBoard[k][l] = 0;
                     answerBoard[k][l] = 0;
                  }
               makeBoard(r);
               GameAction.countMine(gameBoard, answerBoard);
            }
         });
         beginnerButton.addActionListener(GameAction.setupButton(this, 0));
         intermediateButton.addActionListener(GameAction.setupButton(this, 1));
         expertButton.addActionListener(GameAction.setupButton(this, 2));
         customButton.addActionListener(GameAction.setupButton(this, cp, new Font("Dialog", 0, 20)));
         statisticsButton.addActionListener(e -> statisticsFrame(cp));
         cp.add(header, BorderLayout.NORTH);
         GameAction.setupManyComponent(false, null, textField1, textField2, timerField, mineCounter);
         if (showNewGameEnabled)
            GameAction.addManyComponent(header, newGameButton);
         GameAction.addManyComponent(header, textField1, mineCounter, textField2, timerField, statisticsButton);
         GameAction.addManyComponent(westPanel, beginnerButton, intermediateButton, expertButton, customButton);
         for (int i = 0; i < GameAction.height; i++)
            for (int j = 0; j < GameAction.width; j++)
               board.add(cellBoard[i][j]);
         cp.add(board, BorderLayout.CENTER);
         if (showNewGameEnabled)
            cp.add(westPanel, BorderLayout.WEST);
         GameAction.setupFrame(this, "Minesweeper");
      }
   }

   private void showSettingFrame(final Container cp, final Font f36, final JButton exitButton) {
      dispose();
      final Font myFont = new Font("Dialog", 0, 25);
      JFrame settingFrame = new JFrame();
      settingFrame.setLayout(new BorderLayout());
      JTextField usernameLeft = new JTextField("Username"), usernameRight = new JTextField(GameAction.username),
            showNewGame = new JTextField("Show 'New game' buttons"), passwordLeft = new JTextField("Password");
      JRadioButton showNewGameButton = new JRadioButton("Show", showNewGameEnabled),
            notShowNewGameButton = new JRadioButton("Do not show", !showNewGameEnabled);
      JButton changeUsernameButton = new JButton("Change username"),
            changePasswordButton = new JButton("Change password"), newGameButton = new JButton("New game");
      ButtonGroup g = new ButtonGroup();
      changeUsernameButton.addActionListener(e1 -> {
         settingFrame.dispose();
         JFrame changeUsernameFrame = new JFrame();
         changeUsernameFrame.setLayout(new BorderLayout());
         JPanel center = new JPanel(new GridLayout(1, 2)), bottom = new JPanel();
         JButton setUsernameButton = new JButton("Change username"), goBackButton = new JButton("Go back");
         JTextField newUsernameLeft = new JTextField("New username"), newUsernameRight = new JTextField();
         setUsernameButton.addActionListener(e2 -> {
            String newUsername = newUsernameRight.getText().trim();
            if (newUsername.equals(GameAction.username)) {
               JOptionPane.showMessageDialog(null, "New username must not the same as old username!!");
            } else {
               // System.out.println(newUsername);
               ResultSet rs = GameAction.executeQuery(statement, "SELECT * FROM accounts where user_id = ?",
                     newUsername);
               try {
                  if (rs.next()) {
                     JOptionPane.showMessageDialog(null, "Username existed! Please choose another username!");
                  } else {
                     // TODO: Change username here
                     GameAction.writeFile("cookie", newUsername);
                     GameAction.executeUpdate(statement, "UPDATE accounts SET user_id=? where user_id=?", newUsername,
                           GameAction.username);
                     GameAction.executeUpdate(statement, "UPDATE beginner_db SET username=? where username=?",
                           newUsername,
                           GameAction.username);
                     GameAction.executeUpdate(statement, "UPDATE intermediate_db SET username=? where username=?",
                           newUsername, GameAction.username);
                     GameAction.executeUpdate(statement, "UPDATE expert_db SET username=? where username=?",
                           newUsername,
                           GameAction.username);
                     GameAction.executeUpdate(statement, "UPDATE custom_db SET username=? where username=?",
                           newUsername,
                           GameAction.username);
                     GameAction.executeUpdate(statement, "UPDATE game_data SET username=? where username=?",
                           newUsername,
                           GameAction.username);
                     GameAction.username = newUsername;
                     JOptionPane.showMessageDialog(null, "Username changed " + newUsername + " !");
                     changeUsernameFrame.dispose();
                     new Minesweeper(true);
                  }
               } catch (SQLException e) {
                  // TODO Auto-generated catch block
                  e.printStackTrace();
               }
            }
         });
         goBackButton.addActionListener(e2 -> {
            changeUsernameFrame.dispose();
            showSettingFrame(cp, f36, exitButton);
         });
         GameAction.setupManyComponent(true, myFont, newUsernameLeft, setUsernameButton, goBackButton);
         GameAction.addManyComponent(center, newUsernameLeft, newUsernameRight);
         GameAction.addManyComponent(bottom, setUsernameButton, goBackButton);
         changeUsernameFrame.add(center, BorderLayout.CENTER);
         changeUsernameFrame.add(bottom, BorderLayout.SOUTH);
         GameAction.setupFrame(changeUsernameFrame, "Change username", new Dimension(500, 200));
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
            showSettingFrame(cp, f36, exitButton);
         });
         setPasswordButton.addActionListener(e2 -> {
            try {
               final ResultSet rs = GameAction.executeQuery(statement, "SELECT ps FROM accounts where user_id=?",
                     GameAction.username);
               if (rs.next()) {
                  final String newPassword = new String(newPasswordRight.getPassword()),
                        oldPassword = GameAction.hashPassword(new String(oldPasswordRight.getPassword()));
                  if (newPassword.equals(""))
                     JOptionPane.showMessageDialog(null, "Please fill on 'New password' field!");
                  else if (rs.getString("ps").equals(oldPassword)) {
                     if (newPassword.equals(new String(confirmPasswordRight.getPassword()))) {
                        GameAction.executeUpdate(statement, "UPDATE accounts SET ps=? WHERE user_id=?",
                              GameAction.hashPassword(newPassword), GameAction.username);
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
         GameAction.setupManyComponent(true, myFont, oldPasswordLeft, newPasswordLeft, confirmPassWordLeft,
               goBackButton, setPasswordButton);
         GameAction.addManyComponent(tmpFrame, oldPasswordLeft, oldPasswordRight, newPasswordLeft,
               newPasswordRight,
               confirmPassWordLeft, confirmPasswordRight, setPasswordButton, goBackButton);
         GameAction.setupFrame(tmpFrame, "Change password", new Dimension(800, 400));
      });
      JPanel bottom = new JPanel(), center = new JPanel(new GridLayout(3, 2)),
            showNewGamePanel = new JPanel(new GridLayout(2, 1)), changeUsernamePanel = new JPanel(new GridLayout(2, 1));
      g.add(showNewGameButton);
      g.add(notShowNewGameButton);
      showNewGameButton.addActionListener(e1 -> showNewGameEnabled = true);
      notShowNewGameButton.addActionListener(e1 -> showNewGameEnabled = false);
      newGameButton.addActionListener(e1 -> {
         settingFrame.dispose();
         new Minesweeper(true);
      });
      GameAction.setupManyComponent(true, f36, usernameLeft, newGameButton, usernameRight, changePasswordButton,
            showNewGame, showNewGameButton, changeUsernameButton, notShowNewGameButton, passwordLeft);
      GameAction.addManyComponent(bottom, newGameButton, exitButton);
      GameAction.addManyComponent(showNewGamePanel, showNewGameButton, notShowNewGameButton);
      GameAction.addManyComponent(changeUsernamePanel, usernameRight, changeUsernameButton);
      GameAction.addManyComponent(center, usernameLeft, changeUsernamePanel, passwordLeft, changePasswordButton,
            showNewGame, showNewGamePanel);
      settingFrame.add(center, BorderLayout.CENTER);
      settingFrame.add(bottom, BorderLayout.SOUTH);
      GameAction.setupFrame(settingFrame, "Setting", new Dimension(1000, 500));
   }

   public void updateStreak(final boolean isWin) {
      if (isWin) {
         final String tmpMode = GameAction.numMode == 3 ? "custom_" : "";
         GameAction.executeUpdate(statement, "UPDATE " + GameAction.mode.toLowerCase() + "_db SET " + tmpMode + "wins="
               + tmpMode + "wins+1 WHERE username=?", GameAction.username);
      }
      if (GameAction.numMode == 3) {
         final String board = GameAction.height + "x" + GameAction.width + "x" + GameAction.mines;
         currentCustomStreak = currentCustomTable.get(board) == null ? 0 : currentCustomTable.get(board);
         currentCustomStreak = isWin ? currentCustomStreak + 1 : 0;
         if (currentCustomStreak != 0) {
            currentCustomTable.put(board, currentCustomStreak);
            final int maxStreak = maxCustomTable.get(board) == null ? 1 : maxCustomTable.get(board);
            maxCustomTable.put(board, Math.max(currentCustomStreak, maxStreak));
         } else
            currentCustomTable.remove(board);
         final Set<String> currentCustomStreakSet = currentCustomTable.keySet(),
               maxCustomStreakSet = maxCustomTable.keySet();
         final StringBuffer currentStreak = new StringBuffer(), maxStreak = new StringBuffer();
         for (final String string : currentCustomStreakSet)
            currentStreak.append(string + ":" + currentCustomTable.get(string) + ";");
         for (final String string : maxCustomStreakSet)
            maxStreak.append(string + ":" + maxCustomTable.get(string) + ";");
         GameAction.executeUpdate(statement,
               "UPDATE custom_db SET max_streak=?,current_streak=? WHERE username=?", maxStreak.toString(),
               currentStreak.toString(), GameAction.username);
      } else {
         currentNormalStreak[GameAction.numMode] = isWin ? currentNormalStreak[GameAction.numMode] + 1 : 0;
         maxNormalStreak[GameAction.numMode] = Math.max(currentNormalStreak[GameAction.numMode],
               maxNormalStreak[GameAction.numMode]);
         GameAction.executeUpdate(statement,
               "UPDATE " + GameAction.mode.toLowerCase() + "_db SET max_ws=?,current_ws=? WHERE username=?;",
               maxNormalStreak[GameAction.numMode], currentNormalStreak[GameAction.numMode], GameAction.username);
         GameAction.executeUpdate(statement, "INSERT INTO game_data VALUES(?,?,?,?);", GameAction.username,
               GameAction.numMode, isWin,
               LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
      }
   }

   private void open(final int[][] board, final int[][] answer, final int i, final int j) {
      if (i < 0 || i >= GameAction.height || j < 0 || j >= GameAction.width || board[i][j] == 1)
         return;
      if (answer[i][j] != 0) {
         cellBoard[i][j].setValue(answer[i][j]);
         cellBoard[i][j].setBackground(selectedColor);
         return;
      }
      if (cellBoard[i][j].getText().equals(answer[i][j] + ""))
         return;
      cellBoard[i][j].setValue(answer[i][j]);
      cellBoard[i][j].setBackground(selectedColor);
      for (final int[] aroundCells : GameAction.getAroundCells(i, j))
         open(board, answer, i + aroundCells[0], j + aroundCells[1]);
   }

   private void checkWin(final int needOpen) {
      openCell = 0;
      for (int k = 0; k < GameAction.height; k++)
         for (int l = 0; l < GameAction.width; l++)
            if (GameAction.isInteger(cellBoard[k][l].getText()))
               openCell++;
      if (openCell == needOpen) {
         final double playTime = timerField.getText().equals("") ? 0.0 : Double.parseDouble(timerField.getText());
         if (GameAction.numMode != 3)
            GameAction.executeUpdate(statement,
                  "UPDATE " + GameAction.mode.toLowerCase() + "_db SET time=ARRAY_APPEND(time,?)where username=?",
                  playTime, GameAction.username);
         timer.cancel();
         updateStreak(true);
         if (JOptionPane.showConfirmDialog(null,
               GameAction.mode + " - " + playTime + " s\nWin streak: "
                     + (GameAction.numMode == 3 ? currentCustomStreak : currentNormalStreak[GameAction.numMode])
                     + "\nClicks: " + clicks + "\nPlay again?",
               "You win!", 0) != 0)
            System.exit(0);
         else {
            dispose();
            SwingUtilities.invokeLater(() -> new Minesweeper(true));
         }
      }
   }

   private void statisticsFrame(final Container cp) {
      cp.removeAll();
      cp.setLayout(new BorderLayout());
      final JPanel center = new JPanel(new GridLayout(15, 2)), bottom = new JPanel();
      final JTextField usernameText = new JTextField("Username"), usernameResult = new JTextField(GameAction.username),
            beginnerStreakLeft = new JTextField("Beginner highest streak"),
            beginnerStreakRight = new JTextField(maxNormalStreak[0] + ""),
            intermediateStreakLeft = new JTextField("Intermediate highest streak"),
            intermediateStreakRight = new JTextField(maxNormalStreak[1] + ""),
            expertStreakLeft = new JTextField("Expert highest streak"),
            beginnerFastestLeft = new JTextField("Beginner fastest time"),
            beginnerFastestRight = new JTextField(fastestTime[0] + ""),
            intermediateFastestLeft = new JTextField("Intermediate fastest time"),
            intermediateFastestRight = new JTextField(fastestTime[1] + ""),
            expertFastestLeft = new JTextField("Expert fastest time"),
            expertFastestRight = new JTextField(fastestTime[2] + ""),
            expertStreakRight = new JTextField(maxNormalStreak[2] + ""), winLeft = new JTextField("Wins"),
            customWinLeft = new JTextField("Custom wins"), customWinRight = new JTextField(wins[3] + ""),
            winRight = new JTextField(wins[0] + wins[1] + wins[2] + wins[3] + ""),
            beginnerWinLeft = new JTextField("Beginner wins"), beginnerWinRight = new JTextField(wins[0] + ""),
            intermediateWinLeft = new JTextField("Intermediate wins"),
            intermediateWinRight = new JTextField(wins[1] + ""), expertWinLeft = new JTextField("Expert wins"),
            expertWinRight = new JTextField(wins[2] + ""), beginnerMasteryLeft = new JTextField("Beginner mastery"),
            beginnerMasteryRight = new JTextField(GameAction.calculateMastery(0) + ""),
            intermediateMasteryLeft = new JTextField("Intermediate mastery"),
            intermediateMasteryRight = new JTextField(GameAction.calculateMastery(1) + ""),
            expertMasteryLeft = new JTextField("Expert mastery"),
            expertMasteryRight = new JTextField(GameAction.calculateMastery(2) + "");
      final Font myFont = new Font("Dialog", 0, 25);
      final JButton exitButton = new JButton("Exit"), newGameButton = new JButton("New game");
      exitButton.addActionListener(e -> System.exit(0));
      newGameButton.addActionListener(e -> {
         dispose();
         SwingUtilities.invokeLater(() -> new Minesweeper(true));
      });
      GameAction.setupManyComponent(true, myFont, usernameText, usernameResult, beginnerStreakRight, beginnerStreakLeft,
            intermediateStreakRight, intermediateStreakLeft, expertStreakRight, expertStreakLeft, beginnerFastestLeft,
            beginnerFastestRight, intermediateFastestLeft, intermediateFastestRight, expertFastestLeft,
            expertFastestRight, beginnerWinLeft, beginnerWinRight, intermediateWinLeft, intermediateWinRight,
            expertWinLeft, expertWinRight, winLeft, winRight, exitButton, newGameButton, beginnerMasteryLeft,
            beginnerMasteryRight, intermediateMasteryLeft, intermediateMasteryRight, expertMasteryLeft,
            expertMasteryRight, customWinLeft, customWinRight);
      GameAction.addManyComponent(center, usernameText, usernameResult, beginnerFastestLeft, beginnerFastestRight,
            intermediateFastestLeft, intermediateFastestRight, expertFastestLeft, expertFastestRight,
            beginnerMasteryLeft, beginnerMasteryRight, intermediateMasteryLeft, intermediateMasteryRight,
            expertMasteryLeft, expertMasteryRight, beginnerStreakLeft, beginnerStreakRight, intermediateStreakLeft,
            intermediateStreakRight, expertStreakLeft, expertStreakRight, beginnerWinLeft, beginnerWinRight,
            intermediateWinLeft, intermediateWinRight, expertWinLeft, expertWinRight, customWinLeft, customWinRight,
            winLeft, winRight);
      GameAction.addManyComponent(bottom, newGameButton, exitButton);
      cp.add(center, BorderLayout.CENTER);
      cp.add(bottom, BorderLayout.SOUTH);
      GameAction.setupFrame(this, "Statistics", new Dimension(1000, 500));
   }

   private void makeBoard(final Random r) {
      for (int i = 0; i < GameAction.mines; i++) {
         final int x = r.nextInt(0, GameAction.height), y = r.nextInt(0, GameAction.width);
         if (gameBoard[x][y] == 0)
            gameBoard[x][y] = 1;
         else
            i--;
      }
   }
}