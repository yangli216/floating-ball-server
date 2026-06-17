package com.regionalai.floatingball.server.modules.lisresult.controller;

import com.regionalai.floatingball.server.common.api.ApiResponse;
import com.regionalai.floatingball.server.common.api.PageResponse;
import com.regionalai.floatingball.server.common.util.RequestIdUtils;
import com.regionalai.floatingball.server.modules.lisresult.dto.LisReportSubmitRequest;
import com.regionalai.floatingball.server.modules.lisresult.dto.LisReportSubmitResponse;
import com.regionalai.floatingball.server.modules.lisresult.dto.PacsReportSubmitRequest;
import com.regionalai.floatingball.server.modules.lisresult.entity.HiOdsApply;
import com.regionalai.floatingball.server.modules.lisresult.entity.HiOdsApplyLisReport;
import com.regionalai.floatingball.server.modules.lisresult.entity.HiOdsApplyPacsReport;
import com.regionalai.floatingball.server.modules.lisresult.service.LisResultEntryService;
import com.regionalai.floatingball.server.security.AdminContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping({"/admin/api/lis-result-entry", "/admin/api/exam-result-entry"})
public class AdminLisResultEntryController {

    private final LisResultEntryService lisResultEntryService;

    public AdminLisResultEntryController(LisResultEntryService lisResultEntryService) {
        this.lisResultEntryService = lisResultEntryService;
    }

    @GetMapping("/applies")
    public ApiResponse<PageResponse<HiOdsApply>> listApplies(@RequestParam(defaultValue = "1") long current,
                                                             @RequestParam(defaultValue = "10") long size,
                                                             @RequestParam(required = false) String keyword,
                                                             @RequestParam(required = false) String dispType,
                                                             @RequestParam(required = false) String businessType,
                                                             @RequestParam(required = false) String status,
                                                             @RequestParam(required = false) String idOrg,
                                                             @RequestParam(required = false) String dateFrom,
                                                             @RequestParam(required = false) String dateTo,
                                                             HttpServletRequest request) {
        return ApiResponse.success(
            lisResultEntryService.listApplies(current, size, keyword, dispType, businessType, status, idOrg, dateFrom, dateTo),
            RequestIdUtils.resolve(request)
        );
    }

    @GetMapping("/applies/{idApply}/reports")
    public ApiResponse<List<HiOdsApplyLisReport>> listReports(@PathVariable String idApply,
                                                              HttpServletRequest request) {
        return ApiResponse.success(
            lisResultEntryService.listReports(idApply),
            RequestIdUtils.resolve(request)
        );
    }

    @PostMapping("/applies/{idApply}/reports")
    public ApiResponse<LisReportSubmitResponse> submitLegacyLisReport(@PathVariable String idApply,
                                                                      @RequestBody LisReportSubmitRequest body,
                                                                      HttpServletRequest request) {
        return submitLisReport(idApply, body, request);
    }

    @PostMapping("/applies/{idApply}/lis-report")
    public ApiResponse<LisReportSubmitResponse> submitReport(@PathVariable String idApply,
                                                             @RequestBody LisReportSubmitRequest body,
                                                             HttpServletRequest request) {
        return submitLisReport(idApply, body, request);
    }

    @GetMapping("/applies/{idApply}/pacs-report")
    public ApiResponse<HiOdsApplyPacsReport> getPacsReport(@PathVariable String idApply,
                                                           HttpServletRequest request) {
        return ApiResponse.success(
            lisResultEntryService.getPacsReport(idApply),
            RequestIdUtils.resolve(request)
        );
    }

    @PostMapping("/applies/{idApply}/pacs-report")
    public ApiResponse<LisReportSubmitResponse> submitPacsReport(@PathVariable String idApply,
                                                                 @RequestBody PacsReportSubmitRequest body,
                                                                 HttpServletRequest request) {
        return ApiResponse.success(
            lisResultEntryService.submitPacsReport(idApply, body, AdminContextHolder.get()),
            RequestIdUtils.resolve(request)
        );
    }

    private ApiResponse<LisReportSubmitResponse> submitLisReport(String idApply,
                                                                 LisReportSubmitRequest body,
                                                                 HttpServletRequest request) {
        return ApiResponse.success(
            lisResultEntryService.submitReport(idApply, body, AdminContextHolder.get()),
            RequestIdUtils.resolve(request)
        );
    }
}
