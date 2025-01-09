import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class ShortLinkService {

    private static final Map<String, User> users = new HashMap<>();
    private static final Map<String, UrlEntry> shortToUrl = new ConcurrentHashMap<>();
    private static final Map<String, ScheduledFuture<?>> cleanupTasks = new ConcurrentHashMap<>();

    private static final Properties config = new Properties();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static void main(String[] args) {
        loadConfig();
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("Добро пожаловать в сервис коротких ссылок! Введите свой UUID для запуска (или введите \"exit\" для завершения работы):");
            String uuid = scanner.nextLine();
            if ("exit".equalsIgnoreCase(uuid)) {
                shutdownService();
                break;
            }

            User user = users.computeIfAbsent(uuid, User::new);

            while (true) {
                System.out.println("\n1. Сократить URL-адрес\n2. Перейти по короткой ссылке\n3. Обновить лимит URL\n4. Удалить URL\n5. Список ссылок\n6. Выход");
                int choice = scanner.nextInt();
                scanner.nextLine();

                switch (choice) {
                    case 1:
                        System.out.println("Введите URL-адрес, который нужно сократить:");
                        String url = scanner.nextLine();
                        System.out.println("Введите время жизни ссылки в секундах (или 0 по умолчанию):");
                        long ttl = scanner.nextLong();
                        scanner.nextLine();
                        System.out.println("Введите лимит переходов (посещений) по ссылке (или 0 по умолчанию):");
                        int visitLimit = scanner.nextInt();
                        scanner.nextLine();

                        String shortUrl = user.shortenUrl(url, ttl, visitLimit);
                        System.out.println("Сокращенный URL-адрес: " + shortUrl);
                        break;
                    case 2:
                        System.out.println("Введите короткий URL-адрес:");
                        String shortUrlVisit = scanner.nextLine();
                        user.visitShortUrl(shortUrlVisit);
                        break;
                    case 3:
                        System.out.println("Введите короткий URL-адрес для обновления:");
                        String shortUrlUpdate = scanner.nextLine();
                        System.out.println("Введите новый лимит посещений:");
                        int newLimit = scanner.nextInt();
                        scanner.nextLine();
                        user.updateVisitLimit(shortUrlUpdate, newLimit);
                        break;
                    case 4:
                        System.out.println("Введите короткий URL-адрес, который нужно удалить:");
                        String shortUrlDelete = scanner.nextLine();
                        user.deleteShortUrl(shortUrlDelete);
                        break;
                    case 5:
                        user.listUrls();
                        break;
                    case 6:
                        System.out.println("Выход.");
                        break;
                    default:
                        System.out.println("Неверный выбор. Попробуйте снова.");
                }
                if (choice == 6) {
                    break;
                }
            }
        }
    }

    private static void loadConfig() {
        try (InputStream input = new FileInputStream("config.properties")) {
            config.load(input);
        } catch (IOException e) {
            System.out.println("Ошибка при загрузке конфигурационного файла: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void shutdownService() {
        scheduler.shutdownNow();
        cleanupTasks.forEach((shortUrl, task) -> {
            if (!task.isDone()) {
                task.cancel(true);
            }
        });
        cleanupTasks.clear();
        shortToUrl.clear();
        System.out.println("Выключение сервиса.");
    }

    private static class User {
        private final String uuid;
        private final Map<String, UrlEntry> userUrls = new HashMap<>();

        User(String uuid) {
            this.uuid = uuid;
        }

        String shortenUrl(String originalUrl, long ttl, int visitLimit) {
            String shortUrl = UUID.randomUUID().toString().substring(0, 8);
            long actualTtl = Math.min(ttl > 0 ? ttl : Long.MAX_VALUE, Long.parseLong(config.getProperty("default_ttl", "3600")));
            int actualLimit = Math.max(Math.max(visitLimit, 0), Integer.parseInt(config.getProperty("default_visit_limit", "100")));

            UrlEntry entry = new UrlEntry(originalUrl, shortUrl, actualTtl, actualLimit, this.uuid);
            userUrls.put(shortUrl, entry);
            shortToUrl.put(shortUrl, entry);

            ScheduledFuture<?> cleanupTask = scheduler.schedule(() -> deleteShortUrl(shortUrl), actualTtl, TimeUnit.SECONDS);
            cleanupTasks.put(shortUrl, cleanupTask);

            return shortUrl;
        }

        void visitShortUrl(String shortUrl) {
            UrlEntry entry = shortToUrl.get(shortUrl);
            if (entry == null || !entry.getOwnerUuid().equals(this.uuid)) {
                System.out.println("URL-адрес не найден или в доступе отказано.");
                return;
            }

            if (entry.incrementVisitCount()) {
                System.out.println("Перенаправление на: " + entry.getOriginalUrl());
            } else {
                System.out.println("URL-адрес недоступен (истек срок действия или достигнут лимит посещений).");
            }
        }

        void updateVisitLimit(String shortUrl, int newLimit) {
            UrlEntry entry = shortToUrl.get(shortUrl);
            if (entry == null || !entry.getOwnerUuid().equals(this.uuid)) {
                System.out.println("URL-адрес не найден или в доступе отказано.");
                return;
            }
            entry.setVisitLimit(newLimit);
            System.out.println("Лимит посещений обновлен.");
        }

        void deleteShortUrl(String shortUrl) {
            UrlEntry entry = userUrls.remove(shortUrl);
            if (entry != null) {
                shortToUrl.remove(shortUrl);
                ScheduledFuture<?> cleanupTask = cleanupTasks.remove(shortUrl);
                if (cleanupTask != null) cleanupTask.cancel(true);
                System.out.println("Короткая ссылка " + shortUrl + " удалена.");
            } else {
                System.out.println("Короткая ссылка " + shortUrl + " не найдена.");
            }
        }

        void listUrls() {
            if (userUrls.isEmpty()) {
                System.out.println("URL-адреса не найдены.");
                return;
            }

            userUrls.values().forEach(System.out::println);
        }
    }

    private static class UrlEntry {
        private final String originalUrl;
        private final String shortUrl;
        private final long expiryTime;
        private final String ownerUuid;
        private int visitLimit;
        private int visitCount;

        UrlEntry(String originalUrl, String shortUrl, long ttl, int visitLimit, String ownerUuid) {
            this.originalUrl = originalUrl;
            this.shortUrl = shortUrl;
            this.expiryTime = System.currentTimeMillis() + ttl * 1000;
            this.visitLimit = visitLimit;
            this.ownerUuid = ownerUuid;
        }

        String getOriginalUrl() {
            return originalUrl;
        }

        String getOwnerUuid() {
            return ownerUuid;
        }

        boolean incrementVisitCount() {
            if (visitCount >= visitLimit || System.currentTimeMillis() > expiryTime) {
                return false;
            }
            visitCount++;
            return true;
        }

        void setVisitLimit(int newLimit) {
            this.visitLimit = newLimit;
        }

        @Override
        public String toString() {
            return "Короткая ссылка: " + shortUrl + ", Исходная ссылка: " + originalUrl + ", Посещения: " + visitCount + "/" + visitLimit;
        }
    }
}
