package de.health.service.cetp.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.logging.Logger;

@SuppressWarnings("unused")
public class Utils {

    private final static Logger log = Logger.getLogger(Utils.class.getName());

    private Utils() {
    }

    public static void writeFile(String absolutePath, String content) throws IOException {
        try (FileOutputStream os = new FileOutputStream(absolutePath)) {
            if (content != null) {
                os.write(content.getBytes());
            }
            os.flush();
        }
    }

    public static void saveDataToFile(byte[] dataForWriting, File outputFile) throws IOException {
        if (!outputFile.exists()) {
            outputFile.createNewFile();
        }
        try (FileOutputStream os = new FileOutputStream(outputFile)) {
            os.write(dataForWriting);
        }
    }

    public static boolean deleteFiles(File folder, Predicate<File> predicate) {
        AtomicBoolean result = new AtomicBoolean(true);
        File[] files = folder.listFiles();
        if (files == null) {
            return false;
        }
        Arrays.stream(files)
            .filter(predicate)
            .forEach(file -> result.set(result.get() & file.delete()));
        return result.get();
    }

    public static String printException(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String stacktrace = sw.toString();
        return e.getMessage() + " -> " + stacktrace;
    }

    public static Optional<String> getHostFromNetworkInterfaces() {
        Inet4Address localAddress = null;
        try {
            Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
            while (e.hasMoreElements()) {
                NetworkInterface n = e.nextElement();
                Enumeration<InetAddress> ee = n.getInetAddresses();
                while (ee.hasMoreElements()) {
                    InetAddress i = ee.nextElement();
                    if (i instanceof Inet4Address && i.getAddress()[0] != 127) {
                        localAddress = (Inet4Address) i;
                        break;
                    }
                }
            }
        } catch (SocketException ignored) {
        }
        return localAddress == null
            ? Optional.empty()
            : Optional.of(localAddress.getHostAddress());
    }

    public static void terminateExecutor(ExecutorService executorService, String executorName, int awaitMillis) {
        if (executorService != null) {
            log.info(String.format("[%s] Terminating", executorName));
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(awaitMillis, TimeUnit.MILLISECONDS)) {
                    executorService.shutdownNow();
                    if (!executorService.awaitTermination(awaitMillis, TimeUnit.MILLISECONDS)) {
                        log.info(String.format("[%s] Is not terminated", executorName));
                    }
                }
            } catch (InterruptedException ex) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}