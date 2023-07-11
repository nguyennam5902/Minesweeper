import javax.swing.JFrame;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class SetupController {
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
               // Minesweeper.getBoardHeight() = Integer.parseInt(heightAns.getText());
               Minesweeper.setBoardHeight(Integer.parseInt(heightAns.getText()));
               Minesweeper.setBoardWidth(Integer.parseInt(widthAns.getText()));
               Minesweeper.setBoardMines(Integer.parseInt(mineAns.getText()));
            } catch (final Exception e2) {
               error = true;
               JOptionPane.showMessageDialog(null, "ERROR:Input is not an positive integer");
            }
            if (!error) {
               if (Minesweeper.getBoardMines() < Minesweeper.getBoardHeight() * Minesweeper.getBoardWidth()
                     && Minesweeper.getBoardWidth() < 39 && Minesweeper.getBoardHeight() < 51
                     && Minesweeper.getBoardWidth() > 1 && Minesweeper.getBoardHeight() > 1) {
                  frame.dispose();
                  Minesweeper.setMode("Custom");
                  Minesweeper.setNumMode(3);
                  SwingUtilities.invokeLater(() -> new Minesweeper(false));
               } else
                  JOptionPane.showMessageDialog(null,
                        "Cannot make board " + Minesweeper.getBoardHeight() + " x " + Minesweeper.getBoardWidth() + "/"
                              + Minesweeper.getBoardMines());
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
                  GameAction.updateStreak(false);
               }
               setupBoard(f, numMode);
            }
         } else
            setupBoard(f, numMode);
      };
   }

   private static void setupBoard(final JFrame f, final int numMode) {
      Minesweeper.setBoardHeight(numMode == 0 ? 9 : 16);
      Minesweeper.setBoardWidth(numMode == 0 ? 9 : numMode == 1 ? 16 : 30);
      Minesweeper.setBoardMines(numMode == 0 ? 10 : numMode == 1 ? 40 : 99);
      Minesweeper.setMode(numMode == 0 ? "Beginner" : numMode == 1 ? "Intermediate" : "Expert");
      Minesweeper.setNumMode(numMode);
      f.dispose();
      SwingUtilities.invokeLater(() -> new Minesweeper(false));
   }

}
