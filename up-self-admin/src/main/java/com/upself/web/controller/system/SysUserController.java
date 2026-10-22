package com.upself.web.controller.system;

import com.upself.common.core.controller.BaseController;
import com.upself.common.core.domain.AjaxResult;
import com.upself.common.core.page.TableDataInfo;
import com.upself.system.mapper.SysUserMapper;
import com.upself.system.service.ISysUserService;
import com.upself.web.model.AuthBodies.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/system/user")
public class SysUserController extends BaseController {
    @Autowired private ISysUserService service;
    @Autowired private SysUserMapper mapper;

    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('system:user:list')")
    public TableDataInfo list(@RequestParam(required = false) String userName) {
        startPage();
        try {
            return getDataTable(service.list(userName));
        } finally {
            clearPage();
        }
    }

    @GetMapping("/candidates")
    @PreAuthorize("@ss.hasPermi('learning:course:assign')")
    public TableDataInfo candidates(
            @RequestParam String role, @RequestParam(required = false) String keyword) {
        if (!"mentor".equals(role) && !"student".equals(role))
            throw new com.upself.common.exception.ServiceException(
                    400, "Select a student or mentor role.");
        startPage();
        try {
            return getDataTable(service.candidates(role, keyword));
        } finally {
            clearPage();
        }
    }

    @GetMapping("/roles")
    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    public AjaxResult roles() {
        return AjaxResult.success(mapper.selectRoles());
    }

    @PostMapping
    @PreAuthorize("@ss.hasPermi('system:user:add')")
    public AjaxResult create(@Valid @RequestBody Create b) {
        return AjaxResult.success(
                service.create(b.getUserName(), b.getNickName(), b.getPassword(), b.getRoleId()));
    }

    @PutMapping
    @PreAuthorize("@ss.hasPermi('system:user:edit') and @ss.hasPermi('system:user:changeStatus')")
    public AjaxResult update(@Valid @RequestBody Update b) {
        service.update(b.getId(), b.getNickName(), b.getRoleId(), b.getStatus());
        return AjaxResult.success();
    }

    @PutMapping("/resetPwd")
    @PreAuthorize("@ss.hasPermi('system:user:resetPwd')")
    public AjaxResult reset(@Valid @RequestBody ResetPassword b) {
        service.resetPassword(b.getId(), b.getPassword());
        return AjaxResult.success();
    }

    @PutMapping("/{id}/resetTotp")
    @PreAuthorize("@ss.hasPermi('system:user:resetTotp')")
    public AjaxResult resetTotp(@PathVariable Long id) {
        service.resetTotp(id);
        return AjaxResult.success();
    }
}
