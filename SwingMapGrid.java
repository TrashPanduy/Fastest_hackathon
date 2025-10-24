import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.nio.file.*;
import java.io.IOException;


public class SwingMapGrid {
    private static java.util.List<String> phrases = new ArrayList<>();

    private static Integer[] playerPosition = {0, 0};
    private static Timer timer;

    private static boolean up, down, left, right;

    private static char[][] map;
    private static JLabel[][] cells;
    private static JPanel gridPanel;
    private static int level = 0;

private static boolean allEnemiesDefeated() {
    for (int r = 0; r < map.length; r++) {
        for (int c = 0; c < map[r].length; c++) {
            if (map[r][c] == 'E') return false;
        }
    }
    return true;
}

    private static char[][] generateMap(int rows, int cols, int level) {
        try {
            phrases = Files.readAllLines(Paths.get("phrases.txt"));
        } catch (IOException e) {
            System.err.println("Could not load phrases.txt!");
            phrases = Arrays.asList("default phrase"); // fallback
        }

        char[][] map = new char[rows][cols];
        for (int i = 0; i < rows; i++) {
            map[i][0] = '#';
            map[i][cols - 1] = '#';
        }
        for (int i = 0; i < cols; i++) {
            map[0][i] = '#';
            map[rows - 1][i] = '#';
        }
        Random rand = new Random();
        for (int i = 0; i < level; i++) {
            int randRow = rand.nextInt(rows - 2) + 1;
            int randCol = rand.nextInt(cols - 2) + 1;
            map[randRow][randCol] = 'E';
        }

        playerPosition[0] = rows - 1;
        playerPosition[1] = cols / 2;
        
        map[playerPosition[0]][playerPosition[1]] = 'P';
        map[0][playerPosition[1]] = '^';

        return map;
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Map Grid");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 700);

        map = generateMap(5, 5, level+1);
        level = 1;

        gridPanel = new JPanel(new GridLayout(map.length, map[0].length));
        cells = new JLabel[map.length][map[0].length];

        // Create cells
        for (int r = 0; r < map.length; r++) {
            for (int c = 0; c < map[r].length; c++) {
                JLabel cell = new JLabel(String.valueOf(map[r][c]), SwingConstants.CENTER);
                cell.setFont(new Font("Monospaced", Font.PLAIN, 32));
                cell.setBorder(BorderFactory.createLineBorder(Color.BLACK));
                gridPanel.add(cell);
                cells[r][c] = cell;
            }
        }

        // Attach key bindings to the gridPanel
        setupKeyBindings(gridPanel);

        // Timer for game loop
        timer = new Timer(100, e -> {
            int r = playerPosition[0];
            int c = playerPosition[1];
            map[r][c] = ' ';

            if (up && r > 0){
                if(map[r-1][c] == '^'){
                    //check if all Enemies dead, if so create a new map that is larger and with more enemies
                    if(allEnemiesDefeated()){
                        level++;
                        map = generateMap(map.length + 2, map[0].length+2, level*2);
                        resetGrid();
                        return;
                    }
                } else if(map[r-1][c] != '#'){
                    r--;
                }
            }
            if (down && r < map.length - 2) r++;
            if (left && c > 1) c--;
            if (right && c < map[0].length - 2) c++;

            if (map[r][c] == 'E') {
                // Remove enemy immediately so you don't trigger multiple times
                map[r][c] = ' ';

                // Create a NON-MODAL dialog
                final JDialog dialog = new JDialog((Frame) null, "Enemy Encounter!", false);
                dialog.setSize(400, 200);
                dialog.setLocationRelativeTo(null);
                dialog.setLayout(new GridBagLayout());

                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(5, 5, 5, 5);
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.gridx = 0;
                gbc.gridy = 0;

                // First label
                gbc.gridwidth = 2;
                JLabel label1 = new JLabel("Type this exactly to defeat the enemy!", SwingConstants.CENTER);
                dialog.add(label1, gbc);

                // Second label
                gbc.gridy++;
                gbc.gridwidth = 1;
                String message = getRandomPhrase();
                JLabel label2 = new JLabel("Text:" + message);
                dialog.add(label2, gbc);

                // Text box
                gbc.gridx = 1;
                JTextField textField = new JTextField(15);
                dialog.add(textField, gbc);

                // Store user input in an AtomicReference so it can be updated inside listeners
                AtomicReference<String> userInput = new AtomicReference<>("");

                // Done button
                gbc.gridy++;
                gbc.gridx = 0;
                gbc.gridwidth = 2;
                JButton doneButton = new JButton("Done");
                doneButton.addActionListener(ev -> {
                    userInput.set(textField.getText());
                    System.out.println("User typed: " + userInput.get());
                    dialog.dispose();

                    // Example: you can now handle the input, e.g.
                    if (userInput.get().equalsIgnoreCase(message)) {
                        System.out.println("Enemy defeated!");
                    } else {
                        System.out.println("You failed to defeat the enemy.");
                        JOptionPane.showMessageDialog(dialog, "You were defeated! You got to level:" + level, "Defeated!", JOptionPane.ERROR_MESSAGE);
                        map = generateMap(map.length, map[0].length, 2);
                        resetGrid();
                        level = 1;
                    }
                });
                dialog.add(doneButton, gbc);

                dialog.setVisible(true);

                // Timer to auto-close after 10 seconds
                new javax.swing.Timer(10000, ev -> {
                    userInput.set(textField.getText());
                    System.out.println("Dialog closed automatically. User typed: " + userInput.get());
                    dialog.dispose();
                }).start();
            }
            


            playerPosition[0] = r;
            playerPosition[1] = c;
            map[r][c] = 'P';

            // Update cells
            for (int i = 0; i < map.length; i++) {
                for (int j = 0; j < map[i].length; j++) {
                    cells[i][j].setText(String.valueOf(map[i][j]));
                }
            }
        });

        timer.start();

        frame.add(gridPanel);
        frame.setVisible(true);
        gridPanel.requestFocusInWindow(); // Important for key events
    }

    public static String getRandomPhrase() {
        Random rand = new Random();
        return phrases.get(rand.nextInt(phrases.size()));
    }
private static void resetGrid() {
    gridPanel.removeAll();
    gridPanel.setLayout(new GridLayout(map.length, map[0].length));
    cells = new JLabel[map.length][map[0].length];

    for (int r = 0; r < map.length; r++) {
        for (int c = 0; c < map[r].length; c++) {
            JLabel cell = new JLabel(String.valueOf(map[r][c]), SwingConstants.CENTER);
            cell.setFont(new Font("Monospaced", Font.PLAIN, 32));
            cell.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            gridPanel.add(cell);
            cells[r][c] = cell;
        }
    }

    gridPanel.revalidate();
    gridPanel.repaint();

    // Reset player position (bottom center)
    playerPosition[0] = map.length - 1;
    playerPosition[1] = map[0].length / 2;
}


    private static void setupKeyBindings(JPanel panel) {
        InputMap im = panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = panel.getActionMap();

        // Up
        im.put(KeyStroke.getKeyStroke("W"), "upPressed");
        im.put(KeyStroke.getKeyStroke("released W"), "upReleased");
        am.put("upPressed", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { up = true; }
        });
        am.put("upReleased", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { up = false; }
        });

        // Down
        im.put(KeyStroke.getKeyStroke("S"), "downPressed");
        im.put(KeyStroke.getKeyStroke("released S"), "downReleased");
        am.put("downPressed", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { down = true; }
        });
        am.put("downReleased", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { down = false; }
        });

        // Left
        im.put(KeyStroke.getKeyStroke("A"), "leftPressed");
        im.put(KeyStroke.getKeyStroke("released A"), "leftReleased");
        am.put("leftPressed", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { left = true; }
        });
        am.put("leftReleased", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { left = false; }
        });

        // Right
        im.put(KeyStroke.getKeyStroke("D"), "rightPressed");
        im.put(KeyStroke.getKeyStroke("released D"), "rightReleased");
        am.put("rightPressed", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { right = true; }
        });
        am.put("rightReleased", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { right = false; }
        });
    }
}
