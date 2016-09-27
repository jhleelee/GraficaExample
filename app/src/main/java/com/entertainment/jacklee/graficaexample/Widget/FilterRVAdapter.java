package com.entertainment.jacklee.graficaexample.Widget;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.entertainment.jacklee.graficaexample.R;

import java.util.List;

/**
 * Created by Jacklee on 16. 3. 29..
 */
public class FilterRVAdapter extends RecyclerView.Adapter {


    private List<LVItemFilter> rvItemFilterList;

    public class RVItemFilterViewHolder extends RecyclerView.ViewHolder {
        public LinearLayout lo;
        public ImageView iv;
        public TextView tv;

        public RVItemFilterViewHolder(View view) {
            super(view);
            iv = (ImageView) view.findViewById(R.id.iv);
            tv = (TextView) view.findViewById(R.id.tv);
            lo = (LinearLayout) view.findViewById(R.id.lo);
        }
    }


    public FilterRVAdapter(List<LVItemFilter> rvItemFilterList) {
        this.rvItemFilterList = rvItemFilterList;
    }


    @Override
    public RVItemFilterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.i_filter, parent, false);

        return new RVItemFilterViewHolder(itemView);
    }


    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        RVItemFilterViewHolder myViewHolder1 = (RVItemFilterViewHolder) holder;
        myViewHolder1.lo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });


//        LVItemFilter rvItemFilter = lvItemFilters.get(position);
//        holder.tv.setText(rvItemFilter.getItem_Filter().toString());
//        holder.iv.setImageDrawable();
    }

    @Override
    public int getItemCount() {
        return rvItemFilterList.size();
    }


    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);
    }

    @Override
    public void setHasStableIds(boolean hasStableIds) {
        super.setHasStableIds(hasStableIds);
    }

    @Override
    public long getItemId(int position) {
        return super.getItemId(position);
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
    }

    @Override
    public boolean onFailedToRecycleView(RecyclerView.ViewHolder holder) {
        return super.onFailedToRecycleView(holder);
    }

    @Override
    public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
    }

    @Override
    public void onViewDetachedFromWindow(RecyclerView.ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
    }

    @Override
    public void registerAdapterDataObserver(RecyclerView.AdapterDataObserver observer) {
        super.registerAdapterDataObserver(observer);
    }

    @Override
    public void unregisterAdapterDataObserver(RecyclerView.AdapterDataObserver observer) {
        super.unregisterAdapterDataObserver(observer);
    }


    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
    }


}
