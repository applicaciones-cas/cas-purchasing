package org.guanzon.cas.purchasing.controller;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.agent.services.Transaction;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.iface.GValidator;
import org.guanzon.cas.client.Client;
import org.guanzon.cas.client.services.ClientControllers;
import org.guanzon.cas.inv.Inventory;
import org.guanzon.cas.inv.services.InvControllers;
import org.guanzon.cas.inv.warehouse.StockRequest;
import org.guanzon.cas.inv.warehouse.model.Model_Inv_Stock_Request_Master;
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

    List<Model_Inv_Stock_Request_Master> poInvStockRequestMaster;

    public JSONObject InitTransaction() {
        SOURCE_CODE = "InvR";

        poMaster = new PurchaseOrderModels(poGRider).PurchaseOrderMaster();
        poDetail = new PurchaseOrderModels(poGRider).PurchaseOrderDetails();
        paDetail = new ArrayList<>();

        poInvStockRequestMaster = new ArrayList<>();

        return initialize();
    }

    public JSONObject NewTransaction() throws CloneNotSupportedException {
        return newTransaction();
    }

    public JSONObject SaveTransaction() throws SQLException {
        return saveTransaction();
    }

    public JSONObject OpenTransaction(String transactionNo) throws CloneNotSupportedException {
        return openTransaction(transactionNo);
    }

    public JSONObject UpdateTransaction() {
        return updateTransaction();
    }

    public JSONObject ConfirmTransaction(String remarks) throws ParseException {
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

    public JSONObject PostTransaction(String remarks) throws ParseException {
        poJSON = new JSONObject();

        String lsStatus = PurchaseOrderStatus.PROCESSED;
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

    public JSONObject CancelTransaction(String remarks) throws ParseException {
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

    public JSONObject VoidTransaction(String remarks) throws ParseException {
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

        if (Detail(getDetailCount() - 1).getStockID().isEmpty()) {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "Last row has empty item.");
            return poJSON;
        }

        return addDetail();
    }

    /*Search Master References*/
    public JSONObject SearchBranch(String value, boolean byCode) throws ExceptionInInitializerError {
        Branch object = new ParamControllers(poGRider, logwrapr).Branch();
        object.setRecordStatus("1");

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
            Master().setBranchCode(object.getModel().getBranchCode());
        }

        return poJSON;
    }

    public JSONObject SearchIndustry(String value, boolean byCode) throws ExceptionInInitializerError {
        Industry object = new ParamControllers(poGRider, logwrapr).Industry();
        object.setRecordStatus("1");

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
            Master().setIndustryID(object.getModel().getIndustryId());
        }

        return poJSON;
    }

    public JSONObject SearchTerm(String value, boolean byCode) throws ExceptionInInitializerError {
        Term object = new ParamControllers(poGRider, logwrapr).Term();
        object.setRecordStatus("1");

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
            Master().setTermCode(object.getModel().getTermCode());
        }

        return poJSON;
    }

    public JSONObject SearchBarcode(String value, boolean byCode) throws ExceptionInInitializerError {
        Inventory object = new InvControllers(poGRider, logwrapr).Inventory();
        object.setRecordStatus("1");

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
            Detail(getDetailCount() - 1).setStockID(object.getModel().getStockId());
        }

        return poJSON;
    }

    public JSONObject SearchBarcodeDescription(String value, boolean byCode) throws ExceptionInInitializerError {
        Inventory object = new InvControllers(poGRider, logwrapr).Inventory();
        object.setRecordStatus("1");

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
            Detail(getDetailCount() - 1).setStockID(object.getModel().getStockId());
        }

        return poJSON;
    }

    public JSONObject SearchSupplier(String value, boolean byCode) throws ExceptionInInitializerError {
        Client object = new ClientControllers(poGRider, logwrapr).Client();
        object.Master().setClientType("0");

        poJSON = object.Master().searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
            Master().setSupplierID(object.Master().getModel().getClientId());
        }

        return poJSON;
    }

    public JSONObject SearchCompany(String value, boolean byCode) throws ExceptionInInitializerError {
        Company object = new ParamControllers(poGRider, logwrapr).Company();
        object.setRecordStatus("1");

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
            Master().setCompanyID(object.getModel().getCompanyId());
        }

        return poJSON;
    }

    public JSONObject SearchDestination(String value, boolean byCode) throws ExceptionInInitializerError {
        Branch object = new ParamControllers(poGRider, logwrapr).Branch();
        object.setRecordStatus("1");

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
            Master().setDestinationID(object.getModel().getBranchCode());
        }

        return poJSON;
    }

//    public JSONObject SearchReference(String value, boolean byCode) throws ExceptionInInitializerError {
//        PurchaseOrder object = new PurchaseOrderControllers(poGRider, logwrapr).PurchaseOrder();
//        object.Master().setTransactionStatus("1");
//
//        poJSON = object.Master().searchRecord(value, byCode);
//
//        if ("success".equals((String) poJSON.get("result"))) {
//            Master().setDestinationID(object.getModel().getBranchCode());
//        }
//
//        return poJSON;
//    }

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
            if (Detail(0).getQuantityOnHand() == 0) {
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
        SQL_BROWSE = "";
    }

    @Override
    protected JSONObject isEntryOkay(String status) {
        GValidator loValidator = PurchaseOrderValidatorFactory.make(Master().getIndustryID());

        loValidator.setApplicationDriver(poGRider);
        loValidator.setTransactionStatus(status);

        poJSON = loValidator.validate();

        return poJSON;
    }

    public ResultSet getApprovedStockRequestss() {
        String lsSQL = SQL_BROWSE;
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        try {
            if (loRS != null && loRS.next()) {
                loRS.beforeFirst(); // Move cursor back to the first row for iteration
                return loRS;
            } else {
                return null;
            }
        } catch (SQLException e) {
            logwrapr.severe(e.getMessage());
            return null;
        }
    }

    public JSONObject getApprovedStockRequests() {
        String lsSQL = 
                 "SELECT" 
                + "  sTransNox,"
                + "  sBranchCd,"
                + "  sIndstCdx,"
                + "  sCategrCd,"
                + "  dTransact,"
                + "  sReferNox,"
                + "  sRemarksx,"
                + "  sIssNotes,"
                + "  nCurrInvx,"
                + "  nEstInvxx,"
                + "  nEntryNox,"
                + "  sSourceCd,"
                + "  sSourceNo,"
                + "  cTranStat,"
                + "  sModified,"
                + "  dModified,"
                + "  dTimeStmp"
                + " FROM inv_stock_request_master";

        lsSQL = lsSQL + " ORDER BY sTransNox ASC";

        System.out.println("Executing SQL: " + lsSQL.toString());

        ResultSet loRS = poGRider.executeQuery(lsSQL.toString());
        JSONObject poJSON = new JSONObject();
        JSONArray dataArray = new JSONArray();
        try {
            int lnctr = 0;

            if (MiscUtil.RecordCount(loRS) >= 0) {
                poInvStockRequestMaster = new ArrayList<>();
                while (loRS.next()) {
                    // Print the result set
                    JSONObject request = new JSONObject();
                    System.out.println("sTransNox: " + loRS.getString("sTransNox"));
                    System.out.println("sBranchCd: " + loRS.getString("sBranchCd"));
                    System.out.println("dTransact: " + loRS.getDate("dTransact"));
                    System.out.println("nEntryNox: " + loRS.getInt("nEntryNox"));
                    System.out.println("sReferNox: " + loRS.getString("sReferNox"));
                    System.out.println("------------------------------------------------------------------------------");

                    poInvStockRequestMaster.add(invStockRequestMaster(loRS.getString("sTransNox")));
                    poInvStockRequestMaster.get(poInvStockRequestMaster.size() - 1)
                            .openRecord(loRS.getString("sTransNox"));
                    dataArray.add(request);
                    lnctr++;
                }

                System.out.println("Records found: " + lnctr);
                poJSON.put("result", "success");
                poJSON.put("message", "Record loaded successfully.");
                poJSON.put("data", dataArray);

            } else {
                poInvStockRequestMaster = new ArrayList<>();
                addInventoryStockRequestMaster();
                poJSON.put("result", "error");
                poJSON.put("continue", true);
                poJSON.put("message", "No record found .");
            }
            MiscUtil.close(loRS);
        } catch (SQLException e) {
            poJSON.put("result", "error");
            poJSON.put("message", e.getMessage());
        }
        return poJSON;
    }

    private Model_Inv_Stock_Request_Master invStockRequestMaster(String transactionNo) throws SQLException, GuanzonException {
        Model_Inv_Stock_Request_Master object = new InvWarehouseModels(poGRider).InventoryStockRequestMaster();

        JSONObject loJSON = object.openRecord(transactionNo);

        if ("success".equals((String) loJSON.get("result"))) {
            return object;
        } else {
            return new InvWarehouseModels(poGRider).InventoryStockRequestMaster();
        }
    }

    private Model_Inv_Stock_Request_Master invStockRequestMaster() {
        return new InvWarehouseModels(poGRider).InventoryStockRequestMaster();
    }

    public JSONObject addInventoryStockRequestMaster() {
        poJSON = new JSONObject();

        if (poInvStockRequestMaster.isEmpty()) {
            poInvStockRequestMaster.add(invStockRequestMaster());
        } else {
            if (!poInvStockRequestMaster.get(poInvStockRequestMaster.size() - 1).getTransactionNo().isEmpty()) {
                poInvStockRequestMaster.add(invStockRequestMaster());
            } else {
                poJSON.put("result", "error");
                poJSON.put("message", "Unable to add serialLedger.");
                return poJSON;
            }
        }

        poJSON.put("result", "success");
        return poJSON;
    }

    public JSONObject addOrdersToDetail(String transactionNo) throws CloneNotSupportedException {
        StockRequest loTrans = new StockRequest();
        poJSON = new JSONObject();
        poJSON = loTrans.OpenTransaction(transactionNo);
        if ("success".equals((String) poJSON.get("result"))) {
            for (int lnCtr = 0; lnCtr <= loTrans.getDetailCount() - 1; lnCtr++) {
                Detail(getDetailCount() - 1).setTransactionNo(loTrans.Detail(lnCtr).getTransactionNo());
                Detail(getDetailCount() - 1).setEntryNo(lnCtr);
                Detail(getDetailCount() - 1).setStockID(loTrans.Detail(lnCtr).getStockId());
                Detail(getDetailCount() - 1).setDescription(loTrans.Detail(lnCtr).InvMaster().Inventory().getDescription());
//                Detail(getDetailCount() - 1).setOldPrice(loTrans.Detail(lnCtr).InvMaster().Inventory().getCost());
//                Detail(getDetailCount() - 1).setUnitPrice(loTrans.Detail(lnCtr).InvMaster().Inventory().getCost());
                AddDetail();
            }
        }

        return poJSON;
    }

}
