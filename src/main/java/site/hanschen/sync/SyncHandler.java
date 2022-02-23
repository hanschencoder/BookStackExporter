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
import java.nio.file.Files;
import java.nio.file.Paths;
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
    private final String vuePressTemplate;
    private final String vuePressDir;

    public SyncHandler(String outDir,
                       String baseUrl,
                       String tokenId,
                       String tokenSecret,
                       String gitlabToken,
                       String vuePressTemplate,
                       String vuePressDir) {
        this.outDir = outDir;
        this.baseUrl = baseUrl;
        this.tokenId = tokenId;
        this.tokenSecret = tokenSecret;
        this.gitlabToken = gitlabToken;
        this.vuePressTemplate = vuePressTemplate;
        this.vuePressDir = vuePressDir;
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
                Log.println(commitMessage, Log.GREEN);
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

                File sourceRoot = new File(outDir);
                File[] source = sourceRoot.listFiles();
                if (source == null || source.length <= 0) {
                    return;
                }
                for (File gitbookSource : source) {
                    if (!gitbookSource.isDirectory() || gitbookSource.getName().equals(".git")) {
                        continue;
                    }
                    if (gitbookSource.exists()) {
                        Log.println("gitbook build: " + gitbookSource.getName());
                        Process process = CommandExecutor.exec("gitbook build", log -> {
                        }, null, gitbookSource);
                        process.waitFor();
                        Log.println("gitbook pdf: " + gitbookSource.getName());
                        process = CommandExecutor.exec("gitbook pdf", log -> {
                        }, null, gitbookSource);
                        process.waitFor();
                        Log.println("generate success: " + gitbookSource.getName() + ".pdf");
                    }
                }

                File vueTemplate = new File(vuePressTemplate);
                FileUtils.copyFile(new File(sourceRoot, "VuePressConfig.js"), new File(vueTemplate, "/docs/.vuepress/config.js"));

                for (File gitbookSource : source) {
                    if (!gitbookSource.isDirectory() || gitbookSource.getName().equals(".git")) {
                        continue;
                    }
                    createSymbolicLink(new File(vueTemplate, "/docs/" + gitbookSource.getName()), gitbookSource);

                    File symbolAsset = new File(vueTemplate, "/docs/.vuepress/public/" + gitbookSource.getName() + "/assets");
                    FileUtils.forceMkdirParent(symbolAsset);
                    createSymbolicLink(symbolAsset, new File(gitbookSource, "assets"));

                    FileUtils.deleteDirectory(new File(gitbookSource, "_book"));
                    File targetPdf = new File(vueTemplate, "/docs/.vuepress/public/" + gitbookSource.getName() + ".pdf");
                    if (targetPdf.exists()) {
                        FileUtils.delete(targetPdf);
                    }
                    FileUtils.moveFile(new File(gitbookSource, "book.pdf"), targetPdf);
                }

                Log.println("yarn docs:build start");
                Process process = CommandExecutor.exec("yarn docs:build", log -> {
                }, null, vueTemplate);
                process.waitFor();
                FileUtils.deleteDirectory(new File(vuePressDir));
                FileUtils.moveDirectory(new File(vueTemplate, "docs/.vuepress/dist"), new File(vuePressDir));
                Log.println("yarn docs:build success");
            } catch (Throwable e) {
                Log.printStackTrace(e);
                Log.println("oops, " + e, Log.RED);
            }
        }

        private void createSymbolicLink(File link, File target) throws IOException {
            if (link.exists()) {
                return;
            }
            Files.createSymbolicLink(Paths.get(link.getCanonicalPath()), Paths.get(target.getCanonicalPath()));

        }
    }
}
