package site.hanschen.utils;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

public class CommandExecutor {

    public static void exec(String command, Printer printer) {
        if (command.length() == 0) {
            throw new IllegalArgumentException("Empty command");
        }

        StringTokenizer st = new StringTokenizer(command);
        String[] cmdArray = new String[st.countTokens()];
        for (int i = 0; st.hasMoreTokens(); i++) {
            cmdArray[i] = st.nextToken();
        }

        exec(cmdArray, printer);
    }

    public static void exec(String[] command, Printer printer) {
        try {
            printer.println("Exec Command: " + toString(command));
            Process process = Runtime.getRuntime().exec(command);
            OutputThread outputThread = new OutputThread(process.getInputStream(), printer);
            outputThread.start();
        } catch (Exception e) {
            printer.println("Oops, exec failed, command=[" + toString(command) + "], reason=" + e);
        }
    }

    private static String toString(String[] cmd) {
        if (cmd == null || cmd.length == 0) {
            return "null";
        }

        StringBuilder b = new StringBuilder();
        for (int i = 0; i < cmd.length; i++) {
            b.append(cmd[i]);
            if (i == cmd.length - 1) {
                return b.toString();
            }
            b.append(" ");
        }
        return "null";
    }


    private static final class OutputThread extends Thread {

        private final InputStream mInputStream;
        private final Printer mPrinter;

        public OutputThread(InputStream inputStream, Printer printer) {
            mInputStream = inputStream;
            mPrinter = printer;
        }

        @Override
        public void run() {
            BufferedReader in = new BufferedReader(new InputStreamReader(mInputStream));
            String line;
            while (true) {
                try {
                    line = in.readLine();
                    if (line == null) {
                        break;
                    }
                    mPrinter.println(line);
                } catch (IOException e) {
                    mPrinter.println(e.toString());
                    break;
                }
            }
            IOUtils.closeQuietly(mInputStream);
        }
    }

    public interface Printer {

        void println(String log);
    }
}
