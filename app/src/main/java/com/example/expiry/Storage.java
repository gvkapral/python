package com.example.expiry;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Storage {
    private static final String FILE_NAME = "products.json";

    private static File getFile(Context ctx) {
        return new File(ctx.getFilesDir(), FILE_NAME);
    }

    public static List<Product> load(Context ctx) {
        List<Product> list = new ArrayList<>();
        File f = getFile(ctx);
        if (!f.exists()) return list;
        try (FileInputStream in = new FileInputStream(f)) {
            byte[] buf = new byte[(int) f.length()];
            int read = in.read(buf);
            String json = new String(buf, 0, Math.max(read, 0), StandardCharsets.UTF_8);
            if (json.trim().isEmpty()) return list;
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                if (o.has("expiry") && o.has("start")) {
                    // новый формат: start + amount + unit + expiry
                    String unit = o.optString("unit", Product.UNIT_DAY);
                    list.add(new Product(null,
                            o.getLong("start"),
                            o.optInt("amount", 0),
                            unit,
                            o.getLong("expiry")));
                } else if (o.has("name") && o.has("expiry")) {
                    // legacy: name + expiry (дни от неизвестной даты)
                    list.add(new Product(o.getString("name"),
                            o.getLong("expiry"), 0, Product.UNIT_DAY,
                            o.getLong("expiry")));
                }
            }
        } catch (Exception e) {
            // повреждённый файл — начинаем с пустого списка
        }
        return list;
    }

    public static void save(Context ctx, List<Product> list) {
        JSONArray arr = new JSONArray();
        try {
            for (Product p : list) {
                JSONObject o = new JSONObject();
                o.put("start", p.startEpochDay);
                o.put("amount", p.amount);
                o.put("unit", p.unit);
                o.put("expiry", p.expiryEpochDay);
                arr.put(o);
            }
        } catch (JSONException e) {
            return;
        }
        try (FileOutputStream out = new FileOutputStream(getFile(ctx))) {
            out.write(arr.toString(2).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            // игнорируем ошибки записи
        }
    }
}
