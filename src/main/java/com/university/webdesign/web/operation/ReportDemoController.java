package com.university.webdesign.web.operation;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@Controller
public class ReportDemoController {

    @GetMapping("/operation/monthly-report-demo")
    public String demo(Model model) {
        model.addAttribute("employeeName", "刘XX");
        model.addAttribute("phone", "12200993311");
        model.addAttribute("month", "2026-11");

        model.addAttribute("items", List.of(
                Map.of("name", "米饭", "unit", "两", "qty", 4, "price", 0.30, "total", 1.20),
                Map.of("name", "小炒肉", "unit", "份", "qty", 1, "price", 5.00, "total", 5.00),
                Map.of("name", "西红柿蛋汤", "unit", "份", "qty", 1, "price", 3.00, "total", 3.00)
        ));
        model.addAttribute("totalAmount", 9.20);
        return "operation/monthly-report";
    }
}