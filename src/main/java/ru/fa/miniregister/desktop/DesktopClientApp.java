package ru.fa.miniregister.desktop;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.regex.*;

/**
 * Десктопный Swing-клиент MiniRegister.
 * Кнопки вызывают REST API сервера через java.net.http.HttpClient.
 */
public class DesktopClientApp {

    private static final String BASE = "http://localhost:8080";

    private final HttpClient http = HttpClient.newHttpClient();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final JTextArea out = new JTextArea(22, 90);

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DesktopClientApp().show());
    }

    public void show() {
        JFrame frame = new JFrame("MiniRegister — Бронь переговорной (Swing)");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                scheduler.shutdown();
            }
        });

        JButton btnLoad   = new JButton("Загрузить брони");
        JButton btnCreate = new JButton("Создать тестовую бронь");
        JButton btnExport = new JButton("Экспорт CSV");
        JButton btnImport = new JButton("Импорт CSV...");

        // ActionListener — стандартный механизм событий Swing
        btnLoad  .addActionListener(e -> loadRecords());
        btnCreate.addActionListener(e -> createSample());
        btnExport.addActionListener(e -> exportCsv());
        btnImport.addActionListener(e -> importCsv(frame));

        JPanel top = new JPanel(new GridLayout(2, 2, 6, 4));
        top.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        top.add(btnLoad);
        top.add(btnCreate);
        top.add(btnExport);
        top.add(btnImport);

        out.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        out.setLineWrap(true);
        out.setWrapStyleWord(true);
        out.setEditable(false);

        frame.getContentPane().add(top, BorderLayout.NORTH);
        frame.getContentPane().add(new JScrollPane(out), BorderLayout.CENTER);

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        out.setText("1) Запустите сервер: mvn spring-boot:run\n2) Нажмите кнопку.\n");
    }

    // ── кнопки ──────────────────────────────────────────────────────────────

    private void loadRecords() {
        sendGet("/api/records");
    }

    private void createSample() {
        String json = """
                {"title":"Бронь переговорной B-101","description":"Тестовая бронь","status":"NEW"}
                """;
        sendPost("/api/records", json);
    }

    /** POST /api/jobs/export → получает jobId → опрашивает статус до завершения. */
    private void exportCsv() {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/api/jobs/export"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        http.sendAsync(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenAccept(resp -> {
                    SwingUtilities.invokeLater(() ->
                            out.setText("Экспорт запущен (HTTP " + resp.statusCode() + ")\n"
                                    + resp.body() + "\n\nОпрашиваю статус...\n"));
                    String jobId = extractJobId(resp.body());
                    if (jobId != null) pollJob(jobId);
                })
                .exceptionally(ex -> {
                    SwingUtilities.invokeLater(() -> out.setText("ERROR: " + ex.getMessage()));
                    return null;
                });
    }

    /**
     * Открывает диалог выбора файла из ./storage,
     * затем POST /api/jobs/import?fileName=... → опрашивает статус.
     */
    private void importCsv(JFrame parent) {
        File storageDir = new File("./storage");
        if (!storageDir.exists()) storageDir.mkdirs();

        JFileChooser chooser = new JFileChooser(storageDir);
        chooser.setDialogTitle("Выберите CSV-файл для импорта (из папки storage)");
        chooser.setFileFilter(new FileNameExtensionFilter("CSV files (*.csv)", "csv"));

        if (chooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) return;

        String fileName = chooser.getSelectedFile().getName();
        String url = BASE + "/api/jobs/import?fileName="
                + URLEncoder.encode(fileName, StandardCharsets.UTF_8);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        http.sendAsync(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenAccept(resp -> {
                    SwingUtilities.invokeLater(() ->
                            out.setText("Импорт «" + fileName + "» запущен (HTTP " + resp.statusCode() + ")\n"
                                    + resp.body() + "\n\nОпрашиваю статус...\n"));
                    String jobId = extractJobId(resp.body());
                    if (jobId != null) pollJob(jobId);
                })
                .exceptionally(ex -> {
                    SwingUtilities.invokeLater(() -> out.setText("ERROR: " + ex.getMessage()));
                    return null;
                });
    }

    // ── вспомогательные методы ───────────────────────────────────────────────

    /** Опрашивает GET /api/jobs/{jobId} каждые 500 мс, пока статус не DONE или FAILED. */
    private void pollJob(String jobId) {
        scheduler.schedule(() -> {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE + "/api/jobs/" + jobId))
                    .GET()
                    .build();
            try {
                HttpResponse<String> resp = http.send(req,
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                String body = resp.body();
                if (body.contains("\"DONE\"") || body.contains("\"FAILED\"")) {
                    SwingUtilities.invokeLater(() -> out.append("Готово:\n" + body + "\n"));
                } else {
                    SwingUtilities.invokeLater(() -> out.append("...RUNNING\n"));
                    pollJob(jobId); // рекурсивный повтор через 500 мс
                }
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> out.append("ERROR при опросе: " + e.getMessage() + "\n"));
            }
        }, 500, TimeUnit.MILLISECONDS);
    }

    /** Извлекает UUID jobId из JSON-ответа регулярным выражением. */
    private String extractJobId(String json) {
        Matcher m = Pattern.compile("\"jobId\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        return m.find() ? m.group(1) : null;
    }

    private void sendGet(String path) {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + path)).GET().build();
        http.sendAsync(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenAccept(resp -> SwingUtilities.invokeLater(() ->
                        out.setText("HTTP " + resp.statusCode() + "\n" + resp.body())))
                .exceptionally(ex -> {
                    SwingUtilities.invokeLater(() -> out.setText("ERROR: " + ex.getMessage()));
                    return null;
                });
    }

    private void sendPost(String path, String json) {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        http.sendAsync(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenAccept(resp -> SwingUtilities.invokeLater(() ->
                        out.setText("HTTP " + resp.statusCode() + "\n" + resp.body())))
                .exceptionally(ex -> {
                    SwingUtilities.invokeLater(() -> out.setText("ERROR: " + ex.getMessage()));
                    return null;
                });
    }
}
