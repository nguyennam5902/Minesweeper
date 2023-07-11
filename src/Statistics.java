import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class Statistics extends JFrame {
   public static void setupStatisticsFrame(final JFrame currentFrame, final Container cp) {
      cp.removeAll();
      cp.setLayout(new BorderLayout());
      final int[] maxNormalStreak = Minesweeper.getMaxNormalStreak(), wins = Minesweeper.getWins();
      final double[] fastestTime = Minesweeper.getFastestTime();
      final JPanel center = new JPanel(new GridLayout(15, 2)), bottom = new JPanel();
      final JTextField usernameText = new JTextField("Username"),
            usernameResult = new JTextField(Minesweeper.getUsername()),
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
         currentFrame.dispose();
         SwingUtilities.invokeLater(() -> new Minesweeper(true));
      });
      SetupController.setupManyComponent(true, myFont, usernameText, usernameResult, beginnerStreakRight,
            beginnerStreakLeft,
            intermediateStreakRight, intermediateStreakLeft, expertStreakRight, expertStreakLeft, beginnerFastestLeft,
            beginnerFastestRight, intermediateFastestLeft, intermediateFastestRight, expertFastestLeft,
            expertFastestRight, beginnerWinLeft, beginnerWinRight, intermediateWinLeft, intermediateWinRight,
            expertWinLeft, expertWinRight, winLeft, winRight, exitButton, newGameButton, beginnerMasteryLeft,
            beginnerMasteryRight, intermediateMasteryLeft, intermediateMasteryRight, expertMasteryLeft,
            expertMasteryRight, customWinLeft, customWinRight);
      SetupController.addManyComponent(center, usernameText, usernameResult, beginnerFastestLeft, beginnerFastestRight,
            intermediateFastestLeft, intermediateFastestRight, expertFastestLeft, expertFastestRight,
            beginnerMasteryLeft, beginnerMasteryRight, intermediateMasteryLeft, intermediateMasteryRight,
            expertMasteryLeft, expertMasteryRight, beginnerStreakLeft, beginnerStreakRight, intermediateStreakLeft,
            intermediateStreakRight, expertStreakLeft, expertStreakRight, beginnerWinLeft, beginnerWinRight,
            intermediateWinLeft, intermediateWinRight, expertWinLeft, expertWinRight, customWinLeft, customWinRight,
            winLeft, winRight);
      SetupController.addManyComponent(bottom, newGameButton, exitButton);
      cp.add(center, BorderLayout.CENTER);
      cp.add(bottom, BorderLayout.SOUTH);
      SetupController.setupFrame(currentFrame, "Statistics", new Dimension(1000, 500));
   }

}
