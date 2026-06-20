package com.nmmart.retailos.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.nmmart.retailos.R;
import com.nmmart.retailos.models.Banner;
import java.util.List;

public class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.BannerViewHolder> {
    private Context context;
    private List<Banner> banners;

    public BannerAdapter(Context context, List<Banner> banners) {
        this.context = context;
        this.banners = banners;
    }

    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_banner, parent, false);
        return new BannerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        Banner banner = banners.get(position);
        if (banner.imageUrl != null && !banner.imageUrl.isEmpty()) {
            Glide.with(context)
                .load(banner.imageUrl)
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                .placeholder(R.drawable.ic_grocery_bag)
                .error(R.drawable.ic_grocery_bag)
                .dontAnimate()
                .into(holder.ivBannerImage);
        }

        holder.itemView.setOnClickListener(v -> {
            if (banner.redirectPath != null && !banner.redirectPath.isEmpty()) {
                android.content.Intent intent = new android.content.Intent(context, com.nmmart.retailos.ui.activities.ProductListActivity.class);
                intent.putExtra("CATEGORY_NAME", banner.redirectPath);
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return banners.size();
    }

    public void setBanners(List<Banner> banners) {
        this.banners = banners;
        notifyDataSetChanged();
    }

    public static class BannerViewHolder extends RecyclerView.ViewHolder {
        ImageView ivBannerImage;

        public BannerViewHolder(@NonNull View itemView) {
            super(itemView);
            ivBannerImage = itemView.findViewById(R.id.ivBannerImage);
        }
    }
}
