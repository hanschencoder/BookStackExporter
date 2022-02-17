package site.hanschen.sync;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import site.hanschen.exporter.Exporter;
import site.hanschen.utils.CommandExecutor;
import site.hanschen.utils.GsonUtils;
import site.hanschen.utils.Log;
import site.hanschen.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @author chenhang
 */
public class SyncHandler implements HttpHandler {

    private final Timer timer = new Timer();
    private final String outDir;
    private final String baseUrl;
    private final String tokenId;
    private final String tokenSecret;
    private final String gitlabToken;
    private final String gitbookName;
    private final String gitbookDir;

    public SyncHandler(String outDir, String baseUrl, String tokenId, String tokenSecret, String gitlabToken, String gitbookName, String gitbookDir) {
        this.outDir = outDir;
        this.baseUrl = baseUrl;
        this.tokenId = tokenId;
        this.tokenSecret = tokenSecret;
        this.gitlabToken = gitlabToken;
        this.gitbookName = gitbookName;
        this.gitbookDir = gitbookDir;
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
                Log.println(commitMessage);
                Exporter exporter = new Exporter(outDir, "gitbook", baseUrl, tokenId, tokenSecret, true);
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

                if (gitbookName != null && gitbookName.length() > 0) {
                    File gitbookSource = new File(outDir, gitbookName);
                    File generateBook = new File(gitbookSource, "_book");
                    if (gitbookSource.exists()) {
                        Process process = CommandExecutor.exec("gitbook build", Log::println, null, gitbookSource);
                        process.waitFor();
                        process = CommandExecutor.exec("gitbook pdf", Log::println, null, gitbookSource);
                        process.waitFor();
                        File dstDir = new File(gitbookDir);
                        FileUtils.deleteDirectory(dstDir);
                        FileUtils.moveDirectory(generateBook, dstDir);
                        FileUtils.moveFile(new File(gitbookSource, "book.pdf"), new File(dstDir, dstDir.getName() + ".pdf"));
                    }
                }
            } catch (Throwable e) {
                Log.printStackTrace(e);
                Log.println("oops, " + e, Log.RED);
            }
        }
    }
}
