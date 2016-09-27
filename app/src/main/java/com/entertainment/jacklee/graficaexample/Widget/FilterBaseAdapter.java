package com.entertainment.jacklee.graficaexample.Widget;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.entertainment.jacklee.graficaexample.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jacklee on 16. 3. 29..
 */
public class FilterBaseAdapter extends BaseAdapter {
    private String TAG = "FilterBaseAdapter";
    private Context mContext;
    private LayoutInflater mLayoutInflater = null;

    ArrayList<LVItemFilter> mArl;



    public FilterBaseAdapter(ArrayList<LVItemFilter> arl, Context context) {
        mContext = context;
        this.mArl = arl;
        mLayoutInflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }




    @Override
    public int getCount() {
        if (mArl == null) {
            Log.d(TAG, "getCount() return 0");
            return 0;
        } else {
             return mArl.size();
        }    }

    @Override
    public LVItemFilter getItem(int position) {
        if (mArl == null) {
            return null;
        } else {
            LVItemFilter itemFilter = new LVItemFilter();
            try {
                itemFilter = mArl.get(position);
            } catch (Exception e) {

            }
            return itemFilter;
        }    }



    public class  ViewHolder   {
        public LinearLayout lo;
        public ImageView iv;
        public TextView tv;


    }


    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        ViewHolder viewHolder;


        if (view == null) {
            Log.d(TAG, "getView(), view==null");
            LayoutInflater layoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = layoutInflater.inflate(R.layout.i_filter, null);
            viewHolder = new ViewHolder();

            viewHolder  .iv = (ImageView) view.findViewById(R.id.iv);
            viewHolder  . tv = (TextView) view.findViewById(R.id.tv);
            viewHolder  . lo = (LinearLayout) view.findViewById(R.id.lo);




            view.setTag(viewHolder);

        } else {
            Log.d(TAG, "getView(), view!=null");
            viewHolder = (ViewHolder) view.getTag();
        }

        viewHolder.tv.setText((mArl.get(position)).getFilterName());

        return view;

    }
}
