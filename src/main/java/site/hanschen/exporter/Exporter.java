package site.hanschen.exporter;

import okhttp3.ResponseBody;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import site.hanschen.api.BookStackApi;
import site.hanschen.entry.*;
import site.hanschen.utils.Log;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Exporter {

    private final String mOutDir;
    private final String mExportType;
    private final String mBaseUrl;
    private final String mToken;
    private final boolean mForceOverwrite;
    private final BookStackApi mBookStackApi;
    private final Pattern imagePattern;

    public Exporter(String outDir, String exportType, String baseUrl, String tokenId, String tokenSecret, boolean forceOverwrite) {
        mOutDir = outDir;
        mExportType = exportType;
        mBaseUrl = baseUrl;
        mToken = "Token " + tokenId + ":" + tokenSecret;
        mForceOverwrite = forceOverwrite;

        imagePattern = Pattern.compile(baseUrl + "/uploads/images/(gallery|drawio)[^)\"]+(.png|.jpg|.gif){1}");

        Retrofit retrofit = new Retrofit.Builder().baseUrl(mBaseUrl).addConverterFactory(GsonConverterFactory.create()).build();
        mBookStackApi = retrofit.create(BookStackApi.class);
    }

    public void start() throws Exception {
        Log.println("BookStackExporter start...", Log.GREEN);
        File outDir = new File(mOutDir);
        if (outDir.exists()) {
            if (mForceOverwrite) {
                deleteExclude(outDir,
                              pathname -> pathname.getName().equals(".git") ||
                                          pathname.getName().equals(".gitignore") ||
                                          pathname.getName().equals("node_modules") ||
                                          pathname.getName().equals("book.json") ||
                                          pathname.getName().equals("bookinfo"));
            } else {
                Log.println(outDir.getCanonicalPath() + " already exists, use [-f] option to overwrite.", Log.RED);
                return;
            }

        }
        FileUtils.forceMkdir(outDir);
        Log.println("ExportType=" + mExportType + ", OutDir=" + outDir.getCanonicalPath());

        Node<?> root = buildBookHierarchy();
        root.path = outDir;
        dumpBook(root, null, "");

        if (isGitBook()) {
            dumpBookSummary(root);
            downloadImage(root);
        }
    }

    private void deleteExclude(File file, FileFilter fileFilter) throws IOException {
        if (fileFilter.accept(file)) {
            return;
        }
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    deleteExclude(f, fileFilter);
                }
            }
            files = file.listFiles();
            if (files == null || files.length == 0) {
                FileUtils.deleteDirectory(file);
            }
        } else {
            FileUtils.forceDelete(file);
        }
    }

    private Node<?> buildBookHierarchy() throws Exception {

        Node<?> rootNode = new Node<>(null);

        HashMap<Integer, Node<Book>> books = new HashMap<>();
        HashMap<Integer, Node<Chapter>> chapters = new HashMap<>();

        // 1. fetch books
        Data allBook = callApi(() -> mBookStackApi.getAllBook(mToken).execute());
        for (Data.DataEntry entry : allBook.data) {
            Book book = callApi(() -> mBookStackApi.getBook(mToken, entry.id).execute());
            Node<Book> bookNode = new Node<>(book);
            rootNode.children.add(bookNode);
            books.put(entry.id, bookNode);
        }

        // 2. fetch chapters
        Data allChapter = callApi(() -> mBookStackApi.getAllChapter(mToken).execute());
        for (Data.DataEntry entry : allChapter.data) {
            Chapter chapter = callApi(() -> mBookStackApi.getChapter(mToken, entry.id).execute());
            Node<Book> bookNode = books.get(chapter.bookId);
            if (bookNode == null) {
                throw new IllegalStateException("book not found, chapter=" + chapter.name);
            }

            Node<Chapter> chapterNode = new Node<>(chapter);
            bookNode.children.add(chapterNode);
            chapters.put(entry.id, chapterNode);
        }

        // 3. fetch pages
        Data allPage = callApi(() -> mBookStackApi.getAllPage(mToken).execute());
        for (Data.DataEntry entry : allPage.data) {
            Page page = callApi(() -> mBookStackApi.getPage(mToken, entry.id).execute());
            Node<Page> pageNode = new Node<>(page);
            Node<Chapter> chapterNode = chapters.get(page.chapterId);
            if (chapterNode != null) {
                chapterNode.children.add(pageNode);
            } else {
                Node<Book> bookNode = books.get(page.bookId);
                if (bookNode == null) {
                    throw new IllegalStateException("book not found, page=" + page.name);
                }
                bookNode.children.add(pageNode);
            }
        }

        // 4. sort by priority
        sortNode(rootNode);

        // 5. resolve duplicate name
        resolveDuplicateName(rootNode);

        return rootNode;
    }

    private static <T> T callApi(Callable<Response<T>> callable) throws Exception {
        Response<T> response = callable.call();
        if (!response.isSuccessful()) {
            throw new IOException("response.code()=" + response.code());
        }
        if (response.body() == null) {
            throw new IOException("response.body()=null");
        }
        return response.body();
    }

    private void sortNode(Node<?> node) {
        if (node.children.size() >= 2) {
            Collections.sort(node.children);
        }
        for (Node<?> n : node.children) {
            sortNode(n);
        }
    }

    private void resolveDuplicateName(Node<?> node) {
        if (node.children.size() >= 2) {
            Map<String, Integer> nameCount = new HashMap<>();
            for (Node<?> n : node.children) {
                String name = n.getFileName();
                Integer count = nameCount.get(name);
                if (count == null) {
                    count = 1;
                } else {
                    count = count + 1;
                }
                nameCount.put(name, count);

                if (count > 1) {
                    n.setFilename(name + count);
                }
            }
        }
        for (Node<?> n : node.children) {
            resolveDuplicateName(n);
        }
    }

    private void dumpBook(Node<?> node, File parentDir, String prefix) throws Exception {
        if (node.object != null) {
            if (node.object instanceof Book || node.object instanceof Chapter) {
                File dir = new File(parentDir, node.getFileName());
                FileUtils.forceMkdir(dir);
                node.path = dir;

                if (isGitBook()) {
                    String markdown = node.generateMarkDown();
                    if (markdown != null) {
                        File file = new File(dir, "README.md");
                        FileUtils.writeStringToFile(file, markdown, StandardCharsets.UTF_8);
                    }
                }
                Log.println(prefix + " - " + node.getFileName());
            } else if (node.object instanceof Page) {
                Page page = (Page) node.object;
                ResponseBody responseBody = callApi(() -> mBookStackApi.getExportFile(mToken, page.id, getFileType()).execute());
                File dst = new File(parentDir, node.getFileName() + getFileSuffix());
                IOUtils.copy(responseBody.byteStream(), new FileOutputStream(dst));
                node.path = dst;
                Log.println(prefix + " - " + dst.getName());
            }
        } else {
            Log.println(prefix + " - " + node.path.getName());
        }

        for (Node<?> n : node.children) {
            dumpBook(n, node.path, prefix + "    ");
        }
    }

    private String getFileSuffix() {
        switch (mExportType) {
            case "html":
                return ".html";
            case "pdf":
                return ".pdf";
            case "plaintext":
                return ".txt";
            case "markdown":
            case "gitbook":
                return ".md";
            default:
                throw new IllegalStateException("Unsupported export type: " + mExportType);
        }
    }

    private String getFileType() {
        switch (mExportType) {
            case "html":
                return "html";
            case "pdf":
                return "pdf";
            case "plaintext":
                return "txt";
            case "markdown":
            case "gitbook":
                return "markdown";
            default:
                throw new IllegalStateException("Unsupported export type: " + mExportType);
        }
    }

    private boolean isGitBook() {
        return "gitbook".equals(mExportType);
    }

    private void dumpBookSummary(Node<?> root) throws IOException {
        for (Node<?> node : root.children) {
            if (node.object instanceof Book) {
                StringBuilder builder = new StringBuilder();
                handleDumpSummary(node, node.path, "", "", builder);
                FileUtils.writeStringToFile(new File(node.path, "SUMMARY.md"), builder.toString(), StandardCharsets.UTF_8);
            }
        }
    }

    private void handleDumpSummary(Node<?> node, File rootDir, String prefix, String currentLevel, StringBuilder builder) throws IOException {

        String relativePath = node.path.getCanonicalPath().replace(rootDir.getCanonicalPath(), "");
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.replaceFirst("/", "");
        }

        if (node.object instanceof Book) {
            builder.append(prefix).append("# ").append(node.getName()).append("\n\n");
        } else if (node.object instanceof Chapter) {
            builder.append(prefix)
                    .append("* [")
                    .append(currentLevel)
                    .append(" ")
                    .append(node.getName())
                    .append("](")
                    .append(relativePath)
                    .append("/README.md)\n");
        } else if (node.object instanceof Page) {
            builder.append(prefix)
                    .append("* [")
                    .append(currentLevel)
                    .append(" ")
                    .append(node.getName())
                    .append("](")
                    .append(relativePath)
                    .append(")\n");
        }

        for (Node<?> n : node.children) {
            String level = currentLevel + (node.children.indexOf(n) + 1) + ".";
            handleDumpSummary(n, rootDir, (node.object instanceof Book) ? prefix : "    " + prefix, level, builder);
        }
    }

    private void downloadImage(Node<?> root) throws IOException {
        for (Node<?> node : root.children) {
            if (node.object instanceof Book) {
                File assetsDir = new File(node.path, "assets");
                FileUtils.forceMkdir(assetsDir);
                ImageDownloader downloader = new ImageDownloader();
                forAllFile(node.path, new DownloadHandler(assetsDir, downloader));
                List<DownLoadResult> results = downloader.start();
                Map<String, File> urls = new HashMap<>();
                for (DownLoadResult result : results) {
                    if (result.success) {
                        urls.put(result.url, result.path);
                    }
                }
                forAllFile(node.path, new ReplaceHandler(urls));
            }
        }
    }

    private class DownloadHandler implements FileHandler {

        private final File assetsDir;
        private final ImageDownloader imageDownloader;

        public DownloadHandler(File assetsDir, ImageDownloader imageDownloader) {
            this.assetsDir = assetsDir;
            this.imageDownloader = imageDownloader;
        }

        @Override
        public void handle(File file) throws IOException {
            String content = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
            Matcher m = imagePattern.matcher(content);
            while (m.find()) {
                String url = m.group(0);
                File path = new File(assetsDir, url.replace(mBaseUrl, ""));
                imageDownloader.addTask(url, path);
            }
        }
    }

    private class ReplaceHandler implements FileHandler {

        private final Map<String, File> urls;

        public ReplaceHandler(Map<String, File> urls) {
            this.urls = urls;
        }

        @Override
        public void handle(File file) throws IOException {
            String content = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
            Matcher m = imagePattern.matcher(content);
            StringBuffer contentBuilder = new StringBuffer();
            while (m.find()) {
                String remoteUrl = m.group(0);
                File localFile = urls.get(remoteUrl);
                if (localFile != null && localFile.exists()) {
                    String relative = Paths.get(file.getParentFile().getCanonicalPath())
                            .relativize(Paths.get(localFile.getCanonicalPath()))
                            .toString();
                    m.appendReplacement(contentBuilder, relative);
                } else {
                    m.appendReplacement(contentBuilder, m.group(0));
                }
            }
            m.appendTail(contentBuilder);
            FileUtils.writeStringToFile(file, contentBuilder.toString(), StandardCharsets.UTF_8);
        }
    }

    private void forAllFile(File file, FileHandler handler) throws IOException {
        if (file.isFile() && file.getName().endsWith(".md")) {
            handler.handle(file);
            return;
        }
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files == null) {
                return;
            }
            for (File f : files) {
                forAllFile(f, handler);
            }
        }
    }

    private interface FileHandler {

        void handle(File file) throws IOException;
    }

    private static class Node<T> implements Comparable<Node<?>> {

        private final List<Node<?>> children = new ArrayList<>();
        private final T object;
        private File path;
        private String filename;

        private Node(T object) {
            this.object = object;
        }

        public String getFileName() {
            if (filename != null) {
                return filename;
            }
            String name = getName();
            name = name.toLowerCase().replaceAll("[\"#%&()+,/:;<=>?@|\\\\]", "");
            if ("README.md".equals(name)) {
                name = "_" + name;
            }
            return name;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public String getName() {
            if (object instanceof Book) {
                return ((Book) object).name;
            } else if (object instanceof Chapter) {
                return ((Chapter) object).name;
            } else if (object instanceof Page) {
                return ((Page) object).name;
            }
            return "";
        }

        @Override
        public int compareTo(Node node) {
            if (object == null || node.object == null) {
                return 0;
            }
            if (object instanceof Book || node.object instanceof Book) {
                return 0;
            }

            int firstPriority;
            if (object instanceof Chapter) {
                firstPriority = ((Chapter) object).priority;
            } else {
                firstPriority = ((Page) object).priority;
            }

            int secondPriority;
            if (node.object instanceof Chapter) {
                secondPriority = ((Chapter) node.object).priority;
            } else {
                secondPriority = ((Page) node.object).priority;
            }

            return Integer.compare(firstPriority, secondPriority);
        }

        public String generateMarkDown() {
            if (object instanceof Book) {
                return ((Book) object).generateMarkDown();
            } else if (object instanceof Chapter) {
                return ((Chapter) object).generateMarkDown();
            }
            return null;
        }
    }

    private class ImageDownloader {

        private final Map<String, File> tasks = new HashMap<>();
        private final ExecutorService executor = Executors.newScheduledThreadPool(3);

        void addTask(String url, File path) {
            tasks.computeIfAbsent(url, k -> path);
        }

        List<DownLoadResult> start() {
            List<Future<DownLoadResult>> futures = new ArrayList<>();
            for (Map.Entry<String, File> entry : tasks.entrySet()) {
                Future<DownLoadResult> future = executor.submit(new DownloadTask(entry.getKey(), entry.getValue()));
                futures.add(future);
            }

            List<DownLoadResult> results = new ArrayList<>();
            for (Future<DownLoadResult> future : futures) {
                try {
                    results.add(future.get());
                } catch (Exception ignored) {
                }
            }
            executor.shutdown();
            return results;
        }
    }

    private class DownloadTask implements Callable<DownLoadResult> {

        private final String url;
        private final File path;

        public DownloadTask(String url, File path) {
            this.url = url;
            this.path = path;
        }

        @Override
        public DownLoadResult call() {
            try {
                ResponseBody responseBody = callApi(() -> mBookStackApi.getImage(url.replace(mBaseUrl, "")).execute());
                FileUtils.forceMkdir(path.getParentFile());
                IOUtils.copy(responseBody.byteStream(), new FileOutputStream(path));
                return new DownLoadResult(url, path, true);
            } catch (Exception e) {
                try {
                    Log.println("Downloaded failed: " + url + " -> " + path.getCanonicalPath() + ", e=" + e, Log.RED);
                } catch (IOException ignored) {
                }
                return new DownLoadResult(url, path, false);
            }
        }
    }

    private static class DownLoadResult {

        private final String url;
        private final File path;
        private final boolean success;

        public DownLoadResult(String url, File path, boolean success) {
            this.url = url;
            this.path = path;
            this.success = success;
        }
    }
}
