package org.guanzon.cas.purchasing.controller;

import com.mysql.cj.xdevapi.Client;
import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.guanzon.appdriver.agent.ShowDialogFX;
import org.guanzon.appdriver.agent.impl.Transaction;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.constant.RecordStatus;
import org.guanzon.appdriver.constant.UserRight;
import org.json.simple.JSONObject;
import ph.com.guanzongroup.cas.model.Model_PO_Quotation_Request_Master;
import org.guanzon.appdriver.impl.Model;
import org.guanzon.cas.inv.Inventory;
import org.guanzon.cas.purchasing.validator.POQuotationRequestValidatorFactory;
import ph.com.guanzongroup.cas.constants.POQuotationRequestStatus;
import ph.com.guanzongroup.cas.constants.Tables;
import ph.com.guanzongroup.cas.core.ObjectInitiator;
import ph.com.guanzongroup.cas.iface.GValidator;
import ph.com.guanzongroup.cas.iface.POQuotationRequestImpl;
import ph.com.guanzongroup.cas.model.Model_PO_Quotation_Request_Detail;
import ph.com.guanzongroup.cas.model.Model_PO_Quotation_Request_Supplier;
import ph.com.guanzongroup.cas.parameter.Branch;
import ph.com.guanzongroup.cas.parameter.Brand;
import ph.com.guanzongroup.cas.parameter.CategoryLevel2;
import ph.com.guanzongroup.cas.parameter.Company;
import ph.com.guanzongroup.cas.parameter.Department;
import ph.com.guanzongroup.cas.parameter.ModelVariant;
import ph.com.guanzongroup.cas.parameter.Term;

/**
 *
 * @author Arsiela
 */
public class POQuotationRequest extends Transaction implements POQuotationRequestImpl{
    private String psIndustryId = "";
    private String psCompanyId = "";
    private String psCategorCd = "";
    
    private String psSearchBranch;
    private String psSearchCategory;
    private String psSearchDepartment;
    
    List<Model_PO_Quotation_Request_Master> paMasterList;
    List<Model> paDetailRemoved;
    List<Model> paSuppliersRemoved;
    List<Model_PO_Quotation_Request_Supplier> paSuppliers;
    
    public void setIndustryId(String industryId) { psIndustryId = industryId; }
    public void setCompanyId(String companyId) { psCompanyId = companyId; }
    public void setCategoryId(String categoryId) { psCategorCd = categoryId; }
    
    public void setSearchBranch(String searchBranch) { psSearchBranch = searchBranch; }
    public void setSearchDepartment(String searchDepartment) { psSearchDepartment = searchDepartment; }
    public void setSearchCategory(String searchCategory) { psSearchCategory = searchCategory; }
    
    public String getSearchCategory() { return psSearchCategory; }
    public String getSearchDepartment() { return psSearchDepartment; }
    public String getSearchBranch() { return psSearchBranch; }
    
    @Override
    public JSONObject InitTransaction() throws SQLException, CloneNotSupportedException, GuanzonException{
        SOURCE_CODE = "POQR";

        poMaster = ObjectInitiator.createModel(Model_PO_Quotation_Request_Master.class, poGRider, Tables.PURCHASE_QUOTATION_REQUEST_MASTER);
        poDetail = ObjectInitiator.createModel(Model_PO_Quotation_Request_Detail.class, poGRider, Tables.PURCHASE_QUOTATION_REQUEST_DETAIL);

        paMasterList = new ArrayList<>();
        paDetail = new ArrayList<>();
        paDetailRemoved = new ArrayList<>();
        paSuppliers = new ArrayList<>();
        paSuppliersRemoved = new ArrayList<>();

        return initialize();
    }
    
    @Override
    public Model_PO_Quotation_Request_Master Master() {
        return (Model_PO_Quotation_Request_Master) poMaster;
    }
    
    @Override
    public Model_PO_Quotation_Request_Detail Detail(int row) {
        return (Model_PO_Quotation_Request_Detail) paDetail.get(row);
    }
    
    @Override
    public int getDetailCount() {
        if (paDetail == null) {
            paDetail = new ArrayList<>();
        }

        return paDetail.size();
    }

    @Override
    public JSONObject NewTransaction() throws CloneNotSupportedException {
        return super.NewTransaction();
    }

    @Override
    public JSONObject SaveTransaction() throws SQLException, GuanzonException, CloneNotSupportedException {
        return super.SaveTransaction();
    }

    @Override
    public JSONObject OpenTransaction(String transactionNo) throws SQLException, CloneNotSupportedException, GuanzonException{
        //Clear data
        resetMaster();
        resetOthers();
        Detail().clear();
        return super.OpenTransaction(transactionNo);
    }

    @Override
    public JSONObject UpdateTransaction() {
        return super.UpdateTransaction();
    }
    
    @Override
    public JSONObject ConfirmTransaction(String remarks) throws SQLException, CloneNotSupportedException, GuanzonException{
        poJSON = new JSONObject();

        String lsStatus = POQuotationRequestStatus.CONFIRMED;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

        if (lsStatus.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already confirmed.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        //seek for approval
        poJSON =  seekApproval();
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "Transaction confirmed successfully.");
        return poJSON;
    }
    
    @Override
    public JSONObject ApproveTransaction(String remarks) throws SQLException, CloneNotSupportedException, GuanzonException{
        poJSON = new JSONObject();

        String lsStatus = POQuotationRequestStatus.APPROVED;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

        if (lsStatus.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already approved.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        //seek for approval
        if (POQuotationRequestStatus.CONFIRMED.equals(Master().getTransactionStatus())) {
            poJSON =  seekApproval();
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
        }
        
        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "Transaction approved successfully.");
        return poJSON;
    }
    
    @Override
    public JSONObject PostTransaction(String remarks) throws SQLException, CloneNotSupportedException, GuanzonException{
        poJSON = new JSONObject();

        String lsStatus = POQuotationRequestStatus.POSTED;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

        if (lsStatus.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already posted.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        //seek for approval
        poJSON =  seekApproval();
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "Transaction posted successfully.");

        return poJSON;
    }
    
    @Override
    public JSONObject VoidTransaction(String remarks) throws SQLException, CloneNotSupportedException, GuanzonException{
        poJSON = new JSONObject();

        String lsStatus = POQuotationRequestStatus.VOID;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

        if (lsStatus.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already voided.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        //seek for approval
        if (POQuotationRequestStatus.CONFIRMED.equals(Master().getTransactionStatus())) {
            poJSON =  seekApproval();
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
        }
        
        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "Transaction voided successfully.");
        return poJSON;
    }
    
    @Override
    public JSONObject CancelTransaction(String remarks) throws SQLException, CloneNotSupportedException, GuanzonException{
        poJSON = new JSONObject();

        String lsStatus = POQuotationRequestStatus.CANCELLED;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

        if (lsStatus.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already cancelled.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        //seek for approval
        if (POQuotationRequestStatus.CONFIRMED.equals(Master().getTransactionStatus())) {
            poJSON =  seekApproval();
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
        }
        
        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "Transaction cancelled successfully.");
        return poJSON;
    }
    
    @Override
    public JSONObject DisapproveTransaction(String remarks) throws SQLException, CloneNotSupportedException, GuanzonException{
        poJSON = new JSONObject();

        String lsStatus = POQuotationRequestStatus.CANCELLED;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

        if (lsStatus.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already dis-approved.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        //seek for approval
        if (POQuotationRequestStatus.CONFIRMED.equals(Master().getTransactionStatus())) {
            poJSON =  seekApproval();
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
        }
        
        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "Transaction dis-approved successfully.");
        return poJSON;
    }
    
    @Override
    public JSONObject ReturnTransaction(String remarks) throws SQLException, CloneNotSupportedException, GuanzonException{
        poJSON = new JSONObject();
        String lsStatus = POQuotationRequestStatus.RETURNED;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

        if (lsStatus.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already returned.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
                
        //seek for approval
        if (POQuotationRequestStatus.CONFIRMED.equals(Master().getTransactionStatus())) {
            poJSON =  seekApproval();
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
        }
        
        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "Transaction returned successfully.");
        return poJSON;
    }
    
    /*Search References*/
    @Override //POQuotationRequestImpl
    public JSONObject SearchTransaction() throws CloneNotSupportedException, SQLException, GuanzonException {
        poJSON = new JSONObject();
        String lsTransStat = "";
        if (psTranStat != null) {
            if (psTranStat.length() > 1) {
                for (int lnCtr = 0; lnCtr <= psTranStat.length() - 1; lnCtr++) {
                    lsTransStat += ", " + SQLUtil.toSQL(Character.toString(psTranStat.charAt(lnCtr)));
                }
                lsTransStat = " AND a.cTranStat IN (" + lsTransStat.substring(2) + ")";
            } else {
                lsTransStat = " AND a.cTranStat = " + SQLUtil.toSQL(psTranStat);
            }
        }

        initSQL();
        String lsSQL = MiscUtil.addCondition(SQL_BROWSE, " a.sIndstCdx = " + SQLUtil.toSQL(psIndustryId)
                                            + " AND a.sCategrCd = " + SQLUtil.toSQL(psCategorCd)
                                            + " AND a.sBranchCd = " + SQLUtil.toSQL(poGRider.getBranchCode()));
        
        if (lsTransStat != null && !"".equals(lsTransStat)) {
            lsSQL = lsSQL + lsTransStat;
        }

        System.out.println("Executing SQL: " + lsSQL);
        poJSON = ShowDialogFX.Browse(poGRider,
                lsSQL,
                "",
                "Transaction Date»Transaction No»Branch»Department»Category",
                "dTransact»sTransNox»Branch»Department»Category2", 
                "a.dTransact»a.sTransNox»c.sBranchNm»d.sDeptName»f.sDescript", 
                0);

        if (poJSON != null) {
            return OpenTransaction((String) poJSON.get("sTransNox"));
        } else {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No record loaded.");
            return poJSON;
        }
    }
    
    @Override //POQuotationRequestImpl
    public JSONObject SearchTransaction(String fsBranch, String fsDepartment, String fsCateogry, String fsTransactionDate, String fsTransactionNo)
            throws CloneNotSupportedException,
            SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        String lsTransStat = "";
        if (psTranStat != null) {
            if (psTranStat.length() > 1) {
                for (int lnCtr = 0; lnCtr <= psTranStat.length() - 1; lnCtr++) {
                    lsTransStat += ", " + SQLUtil.toSQL(Character.toString(psTranStat.charAt(lnCtr)));
                }
                lsTransStat = " AND a.cTranStat IN (" + lsTransStat.substring(2) + ")";
            } else {
                lsTransStat = " AND a.cTranStat = " + SQLUtil.toSQL(psTranStat);
            }
        }
        
        String lsBranch = fsBranch != null && !"".equals(fsBranch) 
                                                    ? " AND c.sBranchNm LIKE " + SQLUtil.toSQL("%"+fsBranch)
                                                    : "";
        
        String lsDepartment = fsDepartment != null && !"".equals(fsDepartment) 
                                                    ? " AND d.sDeptName LIKE " + SQLUtil.toSQL("%"+fsDepartment)
                                                    : "";
        
        String lsCategory = fsCateogry != null && !"".equals(fsCateogry) 
                                                    ? " AND f.sDescript LIKE " + SQLUtil.toSQL("%"+fsCateogry)
                                                    : "";
        
        String lsTransactionDate = fsTransactionDate != null && !"".equals(fsTransactionDate) && !"1900-01-01".equals(fsTransactionDate) 
                                                    ? " AND a.dTransact = " + SQLUtil.toSQL(fsTransactionDate)
                                                    : "";
        
        String lsTransactionNo = fsTransactionNo != null && !"".equals(fsTransactionNo) 
                                                    ? " AND a.sTransNox LIKE " + SQLUtil.toSQL("%"+fsTransactionNo)
                                                    : "";

        initSQL();
        String lsSQL = MiscUtil.addCondition(SQL_BROWSE, 
                   " a.sIndstCdx = " + SQLUtil.toSQL(psIndustryId)
                 + " AND a.sCategrCd = " + SQLUtil.toSQL(psCategorCd)
                 + " AND a.sBranchCd = " + SQLUtil.toSQL(poGRider.getBranchCode())
                 + lsBranch
                 + lsDepartment
                 + lsCategory
                 + lsTransactionDate
                 + lsTransactionNo
                );
        
        if (lsTransStat != null && !"".equals(lsTransStat)) {
            lsSQL = lsSQL + lsTransStat;
        }

        System.out.println("Executing SQL: " + lsSQL);
        poJSON = ShowDialogFX.Browse(poGRider,
                lsSQL,
                "",
                "Transaction Date»Transaction No»Branch»Department»Category",
                "dTransact»sTransNox»Branch»Department»Category2", 
                "a.dTransact»a.sTransNox»c.sBranchNm»d.sDeptName»f.sDescript", 
                1);

        if (poJSON != null) {
            return OpenTransaction((String) poJSON.get("sTransNox"));
        } else {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No record loaded.");
            return poJSON;
        }
    }
    
    @Override //POQuotationRequestImpl
    public JSONObject SearchDestination(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Branch object = ObjectInitiator.createParameter(Branch.class, poGRider, true, logwrapr);
        object.setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
            Master().setDestination(object.getModel().getBranchCode());
        }
        return poJSON;
    }
     
    @Override //POQuotationRequestImpl
    public JSONObject SearchBranch(String value, boolean byCode, boolean isSearch) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Branch object = ObjectInitiator.createParameter(Branch.class, poGRider, true, logwrapr);
        object.setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);
        if ("success".equals((String) poJSON.get("result"))) {
            if(isSearch){
                setSearchBranch(object.getModel().getBranchName());
            } else {
                Master().setBranchCode(object.getModel().getBranchCode());
            }
        }

        return poJSON;
    }
    
    @Override //POQuotationRequestImpl
    public JSONObject SearchDepartment(String value, boolean byCode, boolean isSearch) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Department object = ObjectInitiator.createParameter(Department.class, poGRider, true, logwrapr);
        object.setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
            if(isSearch){
                setSearchDepartment(object.getModel().getDescription());
            } else {
                Master().setDepartmentId(object.getModel().getDepartmentId());
            }
        }
        return poJSON;
    }
    
    @Override //POQuotationRequestImpl
    public JSONObject SearchCategory(String value, boolean byCode, boolean isSearch) throws SQLException, GuanzonException {
        poJSON = new JSONObject();

        CategoryLevel2 object = ObjectInitiator.createParameter(CategoryLevel2.class, poGRider, true, logwrapr);
        String lsSQL = MiscUtil.addCondition(object.getSQ_Browse(), "cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE)
                                            + " AND (sIndstCdx = '' OR ISNULL(sIndstCdx))"); //+ SQLUtil.toSQL(Master().getIndustryId()));
        
        System.out.println("Executing SQL: " + lsSQL);
        poJSON = ShowDialogFX.Browse(poGRider,
                lsSQL,
                value,
                "Category ID»Description",
                "sCategrCd»sDescript",
                "sCategrCd»sDescript",
                byCode ? 0 : 1);

        if (poJSON != null) {
            poJSON = object.getModel().openRecord((String) poJSON.get("sCategrCd"));
            if ("success".equals((String) poJSON.get("result"))) {
                if(isSearch){
                    setSearchCategory(object.getModel().getDescription());
                } else {
                    System.out.println("Category2 ID: " + object.getModel().getCategoryId());
                    System.out.println("Description " + object.getModel().getDescription());
                    Master().setCategoryLevel2(object.getModel().getCategoryId());
                }
            }
        } else {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No record loaded.");
        }
        
        return poJSON;
    }
    
    @Override //POQuotationRequestImpl
    public JSONObject SearchBrand(String value, boolean byCode, int row) throws ExceptionInInitializerError, SQLException, GuanzonException {
        poJSON = new JSONObject();
        
        if(Master().getCategoryLevel2()== null || "".equals(Master().getCategoryLevel2())){
            poJSON.put("result", "error");
            poJSON.put("message", "Category is not set.");
            return poJSON;
        }
        
        Brand object = ObjectInitiator.createParameter(Brand.class, poGRider, true, logwrapr);
        object.setRecordStatus(RecordStatus.ACTIVE);

//        poJSON = object.searchRecord(value, byCode, Master().getIndustryId());
        poJSON = object.searchRecord(value, byCode, ""); //empPag nasa general empty yung industry according to ma'am she
        if ("success".equals((String) poJSON.get("result"))) {
            if (!object.getModel().getBrandId().equals(Detail(row).getBrandId())) {
                Detail(row).setModelId("");
                Detail(row).setStockId("");
                Detail(row).setDescription("");
            }

            Detail(row).setBrandId(object.getModel().getBrandId());
        }
        return poJSON;
    }

    @Override //POQuotationRequestImpl
    public JSONObject SearchModel(String value, boolean byCode, int row) throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        poJSON.put("row", row);

        if(Detail(row).getBrandId() == null || "".equals(Detail(row).getBrandId())){
            poJSON.put("result", "error");
            poJSON.put("message", "Brand is not set.");
            return poJSON;
        }
        
        ModelVariant object = ObjectInitiator.createParameter(ModelVariant.class, poGRider, true, logwrapr);
        object.setRecordStatus(RecordStatus.ACTIVE);
        System.out.println("Brand ID : "  + Detail(row).getBrandId());
        poJSON = object.searchRecordByBrand(value, byCode, Detail(row).getBrandId());
        poJSON.put("row", row);
        if ("success".equals((String) poJSON.get("result"))) {
            if (!object.getModel().getModelId().equals(Detail(row).getModelId())) {
                Detail(row).setStockId("");
                Detail(row).setDescription("");
            }
            
            Detail(row).setModelId(object.getModel().getModelId());
        }

        return poJSON;
    }
    
    @Override //POQuotationRequestImpl
    public JSONObject SearchInventory(String value, boolean byCode, int row) throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        poJSON.put("row", row);
        
        if(Master().getCategoryLevel2()== null || "".equals(Master().getCategoryLevel2())){
            poJSON.put("result", "error");
            poJSON.put("message", "Category is not set.");
            return poJSON;
        }
        
        String lsBrand = Detail(row).getBrandId() != null && !"".equals(Detail(row).getBrandId()) 
                                                    ? " AND a.sBrandIDx = " + SQLUtil.toSQL(Detail(row).getBrandId())
                                                    : "";
        String lsModel = Detail(row).getModelId()!= null && !"".equals(Detail(row).getModelId()) 
                                                    ? " AND a.sModelIDx = " + SQLUtil.toSQL(Detail(row).getModelId())
                                                    : "";
        String lsCategory2 = Master().getCategoryLevel2() != null && !"".equals(Master().getCategoryLevel2()) 
                                                    ? " AND a.sCategCd2 = " + SQLUtil.toSQL(Master().getCategoryLevel2())
                                                    : "";
        Inventory object = ObjectInitiator.createParameter(Inventory.class, poGRider, true, logwrapr);
        String lsSQL = MiscUtil.addCondition(object.getSQ_Browse(), 
                                             //" a.cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE)
                                            " a.sCategCd1 = " + SQLUtil.toSQL(Master().getCategoryCode())
                                            + lsCategory2
                                            + lsBrand
                                            + lsModel
                                            );
        
        System.out.println("Executing SQL: " + lsSQL);
        poJSON = ShowDialogFX.Browse(poGRider,
                lsSQL,
                value,
                "Barcode»Description»Brand»Model»Variant»UOM",
                "sBarCodex»sDescript»xBrandNme»xModelNme»xVrntName»xMeasurNm",
                "a.sBarCodex»TRIM(a.sDescript)»IFNULL(b.sDescript, '')»IFNULL(c.sDescript, '')»IFNULL(f.sDescript, '')»IFNULL(e.sDescript, '')",
                byCode ? 0 : 1);

        if (poJSON != null) {
            poJSON = object.getModel().openRecord((String) poJSON.get("sStockIDx"));
            if ("success".equals((String) poJSON.get("result"))) {
                JSONObject loJSON = checkExistingDetail(row,
                        object.getModel().getStockId(),
                        object.getModel().getDescription().trim(), 
                        true
                        );
                if ("error".equals((String) loJSON.get("result"))) {
                    if((boolean) loJSON.get("reverse")){
                        return loJSON;
                    } else {
                        row = (int) loJSON.get("row");
                        Detail(row).isReverse(true);
                    }
                }

                Detail(row).setStockId(object.getModel().getStockId());
                Detail(row).setDescription(object.getModel().getDescription().trim());
                Detail(row).setBrandId(object.getModel().getBrandId());
                Detail(row).setModelId(object.getModel().getModelId());
                   
                if(object.getModel().getSellingPrice() != null){
                    Detail(row).setUnitPrice(object.getModel().getSellingPrice().doubleValue());
                } else {
                    Detail(row).setUnitPrice(0.0000);
                }
            } 
            
            System.out.println("Barcode : " + Detail(row).Inventory().getBarCode());
            System.out.println("Description : " + Detail(row).Inventory().getDescription());
            System.out.println("Category : " + Master().Category2().getDescription());
            System.out.println("Brand : " + Detail(row).Brand().getDescription());
            System.out.println("Model : " + Detail(row).Model().getDescription());
            
        } else {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No record loaded.");
        }
        
        if ("error".equals((String) poJSON.get("result"))) {
            if(!"".equals(value)){
                poJSON = checkExistingDetail(row,
                        "",
                        value,
                        false
                        );
                if ("error".equals((String) poJSON.get("result"))) {
                    if((boolean) poJSON.get("reverse")){
                        return poJSON;
                    } else {
                        row = (int) poJSON.get("row");
                        Detail(row).isReverse(true);
                    }
                }
            }

            Detail(row).setStockId("");
            Detail(row).setBrandId("");
            Detail(row).setModelId("");
            Detail(row).setDescription(value);
            Detail(row).setUnitPrice(0.0000);
        }
        
        poJSON.put("result", "success");
        poJSON.put("message", "success");
        poJSON.put("row", row);
        return poJSON;
    }
    
    @Override //POQuotationRequestImpl
    public JSONObject SearchSupplier(String value, boolean byCode, int row) throws SQLException, GuanzonException {
        Client object = new ClientControllers(poGRider, logwrapr).Client();
        object.Master().setRecordStatus(RecordStatus.ACTIVE);
        object.Master().setClientType("1");
        poJSON = object.Master().searchRecord(value, byCode);
        
        if ("success".equals((String) poJSON.get("result"))) {
            JSONObject loJSON = checkExistingSupplier(row,
                    object.Master().getModel().getClientId(),
                    POQuotationRequestSupplierList(row).getCompanyId()
                    );
            if ("error".equals((String) loJSON.get("result"))) {
                if((boolean) loJSON.get("reverse")){
                    return loJSON;
                } else {
                    //reset the current row
                    if( POQuotationRequestSupplierList(row).getEditMode() == EditMode.ADDNEW){
                        POQuotationRequestSupplierList(row).setSupplierId("");
                        POQuotationRequestSupplierList(row).setAddressId("");
                        POQuotationRequestSupplierList(row).setContactId("");
                        POQuotationRequestSupplierList(row).setCompanyId("");
                    }
                    
                    //reverse into true the result row
                    row = (int) loJSON.get("row");
                    POQuotationRequestSupplierList(row).isReverse(true);
                }
            }
            
            POQuotationRequestSupplierList(row).setSupplierId(object.Master().getModel().getClientId());
            POQuotationRequestSupplierList(row).setAddressId(object.ClientAddress().getModel().getAddressId());
            POQuotationRequestSupplierList(row).setContactId(object.ClientInstitutionContact().getModel().getClientId());
        } else {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No record loaded.");
            poJSON.put("row", row);
        }
        System.out.println("Supplier : " + POQuotationRequestSupplierList(row).Supplier().getCompanyName());
        
        poJSON.put("result", "success");
        poJSON.put("message", "success");
        poJSON.put("row", row);
        return poJSON;
    }

    @Override //POQuotationRequestImpl
    public JSONObject SearchTerm(String value, boolean byCode, int row) throws ExceptionInInitializerError, SQLException, GuanzonException {
        poJSON = new JSONObject();
        
        if(POQuotationRequestSupplierList(row).getSupplierId() == null || "".equals(POQuotationRequestSupplierList(row).getSupplierId())){
            poJSON.put("result", "error");
            poJSON.put("message", "Supplier cannot be empty.");
            return poJSON;
        }
        
        Term object = ObjectInitiator.createParameter(Term.class, poGRider, true, logwrapr);
        object.setRecordStatus("1");

        poJSON = object.searchRecord(value, byCode);
        if ("success".equals((String) poJSON.get("result"))) {
            POQuotationRequestSupplierList(row).setTerm(object.getModel().getTermId());
        }

        System.out.println("Term : " + POQuotationRequestSupplierList(row).Term().getTermValue());
        return poJSON;
    }
    
    @Override //POQuotationRequestImpl
    public JSONObject SearchCompany(String value, boolean byCode, int row) throws ExceptionInInitializerError, SQLException, GuanzonException {
        poJSON = new JSONObject();
        
        if(POQuotationRequestSupplierList(row).getSupplierId() == null || "".equals(POQuotationRequestSupplierList(row).getSupplierId())){
            poJSON.put("result", "error");
            poJSON.put("message", "Supplier cannot be empty.");
            return poJSON;
        }
        
        Company object = ObjectInitiator.createParameter(Company.class, poGRider, true, logwrapr);
        object.setRecordStatus(RecordStatus.ACTIVE);
        
        poJSON = object.searchRecord(value, byCode);
        if ("success".equals((String) poJSON.get("result"))) {
            poJSON = checkExistingSupplier(row,
                    POQuotationRequestSupplierList(row).getSupplierId(),
                    object.getModel().getCompanyId()
                    );
            if ("error".equals((String) poJSON.get("result"))) {
                if((boolean) poJSON.get("reverse")){
                    return poJSON;
                } else {
                    //reset the current row
                    if( POQuotationRequestSupplierList(row).getEditMode() == EditMode.ADDNEW){
                        POQuotationRequestSupplierList(row).setSupplierId("");
                        POQuotationRequestSupplierList(row).setAddressId("");
                        POQuotationRequestSupplierList(row).setContactId("");
                        POQuotationRequestSupplierList(row).setCompanyId("");
                    }
                    
                    //reverse into true the result row
                    row = (int) poJSON.get("row");
                    POQuotationRequestSupplierList(row).isReverse(true);
                }
            }
            POQuotationRequestSupplierList(row).setCompanyId(object.getModel().getCompanyId());
        }
        System.out.println("Company ID : " + POQuotationRequestSupplierList(row).Company().getCompanyId());
        System.out.println("Company : " + POQuotationRequestSupplierList(row).Company().getCompanyName());
        
        poJSON.put("result", "success");
        poJSON.put("message", "success");
        poJSON.put("row", row);
        return poJSON;
    }
    
    /*Load*/
    @Override //POQuotationRequestImpl
    public JSONObject LoadPOQuotationRequestList(String fsBranch, String fsDepartment, String fsCateogry, Date fdTransactionDate, String fsTransactionNo, boolean isApproval) {
        try {
            
            String lsBranch = fsBranch != null && !"".equals(fsBranch) 
                                                        ? " AND c.sBranchNm LIKE " + SQLUtil.toSQL("%"+fsBranch)
                                                        : "";

            String lsDepartment = fsDepartment != null && !"".equals(fsDepartment) 
                                                        ? " AND d.sDeptName LIKE " + SQLUtil.toSQL("%"+fsDepartment)
                                                        : "";

            String lsCategory = fsCateogry != null && !"".equals(fsCateogry) 
                                                        ? " AND f.sDescript LIKE " + SQLUtil.toSQL("%"+fsCateogry)
                                                        : "";

            String lsTransactionDate = fdTransactionDate != null && !"".equals(fdTransactionDate) && !"1900-01-01".equals(xsDateShort(fdTransactionDate)) 
                                                        ? " AND a.dTransact = " + SQLUtil.toSQL(xsDateShort(fdTransactionDate))
                                                        : "";

            String lsTransactionNo = fsTransactionNo != null && !"".equals(fsTransactionNo) 
                                                        ? " AND a.sTransNox LIKE " + SQLUtil.toSQL("%"+fsTransactionNo)
                                                        : "";
            
            String lsApproval = "";
            if(isApproval){
                lsApproval = " AND a.sTransNox NOT IN (SELECT q.sSourceNo FROM po_quotation_master q WHERE q.sSourceNo = a.sTransNox ) ";
            }

            String lsTransStat = "";
            if (psTranStat != null) {
                if (psTranStat.length() > 1) {
                    for (int lnCtr = 0; lnCtr <= psTranStat.length() - 1; lnCtr++) {
                        lsTransStat += ", " + SQLUtil.toSQL(Character.toString(psTranStat.charAt(lnCtr)));
                    }
                    lsTransStat = " AND a.cTranStat IN (" + lsTransStat.substring(2) + ")";
                } else {
                    lsTransStat = " AND a.cTranStat = " + SQLUtil.toSQL(psTranStat);
                }
            }

            initSQL();
            String lsSQL = MiscUtil.addCondition(SQL_BROWSE, " a.sIndstCdx = " + SQLUtil.toSQL(psIndustryId)
                    + " AND a.sCategrCd = " + SQLUtil.toSQL(psCategorCd)
                    + lsBranch
                    + lsDepartment
                    + lsCategory
                    + lsTransactionDate
                    + lsTransactionNo
            );
            
            if (lsApproval != null && !"".equals(lsApproval)) {
                lsSQL = lsSQL + lsApproval;
            }
            
            if (lsTransStat != null && !"".equals(lsTransStat)) {
                lsSQL = lsSQL + lsTransStat;
            }

            lsSQL = lsSQL + " ORDER BY a.dTransact DESC ";

            System.out.println("Executing SQL: " + lsSQL);
            ResultSet loRS = poGRider.executeQuery(lsSQL);
            poJSON = new JSONObject();

            int lnctr = 0;

            if (MiscUtil.RecordCount(loRS) >= 0) {
                paMasterList = new ArrayList<>();
                while (loRS.next()) {
                    // Print the result set
                    System.out.println("sTransNox: " + loRS.getString("sTransNox"));
                    System.out.println("dTransact: " + loRS.getDate("dTransact"));
                    System.out.println("------------------------------------------------------------------------------");

                    paMasterList.add(POQuotationRequestMaster());
                    paMasterList.get(paMasterList.size() - 1).openRecord(loRS.getString("sTransNox"));
                    lnctr++;
                }

                System.out.println("Records found: " + lnctr);
                poJSON.put("result", "success");
                poJSON.put("message", "Record loaded successfully.");
            } else {
                paMasterList = new ArrayList<>();
                paMasterList.add(POQuotationRequestMaster());
                poJSON.put("result", "error");
                poJSON.put("continue", true);
                poJSON.put("message", "No record found.");
            }
            MiscUtil.close(loRS);
        } catch (SQLException e) {
            poJSON.put("result", "error");
            poJSON.put("message", e.getMessage());
        } catch (GuanzonException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
        }
        return poJSON;
    }
    
    @Override //POQuotationRequestImpl
    public JSONObject LoadPOQuotationRequestSupplierList()throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        paSuppliers = new ArrayList<>();

        List loList = getPOQuotationRequestSupplier();
        for (int lnCtr = 0; lnCtr <= loList.size() - 1; lnCtr++) {
            paSuppliers.add(POQuotationRequestSupplier());
            poJSON = paSuppliers.get(getPOQuotationRequestSupplierCount()- 1).openRecord((String) loList.get(lnCtr), lnCtr+1);
            if ("success".equals((String) poJSON.get("result"))) {
                if(Master().getEditMode() == EditMode.UPDATE){
                   poJSON = paSuppliers.get(getPOQuotationRequestSupplierCount() - 1).updateRecord();
                }
            }
            
        }
        return poJSON;
    }
    
    
    @Override //POQuotationRequestImpl
    public void ReloadDetail() throws SQLException, CloneNotSupportedException, GuanzonException{
        String lsBrandId = "";
        String lsModelId = "";
        int lnCtr = getDetailCount() - 1;
        while (lnCtr >= 0) {
//            System.out.println("Brand : " + Detail(lnCtr).getBrandId());
//            System.out.println("Model : " + Detail(lnCtr).getModelId());
//            System.out.println("Description : " + Detail(lnCtr).getDescription());
            if ((Detail(lnCtr).getStockId() == null || "".equals(Detail(lnCtr).getStockId()))
                    && (Detail(lnCtr).getDescription()== null || "".equals(Detail(lnCtr).getDescription()))) {
                
                if (Detail(lnCtr).getBrandId() != null
                    && !"".equals(Detail(lnCtr).getBrandId())) {
                    lsBrandId = Detail(lnCtr).getBrandId();
                }
                
                if (Detail(lnCtr).getModelId()!= null
                    && !"".equals(Detail(lnCtr).getModelId())) {
                    lsModelId = Detail(lnCtr).getModelId();
                }
                
                if(Detail(lnCtr).getEditMode() == EditMode.ADDNEW){
//                    System.out.println("Delete Detail : " + lnCtr);
                    deleteDetail(lnCtr); 
                    //Detail().remove(lnCtr);
                }
//                else if (Detail(lnCtr).getEditMode() == EditMode.UPDATE) {
//                    Detail(lnCtr).isReverse(false);
//                    removeDetail(Detail(lnCtr));
//                }
            }
            lnCtr--;
        }

        if ((getDetailCount() - 1) >= 0) {
            if (
                !Detail(getDetailCount() - 1).isReverse() &&
                (Detail(getDetailCount() - 1).getStockId() != null
                    && !"".equals(Detail(getDetailCount() - 1).getStockId()))
                || (Detail(getDetailCount() - 1).getDescription()!= null
                    && !"".equals(Detail(getDetailCount() - 1).getDescription()))) {
                AddDetail();
            }
        }

        if ((getDetailCount() - 1) < 0) {
            AddDetail();
        }
        
        //Set brand Id to last row
        if (!lsBrandId.isEmpty()) {
            Detail(getDetailCount() - 1).setBrandId(lsBrandId);
        }
        if (!lsModelId.isEmpty()) {
            Detail(getDetailCount() - 1).setModelId(lsModelId);
        }
    }
    
    @Override //POQuotationRequestImpl
    public void ReloadSupplier() throws CloneNotSupportedException, SQLException, GuanzonException{     
        
        if(getEditMode() == EditMode.ADDNEW || getEditMode() == EditMode.UPDATE){
        } else {
            return;
        }
        int lnRow = getPOQuotationRequestSupplierCount() - 1;
        while (lnRow >= 0) {
            if ((paSuppliers.get(lnRow).getSupplierId() == null || "".equals(paSuppliers.get(lnRow).getSupplierId()))
                    && (paSuppliers.get(lnRow).getCompanyId() == null || "".equals(paSuppliers.get(lnRow).getCompanyId()))
                    ) {
                
                if(paSuppliers.get(lnRow).getEditMode() == EditMode.ADDNEW){
                    paSuppliers.remove(lnRow);
                } else {
                    paSuppliers.get(lnRow).isReverse(false);
                }
            }
            lnRow--;
        }

        if ((getPOQuotationRequestSupplierCount()- 1) >= 0) {
            if (paSuppliers.get(getPOQuotationRequestSupplierCount() - 1).getSupplierId()!= null && !"".equals(paSuppliers.get(getPOQuotationRequestSupplierCount() - 1).getSupplierId())
                && (paSuppliers.get(getPOQuotationRequestSupplierCount() - 1).getCompanyId() != null && !"".equals(paSuppliers.get(getPOQuotationRequestSupplierCount() - 1).getCompanyId()))
                    ) {
                AddPOQuotationRequestSupplier();
            }
        }

        if ((getPOQuotationRequestSupplierCount() - 1) < 0) {
            AddPOQuotationRequestSupplier();
        }
    }
   
    @Override //POQuotationRequestImpl
    public List<Model_PO_Quotation_Request_Supplier> POQuotationRequestSupplierList() {
        return paSuppliers;
    }
    
    @Override //POQuotationRequestImpl
    public Model_PO_Quotation_Request_Master POQuotationRequestList(int row) {
        return (Model_PO_Quotation_Request_Master) paMasterList.get(row);
    }

    @Override //POQuotationRequestImpl
    public Model_PO_Quotation_Request_Supplier POQuotationRequestSupplierList(int row) {
        return (Model_PO_Quotation_Request_Supplier) paSuppliers.get(row);
    }
    
    private Model_PO_Quotation_Request_Master POQuotationRequestMaster() throws SQLException, GuanzonException {
        return ObjectInitiator.createModel(Model_PO_Quotation_Request_Master.class, poGRider, Tables.PURCHASE_QUOTATION_REQUEST_MASTER);
    }
    
    private Model_PO_Quotation_Request_Supplier POQuotationRequestSupplier() throws SQLException, GuanzonException {
        return ObjectInitiator.createModel(Model_PO_Quotation_Request_Supplier.class, poGRider, Tables.PURCHASE_QUOTATION_REQUEST_SUPPLIER);
    }   
    
    @Override //POQuotationRequestImpl
    public int getPOQuotationRequestCount() {
        if (paMasterList == null) {
            paMasterList = new ArrayList<>();
        }

        return paMasterList.size();
    }
    
    @Override //POQuotationRequestImpl
    public int getPOQuotationRequestSupplierCount() {
        if (paSuppliers == null) {
            paSuppliers = new ArrayList<>();
        }

        return paSuppliers.size();
    }
    
    private List getPOQuotationRequestSupplier() throws SQLException, GuanzonException {
        String lsSQL = MiscUtil.makeSelect(POQuotationRequestSupplier());
        lsSQL = MiscUtil.addCondition(lsSQL, " sTransNox = " + SQLUtil.toSQL(Master().getTransactionNo()));
        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        List<String> loList = new ArrayList();
        while (loRS.next()) {
             loList.add(loRS.getString("sTransNox")); 
        }
        return loList;
    }
    
    @Override
    public JSONObject AddDetail() throws SQLException, CloneNotSupportedException, GuanzonException{
        poJSON = new JSONObject();

        if (getDetailCount() > 0) {
            if (Detail(getDetailCount() - 1).getStockId()!= null || Detail(getDetailCount() - 1).getDescription() != null) {
                if (Detail(getDetailCount() - 1).getStockId().isEmpty() && Detail(getDetailCount() - 1).getDescription().isEmpty()) {
                    poJSON.put("result", "error");
                    poJSON.put("message", "Last row has empty item.");
                    return poJSON;
                }
            }
        }

        return super.addDetail();
    }
    
    @Override //POQuotationRequestImpl
    public JSONObject AddPOQuotationRequestSupplier() throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        
        if (paSuppliers == null) {
            paSuppliers = new ArrayList<>();
        }

        if (paSuppliers.isEmpty()) {
            paSuppliers.add(POQuotationRequestSupplier());
            poJSON = paSuppliers.get(getPOQuotationRequestSupplierCount()- 1).newRecord();
        } else {
            if (paSuppliers.get(paSuppliers.size() - 1).getSupplierId() != null && !"".equals(paSuppliers.get(paSuppliers.size() - 1).getSupplierId())) {
                paSuppliers.add(POQuotationRequestSupplier());
                poJSON = paSuppliers.get(getPOQuotationRequestSupplierCount()- 1).newRecord();
            } else {
                poJSON.put("result", "error");
                poJSON.put("message", "Unable to add supplier.");
                return poJSON;
            }
        }

        poJSON.put("result", "success");
        return poJSON;

    }
    
    @Override //POQuotationRequestImpl
    public JSONObject removeDetails() {
        poJSON = new JSONObject();
        Iterator<Model> detail = Detail().iterator();
        while (detail.hasNext()) {
            Model item = detail.next();
            if (item.getEditMode() == EditMode.ADDNEW) {
                detail.remove();
            } else {
                paDetailRemoved.add(item);
                item.setValue("cReversex", POQuotationRequestStatus.Reverse.EXCLUDE);
            }
        }

        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }
    
    @Override //POQuotationRequestImpl
    public JSONObject removeWithInvDetails() {
        poJSON = new JSONObject();
        Iterator<Model> detail = Detail().iterator();
        while (detail.hasNext()) {
            Model item = detail.next();
            //Only remove the detail with stock id to not be conflict in category level 2 of transaction vs inventory information
            if(item.getValue("sStockIDx") != null && !"".equals(item.getValue("sStockIDx"))){
                if (item.getEditMode() == EditMode.ADDNEW) {
                    detail.remove();
                } else {
                    paDetailRemoved.add(item);
                    item.setValue("cReversex", POQuotationRequestStatus.Reverse.EXCLUDE);
                }
            }
        }

        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }

    private void resetMaster() throws SQLException, GuanzonException{
        poMaster = ObjectInitiator.createModel(Model_PO_Quotation_Request_Master.class, poGRider, Tables.PURCHASE_QUOTATION_REQUEST_MASTER);
    }
    
    private void resetOthers() {
        paSuppliers = new ArrayList<>();
        paDetailRemoved = new ArrayList<>();
        paSuppliersRemoved = new ArrayList<>();
        setSearchBranch("");
        setSearchCategory("");
        setSearchDepartment("");
    }
    
    @Override
    public String getSourceCode() {
        return SOURCE_CODE;
    }
    
    @Override
    public JSONObject willSave()
            throws SQLException,
            GuanzonException,
            CloneNotSupportedException {
        /*Put system validations and other assignments here*/
        poJSON = new JSONObject();
        
        //if current status is Return check changes
        if(POQuotationRequestStatus.RETURNED.equals(Master().getTransactionStatus())){
            boolean lbUpdated = false;
            POQuotationRequest loRecord = ObjectInitiator.createTransaction(POQuotationRequest.class, poGRider, true, logwrapr);
            loRecord.setVerifyEntryNo(true);
            
            loRecord.InitTransaction();
            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
            loRecord.OpenTransaction(Master().getTransactionNo());
            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
            
            lbUpdated = loRecord.getDetailCount() == (getDetailCount() - 1);
            if (lbUpdated) {
                lbUpdated = loRecord.Master().getReferenceNo().equals(Master().getReferenceNo());
            }
            if (lbUpdated) {
                lbUpdated = loRecord.Master().getCategoryLevel2().equals(Master().getCategoryLevel2());
            }
            if (lbUpdated) {
                lbUpdated = Objects.equals(loRecord.Master().getExpectedPurchaseDate(), Master().getExpectedPurchaseDate());
            }
            if (lbUpdated) {
                lbUpdated = loRecord.Master().getDestination().equals(Master().getDestination());
            }
            if (lbUpdated) {
                lbUpdated = loRecord.Master().getRemarks().equals(Master().getRemarks());
            }

            if (lbUpdated) {
                for (int lnCtr = 0; lnCtr <= loRecord.getDetailCount() - 1; lnCtr++) {
                    lbUpdated = loRecord.Detail(lnCtr).getStockId().equals(Detail(lnCtr).getStockId());
                    if (lbUpdated) {
                        lbUpdated = loRecord.Detail(lnCtr).getQuantity().doubleValue() == Detail(lnCtr).getQuantity().doubleValue();
                    } 
                    if (lbUpdated) {
                        lbUpdated = loRecord.Detail(lnCtr).getDescription().equals(Detail(lnCtr).getDescription());
                    }
                    if (!lbUpdated) {
                        break;
                    }
                }
            }

            if (lbUpdated) {
                poJSON.put("result", "error");
                poJSON.put("message", "No update has been made.");
                return poJSON;
            }
        
        }
        
        switch(Master().getTransactionStatus()){
            case POQuotationRequestStatus.CONFIRMED:
            case POQuotationRequestStatus.RETURNED:
                //seek for approval
                poJSON =  seekApproval();
                if (!"success".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }
            break;
        }
        
        if (paDetailRemoved == null) {
            paDetailRemoved = new ArrayList<>();
        }
        
        if(Master().getEditMode() == EditMode.ADDNEW){
            System.out.println("Will Save : " + Master().getNextCode());
            Master().setTransactionNo(Master().getNextCode());
            Master().setPrepared(poGRider.getUserID());
        }
        
        Master().setModifyingId(poGRider.Encrypt(poGRider.getUserID()));
        Master().setModifiedDate(poGRider.getServerDate());
        Master().setPreparedDate(poGRider.getServerDate()); //Re-updated prepared date when user edited the transaction conflict in null when updating transaction
       
        //Check detail
        boolean lbWillDelete = true;
        for(int lnCtr = 0; lnCtr <= getDetailCount()-1; lnCtr++){
            if ((Detail(lnCtr).getQuantity() > 0.00) && Detail(lnCtr).isReverse() ) {
                lbWillDelete = false;
            }
        }
        
        if(lbWillDelete){
            poJSON.put("result", "error");
            poJSON.put("message", "No transaction quantity to be save.");
            return poJSON;
        }
        
        //Check Supplier detail
        int lnRow = 0;
        for(int lnCtr = 0; lnCtr <= getPOQuotationRequestSupplierCount()-1; lnCtr++){
            if(POQuotationRequestSupplierList(lnCtr).isReverse()){
                lnRow++;
                if (POQuotationRequestSupplierList(lnCtr).getSupplierId() != null 
                        && !"".equals(POQuotationRequestSupplierList(lnCtr).getSupplierId())) {
                    
                    if(POQuotationRequestSupplierList(lnCtr).getTerm()== null 
                        || "".equals(POQuotationRequestSupplierList(lnCtr).getTerm())){
                        poJSON.put("result", "error");
                        poJSON.put("message", "Supplier term at row "+lnRow+" cannot be empty.");
                        return poJSON;
                    }
                    
                    if(POQuotationRequestSupplierList(lnCtr).getCompanyId() == null 
                        || "".equals(POQuotationRequestSupplierList(lnCtr).getCompanyId())){
                        poJSON.put("result", "error");
                        poJSON.put("message", "Supplier company at row "+lnRow+" cannot be empty.");
                        return poJSON;
                    }
                }
            }
        }
        
        String lsQuantity = "0.00";
        Iterator<Model> detail = Detail().iterator();
        while (detail.hasNext()) {
            Model item = detail.next();
            if(item.getValue("nQuantity") != null && !"".equals(item.getValue("nQuantity"))){
                lsQuantity = item.getValue("nQuantity").toString();
            }
            if (((item.getValue("sDescript") == null || "".equals(item.getValue("sDescript")))
                  &&  (item.getValue("sStockIDx") == null || "".equals(item.getValue("sStockIDx"))))
                  || (Double.valueOf(lsQuantity) <= 0.00)) {

                if (item.getEditMode() == EditMode.ADDNEW) {
                    detail.remove();
                } else {
                    paDetailRemoved.add(item);
                    item.setValue("cReversex", POQuotationRequestStatus.Reverse.EXCLUDE);
                }

            }
        }
        
        //remove supplier without details
        Iterator<Model_PO_Quotation_Request_Supplier> others = paSuppliers.iterator();
        while (others.hasNext()) {
            Model_PO_Quotation_Request_Supplier item = others.next();
            if (item.getSupplierId() == null || "".equals(item.getSupplierId())){
                others.remove();
            }
        }

        //Validate detail after removing all zero qty and empty stock Id
        if (getDetailCount() <= 0) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transaction detail to be save.");
            return poJSON;
        }

        //assign other info on detail
        for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
            Detail(lnCtr).setTransactionNo(Master().getTransactionNo());
            Detail(lnCtr).setEntryNo(lnCtr+1);
        }
        
        for (int lnCtr = 0; lnCtr <= getPOQuotationRequestSupplierCount()- 1; lnCtr++) {
            POQuotationRequestSupplierList(lnCtr).setTransactionNo(Master().getTransactionNo());
            POQuotationRequestSupplierList(lnCtr).setEntryNo(lnCtr+1);
        }
        
        poJSON.put("result", "success");
        return poJSON;
    }
    
    @Override
    public JSONObject save() {
        /*Put saving business rules here*/
        return isEntryOkay(POQuotationRequestStatus.OPEN);
    }
    
    @Override
    public void saveComplete() {
        /*This procedure was called when saving was complete*/
        System.out.println("Transaction saved successfully.");
    }
    
    @Override
    public JSONObject saveOthers() {
        /*Only modify this if there are other tables to modify except the master and detail tables*/
        poJSON = new JSONObject();
        try {
            //Save PO Quotation Request Supplier
            for (int lnCtr = 0; lnCtr <= getPOQuotationRequestSupplierCount()- 1; lnCtr++) {
                if (paSuppliers.get(lnCtr).getEditMode() == EditMode.ADDNEW || paSuppliers.get(lnCtr).getEditMode() == EditMode.UPDATE) {
                    paSuppliers.get(lnCtr).setModifiedDate(poGRider.getServerDate());
                    poJSON = paSuppliers.get(lnCtr).saveRecord();
                    if ("error".equals((String) poJSON.get("result"))) {
                        System.out.println("Save PO Quotation Request Supplier " + (String) poJSON.get("message"));
                        poJSON.put("result", "error");
                        poJSON.put("message", (String) poJSON.get("message"));
                        return poJSON;
                    }
                }
            }
        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
        }
        poJSON.put("result", "success");
        return poJSON;
    }
    

    @Override
    public JSONObject initFields() {
        try {
            /*Put initial model values here*/
            poJSON = new JSONObject();
            System.out.println("Dept ID : " + poGRider.getDepartment());
            System.out.println("Current User : " + poGRider.getUserID());
            
            Master().setBranchCode(poGRider.getBranchCode());
            Master().setIndustryId(psIndustryId);
            Master().setCategoryCode(psCategorCd);
            Master().setTransactionDate(poGRider.getServerDate());
            Master().setTransactionStatus(POQuotationRequestStatus.OPEN);
            
            //TODO Hardcode Default value is for sir jeff
            Master().setDepartmentId("033"); // Autoset Department of procurement
            Master().setDestination("M001"); //Autoset Destination of sir jeff
            
        } catch (SQLException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
            return poJSON;
        }

        poJSON.put("result", "success");
        return poJSON;
    }
    
    @Override
    protected JSONObject isEntryOkay(String status) {
        GValidator loValidator = POQuotationRequestValidatorFactory.make(Master().getIndustryId());

        loValidator.setApplicationDriver(poGRider);
        loValidator.setTransactionStatus(status);
        loValidator.setMaster(poMaster);
//        loValidator.setDetail(paDetail);

        poJSON = loValidator.validate();

        return poJSON;
    }

    @Override
    public void initSQL() {
        SQL_BROWSE =  " SELECT         "
                    + "    a.sTransNox "
                    + "  , a.sIndstCdx "
                    + "  , a.sBranchCd "
                    + "  , a.sDeptIDxx "
                    + "  , a.sCategrCd "
                    + "  , a.dTransact "
                    + "  , a.sCategCd2 "
                    + "  , a.sDestinat "
                    + "  , a.sReferNox "
                    + "  , a.cTranStat "
                    + "  , b.sDescript AS Industry   "
                    + "  , c.sBranchNm AS Branch     "
                    + "  , d.sDeptName AS Department "
                    + "  , e.sDescript AS Category   "
                    + "  , f.sDescript AS Category2  "
                    + " FROM po_quotation_request_master a "
                    + " LEFT JOIN industry b ON b.sIndstCdx = a.sIndstCdx        "
                    + " LEFT JOIN branch c ON c.sBranchCd = a.sBranchCd          "
                    + " LEFT JOIN department d ON d.sDeptIDxx = a.sDeptIDxx      "
                    + " LEFT JOIN category e ON e.sCategrCd = a.sCategrCd        "
                    + " LEFT JOIN category_level2 f ON f.sCategrCd = a.sCategCd2 " ;
        
    }
    
    /*EXPORT FILE*/
//    private JSONObject sentEmail(){
//        poJSON = new JSONObject();
//        
//        for(int lnCtr=0; lnCtr <= getDetailCount()-1;lnCtr++){
//            //Call sent email method
////            poJSON = sentEmail(lnCtr);
////            if ("success".equals((String) poJSON.get("result"))) {
//                //if success sending of email call exportFile
//                poJSON = exportFile(lnCtr);
//                if ("error".equals((String) poJSON.get("result"))) {
//                    return poJSON;
//                }   
////            }    
//            
//        }
//        
//        poJSON.put("result", "success");
//        return poJSON;
//    }
    
    @Override //POQuotationRequestImpl
    public JSONObject ExportFile(){
        poJSON = new JSONObject();
        
        if(getPOQuotationRequestSupplierCount() <= 0){
            poJSON.put("result", "error");
            poJSON.put("message", "No supplier to be export.");
            return poJSON;
        }
        //retreiving using column index
        System.out.println("-----------------------------------EXPORT-------------------------------------");
        for (int lnCtr = 0; lnCtr <= getPOQuotationRequestSupplierCount() - 1; lnCtr++) {
                if(POQuotationRequestSupplierList(lnCtr).isReverse()){
                System.out.println("Row No ->> " + lnCtr);
                poJSON = ExportFile(lnCtr);
                if ("error".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }   
            }
        }
        System.out.println("------------------------------------------------------------------------------");
        
        poJSON.put("result", "success");
        poJSON.put("message", "Export complete. \n\nCheck folder located in\""+System.getProperty("sys.default.path.temp")+"\"Export folder.");
        return poJSON;
    }
    
    @Override //POQuotationRequestImpl
    public JSONObject ExportFile(int POQuotationRequestSupplierRow){
        poJSON = new JSONObject();
        String lsExportPath = System.getProperty("sys.default.path.temp") + "/Export";
        String lsReportPath = System.getProperty("sys.default.path.config") + "/Reports/POQuotationRequest.jrxml";
        String lsWaterMarkPath = System.getProperty("sys.default.path.config") + "/Reports//images/approved.png";
        try {
            
            File folder = new File(lsExportPath);
            if (!folder.exists()) {
                if (folder.mkdirs()) {
                    // Folder successfully created
                    System.out.println("Folder created at: " + folder.getAbsolutePath());
                } else {
                    // Failed to create folder
                    poJSON.put("result", "error");
                    poJSON.put("message", "Failed to create folder. \n\nEnsure the application has write permissions to \"" + System.getProperty("sys.default.path.temp") + "\"Export");
                    return poJSON;
                }
            }
            
            // 1. Prepare parameters
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("sTransNox", Master().getTransactionNo());
            parameters.put("dTransDte", new java.sql.Date(Master().getTransactionDate().getTime()));
            parameters.put("sRemarks", Master().getRemarks());
            parameters.put("sSupplierNm", POQuotationRequestSupplierList(POQuotationRequestSupplierRow).Supplier().getCompanyName()); 
            parameters.put("sSupplierAddress", POQuotationRequestSupplierList(POQuotationRequestSupplierRow).Address().getAddress()); 
            parameters.put("sContactNo", POQuotationRequestSupplierList(POQuotationRequestSupplierRow).Contact().getMobileNo()); 
            
            parameters.put("sBranchNm", Master().Branch().getBranchName());
            parameters.put("sAddressx", Master().Branch().getAddress());
            parameters.put("sCompnyNm", POQuotationRequestSupplierList(POQuotationRequestSupplierRow).Company().getCompanyName());
            parameters.put("dDatexxx", new java.sql.Date(poGRider.getServerDate().getTime()));
            parameters.put("watermarkImagePath", lsWaterMarkPath);
            List<OrderDetail> orderDetails = new ArrayList<>();
            
            int lnRow = 1;
            String lsDescription = "";
            for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
                if(Detail(lnCtr).isReverse()){
                    lsDescription = Detail(lnCtr).getDescription();
                    orderDetails.add(new OrderDetail(lnRow, lsDescription, Detail(lnCtr).getQuantity()));
                    lnRow++;
                }
                lsDescription = "";
            }
            
            File file = new File(lsReportPath);
            if (file.exists()) {
            } else {
                poJSON.put("result", "error");
                poJSON.put("message", "Jasper file does not exist. \n\nEnsure the file is located in \""+System.getProperty("sys.default.path.config")+"\"Reports");
                return poJSON;
            }

            // 3. Create data source
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(orderDetails);

            // 4. Compile and fill report
            JasperReport jasperReport = JasperCompileManager.compileReport(lsReportPath);
            JasperPrint jasperPrint = JasperFillManager.fillReport(
                    jasperReport,
                    parameters,
                    dataSource
            );
            
            String lsExportFile = lsExportPath + "/" + Master().getTransactionNo() + " - "+  POQuotationRequestSupplierList(POQuotationRequestSupplierRow).Company().getCompanyName() + " " + POQuotationRequestSupplierList(POQuotationRequestSupplierRow).Supplier().getCompanyName() + ".pdf";
            JasperExportManager.exportReportToPdfFile(jasperPrint, lsExportFile);
            
        } catch (JRException e) {
            System.err.println("Error generating report: " + e.getMessage());
            e.printStackTrace();
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(e));
        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
        }
        
        return poJSON;
    }
    
    public static class OrderDetail {
        private Integer nRowNo;
        private String sDescription;
        private double nOrder;

        public OrderDetail(Integer rowNo, String description, double order) {
            this.nRowNo = rowNo;
            this.sDescription = description;
            this.nOrder = order;
        }

        public Integer getnRowNo() {
            return nRowNo;
        }

        public String getsDescription() {
            return sDescription;
        }

        public double getnOrder() {
            return nOrder;
        }
    }    
    
    /*Validate*/
    private JSONObject checkExistingDetail(int row, String stockId, String description, boolean isSearch){
        JSONObject loJSON = new JSONObject();
        loJSON.put("row", row);
        if(stockId == null){
            stockId = "";
        }
        if(description == null){
            description = "";
        }
        int lnRow = 0;
        for (int lnCtr = 0; lnCtr <= getDetailCount()- 1; lnCtr++) {
            if(Detail(lnCtr).isReverse()){
                lnRow++;
            }
            if (lnCtr != row) {
                //Check Existing Stock ID and Description
                if(!"".equals(stockId) || !"".equals(description)){
                    if((stockId.equals(Detail(lnCtr).getStockId())  && isSearch )
                            || (description.equals(Detail(lnCtr).getDescription()))){
                        if(Detail(lnCtr).isReverse()){
                            loJSON.put("result", "error");
                            loJSON.put("message", "Item Description already exists in the transaction detail at row "+lnRow+".");
                            loJSON.put("reverse", true);
                            loJSON.put("row", lnCtr);
                            return loJSON;
                        } else {
                            loJSON.put("result", "error");
                            loJSON.put("reverse", false);
                            loJSON.put("row", lnCtr);
                            return loJSON;
                        }
                    }
                }    
            }
        }
        
        loJSON.put("result", "success");
        loJSON.put("message", "success");
        return loJSON;
    }
    
    private JSONObject checkExistingSupplier(int row, String supplierId, String compnay){
        JSONObject loJSON = new JSONObject();
        int lnRow = 0;
        for(int lnCtr = 0; lnCtr <= getPOQuotationRequestSupplierCount()- 1; lnCtr++){
            if(POQuotationRequestSupplierList(lnCtr).isReverse()){
                lnRow++;
            }
            if (lnCtr != row) {
                System.out.println("Row : " + lnCtr);
                System.out.println("Supplier : " + POQuotationRequestSupplierList(lnCtr).getSupplierId());
                System.out.println("Company : " + POQuotationRequestSupplierList(lnCtr).getCompanyId());
                
                if (POQuotationRequestSupplierList(lnCtr).getSupplierId().equals(supplierId)
                        && POQuotationRequestSupplierList(lnCtr).getCompanyId().equals(compnay)
                        ) {
                    if(POQuotationRequestSupplierList(lnCtr).isReverse()){
                        loJSON.put("result", "error");
                        loJSON.put("reverse", true);
                        loJSON.put("message", "Supplier with the same company already exists in the table at row "+lnRow+".");
                        loJSON.put("row", lnCtr);
                        return loJSON;
                    } else {
                        loJSON.put("result", "error");
                        loJSON.put("reverse", false);
                        loJSON.put("row", lnCtr);
                        return loJSON;
                    }
                } 
            }
        }
        
        loJSON.put("result", "success");
        loJSON.put("message", "success");
        loJSON.put("row", row);
        return loJSON;
    }
    
    /**
     * Seek for Approval
     * @return JSON
     */
    private JSONObject seekApproval(){
        poJSON = new JSONObject();
        if (poGRider.getUserLevel() <= UserRight.ENCODER) {
            poJSON = ShowDialogFX.getUserApproval(poGRider);
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            } else {
                if(Integer.parseInt(poJSON.get("nUserLevl").toString())<= UserRight.ENCODER){
                    poJSON.put("result", "error");
                    poJSON.put("message", "User is not an authorized approving officer.");
                    return poJSON;
                }
            }
        }
        
        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }
    
//    private boolean checkExistStockInventory(){
//        for(int lnCtr = 0; lnCtr <= getDetailCount()-1;lnCtr++){
//            if(Detail(lnCtr).isReverse()){
//                if(Detail(lnCtr).getStockId() != null && !"".equals(Detail(lnCtr).getStockId())){
//                    return true;
//                }
//            }
//        }
//        return false;
//    }
    
//    public Model_PO_Quotation_Request_Detail getDetail() {
//        return (Model_PO_Quotation_Request_Detail) poDetail;
//    }
    
//    private Model_PO_Quotation_Request_Detail DetailRemoved(int row) {
//        return (Model_PO_Quotation_Request_Detail) paDetailRemoved.get(row);
//    }
    
//    private int getDetailRemovedCount() {
//        if (paDetailRemoved == null) {
//            paDetailRemoved = new ArrayList<>();
//        }
//
//        return paDetailRemoved.size();
//    }
    
//    private void removeDetail(Model_PO_Quotation_Request_Detail item) {
//        if (paDetailRemoved == null) {
//            paDetailRemoved = new ArrayList<>();
//        }
//        
//        paDetailRemoved.add(item);
//    }
}
