package com.ghartmann.fintrack.controller;

import com.ghartmann.fintrack.dto.SummaryResponse;
import com.ghartmann.fintrack.service.SummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ghartmann.fintrack.config.OpenApiConfig;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.YearMonth;

@Tag(name = "Resumo", description = "Resumo mensal de receitas, despesas e saldo")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
@RestController
@RequestMapping("/summary")
@RequiredArgsConstructor
public class SummaryController {

    private final SummaryService summaryService;

    @Operation(summary = "Resumo do mês",
            description = "Total de receitas, despesas, saldo e despesas por categoria. "
            + "Formato do mês: AAAA-MM. Sem o parâmetro, usa o mês atual.")
    @GetMapping
    public SummaryResponse getSummary(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        YearMonth target = (month != null) ? month : YearMonth.now();
        return summaryService.getMonthlySummary(target);
    }
}