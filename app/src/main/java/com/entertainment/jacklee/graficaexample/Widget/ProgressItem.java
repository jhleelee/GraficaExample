package com.entertainment.jacklee.graficaexample.Widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.entertainment.jacklee.graficaexample.R;

/**
 * Created by Jacklee on 16. 4. 2..
 */
public class ProgressItem extends LinearLayout{

     public LinearLayout loBody;
    public  View vEdge;

    public ProgressItem(Context context) {
        super(context);

        loBody = (LinearLayout)this.findViewById(R.id.loBody);

       this. vEdge = new View(context);
    }

    public ProgressItem(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ProgressItem(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }



}
