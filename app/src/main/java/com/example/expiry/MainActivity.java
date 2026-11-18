package com.example.expiry;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private final List<Product> products = new ArrayList<>();
    private ProductAdapter adapter;
    private EditText etName;
    private TextView tvPurchasedDate;
    private LocalDate purchasedDate = LocalDate.now();
    private int shelfLifeDays = 0;

    private final DateTimeFormatter dateFmt =
            DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        products.clear();
        products.addAll(Storage.load(this));

        etName = findViewById(R.id.etName);
        tvPurchasedDate = findViewById(R.id.tvPurchasedDate);
        TextView tvShelfLife = findViewById(R.id.tvShelfLife);
        tvPurchasedDate.setText(purchasedDate.format(dateFmt));

        findViewById(R.id.btnPickDate).setOnClickListener(v -> showDatePicker());

        findViewById(R.id.btnMinus).setOnClickListener(v -> changeShelfLife(-1));
        findViewById(R.id.btnPlus).setOnClickListener(v -> changeShelfLife(1));
        tvShelfLife.setOnClickListener(v -> showShelfLifeDialog());
        findViewById(R.id.btnEditShelfLife).setOnClickListener(v -> showShelfLifeDialog());

        MaterialButton btnAdd = findViewById(R.id.btnAdd);
        btnAdd.setOnClickListener(v -> addProduct());

        RecyclerView rv = findViewById(R.id.recycler);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProductAdapter();
        rv.setAdapter(adapter);

        // свайп влево/вправо — удалить продукт
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv,
                                  @NonNull RecyclerView.ViewHolder vh,
                                  @NonNull RecyclerView.ViewHolder t) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int direction) {
                int pos = vh.getBindingAdapterPosition();
                if (pos >= 0 && pos < products.size()) {
                    products.remove(pos);
                    Storage.save(MainActivity.this, products);
                    adapter.notifyItemRemoved(pos);
                }
            }
        }).attachToRecyclerView(rv);

        updateAdapter();
    }

    private void changeShelfLife(int delta) {
        shelfLifeDays = Math.max(0, shelfLifeDays + delta);
        ((TextView) findViewById(R.id.tvShelfLife)).setText(
                getString(R.string.shelf_life_format, shelfLifeDays));
    }

    private void showShelfLifeDialog() {
        EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(shelfLifeDays));
        input.setSelection(input.getText().length());

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.shelf_life_dialog_title)
                .setView(input)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    try {
                        shelfLifeDays = Integer.parseInt(input.getText().toString().trim());
                        if (shelfLifeDays < 0) shelfLifeDays = 0;
                    } catch (NumberFormatException e) {
                        shelfLifeDays = 0;
                    }
                    ((TextView) findViewById(R.id.tvShelfLife)).setText(
                            getString(R.string.shelf_life_format, shelfLifeDays));
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showDatePicker() {
        DatePickerDialog dpd = new DatePickerDialog(this,
                (view, year, month, day) -> {
                    purchasedDate = LocalDate.of(year, month + 1, day);
                    tvPurchasedDate.setText(purchasedDate.format(dateFmt));
                },
                purchasedDate.getYear(),
                purchasedDate.getMonthValue() - 1,
                purchasedDate.getDayOfMonth());
        dpd.show();
    }

    private void addProduct() {
        String name = etName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, R.string.enter_name, Toast.LENGTH_SHORT).show();
            return;
        }
        if (shelfLifeDays <= 0) {
            Toast.makeText(this, R.string.enter_shelf_life, Toast.LENGTH_SHORT).show();
            return;
        }
        long expiry = purchasedDate.plusDays(shelfLifeDays).toEpochDay();
        products.add(new Product(name, expiry));
        Storage.save(this, products);
        etName.setText("");
        updateAdapter();
    }

    private void updateAdapter() {
        Collections.sort(products, (a, b) -> Long.compare(a.expiryEpochDay, b.expiryEpochDay));
        adapter.notifyDataSetChanged();
        findViewById(R.id.tvEmpty).setVisibility(products.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.VH> {

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_product, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Product p = products.get(position);
            LocalDate expiry = LocalDate.ofEpochDay(p.expiryEpochDay);
            long daysLeft = expiry.toEpochDay() - LocalDate.now().toEpochDay();

            h.tvName.setText(p.name);
            h.tvExpiry.setText(getString(R.string.expiry_format, expiry.format(dateFmt)));

            if (daysLeft < 0) {
                h.tvDays.setText(getString(R.string.days_expired, -daysLeft));
                h.tvDays.setTextColor(getColor(R.color.red));
            } else if (daysLeft == 0) {
                h.tvDays.setText(R.string.days_today);
                h.tvDays.setTextColor(getColor(R.color.orange));
            } else if (daysLeft <= 3) {
                h.tvDays.setText(getString(R.string.days_left, daysLeft));
                h.tvDays.setTextColor(getColor(R.color.orange));
            } else {
                h.tvDays.setText(getString(R.string.days_left, daysLeft));
                h.tvDays.setTextColor(getColor(R.color.green));
            }
        }

        @Override
        public int getItemCount() {
            return products.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvExpiry, tvDays;

            VH(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvItemName);
                tvExpiry = itemView.findViewById(R.id.tvItemExpiry);
                tvDays = itemView.findViewById(R.id.tvItemDays);
            }
        }
    }
}
