package com.nmmart.retailos.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.nmmart.retailos.R;
import com.nmmart.retailos.models.Offer;
import java.util.List;

public class OfferAdapter extends RecyclerView.Adapter<OfferAdapter.OfferViewHolder> {
    private List<Offer> offers;

    public OfferAdapter(List<Offer> offers) {
        this.offers = offers;
    }

    public void setOffers(List<Offer> offers) {
        this.offers = offers;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OfferViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_offer, parent, false);
        return new OfferViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OfferViewHolder holder, int position) {
        Offer offer = offers.get(position);
        holder.tvOfferTitle.setText(offer.getTitle());
        holder.tvOfferDescription.setText(offer.getDescription());
        holder.tvOfferDiscount.setText(offer.getDiscount());
    }

    @Override
    public int getItemCount() {
        return offers.size();
    }

    static class OfferViewHolder extends RecyclerView.ViewHolder {
        TextView tvOfferTitle;
        TextView tvOfferDescription;
        TextView tvOfferDiscount;

        public OfferViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOfferTitle = itemView.findViewById(R.id.tvOfferTitle);
            tvOfferDescription = itemView.findViewById(R.id.tvOfferDescription);
            tvOfferDiscount = itemView.findViewById(R.id.tvOfferDiscount);
        }
    }
}
