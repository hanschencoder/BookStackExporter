package site.hanschen.sync;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.spi.HttpServerProvider;
import org.apache.commons.cli.*;
import site.hanschen.Log;
import site.hanschen.Utils;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * @author chenhang
 */
public class BookStackServer {

    public static void main(String[] args) throws IOException {
        Utils.disableWarning();
        CommandLine commandLine = parserOption(args);
        if (commandLine == null) {
            return;
        }

        String outDir = commandLine.getOptionValue("o", "./BookStack");
        String host = commandLine.getOptionValue("host", null);
        String tokenId = commandLine.getOptionValue("tokenId", null);
        String tokenSecret = commandLine.getOptionValue("tokenSecret", null);
        String gitlabToken = commandLine.getOptionValue("gitlabToken", null);
        int port = Integer.parseInt(commandLine.getOptionValue("webhookPort"));

        Log.println("WebHook Server Start");
        HttpServerProvider provider = HttpServerProvider.provider();
        HttpServer httpserver = provider.createHttpServer(new InetSocketAddress(port), 0);
        httpserver.createContext("/webhooks", new BookStackHookHandler(outDir, host, tokenId, tokenSecret, gitlabToken));
        httpserver.setExecutor(null);
        httpserver.start();
    }

    private static CommandLine parserOption(String[] args) {
        Options options = new Options();
        Option opt = new Option("o", "output", true, "Export dir");
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

        opt = new Option("webhookPort", null, true, "BookStack web hook port");
        opt.setRequired(true);
        options.addOption(opt);

        opt = new Option("gitlabToken", null, true, "Gitlab personal access tokens");
        opt.setRequired(true);
        options.addOption(opt);

        opt = new Option("h", "help", false, "Print help");
        opt.setRequired(false);
        options.addOption(opt);

        HelpFormatter hf = new HelpFormatter();
        hf.setWidth(110);
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine commandLine = parser.parse(options, args);
            try {
                int port = Integer.parseInt(commandLine.getOptionValue("webhookPort"));
            } catch (Exception e) {
                throw new IllegalArgumentException("webhookPort=" + commandLine.getOptionValue("webhookPort"));
            }
            if (commandLine.hasOption("h")) {
                hf.printHelp("BookStackServer", options, true);
            }
            return commandLine;
        } catch (Exception e) {
            hf.printHelp("BookStackServer", options, true);
            Log.println(e.toString(), Log.RED);
        }
        return null;
    }
}
