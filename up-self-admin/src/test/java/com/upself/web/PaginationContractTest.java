package com.upself.web;

import static org.junit.jupiter.api.Assertions.*;

import com.github.pagehelper.Page;
import com.upself.common.core.controller.BaseController;
import com.upself.common.core.page.TableDataInfo;
import com.upself.common.core.page.TableSupport;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class PaginationContractTest {
    @AfterEach
    void clean() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void referenceCurrentWinsOverTemporaryAliasAndPageSizeIsBounded() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("current", "2");
        request.setParameter("pageNum", "7");
        request.setParameter("pageSize", "500");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        assertEquals(2, TableSupport.buildPageRequest().getPageNum());
        assertEquals(100, TableSupport.buildPageRequest().getPageSize());
    }

    @Test
    void existingCallerAliasRemainsCompatible() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("pageNum", "3");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        assertEquals(3, TableSupport.buildPageRequest().getPageNum());
    }

    @Test
    void malformedAndNegativeParametersCannotCreateUnboundedQueries() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("current", "invalid");
        request.setParameter("pageSize", "-10");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        assertEquals(1, TableSupport.buildPageRequest().getPageNum());
        assertEquals(1, TableSupport.buildPageRequest().getPageSize());
    }

    @Test
    void tableUsesDatabaseTotalRatherThanCurrentPageLength() {
        Page<Integer> page = new Page<>(2, 10);
        page.add(9);
        page.setTotal(29);
        TableDataInfo response = new Exposed().table(page);
        assertEquals(29, response.getTotal());
        assertEquals(1, response.getRows().size());
        assertEquals(200, response.getCode());
    }

    private static class Exposed extends BaseController {
        TableDataInfo table(Page<Integer> page) {
            return getDataTable(page);
        }
    }
}
