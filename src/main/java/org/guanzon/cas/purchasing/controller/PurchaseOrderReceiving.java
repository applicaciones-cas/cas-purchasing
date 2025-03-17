/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.guanzon.cas.purchasing.controller;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.guanzon.appdriver.agent.ShowDialogFX;
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
import org.guanzon.cas.parameter.Term;
import org.guanzon.cas.parameter.services.ParamControllers;
import org.guanzon.cas.purchasing.model.Model_POR_Detail;
import org.guanzon.cas.purchasing.model.Model_POR_Master;
import org.guanzon.cas.purchasing.model.Model_POR_Serial;
import org.guanzon.cas.purchasing.model.Model_PO_Master;
import org.guanzon.cas.purchasing.services.PurchaseOrderControllers;
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
    private Model_POR_Serial poOthers;
    List<Model_POR_Serial> paOthers;
    List<Model_PO_Master> paPOMaster;
    
    public JSONObject InitTransaction(){      
        SOURCE_CODE = "InvR";
        
        poMaster = new PurchaseOrderReceivingModels(poGRider).PurchaseOrderReceivingMaster();
        poDetail = new PurchaseOrderReceivingModels(poGRider).PurchaseOrderReceivingDetails();
        poOthers = new PurchaseOrderReceivingModels(poGRider).PurchaseOrderReceivingSerial();
        paOthers = new ArrayList<>();
        paDetail = new ArrayList<>();
        paPOMaster = new ArrayList<>();
        
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
    
    public JSONObject searchTransaction() throws CloneNotSupportedException, SQLException, GuanzonException {
        poJSON = new JSONObject();
//            if (psTranStat.length() > 1) {
//                for (int lnCtr = 0; lnCtr <= psTranStat.length() - 1; lnCtr++) {
//                    lsCondition += ", " + SQLUtil.toSQL(Character.toString(psTranStat.charAt(lnCtr)));
//                }
//                lsCondition = "cTranStat IN (" + lsCondition.substring(2) + ")";
//            } else {
//                lsCondition = "cTranStat = " + SQLUtil.toSQL(psTranStat);
//            }
        initSQL();
        String lsSQL = MiscUtil.addCondition(SQL_BROWSE, " a.sIndstCdx = " +  SQLUtil.toSQL(poGRider.getIndustry()));

        poJSON = ShowDialogFX.Search(poGRider,
                lsSQL,
                "",
                "Transaction Date»Transaction No»Supplier",
                "dTransact»sTransNox»sCompnyNm",
                "dTransact»sTransNox»sCompnyNm",
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
    
    @Override
    public int getDetailCount(){
        if (paDetail == null){
            paDetail = new ArrayList<>();
        }   
        
        return paDetail.size();
    }
    
    public JSONObject AddDetail() throws CloneNotSupportedException{
        if(Detail(getDetailCount() - 1).getStockId() != null){
            if (Detail(getDetailCount() - 1).getStockId().isEmpty()) {
                poJSON = new JSONObject();
                poJSON.put("result", "error");
                poJSON.put("message", "Last row has empty item.");
                return poJSON;
            }
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
            Master().setAddressId(object.ClientAddress().getModel().getAddressId()); //TODO
            Master().setContactId(object.ClientInstitutionContact().getModel().getClientId()); //TODO
            Master().setTermCode("");//TODO
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
            Master().setTermCode(object.getModel().getTermId());
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
            Detail(row).setUnitPrce(object.getModel().getCost().doubleValue());
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
        Inventory object = new InvControllers(poGRider, logwrapr).Inventory();
        object.getModel().setRecordStatus(RecordStatus.ACTIVE);
        object.getModel().setBrandId( Detail(row).Inventory().getBrandId());

        poJSON = object.searchRecordOfVariants(value, byCode);

        if ("success".equals((String) poJSON.get("result"))){
            Detail(row).setStockId(object.getModel().getStockId());
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
    
    public JSONObject computeFields() throws SQLException, GuanzonException{
        poJSON = new JSONObject();
        
        //Compute Transaction Total
        Double ldblUnitPrice = 0.00;
        Double ldblDiscount = Master().getDiscount().doubleValue();
        for(int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++){
            ldblUnitPrice = Detail(lnCtr).getUnitPrce().doubleValue();
            ldblUnitPrice = ldblUnitPrice + (ldblUnitPrice * Detail(lnCtr).getQuantity());
        }
        ldblUnitPrice = ldblUnitPrice - ldblDiscount;
        Master().setTransactionTotal(ldblUnitPrice);
        
        //Compute Term Due Date
       
        Long lnTerm = Math.round(Double.parseDouble(Master().Term().getTermValue().toString()));
        LocalDate ldReferenceDate = Master().getReferenceDate().toInstant()
                                 .atZone(ZoneId.systemDefault())
                                 .toLocalDate();
        Date ldTermDue = java.util.Date.from(ldReferenceDate.plusDays(lnTerm).atStartOfDay(ZoneId.systemDefault()).toInstant());
        Master().setDueDate(ldTermDue);
        Master().setTermDueDate(ldTermDue);
        
        return poJSON;
    }
    
    public JSONObject computeDiscount(double discount){
        poJSON = new JSONObject();
        Double ldblUnitPrice = 0.00;
        Double ldblDiscRate = 0.00;
        
        for(int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++){
            ldblUnitPrice = Detail(lnCtr).getUnitPrce().doubleValue();
            ldblUnitPrice = ldblUnitPrice + (ldblUnitPrice * Detail(lnCtr).getQuantity());
        }
        
        ldblDiscRate = discount / ldblUnitPrice;
        Master().setDiscountRate(ldblDiscRate);
        
        return poJSON;
    }
    
    public JSONObject computeDiscountRate(double discountRate){
        poJSON = new JSONObject();
        Double ldblUnitPrice = 0.00;
        Double ldblDiscount = 0.00;
        
        for(int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++){
            ldblUnitPrice = Detail(lnCtr).getUnitPrce().doubleValue();
            ldblUnitPrice = ldblUnitPrice + (ldblUnitPrice * Detail(lnCtr).getQuantity());
        }
        
        ldblDiscount = ldblUnitPrice * discountRate;
        Master().setDiscount(ldblDiscount);
        
        return poJSON;
    }
    
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
            poJSON = new JSONObject();

            int lnctr = 0;

            if (MiscUtil.RecordCount(loRS) >= 0) {
                paPOMaster = new ArrayList<>();
                while (loRS.next()) {
                    // Print the result set
                    System.out.println("sTransNox: " + loRS.getString("sTransNox"));
                    System.out.println("dTransact: " + loRS.getDate("dTransact"));
                    System.out.println("sSupplier: " + loRS.getString("sSupplier"));
                    System.out.println("------------------------------------------------------------------------------");

                    paPOMaster.add(PurchaseOrderMaster());
                    paPOMaster.get(paPOMaster.size() - 1).openRecord(loRS.getString("sTransNox"));
                    lnctr++;
                }

                System.out.println("Records found: " + lnctr);
                poJSON.put("result", "success");
                poJSON.put("message", "Record loaded successfully.");
            } else {
                paPOMaster = new ArrayList<>();
                paPOMaster.add(PurchaseOrderMaster());
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
    
    private Model_PO_Master PurchaseOrderMaster() {
        return new PurchaseOrderModels(poGRider).PurchaseOrderMaster();
    }
    
    public Model_PO_Master PurchaseOrderList(int row) {
        return (Model_PO_Master) paPOMaster.get(row);
    }
    
    public int getPurchaseOrderCount() {
       return this.paPOMaster.size();
    }
    
    public JSONObject addPurchaseOrderToPORDetail(String transactionNo) throws CloneNotSupportedException, SQLException, GuanzonException {
        poJSON = new JSONObject();
        boolean lbExist = false;
        
        PurchaseOrderControllers loTrans = new PurchaseOrderControllers(poGRider, logwrapr);
        poJSON = loTrans.PurchaseOrder().InitTransaction();
        
        if ("success".equals((String) poJSON.get("result"))) {
            poJSON = loTrans.PurchaseOrder().OpenTransaction(transactionNo);
            if ("success".equals((String) poJSON.get("result"))) {
                for (int lnCtr = 0; lnCtr <= loTrans.PurchaseOrder().getDetailCount() - 1; lnCtr++) {
                    if (getDetailCount() > 1) {
                        if  (Detail(getDetailCount() - 2).getOrderNo().equals(loTrans.PurchaseOrder().Detail(lnCtr).getTransactionNo()) &&
                            (Detail(getDetailCount() - 2).getStockId().equals(loTrans.PurchaseOrder().Detail(lnCtr).getStockID()))) {
                            lbExist = true;
                        }   
                    }
                    
                    if(!lbExist){
                        Detail(getDetailCount() - 1).setOrderNo(loTrans.PurchaseOrder().Detail(lnCtr).getTransactionNo());
                        Detail(getDetailCount() - 1).setStockId(loTrans.PurchaseOrder().Detail(lnCtr).getStockID());
                        Detail(getDetailCount() - 1).setUnitType(loTrans.PurchaseOrder().Detail(lnCtr).Inventory().getUnitType());
                        Detail(getDetailCount() - 1).setOrderQty(loTrans.PurchaseOrder().Detail(lnCtr).getQuantity());
                        Detail(getDetailCount() - 1).setWhCount(loTrans.PurchaseOrder().Detail(lnCtr).getQuantity());
                        Detail(getDetailCount() - 1).setUnitPrce(loTrans.PurchaseOrder().Detail(lnCtr).getUnitPrice()); //TODO CONFLICT WITH BIGDECIMAL IN PO CLASS
                        
                        AddDetail();
                    }
                }
            } else {
                poJSON.put("result", "error");
                poJSON.put("message", "No records found.");
            }
        } else {
            poJSON.put("result", "error");
            poJSON.put("message", "No records found.");
        }
        return poJSON;
    }
    
    public JSONObject getPurchaseOrderReceivingSerial(int entryNo) throws SQLException, GuanzonException{
        poJSON = new JSONObject();
        
        if(paOthers == null){
           paOthers = new ArrayList<>();
        }
        
        try {
            String lsSQL = " SELECT "                                                
                            + "    sTransNox "                                          
                            + " ,  nEntryNox "                                           
                            + " ,  sSerialID "                          
                            + " FROM po_receiving_serial " ;
            
            lsSQL = MiscUtil.addCondition(lsSQL, " sTransNox = " + SQLUtil.toSQL(Master().getTransactionNo())
                                                + " AND nEntryNox = " + SQLUtil.toSQL(entryNo)
                                        )       + " ORDER BY sSerialID ASC ";
            
            System.out.println("Executing SQL: " + lsSQL);

            ResultSet loRS = poGRider.executeQuery(lsSQL);

            int lnctr = 0;

            if (MiscUtil.RecordCount(loRS) >= 0) {
                while (loRS.next()) {
                    // Print the result set
                    System.out.println("sTransNox: " + loRS.getString("sTransNox"));
                    System.out.println("nEntryNox: " + loRS.getInt("nEntryNox"));
                    System.out.println("sSerialID: " + loRS.getString("sSerialID"));
                    System.out.println("------------------------------------------------------------------------------");

                    populatePurchaseOrderReceivingSerial(entryNo, loRS.getString("sSerialID"));
                    
                    lnctr++;
                }

                System.out.println("Records found: " + lnctr);
                poJSON.put("result", "success");
                poJSON.put("message", "Record loaded successfully.");

            } else {
                poJSON.put("result", "error");
                poJSON.put("continue", true);
                poJSON.put("message", "No record found .");
            }
            
            poJSON = populatePurchaseOrderReceivingSerial(entryNo, "");
            MiscUtil.close(loRS);
        } catch (SQLException e) {
            poJSON.put("result", "error");
            poJSON.put("message", e.getMessage());
        } catch (GuanzonException ex) {
            Logger.getLogger(PurchaseOrderReceiving.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return poJSON;
    }
    
    private Model_POR_Serial PurchaseOrderReceivingSerial() {
        return new PurchaseOrderReceivingModels(poGRider).PurchaseOrderReceivingSerial();
    }
    
    public JSONObject addPurchaseOrderReceivingSerial() {
        poJSON = new JSONObject();

        if (paOthers.isEmpty()) {
            paOthers.add(PurchaseOrderReceivingSerial());
        } else {
            if (!paOthers.get(paOthers.size() - 1).getTransactionNo().isEmpty()) {
                paOthers.add(PurchaseOrderReceivingSerial());
            } else {
                poJSON.put("result", "error");
                poJSON.put("message", "Unable to add purchase order.");
                return poJSON;
            }
        }
        
        poJSON.put("result", "success");
        return poJSON;
    }
    
    private JSONObject populatePurchaseOrderReceivingSerial(int entryNo, String serialId) throws SQLException, GuanzonException{
        poJSON = new JSONObject();
        int lnQuantity = Detail(entryNo).getQuantity();
        int lnSerialCnt = 0;
        
        paOthers.add(PurchaseOrderReceivingSerial());
        if(serialId.isEmpty()){
            paOthers.get(paOthers.size() - 1).openRecord(Master().getTransactionNo(), entryNo,serialId );
        } else {
            //get total count of serial per per entry no
            for(int lnCtr = 0; lnCtr <= paOthers.size() - 1;lnCtr++){
                if(paOthers.get(lnCtr).getEntryNo() == entryNo ){
                    lnSerialCnt++;
                }
            }

            while (lnSerialCnt < lnQuantity){
                poJSON = paOthers.get(paOthers.size() - 1).newRecord();
                if("success".equals((String) poJSON.get("result"))){
                     paOthers.get(paOthers.size() - 1).setEntryNo(entryNo);
                     paOthers.get(paOthers.size() - 1).setStockId(Detail(entryNo).getStockId());
                } else {
                    return poJSON;
                }

                lnSerialCnt++;
            }
        }
        
        poJSON.put("result", "success");
        return poJSON;
    }
    
    public Model_POR_Serial PurchaseOrderReceivingSerialList(int row) {
        return (Model_POR_Serial) paOthers.get(row);
    }
    
    public int getPurchaseOrderReceivingSerialCount() {
       return this.paOthers.size();
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
        
        if(getDetailCount() <= 0){
            poJSON.put("result", "error");
            poJSON.put("message", "No Purchase order detail to be save.");
            return poJSON;
        }
        
        //assign other info on detail
        for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr ++){            
            Detail(lnCtr).setTransactionNo(Master().getTransactionNo());
            Detail(lnCtr).setWhCount(Detail(lnCtr).getQuantity());
            Detail(lnCtr).setEntryNo(lnCtr + 1);
        }
        
        //assign other info on por serial
        for (int lnCtr = 0; lnCtr <= getPurchaseOrderReceivingSerialCount()- 1; lnCtr ++){            
            PurchaseOrderReceivingSerialList(lnCtr).setTransactionNo(Master().getTransactionNo());
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
        SQL_BROWSE =  " SELECT "
                    + "   a.dTransact  "
                    + " , a.sTransNox  "
                    + " , a.sIndstCdx  "
                    + " , a.sCompnyID  "
                    + " , b.sCompnyNm  "
                    + " FROM po_receiving_master a "
                    + " LEFT JOIN client_master b ON b.sClientID = a.sSupplier ";
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
