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
import org.guanzon.cas.parameter.Brand;
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
    String psRecdStat = Logical.YES;

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

        //change status
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
        poJSON = isEntryOkay(PurchaseOrderStatus.CONFIRMED);
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
        poJSON = isEntryOkay(PurchaseOrderStatus.CONFIRMED);
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
        poJSON = isEntryOkay(PurchaseOrderStatus.CONFIRMED);
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

    public JSONObject SearchBarcode(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Inventory object = new InvControllers(poGRider, logwrapr).Inventory();
        object.setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
            Detail(getDetailCount() - 1).setStockID(object.getModel().getStockId());
        }

        return poJSON;
    }

    public JSONObject SearchBarcodeDescription(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Inventory object = new InvControllers(poGRider, logwrapr).Inventory();
        object.setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
            Detail(getDetailCount() - 1).setStockID(object.getModel().getStockId());
        }

        return poJSON;
    }

    public JSONObject SearchSupplier(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Client object = new ClientControllers(poGRider, logwrapr).Client();
        object.Master().setClientType("0");

        poJSON = object.Master().searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
            Master().setSupplierID(object.Master().getModel().getClientId());
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

    public JSONObject SearchBrand(String value, boolean byCode, int row) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Brand object = new ParamControllers(poGRider, logwrapr).Brand();
        object.getModel().setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);
        if ("success".equals((String) poJSON.get("result"))) {
            Detail(row).Inventory().setBrandId(object.getModel().getBrandId());
        }

        return poJSON;
    }

    public JSONObject SearchModel(String value, boolean byCode, int row) throws SQLException, GuanzonException {
        Inventory object = new InvControllers(poGRider, logwrapr).Inventory();
        object.getModel().setRecordStatus(RecordStatus.ACTIVE);
        object.getModel().setBrandId(Detail(row).Inventory().getBrandId());

        poJSON = object.searchRecordOfVariants(value, byCode);
        if ("success".equals((String) poJSON.get("result"))) {
            Detail(row).setStockID(object.getModel().getStockId());
            Detail(row).setUnitPrice(object.getModel().getCost().doubleValue());
        }

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
    public JSONObject willSave() {
        /*Put system validations and other assignments here*/
        poJSON = new JSONObject();

        //remove items with no stockid or quantity order
        Iterator<Model> detail = Detail().iterator();
        while (detail.hasNext()) {
            Model item = detail.next(); // Store the item before checking conditions

            if ("".equals((String) item.getValue("sStockIDx"))
                    || (int) item.getValue("nQuantity") <= 0) {
                detail.remove(); // Correctly remove the item
            }
        }

        //assign other info on detail
        for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
            Detail(lnCtr).setTransactionNo(Master().getTransactionNo());
            Detail(lnCtr).setEntryNo(lnCtr + 1);
        }

        if (getDetailCount() == 1) {
            //do not allow a single item detail with no quantity order
            if (Detail(0).getQuantity().equals(0)) {
                poJSON.put("result", "error");
                poJSON.put("message", "Your order has zero quantity.");
                return poJSON;
            }
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
        String lsSQL = MiscUtil.addCondition(SQL_BROWSE, " a.sIndstCdx = " + SQLUtil.toSQL(poGRider.getIndustry()));

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
        String lsRecdStat = "";
        if (psTranStat.length() > 1) {
            for (int lnCtr = 0; lnCtr <= psTranStat.length() - 1; lnCtr++) {
                lsRecdStat += ", " + SQLUtil.toSQL(Character.toString(psTranStat.charAt(lnCtr)));
            }
            lsRecdStat = " AND a.cTranStat IN (" + lsRecdStat.substring(2) + ")";
        } else {
            lsRecdStat = " AND a.cTranStat = " + SQLUtil.toSQL(psTranStat);
        }
        String lsIndustryCondition = Master().getIndustryID().isEmpty() || Master().getIndustryID() == null
                ? "WHERE d.sIndstCdx LIKE '%' AND d.sIndstCdx = c.sIndstCdx)"
                : "WHERE d.sIndstCdx = " + SQLUtil.toSQL(Master().getIndustryID()) + " AND d.sIndstCdx = c.sIndstCdx)";
        String lsCompanyCondition = Master().getCompanyID().isEmpty() || Master().getCompanyID() == null
                ? "WHERE f.sCompnyID LIKE '%' AND f.sCompnyID = e.sCompnyID)"
                : "WHERE f.sCompnyID = " + SQLUtil.toSQL(Master().getIndustryID()) + " AND f.sCompnyID = e.sCompnyID)";

        String lsSQL = "SELECT"
                + "  a.sTransNox,"
                + "  a.sBranchCd,"
                + "  a.dTransact,"
                + "  a.sReferNox,"
                + "  a.cTranStat,"
                + "  e.sBranchNm,"
                + "  COUNT(DISTINCT b.sStockIDx) AS total_details"
                + " FROM inv_stock_request_master a"
                + " LEFT JOIN inv_stock_request_detail b ON a.sTransNox = b.sTransNox"
                + " LEFT JOIN inventory c ON b.sStockIDx = c.sStockIDx"
                + " LEFT JOIN branch e ON a.sBranchCd = e.sBranchCd";

        String condition = " EXISTS (SELECT 1 FROM industry d "
                + lsIndustryCondition
                + " AND EXISTS (SELECT 1 FROM company f "
                + lsCompanyCondition
                + " AND b.nApproved > 0";

        lsSQL = lsSQL + (MiscUtil.addCondition("", condition));
        if (!psRecdStat.isEmpty()) {
            lsSQL = lsSQL + lsRecdStat;
        }

        lsSQL = lsSQL + (" GROUP BY a.sTransNox, a.sBranchCd, a.dTransact, a.sReferNox, a.cTranStat, e.sBranchNm");
        lsSQL = lsSQL + (" ORDER BY a.dTransact DESC");
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

            while (loRS.next()) {
                JSONObject request = new JSONObject();
                request.put("sTransNox", loRS.getString("sTransNox"));
                request.put("sBranchCd", loRS.getString("sBranchCd"));
                request.put("dTransact", loRS.getDate("dTransact"));
                request.put("sReferNox", loRS.getString("sReferNox"));
                request.put("cTranStat", loRS.getString("cTranStat"));
                request.put("sBranchNm", loRS.getString("sBranchNm"));
                request.put("total_details", loRS.getInt("total_details"));

                dataArray.add(request);
                lnctr++;
            }

            if (lnctr > 0) {
                loJSON.put("result", "success");
                loJSON.put("message", "Record loaded successfully.");
                loJSON.put("data", dataArray);
            } else {
                loJSON.put("result", "error");
                loJSON.put("continue", true);
                loJSON.put("message", "No records found.");
            }
        } catch (SQLException e) {
            loJSON.put("result", "error");
            loJSON.put("message", e.getMessage());
        } finally {
            MiscUtil.close(loRS);
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
                Detail(lnLastIndex).setUnitPrice(Detail(lnCtr).Inventory().getCost().doubleValue());
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
        String lsRecdStat = "";
        if (psTranStat.length() > 1) {
            for (int lnCtr = 0; lnCtr <= psTranStat.length() - 1; lnCtr++) {
                lsRecdStat += ", " + SQLUtil.toSQL(Character.toString(psTranStat.charAt(lnCtr)));
            }
            lsRecdStat = " AND a.cTranStat IN (" + lsRecdStat.substring(2) + ")";
        } else {
            lsRecdStat = " AND a.cTranStat = " + SQLUtil.toSQL(psTranStat);
        }

        String lsIndustryCondition = Master().getIndustryID().isEmpty() || Master().getIndustryID() == null
                ? "a.sIndstCdx LIKE '%'"
                : "a.sIndstCdx = " + SQLUtil.toSQL(Master().getIndustryID());
        String lsSQL = " SELECT "
                + "  a.sTransNox,"
                + "  c.sBranchNm,"
                + "  a.sBranchCd,"
                + "  a.dTransact,"
                + "  a.sReferNox,"
                + "  a.cTranStat,"
                + " COUNT(b.sStockIDx) AS nNoItemsx"
                + " FROM po_master a "
                + " LEFT JOIN po_detail b ON a.sTransNox = b.sTransNox"
                + " LEFT JOIN branch c ON a.sBranchCd = c.sBranchCd"
                + " LEFT JOIN industry d ON a.sIndstCdx = d.sIndstCdx";

//            lsSQL = MiscUtil.addCondition(lsSQL, " a.sIndstCdx = " + SQLUtil.toSQL(Master().getIndustryID())
//                    + " AND a.sCompnyID = " + SQLUtil.toSQL(Master().getCompanyID())
//                    + " AND a.sSupplier = " + SQLUtil.toSQL(Master().getSupplierID())
//            );
        lsSQL = MiscUtil.addCondition(lsSQL, lsIndustryCondition);
        if (!psRecdStat.isEmpty()) {
            lsSQL = lsSQL + lsRecdStat;
        }
        lsSQL = lsSQL + " GROUP BY  a.sTransNox, a.sBranchCd, a.dTransact,a.sReferNox,a.cTranStat, c.sBranchNm "
                + "ORDER BY dTransact ASC";
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
                    Logger.getLogger(PurchaseOrder.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(PurchaseOrder.class.getName()).log(Level.SEVERE, null, ex);
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
