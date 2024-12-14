package paint;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class PaintApp extends JFrame {

    private int x1, y1, x2, y2;
    private Graphics2D g2d;
    private BufferedImage image;
    private Color currentColor = Color.BLACK;
    private int strokeWidth = 5;
    private final Color backgroundColor = Color.WHITE;

    // Data structure to store drawing history
    private final ArrayList<Line> drawingHistory = new ArrayList<>();
    private final HashMap<String, Color> colorMap = new HashMap<>();

    public PaintApp() {
        setTitle("Paint App");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Set custom application icon
        try {
            Image icon = Toolkit.getDefaultToolkit().getImage(getClass().getResource("./resources/paint_icon.png"));
            setIconImage(icon);
        } catch (Exception e) {
            System.out.println("Icon not found: " + e.getMessage());
        }

        // Initialize color palette
        initializeColorMap();

        // Create the drawing canvas
        image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(backgroundColor);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Create the drawing panel
        JPanel drawingPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(image, 0, 0, null);
            }
        };
        drawingPanel.setBackground(backgroundColor);
        drawingPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                x1 = e.getX();
                y1 = e.getY();

                // Right-click flood fill
                if (SwingUtilities.isRightMouseButton(e)) {
                    Color targetColor = new Color(image.getRGB(x1, y1));
                    if (!targetColor.equals(currentColor)) {
                        fillArea(x1, y1, targetColor, currentColor);
                        repaint();
                    }
                }
            }
        });
        drawingPanel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                x2 = e.getX();
                y2 = e.getY();
                g2d.setColor(currentColor);
                g2d.setStroke(new BasicStroke(strokeWidth));
                g2d.drawLine(x1, y1, x2, y2);

                // Save line to drawing history
                drawingHistory.add(new Line(x1, y1, x2, y2, currentColor, strokeWidth));

                x1 = x2;
                y1 = y2;
                repaint();
            }
        });
        add(drawingPanel, BorderLayout.CENTER);

        // Create the control panel
        JPanel controlsPanel = new JPanel();
        add(controlsPanel, BorderLayout.NORTH);

        // Clear button
        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> {
            g2d.setColor(backgroundColor);
            g2d.fillRect(0, 0, getWidth(), getHeight());
            g2d.setColor(currentColor);
            drawingHistory.clear();
            repaint();
        });
        controlsPanel.add(clearButton);

        // Undo button
        JButton undoButton = new JButton("Undo");
        undoButton.addActionListener(e -> {
            if (!drawingHistory.isEmpty()) {
                drawingHistory.remove(drawingHistory.size() - 1);
                redrawFromHistory();
            }
        });
        controlsPanel.add(undoButton);

        // Color palette
        JButton colorButton = new JButton("Pick Color");
        colorButton.addActionListener(e -> {
            Color selectedColor = JColorChooser.showDialog(this, "Select a Color", currentColor);
            if (selectedColor != null) {
                currentColor = selectedColor;
            }
        });
        controlsPanel.add(colorButton);

        // Thickness slider
        JSlider strokeSlider = new JSlider(1, 10, 5);
        strokeSlider.addChangeListener(e -> strokeWidth = strokeSlider.getValue());
        controlsPanel.add(new JLabel("Thickness:"));
        controlsPanel.add(strokeSlider);

        // Save button
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> saveImage());
        controlsPanel.add(saveButton);

        // Eraser button
        JButton eraserButton = new JButton("Eraser");
        eraserButton.addActionListener(e -> currentColor = backgroundColor);
        controlsPanel.add(eraserButton);
    }

    /**
     * Initializes the predefined color palette.
     */
    private void initializeColorMap() {
        colorMap.put("Black", Color.BLACK);
        colorMap.put("White", Color.WHITE);
        colorMap.put("Red", Color.RED);
        colorMap.put("Green", Color.GREEN);
        colorMap.put("Blue", Color.BLUE);
        colorMap.put("Yellow", Color.YELLOW);
    }

    /**
     * Redraws the canvas using the drawing history.
     */
    private void redrawFromHistory() {
        g2d.setColor(backgroundColor);
        g2d.fillRect(0, 0, getWidth(), getHeight());
        for (Line line : drawingHistory) {
            g2d.setColor(line.color);
            g2d.setStroke(new BasicStroke(line.strokeWidth));
            g2d.drawLine(line.x1, line.y1, line.x2, line.y2);
        }
        repaint();
    }

    /**
     * Recursive flood-fill algorithm to fill a bounded area with a color.
     *
     * @param x               The x-coordinate to start filling
     * @param y               The y-coordinate to start filling
     * @param targetColor     The color to replace
     * @param replacementColor The new color to fill
     */
    private void fillArea(int x, int y, Color targetColor, Color replacementColor) {
        if (x < 0 || x >= image.getWidth() || y < 0 || y >= image.getHeight()) return;
        if (!new Color(image.getRGB(x, y)).equals(targetColor)) return;
        if (new Color(image.getRGB(x, y)).equals(replacementColor)) return;

        image.setRGB(x, y, replacementColor.getRGB());

        fillArea(x + 1, y, targetColor, replacementColor);
        fillArea(x - 1, y, targetColor, replacementColor);
        fillArea(x, y + 1, targetColor, replacementColor);
        fillArea(x, y - 1, targetColor, replacementColor);
    }

    /**
     * Opens a file chooser dialog to save the current drawing as a PNG image.
     */
    private void saveImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Image");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PNG Image", "png"));
        int result = fileChooser.showSaveDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().endsWith(".png")) {
                file = new File(file.getAbsolutePath() + ".png");
            }
            try {
                ImageIO.write(image, "PNG", file);
                JOptionPane.showMessageDialog(this, "Image saved successfully!");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error saving image: " + ex.getMessage());
            }
        }
    }

    /**
     * The main entry point for the application.
     *
     * @param args Command-line arguments (not used)
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PaintApp().setVisible(true));
    }

    /**
     * Represents a line segment drawn on the canvas.
     */
    static class Line {
        int x1, y1, x2, y2, strokeWidth;
        Color color;

        Line(int x1, int y1, int x2, int y2, Color color, int strokeWidth) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.color = color;
            this.strokeWidth = strokeWidth;
        }
    }
}
