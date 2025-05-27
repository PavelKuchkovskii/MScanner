package Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Запуск:
 *   java Main "C:\\path\\to\\all_files_combined.txt"
 *
 * Что делает:
 *   1. Читает объединённый файл.
 *   2. Ищет секции вида  ===== BEGIN FILE: <path> =====
 *   3. Пропускает «старые» файлы (у которых в <path> нет подкаталогов).
 *      Новыми считаем всё, что содержит хотя бы один '/' или '\'.
 *   4. Создаёт каталог %USERPROFILE%\Desktop\src\… и сохраняет контент
 *      каждой новой секции в соответствующий файл.
 */
public class Main {

    // Регэксп для строки-заголовка
    private static final Pattern HEADER =
            Pattern.compile("^===== BEGIN FILE: (.+?) =====");

    public static void main(String[] args) throws IOException {
        String path = "";
        if (args.length == 0) {
            path = "C:\\Users\\Admin\\Downloads\\all_files_combined.txt";
        }

        Path combinedFile = Paths.get(path);
        if (!Files.isRegularFile(combinedFile)) {
            System.err.println("Файл не найден: " + combinedFile);
            return;
        }

        // Папка src на рабочем столе Windows
        Path desktopSrc = Paths.get(System.getProperty("user.home"), "Desktop", "src");
        Files.createDirectories(desktopSrc);

        try (BufferedReader br = Files.newBufferedReader(combinedFile, StandardCharsets.UTF_8)) {
            String line;
            String currentPath = null;
            StringBuilder content = null;
            boolean skipMeta = false;

            while ((line = br.readLine()) != null) {
                Matcher m = HEADER.matcher(line);
                if (m.find()) {
                    // Сохраняем предыдущий файл, если был
                    if (currentPath != null && content != null) {
                        writeFile(desktopSrc, currentPath, content.toString());
                    }
                    currentPath = m.group(1).trim();
                    content = new StringBuilder();
                    skipMeta = true;          // следующие строки – Size / Last Modified / blank
                    continue;
                }

                if (currentPath != null && content != null) {
                    if (skipMeta) {
                        // ждём пустой строки после мета-информации
                        if (line.isEmpty()) skipMeta = false;
                        continue;
                    }
                    content.append(line).append(System.lineSeparator());
                }
            }
            // сохранить последнюю секцию
            if (currentPath != null && content != null) {
                writeFile(desktopSrc, currentPath, content.toString());
            }
        }
        System.out.println("Готово! Новые файлы распакованы в: " + desktopSrc);
    }

    /** Создаёт файл, если он считается «новым», и записывает туда content. */
    private static void writeFile(Path root, String relativePath, String content) throws IOException {
        // Старые файлы лежат без подкаталогов → пропускаем, если нет разделителей
        if (!(relativePath.contains("/") || relativePath.contains("\\"))) {
            return;
        }

        Path target = root.resolve(relativePath.replace('\\', '/'));
        Files.createDirectories(target.getParent());
        Files.write(target, content.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
}
