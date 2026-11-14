package com.businessmanager.service;

import com.businessmanager.dao.ReportDAO;
import com.businessmanager.model.ReportFilterDTO;
import com.businessmanager.security.AccessControl;

import java.sql.SQLException;
import java.util.List;

public class ReportService {

    private final ReportDAO reportDAO;

    public ReportService() {
        this.reportDAO = new ReportDAO();
    }

    public ReportService(ReportDAO reportDAO) {
        this.reportDAO = reportDAO;
    }

    public List<Object[]> generateReport(int businessId, ReportFilterDTO filter) throws SQLException {
        if (!AccessControl.canAccessModule("REPORTS")) {
            throw new SecurityException("Access denied to REPORTS module.");
        }
        return reportDAO.generateReport(businessId, filter);
    }
}
