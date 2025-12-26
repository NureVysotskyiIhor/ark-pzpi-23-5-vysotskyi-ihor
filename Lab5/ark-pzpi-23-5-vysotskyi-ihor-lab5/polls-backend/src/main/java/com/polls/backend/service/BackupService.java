package com.polls.backend.service;

import com.polls.backend.dto.DbInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class BackupService {

    private static final Logger logger = LoggerFactory.getLogger(BackupService.class);
    private static final DateTimeFormatter dateFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Value("${backup.directory:./backups}")
    private String backupDir;

    /**
     * ФАЗА 1: Створення резервної копії
     * Використовує pg_dump для PostgreSQL
     */
    public String createBackup() {
        try {
            // Створення директорії, якщо не існує
            Files.createDirectories(Paths.get(backupDir));

            // Генерування імені файлу з часовою міткою
            String timestamp = LocalDateTime.now().format(dateFormatter);
            String filename = "polls_backup_" + timestamp + ".sql";
            String filepath = backupDir + "/" + filename;

            // Витягування деталей БД з URL
            // jdbc:postgresql://host:port/dbname → host, port, dbname
            DbInfo db = parseJdbcUrl(dbUrl);

            String dbName = db.dbName();
            String dbHost = db.host();
            String dbPort = db.port();

            // МАТЕМАТИКА: Команда pg_dump
            // pg_dump -h HOST -p PORT -U USERNAME -F plain DATABASE > backup.sql
            ProcessBuilder pb = new ProcessBuilder(
                    "pg_dump",
                    "-h", dbHost,
                    "-p", dbPort,
                    "-U", dbUsername,
                    "-F", "plain",  // plain text format (не binary)
                    "-v",           // verbose
                    dbName
            );

            // Встановлення змінної середовища для пароля
            Map<String, String> env = pb.environment();
            env.put("PGPASSWORD", dbPassword);

            // Перенаправлення виходу у файл
            pb.redirectOutput(new File(filepath));
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);

            // ВИКОНАННЯ
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                logger.info("✅ Резервна копія успішно створена: {}", filepath);

                // Отримання розміру файлу
                long fileSize = Files.size(Paths.get(filepath));
                logger.info("📊 Розмір файлу: {} байт", fileSize);

                return filepath;
            } else {
                logger.error("❌ Помилка при створенні резервної копії. Exit code: {}",
                        exitCode);
                throw new RuntimeException("pg_dump failed with exit code: " + exitCode);
            }

        } catch (Exception e) {
            logger.error("❌ Помилка при створенні резервної копії", e);
            throw new RuntimeException("Backup failed: " + e.getMessage(), e);
        }
    }

    /**
     * ФАЗА 2: Відновлення з резервної копії
     * Використовує psql для PostgreSQL
     */
    public boolean restoreBackup(String backupFilepath) {
        try {
            // Перевірка існування файлу
            if (!Files.exists(Paths.get(backupFilepath))) {
                logger.error("❌ Файл резервної копії не знайдено: {}", backupFilepath);
                return false;
            }

            DbInfo db = parseJdbcUrl(dbUrl);

            String dbName = db.dbName();
            String dbHost = db.host();
            String dbPort = db.port();

            // МАТЕМАТИКА: Команда відновлення
            // psql -h HOST -p PORT -U USERNAME -d DATABASE < backup.sql
            ProcessBuilder pb = new ProcessBuilder(
                    "psql",
                    "-h", dbHost,
                    "-p", dbPort,
                    "-U", dbUsername,
                    "-d", dbName,
                    "-f", backupFilepath  // читання з файлу
            );

            // Встановлення змінної середовища для пароля
            Map<String, String> env = pb.environment();
            env.put("PGPASSWORD", dbPassword);

            pb.redirectError(ProcessBuilder.Redirect.INHERIT);

            // ВИКОНАННЯ
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                logger.info("✅ Резервна копія успішно відновлена з: {}",
                        backupFilepath);
                return true;
            } else {
                logger.error("❌ Помилка при відновленні резервної копії. Exit code: {}",
                        exitCode);
                return false;
            }

        } catch (Exception e) {
            logger.error("❌ Помилка при відновленні резервної копії", e);
            return false;
        }
    }

    /**
     * ФАЗА 3: Отримання списку резервних копій
     */

    /**
     * ФАЗА 3: Отримання списку резервних копій
     */
    public List<Map<String, Object>> listBackups() {
        List<Map<String, Object>> backups = new ArrayList<>();

        try {
            if (!Files.exists(Paths.get(backupDir))) {
                return backups;
            }

            Files.list(Paths.get(backupDir))
                    .filter(path -> path.toString().endsWith(".sql"))
                    .forEach(path -> {
                        try {
                            Map<String, Object> backup = new HashMap<>();
                            backup.put("filename", path.getFileName().toString());
                            backup.put("filepath", path.toString());
                            backup.put("size", Files.size(path));

                            // ✅ Конвертуємо FileTime в мілісекунди (long)
                            long createdTime = Files.getLastModifiedTime(path).toMillis();
                            backup.put("created", createdTime);

                            // Або як ISO-8601 строка (красивіше):
                            // backup.put("created",
                            //    LocalDateTime.ofInstant(
                            //        Instant.ofEpochMilli(createdTime),
                            //        ZoneId.systemDefault()
                            //    )
                            // );

                            backups.add(backup);
                        } catch (IOException e) {
                            logger.warn("⚠️ Помилка при читанні інформації про файл: {}",
                                    path, e);
                        }
                    });

            logger.info("📋 Знайдено {} резервних копій", backups.size());
            return backups;

        } catch (IOException e) {
            logger.error("❌ Помилка при отриманні списку резервних копій", e);
            return backups;
        }
    }

    /**
     * ФАЗА 4: Видалення старих резервних копій
     * МАТЕМАТИКА: Видалення файлів старших за N днів
     */
    public int deleteOldBackups(int daysOld) {
        AtomicInteger deletedCount = new AtomicInteger(0);

        try {
            if (!Files.exists(Paths.get(backupDir))) {
                return 0;
            }

            long cutoffTime = System.currentTimeMillis() -
                    (daysOld * 24 * 60 * 60 * 1000L);  // N днів у мс

            Files.list(Paths.get(backupDir))
                    .filter(path -> path.toString().endsWith(".sql"))
                    .forEach(path -> {
                        try {
                            long lastModified = Files.getLastModifiedTime(path).toMillis();
                            if (lastModified < cutoffTime) {
                                Files.delete(path);
                                deletedCount.incrementAndGet();
                            }
                        } catch (IOException e) {
                            logger.warn("⚠️ Помилка при видаленні файлу: {}", path, e);
                        }
                    });

            logger.info("✅ Видалено {} старих резервних копій", deletedCount);
            return deletedCount.get();

        } catch (IOException e) {
            logger.error("❌ Помилка при видаленні старих резервних копій", e);
            return 0;
        }
    }

    // ========================================================================
    // ДОПОМІЖНІ МЕТОДИ
    // ========================================================================

    private DbInfo parseJdbcUrl(String jdbcUrl) {
        try {
            // убираем jdbc:
            URI uri = new URI(jdbcUrl.substring(5));

            String host = uri.getHost();
            int port = (uri.getPort() == -1) ? 5432 : uri.getPort();

            String path = uri.getPath(); // /neondb
            String dbName = path.startsWith("/") ? path.substring(1) : path;

            return new DbInfo(host, String.valueOf(port), dbName);
        } catch (Exception e) {
            throw new RuntimeException("Invalid JDBC URL: " + jdbcUrl, e);
        }
    }

}