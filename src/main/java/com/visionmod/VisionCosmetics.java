
package com.visionmod;

import com.google.gson.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.io.Reader;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

public final class VisionCosmetics {

    private static final String SERVER =
        "https://rough-truth-7b7dvision-cosmetics.vision-mod.workers.dev";

    private static final Identifier HEAD_TEXTURE =
        Identifier.fromNamespaceAndPath(
            "visionmod", "textures/cosmetics/head.png"
        );

    private static final Identifier BACK_TEXTURE =
        Identifier.fromNamespaceAndPath(
            "visionmod", "textures/cosmetics/back.png"
        );

    private static final HttpClient HTTP =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .build();

    private static final ExecutorService NETWORK =
        Executors.newSingleThreadExecutor(task -> {
            Thread t = new Thread(
                task, "VisionCosmetics-network"
            );
            t.setDaemon(true);
            return t;
        });

    private static final ExecutorService UPLOAD_NETWORK =
        Executors.newSingleThreadExecutor(task -> {
            Thread t = new Thread(
                task, "VisionCosmetics-upload"
            );
            t.setDaemon(true);
            return t;
        });

    private static final Map<UUID, Entry> CACHE =
        new ConcurrentHashMap<>();

    private static final Set<UUID> FETCHING =
        ConcurrentHashMap.newKeySet();

    private static final Map<UUID, Long> LAST_FETCH =
        new ConcurrentHashMap<>();

    private static final Map<String, Model> LOCAL_MODELS =
        new ConcurrentHashMap<>();

    private static final Path SETTINGS_FILE =
        FabricLoader.getInstance()
            .getConfigDir()
            .resolve("visionmod/cosmetics-sync.properties");

    private static final Properties SETTINGS =
        new Properties();

    private static final SecureRandom RANDOM =
        new SecureRandom();

    private static UUID currentUuid;
    private static volatile boolean pendingUpload;
    private static volatile boolean uploading;
    private static volatile long retryAfter;
    private static int ticks;

    public static String localHead = "none";
    public static String localBack = "none";
    public static volatile String syncStatus = "NOT CONNECTED";

    private record Entry(
        String head,
        String back,
        Model headModel,
        Model backModel,
        long fetchedAt
    ) {}

    private record Box(
        float x1, float y1, float z1,
        float x2, float y2, float z2,
        int color
    ) {}

    private record Model(List<Box> boxes) {}

    private VisionCosmetics() {}

    public static void init() {
        loadSettings();
        loadLocalModels();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            if (client.level == null || client.player == null) {
                CACHE.clear();
                FETCHING.clear();
                LAST_FETCH.clear();
                currentUuid = null;
                ticks = 0;
                syncStatus = "NOT CONNECTED";
                return;
            }

            UUID uuid = client.player.getUUID();

            if (!uuid.equals(currentUuid)) {
                currentUuid = uuid;

                localHead = SETTINGS.getProperty(
                    "head." + uuid, "none"
                );

                localBack = SETTINGS.getProperty(
                    "back." + uuid, "none"
                );

                pendingUpload = true;
                uploading = false;
                retryAfter = 0;
                syncStatus = "CONNECTING";
            }

            if (++ticks % 40 != 0) {
                return;
            }

            if (ticks % 600 == 0) {
                loadLocalModels();
            }

            if (!VisionModClient.enabled ||
                !VisionModClient.cosmetics) {
                return;
            }

            if (pendingUpload &&
                !uploading &&
                System.currentTimeMillis() >= retryAfter) {
                queueUpload(uuid);
            }

            if (!VisionModClient.showOtherCosmetics) {
                return;
            }

            long now = System.currentTimeMillis();

            for (AbstractClientPlayer player :
                    client.level.players()) {

                if (player == client.player ||
                    player.distanceToSqr(client.player) > 48 * 48) {
                    continue;
                }

                UUID otherId = player.getUUID();

                if (FETCHING.contains(otherId)) {
                    continue;
                }

                long last = LAST_FETCH.getOrDefault(
                    otherId, 0L
                );

                if (now - last < 60_000L) {
                    continue;
                }

                LAST_FETCH.put(otherId, now);
                FETCHING.add(otherId);

                NETWORK.execute(() -> {
                    try {
                        fetch(otherId);
                    } finally {
                        FETCHING.remove(otherId);
                    }
                });
            }
        });

        WorldRenderEvents.BEFORE_ENTITIES.register(context -> {

            Minecraft mc = Minecraft.getInstance();
            ClientLevel level = mc.level;

            if (level == null ||
                mc.player == null ||
                !VisionModClient.enabled ||
                !VisionModClient.cosmetics ||
                context.consumers() == null) {
                return;
            }

            PoseStack matrices = context.matrices();

            Vec3 camera = mc.gameRenderer
                .getMainCamera()
                .position();

            for (AbstractClientPlayer player : level.players()) {

                if (player.distanceToSqr(mc.player) > 48 * 48 ||
                    player.isInvisible()) {
                    continue;
                }

                String head;
                String back;

                if (player == mc.player) {
                    head = localHead;
                    back = localBack;
                } else {
                    if (!VisionModClient.showOtherCosmetics) {
                        continue;
                    }

                    Entry entry = CACHE.get(player.getUUID());

                    if (entry == null) {
                        continue;
                    }

                    head = entry.head();
                    back = entry.back();
                }

                double x = player.getX() - camera.x;
                double y = player.getY() - camera.y;
                double z = player.getZ() - camera.z;

                matrices.pushPose();
                matrices.translate(x, y, z);

                matrices.mulPose(
                    new Quaternion
