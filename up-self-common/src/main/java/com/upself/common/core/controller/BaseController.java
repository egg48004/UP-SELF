package com.upself.common.core.controller;

import com.github.pagehelper.PageInfo;
import com.upself.common.core.domain.AjaxResult;
import com.upself.common.core.page.TableDataInfo;
import com.upself.common.utils.PageUtils;

import java.util.List;

/** Shared web handling, following the reference BaseController contract. */
public class BaseController {
    protected void startPage() {
        PageUtils.startPage();
    }

    protected void clearPage() {
        PageUtils.clearPage();
    }

    protected TableDataInfo getDataTable(List<?> list) {
        TableDataInfo r = new TableDataInfo();
        r.setCode(200);
        r.setMsg("Success");
        r.setRows(list);
        r.setTotal(new PageInfo<>(list).getTotal());
        return r;
    }

    public AjaxResult success() {
        return AjaxResult.success();
    }

    protected AjaxResult toAjax(int rows) {
        return rows > 0 ? AjaxResult.success() : AjaxResult.error();
    }

    protected AjaxResult toAjax(boolean result) {
        return result ? AjaxResult.success() : AjaxResult.error();
    }
}
