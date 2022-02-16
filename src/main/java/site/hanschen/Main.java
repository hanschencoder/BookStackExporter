package site.hanschen;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.spi.HttpServerProvider;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import site.hanschen.exporter.Exporter;
import site.hanschen.sync.SyncHandler;
import site.hanschen.utils.Log;
import site.hanschen.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.logging.*;

/**
 * @author chenhang
 */
public class Main {

    public static void main(String[] args) {
        Utils.disableWarning();
        try {
            Log.setPrinter(new Printer(new File("./log", "log_exporter.txt")));
        } catch (Exception ignored) {
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> Log.println("VM Shutdown")));
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Log.println("uncaughtException, thread=" + thread);
            Log.println("uncaughtException, throwable=" + throwable);
            Log.printStackTrace(throwable);
        });

        CommandLine commandLine = parserOption(args);
        if (commandLine == null) {
            return;
        }

        String outDir = commandLine.getOptionValue("o", "./BookStack");
        String exportType = commandLine.getOptionValue("t", "gitbook");
        String baseUrl = commandLine.getOptionValue("baseUrl", null);
        String tokenId = commandLine.getOptionValue("tokenId", null);
        String tokenSecret = commandLine.getOptionValue("tokenSecret", null);
        boolean force = commandLine.hasOption("f");
        boolean syncServer = commandLine.hasOption("syncServer");

        try {
            if (syncServer) {
                int port = Integer.parseInt(commandLine.getOptionValue("webhookPort"));
                String gitlabToken = commandLine.getOptionValue("gitlabToken", null);
                String gitbookName = commandLine.getOptionValue("gitbookName", null);
                String gitbookDir = commandLine.getOptionValue("gitbookDir", "./GitBook");
                Log.println("WebHook Server Start");
                HttpServerProvider provider = HttpServerProvider.provider();
                HttpServer httpserver = provider.createHttpServer(new InetSocketAddress(port), 0);
                httpserver.createContext("/webhooks", new SyncHandler(outDir, baseUrl, tokenId, tokenSecret, gitlabToken, gitbookName, gitbookDir));
                httpserver.setExecutor(null);
                httpserver.start();
            } else {
                Exporter exporter = new Exporter(outDir, exportType, baseUrl, tokenId, tokenSecret, force);
                exporter.start();
            }
        } catch (Exception e) {
            Log.println(e.toString(), Log.RED);
            Log.printStackTrace(e);
        }
    }

    private static CommandLine parserOption(String[] args) {
        Options options = new Options();

        Option opt = new Option("syncServer", null, false, "Start as sync server");
        opt.setRequired(false);
        options.addOption(opt);

        opt = new Option("o", "output", true, "Export dir");
        opt.setRequired(false);
        options.addOption(opt);

        opt = new Option("t", "type", true, "Export type, [gitbook|pdf|markdown|plaintext|html]");
        opt.setRequired(false);
        options.addOption(opt);

        opt = new Option("baseUrl", null, true, "Base url of BookStack");
        opt.setRequired(true);
        options.addOption(opt);

        opt = new Option("tokenId",
                         null,
                         true,
                         "This is a non-editable system generated identifier for this token which will need to be provided in API requests.");
        opt.setRequired(true);
        options.addOption(opt);

        opt = new Option("tokenSecret",
                         null,
                         true,
                         "This is a system generated secret for this token which will need to be provided in API requests. This will only be displayed this one time so copy this value to somewhere safe and secure.");
        opt.setRequired(true);
        options.addOption(opt);

        opt = new Option("f", "force", false, "Force overwrite");
        opt.setRequired(false);
        options.addOption(opt);

        opt = new Option("webhookPort", null, true, "BookStack web hook port");
        opt.setRequired(false);
        options.addOption(opt);

        opt = new Option("gitlabToken", null, true, "Gitlab personal access tokens");
        opt.setRequired(false);
        options.addOption(opt);

        opt = new Option("gitbookName", null, true, "Name of GitBook");
        opt.setRequired(false);
        options.addOption(opt);

        opt = new Option("gitbookDir", null, true, "Output of GitBook build");
        opt.setRequired(false);
        options.addOption(opt);

        opt = new Option("h", "help", false, "Print help");
        opt.setRequired(false);
        options.addOption(opt);

        HelpFormatter hf = new HelpFormatter();
        hf.setWidth(110);

        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine commandLine = parser.parse(options, args);

            if (commandLine.hasOption("syncServer")) {
                if (!commandLine.hasOption("webhookPort")) {
                    throw new IllegalArgumentException("webhookPort not found");
                } else {
                    try {
                        int port = Integer.parseInt(commandLine.getOptionValue("webhookPort"));
                    } catch (Exception e) {
                        throw new IllegalArgumentException("webhookPort=" + commandLine.getOptionValue("webhookPort"));
                    }
                }
                if (!commandLine.hasOption("gitlabToken")) {
                    throw new IllegalArgumentException("gitlabToken not found");
                }
            } else {
                String[] fileTypes = new String[]{"html", "pdf", "plaintext", "markdown", "gitbook"};
                String fileType = commandLine.getOptionValue("t", "gitbook");
                if (!Arrays.asList(fileTypes).contains(fileType)) {
                    throw new IllegalArgumentException("Invalid file type, must be one of [gitbook|pdf|markdown|plaintext|html]");
                }
            }

            if (commandLine.hasOption("h")) {
                hf.printHelp("BookStackExporter", options, true);
            }

            return commandLine;
        } catch (Exception e) {
            hf.printHelp("BookStackExporter", options, true);
            Log.println("\n" + e, Log.RED);
        }
        return null;
    }

    private static class Printer implements Log.Printer {

        private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        private final Logger logger;

        public Printer(File logFile) throws IOException {
            FileUtils.forceMkdirParent(logFile);
            logger = Logger.getLogger("book-stack-exporter");

            for (Handler handler : logger.getHandlers()) {
                logger.removeHandler(handler);
            }
            for (Handler handler : logger.getParent().getHandlers()) {
                logger.getParent().removeHandler(handler);
            }

            Formatter formatter = new Formatter() {
                @Override
                public String format(LogRecord logRecord) {
                    return logRecord.getMessage();
                }
            };
            FileHandler fileHandler = new FileHandler(logFile.getCanonicalPath(), true);
            fileHandler.setFormatter(formatter);
            logger.addHandler(fileHandler);

            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setFormatter(formatter);
            logger.addHandler(consoleHandler);

            logger.setLevel(Level.INFO);
        }

        @Override
        public void println(String message) {
            String prefix = "[" + sdf.format(new Date()) + "] ";
            message = prefix + message + "\n";
            logger.log(Level.INFO, message);
        }
    }
}
