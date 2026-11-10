package com.university.webdesign.web.operation;

import com.university.webdesign.common.enums.PermissionEnum;
import com.university.webdesign.config.RequiresPerm;
import com.university.webdesign.service.operation.ServiceWindowService;
import com.university.webdesign.service.operation.dto.ServiceWindowCmd;
import com.university.webdesign.service.operation.dto.ServiceWindowVO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 时间窗口配置页面控制器（M3 运营与履约）。
 * <p>
 * 订餐截止时间与配餐开始时间是全系统唯一的时间来源，修改后立即生效。
 * 访问控制：需要 {@link PermissionEnum#OPERATION_WINDOW_MANAGE} 权限点（餐厅经理）。
 */
@Controller
@RequestMapping("/operation/configs/service-window")
@RequiresPerm(PermissionEnum.OPERATION_WINDOW_MANAGE)
public class ServiceWindowPageController
{
    private final ServiceWindowService serviceWindowService;

    public ServiceWindowPageController(ServiceWindowService serviceWindowService) {
        this.serviceWindowService = serviceWindowService;
    }

    @GetMapping
    public String page(
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model) {
        LocalDate target = date == null ? LocalDate.now() : date;
        ServiceWindowVO window = serviceWindowService.get(target, null, null);
        model.addAttribute("window", window);
        model.addAttribute("date", target);

        // 👇 【新增】通知导航栏高亮“时间配置”
        model.addAttribute("active", "window");

        return "operation/service-window";
    }

    @PostMapping
    public String save(
            @RequestParam(value = "configId", required = false) Long configId,
            @RequestParam("cutoffTime") String cutoffTime,
            @RequestParam("deliveryStartTime") String deliveryStartTime,
            @RequestParam(value = "effectiveFrom", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveFrom,
            @RequestParam(value = "scope", required = false) String scope,
            @RequestParam(value = "deptId", required = false) Long deptId,
            RedirectAttributes redirect) {
        try {
            ServiceWindowCmd cmd = new ServiceWindowCmd();
            cmd.setCutoffTime(LocalTime.parse(cutoffTime));
            cmd.setDeliveryStartTime(LocalTime.parse(deliveryStartTime));
            cmd.setEffectiveFrom(effectiveFrom);
            cmd.setScope(scope);
            cmd.setDeptId(deptId);
            if (configId == null) {
                serviceWindowService.create(cmd);
                redirect.addFlashAttribute("message", "时间窗口已创建");
            } else {
                serviceWindowService.update(configId, cmd);
                redirect.addFlashAttribute("message", "时间窗口已更新，立即生效");
            }
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/operation/configs/service-window";
    }
}