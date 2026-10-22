package com.upself.common.core.page;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

public final class TableSupport {
    public static final String PAGE_NUM = "current", PAGE_SIZE = "pageSize";

    private TableSupport() {}

    public static PageDomain buildPageRequest() {
        HttpServletRequest r =
                ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                        .getRequest();
        PageDomain p = new PageDomain();
        // Temporary pageNum alias keeps the existing verification page/scripts compatible.
        String current = r.getParameter(PAGE_NUM);
        if (current == null) current = r.getParameter("pageNum");
        p.setPageNum(number(current, 1, 1, Integer.MAX_VALUE));
        p.setPageSize(number(r.getParameter(PAGE_SIZE), 10, 1, 100));
        return p;
    }

    private static int number(String s, int fallback, int min, int max) {
        try {
            return Math.max(min, Math.min(max, Integer.parseInt(s)));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
