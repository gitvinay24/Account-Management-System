package com.businessmanager.service;

import com.businessmanager.dao.BranchDAO;
import com.businessmanager.model.Branch;
import com.businessmanager.security.AccessControl;

import java.sql.SQLException;
import java.util.List;

public class BranchService {

    private final BranchDAO branchDAO;

    public BranchService() {
        this.branchDAO = new BranchDAO();
    }

    public BranchService(BranchDAO branchDAO) {
        this.branchDAO = branchDAO;
    }

    public List<Branch> getBranchesForBusiness(int businessId) throws SQLException {
        if (!AccessControl.canAccessModule("BRANCHES")) {
            throw new SecurityException("Access denied to BRANCHES module.");
        }
        return branchDAO.findByBusiness(businessId);
    }

    public Branch getBranchById(int id) throws SQLException {
        if (!AccessControl.canAccessModule("BRANCHES")) {
            throw new SecurityException("Access denied to BRANCHES module.");
        }
        return branchDAO.findById(id);
    }

    public int createBranch(Branch branch) throws SQLException {
        AccessControl.requireWrite("BRANCHES");
        return branchDAO.create(branch);
    }

    public boolean updateBranch(Branch branch) throws SQLException {
        AccessControl.requireWrite("BRANCHES");
        return branchDAO.update(branch);
    }

    public boolean deleteBranch(int id) throws SQLException {
        AccessControl.requireWrite("BRANCHES");
        return branchDAO.delete(id);
    }
}
