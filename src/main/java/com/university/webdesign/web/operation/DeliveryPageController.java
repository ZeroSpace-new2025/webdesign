package com.university.webdesign.web.operation;

import com.university.webdesign.common.PageResult;
import com.university.webdesign.service.operation.DeliveryService;
import com.university.webdesign.service.operation.dto.DeliveryQuery;
import com.university.webdesign.service.operation.dto.DeliveryTaskVO;
import com.university.webdesign.service.operation.dto.PrintBatchVO;
import com.university.webdesign.service.operation.dto.TaskStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/operation/deliveries")
public class DeliveryPageController
{
    private final DeliveryService deliveryService;

    public DeliveryPageController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @GetMapping
    public String list(
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "workstation", required = false) String workstation,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            Model model) {
        DeliveryQuery query = new DeliveryQuery();
        query.setDate(date == null ? LocalDate.now() : date);
        query.setStatus(status);
        query.setWorkstation(workstation);
        query.setPageNum(page);
        query.setPageSize(size);
        PageResult<DeliveryTaskVO> result = deliveryService.page(query);
        model.addAttribute("page", result);
        model.addAttribute("date", query.getDate());
        model.addAttribute("status", status);
        model.addAttribute("workstation", workstation);
        model.addAttribute("allStatus", TaskStatus.values());
        return "operation/deliveries";
    }

    @PostMapping("/generate")
    public String generate(
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            RedirectAttributes redirect) {
        try {
            List<Long> ids = deliveryService.generateTasks(date, null, null);
            redirect.addFlashAttribute("message", "已生成 " + ids.size() + " 张配送单");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/operation/deliveries" + (date == null ? "" : "?date=" + date);
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable("id") Long id, Model model) {
        model.addAttribute("task", deliveryService.getDetail(id));
        model.addAttribute("allStatus", TaskStatus.values());
        return "operation/deliveries-detail";
    }

    @PostMapping("/batch-print")
    public String batchPrint(
            @RequestParam(value = "taskIds", required = false) List<Long> taskIds,
            @RequestParam(value = "templateId", required = false) String templateId,
            Model model,
            RedirectAttributes redirect) {
        try {
            PrintBatchVO result = deliveryService.batchPrint(taskIds, templateId);
            model.addAttribute("batch", result);
            return "operation/deliveries-print";
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/operation/deliveries";
        }
    }

    @PostMapping("/{id}/status")
    public String updateStatus(
            @PathVariable("id") Long id,
            @RequestParam("status") String status,
            @RequestParam(value = "receiver", required = false) String receiver,
            @RequestParam(value = "remark", required = false) String remark,
            RedirectAttributes redirect) {
        try {
            deliveryService.updateStatus(id, TaskStatus.parse(status), receiver, remark);
            redirect.addFlashAttribute("message", "状态已更新");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/operation/deliveries/" + id;
    }
}