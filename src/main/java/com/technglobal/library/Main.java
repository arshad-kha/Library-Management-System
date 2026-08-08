package com.technglobal.library;

import com.technglobal.library.ui.MainFrame;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // Use the OS-native look and feel for a more polished, professional UI
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // fall back to default Swing look and feel
        }

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
