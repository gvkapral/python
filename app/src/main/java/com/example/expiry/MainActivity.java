package com.example.expiry;

import android.graphics.Typeface;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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

    private EditText etDate;
    private EditText etAmount;
    private TextView tvResult;
    private MaterialButton btnSave;
    private TextView tvEmpty;

    private LocalDate startDate = LocalDate.now();
    private String unit = Product.UNIT_DAY;
    private final Button[] unitButtons = new Button[4];
    private final String[] unitValues = {
            Product.UNIT_DAY, Product.UNIT_WEEK, Product.UNIT_MONTH, Product.UNIT_YEAR };

    private final DateTimeFormatter fmtInput = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private final DateTimeFormatter fmtLong =
            DateTimeFormatter.ofPattern("d MMMM yyyy 'г'.", new Locale("ru"));
    private final DateTimeFormatter fmtShort =
            DateTimeFormatter.ofPattern("d MMM yyyy 'г'.", new Locale("ru"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        products.clear();
        products.addAll(Storage.load(this));
        // пересчёт по новой формуле (день начала = первый день хранения)
        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            if (p.hasDuration()) {
                products.set(i, new Product(p.name, p.startEpochDay, p.amount, p.unit,
                        calcExpiry(LocalDate.ofEpochDay(p.startEpochDay), p.amount, p.unit).toEpochDay()));
            }
        }

        etDate = findViewById(R.id.etDate);
        etAmount = findViewById(R.id.etAmount);
        tvResult = findViewById(R.id.tvResult);
        btnSave = findViewById(R.id.btnSave);
        tvEmpty = findViewById(R.id.tvEmpty);

        // дата: поле открывает системный календарь
        etDate.setText(startDate.format(fmtInput));
        etDate.setOnClickListener(v -> showDatePicker());

        // срок хранения: живой пересчёт
        etAmount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { recalc(); }
        });

        // единицы: сетка 2×2, выбор вручную
        unitButtons[0] = findViewById(R.id.btnUnitDay);
        unitButtons[1] = findViewById(R.id.btnUnitWeek);
        unitButtons[2] = findViewById(R.id.btnUnitMonth);
        unitButtons[3] = findViewById(R.id.btnUnitYear);
        for (int i = 0; i < unitButtons.length; i++) {
            final String value = unitValues[i];
            unitButtons[i].setOnClickListener(v -> setUnit(value));
        }
        setUnit(Product.UNIT_DAY);

        // пресеты длительности — подставляют значение без клавиатуры
        int[] presetIds = { R.id.preset3, R.id.preset7, R.id.preset14, R.id.preset30, R.id.preset90 };
        for (int id : presetIds) {
            Button b = findViewById(id);
            b.setOnClickListener(v -> {
                etAmount.setText(b.getText().toString());
                etAmount.setSelection(etAmount.getText().length());
            });
        }

        btnSave.setOnClickListener(v -> saveCalculation());

        RecyclerView rv = findViewById(R.id.recycler);
        rv.setLayoutManager(new LinearLayoutManager(this) {
            @Override
            public boolean canScrollVertically() {
                return false; // скроллит внешний NestedScrollView
            }
        });
        adapter = new ProductAdapter();
        rv.setAdapter(adapter);

        recalc();
        updateAdapter();
    }

    private void showDatePicker() {
        new DatePickerDialog(this,
                (view, year, month, day) -> {
                    startDate = LocalDate.of(year, month + 1, day);
                    etDate.setText(startDate.format(fmtInput));
                    recalc();
                },
                startDate.getYear(),
                startDate.getMonthValue() - 1,
                startDate.getDayOfMonth()).show();
    }

    private int getAmount() {
        try {
            return Integer.parseInt(etAmount.getText().toString().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void setUnit(String value) {
        unit = value;
        for (int i = 0; i < unitButtons.length; i++) {
            boolean selected = unitValues[i].equals(value);
            unitButtons[i].setBackgroundResource(selected
                    ? R.drawable.bg_unit_selected : R.drawable.bg_unit_unselected);
            unitButtons[i].setTextColor(getColor(selected
                    ? R.color.white : R.color.text_secondary));
        }
        recalc();
    }

    private LocalDate calcExpiry(LocalDate start, int amount, String unit) {
        // день начала считается первым днём хранения → окончание = начало + срок − 1 день
        LocalDate shifted;
        switch (unit) {
            case Product.UNIT_WEEK:  shifted = start.plusWeeks(amount); break;
            case Product.UNIT_MONTH: shifted = start.plusMonths(amount); break;
            case Product.UNIT_YEAR:  shifted = start.plusYears(amount); break;
            case Product.UNIT_DAY:
            default:                 shifted = start.plusDays(amount); break;
        }
        return shifted.minusDays(1);
    }

    private void recalc() {
        int amount = getAmount();
        if (amount > 0) {
            LocalDate expiry = calcExpiry(startDate, amount, unit);
            tvResult.setText(expiry.format(fmtLong));
            tvResult.setTextColor(getColor(R.color.primary));
            tvResult.setTextSize(27);
            tvResult.setTypeface(getResources().getFont(R.font.playfair_700), Typeface.BOLD);
            btnSave.setEnabled(true);
        } else {
            tvResult.setText(R.string.result_empty);
            tvResult.setTextColor(getColor(R.color.text_secondary));
            tvResult.setTextSize(15);
            tvResult.setTypeface(getResources().getFont(R.font.inter_400), Typeface.NORMAL);
            btnSave.setEnabled(false);
        }
    }

    private void saveCalculation() {
        int amount = getAmount();
        if (amount <= 0) return;
        LocalDate expiry = calcExpiry(startDate, amount, unit);
        products.add(new Product(null, startDate.toEpochDay(), amount, unit, expiry.toEpochDay()));
        Storage.save(this, products);
        etAmount.setText("");
        updateAdapter();
    }

    private void deleteProduct(int position) {
        if (position >= 0 && position < products.size()) {
            products.remove(position);
            Storage.save(this, products);
            updateAdapter();
        }
    }

    private void updateAdapter() {
        Collections.sort(products, (a, b) -> Long.compare(a.expiryEpochDay, b.expiryEpochDay));
        adapter.notifyDataSetChanged();
        tvEmpty.setVisibility(products.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // ==================== Склонения ====================

    private String durationText(Product p) {
        if (!p.hasDuration()) return p.name != null ? p.name : "";
        int n = p.amount;
        int resId;
        switch (p.unit) {
            case Product.UNIT_WEEK:  resId = plural(n, R.string.unit_week_one, R.string.unit_week_few, R.string.unit_week_many); break;
            case Product.UNIT_MONTH: resId = plural(n, R.string.unit_month_one, R.string.unit_month_few, R.string.unit_month_many); break;
            case Product.UNIT_YEAR:  resId = plural(n, R.string.unit_year_one, R.string.unit_year_few, R.string.unit_year_many); break;
            case Product.UNIT_DAY:
            default:                 resId = plural(n, R.string.unit_day_one, R.string.unit_day_few, R.string.unit_day_many); break;
        }
        return getString(R.string.duration_format, n, getString(resId));
    }

    private int plural(int n, int one, int few, int many) {
        int m10 = n % 10, m100 = n % 100;
        if (m10 == 1 && m100 != 11) return one;
        if (m10 >= 2 && m10 <= 4 && (m100 < 10 || m100 >= 20)) return few;
        return many;
    }

    // ==================== Адаптер ====================

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

            h.tvDuration.setText(durationText(p));

            LocalDate expiry = LocalDate.ofEpochDay(p.expiryEpochDay);
            h.tvExpiry.setText(expiry.format(fmtLong));

            LocalDate start = LocalDate.ofEpochDay(p.startEpochDay);
            h.tvStart.setText(getString(R.string.card_start_format, start.format(fmtShort)));

            h.btnDelete.setOnClickListener(v -> {
                int pos = h.getBindingAdapterPosition();
                deleteProduct(pos);
            });
        }

        @Override
        public int getItemCount() {
            return products.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tvDuration, tvExpiry, tvStart;
            ImageButton btnDelete;

            VH(@NonNull View itemView) {
                super(itemView);
                tvDuration = itemView.findViewById(R.id.tvItemDuration);
                tvExpiry = itemView.findViewById(R.id.tvItemExpiry);
                tvStart = itemView.findViewById(R.id.tvItemStart);
                btnDelete = itemView.findViewById(R.id.btnDelete);
            }
        }
    }
}
