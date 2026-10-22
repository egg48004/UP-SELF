package com.upself.common.utils;

import com.github.pagehelper.PageHelper;
import com.upself.common.core.page.*;

public final class PageUtils {
    private PageUtils() {}

    public static void startPage() {
        PageDomain p = TableSupport.buildPageRequest();
        PageHelper.startPage(p.getPageNum(), p.getPageSize());
    }

    public static void clearPage() {
        PageHelper.clearPage();
    }
}
