package site.hanschen.sync;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import site.hanschen.Exporter;
import site.hanschen.GsonUtils;
import site.hanschen.Log;
import site.hanschen.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @author chenhang
 */
public class BookStackHookHandler implements HttpHandler {

    private final Timer timer = new Timer();
    private final String outDir;
    private final String host;
    private final String tokenId;
    private final String tokenSecret;
    private final String gitlabToken;

    public BookStackHookHandler(String outDir, String host, String tokenId, String tokenSecret, String gitlabToken) {
        this.outDir = outDir;
        this.host = host;
        this.tokenId = tokenId;
        this.tokenSecret = tokenSecret;
        this.gitlabToken = gitlabToken;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (!"POST".equals(exchange.getRequestMethod())) {
                throw new IllegalArgumentException("Unsupported method: " + exchange.getRequestMethod());
            }
            String requestBody = IOUtils.toString(exchange.getRequestBody(), StandardCharsets.UTF_8);
            WebHookEvent event = GsonUtils.fromJson(requestBody, WebHookEvent.class);
            if (event == null || event.text == null || event.text.length() <= 0) {
                throw new IllegalArgumentException("WebHookEvent can't be null, requestBody=" + requestBody);
            }

            String commit = String.format("[%s][%s] %s", Utils.formatDate(event.triggeredAt), event.triggeredBy.name, event.text);
            timer.schedule(new UpdateTask(commit), 0);
            exchange.sendResponseHeaders(200, 0);
            exchange.getResponseBody().close();
        } catch (Exception e) {
            exchange.sendResponseHeaders(400, 0);
            exchange.getResponseBody().write(e.toString().getBytes(StandardCharsets.UTF_8));
            exchange.getResponseBody().close();
        }
    }

    private class UpdateTask extends TimerTask {

        private final String commitMessage;

        public UpdateTask(String commitMessage) {
            this.commitMessage = commitMessage;
        }

        @Override
        public void run() {
            try {
                Exporter exporter = new Exporter(outDir, "gitbook", host, tokenId, tokenSecret, true);
                exporter.start();

                Git git = Git.open(new File(outDir));
                if (git.status().call().isClean()) {
                    return;
                }

                git.add().setUpdate(true).addFilepattern(".").call();
                git.add().setUpdate(false).addFilepattern(".").call();
                git.commit().setMessage(commitMessage).call();
                git.push().setCredentialsProvider(new UsernamePasswordCredentialsProvider("oauth2", gitlabToken)).call();
                Log.println("push success");
            } catch (Throwable e) {
                e.printStackTrace();
                Log.println("oops, " + e, Log.RED);
            }
        }
    }
}
