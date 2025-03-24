package org.guanzon.cas.purchasing.controller;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.view.JasperViewer;
import org.guanzon.appdriver.agent.ShowDialogFX;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.agent.services.Transaction;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.constant.Logical;
import org.guanzon.appdriver.constant.RecordStatus;
import org.guanzon.appdriver.iface.GValidator;
import org.guanzon.cas.client.Client;
import org.guanzon.cas.client.services.ClientControllers;
import org.guanzon.cas.inv.Inventory;
import org.guanzon.cas.inv.services.InvControllers;
import org.guanzon.cas.inv.warehouse.model.Model_Inv_Stock_Request_Master;
import org.guanzon.cas.inv.warehouse.services.InvWarehouseControllers;
import org.guanzon.cas.inv.warehouse.services.InvWarehouseModels;
import org.guanzon.cas.parameter.Branch;
import org.guanzon.cas.parameter.Company;
import org.guanzon.cas.parameter.Industry;
import org.guanzon.cas.parameter.Term;
import org.guanzon.cas.parameter.services.ParamControllers;
import org.guanzon.cas.purchasing.model.Model_PO_Detail;
import org.guanzon.cas.purchasing.model.Model_PO_Master;
import org.guanzon.cas.purchasing.services.PurchaseOrderModels;
import org.guanzon.cas.purchasing.status.PurchaseOrderStatus;
import org.guanzon.cas.purchasing.validator.PurchaseOrderValidatorFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

public class PurchaseOrder extends Transaction {

    List<Model_Inv_Stock_Request_Master> paStockRequest;
    List<Model_PO_Master> paPOMaster;

    public JSONObject InitTransaction() {
        SOURCE_CODE = "PO";
        poMaster = new PurchaseOrderModels(poGRider).PurchaseOrderMaster();
        poDetail = new PurchaseOrderModels(poGRider).PurchaseOrderDetails();
        paDetail = new ArrayList<>();

        return initialize();
    }

    public JSONObject NewTransaction() throws CloneNotSupportedException {
        return newTransaction();
    }

    public JSONObject SaveTransaction() throws SQLException, GuanzonException {
        return saveTransaction();
    }

    public JSONObject OpenTransaction(String transactionNo) throws CloneNotSupportedException, SQLException, GuanzonException {
        return openTransaction(transactionNo);
    }

    public JSONObject UpdateTransaction() {
        return updateTransaction();
    }

    public JSONObject ConfirmTransaction(String remarks) throws ParseException, SQLException, GuanzonException {
        poJSON = new JSONObject();

        String lsStatus = PurchaseOrderStatus.CONFIRMED;
        boolean lbConfirm = true;

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
        poJSON = isEntryOkay(PurchaseOrderStatus.CONFIRMED);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        poJSON = ShowDialogFX.getUserApproval(poGRider);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbConfirm);

        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON = new JSONObject();
        poJSON.put("result", "success");

        if (lbConfirm) {
            poJSON.put("message", "Transaction confirmed successfully.");
        } else {
            poJSON.put("message", "Transaction confirmation request submitted successfully.");
        }

        return poJSON;
    }

    public JSONObject PostTransaction(String remarks) throws ParseException, SQLException, GuanzonException {
        poJSON = new JSONObject();

        String lsStatus = PurchaseOrderStatus.APPROVED;
        boolean lbConfirm = true;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

        if (lsStatus.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already processed.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(PurchaseOrderStatus.APPROVED);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        poJSON = ShowDialogFX.getUserApproval(poGRider);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbConfirm);

        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON = new JSONObject();
        poJSON.put("result", "success");

        if (lbConfirm) {
            poJSON.put("message", "Transaction posted successfully.");
        } else {
            poJSON.put("message", "Transaction posting request submitted successfully.");
        }

        return poJSON;
    }

    public JSONObject CancelTransaction(String remarks) throws ParseException, SQLException, GuanzonException {
        poJSON = new JSONObject();

        String lsStatus = PurchaseOrderStatus.CANCELLED;
        boolean lbConfirm = true;

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
        poJSON = isEntryOkay(PurchaseOrderStatus.CANCELLED);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON = ShowDialogFX.getUserApproval(poGRider);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbConfirm);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON = new JSONObject();
        poJSON.put("result", "success");

        if (lbConfirm) {
            poJSON.put("message", "Transaction cancelled successfully.");
        } else {
            poJSON.put("message", "Transaction cancellation request submitted successfully.");
        }

        return poJSON;
    }

    public JSONObject VoidTransaction(String remarks) throws ParseException, SQLException, GuanzonException {
        poJSON = new JSONObject();

        String lsStatus = PurchaseOrderStatus.VOID;
        boolean lbConfirm = true;

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
        poJSON = isEntryOkay(PurchaseOrderStatus.VOID);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON = ShowDialogFX.getUserApproval(poGRider);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbConfirm);

        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON = new JSONObject();
        poJSON.put("result", "success");

        if (lbConfirm) {
            poJSON.put("message", "Transaction voided successfully.");
        } else {
            poJSON.put("message", "Transaction voiding request submitted successfully.");
        }

        return poJSON;
    }

    public JSONObject ReturnTransaction(String remarks) throws ParseException, SQLException, GuanzonException {
        poJSON = new JSONObject();

        String lsStatus = PurchaseOrderStatus.RETURNED;
        boolean lbReturn = true;

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
        poJSON = isEntryOkay(PurchaseOrderStatus.RETURNED);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON = ShowDialogFX.getUserApproval(poGRider);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbReturn);

        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON = new JSONObject();
        poJSON.put("result", "success");

        if (lbReturn) {
            poJSON.put("message", "Transaction returned successfully.");
        } else {
            poJSON.put("message", "Transaction returned request submitted successfully.");
        }

        return poJSON;
    }

    public JSONObject AddDetail() throws CloneNotSupportedException {
        JSONObject loJSON = new JSONObject();
        if (Detail(getDetailCount() - 1).getStockID().isEmpty()) {
            loJSON = new JSONObject();
            loJSON.put("result", "error");
            loJSON.put("message", "Last row has empty item.");
            return loJSON;
        }
        return addDetail();
    }

    /*Search Master References*/
    public JSONObject SearchBranch(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Branch object = new ParamControllers(poGRider, logwrapr).Branch();
        object.setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
            Master().setBranchCode(object.getModel().getBranchCode());
        }

        return poJSON;
    }

    public JSONObject SearchIndustry(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Industry object = new ParamControllers(poGRider, logwrapr).Industry();
        object.setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
            Master().setIndustryID(object.getModel().getIndustryId());
        }

        return poJSON;
    }

    public JSONObject SearchTerm(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Term object = new ParamControllers(poGRider, logwrapr).Term();
        object.setRecordStatus("1");

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
            Master().setTermCode(object.getModel().getTermId());
        }

        return poJSON;
    }

    public JSONObject SearchBarcode(String value, boolean byCode, int row)
            throws ExceptionInInitializerError, SQLException, GuanzonException, CloneNotSupportedException {

        Inventory object = new InvControllers(poGRider, logwrapr).Inventory();
        object.setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        String scannedStockID = object.getModel().getStockId();

        for (int lnCtr = 0; lnCtr < getDetailCount(); lnCtr++) {
            if (lnCtr != row) {
                String existingStockID = (String) Detail(lnCtr).getStockID();
                if (scannedStockID.equals(existingStockID)) {
                    poJSON.put("result", "error");
                    poJSON.put("message", "This barcode is already added in another row.");
                    return poJSON;
                }
            }
        }

        Detail(row).setStockID(scannedStockID);
        AddDetail();

        return poJSON;
    }

    public JSONObject SearchBarcodeDescription(String value, boolean byCode, int row) throws ExceptionInInitializerError, SQLException, GuanzonException, CloneNotSupportedException {
        Inventory object = new InvControllers(poGRider, logwrapr).Inventory();
        object.setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        String scannedStockID = object.getModel().getStockId();

        for (int lnCtr = 0; lnCtr < getDetailCount(); lnCtr++) {
            if (lnCtr != row) {
                String existingStockID = (String) Detail(lnCtr).getStockID();
                if (scannedStockID.equals(existingStockID)) {
                    poJSON.put("result", "error");
                    poJSON.put("message", "This barcode is already added in another row.");
                    return poJSON;
                }
            }
        }

        Detail(row).setStockID(scannedStockID);
        AddDetail();
        return poJSON;
    }

    public JSONObject SearchSupplier(String value, boolean byCode) throws SQLException, GuanzonException {
        Client object = new ClientControllers(poGRider, logwrapr).Client();
        object.Master().setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.Master().searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
            Master().setSupplierID(object.Master().getModel().getClientId());
            Master().setAddressID(object.ClientAddress().getModel().getAddressId()); //TODO
            Master().setContactID(object.ClientInstitutionContact().getModel().getClientId()); //TODO
        }

        return poJSON;
    }

    public JSONObject SearchCompany(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Company object = new ParamControllers(poGRider, logwrapr).Company();
        object.setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
            Master().setCompanyID(object.getModel().getCompanyId());
        }

        return poJSON;
    }

    public JSONObject SearchDestination(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Branch object = new ParamControllers(poGRider, logwrapr).Branch();
        object.setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
            Master().setDestinationID(object.getModel().getBranchCode());
        }

        return poJSON;
    }

    public JSONObject SearchBrand(String value, boolean byCode, int row) throws ExceptionInInitializerError, SQLException, GuanzonException, CloneNotSupportedException {
        Inventory object = new InvControllers(poGRider, logwrapr).Inventory();
        object.getModel().setRecordStatus(RecordStatus.ACTIVE);
        object.getModel().setBrandId(Detail(row).Inventory().getBrandId());

        poJSON = object.searchRecord(value, byCode);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        String scannedStockID = object.getModel().getStockId();

        for (int lnCtr = 0; lnCtr < getDetailCount(); lnCtr++) {
            if (lnCtr != row) {
                String existingStockID = (String) Detail(lnCtr).getStockID();
                if (scannedStockID.equals(existingStockID)) {
                    poJSON.put("result", "error");
                    poJSON.put("message", "This barcode is already added in another row.");
                    return poJSON;
                }
            }
        }
        Detail(row).setStockID(scannedStockID);
        Detail(row).Inventory().setBrandId(object.getModel().getBrandId());
        Detail(row).setUnitPrice(object.getModel().getCost().doubleValue());
        AddDetail();
        return poJSON;
    }

    public JSONObject SearchModel(String value, boolean byCode, int row) throws SQLException, GuanzonException, CloneNotSupportedException {
        Inventory object = new InvControllers(poGRider, logwrapr).Inventory();
        object.getModel().setRecordStatus(RecordStatus.ACTIVE);
        object.getModel().setBrandId(Detail(row).Inventory().getBrandId());

        poJSON = object.searchRecord(value, byCode);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        String scannedStockID = object.getModel().getStockId();

        for (int lnCtr = 0; lnCtr < getDetailCount(); lnCtr++) {
            if (lnCtr != row) {
                String existingStockID = (String) Detail(lnCtr).getStockID();
                if (scannedStockID.equals(existingStockID)) {
                    poJSON.put("result", "error");
                    poJSON.put("message", "This barcode is already added in another row.");
                    return poJSON;
                }
            }
        }
        Detail(row).setStockID(scannedStockID);
        AddDetail();
        return poJSON;
    }


    /*End - Search Master References*/
    @Override
    public String getSourceCode() {
        return SOURCE_CODE;
    }

    @Override
    public Model_PO_Master Master() {
        return (Model_PO_Master) poMaster;
    }

    @Override
    public Model_PO_Detail Detail(int row) {
        return (Model_PO_Detail) paDetail.get(row);
    }

    @Override
    public JSONObject willSave() throws SQLException {
        /*Put system validations and other assignments here*/
        poJSON = new JSONObject();

        if (Master().getTransactionStatus().equals(PurchaseOrderStatus.RETURNED)) {
            Master().setTransactionStatus(PurchaseOrderStatus.OPEN); //If edited update trasaction status into open
        }

        if (getDetailCount() == 1) {
            //do not allow a single item detail with no quantity order
            if (Detail(0).getQuantity().intValue() == 0) {
                poJSON.put("result", "error");
                poJSON.put("message", "Your order has zero quantity.");
                return poJSON;
            }
        }

        Iterator<Model> detail = Detail().iterator();
        while (detail.hasNext()) {
            Model item = detail.next();

            if ("".equals((String) item.getValue("sStockIDx"))
                    || (int) item.getValue("nQuantity") <= 0) {
                detail.remove();
            }
        }

        if (getDetailCount() <= 0) {
            poJSON.put("result", "error");
            poJSON.put("message", "No Purchase order detail to be save.");
            return poJSON;
        }

        //assign other info on detail
        for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
            Detail(lnCtr).setTransactionNo(Master().getTransactionNo());
            Detail(lnCtr).setEntryNo(lnCtr + 1);
            Detail(lnCtr).setModifiedDate(poGRider.getServerDate());
        }

        poJSON.put("result", "success");
        return poJSON;
    }

    @Override
    public JSONObject save() {
        /*Put saving business rules here*/
        return isEntryOkay(PurchaseOrderStatus.OPEN);
    }

    @Override
    public JSONObject saveOthers() {
        /*Only modify this if there are other tables to modify except the master and detail tables*/
        poJSON = new JSONObject();

        poJSON.put("result", "success");
        return poJSON;
    }

    @Override
    public void saveComplete() {
        /*This procedure was called when saving was complete*/
        System.out.println("Transaction saved successfully.");
    }

    @Override
    public JSONObject initFields() {
        /*Put initial model values here*/
        poJSON = new JSONObject();

        poJSON.put("result", "success");
        return poJSON;
    }

    @Override
    public void initSQL() {
        SQL_BROWSE = "SELECT "
                + "  a.sTransNox,"
                + "  a.dTransact,"
                + "  b.sDescript,"
                + "  c.sCompnyNm,"
                + "  e.sCompnyNm "
                + " FROM po_master a "
                + "LEFT JOIN Industry b ON a.sIndstCdx = b.sIndstCdx "
                + "LEFT JOIN company c ON c.sCompnyID = a.sCompnyID "
                + "LEFT JOIN inv_supplier d ON a.sSupplier = d.sSupplier "
                + "LEFT JOIN client_master e ON d.sSupplier = e.sClientID";

    }

    @Override
    protected JSONObject isEntryOkay(String status) {
        GValidator loValidator = PurchaseOrderValidatorFactory.make(Master().getIndustryID());

        loValidator.setApplicationDriver(poGRider);
        loValidator.setTransactionStatus(status);
        loValidator.setMaster(Master());

        poJSON = loValidator.validate();

        return poJSON;
    }

    public JSONObject searchTransaction(String fsValue) throws CloneNotSupportedException, SQLException, GuanzonException {
        poJSON = new JSONObject();
        String lsCondition = "";
        if (psTranStat.length() > 1) {
            for (int lnCtr = 0; lnCtr <= psTranStat.length() - 1; lnCtr++) {
                lsCondition += ", " + SQLUtil.toSQL(Character.toString(psTranStat.charAt(lnCtr)));
            }
            lsCondition = "cTranStat IN (" + lsCondition.substring(2) + ")";
        } else {
            lsCondition = "cTranStat = " + SQLUtil.toSQL(psTranStat);
        }
        initSQL();
        String lsIndustryCondition = !Master().getIndustryID().isEmpty()
                ? " a.sIndstCdx = " + SQLUtil.toSQL(Master().getIndustryID())
                : " a.sIndstCdx LIKE '%'";
        String lsCompanyCondition = !Master().getCompanyID().isEmpty()
                ? " a.sCompnyID = " + SQLUtil.toSQL(Master().getCompanyID())
                : " a.sCompnyID LIKE '%'";
        String lsSupplier = !Master().getSupplierID().isEmpty()
                ? " a.sSupplier = " + SQLUtil.toSQL(Master().getSupplierID())
                : " a.sSupplier LIKE '%'";
        String lsReferNo = !Master().getReference().isEmpty()
                ? " a.sReferNox = " + SQLUtil.toSQL(Master().getReference())
                : " a.sReferNox LIKE '%'";
        String lsFilterCondition = lsIndustryCondition
                + " AND "
                + lsCompanyCondition
                + " AND "
                + lsSupplier
                + " AND "
                + lsReferNo;
        String lsSQL = MiscUtil.addCondition(SQL_BROWSE, lsFilterCondition);
        System.out.println("SQLl: " + lsSQL);
        poJSON = ShowDialogFX.Browse(poGRider,
                lsSQL,
                fsValue,
                "Transaction Date»Transaction No»Company»Supplier",
                "dTransact»sTransNox»c.sCompnyNm»e.sCompnyNm",
                "dTransact»sTransNox»c.sCompnyNm»e.sCompnyNm",
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

    private Model_Inv_Stock_Request_Master InvStockRequestList() {
        return new InvWarehouseModels(poGRider).InventoryStockRequestMaster();
    }

    public Model_Inv_Stock_Request_Master InvStockRequestMaster(int row) {
        return (Model_Inv_Stock_Request_Master) paStockRequest.get(row);
    }

    public int getInvStockRequestCount() {
        return this.paStockRequest.size();
    }

    public JSONObject getApprovedStockRequests() throws SQLException, GuanzonException {
        String lsTransStat = "";
        if (psTranStat.length() > 1) {
            for (int lnCtr = 0; lnCtr <= psTranStat.length() - 1; lnCtr++) {
                lsTransStat += ", " + SQLUtil.toSQL(Character.toString(psTranStat.charAt(lnCtr)));
            }
            lsTransStat = " AND a.cTranStat IN (" + lsTransStat.substring(2) + ")";
        } else {
            lsTransStat = " AND a.cTranStat = " + SQLUtil.toSQL(psTranStat);
        }
        String lsIndustryCondition = Master().getIndustryID().isEmpty()
                ? "a.sIndstCdx LIKE '%'"
                : "a.sIndstCdx = " + SQLUtil.toSQL(Master().getIndustryID());
        String lsCompanyCondition = Master().getCompanyID().isEmpty()
                ? "e.sCompnyID LIKE '%'"
                : "e.sCompnyID = " + SQLUtil.toSQL(Master().getIndustryID());

        String lsFilterCondition = lsIndustryCondition
                + " AND "
                + lsCompanyCondition
                + " AND b.nApproved > 0 ";
        String lsSQL = "SELECT"
                + "  a.sTransNox,"
                + "  e.sBranchNm,"
                + "  a.sBranchCd,"
                + "  a.cTranStat,"
                + "  a.dTransact,"
                + "  a.sReferNox,"
                + "  a.cTranStat,"
                + "  a.sIndstCdx,"
                + "  COUNT(DISTINCT b.sStockIDx) AS total_details"
                + " FROM inv_stock_request_master a"
                + " LEFT JOIN inv_stock_request_detail b ON a.sTransNox = b.sTransNox"
                + " LEFT JOIN inventory c ON b.sStockIDx = c.sStockIDx"
                + " LEFT JOIN branch e ON a.sBranchCd = e.sBranchCd"
                + " LEFT JOIN industry f ON a.sIndstCdx = f.sIndstCdx";

        lsSQL = MiscUtil.addCondition(lsSQL, lsFilterCondition);
        if (!psTranStat.isEmpty()) {
            lsSQL = lsSQL + lsTransStat;
        }

        lsSQL = lsSQL + " GROUP BY a.sTransNox, a.sBranchCd, a.dTransact, a.sReferNox, a.cTranStat, e.sBranchNm"
                + " ORDER BY a.dTransact DESC";
        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        JSONArray dataArray = new JSONArray();
        JSONObject loJSON = new JSONObject();

        if (loRS == null) {
            loJSON.put("result", "error");
            loJSON.put("message", "Query execution failed.");
            return loJSON;
        }

        try {
            int lnctr = 0;
            if (MiscUtil.RecordCount(loRS) >= 0) {
                while (loRS.next()) {
                    JSONObject request = new JSONObject();
                    request.put("sTransNox", loRS.getString("a.sTransNox"));
                    request.put("sBranchCd", loRS.getString("a.sBranchCd"));
                    request.put("dTransact", loRS.getDate("a.dTransact"));
                    request.put("sReferNox", loRS.getString("a.sReferNox"));
                    request.put("cTranStat", loRS.getString("a.cTranStat"));
                    request.put("sBranchNm", loRS.getString("e.sBranchNm"));
                    request.put("total_details", loRS.getInt("total_details"));

                    dataArray.add(request);
                    lnctr++;
                }
                loJSON.put("result", "success");
                loJSON.put("message", "Record loaded successfully.");
                loJSON.put("data", dataArray);
            } else {
                dataArray = new JSONArray();
                dataArray.add("{}");
                loJSON.put("result", "error");
                loJSON.put("continue", true);
                loJSON.put("message", "No record found .");
            }
            MiscUtil.close(loRS);
        } catch (SQLException e) {
            loJSON.put("result", "error");
            loJSON.put("message", e.getMessage());
        }
        return loJSON;
    }

    public JSONObject addStockRequestOrdersToPODetail(String transactionNo) throws CloneNotSupportedException, SQLException, GuanzonException {
        JSONObject loJSON = new JSONObject();
        InvWarehouseControllers loTrans = new InvWarehouseControllers(poGRider, logwrapr);

        loJSON = loTrans.StockRequest().InitTransaction();
        if (!"success".equals(loJSON.get("result"))) {
            loJSON.put("result", "error");
            loJSON.put("message", "Failed to initialize transaction.");
            return loJSON;
        }

        poJSON = loTrans.StockRequest().OpenTransaction(transactionNo);
        if (!"success".equals(poJSON.get("result"))) {
            loJSON.put("result", "error");
            loJSON.put("message", "No records found.");
            return loJSON;
        }

        for (int lnCtr = 0; lnCtr <= loTrans.StockRequest().getDetailCount() - 1; lnCtr++) {
            if (loTrans.StockRequest().Detail(lnCtr).getApproved() - (loTrans.StockRequest().Detail(lnCtr).getIssued() + loTrans.StockRequest().Detail(lnCtr).getPurchase()) > 0) {
                AddDetail();
                int lnLastIndex = getDetailCount() - 1;
                Detail(lnLastIndex).setSouceNo(loTrans.StockRequest().Detail(lnCtr).getTransactionNo());
                Detail(lnLastIndex).setTransactionNo(loTrans.StockRequest().Detail(lnCtr).getTransactionNo());
                Detail(lnLastIndex).setEntryNo(lnLastIndex + 1);
                Detail(lnLastIndex).setStockID(loTrans.StockRequest().Detail(lnCtr).getStockId());
                Detail(lnLastIndex).setRecordOrder(0);
                Detail(lnLastIndex).setUnitPrice(loTrans.StockRequest().Detail(lnCtr).Inventory().getCost().doubleValue());
                Detail(lnLastIndex).setQuantity(0);
                Detail(lnLastIndex).setReceivedQuantity(loTrans.StockRequest().Detail(lnCtr).getReceived());
                Detail(lnLastIndex).setCancelledQuantity(loTrans.StockRequest().Detail(lnCtr).getCancelled());
                Detail(lnLastIndex).setSouceCode(SOURCE_CODE);
            }
        }

        loJSON.put("result", "success");
        loJSON.put("message", "Record loaded successfully.");
        AddDetail();
        return loJSON;
    }

    private Model_PO_Master POMasterList() {
        return new PurchaseOrderModels(poGRider).PurchaseOrderMaster();
    }

    public Model_PO_Master POMaster(int row) {
        return (Model_PO_Master) paPOMaster.get(row);
    }

    public int getPOMasterCount() {
        return this.paPOMaster.size();
    }

    public JSONObject getPurchaseOrder() throws SQLException, GuanzonException {
        JSONObject loJSON = new JSONObject();
        String lsTransStat = "";
        if (psTranStat.length() > 1) {
            for (int lnCtr = 0; lnCtr <= psTranStat.length() - 1; lnCtr++) {
                lsTransStat += ", " + SQLUtil.toSQL(Character.toString(psTranStat.charAt(lnCtr)));
            }
            lsTransStat = " AND a.cTranStat IN (" + lsTransStat.substring(2) + ")";
        } else {
            lsTransStat = " AND a.cTranStat = " + SQLUtil.toSQL(psTranStat);
        }

        String lsSQL = " SELECT "
                + "  a.sTransNox,"
                + "  c.sBranchNm,"
                + "  a.sBranchCd,"
                + "  a.dTransact,"
                + "  a.sReferNox,"
                + "  a.cTranStat"
                + " FROM po_master a "
                + " LEFT JOIN branch c ON a.sBranchCd = c.sBranchCd"
                + " LEFT JOIN industry d ON a.sIndstCdx = d.sIndstCdx"
                + " , po_detail b ";

        String lsIndustryCondition = !Master().getIndustryID().isEmpty()
                ? " a.sIndstCdx = " + SQLUtil.toSQL(Master().getIndustryID())
                : " a.sIndstCdx LIKE '%'";
        String lsCompanyCondition = !Master().getCompanyID().isEmpty()
                ? " a.sCompnyID = " + SQLUtil.toSQL(Master().getCompanyID())
                : " a.sCompnyID LIKE '%'";
        String lsSupplier = !Master().getSupplierID().isEmpty()
                ? " a.sSupplier = " + SQLUtil.toSQL(Master().getSupplierID())
                : " a.sSupplier LIKE '%'";
        String lsReferNo = !Master().getReference().isEmpty()
                ? " a.sReferNox = " + SQLUtil.toSQL(Master().getReference())
                : " a.sReferNox LIKE '%'";
        String lsFilterCondition = lsIndustryCondition
                + " AND "
                + lsCompanyCondition
                + " AND "
                + lsSupplier
                + " AND "
                + lsReferNo;
        lsSQL = MiscUtil.addCondition(lsSQL, lsFilterCondition);
        if (!psTranStat.isEmpty()) {
            lsSQL = lsSQL + lsTransStat;
        }
        lsSQL = lsSQL + " GROUP BY  a.sTransNox"
                + " ORDER BY dTransact ASC";
        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);

        int lnctr = 0;

        if (MiscUtil.RecordCount(loRS) >= 0) {
            paPOMaster = new ArrayList<>();
            while (loRS.next()) {
                // Print the result set
                System.out.println("sTransNox: " + loRS.getString("sTransNox"));
                System.out.println("dTransact: " + loRS.getDate("dTransact"));
                System.out.println("------------------------------------------------------------------------------");

                paPOMaster.add(POMasterList());
                paPOMaster.get(paPOMaster.size() - 1).openRecord(loRS.getString("sTransNox"));
                lnctr++;
            }

            System.out.println("Records found: " + lnctr);
            loJSON.put("result", "success");
            loJSON.put("message", "Record loaded successfully.");

        } else {
            paPOMaster = new ArrayList<>();
            paPOMaster.add(POMasterList());
            loJSON.put("result", "error");
            loJSON.put("continue", true);
            loJSON.put("message", "No record found .");
        }
        MiscUtil.close(loRS);

        return loJSON;
    }

    public JSONObject PrintTransaction() throws SQLException, CloneNotSupportedException, GuanzonException {
        poJSON = new JSONObject();
        boolean lbPrint = true;
        poJSON = printTransaction();
        if ("success".equals((String) poJSON.get("result"))) {
            if (((String) poMaster.getValue("cTranStat")).equals(PurchaseOrderStatus.APPROVED)) {
                poJSON = OpenTransaction((String) poMaster.getValue("sTransNox"));
                if ("error".equals((String) poJSON.get("result"))) {
                    poJSON.put("message", (String) poJSON.get("message"));
                    lbPrint = false;
                }
                poJSON = UpdateTransaction();
                if ("error".equals((String) poJSON.get("result"))) {
                    poJSON.put("message", (String) poJSON.get("message"));
                    lbPrint = false;
                }
                poMaster.setValue("dModified", poGRider.getServerDate());
                poMaster.setValue("sModified", poGRider.getUserID());

                poJSON = SaveTransaction();
                if ("error".equals((String) poJSON.get("result"))) {
                    poJSON.put("message", (String) poJSON.get("message"));
                    lbPrint = false;
                }
            }
        } else {
            lbPrint = false;
        }

        if (lbPrint) {
            poJSON.put("result", "success");
            poJSON.put("message", "Transaction printed successfully.");
        } else {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction printed aborted.");
        }

        return poJSON;
    }

    public JSONObject printTransaction() {
        poJSON = new JSONObject();
        String watermarkPath = "D:\\GGC_Maven_Systems\\Reports\\images\\draft.png"; //set draft as default
        try {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("sBranchNm", poGRider.getBranchName());
            parameters.put("sAddressx", poGRider.getAddress());
            parameters.put("sCompnyNm", poGRider.getClientName());
            parameters.put("sTransNox", Master().getTransactionNo());
            parameters.put("sApprval1", "John Doe");
            parameters.put("sApprval2", "Lu Cifer");
            parameters.put("sApprval3", "Le Min Hoo");
            parameters.put("sRemarks", Master().getRemarks());
            parameters.put("dTransDte", new java.sql.Date(Master().getTransactionDate().getTime()));
            parameters.put("dDatexxx", new java.sql.Date(poGRider.getServerDate().getTime()));

            if (Master().getTransactionStatus().equals(PurchaseOrderStatus.APPROVED)) {
                watermarkPath = "D:\\GGC_Maven_Systems\\Reports\\images\\approved.png";
            }
            parameters.put("watermarkImagePath", watermarkPath);
            List<OrderDetail> orderDetails = new ArrayList<>();

            double lnTotal = 0.0;
            for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
                lnTotal = Detail(lnCtr).getUnitPrice().doubleValue() * Detail(lnCtr).getQuantity().intValue();
                try {
                    orderDetails.add(new OrderDetail(lnCtr,
                            String.valueOf(Detail(lnCtr).getSouceNo()),
                            Detail(lnCtr).Inventory().getBarCode(),
                            Detail(lnCtr).Inventory().getDescription(),
                            Detail(lnCtr).getUnitPrice().doubleValue(),
                            Detail(lnCtr).getQuantity().intValue(),
                            lnTotal));

                } catch (GuanzonException ex) {
                    Logger.getLogger(PurchaseOrder.class
                            .getName()).log(Level.SEVERE, null, ex);
                }
            }

            // 3. Create data source
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(orderDetails);

            // 4. Compile and fill report
            String jrxmlPath = "D:\\GGC_Maven_Systems\\Reports\\PurchaseOrder.jrxml"; //TODO
            JasperReport jasperReport = JasperCompileManager.compileReport(jrxmlPath);
            JasperPrint jasperPrint = JasperFillManager.fillReport(
                    jasperReport,
                    parameters,
                    dataSource
            );
            JasperViewer viewer = new JasperViewer(jasperPrint, false);
            viewer.setVisible(true);

        } catch (JRException e) {
            System.err.println("Error generating report: " + e.getMessage());
            e.printStackTrace();

        } catch (SQLException ex) {
            Logger.getLogger(PurchaseOrder.class
                    .getName()).log(Level.SEVERE, null, ex);
        }

        return poJSON;

    }

    public static class OrderDetail {

        private Integer nRowNo;
        private String sOrderNo;
        private String sBarcode;
        private String sDescription;
        private double nUprice;
        private Integer nOrder;
        private double nTotal;

        public OrderDetail(Integer rowNo, String orderNo, String barcode, String description,
                double uprice, Integer order, double total) {
            this.nRowNo = rowNo;
            this.sOrderNo = orderNo;
            this.sBarcode = barcode;
            this.sDescription = description;
            this.nUprice = uprice;
            this.nOrder = order;
            this.nTotal = total;
        }

        public Integer getnRowNo() {
            return nRowNo;
        }

        public String getsOrderNo() {
            return sOrderNo;
        }

        public String getsBarcode() {
            return sBarcode;
        }

        public String getsDescription() {
            return sDescription;
        }

        public double getnUprice() {
            return nUprice;
        }

        public Integer getnOrder() {
            return nOrder;
        }

        public double getnTotal() {
            return nTotal;
        }
    }

}
