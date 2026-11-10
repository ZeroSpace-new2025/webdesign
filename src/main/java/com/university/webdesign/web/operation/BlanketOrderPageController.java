package com.university.webdesign.web.operation;

import com.university.webdesign.common.DateRange;
import com.university.webdesign.common.PageQuery;
import com.university.webdesign.common.PageResult;
import com.university.webdesign.common.enums.PermissionEnum;
import com.university.webdesign.config.RequiresPerm;
import com.university.webdesign.service.operation.BlanketOrderService;
import com.university.webdesign.service.operation.dto.CategoryStatVO;
import com.university.webdesign.service.operation.dto.CategorySumVO;
import com.university.webdesign.service.operation.dto.DailyStatVO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 厨房备料页面控制器（M3 运营与履约）。
 * <p>
 * 对应需求：订餐截止后聚合总括订单，按分类统计产量供后厨备料，并可打印生产单。
 * 访问控制：需要 {@link PermissionEnum#OPERATION_AGGREGATE} 权限点（厨房主管、餐厅经理）。
 */
@Controller
@RequestMapping("/operation/blanket-orders")
@RequiresPerm(PermissionEnum.OPERATION_AGGREGATE)
public class BlanketOrderPageController
{
    private final BlanketOrderService blanketOrderService;

    public BlanketOrderPageController(BlanketOrderService blanketOrderService) {
        this.blanketOrderService = blanketOrderService;
    }

    @GetMapping
    public String page(
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model) {
        LocalDate target = date == null ? LocalDate.now() : date;
        List<CategoryStatVO> items = blanketOrderService.listAggregated(target, null);
        List<CategorySumVO> categorySums = blanketOrderService.sumByCategory(target);
        Map<String, List<CategoryStatVO>> groups = new LinkedHashMap<>();
        for (CategoryStatVO item : items) {
            String category = item.getCategory() == null ? "未分类" : item.getCategory();
            groups.computeIfAbsent(category, key -> new ArrayList<>()).add(item);
        }
        model.addAttribute("date", target);
        model.addAttribute("groups", groups);
        model.addAttribute("categorySums", categorySums);
        model.addAttribute("totalItems", items.size());
        model.addAttribute("hasSnapshot", !items.isEmpty());
        model.addAttribute("active", "blanket");
        return "operation/blanket-orders";
    }

    @PostMapping("/aggregate")
    public String aggregate(
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "force", defaultValue = "false") boolean force,
            RedirectAttributes redirect) {
        try {
            DailyStatVO result = blanketOrderService.aggregate(date, force);
            redirect.addFlashAttribute("message",
                    "聚合完成：有效订单 " + result.getTotalOrders() + " 单，总金额 " + result.getTotalAmount());
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/operation/blanket-orders" + (date == null ? "" : "?date=" + date);
    }

    @GetMapping("/print/preview")
    public String printPreview(
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model) {
        LocalDate target = date == null ? LocalDate.now() : date;

        // 查出当天的汇总列表
        List<CategoryStatVO> items = blanketOrderService.listAggregated(target, null);

        // 👇 算出总计金额
        BigDecimal totalAmount = BigDecimal.ZERO;
        if (items != null) {
            for (CategoryStatVO item : items) {
                if (item.getTotalAmount() != null) {
                    totalAmount = totalAmount.add(item.getTotalAmount());
                }
            }
        }

        model.addAttribute("date", target);
        model.addAttribute("items", items);
        model.addAttribute("totalAmount", totalAmount); // 传给前端
        return "operation/blanket-orders-print";
    }

    @GetMapping("/history")
    public String history(
            @RequestParam(value = "dateFrom", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(value = "dateTo", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            Model model) {
        PageQuery query = new PageQuery();
        query.setPageNum(page);
        query.setPageSize(size);
        PageResult<DailyStatVO> result = blanketOrderService.pageDailyStat(
                new DateRange(dateFrom, dateTo), query);
        model.addAttribute("page", result);
        model.addAttribute("dateFrom", dateFrom);
        model.addAttribute("dateTo", dateTo);
        model.addAttribute("active", "blanket");
        return "operation/blanket-orders-history";
    }
}