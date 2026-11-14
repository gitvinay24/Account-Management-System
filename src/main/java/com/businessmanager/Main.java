package com.businessmanager;

import com.businessmanager.ui.MainFrame;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class Main {

    public static void main(String[] args) {

        // Surface any uncaught exception (database outages, SQL errors, ...) as a
        // readable dialog instead of failing silently on the event dispatch thread.
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            throwable.printStackTrace();
            if (!SwingUtilities.isEventDispatchThread()) {
                return; // background failures are logged; UI keeps responding
            }
            String message = throwable.getMessage();
            if (message == null || message.isBlank()) {
                message = throwable.getClass().getSimpleName();
            }
            JOptionPane.showMessageDialog(
                    null,
                    "An unexpected error occurred:\n\n" + message,
                    "Account Management",
                    JOptionPane.ERROR_MESSAGE);
        });

        SwingUtilities.invokeLater(() -> {

            MainFrame frame =
                    new MainFrame();

            frame.setVisible(true);
        });
    }
}
