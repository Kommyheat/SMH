package com.app.smh.scan;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.smh.R;

import java.util.List;

public class DrugResultAdapter extends RecyclerView.Adapter<DrugResultAdapter.DrugViewHolder> {

    private final List<DrugResultItem> items;
    private final DrugInfoApiManager apiManager;

    public DrugResultAdapter(List<DrugResultItem> items) {
        this.items = items;
        this.apiManager = new DrugInfoApiManager();
    }

    @NonNull
    @Override
    public DrugViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_drug_result, parent, false);
        return new DrugViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DrugViewHolder holder, int position) {
        DrugResultItem item = items.get(position);

        holder.tvDrugName.setText(item.getRecognizedName());
        holder.progressBar.setVisibility(item.isLoading() ? View.VISIBLE : View.GONE);
        holder.layoutDetail.setVisibility(item.isExpanded() ? View.VISIBLE : View.GONE);
        holder.btnExpand.setRotation(item.isExpanded() ? 90f : 0f);

        if (item.hasDetail()) {
            holder.tvItemName.setText(safeText(item.getItemName()));
            holder.tvEntpName.setText(safeText(item.getEntpName()));
            holder.tvEfcy.setText(safeText(item.getEfcyQesitm()));
            holder.tvUseMethod.setText(safeText(item.getUseMethodQesitm()));
            holder.tvWarn.setText(safeText(item.getAtpnWarnQesitm()));
            holder.tvStorage.setText(safeText(item.getDepositMethodQesitm()));
        }

        holder.btnExpand.setOnClickListener(v -> {
            int currentPosition = holder.getBindingAdapterPosition();
            if (currentPosition == RecyclerView.NO_POSITION) return;

            DrugResultItem currentItem = items.get(currentPosition);

            if (currentItem.isLoading()) return;

            if (currentItem.hasDetail()) {
                currentItem.setExpanded(!currentItem.isExpanded());
                notifyItemChanged(currentPosition);
                return;
            }

            currentItem.setLoading(true);
            notifyItemChanged(currentPosition);

            apiManager.fetchDrugDetail(currentItem.getRecognizedName(), new DrugInfoApiManager.DetailCallback() {
                @Override
                public void onSuccess(DrugResultItem result) {
                    currentItem.setLoading(false);
                    currentItem.setExpanded(true);
                    currentItem.setItemName(result.getItemName());
                    currentItem.setEntpName(result.getEntpName());
                    currentItem.setEfcyQesitm(result.getEfcyQesitm());
                    currentItem.setUseMethodQesitm(result.getUseMethodQesitm());
                    currentItem.setAtpnWarnQesitm(result.getAtpnWarnQesitm());
                    currentItem.setDepositMethodQesitm(result.getDepositMethodQesitm());

                    // 추가: 캐시에 저장 (약품명 → 상세정보)
                    DrugDetailCache.getInstance().put(currentItem.getRecognizedName(), result);
                    notifyItemChanged(currentPosition);
                }

                @Override
                public void onError(String message) {
                    currentItem.setLoading(false);
                    notifyItemChanged(currentPosition);
                    Toast.makeText(holder.itemView.getContext(), message, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String safeText(String value) {
        return TextUtils.isEmpty(value) ? "-" : value;
    }

    static class DrugViewHolder extends RecyclerView.ViewHolder {

        TextView tvDrugName;
        ImageButton btnExpand;
        ProgressBar progressBar;

        View layoutDetail;
        TextView tvItemName;
        TextView tvEntpName;
        TextView tvEfcy;
        TextView tvUseMethod;
        TextView tvWarn;
        TextView tvStorage;

        public DrugViewHolder(@NonNull View itemView) {
            super(itemView);

            tvDrugName = itemView.findViewById(R.id.tv_drug_name);
            btnExpand = itemView.findViewById(R.id.btn_expand);
            progressBar = itemView.findViewById(R.id.progress_bar);

            layoutDetail = itemView.findViewById(R.id.layout_detail);
            tvItemName = itemView.findViewById(R.id.tv_item_name);
            tvEntpName = itemView.findViewById(R.id.tv_entp_name);
            tvEfcy = itemView.findViewById(R.id.tv_efcy);
            tvUseMethod = itemView.findViewById(R.id.tv_use_method);
            tvWarn = itemView.findViewById(R.id.tv_warn);
            tvStorage = itemView.findViewById(R.id.tv_storage);
        }
    }
}
