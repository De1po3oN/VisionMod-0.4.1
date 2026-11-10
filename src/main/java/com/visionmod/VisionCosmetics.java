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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/**
 * Built-in and locally imported solid cuboid cosmetics.
 * This does NOT upload cosmetic selections or texture files.
 */
public final class VisionCosmetics {
    private static final String SERVER =
        "https://rough-truth-7b7dvision-cosmetics.vision-mod.workers.dev";
    // Minecraft player model points toward local -Z. Back is local +Z.
    // Keep accessories in model coordinates, independent of camera / FreeLook.
    private static final Identifier HEAD_TEXTURE = Identifier.fromNamespaceAndPath("visionmod", "textures/cosmetics/head.png");
    private static final Identifier BACK_TEXTURE = Identifier.fromNamespaceAndPath("visionmod", "textures/cosmetics/back.png");
    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(4)).build();
    private static final ExecutorService NETWORK = Executors.newSingleThreadExecutor(task -> {
        Thread t = new Thread(task, "VisionCosmetics-network");
        t.setDaemon(true);
        return t;
    });
    private static final Map<UUID, Entry> CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Model> LOCAL_MODELS = new ConcurrentHashMap<>();
    private static int ticks;
    public static String localHead = "none";
    public static String localBack = "none";

    private record Entry(String head, String back, long fetchedAt) {}
    private record Box(float x1, float y1, float z1, float x2, float y2, float z2, int color) {}
    private record Model(List<Box> boxes) {}

    private VisionCosmetics() {}

    public static void init() {
        loadLocalModels();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level == null || client.player == null) {
                CACHE.clear(); ticks = 0; return;
            }
            if (++ticks % 600 != 0) return;
            // Local files can be changed without restarting Minecraft.
            loadLocalModels();
            if (!VisionModClient.enabled || !VisionModClient.cosmetics ||
                !VisionModClient.showOtherCosmetics) return;
            long now = System.currentTimeMillis();
            for (AbstractClientPlayer player : client.level.players()) {
                if (player == client.player || player.distanceToSqr(client.player) > 48 * 48) continue;
                UUID uuid = player.getUUID();
                Entry previous = CACHE.get(uuid);
                if (previous != null && now - previous.fetchedAt() < 30000L) continue;
                CACHE.put(uuid, new Entry("none", "none", now));
                NETWORK.execute(() -> fetch(uuid));
            }
        });
        WorldRenderEvents.BEFORE_ENTITIES.register(context -> {
            Minecraft mc = Minecraft.getInstance();
            ClientLevel level = mc.level;
            if (level == null || mc.player == null || !VisionModClient.enabled ||
                !VisionModClient.cosmetics || context.consumers() == null) return;
            PoseStack matrices = context.matrices();
            Vec3 camera = mc.gameRenderer.getMainCamera().position();
            for (AbstractClientPlayer player : level.players()) {
                if (player.distanceToSqr(mc.player) > 48 * 48 || player.isInvisible()) continue;
                String head, back;
                if (player == mc.player) {
                    head = localHead; back = localBack;
                } else {
                    if (!VisionModClient.showOtherCosmetics) continue;
                    Entry e = CACHE.get(player.getUUID());
                    if (e == null) continue;
                    head = e.head(); back = e.back();
                }
                double x = player.getX() - camera.x;
                double y = player.getY() - camera.y;
                double z = player.getZ() - camera.z;

                // Body rotation is independent from head look direction.
                // Body-attached wings/backpacks thus do not swing with head yaw.
                matrices.pushPose();
                matrices.translate(x, y, z);
                matrices.mulPose(new Quaternionf().rotationY(
                    (float) Math.toRadians(180.0F - player.yBodyRot)));
                renderModel(matrices,
                    context.consumers().getBuffer(RenderTypes.entityCutout(BACK_TEXTURE)),
                    getModel("back", back));
                matrices.popPose();

                matrices.pushPose();
                matrices.translate(x, y, z);
                matrices.mulPose(new Quaternionf().rotationY(
                    (float) Math.toRadians(180.0F - player.yHeadRot)));
                renderModel(matrices,
                    context.consumers().getBuffer(RenderTypes.entityCutout(HEAD_TEXTURE)),
                    getModel("head", head));
                matrices.popPose();
            }
        });
    }

    private static Model getModel(String slot, String id) {
        if (id == null || id.equals("none")) return null;
        Model custom = LOCAL_MODELS.get(slot + "/" + id);
        if (custom != null) return custom;
        List<Box> boxes = new ArrayList<>();
        if (slot.equals("head") && id.equals("ears")) {
            boxes.add(new Box(-.31f, 1.77f, -.15f, -.19f, 2.05f, .09f, 0xFFFF78CB));
            boxes.add(new Box(.19f, 1.77f, -.15f, .31f, 2.05f, .09f, 0xFFFF78CB));
            boxes.add(new Box(-.29f, 1.81f, -.16f, -.22f, 1.99f, -.145f, 0xFFFFC8E5));
            boxes.add(new Box(.22f, 1.81f, -.16f, .29f, 1.99f, -.145f, 0xFFFFC8E5));
        } else if (slot.equals("head") && id.equals("crown")) {
            boxes.add(new Box(-.32f, 1.84f, -.32f, .32f, 1.98f, .32f, 0xFFFFD166));
            for (int i = -1; i <= 1; i++) {
                boxes.add(new Box(i*.22f-.055f, 1.98f, -.29f, i*.22f+.055f, 2.12f, -.19f, 0xFFFFD166));
            }
        } else if (slot.equals("back") && id.equals("wings")) {
            // Multiple solid segments instead of two wireframe boxes.
            boxes.add(new Box(-.88f, 1.17f, .23f, -.26f, 1.50f, .33f, 0xFF67E8F9));
            boxes.add(new Box(-1.04f, .83f, .25f, -.39f, 1.19f, .34f, 0xFF3AADD9));
            boxes.add(new Box(-.90f, .58f, .26f, -.53f, .88f, .35f, 0xFF1989BC));
            boxes.add(new Box(.26f, 1.17f, .23f, .88f, 1.50f, .33f, 0xFF67E8F9));
            boxes.add(new Box(.39f, .83f, .25f, 1.04f, 1.19f, .34f, 0xFF3AADD9));
            boxes.add(new Box(.53f, .58f, .26f, .90f, .88f, .35f, 0xFF1989BC));
        } else if (slot.equals("back") && id.equals("pack")) {
            boxes.add(new Box(-.29f, .68f, .21f, .29f, 1.46f, .53f, 0xFF56B88E));
            boxes.add(new Box(-.24f, .75f, .54f, .24f, 1.13f, .63f, 0xFF2B7861));
            boxes.add(new Box(-.32f, 1.35f, .18f, -.23f, 1.48f, .51f, 0xFFB6CDB7));
            boxes.add(new Box(.23f, 1.35f, .18f, .32f, 1.48f, .51f, 0xFFB6CDB7));
        }
        return boxes.isEmpty() ? null : new Model(List.copyOf(boxes));
    }

    private static void renderModel(PoseStack pose, VertexConsumer consumer, Model model) {
        if (model == null) return;
        Matrix4f matrix = pose.last().pose();
        for (Box b : model.boxes()) {
            int c = b.color();
            // Six filled faces, with simple directional shading.
            face(matrix, consumer, c, .9f,
                b.x1(),b.y1(),b.z2(), b.x2(),b.y1(),b.z2(),
                b.x2(),b.y2(),b.z2(), b.x1(),b.y2(),b.z2());
            face(matrix, consumer, c, .65f,
                b.x2(),b.y1(),b.z1(), b.x1(),b.y1(),b.z1(),
                b.x1(),b.y2(),b.z1(), b.x2(),b.y2(),b.z1());
            face(matrix, consumer, c, .8f,
                b.x1(),b.y1(),b.z1(), b.x1(),b.y1(),b.z2(),
                b.x1(),b.y2(),b.z2(), b.x1(),b.y2(),b.z1());
            face(matrix, consumer, c, .8f,
                b.x2(),b.y1(),b.z2(), b.x2(),b.y1(),b.z1(),
                b.x2(),b.y2(),b.z1(), b.x2(),b.y2(),b.z2());
            face(matrix, consumer, c, 1f,
                b.x1(),b.y2(),b.z2(), b.x2(),b.y2(),b.z2(),
                b.x2(),b.y2(),b.z1(), b.x1(),b.y2(),b.z1());
            face(matrix, consumer, c, .55f,
                b.x1(),b.y1(),b.z1(), b.x2(),b.y1(),b.z1(),
                b.x2(),b.y1(),b.z2(), b.x1(),b.y1(),b.z2());
        }
    }

    private static void face(Matrix4f matrix, VertexConsumer consumer, int color, float shade,
            float ax,float ay,float az,float bx,float by,float bz,
            float cx,float cy,float cz,float dx,float dy,float dz) {
        int r = Math.round(((color >>> 16) & 255) * shade);
        int g = Math.round(((color >>> 8) & 255) * shade);
        int b = Math.round((color & 255) * shade);
        int packed = 0xFF000000 | (r << 16) | (g << 8) | b;
        // Face normal computed once; diffuse light and texture coordinates
        // are supplied because entityCutout requires the entity vertex format.
        float ux = bx - ax, uy = by - ay, uz = bz - az;
        float vx = dx - ax, vy = dy - ay, vz = dz - az;
        float nx = uy * vz - uz * vy;
        float ny = uz * vx - ux * vz;
        float nz = ux * vy - uy * vx;
        float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (length > 0.00001f) { nx /= length; ny /= length; nz /= length; }
        vertex(consumer, matrix, ax, ay, az, packed, 0f, 0f, nx, ny, nz);
        vertex(consumer, matrix, bx, by, bz, packed, 1f, 0f, nx, ny, nz);
        vertex(consumer, matrix, cx, cy, cz, packed, 1f, 1f, nx, ny, nz);
        vertex(consumer, matrix, dx, dy, dz, packed, 0f, 1f, nx, ny, nz);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix,
            float x, float y, float z, int color, float u, float v,
            float nx, float ny, float nz) {
        consumer.addVertex(matrix, x, y, z)
            .setColor(color).setUv(u, v).setOverlay(0)
            .setLight(0x00F000F0).setNormal(nx, ny, nz);
    }

    // Import minimal, bounded local cuboid models. NOT arbitrary Blockbench files or PNG.
    // Format: {"boxes":[{"from":[x,y,z],"to":[x,y,z],"color":"#RRGGBB"}]}
    public static void loadLocalModels() {
        Path base = FabricLoader.getInstance().getConfigDir().resolve("visionmod/cosmetics");
        Map<String, Model> fresh = new HashMap<>();
        for (String slot : List.of("head", "back")) {
            Path dir = base.resolve(slot);
            if (!Files.isDirectory(dir)) continue;
            try (DirectoryStream<Path> files = Files.newDirectoryStream(dir, "*.json")) {
                for (Path file : files) {
                    if (Files.size(file) > 32768L) continue;
                    String id = file.getFileName().toString().replaceFirst("\\.json$", "");
                    if (!id.matches("[a-z0-9_-]{1,32}")) continue;
                    try (Reader reader = Files.newBufferedReader(file)) {
                        JsonArray values = JsonParser.parseReader(reader)
                            .getAsJsonObject().getAsJsonArray("boxes");
                        if (values == null || values.size() > 64) continue;
                        List<Box> boxes = new ArrayList<>();
                        for (JsonElement element : values) {
                            JsonObject obj = element.getAsJsonObject();
                            float[] from = coords(obj.getAsJsonArray("from"));
                            float[] to = coords(obj.getAsJsonArray("to"));
                            if (from == null || to == null) { boxes.clear(); break; }
                            String hex = obj.get("color").getAsString();
                            if (!hex.matches("#[0-9a-fA-F]{6}")) { boxes.clear(); break; }
                            int color = 0xFF000000 | Integer.parseInt(hex.substring(1), 16);
                            boxes.add(new Box(Math.min(from[0],to[0]),Math.min(from[1],to[1]),Math.min(from[2],to[2]),
                                Math.max(from[0],to[0]),Math.max(from[1],to[1]),Math.max(from[2],to[2]), color));
                        }
                        if (!boxes.isEmpty()) fresh.put(slot + "/" + id, new Model(List.copyOf(boxes)));
                    } catch (RuntimeException ignored) { }
                }
            } catch (Exception ignored) { }
        }
        LOCAL_MODELS.clear();
        LOCAL_MODELS.putAll(fresh);
    }

    private static float[] coords(JsonArray a) {
        if (a == null || a.size() != 3) return null;
        float[] v = new float[3];
        for (int i=0;i<3;i++) {
            try { v[i] = a.get(i).getAsFloat(); }
            catch (Exception e) { return null; }
            if (!Float.isFinite(v[i]) || Math.abs(v[i]) > 3F) return null;
        }
        return v;
    }

    public static List<String> localIds(String slot) {
        TreeSet<String> ids = new TreeSet<>();
        ids.add("none");
        if (slot.equals("head")) { ids.add("ears"); ids.add("crown"); }
        if (slot.equals("back")) { ids.add("wings"); ids.add("pack"); }
        for (String key : LOCAL_MODELS.keySet()) {
            if (key.startsWith(slot + "/")) ids.add(key.substring(slot.length()+1));
        }
        return new ArrayList<>(ids);
    }

    private static String name(JsonObject data, String key) {
        if (!data.has(key) || data.get(key).isJsonNull() || !data.get(key).isJsonPrimitive()) return "none";
        String id = data.get(key).getAsString();
        if (!id.matches("[a-z0-9_-]{1,32}")) return "none";
        return id;
    }

    private static void fetch(UUID uuid) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(SERVER + "/cosmetics/" + uuid))
                .timeout(Duration.ofSeconds(5)).GET().build();
            HttpResponse<String> response = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 || response.body().length() > 4096) return;
            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            if (!root.has("cosmetics") || !root.get("cosmetics").isJsonObject()) return;
            JsonObject settings = root.getAsJsonObject("cosmetics");
            CACHE.put(uuid, new Entry(name(settings,"head"), name(settings,"back"), System.currentTimeMillis()));
        } catch (Exception ignored) {
            // Offline service must not crash Minecraft.
        }
    }
}
