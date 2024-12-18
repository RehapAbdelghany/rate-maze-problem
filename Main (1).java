
import java.awt.*;
import java.util.concurrent.*;
import javax.swing.*;

public class Main extends JFrame {
    private int N;
    private int[][] maze;
    private boolean[][] visited;
    private JButton[][] buttons;
    private JPanel mazePanel;
    private ExecutorService threadPool;
    private Color[] threadColors = {Color.CYAN, Color.MAGENTA, Color.GREEN, Color.BLUE, Color.ORANGE , };
    private int colorIndex = 0;
    private volatile boolean solutionFound = false; // Flag to indicate if a solution is found

    public Main() {
        setupGUI();
    }

    private void setupGUI() {
        setTitle("Rat in a Maze - Multithreading");
        setSize(700, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Input panel
        JPanel inputPanel = new JPanel();
        JLabel label = new JLabel("Enter Maze Size (N): ");
        JTextField sizeField = new JTextField(5);
        JButton generateButton = new JButton("Generate Maze");
        inputPanel.add(label);
        inputPanel.add(sizeField);
        inputPanel.add(generateButton);
        add(inputPanel, BorderLayout.NORTH);

        // Maze display panel
        mazePanel = new JPanel();
        add(mazePanel, BorderLayout.CENTER);

        // Solve button
        JButton solveButton = new JButton("Solve Maze");
        add(solveButton, BorderLayout.SOUTH);

        // Action listeners
        generateButton.addActionListener(e -> {
            try {
                N = Integer.parseInt(sizeField.getText());
                if (N <= 0) throw new NumberFormatException();
                generateMazeUI();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter a valid positive integer.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        solveButton.addActionListener(e -> {
            if (maze == null) {
                JOptionPane.showMessageDialog(this, "Please generate a maze first.", "Error", JOptionPane.ERROR_MESSAGE);
            } else {
                solveMaze();
            }
        });

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void generateMazeUI() {
        maze = new int[N][N];
        visited = new boolean[N][N];
        buttons = new JButton[N][N];
        mazePanel.removeAll();
        mazePanel.setLayout(new GridLayout(N, N));

        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                buttons[i][j] = new JButton("1");
                buttons[i][j].setBackground(Color.WHITE);
                maze[i][j] = 1; // Default value
                int x = i, y = j;

                buttons[i][j].addActionListener(e -> {
                    if (maze[x][y] == 0) {
                        maze[x][y] = 1;
                        buttons[x][y].setText("1");
                        buttons[x][y].setBackground(Color.WHITE);
                    } else {
                        maze[x][y] = 0;
                        buttons[x][y].setText("0");
                        buttons[x][y].setBackground(Color.LIGHT_GRAY);
                    }
                });

                mazePanel.add(buttons[i][j]);
            }
        }

        // Set start and end nodes
        maze[0][0] = 1;
        maze[N - 1][N - 1] = 1;
        buttons[0][0].setText("S");
        buttons[0][0].setBackground(Color.YELLOW);
        buttons[N - 1][N - 1].setText("E");
        buttons[N - 1][N - 1].setBackground(Color.YELLOW);

        mazePanel.revalidate();
        mazePanel.repaint();
    }

    private boolean isValid(int x, int y) {
        return x >= 0 && y >= 0 && x < N && y < N && maze[x][y] == 1 && !visited[x][y];
    }

    private void solveMaze() {
        threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        solutionFound = false; // Reset the solution flag
    
        threadPool.execute(() -> {
            if (!solve(0, 0, getNextColor())) {
                if (!solutionFound) {
                    SwingUtilities.invokeLater(() -> 
                        JOptionPane.showMessageDialog(this, "No Solution Exists!", "Failure", JOptionPane.ERROR_MESSAGE));
                }
            } else {
                SwingUtilities.invokeLater(() -> 
                    JOptionPane.showMessageDialog(this, "Solution Found!", "Success", JOptionPane.INFORMATION_MESSAGE));
            }
            threadPool.shutdownNow();
        });
    }
    
    private boolean solve(int x, int y, Color threadColor) {
        if (solutionFound) return false; // Stop further execution if a solution is found
    
        if (x == N - 1 && y == N - 1) {
            synchronized (this) {
                solutionFound = true; // Mark that a solution has been found
            }
            markPath(x, y, Color.ORANGE); // Goal reached, mark final path
            return true;
        }
    
        if (!isValid(x, y)) {
            return false;
        }
        boolean downPath, rightPath;
        synchronized (this) {
            if (!isValid(x, y) || visited[x][y]) {
                return false;
            }
            visited[x][y] = true;
            markPath(x, y, threadColor);
            downPath = isValid(x + 1, y) && !visited[x + 1][y];
            rightPath = isValid(x, y + 1) && !visited[x][y + 1];
        }
    
        if (downPath && rightPath) {
            int activeThreads = ((ThreadPoolExecutor) threadPool).getActiveCount();
            int maxThreads = Runtime.getRuntime().availableProcessors();
    
            if (activeThreads < maxThreads) {
                Future<Boolean> downThread = threadPool.submit(() -> solve(x + 1, y, getNextColor()));
                Future<Boolean> rightThread = threadPool.submit(() -> solve(x, y + 1, getNextColor()));
    
                try {
                    if (downThread.get()) return true;
                    if (rightThread.get()) return true;
                } catch (InterruptedException | ExecutionException e) {
                    Thread.currentThread().interrupt(); // Restore interrupt flag
                }
            } else {
                if (solve(x + 1, y, threadColor)) return true;
                if (solve(x, y + 1, threadColor)) return true;
            }
        } else if (downPath) {
            if (solve(x + 1, y, threadColor)) return true;
        } else if (rightPath) {
            if (solve(x, y + 1, threadColor)) return true;
        }
    
        synchronized (this) {
            visited[x][y] = false; // Backtrack
        }
    
        //markPath(x, y, Color.RED); // Backtracking visual indicator
        return false;
    }
    
    
    private void markPath(int x, int y, Color color) {
        SwingUtilities.invokeLater(() -> buttons[x][y].setBackground(color));
        try {
            Thread.sleep(100); // Delay for visualization
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private synchronized Color getNextColor() {
        Color color = threadColors[colorIndex % threadColors.length];
        colorIndex++;
        return color;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::new);
    }
}
