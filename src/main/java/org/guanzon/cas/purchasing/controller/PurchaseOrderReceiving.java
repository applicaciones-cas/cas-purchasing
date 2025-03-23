/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.guanzon.cas.purchasing.controller;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
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
import org.guanzon.appdriver.agent.ShowMessageFX;
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
import org.guanzon.cas.inv.InvSerial;
import org.guanzon.cas.inv.Inventory;
import org.guanzon.cas.inv.model.Model_Inv_Serial;
import org.guanzon.cas.inv.model.Model_Inv_Serial_Ledger;
import org.guanzon.cas.inv.model.Model_Inv_Serial_Registration;
import org.guanzon.cas.inv.services.InvControllers;
import org.guanzon.cas.inv.services.InvModels;
import org.guanzon.cas.parameter.Brand;
import org.guanzon.cas.parameter.Company;
import org.guanzon.cas.parameter.InvLocation;
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
    private Model_Inv_Serial poInvSerial;
    private Model_Inv_Serial_Registration poInvSerialRegistration;
    private Model_Inv_Serial_Ledger poInvSerialLedger;
    private boolean pbApproval = false;
    
    List<Model_PO_Master> paPOMaster;
    List<Model_POR_Master> paPORMaster;
    List<Model_POR_Serial> paOthers;
    List<PurchaseOrder> paPurchaseOrder;
    
    public JSONObject InitTransaction(){      
        SOURCE_CODE = "InvR";
        
        poMaster = new PurchaseOrderReceivingModels(poGRider).PurchaseOrderReceivingMaster();
        poDetail = new PurchaseOrderReceivingModels(poGRider).PurchaseOrderReceivingDetails();
        poOthers = new PurchaseOrderReceivingModels(poGRider).PurchaseOrderReceivingSerial();
        poInvSerial = new InvModels(poGRider).InventorySerial();
        poInvSerialRegistration = new InvModels(poGRider).InventorySerialRegistration();
        poInvSerialLedger = new InvModels(poGRider).InventorySerialLedger();
        
        paPurchaseOrder = new ArrayList<>();
        paPORMaster = new ArrayList<>();
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
    
    public JSONObject ConfirmTransaction(String remarks) throws ParseException, SQLException, GuanzonException, CloneNotSupportedException{
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
        
        //Update Purchase Order
        poJSON = updatePurchaseOrder(lsStatus, true);
        if (!"success".equals((String) poJSON.get("result"))) return poJSON;
        
        //change status
        poJSON =  statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks,  lsStatus, !lbConfirm);
        if (!"success".equals((String) poJSON.get("result"))) return poJSON;
        
        //Update Purchase Order, Serial Ledger, Inventory
        poJSON = updateOthers();
        if (!"success".equals((String) poJSON.get("result"))) return poJSON;
        
        poJSON = new JSONObject();
        poJSON.put("result", "success");
        if (lbConfirm) poJSON.put("message", "Transaction confirmed successfully.");
        else poJSON.put("message", "Transaction confirmation request submitted successfully."); 
        
        return poJSON;
    }
    
    public JSONObject ApproveTransaction(String remarks) throws ParseException, SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();
        
        String lsStatus = PurchaseOrderReceivingStatus.APPROVED;
        boolean lbApprove = true;

        if (getEditMode() != EditMode.READY){
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");                
            return poJSON;
        }

        if (lsStatus.equals((String) poMaster.getValue("cTranStat"))){
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already approved.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(PurchaseOrderReceivingStatus.APPROVED);
        if (!"success".equals((String) poJSON.get("result"))) return poJSON;
        
        //Validate Discrepancy
        poJSON = updatePurchaseOrder(lsStatus, true);
        if (!"success".equals((String) poJSON.get("result"))) return poJSON;
        
        if(pbApproval){
            poJSON = ShowDialogFX.getUserApproval(poGRider);
            if (!"success".equals((String) poJSON.get("result"))) return poJSON;
        }
        
        //change status
        poJSON =  statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks,  lsStatus, !lbApprove);
        if (!"success".equals((String) poJSON.get("result"))) return poJSON;
        
        //Update Purchase Order, Serial Ledger, Inventory
        poJSON = updateOthers();
        if (!"success".equals((String) poJSON.get("result"))) return poJSON;

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        if (lbApprove) poJSON.put("message", "Transaction approved successfully.");
        else poJSON.put("message", "Transaction approved request submitted successfully.");
        
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
    
    public JSONObject CancelTransaction(String remarks) throws ParseException, SQLException, GuanzonException, CloneNotSupportedException{
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
        
        
        //update Purchase Order
        poJSON = updatePurchaseOrder(lsStatus, true);
        if (!"success".equals((String) poJSON.get("result"))) return poJSON;
        
        //change status
        poJSON =  statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks,  lsStatus, !lbCancelled);
        if (!"success".equals((String) poJSON.get("result"))) return poJSON;
        
        //Update Purchase Order, Serial Ledger, Inventory
        poJSON = updateOthers();
        if (!"success".equals((String) poJSON.get("result"))) return poJSON;
        
        poJSON = new JSONObject();
        poJSON.put("result", "success");
        if (lbCancelled) poJSON.put("message", "Transaction cancelled successfully.");
        else poJSON.put("message", "Transaction cancellation request submitted successfully.");
        
        return poJSON;
    }
    
    public JSONObject VoidTransaction(String remarks) throws ParseException, SQLException, GuanzonException, CloneNotSupportedException {
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
        
        //Validate Discrepancy
        poJSON = updatePurchaseOrder(lsStatus, true);
        if (!"success".equals((String) poJSON.get("result"))) return poJSON;
        
        if(pbApproval){
            poJSON = ShowDialogFX.getUserApproval(poGRider);
            if (!"success".equals((String) poJSON.get("result"))) return poJSON;
        }

        //change status
        poJSON =  statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks,  lsStatus, !lbVoid);
        if (!"success".equals((String) poJSON.get("result"))) return poJSON;
        
        //Update Purchase Order, Serial Ledger, Inventory
        poJSON = updateOthers();
        if (!"success".equals((String) poJSON.get("result"))) return poJSON;
        
        poJSON = new JSONObject();
        poJSON.put("result", "success");
        if (lbVoid) poJSON.put("message", "Transaction voided successfully.");
        else poJSON.put("message", "Transaction voiding request submitted successfully.");
        
        return poJSON;
    }
    
    public JSONObject ReturnTransaction(String remarks) throws ParseException, SQLException, GuanzonException {
        poJSON = new JSONObject();
        
        String lsStatus = PurchaseOrderReceivingStatus.RETURNED;
        boolean lbReturn = true;

        if (getEditMode() != EditMode.READY){
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");                
            return poJSON;
        }

        if (lsStatus.equals((String) poMaster.getValue("cTranStat"))){
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already returned.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(PurchaseOrderReceivingStatus.RETURNED);
        if (!"success".equals((String) poJSON.get("result"))) return poJSON;

        //change status
        poJSON =  statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks,  lsStatus, !lbReturn);

        if (!"success".equals((String) poJSON.get("result"))) return poJSON;

        poJSON = new JSONObject();
        poJSON.put("result", "success");

        if (lbReturn) poJSON.put("message", "Transaction returned successfully.");
        else poJSON.put("message", "Transaction returned request submitted successfully.");
        
        return poJSON;
    }
    
    public JSONObject searchTransaction() throws CloneNotSupportedException, SQLException, GuanzonException {
        poJSON = new JSONObject();
        initSQL();
        String lsSQL = MiscUtil.addCondition(SQL_BROWSE, " a.sIndstCdx = " +  SQLUtil.toSQL(poGRider.getIndustry()));

        poJSON = ShowDialogFX.Browse(poGRider,
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
    public JSONObject SearchCompany(String value, boolean byCode) throws SQLException, GuanzonException {
        Company object = new ParamControllers(poGRider, logwrapr).Company();
        object.setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))){
            Master().setCompanyId(object.getModel().getCompanyId());
        } else {
            Master().setCompanyId("");
        }

        return poJSON;
    }
    
    public JSONObject SearchSupplier(String value, boolean byCode) throws SQLException, GuanzonException {
        Client object = new ClientControllers(poGRider, logwrapr).Client();
        object.Master().setRecordStatus(RecordStatus.ACTIVE);
//        object.Master().setClientType(1);

        poJSON = object.Master().searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))){
            Master().setSupplierId(object.Master().getModel().getClientId());
            Master().setAddressId(object.ClientAddress().getModel().getAddressId()); //TODO
            Master().setContactId(object.ClientInstitutionContact().getModel().getClientId()); //TODO
            Master().setTermCode("");//TODO
        } else {
            Master().setSupplierId("");
            Master().setAddressId(""); //TODO
            Master().setContactId(""); //TODO
        }

        return poJSON;
    }
    
    public JSONObject SearchTrucking(String value, boolean byCode) throws SQLException, GuanzonException {
        Client object = new ClientControllers(poGRider, logwrapr).Client();
        object.Master().setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.Master().searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))){
            Master().setTruckingId(object.Master().getModel().getClientId());
        } else {
            Master().setTruckingId("");
        }

        return poJSON;
    }
    
    public JSONObject SearchTerm(String value, boolean byCode) throws SQLException, GuanzonException {
        Term object = new ParamControllers(poGRider, logwrapr).Term();
        object.getModel().setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))){
            Master().setTermCode(object.getModel().getTermId());
        } else {
            Master().setTermCode("");
        }

        return poJSON;
    }
    
    public JSONObject SearchBarcode(String value, boolean byCode, int row) throws SQLException, GuanzonException {
        Inventory object = new InvControllers(poGRider, logwrapr).Inventory();
        object.getModel().setRecordStatus(RecordStatus.ACTIVE);
//        poJSON = object.searchRecord(value, byCode, Master().getSupplierId()); TODO
        
        poJSON = object.searchRecord(value, byCode);
        if ("success".equals((String) poJSON.get("result"))){
            for(int lnRow = 0; lnRow <= getDetailCount() - 1; lnRow++){
                if(lnRow != row ){
                    if  ((Detail(lnRow).getOrderNo().equals("") || Detail(lnRow).getOrderNo() == null) &&
                        (Detail(lnRow).getStockId().equals(object.getModel().getStockId()))) {
                        poJSON.put("result", "error");
                        poJSON.put("message", "Barcode: " + object.getModel().getBarCode()+ " already exist in table at row " + (lnRow+1) + ".");
                        return poJSON;
                    } 
                }
            }
            
            Detail(row).setStockId(object.getModel().getStockId());
            Detail(row).setUnitType(object.getModel().getUnitType());
            Detail(row).setUnitPrce(object.getModel().getCost().doubleValue());
        } else {
            Detail(row).setStockId("");
            Detail(row).setUnitType("");
            Detail(row).setUnitPrce(0.00);
        }
        return poJSON;
    }
    
    public JSONObject SearchSupersede(String value, boolean byCode, int row) throws SQLException, GuanzonException {
        Inventory object = new InvControllers(poGRider, logwrapr).Inventory();
        object.getModel().setRecordStatus(RecordStatus.ACTIVE);
//        poJSON = object.searchRecord(value, byCode, Master().getSupplierId()); TODO

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))){
            Detail(row).setReplaceId(object.getModel().getStockId());
        } else {
            Detail(row).setReplaceId("");
        }

        return poJSON;
    }
    
    public JSONObject SearchDescription(String value, boolean byCode, int row) throws SQLException, GuanzonException {
        Inventory object = new InvControllers(poGRider, logwrapr).Inventory();
        object.getModel().setRecordStatus(RecordStatus.ACTIVE);
//        poJSON = object.searchRecord(value, byCode, Master().getSupplierId()); TODO

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))){
            for(int lnRow = 0; lnRow <= getDetailCount() - 1; lnRow++){
                if(lnRow != row ){
                    if  ((Detail(lnRow).getOrderNo().equals("") || Detail(lnRow).getOrderNo() == null) &&
                        (Detail(lnRow).getStockId().equals(object.getModel().getStockId()))) {
                        poJSON.put("result", "error");
                        poJSON.put("message", "Barcode: " + object.getModel().getBarCode()+ " already exist in table at row " + (lnRow+1) + ".");
                        return poJSON;
                    } 
                }
            }
            
            Detail(row).setStockId(object.getModel().getStockId());
            Detail(row).setUnitType(object.getModel().getUnitType());
            Detail(row).setUnitPrce(object.getModel().getCost().doubleValue());
        } else {
            Detail(row).setStockId("");
            Detail(row).setUnitType("");
            Detail(row).setUnitPrce(0.00);
        }

        return poJSON;
    }
    
    //TODO
    public JSONObject SearchBrand(String value, boolean byCode, int row) throws ExceptionInInitializerError, SQLException, GuanzonException{
        Brand object = new ParamControllers(poGRider, logwrapr).Brand();
        object.getModel().setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode,poGRider.getIndustry());
        if ("success".equals((String) poJSON.get("result"))){
            Detail(row).setBrandId(object.getModel().getBrandId());
            Detail(row).setModelVariantId("");
        } else {
            Detail(row).setBrandId("");
            Detail(row).setModelVariantId("");
        }
        
        return poJSON;
    }
    
    public JSONObject SearchModel(String value, boolean byCode, int row) throws SQLException, GuanzonException {
        Inventory object = new InvControllers(poGRider, logwrapr).Inventory();
        object.getModel().setRecordStatus(RecordStatus.ACTIVE);
        object.getModel().setBrandId( Detail(row).Inventory().getBrandId());

        
        poJSON = object.searchRecord(value, byCode );
//        poJSON = object.searchRecord(value, byCode, null,Detail(row).Inventory().getBrandId() );
//        poJSON = object.searchRecord(value, byCode, Detail(row).Inventory().getBrandId(), Detail(row).Inventory().getBrandId()); TODO
        if ("success".equals((String) poJSON.get("result"))){
            for(int lnRow = 0; lnRow <= getDetailCount() - 1; lnRow++){
                if(lnRow != row ){
                    if  ((Detail(lnRow).getOrderNo().equals("") || Detail(lnRow).getOrderNo() == null) &&
                        (Detail(lnRow).getStockId().equals(object.getModel().getStockId()))) {
                        poJSON.put("result", "error");
                        poJSON.put("message", "Model: " + object.getModel().getDescription()+ " already exist in table at row " + (lnRow+1) + ".");
                        return poJSON;
                    } 
                }
            }
            
            Detail(row).setStockId(object.getModel().getStockId());
            Detail(row).setUnitType(object.getModel().getUnitType());
            Detail(row).setModelVariantId(object.getModel().getVariantId());
            Detail(row).setUnitPrce(object.getModel().getCost().doubleValue());
        } else {
            Detail(row).setStockId("");
            Detail(row).setUnitType("");
            Detail(row).setBrandId("");
            Detail(row).setModelVariantId("");
            Detail(row).setUnitPrce(0.00);
        }

        return poJSON;
    }
    
    public JSONObject SearchLocation(String value, boolean byCode, int porRow) throws SQLException, GuanzonException {
        InvLocation object = new ParamControllers(poGRider, logwrapr).InventoryLocation();
        object.getModel().setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);
        if ("success".equals((String) poJSON.get("result"))){
            PurchaseOrderReceivingSerialList(porRow).setLocationId(object.getModel().getLocationId());
        } else {
            PurchaseOrderReceivingSerialList(porRow).setLocationId("");
        }

        return poJSON;
    }
    
    public JSONObject computeFields() throws SQLException, GuanzonException {
        poJSON = new JSONObject();

        //Compute Transaction Total
        Double ldblTotal = 0.00;
        Double ldblDiscount = Master().getDiscount().doubleValue();
        for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {

            ldblTotal += (Detail(lnCtr).getUnitPrce().doubleValue() * Detail(lnCtr).getQuantity().intValue());
        }
        if (ldblDiscount < 0 || ldblDiscount > ldblTotal) {
        } else {
            ldblTotal = ldblTotal - ldblDiscount;
            Master().setTransactionTotal(ldblTotal);
        }
        //Compute Term Due Date
        LocalDate ldReferenceDate = strToDate(xsDateShort(Master().getReferenceDate()));
        Long lnTerm = Math.round(Double.parseDouble(Master().Term().getTermValue().toString()));
        Date ldTermDue = java.util.Date.from(ldReferenceDate.plusDays(lnTerm).atStartOfDay(ZoneId.systemDefault()).toInstant());
        Master().setDueDate(ldTermDue);
        Master().setTermDueDate(ldTermDue);

        return poJSON;
    }
    
    /*Convert Date to String*/
    private static String xsDateShort(Date fdValue) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String date = sdf.format(fdValue);
        return date;
    }
    
    private LocalDate strToDate(String val) {
        DateTimeFormatter date_formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate localDate = LocalDate.parse(val, date_formatter);
        return localDate;
    }
    
    public JSONObject computeDiscountRate(double discount){
        poJSON = new JSONObject();
        Double ldblTotal = 0.00;
        Double ldblDiscRate = 0.00;
        
        for(int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++){
            ldblTotal += (Detail(lnCtr).getUnitPrce().doubleValue() * Detail(lnCtr).getQuantity().intValue());
        }
        
        if (discount < 0 || discount > ldblTotal) {
            poJSON.put("result", "error");
            poJSON.put("message", "Discount amount cannot be negative or exceed the transaction total.");
        } else{
           poJSON.put("result", "success");
           poJSON.put("message", "success");
           ldblDiscRate = (discount / ldblTotal) * 100;
           Master().setDiscountRate(ldblDiscRate);
        }
        return poJSON;
    }
    
    public JSONObject computeDiscount(double discountRate){
        poJSON = new JSONObject();
        Double ldblTotal = 0.00;
        Double ldblDiscount = 0.00;
        
        for(int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++){
            ldblTotal += (Detail(lnCtr).getUnitPrce().doubleValue() * Detail(lnCtr).getQuantity().intValue());
        }
        
        ldblDiscount = ldblTotal * (discountRate / 100.00);
        Master().setDiscount(ldblDiscount);
        
        return poJSON;
    }
    
//    public JSONObject getPurchaseOrderReceiving(boolean isConfirmed, String companyId, String supplierId, String referenceNo){ 
    public JSONObject loadPurchaseOrderReceiving(boolean isConfirmed, String referenceNo ){ 
        try {
            initSQL();
            String lsSQL = MiscUtil.addCondition(SQL_BROWSE, " a.sIndstCdx = " +  SQLUtil.toSQL(poGRider.getIndustry())
                                                                + " AND a.sCompnyID LIKE " +  SQLUtil.toSQL("%"+ Master().getCompanyId()))
//                                                                + " AND a.sSupplier LIKE " +  SQLUtil.toSQL("%"+ Master().getSupplierId())
                                                                + " AND a.sTransNox LIKE " +  SQLUtil.toSQL("%"+ referenceNo);
            if(isConfirmed){
                lsSQL = lsSQL + " AND (a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderReceivingStatus.CONFIRMED)
                              + " OR a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderReceivingStatus.APPROVED) + " ) ";
            } else {
                lsSQL = lsSQL + " AND (a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderReceivingStatus.OPEN) 
                              + " OR a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderReceivingStatus.CONFIRMED) + " ) ";
            }
            
            System.out.println("Executing SQL: " + lsSQL);
            ResultSet loRS = poGRider.executeQuery(lsSQL);
            poJSON = new JSONObject();

            int lnctr = 0;

            if (MiscUtil.RecordCount(loRS) >= 0) {
                paPORMaster = new ArrayList<>();
                while (loRS.next()) {
                    // Print the result set
                    System.out.println("sTransNox: " + loRS.getString("sTransNox"));
                    System.out.println("dTransact: " + loRS.getDate("dTransact"));
                    System.out.println("sCompnyNm: " + loRS.getString("sCompnyNm"));
                    System.out.println("------------------------------------------------------------------------------");

                    paPORMaster.add(PurchaseOrderReceivingMaster());
                    paPORMaster.get(paPORMaster.size() - 1).openRecord(loRS.getString("sTransNox"));
                    lnctr++;
                }

                System.out.println("Records found: " + lnctr);
                poJSON.put("result", "success");
                poJSON.put("message", "Record loaded successfully.");
            } else {
                paPORMaster = new ArrayList<>();
                paPORMaster.add(PurchaseOrderReceivingMaster());
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
    
    private Model_POR_Master PurchaseOrderReceivingMaster() {
        return new PurchaseOrderReceivingModels(poGRider).PurchaseOrderReceivingMaster();
    }
    
    public Model_POR_Master PurchaseOrderReceivingList(int row) {
        return (Model_POR_Master) paPORMaster.get(row);
    }
    
    public int getPurchaseOrderReceivingCount() {
       return this.paPORMaster.size();
    }
    
    public JSONObject getApprovedPurchaseOrder(){
        try {
            String lsSQL = " SELECT "                                                
                            + "   a.sTransNox "                                         
                            + " , a.dTransact "                                         
                            + " , b.sCompnyNm AS sSupplier "                            
                            + " FROM po_master a "         
                            + " LEFT JOIN client_master b ON b.sClientID = a.sSupplier "  ;
            
            lsSQL = MiscUtil.addCondition(lsSQL, " a.sIndstCdx = " + SQLUtil.toSQL(Master().getIndustryId())
                                                + " AND a.sCompnyID = " + SQLUtil.toSQL(Master().getCompanyId())
//                                                + " AND a.sSupplier = " + SQLUtil.toSQL(Master().getSupplierId())
//                                                + " AND a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderStatus.APPROVED)
                                                + " AND a.cProcessd = '0'" //get po that is approve but not yet processed
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
                    
                    for(int lnRow = 0; lnRow <= getDetailCount() - 1; lnRow++){
                        if  (Detail(lnRow).getOrderNo().equals(loTrans.PurchaseOrder().Detail(lnCtr).getTransactionNo()) &&
                            (Detail(lnRow).getStockId().equals(loTrans.PurchaseOrder().Detail(lnCtr).getStockID()))) {
                            lbExist = true;
                            break;
                        } 
                        
                        if  (Detail(lnRow).getOrderNo() != null && !Detail(lnRow).getOrderNo().equals("")){
                            //check when pre-owned po is already exist in detail. 
                            //if exist only pre-owned purchase order will allow to insert in por detail 
                            if(Detail(lnRow).PurchaseOrderMaster().getPreOwned() != loTrans.PurchaseOrder().Master().getPreOwned()){
                                poJSON.put("result", "error");
                                poJSON.put("message", "Purchase orders for pre-owned items cannot be combined with purchase orders for new items.");
                                return poJSON;
                            }
                        }
                    }
                    
                    if(!lbExist){
                        //Only insert po detail that has item to receive
                        if(loTrans.PurchaseOrder().Detail(lnCtr).getQuantity().intValue() > loTrans.PurchaseOrder().Detail(lnCtr).getReceivedQuantity().intValue()){
                            Detail(getDetailCount() - 1).setOrderNo(loTrans.PurchaseOrder().Detail(lnCtr).getTransactionNo());
                            Detail(getDetailCount() - 1).setStockId(loTrans.PurchaseOrder().Detail(lnCtr).getStockID());
                            Detail(getDetailCount() - 1).setUnitType(loTrans.PurchaseOrder().Detail(lnCtr).Inventory().getUnitType());
                            Detail(getDetailCount() - 1).setOrderQty(loTrans.PurchaseOrder().Detail(lnCtr).getQuantity());
                            Detail(getDetailCount() - 1).setWhCount(loTrans.PurchaseOrder().Detail(lnCtr).getQuantity());
                            Detail(getDetailCount() - 1).setUnitPrce(loTrans.PurchaseOrder().Detail(lnCtr).getUnitPrice());

                            AddDetail(); 
                        }
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
    
    public void resetOthers(){
        paOthers = new ArrayList<>();
    }
    
    //Purchase Order Receiving Serial
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

                    poJSON = populatePurchaseOrderReceivingSerial(entryNo, loRS.getString("sSerialID"));
                    if("error".equals((String) poJSON.get("result"))){
                        return poJSON;
                    }
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
            if("error".equals((String) poJSON.get("result"))){
                return poJSON;
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
    
    private JSONObject populatePurchaseOrderReceivingSerial(int entryNo, String serialId) throws SQLException, GuanzonException{
        poJSON = new JSONObject();
        int lnQuantity = Detail(entryNo-1).getQuantity().intValue();
        int lnSerialCnt = 0;
        boolean lbShowMessage = false;
        
        if(!serialId.isEmpty()){
            //1. Checke Serial if already exist in POR Serial list
            for(int lnCtr = 0; lnCtr <= getPurchaseOrderReceivingSerialCount()- 1;lnCtr++){
                if(paOthers.get(lnCtr).getSerialId().equals(serialId)){
                    poJSON.put("result", "success");
                    return poJSON;
                }
            }
            
            paOthers.add(PurchaseOrderReceivingSerial());
            poJSON = paOthers.get(getPurchaseOrderReceivingSerialCount() - 1).openRecord(Master().getTransactionNo(), entryNo,serialId );
            if("error".equals((String) poJSON.get("result"))){
                return poJSON;
            }
            
            poJSON = paOthers.get(getPurchaseOrderReceivingSerialCount() - 1).updateRecord();
            if("error".equals((String) poJSON.get("result"))){
                return poJSON;
            }
            
            paOthers.get(getPurchaseOrderReceivingSerialCount() - 1).setSerial01(paOthers.get(getPurchaseOrderReceivingSerialCount() - 1).InventorySerial().getSerial01());
            paOthers.get(getPurchaseOrderReceivingSerialCount() - 1).setSerial02(paOthers.get(getPurchaseOrderReceivingSerialCount() - 1).InventorySerial().getSerial02());
            paOthers.get(getPurchaseOrderReceivingSerialCount() - 1).setConductionStickerNo(paOthers.get(getPurchaseOrderReceivingSerialCount() - 1).InventorySerialRegistration().getConductionStickerNo());
            paOthers.get(getPurchaseOrderReceivingSerialCount() - 1).setPlateNo(paOthers.get(getPurchaseOrderReceivingSerialCount() - 1).InventorySerialRegistration().getPlateNoP());
        
        } else {
            
            //get total count of por serial per entry no
            for(int lnCtr = 0; lnCtr <= getPurchaseOrderReceivingSerialCount()- 1;lnCtr++){
                if(paOthers.get(lnCtr).getEntryNo() == entryNo ){
                    lnSerialCnt++;
                }
            }
            
            //if por serial is less than the quantity declared in por detail add row
            if(lnSerialCnt < lnQuantity){
                //Add row for others
                while (lnSerialCnt < lnQuantity){
                    paOthers.add(PurchaseOrderReceivingSerial());
                    poJSON = paOthers.get(getPurchaseOrderReceivingSerialCount() - 1).newRecord();
                    if("success".equals((String) poJSON.get("result"))){
                        paOthers.get(getPurchaseOrderReceivingSerialCount() - 1).setEntryNo(entryNo);
                        paOthers.get(getPurchaseOrderReceivingSerialCount() - 1).setStockId(Detail(entryNo-1).getStockId());
                        paOthers.get(getPurchaseOrderReceivingSerialCount() - 1).setSerialId("");
                    } else {
                        return poJSON;
                    }

                    lnSerialCnt++;
                }
            } else {
                //Remove row for excess por serial
                while(lnSerialCnt > lnQuantity){
                    //get total count of serial per entry no
                    for (int lnCtr = getPurchaseOrderReceivingSerialCount() - 1; lnCtr >= 0; lnCtr--) {  // Iterate backward
                        if(paOthers.get(lnCtr).getEntryNo() == entryNo ){
                            //Priority to remove the empty serial01 || emptry serial02
                            if((paOthers.get(lnCtr).getSerial01() == null || paOthers.get(lnCtr).getSerial01().equals("")) ||
                                    (paOthers.get(lnCtr).getSerial02() == null || paOthers.get(lnCtr).getSerial02().equals(""))){
                                paOthers.remove(lnCtr);
                                lnSerialCnt--;
                                break;
                            } 
                            
                            if ((paOthers.get(lnCtr).getSerial01() != null && !paOthers.get(lnCtr).getSerial01().equals("")) ||
                                    (paOthers.get(lnCtr).getSerial02() != null && !paOthers.get(lnCtr).getSerial02().equals(""))){
                                if(!lbShowMessage){
                                    if (ShowMessageFX.OkayCancel(null, "Purchase Order Receiving Serial", 
                                            "The quantity has been reduced. Do you want to disregard the changes and delete the serial number? ") == true) {
                                    } else {
                                        poJSON.put("result", "error");
                                        poJSON.put("message", "You have cancelled the operation. The serial number was not deleted.");
                                        return poJSON; 
                                    }
                                    lbShowMessage = true; //set true to identify that prompt message is already called
                                }
                                paOthers.remove(lnCtr);
                                lnSerialCnt--;
                                break;
                            } 
                        } 
                    }
                }
            }
        }
        
        poJSON.put("result", "success");
        return poJSON;
    }
    
//    private JSONObject populatePurchaseOrderReceivingSerial(int entryNo, String serialId) throws SQLException, GuanzonException{
//        poJSON = new JSONObject();
//        int lnQuantity = Detail(entryNo).getQuantity();
//        int lnSerialCnt = 0;
//        
//        if(!serialId.isEmpty()){
//            paOthers.add(PurchaseOrderReceivingSerial());
//            paInvSerial.add(InventorySerial());
//            
//            paOthers.get(getPurchaseOrderReceivingSerialCount() - 1).openRecord(Master().getTransactionNo(), entryNo,serialId );
//            paInvSerial.get(getInventorySerialCount()- 1).openRecord(serialId );
//        } else {
//            //get total count of serial per per entry no
//            for(int lnCtr = 0; lnCtr <= getInventorySerialCount() - 1;lnCtr++){
//                if(paOthers.get(lnCtr).getEntryNo() == entryNo ){
//                    lnSerialCnt++;
//                }
//            }
//
//            while (lnSerialCnt < lnQuantity){
//                paOthers.add(PurchaseOrderReceivingSerial());
//                paInvSerial.add(InventorySerial());
//                
//                //Inventory Serial
//                poJSON = InventorySerialList(getInventorySerialCount() - 1).newRecord();
//                if("success".equals((String) poJSON.get("result"))){
//                    paInvSerial.get(getInventorySerialCount() - 1).setBranchCode(poGRider.getBranchCode());
//                    paInvSerial.get(getInventorySerialCount() - 1).setCompnyId(Master().getCompanyId());
//                    paInvSerial.get(getInventorySerialCount() - 1).setStockId(Detail(entryNo).getStockId());
//                } else {
//                    return poJSON;
//                }
//                
//                //POR Serial
//                poJSON = paOthers.get(getPurchaseOrderReceivingSerialCount() - 1).newRecord();
//                if("success".equals((String) poJSON.get("result"))){
//                    paOthers.get(getPurchaseOrderReceivingSerialCount() - 1).setEntryNo(entryNo);
//                    paOthers.get(getPurchaseOrderReceivingSerialCount() - 1).setStockId(Detail(entryNo).getStockId());
//                    paOthers.get(getPurchaseOrderReceivingSerialCount() - 1).setSerialId(InventorySerialList(getInventorySerialCount() - 1).getSerialId());
//                } else {
//                    return poJSON;
//                }
//
//                lnSerialCnt++;
//            }
//        }
//        
//        poJSON.put("result", "success");
//        return poJSON;
//    }
    
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
    
    private Model_POR_Serial PurchaseOrderReceivingSerial() {
        return new PurchaseOrderReceivingModels(poGRider).PurchaseOrderReceivingSerial();
    }
    
    public Model_POR_Serial PurchaseOrderReceivingSerialList(int row) {
        return (Model_POR_Serial) paOthers.get(row);
    }
    
    public int getPurchaseOrderReceivingSerialCount() {
       return this.paOthers.size();
    }
    
    public List<Model_POR_Serial> PurchaseOrderReceivingSerialList() {
        return paOthers;
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
    public JSONObject willSave() throws SQLException {
        /*Put system validations and other assignments here*/
        poJSON = new JSONObject();
        
        if(Master().getTransactionStatus().equals(PurchaseOrderReceivingStatus.RETURNED)){
            Master().setTransactionStatus(PurchaseOrderReceivingStatus.OPEN); //If edited update trasaction status into open
        }
        Master().setModifiedDate(poGRider.getServerDate());      

        Iterator<Model> detail = Detail().iterator();
        while (detail.hasNext()) {
            Model item = detail.next(); 

            if ("".equals((String) item.getValue("sStockIDx"))
                    || (int) item.getValue("nQuantity") <= 0) {
                detail.remove(); 
            }
        }
        
        if(getDetailCount() <= 0){
            poJSON.put("result", "error");
            poJSON.put("message", "No Purchase order detail to be save.");
            return poJSON;
        }
        
        if (getDetailCount() == 1){
            //do not allow a single item detail with no quantity order
            if (Detail(0).getQuantity().intValue() == 0) {
                poJSON.put("result", "error");
                poJSON.put("message", "Your order has zero quantity.");
                return poJSON;
            }
        }  
        
        int lnList = 0;
        int lnTotal = getPurchaseOrderReceivingSerialCount() - 1;
        while(lnList <= lnTotal){
            if(PurchaseOrderReceivingSerialList(lnList).getSerialId() == null || PurchaseOrderReceivingSerialList(lnList).getSerialId().equals("")){
                if((PurchaseOrderReceivingSerialList(lnList).getSerial01() == null || PurchaseOrderReceivingSerialList(lnList).getSerial01().equals("")) 
                    && (PurchaseOrderReceivingSerialList(lnList).getSerial02() == null || PurchaseOrderReceivingSerialList(lnList).getSerial02().equals(""))
                    ){
                    paOthers.remove(lnList);
                    lnList--;
                    lnTotal = getPurchaseOrderReceivingSerialCount() - 1;
                    if(lnTotal < 0){
                        break;
                    }
                    if(lnList < 0){
                        lnList = 0;
                    }
                    continue;
                }

                //If there a value for serial 1 do not allow saving when serial 2 and location is empty 
                if((PurchaseOrderReceivingSerialList(lnList).getSerial01() != null || !PurchaseOrderReceivingSerialList(lnList).getSerial01().equals("")) 
                    && ((PurchaseOrderReceivingSerialList(lnList).getSerial02() == null || PurchaseOrderReceivingSerialList(lnList).getSerial02().equals(""))
                    || (PurchaseOrderReceivingSerialList(lnList).getLocationId() == null || PurchaseOrderReceivingSerialList(lnList).getLocationId().equals(""))
                        ) ||
                    (PurchaseOrderReceivingSerialList(lnList).getSerial02() != null || !PurchaseOrderReceivingSerialList(lnList).getSerial02().equals("")) 
                    && ((PurchaseOrderReceivingSerialList(lnList).getSerial01() == null || PurchaseOrderReceivingSerialList(lnList).getSerial01().equals(""))
                    || (PurchaseOrderReceivingSerialList(lnList).getLocationId() == null || PurchaseOrderReceivingSerialList(lnList).getLocationId().equals(""))
                        )){

                    poJSON.put("result", "error");
                    poJSON.put("message", "Serial and Location cannot be empty for Entry No " + PurchaseOrderReceivingSerialList(lnList).getEntryNo());
                    return poJSON;
                }
            }
            lnList++;
        }
        
        //assign other info on detail
        for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr ++){            
            Detail(lnCtr).setTransactionNo(Master().getTransactionNo());
            Detail(lnCtr).setWhCount(Detail(lnCtr).getQuantity());
            Detail(lnCtr).setEntryNo(lnCtr + 1);
            Detail(lnCtr).setModifiedDate(poGRider.getServerDate());
        }
        
        //assign other info on por serial
        for (int lnCtr = 0; lnCtr <= getPurchaseOrderReceivingSerialCount() - 1; lnCtr ++){            
            PurchaseOrderReceivingSerialList(lnCtr).setTransactionNo(Master().getTransactionNo());
            PurchaseOrderReceivingSerialList(lnCtr).setModifiedDate(poGRider.getServerDate());
        }
        
        poJSON.put("result", "success");
        return poJSON;
    }
    
    @Override
    public JSONObject save() {
        /*Put saving business rules here*/
        return isEntryOkay(PurchaseOrderReceivingStatus.OPEN);
    }
    
//    @Override
//    public JSONObject saveOthers() {
//        /*Only modify this if there are other tables to modify except the master and detail tables*/
//        poJSON = new JSONObject();
//        int lnRow;
//        
//        try {
//        
//            //Purchase Order Receiving Serial
//            for(lnRow = 0; lnRow <= getPurchaseOrderReceivingSerialCount() - 1; lnRow++){
//                //1. Check for serial ID
//                if(paOthers.get(lnRow).getSerialId().equals("") || paOthers.get(lnRow).getSerialId() == null){
//                    //Inventory Serial
//                    poJSON = poInvSerial.newRecord();
//                    if("error".equals((String) poJSON.get("result"))){
//                        System.out.println((String) poJSON.get("message"));
//                    } 
//                    
//                    //Inventory Serial Registration
//                    if((!paOthers.get(lnRow).getConductionStickerNo().equals("") || paOthers.get(lnRow).getConductionStickerNo() != null) ||
//                            (!paOthers.get(lnRow).getPlateNo().equals("") || paOthers.get(lnRow).getPlateNo() != null)) {
//                        poJSON = poInvSerialRegistration.newRecord();
//                        if("error".equals((String) poJSON.get("result"))){
//                            System.out.println((String) poJSON.get("message"));
//                        } 
//                    }
//                    
//                } else {
//                    //2. Open Inventory Serial / Registration equal to serial ID value
//                    poJSON = poInvSerial.openRecord(paOthers.get(lnRow).getSerialId());
//                    if("error".equals((String) poJSON.get("result"))){
//                        System.out.println((String) poJSON.get("message"));
//                    } else {
//                        poJSON = poInvSerial.updateRecord();
//                        if("error".equals((String) poJSON.get("result"))){
//                            System.out.println((String) poJSON.get("message"));
//                        } 
//                    }
//                    
//                    //Inventory Serial Registration
//                    if((!paOthers.get(lnRow).getConductionStickerNo().equals("") || paOthers.get(lnRow).getConductionStickerNo() != null) ||
//                            (!paOthers.get(lnRow).getPlateNo().equals("") || paOthers.get(lnRow).getPlateNo() != null)) {
//                        poJSON = poInvSerialRegistration.openRecord(paOthers.get(lnRow).getSerialId());
//                        if("error".equals((String) poJSON.get("result"))){
//                            poJSON = poInvSerialRegistration.newRecord();
//                            if("error".equals((String) poJSON.get("result"))){
//                                System.out.println((String) poJSON.get("message"));
//                            } 
//                        } else {
//                            poJSON = poInvSerialRegistration.updateRecord();
//                            if("error".equals((String) poJSON.get("result"))){
//                                System.out.println((String) poJSON.get("message"));
//                            } 
//                        } 
//                    } 
//                }
//                
//                //3. Set value to Inventory Serial / Registration 
//                if(poInvSerial.getEditMode() == EditMode.ADDNEW || poInvSerial.getEditMode() == EditMode.UPDATE){
//                    poInvSerial.setLocation(paOthers.get(lnRow).getLocationId());
//                    poInvSerial.setStockId(paOthers.get(lnRow).getStockId());
//                    poInvSerial.setSerial01(paOthers.get(lnRow).getSerial01());
//                    poInvSerial.setSerial02(paOthers.get(lnRow).getSerial02());
//                    
//                    if(poInvSerial.getEditMode() == EditMode.ADDNEW){
//                        poInvSerial.setBranchCode(poGRider.getBranchCode());
//                        poInvSerial.setCompnyId(Master().getCompanyId());
//                    }
//                    
//                    //3.1 Save Inventory Serial 
//                    poInvSerial.setModifiedDate(poGRider.getServerDate());
//                    poJSON = poInvSerial.saveRecord();
//                    if("error".equals((String) poJSON.get("result"))){
//                        System.out.println((String) poJSON.get("message"));
//                    }
//                }
//                
//                if(poInvSerialRegistration.getEditMode() == EditMode.ADDNEW || poInvSerialRegistration.getEditMode() == EditMode.UPDATE){
//                    if((!paOthers.get(lnRow).getConductionStickerNo().equals("") || paOthers.get(lnRow).getConductionStickerNo() != null) ||
//                            (!paOthers.get(lnRow).getPlateNo().equals("") || paOthers.get(lnRow).getPlateNo() != null)) {
//                        //Inventory Serial Registration
//                        poInvSerialRegistration.setSerialId(poInvSerial.getSerialId());
//                        poInvSerialRegistration.setPlateNoP(paOthers.get(lnRow).getPlateNo());
//                        poInvSerialRegistration.setConductionStickerNo(paOthers.get(lnRow).getConductionStickerNo());
//                        
//                        //3.2 Save Inventory Serial Registration
//                        poJSON = poInvSerialRegistration.saveRecord();
//                        if("error".equals((String) poJSON.get("result"))){
//                            System.out.println((String) poJSON.get("message"));
//                        }
//                    }
//                }
//                
//                if(paOthers.get(lnRow).getSerialId().equals("") || paOthers.get(lnRow).getSerialId() == null){
//                    //Set saved serial id in POR Serial
//                    paOthers.get(lnRow).setSerialId(poInvSerial.getSerialId());
//                }
//                
//                System.out.println("edit mode" + paOthers.get(lnRow).getEditMode());
//                //4. Save Purchase Receiving Serial
//                poJSON = paOthers.get(lnRow).saveRecord();
//                if("error".equals((String) poJSON.get("result"))){
//                    return poJSON;
//                } 
//            }
//
////            //Save Inventory Ledger when transaction is confirmed
////            if(Master().getTransactionStatus().equals(PurchaseOrderReceivingStatus.CONFIRMED)){
////                poJSON = poInvSerialLedger.openRecord(paOthers.get(lnRow).getSerialId()); 
////                //if it does not exist in Serial Ledger, generate new entry for serial ledger
////                if("error".equals((String) poJSON.get("result"))){
////                    poInvSerialLedger.newRecord();
////                    poInvSerialLedger.setSerialId(paOthers.get(lnRow).getSerialId());
////                    poInvSerialLedger.setBranchCode(poGRider.getBranchCode());
////                    poInvSerialLedger.setLedgerNo(lnRow);
////                    poInvSerialLedger.setTransactionDate(Master().getTransactionDate());
////                    poInvSerialLedger.setSourceNo(Master().getTransactionNo());
////                    poInvSerialLedger.setSourceCode(SOURCE_CODE);
////                    poInvSerialLedger.setSoldStatus(paOthers.get(lnRow).InventorySerial().getSoldStatus());
////                    
////                    poJSON = poInvSerialLedger.saveRecord();
////                    if("error".equals((String) poJSON.get("result"))){
////                        System.out.println((String) poJSON.get("message"));
////                    } 
////                } 
////            
////            }
//        } catch (SQLException ex) {
//            Logger.getLogger(PurchaseOrderReceiving.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (GuanzonException ex) {
//            Logger.getLogger(PurchaseOrderReceiving.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        poJSON.put("result", "success");
//        return poJSON;
//    }
    
//    @Override
//    public JSONObject saveOthers() {
//        /*Only modify this if there are other tables to modify except the master and detail tables*/
//        poJSON = new JSONObject();
//        int lnCtr, lnRow;
//        List<String> lsPrevSerial = new ArrayList<>();
//        List<String> lsNewSerial = new ArrayList<>();
//        
//        try {
////            //Save Inventory Serial
////            for(lnCtr = 0; lnCtr <= getInventorySerialCount() - 1; lnCtr++ ){
////                //Store initial serial number
////                if( paInvSerial.get(lnCtr).getEditMode() == EditMode.ADDNEW){
////                    lsPrevSerial.add(paInvSerial.get(lnCtr).getSerialId());
////                    lsNewSerial.add(paInvSerial.get(lnCtr).getSerialId());
////                }
////                
////                paInvSerial.get(lnCtr).setModifiedDate(poGRider.getServerDate());
////                poJSON = paInvSerial.get(lnCtr).saveRecord();
////                if("error".equals((String) poJSON.get("result"))){
////                    System.out.println("ERROR: " + (String) poJSON.get("message"));
////                    return poJSON;
////                } 
////                
////                //Update new serial number
////                lsNewSerial.set(lsNewSerial.size()-1, paInvSerial.get(lnCtr).getSerialId());
////            }
////        
////            //Save Purchase Order Receiving Serial
////            for(lnRow = 0; lnRow <= getPurchaseOrderReceivingSerialCount() - 1; lnRow++){
////                //Update serial ID of POR Serial
////                for(lnCtr = 0; lnCtr <= lsPrevSerial.size()-1; lnCtr++){
////                    if(!lsPrevSerial.get(lnCtr).equals(lsNewSerial.get(lnCtr))){
////                        if(lsPrevSerial.equals(paOthers.get(lnRow).getSerialId())){
////                            paOthers.get(lnRow).setSerialId(lsNewSerial.get(lnCtr));
////                            lsPrevSerial.remove(lnCtr);
////                            lsNewSerial.remove(lnCtr);
////                            break;
////                        }
////                    }
////                }
////                
////                paOthers.get(lnRow).setModifiedDate(poGRider.getServerDate());
////                poJSON = paOthers.get(lnRow).saveRecord();
////                if("error".equals((String) poJSON.get("result"))){
////                    return poJSON;
////                } 
////            }
//        
//            //Save Purchase Order Receiving Serial
//            for(lnRow = 0; lnRow <= getPurchaseOrderReceivingSerialCount() - 1; lnRow++){
//                if(paOthers.get(lnRow).getSerialId().equals("") || paOthers.get(lnRow).getSerialId() == null){
//                    //Save Inventory Serial
//                    poJSON = poInvSerial.newRecord();
//                    if("error".equals((String) poJSON.get("result"))){
//                        return poJSON; //TODO
//                    } else {
//                        poInvSerial.setBranchCode(poGRider.getBranchCode());
//                        poInvSerial.setCompnyId(Master().getCompanyId());
//                        poInvSerial.setLocation(paOthers.get(lnRow).getLocationId());
//                        poInvSerial.setStockId(paOthers.get(lnRow).getStockId());
//                        poInvSerial.setSerial01(paOthers.get(lnRow).getSerial01());
//                        poInvSerial.setSerial02(paOthers.get(lnRow).getSerial02());
//                    }
//                    
//                    poInvSerial.setModifiedDate(poGRider.getServerDate());
//                    poJSON = poInvSerial.saveRecord();
//                    if("error".equals((String) poJSON.get("result"))){
////                        return poJSON; //TODO
//                    }
//                        
//                    //Inventory Serial Registration
//                    if((!paOthers.get(lnRow).getConductionStickerNo().equals("") || paOthers.get(lnRow).getConductionStickerNo() != null) ||
//                            (!paOthers.get(lnRow).getPlateNo().equals("") || paOthers.get(lnRow).getPlateNo() != null)) {
//                        poJSON = poInvSerialRegistration.newRecord();
//                        if("error".equals((String) poJSON.get("result"))){
//                            return poJSON; //TODO
//                        } else {
//                            //Inventory Serial
//                            poInvSerialRegistration.setSerialId(poInvSerial.getSerialId());
//                            poInvSerialRegistration.setPlateNoH(paOthers.get(lnRow).getPlateNo());
////                            poInvSerialRegistration.setConductionSticker(paOthers.get(lnRow).getConductionSticker());
//                        }
//                        
//                        poJSON = poInvSerialRegistration.saveRecord();
//                        if("error".equals((String) poJSON.get("result"))){
////                            return poJSON; //TODO
//                        }
//                    }
//                    
//                    paOthers.get(lnRow).setSerialId(poInvSerial.getSerialId());
//                }
//                
//                //Update Inventory Serial
//                paOthers.get(lnRow).setModifiedDate(poGRider.getServerDate());
//                poJSON = paOthers.get(lnRow).saveRecord();
//                if("error".equals((String) poJSON.get("result"))){
//                    return poJSON;
//                } 
//            }
//
//            //Save Inventory Ledger
//        
//        } catch (SQLException ex) {
//            Logger.getLogger(PurchaseOrderReceiving.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (GuanzonException ex) {
//            Logger.getLogger(PurchaseOrderReceiving.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        poJSON.put("result", "success");
//        return poJSON;
//    }
//    
    @Override
    public JSONObject saveOthers() {
        /*Only modify this if there are other tables to modify except the master and detail tables*/
        poJSON = new JSONObject();
        int lnCtr, lnRow;
        
        try {
            
            InvSerial loInvSerial = new InvControllers(poGRider, logwrapr).InventorySerial();
            loInvSerial.initialize();
            loInvSerial.setWithParentClass(true);
            
            //Purchase Order Receiving Serial
            for(lnRow = 0; lnRow <= getPurchaseOrderReceivingSerialCount() - 1; lnRow++){
                
                //1. Check for Serial ID
                if(paOthers.get(lnRow).getSerialId().equals("") || paOthers.get(lnRow).getSerialId() == null){
                    //1.1 Create New Inventory Serial
                    poJSON = loInvSerial.newRecord();
                    if("error".equals((String) poJSON.get("result"))){
                        return poJSON; //TODO
                    } 
                    
                } else {
                    //1.2 Update Inventory Serial / Registration//Create New Inventory Serial
                    poJSON = loInvSerial.openRecord(paOthers.get(lnRow).getSerialId());
                    if("error".equals((String) poJSON.get("result"))){
                        return poJSON; //TODO
                    } 
                }
                
                //2. Update values for serial
                if(loInvSerial.getEditMode() == EditMode.ADDNEW || loInvSerial.getEditMode() == EditMode.UPDATE){
                    loInvSerial.getModel().setStockId(paOthers.get(lnRow).getStockId());
                    loInvSerial.getModel().setSerial01(paOthers.get(lnRow).getSerial01());
                    loInvSerial.getModel().setSerial02(paOthers.get(lnRow).getSerial02());
                    loInvSerial.getModel().setLocation(paOthers.get(lnRow).getLocationId());
                    
                    //2.1 Only set branch code and company id during creation of serial in por
                    if(loInvSerial.getEditMode() == EditMode.ADDNEW){
                        loInvSerial.getModel().setBranchCode(poGRider.getBranchCode());
                        loInvSerial.getModel().setCompnyId(Master().getCompanyId());
                    }
                    
                    if((!paOthers.get(lnRow).getConductionStickerNo().equals("") || paOthers.get(lnRow).getConductionStickerNo() != null) ||
                        (!paOthers.get(lnRow).getPlateNo().equals("") || paOthers.get(lnRow).getPlateNo() != null)) {
                        loInvSerial.SerialRegistration().setConductionStickerNo(paOthers.get(lnRow).getConductionStickerNo());
                        loInvSerial.SerialRegistration().setPlateNoP(paOthers.get(lnRow).getPlateNo());
                    }
                    
                    //3. Save Inventory Serial
                    poJSON = loInvSerial.saveRecord();
                    if("error".equals((String) poJSON.get("result"))){
                        System.out.println("inv serial" + (String) poJSON.get("message"));
                    }
                }
                
                //4. Set serial id to por serial
                if(paOthers.get(lnRow).getSerialId().equals("") || paOthers.get(lnRow).getSerialId() == null){
                    paOthers.get(lnRow).setSerialId(loInvSerial.getModel().getSerialId()); 
                }
                
                //5. Save Purchase Order Receiving Serial
                paOthers.get(lnRow).setModifiedDate(poGRider.getServerDate());
                poJSON = paOthers.get(lnRow).saveRecord();
                if("error".equals((String) poJSON.get("result"))){
                    return poJSON;
                } 
            }
            
            //Pending development need to clarify the approval
            if(Master().getTransactionStatus().equals(PurchaseOrderReceivingStatus.CONFIRMED)){
                poJSON = updatePurchaseOrder(Master().getTransactionStatus(), false);
                if (!"success".equals((String) poJSON.get("result"))) return poJSON;

                //Update Purchase Order, Serial Ledger, Inventory
                poJSON = updateOthers();
                if (!"success".equals((String) poJSON.get("result"))) return poJSON;
            }
            
        } catch (SQLException ex) {
            Logger.getLogger(PurchaseOrderReceiving.class.getName()).log(Level.SEVERE, null, ex);
        } catch (GuanzonException ex) {
            Logger.getLogger(PurchaseOrderReceiving.class.getName()).log(Level.SEVERE, null, ex);
        } catch (CloneNotSupportedException ex) {
            Logger.getLogger(PurchaseOrderReceiving.class.getName()).log(Level.SEVERE, null, ex);
        }
        poJSON.put("result", "success");
        return poJSON;
    }
    
    private PurchaseOrder PurchaseOrder(){
        return new PurchaseOrderControllers(poGRider, logwrapr).PurchaseOrder();
    }
    
    private JSONObject updatePurchaseOrder(String status, boolean isUpdateStatus) throws CloneNotSupportedException, SQLException, GuanzonException{
        poJSON = new JSONObject();
        int lnCtr, lnRow, lnList;
        int lnRecQty = 0;
        boolean lbExist = false;
        boolean lbSkip = false;
        
        //Update Purchase Order 
        for (lnCtr = 0; lnCtr <= getDetailCount()-1; lnCtr++){
            System.out.println("Detail(lnCtr).getOrderNo() : " + lnCtr + " : "  + Detail(lnCtr).getOrderNo());
            if(Detail(lnCtr).getOrderNo() != null && !Detail(lnCtr).getOrderNo().equals("")){
                
                //1. Check for discrepancy
                if(Detail(lnCtr).getOrderQty().intValue() != Detail(lnCtr).getQuantity().intValue()){
                    if(isUpdateStatus){
                        System.out.println("Require Approval");
                        if(!pbApproval){
                            poJSON = ShowDialogFX.getUserApproval(poGRider);
                            if (!"success".equals((String) poJSON.get("result"))) return poJSON;
                            pbApproval = true; //set value into True since user approval already called.
                        }
                    }
                }
                
                //2.check if order no is already exist in purchase order array list
                for(lnRow= 0;lnRow <= paPurchaseOrder.size() - 1; lnRow++){
                    System.out.println("paPurchaseOrder.get(lnRow).Master().getTransactionNo() : " + paPurchaseOrder.get(lnRow).Master().getTransactionNo());
                    if(paPurchaseOrder.get(lnRow).Master().getTransactionNo() != null){
                        if( paPurchaseOrder.get(lnRow).Master().getTransactionNo().equals(Detail(lnCtr).getOrderNo())){
                            lbExist = true; 
                            lbSkip = false;
                            break;
                        }
                    } 
                }
                
                //3. If order no is not exist add it on puchase order array list then open the transaction
                if(!lbExist){
                    paPurchaseOrder.add(PurchaseOrder());
                    paPurchaseOrder.get(paPurchaseOrder.size() - 1).InitTransaction();
                    paPurchaseOrder.get(paPurchaseOrder.size() - 1).OpenTransaction(Detail(lnCtr).getOrderNo());
                    paPurchaseOrder.get(paPurchaseOrder.size() - 1).UpdateTransaction();
                    lnList = paPurchaseOrder.size() - 1;
                } else {
                    //if already exist, get the row no of purchase order
                    lnList = lnRow;
                }

                for(lnRow = 0; lnRow <= paPurchaseOrder.get(lnList).getDetailCount()-1; lnRow++){
                    if(Detail(lnCtr).getStockId().equals(paPurchaseOrder.get(lnList).Detail(lnRow).getStockID())){
                        //Get total received qty from other po receiving entry
                        lnRecQty = getReceivedQty(Detail(lnRow).getOrderNo(), Detail(lnRow).getStockId());

                        switch(status){
                            case PurchaseOrderReceivingStatus.CONFIRMED:
                            case PurchaseOrderReceivingStatus.APPROVED: 
                                //Add received qty in po receiving
                                lnRecQty = lnRecQty + Detail(lnCtr).getQuantity().intValue();

                                if(!paPurchaseOrder.get(lnList).Detail(lnRow).getQuantity().equals(lnRecQty)){
                                    pbApproval = false;
                                }
                                break;
                            case PurchaseOrderReceivingStatus.RETURNED: 
                                //Deduct received qty in po receiving
                                lnRecQty = lnRecQty - Detail(lnCtr).getQuantity().intValue();
                                break;
                        }
                        //set Receive qty in Purchase Order detail
                        paPurchaseOrder.get(lnList).Detail(lnRow).setReceivedQuantity(lnRecQty);
                        paPurchaseOrder.get(lnList).Detail(lnRow).setModifiedDate(poGRider.getServerDate());
                        break;
                    }
                }
            }

            //Reset variable
            lnRecQty = 0;
            lbExist = false;
            lbSkip = false;
        }
        poJSON.put("result", "success");
        return poJSON;
    }
    
    //Open record for checking total receive qty per purchase order
    private int getReceivedQty(String orderNo, String stockId) throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        int lnRecQty = 0;
        String lsSQL =    " SELECT "
                        + " b.nQuantity AS nQuantity "
                        + " FROM po_receiving_master a "
                        + " LEFT JOIN po_receiving_detail b ON b.sTransNox = a.sTransNox ";
        lsSQL = MiscUtil.addCondition(lsSQL, " a.sTransNox <> " + SQLUtil.toSQL(Master().getTransactionNo()) 
                                    + " AND b.sOrderNox = " + SQLUtil.toSQL(orderNo) 
                                    + " AND b.sStockIDx = " + SQLUtil.toSQL(stockId)
                                    + " AND ( a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderReceivingStatus.CONFIRMED)
                                    + " OR a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderReceivingStatus.APPROVED)
                                    + " ) ");
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        try {
            if (MiscUtil.RecordCount(loRS) >= 0) {
                while (loRS.next()) {
                    lnRecQty = lnRecQty + loRS.getInt("nQuantity");
                } 
            }
            MiscUtil.close(loRS);
        } catch (SQLException e) {
            System.out.println("No record loaded.");
            lnRecQty = 0;
        } 
        return lnRecQty;
    }
    
     private JSONObject updateOthers() {
        /*Only modify this if there are other tables to modify except the master and detail tables*/
        poJSON = new JSONObject();
        int lnCtr, lnRow;
        boolean lbProcessed = true;
        try {
            //Update Purchase Order exist in PO Receiving Detail 
            for(lnCtr = 0; lnCtr <= paPurchaseOrder.size()-1; lnCtr++){
                //Check Order qty vs Received qty 
                for(lnRow = 0; lnRow <= paPurchaseOrder.get(lnCtr).getDetailCount()-1; lnRow++){
                    if(paPurchaseOrder.get(lnCtr).Detail(lnRow).getQuantity().intValue() > paPurchaseOrder.get(lnCtr).Detail(lnRow).getReceivedQuantity().intValue()){
                       lbProcessed = false;
                       break;
                    } 
                }
                
                paPurchaseOrder.get(lnCtr).Master().setProcessed(lbProcessed);
                paPurchaseOrder.get(lnCtr).Master().setModifiedDate(poGRider.getServerDate());
                poJSON = paPurchaseOrder.get(lnCtr).SaveTransaction();
                if("error".equals((String) poJSON.get("result"))){
                    System.out.println("Purchase Order Saving " + (String) poJSON.get("message"));
                }
            }
            
            if(Master().getTransactionStatus().equals(PurchaseOrderReceivingStatus.CONFIRMED) ||
                    Master().getTransactionStatus().equals(PurchaseOrderReceivingStatus.APPROVED)){
                //Save Inventory Serial Ledger TODO
                InvSerial loInvSerial = new InvControllers(poGRider, logwrapr).InventorySerial();
                loInvSerial.initialize();
                loInvSerial.setWithParentClass(true);
    //            InventoryTrans.POReceiving();
            }
            
        } catch (SQLException ex) {
            Logger.getLogger(PurchaseOrderReceiving.class.getName()).log(Level.SEVERE, null, ex);
        } catch (GuanzonException ex) {
            Logger.getLogger(PurchaseOrderReceiving.class.getName()).log(Level.SEVERE, null, ex);
        }
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
                    + " , a.sSupplier  "
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
    
    public JSONObject printRecord() {
        poJSON = new JSONObject();
        String watermarkPath = "D:\\GGC_Maven_Systems\\Reports\\images\\draft.png"; //set draft as default
        try {
            // 1. Prepare parameters
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("sBranchNm", poGRider.getBranchName());
            parameters.put("sAddressx", poGRider.getAddress());
            parameters.put("sCompnyNm", poGRider.getClientName());
            parameters.put("sTransNox", Master().getTransactionNo());
            parameters.put("dReferDte", Master().getReferenceDate());
            parameters.put("sReferNox", Master().getReferenceNo());
            parameters.put("sApprval1", "Jane Smith");
            parameters.put("sApprval2", "Mike Johnson");
            parameters.put("sApprval3", "Sarah Williams");
            parameters.put("sRemarks", Master().getRemarks());
            parameters.put("dTransDte", new java.sql.Date(Master().getTransactionDate().getTime()));
            parameters.put("dDatexxx", new java.sql.Date(poGRider.getServerDate().getTime()));

            // Set watermark based on approval status
            switch(Master().getTransactionStatus()){
                case PurchaseOrderReceivingStatus.APPROVED: 
                    watermarkPath = "D:\\GGC_Maven_Systems\\Reports\\images\\approved.png";
                    break;
                case PurchaseOrderReceivingStatus.POSTED: 
                    watermarkPath = "D:\\GGC_Maven_Systems\\Reports\\images\\posted.png";
                    break;
                case PurchaseOrderReceivingStatus.CANCELLED: 
                    watermarkPath = "D:\\GGC_Maven_Systems\\Reports\\images\\cancelled.png";
                    break;
            }
            
            parameters.put("watermarkImagePath", watermarkPath);
            List<OrderDetail> orderDetails = new ArrayList<>();

            double lnTotal = 0.0;
            for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
                lnTotal = Detail(lnCtr).getUnitPrce().doubleValue() * Detail(lnCtr).getQuantity().intValue();
                orderDetails.add(new OrderDetail(lnCtr, String.valueOf(Detail(lnCtr).getOrderNo()), Detail(lnCtr).Inventory().getBarCode(), Detail(lnCtr).Inventory().getDescription(), Detail(lnCtr).getUnitPrce().doubleValue(), Detail(lnCtr).getQuantity().intValue(), lnTotal));
            }

            // 3. Create data source
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(orderDetails);

            // 4. Compile and fill report
            String jrxmlPath = "D:\\GGC_Maven_Systems\\Reports\\PurchaseOrderReceiving.jrxml"; //TODO
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
            Logger.getLogger(PurchaseOrderReceiving.class.getName()).log(Level.SEVERE, null, ex);
        } catch (GuanzonException ex) {
            Logger.getLogger(PurchaseOrderReceiving.class.getName()).log(Level.SEVERE, null, ex);
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
