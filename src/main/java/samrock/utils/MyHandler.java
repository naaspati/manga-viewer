package samrock.utils;

import java.awt.Dialog.ModalityType;
import java.awt.Font;
import java.awt.Insets;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.Objects;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import samrock.gui.SamRock;

public class MyHandler extends Handler {
    @Override
    public void publish(LogRecord record) {
        if(record.getLevel() != Level.SEVERE)
            return;
        
        JDialog dialog = new JDialog(SamRock.getMain(), "Log: " + record.getLevel().getLocalizedName(), ModalityType.DOCUMENT_MODAL);

        StringWriter sw = new StringWriter();

        append(sw, "Logger", record.getLoggerName());    
        if(!Objects.equals(record.getLoggerName(), record.getSourceClassName())) 
            append(sw, "Class", record.getSourceClassName());    
        append(sw, "Method", record.getSourceMethodName());
        append(sw, "Time", new Date(record.getMillis()));

        if(record.getMessage() != null) {
            sw.append("\nMessage\n")
            .append("----------\n")
            .append(record.getMessage())
            .append('\n');
        }
        if(record.getThrown() != null) {
            sw.append("\nTrace\n")
            .append("--------\n");
            PrintWriter pw = new PrintWriter(sw);
            record.getThrown().printStackTrace(pw);
            pw.close();
        }

        JTextArea ta = new JTextArea(sw.toString());
        ta.setEditable(false);
        ta.setFont(new Font("Consolas", Font.PLAIN, 20));
        ta.setMargin(new Insets(5, 5, 5, 5));

        dialog.add(new JScrollPane(ta));
        dialog.pack();
        dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        dialog.setLocationRelativeTo(SamRock.getMain());
        dialog.setVisible(true);
    }

    private void append(StringWriter sb, String title, Object value) {
        if(value == null)
            return;
        sb.append(title).append(": ").append(String.valueOf(value)).append('\n');
    }

    @Override
    public void flush() {

    }

    @Override
    public void close() throws SecurityException {}

}
