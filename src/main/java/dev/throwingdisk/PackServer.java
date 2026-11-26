package dev.throwingdisk;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/** Hosts the generated resource pack over HTTP and sends it to players on join. */
final class PackServer {
    private final ThrowingDiskPlugin plugin;
    private HttpServer server;
    private ExecutorService executor;
    private byte[] data;
    private byte[] sha1;
    private String url;

    PackServer(ThrowingDiskPlugin plugin) {
        this.plugin = plugin;
    }

    void start() {
        String host = plugin.getConfig().getString("resourcepack.host", "");
        int port = plugin.getConfig().getInt("resourcepack.port", 8123);
        if (host.isBlank()) {
            plugin.getLogger().warning("resourcepack.host is empty in config.yml. Set it to your server's "
                    + "public IP or domain, then restart. The disk will look like a missing texture until then.");
            return;
        }
        try {
            data = ResourcePackGenerator.build();
            sha1 = MessageDigest.getInstance("SHA-1").digest(data);
            executor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "ThrowingDisk-PackServer");
                t.setDaemon(true);
                return t;
            });
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/pack.zip", ex -> {
                ex.getResponseHeaders().add("Content-Type", "application/zip");
                ex.sendResponseHeaders(200, data.length);
                try (OutputStream o = ex.getResponseBody()) {
                    o.write(data);
                }
            });
            server.setExecutor(executor);
            server.start();
            url = "http://" + host + ":" + port + "/pack.zip";
            plugin.getLogger().info("Resource pack hosted at " + url + " (open TCP port " + port + ").");
        } catch (IOException | NoSuchAlgorithmException ex) {
            plugin.getLogger().warning("Could not start pack server: " + ex.getMessage());
            url = null;
        }
    }

    void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    void send(Player p) {
        if (url == null) {
            return;
        }
        p.setResourcePack(url, sha1, Component.text("Throwing Disk textures"),
                plugin.getConfig().getBoolean("resourcepack.required", false));
    }
}
