package site.hanschen;

import org.apache.commons.cli.*;
import sun.misc.Unsafe;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;

public class BookStackExporter {

    public static void main(String[] args) throws IOException {
        disableWarning();
        CommandLine commandLine = parserOption(args);
        if (commandLine == null) {
            return;
        }

        String outDir = commandLine.getOptionValue("o", "./BookStack");
        String fileType = commandLine.getOptionValue("t", "markdown");
        String host = commandLine.getOptionValue("host", null);
        String tokenId = commandLine.getOptionValue("tokenId", null);
        String tokenSecret = commandLine.getOptionValue("tokenSecret", null);
        boolean force = commandLine.hasOption("f");
        Exporter exporter = new Exporter(outDir, fileType, host, tokenId, tokenSecret, force);
        try {
            exporter.start();
        } catch (Exception e) {
            Log.println("oops, " + e, Log.RED);
            e.printStackTrace();
        }
    }

    private static void disableWarning() {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            Unsafe u = (Unsafe) theUnsafe.get(null);

            Class<?> cls = Class.forName("jdk.internal.module.IllegalAccessLogger");
            Field logger = cls.getDeclaredField("logger");
            u.putObjectVolatile(cls, u.staticFieldOffset(logger), null);
        } catch (Exception ignored) {
        }
    }

    private static CommandLine parserOption(String[] args) {
        Options options = new Options();
        Option opt = new Option("o", "output", true, "Export dir");
        opt.setRequired(false);
        options.addOption(opt);

        opt = new Option("t", "type", true, "File type, [pdf|markdown|plaintext|html]");
        opt.setRequired(false);
        options.addOption(opt);

        opt = new Option("host", null, true, "Base url of BookStack");
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

        opt = new Option("h", "help", false, "Print help");
        opt.setRequired(false);
        options.addOption(opt);

        opt = new Option("f", "force", false, "Force overwrite");
        opt.setRequired(false);
        options.addOption(opt);

        HelpFormatter hf = new HelpFormatter();
        hf.setWidth(110);
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine commandLine = parser.parse(options, args);

            String[] fileTypes = new String[]{"html", "pdf", "plaintext", "markdown", "gitbook"};
            String fileType = commandLine.getOptionValue("t", "markdown");
            if (!Arrays.asList(fileTypes).contains(fileType)) {
                Log.println("Invalid file type, must be one of [gitbook|pdf|markdown|plaintext|html]\n", Log.RED);
                hf.printHelp("BookStackExporter", options, true);
                return null;
            }

            if (commandLine.hasOption("h")) {
                hf.printHelp("BookStackExporter", options, true);
            }

            return commandLine;
        } catch (ParseException ignored) {
            hf.printHelp("BookStackExporter", options, true);
        }
        return null;
    }
}
