package com.entertainment.jacklee.graficaexample.Widget;

import com.entertainment.jacklee.graficaexample.CameraFilter.filter.FilterManager;
import com.entertainment.jacklee.graficaexample.CameraFilter.filter.IFilter;

/**
 * Created by Jacklee on 16. 3. 29..
 */
public class LVItemFilter {

    private FilterManager.FilterType filterType;
    private String strFilterName;

    public LVItemFilter() {

    }

    public LVItemFilter(FilterManager.FilterType filterType, String strFilterName) {
        this.filterType = filterType;
        this.strFilterName = strFilterName;

    }

    public FilterManager.FilterType  getFilterType() {
        return filterType;
    }

    public void setFilterType(FilterManager.FilterType filterType) {
        this.filterType = filterType;
    }


    public String getFilterName() {
        return strFilterName;
    }

    public void setFilterName(String strFilterName) {
        this.strFilterName = strFilterName;
    }

}
