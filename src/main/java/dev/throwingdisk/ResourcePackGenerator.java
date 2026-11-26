package dev.throwingdisk;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;

/** Generates the resource pack (custom item model + disk texture) at runtime. */
public final class ResourcePackGenerator {
    private ResourcePackGenerator() {}

    public static Path generate(Path dataFolder) throws IOException {
        Files.createDirectories(dataFolder);
        Path out = dataFolder.resolve("ThrowingDiskPack.zip");
        Files.write(out, build());
        return out;
    }

    public static byte[] build() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bos)) {
            put(zip, "pack.mcmeta", ("{\"pack\":{\"description\":\"Throwing Disk\",\"pack_format\":75,"
                    + "\"supported_formats\":{\"min_inclusive\":46,\"max_inclusive\":99}}}")
                    .getBytes(StandardCharsets.UTF_8));
            put(zip, "assets/throwingdisk/items/disk.json",
                    "{\"model\":{\"type\":\"minecraft:model\",\"model\":\"throwingdisk:item/disk\"}}"
                            .getBytes(StandardCharsets.UTF_8));
            put(zip, "assets/throwingdisk/models/item/disk.json",
                    ("{\"parent\":\"minecraft:item/generated\",\"textures\":"
                            + "{\"layer0\":\"throwingdisk:item/disk\"}}").getBytes(StandardCharsets.UTF_8));
            put(zip, "assets/throwingdisk/textures/item/disk.png", texture());
        }
        return bos.toByteArray();
    }

    private static void put(ZipOutputStream zip, String name, byte[] data) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(data);
        zip.closeEntry();
    }

    private static byte[] texture() throws IOException {
        int n = 32;
        BufferedImage img = new BufferedImage(n, n, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(40, 50, 70));
        g.fillOval(2, 2, 28, 28);
        g.setColor(new Color(70, 200, 230));
        g.fillOval(4, 4, 24, 24);
        g.setColor(new Color(30, 90, 140));
        g.fillOval(8, 8, 16, 16);
        g.setColor(new Color(210, 245, 255));
        g.fillOval(11, 11, 10, 10);
        g.setColor(new Color(40, 50, 70));
        g.fillOval(14, 14, 4, 4);
        g.setColor(new Color(255, 255, 255, 200));
        g.fillRect(8, 6, 6, 2);
        g.dispose();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", bos);
        return bos.toByteArray();
    }
}
