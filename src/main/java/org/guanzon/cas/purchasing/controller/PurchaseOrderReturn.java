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
import org.guanzon.cas.inv.InvSerial;
import org.guanzon.cas.inv.InventoryTransaction;
import org.guanzon.cas.inv.services.InvControllers;
import org.guanzon.cas.purchasing.model.Model_POReturn_Detail;
import org.guanzon.cas.purchasing.model.Model_POReturn_Master;
import org.guanzon.cas.purchasing.services.PurchaseOrderReceivingControllers;
import org.guanzon.cas.purchasing.services.PurchaseOrderReturnControllers;
import org.guanzon.cas.purchasing.services.PurchaseOrderReturnModels;
import org.guanzon.cas.purchasing.status.PurchaseOrderReturnStatus;
import org.guanzon.cas.purchasing.status.PurchaseOrderReturnStatus;
import org.guanzon.cas.purchasing.validator.PurchaseOrderReturnValidatorFactory;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

/**
 *
 * @author Arsiela 04-28-2025
 */
public class PurchaseOrderReturn extends Transaction{

    private boolean pbApproval = false;
    private boolean pbIsPrint = false;
    private String psIndustryId = "";
    private String psCompanyId = "";
    private String psCategorCd = "";
    
    List<Model_POReturn_Master> paPORMaster;
    List<InventoryTransaction> paInventoryTransaction;
    List<Model> paDetailRemoved;
    
    public JSONObject InitTransaction() {
        SOURCE_CODE = "PRet";

        poMaster = new PurchaseOrderReturnModels(poGRider).PurchaseOrderReturnMaster();
        poDetail = new PurchaseOrderReturnModels(poGRider).PurchaseOrderReturnDetails();

        paDetail = new ArrayList<>();
        paDetailRemoved = new ArrayList<>();
        paInventoryTransaction = new ArrayList<>();

        psIndustryId = poGRider.getIndustry();

        return initialize();
    }
    
    public JSONObject NewTransaction()
            throws CloneNotSupportedException {
        return newTransaction();
    }

    public JSONObject SaveTransaction()
            throws SQLException,
            GuanzonException,
            CloneNotSupportedException {
        return saveTransaction();
    }

    public JSONObject OpenTransaction(String transactionNo)
            throws CloneNotSupportedException,
            SQLException,
            GuanzonException {
            //Clear data
            resetMaster();
            Detail().clear();
        return openTransaction(transactionNo);
    }

    public JSONObject UpdateTransaction() {
        return updateTransaction();
    }

    public JSONObject ConfirmTransaction(String remarks)
            throws ParseException,
            SQLException,
            GuanzonException,
            CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = PurchaseOrderReturnStatus.CONFIRMED;
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
        poJSON = isEntryOkay(PurchaseOrderReturnStatus.CONFIRMED);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        //Set receive qty to Purchase Order
        poJSON = setValueToOthers(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        if (pbApproval) {
            poJSON = ShowDialogFX.getUserApproval(poGRider);
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
        }

        poGRider.beginTrans("UPDATE STATUS", "ConfirmTransaction", SOURCE_CODE, Master().getTransactionNo());

        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbConfirm, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        //Update Inventory, Serial Ledger
        poJSON = saveUpdateOthers(PurchaseOrderReturnStatus.CONFIRMED);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        poGRider.commitTrans();

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        if (lbConfirm) {
            poJSON.put("message", "Transaction confirmed successfully.");
        } else {
            poJSON.put("message", "Transaction confirmation request submitted successfully.");
        }

        return poJSON;
    }

    public JSONObject ReturnTransaction(String remarks)
            throws ParseException,
            SQLException,
            GuanzonException,
            CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = PurchaseOrderReturnStatus.RETURNED;
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
        poJSON = isEntryOkay(PurchaseOrderReturnStatus.RETURNED);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        if (PurchaseOrderReturnStatus.CONFIRMED.equals(Master().getTransactionStatus())) {
            poJSON = ShowDialogFX.getUserApproval(poGRider);
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
            
            //Set receive qty to Purchase Order
            poJSON = setValueToOthers(lsStatus);
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
        }

        poGRider.beginTrans("UPDATE STATUS", "ReturnTransaction", SOURCE_CODE, Master().getTransactionNo());

        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbReturn, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }
        
        if (PurchaseOrderReturnStatus.CONFIRMED.equals(Master().getTransactionStatus())) {
            //Update Inventory, Serial Ledger
            poJSON = saveUpdateOthers(PurchaseOrderReturnStatus.CONFIRMED);
            if (!"success".equals((String) poJSON.get("result"))) {
                poGRider.rollbackTrans();
                return poJSON;
            }
        }

        poGRider.commitTrans();

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        if (lbReturn) {
            poJSON.put("message", "Transaction returned successfully.");
        } else {
            poJSON.put("message", "Transaction return request submitted successfully.");
        }

        return poJSON;
    }

    public JSONObject PostTransaction(String remarks)
            throws ParseException,
            SQLException,
            GuanzonException,
            CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = PurchaseOrderReturnStatus.POSTED;
        boolean lbPosted = true;

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
        poJSON = isEntryOkay(PurchaseOrderReturnStatus.POSTED);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbPosted);

        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        if (lbPosted) {
            poJSON.put("message", "Transaction posted successfully.");
        } else {
            poJSON.put("message", "Transaction posting request submitted successfully.");
        }

        return poJSON;
    }

    public JSONObject CancelTransaction(String remarks)
            throws ParseException,
            SQLException,
            GuanzonException,
            CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = PurchaseOrderReturnStatus.CANCELLED;
        boolean lbCancelled = true;

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
        poJSON = isEntryOkay(PurchaseOrderReturnStatus.CANCELLED);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
        if (PurchaseOrderReturnStatus.CONFIRMED.equals(Master().getTransactionStatus())) {
            poJSON = ShowDialogFX.getUserApproval(poGRider);
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
            
            //update Purchase Order
            poJSON = setValueToOthers(lsStatus);
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
        }

        poGRider.beginTrans("UPDATE STATUS", "CancelledTransaction", SOURCE_CODE, Master().getTransactionNo());

        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbCancelled, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }
        
        if (PurchaseOrderReturnStatus.CONFIRMED.equals(Master().getTransactionStatus())) {
            //Update Serial Ledger, Inventory
            poJSON = saveUpdateOthers(PurchaseOrderReturnStatus.CONFIRMED);
            if (!"success".equals((String) poJSON.get("result"))) {
                poGRider.rollbackTrans();
                return poJSON;
            }
        }

        poGRider.commitTrans();

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        if (lbCancelled) {
            poJSON.put("message", "Transaction cancelled successfully.");
        } else {
            poJSON.put("message", "Transaction cancellation request submitted successfully.");
        }

        return poJSON;
    }

    public JSONObject VoidTransaction(String remarks)
            throws ParseException,
            SQLException,
            GuanzonException,
            CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = PurchaseOrderReturnStatus.VOID;
        boolean lbVoid = true;

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
        poJSON = isEntryOkay(PurchaseOrderReturnStatus.VOID);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        if (PurchaseOrderReturnStatus.CONFIRMED.equals(Master().getTransactionStatus())) {
            poJSON = ShowDialogFX.getUserApproval(poGRider);
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
            
            //update Purchase Order
            poJSON = setValueToOthers(lsStatus);
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
        }

        poGRider.beginTrans("UPDATE STATUS", "VoidTransaction", SOURCE_CODE, Master().getTransactionNo());

        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbVoid, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }
        
        if (PurchaseOrderReturnStatus.CONFIRMED.equals(Master().getTransactionStatus())) {
            //Update Serial Ledger, Inventory
            poJSON = saveUpdateOthers(PurchaseOrderReturnStatus.CONFIRMED);
            if (!"success".equals((String) poJSON.get("result"))) {
                poGRider.rollbackTrans();
                return poJSON;
            }
        }

        poGRider.commitTrans();

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        if (lbVoid) {
            poJSON.put("message", "Transaction voided successfully.");
        } else {
            poJSON.put("message", "Transaction voiding request submitted successfully.");
        }

        return poJSON;
    }
    
    public JSONObject searchTransaction()
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

        initSQL();
        String lsSQL = MiscUtil.addCondition(SQL_BROWSE, " a.sIndstCdx = " + SQLUtil.toSQL(psIndustryId)
                + " AND a.sCompnyID = " + SQLUtil.toSQL(psCompanyId)
                + " AND a.sCategrCd = " + SQLUtil.toSQL(psCategorCd)
                + " AND a.sSupplier LIKE " + SQLUtil.toSQL("%" + Master().getSupplierId()));
        if (psTranStat != null && !"".equals(psTranStat)) {
            lsSQL = lsSQL + lsTransStat;
        }

        System.out.println("Executing SQL: " + lsSQL);
        poJSON = ShowDialogFX.Browse(poGRider,
                lsSQL,
                "",
                "Transaction Date»Transaction No»Industry»Company»Supplier",
                "dTransact»sTransNox»sIndustry»sCompnyNm»sSupplrNm",
                "a.dTransact»a.sTransNox»d.sDescript»c.sCompnyNm»b.sCompnyNm",
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
    
    public JSONObject searchTransaction(String industryId, String companyId, String supplierId, String sReferenceNo)
            throws CloneNotSupportedException,
            SQLException,
            GuanzonException {
        if(supplierId == null){
            supplierId = "";
        }
        if(sReferenceNo == null){
            sReferenceNo = "";
        }
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
        String lsSQL = MiscUtil.addCondition(SQL_BROWSE, " a.sIndstCdx = " + SQLUtil.toSQL(industryId)
                + " AND a.sCompnyID = " + SQLUtil.toSQL(companyId)
                + " AND a.sSupplier LIKE " + SQLUtil.toSQL("%" + supplierId)
                + " AND a.sTransNox LIKE " + SQLUtil.toSQL("%" + sReferenceNo));
        if (psTranStat != null && !"".equals(psTranStat)) {
            lsSQL = lsSQL + lsTransStat;
        }

        System.out.println("Executing SQL: " + lsSQL);
        poJSON = ShowDialogFX.Browse(poGRider,
                lsSQL,
                "",
                "Transaction Date»Transaction No»Industry»Company»Supplier",
                "dTransact»sTransNox»sIndustry»sCompnyNm»sSupplrNm",
                "a.dTransact»a.sTransNox»d.sDescript»c.sCompnyNm»b.sCompnyNm",
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

    public void setIndustryId(String industryId) {
        psIndustryId = industryId;
    }

    public void setCompanyId(String companyId) {
        psCompanyId = companyId;
    }

    public void setCategoryId(String categoryId) {
        psCategorCd = categoryId;
    }

    public void resetMaster() {
        poMaster = new PurchaseOrderReturnModels(poGRider).PurchaseOrderReturnMaster();
    }
    
    @Override
    public String getSourceCode() {
        return SOURCE_CODE;
    }

    @Override
    public Model_POReturn_Master Master() {
        return (Model_POReturn_Master) poMaster;
    }

    @Override
    public Model_POReturn_Detail Detail(int row) {
        return (Model_POReturn_Detail) paDetail.get(row);
    }
    
    @Override
    public int getDetailCount() {
        if (paDetail == null) {
            paDetail = new ArrayList<>();
        }

        return paDetail.size();
    }
    
    public JSONObject AddDetail()
            throws CloneNotSupportedException {
        poJSON = new JSONObject();

        if (getDetailCount() > 0) {
            if (Detail(getDetailCount() - 1).getStockId() != null) {
                if (Detail(getDetailCount() - 1).getStockId().isEmpty()) {
                    poJSON.put("result", "error");
                    poJSON.put("message", "Last row has empty item.");
                    return poJSON;
                }
            }
        }

        return addDetail();
    }

    public int getDetailRemovedCount() {
        if (paDetailRemoved == null) {
            paDetailRemoved = new ArrayList<>();
        }

        return paDetailRemoved.size();
    }

    public Model_POReturn_Detail DetailRemove(int row) {
        return (Model_POReturn_Detail) paDetailRemoved.get(row);
    }
    
    public JSONObject SearchSupplier(String value, boolean byCode)
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();

        Client object = new ClientControllers(poGRider, logwrapr).Client();
        object.Master().setRecordStatus(RecordStatus.ACTIVE);
        object.Master().setClientType("1");
        poJSON = object.Master().searchRecord(value, byCode);
        if ("success".equals((String) poJSON.get("result"))) {
            Master().setSupplierId(object.Master().getModel().getClientId());
            Master().setAddressId(object.ClientAddress().getModel().getAddressId()); //TODO
            Master().setContactId(object.ClientInstitutionContact().getModel().getClientId()); //TODO
        }

        return poJSON;
    }
    
    public JSONObject SearchPOReceiving(String value, boolean byCode)
            throws SQLException,
            GuanzonException,
            CloneNotSupportedException {
        poJSON = new JSONObject();

        PurchaseOrderReceiving object = new PurchaseOrderReceivingControllers(poGRider, logwrapr).PurchaseOrderReceiving();
        object.InitTransaction();
        object.setTransactionStatus(PurchaseOrderReturnStatus.CONFIRMED + "" + PurchaseOrderReturnStatus.PAID + "" + PurchaseOrderReturnStatus.POSTED);
        
        if(byCode){
            poJSON = object.searchTransaction(psIndustryId, psCompanyId, psCategorCd, Master().getSupplierId(), null, value);
        } else {
            poJSON = object.searchTransaction(psIndustryId, psCompanyId, psCategorCd, Master().getSupplierId(), value, null);
        }
        
        if ("success".equals((String) poJSON.get("result"))) {
            Master().setSourceNo(object.Master().getTransactionNo());
            Master().setSupplierId(object.Master().getSupplierId());
            Master().setAddressId(object.Master().getAddressId()); 
            Master().setContactId(object.Master().getContactId()); 
        }

        return poJSON;
    }
    
    public JSONObject SearchBarcode(String value, int row)
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        poJSON.put("row", row);
        
        if(Master().getSourceNo() == null || "".equals(Master().getSourceNo())){
            poJSON.put("result", "error");
            poJSON.put("message", "PO Receiving No is not set.");
            return poJSON;
        }
        
        PurchaseOrderReceiving object = new PurchaseOrderReceivingControllers(poGRider, logwrapr).PurchaseOrderReceiving();
        object.InitTransaction();
        poJSON = object.searchBarcodePORDetail(value, Master().getSourceNo());
        poJSON.put("row", row);
        System.out.println("result" + (String) poJSON.get("result"));
        if ("success".equals((String) poJSON.get("result"))) {
//            poJSON = checkExistingStock(object.getDetail().getStockId(), object.getDetail().getBarCode(), "1900-01-01", row, false);
//            if ("error".equals((String) poJSON.get("result"))) {
//                return poJSON;
//            }

            Detail(row).setStockId(object.getDetail().getStockId());
            Detail(row).setUnitPrce(object.getDetail().getUnitPrce().doubleValue());
            Detail(row).setUnitType(object.getDetail().getUnitType());
        }
        return poJSON;
    }
    
    public JSONObject SearchDescription(String value, int row)
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        poJSON.put("row", row);
        
        if(Master().getSourceNo() == null || "".equals(Master().getSourceNo())){
            poJSON.put("result", "error");
            poJSON.put("message", "PO Receiving No is not set.");
            return poJSON;
        }
        
        PurchaseOrderReceiving object = new PurchaseOrderReceivingControllers(poGRider, logwrapr).PurchaseOrderReceiving();
        object.InitTransaction();
        poJSON = object.searchDescriptionPORDetail(value, Master().getSourceNo());
        poJSON.put("row", row);
        System.out.println("result" + (String) poJSON.get("result"));
        if ("success".equals((String) poJSON.get("result"))) {
//            poJSON = checkExistingStock(object.getModel().getStockId(), object.getModel().getBarCode(), "1900-01-01", row, false);
//            if ("error".equals((String) poJSON.get("result"))) {
//                return poJSON;
//            }

            Detail(row).setStockId(object.getDetail().getStockId());
            Detail(row).setUnitPrce(object.getDetail().getUnitPrce().doubleValue());
            Detail(row).setUnitType(object.getDetail().getUnitType());
        }
        return poJSON;
    }
    
    public JSONObject SearchEngineNo(String value, int row)
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        poJSON.put("row", row);
        
        if(Master().getSourceNo() == null || "".equals(Master().getSourceNo())){
            poJSON.put("result", "error");
            poJSON.put("message", "PO Receiving No is not set.");
            return poJSON;
        }
        
        PurchaseOrderReceiving object = new PurchaseOrderReceivingControllers(poGRider, logwrapr).PurchaseOrderReceiving();
        object.InitTransaction();
        poJSON = object.searchEnginePORDetail(value, Master().getSourceNo());
        poJSON.put("row", row);
        System.out.println("result" + (String) poJSON.get("result"));
        if ("success".equals((String) poJSON.get("result"))) {
//            poJSON = checkExistingStock(object.getModel().getStockId(), object.getModel().getBarCode(), "1900-01-01", row, false);
//            if ("error".equals((String) poJSON.get("result"))) {
//                return poJSON;
//            }

            Detail(row).setSerialId(object.getSerial().getSerialId());
            Detail(row).setStockId(object.getSerial().getStockId());
            Detail(row).setUnitPrce(object.getSerial().PurchaseOrderReceivingDetails().getUnitPrce().doubleValue());
            Detail(row).setUnitType(object.getSerial().PurchaseOrderReceivingDetails().getUnitType());
        }
        return poJSON;
    }
    
    public JSONObject SearchFrameNo(String value, int row)
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        poJSON.put("row", row);
        
        if(Master().getSourceNo() == null || "".equals(Master().getSourceNo())){
            poJSON.put("result", "error");
            poJSON.put("message", "PO Receiving No is not set.");
            return poJSON;
        }
        
        PurchaseOrderReceiving object = new PurchaseOrderReceivingControllers(poGRider, logwrapr).PurchaseOrderReceiving();
        object.InitTransaction();
        poJSON = object.searchFramePORDetail(value, Master().getSourceNo());
        poJSON.put("row", row);
        System.out.println("result" + (String) poJSON.get("result"));
        if ("success".equals((String) poJSON.get("result"))) {
//            poJSON = checkExistingStock(object.getModel().getStockId(), object.getModel().getBarCode(), "1900-01-01", row, false);
//            if ("error".equals((String) poJSON.get("result"))) {
//                return poJSON;
//            }

            Detail(row).setSerialId(object.getSerial().getSerialId());
            Detail(row).setStockId(object.getSerial().getStockId());
            Detail(row).setUnitPrce(object.getSerial().PurchaseOrderReceivingDetails().getUnitPrce().doubleValue());
            Detail(row).setUnitType(object.getSerial().PurchaseOrderReceivingDetails().getUnitType());
        }
        return poJSON;
    }
    
    public JSONObject SearchIMEINo(String value, int row)
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        poJSON.put("row", row);
        
        if(Master().getSourceNo() == null || "".equals(Master().getSourceNo())){
            poJSON.put("result", "error");
            poJSON.put("message", "PO Receiving No is not set.");
            return poJSON;
        }
        
        PurchaseOrderReceiving object = new PurchaseOrderReceivingControllers(poGRider, logwrapr).PurchaseOrderReceiving();
        object.InitTransaction();
        poJSON = object.searchImeiPORDetail(value, Master().getSourceNo());
        poJSON.put("row", row);
        System.out.println("result" + (String) poJSON.get("result"));
        if ("success".equals((String) poJSON.get("result"))) {
//            poJSON = checkExistingStock(object.getModel().getStockId(), object.getModel().getBarCode(), "1900-01-01", row, false);
//            if ("error".equals((String) poJSON.get("result"))) {
//                return poJSON;
//            }

            Detail(row).setSerialId(object.getSerial().getSerialId());
            Detail(row).setStockId(object.getSerial().getStockId());
            Detail(row).setUnitPrce(object.getSerial().PurchaseOrderReceivingDetails().getUnitPrce().doubleValue());
            Detail(row).setUnitType(object.getSerial().PurchaseOrderReceivingDetails().getUnitType());
        }
        return poJSON;
    }
    
    public JSONObject SearchPlateNo(String value, int row)
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        poJSON.put("row", row);
        
        if(Master().getSourceNo() == null || "".equals(Master().getSourceNo())){
            poJSON.put("result", "error");
            poJSON.put("message", "PO Receiving No is not set.");
            return poJSON;
        }
        
        PurchaseOrderReceiving object = new PurchaseOrderReceivingControllers(poGRider, logwrapr).PurchaseOrderReceiving();
        object.InitTransaction();
        poJSON = object.searchPlatePORDetail(value, Master().getSourceNo());
        poJSON.put("row", row);
        System.out.println("result" + (String) poJSON.get("result"));
        if ("success".equals((String) poJSON.get("result"))) {
//            poJSON = checkExistingStock(object.getModel().getStockId(), object.getModel().getBarCode(), "1900-01-01", row, false);
//            if ("error".equals((String) poJSON.get("result"))) {
//                return poJSON;
//            }

            Detail(row).setSerialId(object.getSerial().getSerialId());
            Detail(row).setStockId(object.getSerial().getStockId());
            Detail(row).setUnitPrce(object.getSerial().PurchaseOrderReceivingDetails().getUnitPrce().doubleValue());
            Detail(row).setUnitType(object.getSerial().PurchaseOrderReceivingDetails().getUnitType());
        }
        return poJSON;
    }
    
    public JSONObject SearchCSNo(String value, int row)
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        poJSON.put("row", row);
        
        if(Master().getSourceNo() == null || "".equals(Master().getSourceNo())){
            poJSON.put("result", "error");
            poJSON.put("message", "PO Receiving No is not set.");
            return poJSON;
        }
        
        PurchaseOrderReceiving object = new PurchaseOrderReceivingControllers(poGRider, logwrapr).PurchaseOrderReceiving();
        object.InitTransaction();
        poJSON = object.searchConductionStickerNoPORDetail(value, Master().getSourceNo());
        poJSON.put("row", row);
        System.out.println("result" + (String) poJSON.get("result"));
        if ("success".equals((String) poJSON.get("result"))) {
//            poJSON = checkExistingStock(object.getModel().getStockId(), object.getModel().getBarCode(), "1900-01-01", row, false);
//            if ("error".equals((String) poJSON.get("result"))) {
//                return poJSON;
//            }

            Detail(row).setSerialId(object.getSerial().getSerialId());
            Detail(row).setStockId(object.getSerial().getStockId());
            Detail(row).setUnitPrce(object.getSerial().PurchaseOrderReceivingDetails().getUnitPrce().doubleValue());
            Detail(row).setUnitType(object.getSerial().PurchaseOrderReceivingDetails().getUnitType());
        }
        return poJSON;
    }
    
    public int getReceiveQty(String stockId) {
        poJSON = new JSONObject();
        int lnRecQty = 0;
        try {
            
            if(Master().getSourceNo() == null || "".equals(Master().getSourceNo())){
                return 0;
            }
            
            String lsSQL = " SELECT "
                    + " a.nQuantity AS nQuantity "
                    + " FROM po_receiving_detail a " ;
            lsSQL = MiscUtil.addCondition(lsSQL, " a.sTransNox = " + SQLUtil.toSQL(Master().getSourceNo()));
            
            System.out.println("Executing SQL: " + lsSQL);
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
        } catch (SQLException ex) {
            Logger.getLogger(PurchaseOrderReturn.class.getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
        }
        
        return lnRecQty;
    }
    
    public JSONObject checkExistingStock(String stockId, String description, String expiryDate, int row, boolean isSave) {
        for(int lnRow = 0; lnRow <= getDetailCount() - 1; lnRow++){
            if(lnRow != row ){
                if  (stockId.equals(Detail(lnRow).getStockId())) {
                    poJSON.put("result", "error");
                    poJSON.put("message", description+ " already exist in table at row " + (lnRow+1) + ".");
                    poJSON.put("row", lnRow);
                    System.out.println("json row : " + poJSON.get("row"));
                    return poJSON;
                } 
            }
        }
        
        poJSON.put("result", "success");
        return poJSON;
    }
    
    public JSONObject removeDetails() {
        poJSON = new JSONObject();
        Iterator<Model> detail = Detail().iterator();
        while (detail.hasNext()) {
            Model item = detail.next();
            detail.remove();
        }

        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }
    
    public JSONObject computeFields()
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();

        //Compute Transaction Total
        Double ldblTotal = 0.00;
        for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
            ldblTotal += (Detail(lnCtr).getUnitPrce().doubleValue() * Detail(lnCtr).getQuantity().intValue());
        }
        
        Master().setTransactionTotal(ldblTotal);

        return poJSON;
    }
    
    
    public JSONObject loadPurchaseOrderReturn(String formName, String supplierId, String referenceNo) {
        try {
            if (supplierId == null) {
                supplierId = "";
            }
            if (referenceNo == null) {
                referenceNo = "";
            }
            initSQL();
            String lsSQL = MiscUtil.addCondition(SQL_BROWSE, " a.sIndstCdx = " + SQLUtil.toSQL(psIndustryId)
                    + " AND a.sCompnyID = " + SQLUtil.toSQL(psCompanyId)
                    + " AND a.sBranchCd = " + SQLUtil.toSQL(poGRider.getBranchCode())
                    + " AND a.sCategrCd = "+ SQLUtil.toSQL(psCategorCd)
                    + " AND a.sSupplier LIKE " + SQLUtil.toSQL("%" + supplierId)
                    + " AND a.sTransNox LIKE " + SQLUtil.toSQL("%" + referenceNo)
            );
            switch (formName) {
                case "confirmation":
                    lsSQL = lsSQL + " AND ( a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderReturnStatus.OPEN)
                                  + " OR a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderReturnStatus.CONFIRMED) + " ) "
                                  + " AND a.cProcessd = " + SQLUtil.toSQL("0");
                    break;
                case "history":
                    //load all purchase order receiving
                    break;
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

                    paPORMaster.add(PurchaseOrderReturnMaster());
                    paPORMaster.get(paPORMaster.size() - 1).openRecord(loRS.getString("sTransNox"));
                    lnctr++;
                }

                System.out.println("Records found: " + lnctr);
                poJSON.put("result", "success");
                poJSON.put("message", "Record loaded successfully.");
            } else {
                paPORMaster = new ArrayList<>();
                paPORMaster.add(PurchaseOrderReturnMaster());
                poJSON.put("result", "error");
                poJSON.put("continue", true);
                poJSON.put("message", "No record found .");
            }
            MiscUtil.close(loRS);
        } catch (SQLException e) {
            poJSON.put("result", "error");
            poJSON.put("message", e.getMessage());
        } catch (GuanzonException ex) {
            Logger.getLogger(PurchaseOrderReturn.class.getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
        }
        return poJSON;
    }

    private Model_POReturn_Master PurchaseOrderReturnMaster() {
        return new PurchaseOrderReturnModels(poGRider).PurchaseOrderReturnMaster();
    }

    public Model_POReturn_Master PurchaseOrderReturnList(int row) {
        return (Model_POReturn_Master) paPORMaster.get(row);
    }

    public int getPurchaseOrderReturnCount() {
        return this.paPORMaster.size();
    }
    
    public int getReturnQty(String stockId, boolean isAdd)
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        
        if(Master().getSourceNo() == null || "".equals(Master().getSourceNo())){
            return 0;
        }
        
        int lnRecQty = 0;
        String lsSQL = " SELECT "
                + " b.nQuantity AS nQuantity "
                + " FROM po_return_master a "
                + " LEFT JOIN po_return_detail b ON b.sTransNox = a.sTransNox " ;
        lsSQL = MiscUtil.addCondition(lsSQL, " a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderReturnStatus.CONFIRMED)
                                                + " AND a.sSourceNo = " + SQLUtil.toSQL(Master().getSourceNo()));
        
//        if(!isInvTrans){
//            lsSQL = lsSQL + " AND a.sSourceNo = " + SQLUtil.toSQL(Master().getSourceNo());
//        } 
        
        if (isAdd) {
            lsSQL = lsSQL + " AND a.sTransNox <> " + SQLUtil.toSQL(Master().getTransactionNo());
        }

        System.out.println("Executing SQL: " + lsSQL);
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
    
    @Override
    public JSONObject willSave()
            throws SQLException,
            GuanzonException,
            CloneNotSupportedException {
        /*Put system validations and other assignments here*/
        poJSON = new JSONObject();
        int lnSerialCnt = 0;
        boolean lbUpdated = false;
        
        if (paDetailRemoved == null) {
            paDetailRemoved = new ArrayList<>();
        }
        
        if(!pbIsPrint){
            if (PurchaseOrderReturnStatus.CONFIRMED.equals(Master().getTransactionStatus())) {
                poJSON = ShowDialogFX.getUserApproval(poGRider);
                if (!"success".equals((String) poJSON.get("result"))) {
                    return poJSON;
                } 
            }
        }

        Master().setModifyingId(poGRider.getUserID());
        Master().setModifiedDate(poGRider.getServerDate());
        
        boolean lbHasQty = false;
        for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
            if (Detail(lnCtr).getQuantity().intValue() > 0) {
                lbHasQty = true;
                break;
            }
        }
        
        if(!lbHasQty){
            poJSON.put("result", "error");
            poJSON.put("message", "Your transaction cannot be zero quantity.");
            return poJSON;
        }
        
        Iterator<Model> detail = Detail().iterator();
        while (detail.hasNext()) {
            Model item = detail.next();

            if ("".equals((String) item.getValue("sStockIDx"))
                    || (int) item.getValue("nQuantity") <= 0) {
                detail.remove();
                paDetailRemoved.add(item);
            }
        }
        
        //Validate detail after removing all zero qty and empty stock Id
        if (getDetailCount() <= 0) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transaction detail to be save.");
            return poJSON;
        }

        if (getDetailCount() == 1) {
            //do not allow a single item detail with no quantity order
            if (Detail(0).getQuantity().intValue() == 0) {
                poJSON.put("result", "error");
                poJSON.put("message", "Your transaction has zero quantity.");
                return poJSON;
            }
        }

        if (PurchaseOrderReturnStatus.RETURNED.equals(Master().getTransactionStatus())) {
            PurchaseOrderReturn loRecord = new PurchaseOrderReturnControllers(poGRider, null).PurchaseOrderReturn();
            loRecord.InitTransaction();
            loRecord.OpenTransaction(Master().getTransactionNo());

            lbUpdated = loRecord.getDetailCount() == getDetailCount();
            if (lbUpdated) {
                lbUpdated = loRecord.Master().getTransactionTotal().doubleValue() == Master().getTransactionTotal().doubleValue();
            }
            if (lbUpdated) {
                for (int lnCtr = 0; lnCtr <= loRecord.getDetailCount() - 1; lnCtr++) {
                    lbUpdated = loRecord.Detail(lnCtr).getStockId().equals(Detail(lnCtr).getStockId());
                    if (lbUpdated) {
                        lbUpdated = loRecord.Detail(lnCtr).getQuantity().equals(Detail(lnCtr).getQuantity());
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

            Master().setPrint("0"); 
            Master().setTransactionStatus(PurchaseOrderReturnStatus.OPEN); //If edited update trasaction status into open
            
        }

        //assign other info on detail
        for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
            if(getReceiveQty(Detail(lnCtr).getStockId()) < Detail(lnCtr).getQuantity().intValue()){
                poJSON.put("result", "error");
                poJSON.put("message", "Return quantity cannot be greater than the receive quantity.");
                return poJSON;
            }

            //Set value to por detail
            Detail(lnCtr).setTransactionNo(Master().getTransactionNo());
            Detail(lnCtr).setEntryNo(lnCtr + 1);
            Detail(lnCtr).setModifiedDate(poGRider.getServerDate());
        }

        //Allow the user to edit details but seek an approval from the approving officer
        if (PurchaseOrderReturnStatus.CONFIRMED.equals(Master().getTransactionStatus())) {
            poJSON = setValueToOthers(Master().getTransactionStatus());
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
        }

        poJSON.put("result", "success");
        return poJSON;
    }

    @Override
    public JSONObject save() {
        /*Put saving business rules here*/
        return isEntryOkay(PurchaseOrderReturnStatus.OPEN);
    }

    @Override
    public JSONObject saveOthers() {
        /*Only modify this if there are other tables to modify except the master and detail tables*/
        poJSON = new JSONObject();
        int lnCtr, lnRow;

        try {
            //Save Serial Ledger, Inventory
            if (PurchaseOrderReturnStatus.CONFIRMED.equals(Master().getTransactionStatus())) {
                poJSON = saveUpdateOthers(PurchaseOrderReturnStatus.CONFIRMED);
                if (!"success".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }
            }

        } catch (CloneNotSupportedException ex) {
            Logger.getLogger(PurchaseOrderReturn.class.getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
            return poJSON;
        }
        poJSON.put("result", "success");
        return poJSON;
    }
    
//    TODO
//    private InventoryTransaction InventoryTransaction(){
//        return new InventoryTransactionControllers(poGRider, logwrapr).InventoryTransaction();
//    }
    
    private JSONObject setValueToOthers(String status)
            throws CloneNotSupportedException,
            SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        paInventoryTransaction = new ArrayList<>();
        int lnCtr;
        int lnRetQty = 0;
        int lnRecQty = 0;

        for (lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
            System.out.println("----------------------PURCHASE ORDER RETURN DETAIL---------------------- ");
            System.out.println("TransNo : " + (lnCtr + 1) + " : " + Detail(lnCtr).getTransactionNo());
            System.out.println("SerialId : " + (lnCtr + 1) + " : " + Detail(lnCtr).getSerialId());
            System.out.println("StockId : " + (lnCtr + 1) + " : " + Detail(lnCtr).getStockId());
            System.out.println("------------------------------------------------------------------ ");
            
            lnRecQty = getReceiveQty( Detail(lnCtr).getStockId());
            switch (status) {
                case PurchaseOrderReturnStatus.CONFIRMED:
                case PurchaseOrderReturnStatus.PAID:
                case PurchaseOrderReturnStatus.POSTED:
                    //Total return qty for specific po receiving
                    lnRetQty = getReturnQty( Detail(lnCtr).getStockId(), true);
                    lnRetQty = lnRetQty + Detail(lnCtr).getQuantity().intValue();

                    if(lnRetQty > lnRecQty){
                        poJSON.put("result", "error");
                        poJSON.put("message", "Confirmed return quantity cannot be greater than the receive quantity for PO Receiving No. " + Master().getSourceNo());
                        return poJSON;
                    }
                    break;
            }
            
            //Inventory Transaction
            updateInventoryTransaction(status, Detail(lnCtr).getStockId(), Detail(lnCtr).getQuantity().intValue());
        }

        //Update inventory removed in purchase order return
        for (lnCtr = 0; lnCtr <= getDetailRemovedCount() - 1; lnCtr++) {
            //Inventory Transaction 
            updateInventoryTransaction(status, DetailRemove(lnCtr).getStockId(), DetailRemove(lnCtr).getQuantity().intValue());
        }

        poJSON.put("result", "success");
        return poJSON;
    }

    private JSONObject saveUpdateOthers(String status)
            throws CloneNotSupportedException {
        /*Only modify this if there are other tables to modify except the master and detail tables*/
        poJSON = new JSONObject();
        int lnCtr, lnRow;
        try {
            //1. Save Inventory Transaction TODO
            for (lnCtr = 0; lnCtr <= paInventoryTransaction.size() - 1; lnCtr++) {
//                paInventoryTransaction.get(lnCtr).Master().setModifiedDate(poGRider.getServerDate());
//                paInventoryTransaction.get(lnCtr).setWithParent(true);
//                poJSON = paInventoryTransaction.get(lnCtr).SaveTransaction();
                if ("error".equals((String) poJSON.get("result"))) {
                    System.out.println("Purchase Order Saving " + (String) poJSON.get("message"));
                    return poJSON;
                }
            }

            //3. Save Inventory Serial Ledger TODO
            if (PurchaseOrderReturnStatus.CONFIRMED.equals(Master().getTransactionStatus())
                    || PurchaseOrderReturnStatus.POSTED.equals(Master().getTransactionStatus())) {
                InvSerial loInvSerial = new InvControllers(poGRider, logwrapr).InventorySerial();
                loInvSerial.initialize();
                loInvSerial.setWithParentClass(true);
                //            InventoryTrans.POReturn();
            }

        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(PurchaseOrderReturn.class.getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
            return poJSON;
        }
        poJSON.put("result", "success");
        return poJSON;
    }

    //TODO
    private void updateInventoryTransaction(String status, String stockId, int quantity)
            throws GuanzonException,
            SQLException,
            CloneNotSupportedException {
//        int lnRow, lnList;
//        int lnRetQty = 0;
//        int lnQtyOnHnd = 0;
//        boolean lbExist = false;
//        //1.check if stock Id is already exist
//        for(lnRow= 0;lnRow <= paInventoryTransaction.size() - 1; lnRow++){
//            System.out.println("Stock ID : " + paInventoryTransaction.get(lnRow).Master().getStockId());
//            if(paInventoryTransaction.get(lnRow).Master().getStockId() != null){
//                if( paInventoryTransaction.get(lnRow).Master().getStockId().equals(stockId)){
//                    lbExist = true; 
//                    break;
//                }
//            } 
//        }
//
//        //2. If stock id is not exist add it on inventory transaction array list then open the transaction
//        if(!lbExist){
//            paInventoryTransaction.add(InventoryTransaction());
//            paInventoryTransaction.get(paInventoryTransaction.size() - 1).InitTransaction();
//            paInventoryTransaction.get(paInventoryTransaction.size() - 1).OpenTransaction(stockId);
//            paInventoryTransaction.get(paInventoryTransaction.size() - 1).UpdateTransaction();
//            lnList = paInventoryTransaction.size() - 1;
//        } else {
//            //if already exist, get the row no of stock id
//            lnList = lnRow;
//        }
//          TODO
//        if(stockId.equals(paInventoryTransaction.get(lnList).Master().getStockID())){
//            switch(status){
//                case PurchaseOrderReturnStatus.CONFIRMED:
//                case PurchaseOrderReturnStatus.PAID:
//                case PurchaseOrderReturnStatus.POSTED:
                      //Total return qty
//                    //lnRetQty = getReturnQty(stockId, true, true);
//                    lnQtyOnHnd = paInventoryTransaction.get(lnList).Master().getQuantityOnHand() - quantity;
//                    break;
//                case PurchaseOrderReturnStatus.VOID:
//                case PurchaseOrderReturnStatus.RETURNED: 
                      //Total return qty
//                    //lnRetQty = getReturnQty(stockId, false, true);
//                    lnQtyOnHnd = paInventoryTransaction.get(lnList).Master().getQuantityOnHand() + quantity;
//                    break;
//            }
//            //set value to Inventory Transaction
//            paInventoryTransaction.get(lnList).Master().setQuantityOnHand(lnQtyOnHnd);
//            paInventoryTransaction.get(lnList).Master().setModifiedDate(poGRider.getServerDate());
//        }
    }
    
    @Override
    public void saveComplete() {
        /*This procedure was called when saving was complete*/
        System.out.println("Transaction saved successfully.");
    }

    @Override
    public JSONObject initFields() {
        try {
            /*Put initial model values here*/
            poJSON = new JSONObject();
            Master().setBranchCode(poGRider.getBranchCode());
            Master().setIndustryId(psIndustryId);
            Master().setCompanyId(psCompanyId);
            Master().setCategoryCode(psCategorCd);
            Master().setTransactionDate(poGRider.getServerDate());
            Master().setTransactionStatus(PurchaseOrderReturnStatus.OPEN);
            Master().setSourceCode(SOURCE_CODE);

        } catch (SQLException ex) {
            Logger.getLogger(PurchaseOrderReturn.class.getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
            return poJSON;
        }

        poJSON.put("result", "success");
        return poJSON;
    }
    
    @Override
    public void initSQL() {
        SQL_BROWSE = " SELECT "
                + "   a.dTransact  "
                + " , a.sTransNox  "
                + " , a.sIndstCdx  "
                + " , a.sCompnyID  "
                + " , a.sSupplier  "
                + " , b.sCompnyNm  AS sSupplrNm"
                + " , c.sCompnyNm  AS sCompnyNm"
                + " , d.sDescript  AS sIndustry"
                + " FROM po_return_master a "
                + " LEFT JOIN client_master b ON b.sClientID = a.sSupplier "
                + " LEFT JOIN company c ON c.sCompnyID = a.sCompnyID "
                + " LEFT JOIN industry d ON d.sIndstCdx = a.sIndstCdx ";
    }
    
    @Override
    protected JSONObject isEntryOkay(String status) {
        GValidator loValidator = PurchaseOrderReturnValidatorFactory.make(Master().getIndustryId());

        loValidator.setApplicationDriver(poGRider);
        loValidator.setTransactionStatus(status);
        loValidator.setMaster(poMaster);
//        loValidator.setDetail(paDetail);

        poJSON = loValidator.validate();

        return poJSON;
    }
    
}
