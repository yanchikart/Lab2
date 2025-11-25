import gui.MainFrame;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // На всякий случай включим нормальный L&F
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}


