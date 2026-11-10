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

    /** Called immediately when a user changes a cosmetic in the menu. */
    public static void selectionChanged() {
        pendingUpload = true;
        retryAfter = 0;
        syncStatus = "QUEUED";
        if (currentUuid != null) {
            SETTINGS.setProperty("head." + currentUuid, localHead);
            SETTINGS.setProperty("back." + currentUuid, localBack);
            saveSettings();
        }
    }

    private static void loadSettings() {
        try {
            if (Files.exists(SETTINGS_FILE)) {
                try (InputStream in = Files.newInputStream(SETTINGS_FILE)) {
                    SETTINGS.load(in);
                }
            }
        } catch (Exception ex) {
            System.err.println("[VisionMod] Failed reading cosmetics sync config: " + ex);
        }
    }

    private static synchronized void saveSettings() {
        try {
            Files.createDirectories(SETTINGS_FILE.getParent());
            try (OutputStream out = Files.newOutputStream(SETTINGS_FILE)) {
                SETTINGS.store(out, "Vision Mod cosmetic settings and private access keys. Do not share this file.");
            }
        } catch (Exception ex) {
            System.err.println("[VisionMod] Failed writing cosmetics sync config: " + ex);
        }
    }

    private static String privateKey(UUID uuid) {
        String settingName = "secret." + uuid;
        String token = SETTINGS.getProperty(settingName);
        if (token != null && token.matches("[a-f0-9]{64}")) return token;
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        StringBuilder result = new StringBuilder(64);
        for (byte b : bytes) result.append(String.format(Locale.ROOT, "%02x", b & 255));
        token = result.toString();
        SETTINGS.setProperty(settingName, token);
        saveSettings();
        return token;
    }

    private static JsonObject toJson(Model model) {
        if (model == null) return null;
        JsonObject result = new JsonObject();
        JsonArray boxes = new JsonArray();
        for (Box box : model.boxes()) {
            JsonObject value = new JsonObject();
            JsonArray from = new JsonArray();
            from.add(box.x1()); from.add(box.y1()); from.add(box.z1());
            JsonArray to = new JsonArray();
            to.add(box.x2()); to.add(box.y2()); to.add(box.z2());
            value.add("from", from);
            value.add("to", to);
            value.addProperty("color", String.format(Locale.ROOT, "#%06X", box.color() & 0xFFFFFF));
            boxes.add(value);
        }
        result.add("boxes", boxes);
        return result;
    }

    private static Model fromJson(JsonElement input) {
        try {
            if (input == null || input.isJsonNull() || !input.isJsonObject()) return null;
            JsonArray values = input.getAsJsonObject().getAsJsonArray("boxes");
            if (values == null || values.size() == 0 || values.size() > 64) return null;
            List<Box> boxes = new ArrayList<>();
            for (JsonElement element : values) {
                JsonObject object = element.getAsJsonObject();
                float[] from = coords(object.getAsJsonArray("from"));
                float[] to = coords(object.getAsJsonArray("to"));
                if (from == null || to == null) return null;
                String hex = object.get("color").getAsString();
                if (!hex.matches("#[0-9A-Fa-f]{6}")) return null;
                int color = 0xFF000000 | Integer.parseInt(hex.substring(1), 16);
                boxes.add(new Box(Math.min(from[0], to[0]), Math.min(from[1], to[1]), Math.min(from[2], to[2]),
                    Math.max(from[0], to[0]), Math.max(from[1], to[1]), Math.max(from[2], to[2]), color));
            }
            return new Model(List.copyOf(boxes));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void queueUpload(UUID uuid) {
        uploading = true;
        pendingUpload = false;
        syncStatus = "UPLOADING";
        String head = localHead;
        String back = localBack;
        String secret = privateKey(uuid);
        Model headModel = LOCAL_MODELS.get("head/" + head);
        Model backModel = LOCAL_MODELS.get("back/" + back);
        JsonObject payload = new JsonObject();
        payload.addProperty("head", head);
        payload.addProperty("back", back);
        payload.add("headModel", headModel == null ? JsonNull.INSTANCE : toJson(headModel));
        payload.add("backModel", backModel == null ? JsonNull.INSTANCE : toJson(backModel));
        String body = payload.toString();

        UPLOAD_NETWORK.execute(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(SERVER + "/cosmetics/" + uuid))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + secret)
                    .PUT(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
                HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    syncStatus = "SYNCED";
                } else if (response.statusCode() == 403 || response.statusCode() == 409) {
                    syncStatus = "UUID CLAIMED (403)";
                    // This account's UUID was already claimed with another key.
                    // Do not repeatedly retry locked accounts.
                    retryAfter = Long.MAX_VALUE;
                } else {
                    syncStatus = "HTTP " + response.statusCode();
                    pendingUpload = true;
                    retryAfter = System.currentTimeMillis() + 15_000L;
                    System.err.println("[VisionMod] Cosmetics upload HTTP " + response.statusCode() + ": " + response.body());
                }
            } catch (Exception ex) {
                syncStatus = "OFFLINE";
                pendingUpload = true;
                retryAfter = System.currentTimeMillis() + 15_000L;
                System.err.println("[VisionMod] Cosmetics upload failed: " + ex);
            } finally {
                uploading = false;
            }
        });
    }

    private static void fetch(UUID uuid) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(SERVER + "/cosmetics/" + uuid))
                .timeout(Duration.ofSeconds(10)).GET().build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 || response.body().length() > 16_384) return;
            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            if (!root.has("cosmetics") || !root.get("cosmetics").isJsonObject()) return;
            JsonObject cosmetics = root.getAsJsonObject("cosmetics");
            CACHE.put(uuid, new Entry(
                name(cosmetics, "head"), name(cosmetics, "back"),
                fromJson(cosmetics.get("headModel")), fromJson(cosmetics.get("backModel")),
                System.currentTimeMillis()));
        } catch (Exception ex) {
            System.err.println("[VisionMod] Failed fetching cosmetics for " + uuid + ": " + ex);
        }
    }
}
