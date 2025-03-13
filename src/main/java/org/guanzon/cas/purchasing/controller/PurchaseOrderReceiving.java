/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.guanzon.cas.purchasing.controller;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.agent.services.Transaction;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.constant.RecordStatus;
import org.guanzon.appdriver.iface.GValidator;
import org.guanzon.cas.client.Client;
import org.guanzon.cas.client.services.ClientControllers;
import org.guanzon.cas.inv.Inventory;
import org.guanzon.cas.inv.services.InvControllers;
import org.guanzon.cas.parameter.Branch;
import org.guanzon.cas.parameter.Brand;
import org.guanzon.cas.parameter.Company;
import org.guanzon.cas.parameter.Industry;
import org.guanzon.cas.parameter.ModelSeries;
import org.guanzon.cas.parameter.Term;
import org.guanzon.cas.parameter.services.ParamControllers;
import org.guanzon.cas.purchasing.model.Model_POR_Detail;
import org.guanzon.cas.purchasing.model.Model_POR_Master;
import org.guanzon.cas.purchasing.model.Model_PO_Master;
import org.guanzon.cas.purchasing.services.PurchaseOrderModels;
import org.guanzon.cas.purchasing.services.PurchaseOrderReceivingModels;
import org.guanzon.cas.purchasing.status.PurchaseOrderReceivingStatus;
import org.guanzon.cas.purchasing.validator.PurchaseOrderReceivingValidatorFactory;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

/**
 *
 * @author Arsiela 03-12-2025
 */
public class PurchaseOrderReceiving extends Transaction{ 
    
    List<Model_PO_Master> poPOMaster;
    
    public JSONObject InitTransaction(){      
        SOURCE_CODE = "InvR";
        
        poMaster = new PurchaseOrderReceivingModels(poGRider).PurchaseOrderReceivingMaster();
        poDetail = new PurchaseOrderReceivingModels(poGRider).PurchaseOrderReceivingDetails();
        paDetail = new ArrayList<>();
        poPOMaster = new ArrayList<>();
        
        return initialize();
    }
    
    public JSONObject NewTransaction() throws CloneNotSupportedException{    
        return newTransaction();
    }
    
    public JSONObject SaveTransaction() throws SQLException, GuanzonException{
        return saveTransaction();
    }
    
    public JSONObject OpenTransaction(String transactionNo) throws CloneNotSupportedException, SQLException, GuanzonException{  
        return openTransaction(transactionNo);   
    }
    
    public JSONObject UpdateTransaction(){
        return updateTransaction();
    }
    
    public JSONObject ConfirmTransaction(String remarks) throws ParseException, SQLException, GuanzonException{
        poJSON = new JSONObject();

        String lsStatus = PurchaseOrderReceivingStatus.CONFIRMED;
        boolean lbConfirm = true;

        if (getEditMode() != EditMode.READY){
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");                
            return poJSON;
        }

        if (lsStatus.equals((String) poMaster.getValue("cTranStat"))){
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already confirmed.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(PurchaseOrderReceivingStatus.CONFIRMED);
        if (!"success".equals((String) poJSON.get("result"))) return poJSON;

        //change status
        poJSON =  statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks,  lsStatus, !lbConfirm);

        if (!"success".equals((String) poJSON.get("result"))) return poJSON;

        poJSON = new JSONObject();
        poJSON.put("result", "success");

        if (lbConfirm) poJSON.put("message", "Transaction confirmed successfully.");
        else poJSON.put("message", "Transaction confirmation request submitted successfully."); 
        
        return poJSON;
    }
    
    public JSONObject PostTransaction(String remarks) throws ParseException, SQLException, GuanzonException{
        poJSON = new JSONObject();

        String lsStatus = PurchaseOrderReceivingStatus.POSTED;
        boolean lbPosted = true;

        if (getEditMode() != EditMode.READY){
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");                
            return poJSON;
        }

        if (lsStatus.equals((String) poMaster.getValue("cTranStat"))){
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already processed.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(PurchaseOrderReceivingStatus.POSTED);
        if (!"success".equals((String) poJSON.get("result"))) return poJSON;

        //change status
        poJSON =  statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks,  lsStatus, !lbPosted);

        if (!"success".equals((String) poJSON.get("result"))) return poJSON;

        poJSON = new JSONObject();
        poJSON.put("result", "success");

        if (lbPosted) poJSON.put("message", "Transaction posted successfully.");
        else poJSON.put("message", "Transaction posting request submitted successfully.");
        
        return poJSON;
    }
    
    public JSONObject CancelTransaction(String remarks) throws ParseException, SQLException, GuanzonException{
        poJSON = new JSONObject();

        String lsStatus = PurchaseOrderReceivingStatus.CANCELLED;
        boolean lbCancelled = true;

        if (getEditMode() != EditMode.READY){
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");                
            return poJSON;
        }

        if (lsStatus.equals((String) poMaster.getValue("cTranStat"))){
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already cancelled.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(PurchaseOrderReceivingStatus.CANCELLED);
        if (!"success".equals((String) poJSON.get("result"))) return poJSON;

        //change status
        poJSON =  statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks,  lsStatus, !lbCancelled);

        if (!"success".equals((String) poJSON.get("result"))) return poJSON;

        poJSON = new JSONObject();
        poJSON.put("result", "success");

        if (lbCancelled) poJSON.put("message", "Transaction cancelled successfully.");
        else poJSON.put("message", "Transaction cancellation request submitted successfully.");
        
        return poJSON;
    }
    
    public JSONObject VoidTransaction(String remarks) throws ParseException, SQLException, GuanzonException {
        poJSON = new JSONObject();
        
        String lsStatus = PurchaseOrderReceivingStatus.VOID;
        boolean lbVoid = true;

        if (getEditMode() != EditMode.READY){
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");                
            return poJSON;
        }

        if (lsStatus.equals((String) poMaster.getValue("cTranStat"))){
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already voided.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(PurchaseOrderReceivingStatus.VOID);
        if (!"success".equals((String) poJSON.get("result"))) return poJSON;

        //change status
        poJSON =  statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks,  lsStatus, !lbVoid);

        if (!"success".equals((String) poJSON.get("result"))) return poJSON;

        poJSON = new JSONObject();
        poJSON.put("result", "success");

        if (lbVoid) poJSON.put("message", "Transaction voided successfully.");
        else poJSON.put("message", "Transaction voiding request submitted successfully.");
        
        return poJSON;
    }
    
    public JSONObject AddDetail() throws CloneNotSupportedException{
        if (Detail(getDetailCount() - 1).getStockId().isEmpty()) {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "Last row has empty item.");
            return poJSON;
        }

        return addDetail();
    }
    
    /*Search Master References*/
    public JSONObject SearchIndustry(String value, boolean byCode) throws SQLException, GuanzonException {
        Industry object = new ParamControllers(poGRider, logwrapr).Industry();
        object.setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))){
            Master().setIndustryId(object.getModel().getIndustryId());
        }

        return poJSON;
    }
    
    public JSONObject SearchCompany(String value, boolean byCode) throws SQLException, GuanzonException {
        Company object = new ParamControllers(poGRider, logwrapr).Company();
        object.setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))){
            Master().setCompanyId(object.getModel().getCompanyId());
        }

        return poJSON;
    }
    
    public JSONObject SearchBranch(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException{
        Branch object = new ParamControllers(poGRider, logwrapr).Branch();
        object.setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))){
            Master().setBranchCode(object.getModel().getBranchCode());
        }    
        
        return poJSON;
    }
    
    public JSONObject SearchSupplier(String value, boolean byCode) throws SQLException, GuanzonException {
        Client object = new ClientControllers(poGRider, logwrapr).Client();
        object.Master().setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.Master().searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))){
            Master().setSupplierId(object.Master().getModel().getClientId());
            Master().setAddressId(object.ClientAddress().getModel().getAddressId());
//            Master().setTermCode("");
        }

        return poJSON;
    }
    
    public JSONObject SearchTrucking(String value, boolean byCode) throws SQLException, GuanzonException {
        Client object = new ClientControllers(poGRider, logwrapr).Client();
        object.Master().setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.Master().searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))){
            Master().setTruckingId(object.Master().getModel().getClientId());
        }

        return poJSON;
    }
    
    public JSONObject SearchTerm(String value, boolean byCode) throws SQLException, GuanzonException {
        Term object = new ParamControllers(poGRider, logwrapr).Term();
        object.getModel().setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))){
            Master().setTermCode(object.getModel().getTermCode());
        }

        return poJSON;
    }
    
    public JSONObject SearchBarcode(String value, boolean byCode, int row) throws SQLException, GuanzonException {
        Inventory object = new InvControllers(poGRider, logwrapr).Inventory();
        object.getModel().setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))){
            Detail(row).setStockId(object.getModel().getStockId());
            Detail(row).setUnitType(object.getModel().getUnitType());
        }

        return poJSON;
    }
    
    public JSONObject SearchSupersede(String value, boolean byCode, int row) throws SQLException, GuanzonException {
        Inventory object = new InvControllers(poGRider, logwrapr).Inventory();
        object.getModel().setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))){
            Detail(row).setReplaceId(object.getModel().getStockId());
        }

        return poJSON;
    }
    
    public JSONObject SearchDescription(String value, boolean byCode, int row) throws SQLException, GuanzonException {
        Inventory object = new InvControllers(poGRider, logwrapr).Inventory();
        object.getModel().setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))){
            Detail(row).setStockId(object.getModel().getStockId());
        }

        return poJSON;
    }
    
    public JSONObject SearchBrand(String value, boolean byCode, int row) throws ExceptionInInitializerError, SQLException, GuanzonException{
        Brand object = new ParamControllers(poGRider, logwrapr).Brand();
        object.getModel().setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))){
            Detail(row).Inventory().setBrandId(object.getModel().getBrandId());
        }    
        
        return poJSON;
    }
    
    public JSONObject SearchModel(String value, boolean byCode, int row) throws SQLException, GuanzonException {
        ModelSeries object = new ParamControllers(poGRider, logwrapr).ModelSeries();
        object.getModel().setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))){
            Detail(row).Inventory().setStockId("");
        }

        return poJSON;
    }
    
//    public ResultSet getApprovedPurchaseOrder() {
//        String lsSQL = SQL_BROWSE;
//        ResultSet loRS = poGRider.executeQuery(lsSQL);
//        try {
//            if (loRS != null && loRS.next()) {
//                loRS.beforeFirst(); // Move cursor back to the first row for iteration
//                return loRS;
//            } else {
//                return null;
//            }
//        } catch (SQLException e) {
//            logwrapr.severe(e.getMessage());
//            return null;
//        }
//    }
    
    
    public JSONObject getApprovedPurchaseOrder(){
        try {
            String lsSQL = " SELECT "                                                
                            + "   a.sTransNox "                                         
                            + " , a.dTransact "                                         
                            + " , b.sCompnyNm AS sSupplier "                            
                            + " FROM po_master a "                                      
                            + " LEFT JOIN client_master b ON b.sClientID = a.sSupplier ";
            
            lsSQL = MiscUtil.addCondition(lsSQL, " a.sIndstCdx = " + SQLUtil.toSQL(Master().getIndustryId())
                                                + " AND a.sCompnyID = " + SQLUtil.toSQL(Master().getCompanyId())
                                                + " AND a.sSupplier = " + SQLUtil.toSQL(Master().getSupplierId())
                                        )       + " ORDER BY dTransact ASC";
            
            System.out.println("Executing SQL: " + lsSQL);

            ResultSet loRS = poGRider.executeQuery(lsSQL);
            JSONObject poJSON = new JSONObject();

            int lnctr = 0;

            if (MiscUtil.RecordCount(loRS) >= 0) {
                poPOMaster = new ArrayList<>();
                while (loRS.next()) {
                    // Print the result set
                    System.out.println("sTransNox: " + loRS.getString("sTransNox"));
                    System.out.println("dTransact: " + loRS.getDate("dTransact"));
                    System.out.println("sSupplier: " + loRS.getString("sSupplier"));
                    System.out.println("------------------------------------------------------------------------------");

                    poPOMaster.add(PurchaseOrderMaster(loRS.getString("sTransNox")));
                    poPOMaster.get(poPOMaster.size() - 1).openRecord(loRS.getString("sTransNox"));
                    lnctr++;
                }

                System.out.println("Records found: " + lnctr);
                poJSON.put("result", "success");
                poJSON.put("message", "Record loaded successfully.");

            } else {
                poPOMaster = new ArrayList<>();
                addPurchaseOrderMaster();
                poJSON.put("result", "error");
                poJSON.put("continue", true);
                poJSON.put("message", "No record found .");
            }
            MiscUtil.close(loRS);
        } catch (SQLException e) {
            poJSON.put("result", "error");
            poJSON.put("message", e.getMessage());
        } catch (GuanzonException ex) {
            Logger.getLogger(PurchaseOrderReceiving.class.getName()).log(Level.SEVERE, null, ex);
        }
        return poJSON;
    }
    
    private Model_PO_Master PurchaseOrderMaster(String transactionNo) throws SQLException, GuanzonException {
        Model_PO_Master object = new PurchaseOrderModels(poGRider).PurchaseOrderMaster();

        JSONObject loJSON = object.openRecord(transactionNo);

        if ("success".equals((String) loJSON.get("result"))) {
            return object;
        } else {
            return new PurchaseOrderModels(poGRider).PurchaseOrderMaster();
        }
    }
    
    private Model_PO_Master PurchaseOrderMaster() {
        return new PurchaseOrderModels(poGRider).PurchaseOrderMaster();
    }
    
    public JSONObject addPurchaseOrderMaster() {
        poJSON = new JSONObject();

        if (poPOMaster.isEmpty()) {
            poPOMaster.add(PurchaseOrderMaster());
        } else {
            if (!poPOMaster.get(poPOMaster.size() - 1).getTransactionNo().isEmpty()) {
                poPOMaster.add(PurchaseOrderMaster());
            } else {
                poJSON.put("result", "error");
                poJSON.put("message", "Unable to add purchase order.");
                return poJSON;
            }
        }

        poJSON.put("result", "success");
        return poJSON;
    }
    
    public Model_PO_Master PurchaseOrderList(int row) {
        return (Model_PO_Master) poPOMaster.get(row);
    }
    
    public List<Model_PO_Master> PurchaseOrderList() {
       return this.poPOMaster;
    }
    
    @Override
    public String getSourceCode(){
        return SOURCE_CODE;
    }    
    
    @Override
    public Model_POR_Master Master() {
        return (Model_POR_Master) poMaster;
    }

    @Override
    public Model_POR_Detail Detail(int row) {
        return (Model_POR_Detail) paDetail.get(row);
    }    
        
    @Override
    public JSONObject willSave() {
        /*Put system validations and other assignments here*/
        poJSON = new JSONObject();
        
        if (getDetailCount() == 1){
            //do not allow a single item detail with no quantity order
            if (Detail(0).getQuantity() == 0) {
                poJSON.put("result", "error");
                poJSON.put("message", "Your order has zero quantity.");
                return poJSON;
            }
        }        

        Iterator<Model> detail = Detail().iterator();
        while (detail.hasNext()) {
            Model item = detail.next(); // Store the item before checking conditions

            if ("".equals((String) item.getValue("sStockIDx"))
                    || (int) item.getValue("nQuantity") <= 0) {
                detail.remove(); // Correctly remove the item
            }
        }

        //assign other info on detail
        for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr ++){            
            Detail(lnCtr).setTransactionNo(Master().getTransactionNo());
            Detail(lnCtr).setEntryNo(lnCtr + 1);
        }
        
        poJSON.put("result", "success");
        return poJSON;
    }
    
    @Override
    public JSONObject save() {
        /*Put saving business rules here*/
        return isEntryOkay(PurchaseOrderReceivingStatus.OPEN);
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
    public void initSQL(){
        SQL_BROWSE = "";
    }
    
    @Override
    protected JSONObject isEntryOkay(String status){
        GValidator loValidator = PurchaseOrderReceivingValidatorFactory.make(Master().getIndustryId());
        
        loValidator.setApplicationDriver(poGRider);
        loValidator.setTransactionStatus(status);
        
        poJSON = loValidator.validate();
        
        return poJSON;
    }
    
}
