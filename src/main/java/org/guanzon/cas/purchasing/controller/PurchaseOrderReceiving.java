/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.guanzon.cas.purchasing.controller;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javax.script.ScriptException;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperPrintManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.swing.JRViewer;
import net.sf.jasperreports.swing.JRViewerToolbar;
import net.sf.jasperreports.view.JasperViewer;
import org.guanzon.appdriver.agent.ShowDialogFX;
import org.guanzon.appdriver.agent.ShowMessageFX;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.agent.services.Transaction;
import org.guanzon.appdriver.agent.systables.SysTableContollers;
import org.guanzon.appdriver.agent.systables.TransactionAttachment;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.constant.Logical;
import org.guanzon.appdriver.constant.RecordStatus;
import org.guanzon.appdriver.constant.UserRight;
import org.guanzon.appdriver.iface.GValidator;
import org.guanzon.cas.client.Client;
import org.guanzon.cas.client.services.ClientControllers;
import org.guanzon.cas.inv.InvSerial;
import org.guanzon.cas.inv.Inventory;
import org.guanzon.cas.inv.InventoryTransaction;
import org.guanzon.cas.inv.model.Model_Inv_Serial;
import org.guanzon.cas.inv.services.InvControllers;
import org.guanzon.cas.inv.services.InvModels;
import org.guanzon.cas.parameter.Branch;
import org.guanzon.cas.parameter.Brand;
import org.guanzon.cas.parameter.Company;
import org.guanzon.cas.parameter.InvLocation;
import org.guanzon.cas.parameter.Term;
import org.guanzon.cas.parameter.services.ParamControllers;
import org.guanzon.cas.purchasing.model.Model_POR_Detail;
import org.guanzon.cas.purchasing.model.Model_POR_Master;
import org.guanzon.cas.purchasing.model.Model_POR_Serial;
import org.guanzon.cas.purchasing.model.Model_POReturn_Detail;
import org.guanzon.cas.purchasing.model.Model_POReturn_Master;
import org.guanzon.cas.purchasing.model.Model_PO_Master;
import org.guanzon.cas.purchasing.services.PurchaseOrderControllers;
import org.guanzon.cas.purchasing.services.PurchaseOrderModels;
import org.guanzon.cas.purchasing.services.PurchaseOrderReceivingControllers;
import org.guanzon.cas.purchasing.services.PurchaseOrderReceivingModels;
import org.guanzon.cas.purchasing.services.PurchaseOrderReturnControllers;
import org.guanzon.cas.purchasing.services.PurchaseOrderReturnModels;
import org.guanzon.cas.purchasing.status.PurchaseOrderReceivingStatus;
import org.guanzon.cas.purchasing.status.PurchaseOrderReturnStatus;
import org.guanzon.cas.purchasing.status.PurchaseOrderStatus;
import org.guanzon.cas.purchasing.validator.PurchaseOrderReceivingValidatorFactory;
import org.guanzon.cas.tbjhandler.TBJEntry;
import org.guanzon.cas.tbjhandler.TBJTransaction;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import ph.com.guanzongroup.cas.cashflow.CachePayable;
import ph.com.guanzongroup.cas.cashflow.Journal;
import ph.com.guanzongroup.cas.cashflow.model.Model_Journal_Master;
import ph.com.guanzongroup.cas.cashflow.services.CashflowControllers;
import ph.com.guanzongroup.cas.cashflow.services.CashflowModels;
import ph.com.guanzongroup.cas.cashflow.status.CachePayableStatus;

/**
 *
 * @author Arsiela 03-12-2025
 */
public class PurchaseOrderReceiving extends Transaction {

    private boolean pbApproval = false;
    private boolean pbIsPrint = false;
    private boolean pbIsWithDiscount = false;
    private boolean pbIsWithDiscountRate = false;
    private boolean pbIsFinance = false;
    private String psPurpose = PurchaseOrderReceivingStatus.Purpose.REGULAR;
    private String psIndustryId = "";
    private String psCompanyId = "";
    private String psCategorCd = "";
    
    private Model_POR_Serial poSerial;
    private CachePayable poCachePayable;
    private CachePayable poCachePayableTrucking;
    private Journal poJournal;
    
    List<Model_PO_Master> paPOMaster;
    List<Model_POReturn_Master> paPOReturnMaster;
    List<Model_POR_Master> paPORMaster;
    List<Model_POR_Serial> paOthers;
    List<PurchaseOrder> paPurchaseOrder;
    List<PurchaseOrderReturn> paPurchaseOrderReturn;
    List<InventoryTransaction> paInventoryTransaction;
    List<TransactionAttachment> paAttachments;
    List<Model> paDetailRemoved;

    public JSONObject InitTransaction() {
        SOURCE_CODE = "PORc";

        poMaster = new PurchaseOrderReceivingModels(poGRider).PurchaseOrderReceivingMaster();
        poDetail = new PurchaseOrderReceivingModels(poGRider).PurchaseOrderReceivingDetails();
        poSerial = new PurchaseOrderReceivingModels(poGRider).PurchaseOrderReceivingSerial();

        paPORMaster = new ArrayList<>();
        paOthers = new ArrayList<>();
        paDetail = new ArrayList<>();
        paDetailRemoved = new ArrayList<>();
        paAttachments = new ArrayList<>();
        paPOMaster = new ArrayList<>();
        paPurchaseOrder = new ArrayList<>();
        paPurchaseOrderReturn = new ArrayList<>();
        paInventoryTransaction = new ArrayList<>();

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
            resetOthers();
            Detail().clear();
            Journal();
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

        String lsStatus = PurchaseOrderReceivingStatus.CONFIRMED;
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
        poJSON = isEntryOkay(PurchaseOrderReceivingStatus.CONFIRMED);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        //Set receive qty to Purchase Order
        poJSON = setValueToOthers(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
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
        
        //if with discrepancy require approval
//        if (pbApproval) {
//            if (poGRider.getUserLevel() <= UserRight.ENCODER) {
//                poJSON = ShowDialogFX.getUserApproval(poGRider);
//                if (!"success".equals((String) poJSON.get("result"))) {
//                    return poJSON;
//                }
//            }
//        } else {
//            if (poGRider.getUserLevel() <= UserRight.ENCODER) {
//                poJSON.put("result", "error");
//                poJSON.put("message", "You are not allow to confirm this transaction.");
//                return poJSON;
//            }
//        }

        poGRider.beginTrans("UPDATE STATUS", "ConfirmTransaction", SOURCE_CODE, Master().getTransactionNo());

        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbConfirm, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        //Update Purchase Order, Inventory, Serial Ledger
        poJSON = saveUpdateOthers(PurchaseOrderReceivingStatus.CONFIRMED);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }
        
        //Update Inventory Serial
        poJSON = saveUpdateInvSerial(PurchaseOrderReceivingStatus.CONFIRMED);
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

        String lsStatus = PurchaseOrderReceivingStatus.RETURNED;
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
        poJSON = isEntryOkay(PurchaseOrderReceivingStatus.RETURNED);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        if (PurchaseOrderReceivingStatus.CONFIRMED.equals(Master().getTransactionStatus())) {
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
        
        if (PurchaseOrderReceivingStatus.CONFIRMED.equals(Master().getTransactionStatus())) {
            //Update Purchase Order, Inventory, Serial Ledger
            poJSON = saveUpdateOthers(PurchaseOrderReceivingStatus.CONFIRMED);
            if (!"success".equals((String) poJSON.get("result"))) {
                poGRider.rollbackTrans();
                return poJSON;
            }
        }
        
        //Update Inventory Serial
        poJSON = saveUpdateInvSerial(PurchaseOrderReceivingStatus.RETURNED);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
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
    
    public JSONObject PaidTransaction(String remarks)
            throws ParseException,
            SQLException,
            GuanzonException,
            CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = PurchaseOrderReceivingStatus.PAID;
        boolean lbPaid = true;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

        if (lsStatus.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already paid.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(PurchaseOrderReceivingStatus.PAID);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        //Update purchase order
        poJSON = setValueToOthers(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

//        if (pbApproval) {
//            if (poGRider.getUserLevel() <= UserRight.ENCODER) {
//                poJSON = ShowDialogFX.getUserApproval(poGRider);
//                if (!"success".equals((String) poJSON.get("result"))) {
//                    return poJSON;
//                }
//            }
//        }

        poGRider.beginTrans("UPDATE STATUS", "PaidTransaction", SOURCE_CODE, Master().getTransactionNo());

        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbPaid, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        //Update Purchase Order, Serial Ledger, Inventory
        poJSON = saveUpdateOthers(PurchaseOrderReceivingStatus.PAID);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        poGRider.commitTrans();

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        if (lbPaid) {
            poJSON.put("message", "Transaction paid successfully.");
        } else {
            poJSON.put("message", "Transaction paid request submitted successfully.");
        }

        return poJSON;
    }

    public JSONObject PostTransaction(String remarks)
            throws ParseException,
            SQLException,
            GuanzonException,
            CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = PurchaseOrderReceivingStatus.POSTED;
        boolean lbPosted = true;
        
        poJSON = OpenTransaction(Master().getTransactionNo());
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

        if (lsStatus.equals((String) poMaster.getValue("cTranStat")) || Master().isProcessed()) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already processed.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(PurchaseOrderReceivingStatus.POSTED);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
        //check JE
        if(poJournal == null){
            poJSON.put("result", "error");
            poJSON.put("message", "Please review journal entry before posting.");
            return poJSON;
        } else {
            switch(poJournal.getEditMode()){
                case EditMode.ADDNEW:
                case EditMode.UPDATE:
                break;
                case EditMode.READY:
                    if(poJournal.Master().getTransactionNo() != null && !"".equals(poJournal.Master().getTransactionNo())){
                        //poJournal.UpdateTransaction();
                    } else {
                        poJSON.put("result", "error");
                        poJSON.put("message", "Please review journal entry before posting.");
                        return poJSON; 
                    }
                break;
                default:
                    poJSON.put("result", "error");
                    poJSON.put("message", "Please review journal entry before posting.");
                    return poJSON;
            }
        }
        
        poJSON = validateJournal();
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
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
        
        //populate cache payable
        poJSON = populateCachePayable();
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
        //populate cache payable freight
        if(Master().getTruckingId() != null && !"".equals(Master().getTruckingId()) 
            && Master().getFreight().doubleValue() > 0.0000
            && !Master().getTruckingId().equals(Master().getSupplierId())){
            if("".equals(getInvTypeCode("freight"))){
                poJSON.put("result", "error");
                poJSON.put("message", "Freight transaction type cannot be empty.\nContact System Administrator to address the issue.");
                return poJSON;
            }
            poJSON = populateCachePayableFreight();
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
        }
        
        poGRider.beginTrans("UPDATE STATUS", "PostTransaction", SOURCE_CODE, Master().getTransactionNo());
        
        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbPosted, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }
        
        //Update process in PO Receiving Master
        poJSON = Master().openRecord(Master().getTransactionNo());
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }
        
        poJSON = Master().updateRecord();
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }
        
        Master().isProcessed(true);
        Master().setModifyingId(poGRider.Encrypt(poGRider.getUserID()));
        Master().setModifiedDate(poGRider.getServerDate());
        poJSON = Master().saveRecord();
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }
        
        poCachePayable.setWithParent(true);
        poJSON = poCachePayable.SaveTransaction();
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }
        
        if(Master().getTruckingId() != null && !"".equals(Master().getTruckingId()) 
            && Master().getFreight().doubleValue() > 0.0000
            && !Master().getTruckingId().equals(Master().getSupplierId())){
            poCachePayableTrucking.setWithParent(true);
            poJSON = poCachePayableTrucking.SaveTransaction();
            if (!"success".equals((String) poJSON.get("result"))) {
                poGRider.rollbackTrans();
                return poJSON;
            }
        }
        
        poGRider.commitTrans();
        
        poJournal.setWithParent(true);
        poJournal.setWithUI(true);
        poJSON = poJournal.ConfirmTransaction("Confirm Transaction");
        if ("error".equals((String) poJSON.get("result"))) {
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

        String lsStatus = PurchaseOrderReceivingStatus.CANCELLED;
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
        poJSON = isEntryOkay(PurchaseOrderReceivingStatus.CANCELLED);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
        if (PurchaseOrderReceivingStatus.CONFIRMED.equals(Master().getTransactionStatus())) {
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
            
            //update Purchase Order
            poJSON = setValueToOthers(lsStatus);
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
        }

        poGRider.beginTrans("UPDATE STATUS", "CancelTransaction", SOURCE_CODE, Master().getTransactionNo());

        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbCancelled, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }
        
        if (PurchaseOrderReceivingStatus.CONFIRMED.equals(Master().getTransactionStatus())) {
            //Update Purchase Order, Serial Ledger, Inventory
            poJSON = saveUpdateOthers(PurchaseOrderReceivingStatus.CONFIRMED);
            if (!"success".equals((String) poJSON.get("result"))) {
                poGRider.rollbackTrans();
                return poJSON;
            }
        }
        
        //Update Inventory Serial
        poJSON = saveUpdateInvSerial(PurchaseOrderReceivingStatus.CANCELLED);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }
        
        //Delete Inventory Serial
//        poJSON = deleteInvSerial();
//        if (!"success".equals((String) poJSON.get("result"))) {
//            poGRider.rollbackTrans();
//            return poJSON;
//        }

        poGRider.commitTrans();
        
        //Check journal
        String lsJournal = existJournal();
        if(lsJournal != null && !"".equals(lsJournal)){
            poJournal = new CashflowControllers(poGRider, logwrapr).Journal();
            poJournal.InitTransaction();
            poJSON = poJournal.OpenTransaction(lsJournal);
            if ("error".equals((String) poJSON.get("result"))){
                return poJSON;
            }
            
            poJSON = poJournal.VoidTransaction("VoidTransaction");
            if ("error".equals((String) poJSON.get("result"))){
                return poJSON;
            }
        }

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

        String lsStatus = PurchaseOrderReceivingStatus.VOID;
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
        poJSON = isEntryOkay(PurchaseOrderReceivingStatus.VOID);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        if (PurchaseOrderReceivingStatus.CONFIRMED.equals(Master().getTransactionStatus())) {
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
        
        if (PurchaseOrderReceivingStatus.CONFIRMED.equals(Master().getTransactionStatus())) {
            //Update Purchase Order, Serial Ledger, Inventory
            poJSON = saveUpdateOthers(PurchaseOrderReceivingStatus.CONFIRMED);
            if (!"success".equals((String) poJSON.get("result"))) {
                poGRider.rollbackTrans();
                return poJSON;
            }
        }
            
        //Update Inventory Serial
        poJSON = saveUpdateInvSerial(PurchaseOrderReceivingStatus.VOID);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }
        
        //Delete Inventory Serial TODO
//        poJSON = deleteInvSerial();
//        if (!"success".equals((String) poJSON.get("result"))) {
//            poGRider.rollbackTrans();
//            return poJSON;
//        }

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
        String lsPurpose = " AND a.cPurposex = " + SQLUtil.toSQL(psPurpose);
        initSQL();
        String lsSQL = MiscUtil.addCondition(SQL_BROWSE, " a.sIndstCdx = " + SQLUtil.toSQL(psIndustryId)
                + " AND a.sCompnyID = " + SQLUtil.toSQL(psCompanyId)
                + " AND a.sCategrCd = " + SQLUtil.toSQL(psCategorCd)
                + " AND a.sBranchCd = " + SQLUtil.toSQL(poGRider.getBranchCode())
                + " AND a.sSupplier LIKE " + SQLUtil.toSQL("%" + Master().getSupplierId())
                + lsPurpose );
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
    
    public JSONObject searchTransaction(String industryId, String companyId, String supplier, String sReferenceNo)
            throws CloneNotSupportedException,
            SQLException,
            GuanzonException {
        if(supplier == null){
            supplier = "";
        }
        if(sReferenceNo == null){
            sReferenceNo = "";
        }
        
        if(industryId == null || "".equals(industryId)){
            industryId = psIndustryId;
        }
        
        if(companyId == null || "".equals(companyId)){
            companyId = psCompanyId;
        }
        
        String lsPurpose = " AND a.cPurposex = " + SQLUtil.toSQL(psPurpose);
        
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
                + " AND a.sCategrCd = " + SQLUtil.toSQL(psCategorCd)
                + " AND a.sBranchCd = " + SQLUtil.toSQL(poGRider.getBranchCode())
                + " AND b.sCompnyNm LIKE " + SQLUtil.toSQL("%" + supplier)
                + " AND a.sTransNox LIKE " + SQLUtil.toSQL("%" + sReferenceNo)
                + lsPurpose);
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
    
    public JSONObject searchTransaction(String industryId, String companyId, String supplier, String receivingBranch, String sReferenceNo)
            throws CloneNotSupportedException,
            SQLException,
            GuanzonException {
        if(supplier == null){
            supplier = "";
        }
        if(receivingBranch == null){
            receivingBranch = "";
        }
        if(sReferenceNo == null){
            sReferenceNo = "";
        }
        
        if(industryId == null || "".equals(industryId)){
            industryId = psIndustryId;
        }
        
        if(companyId == null || "".equals(companyId)){
            companyId = psCompanyId;
        }
        
        
        String lsPurpose = " AND a.cPurposex = " + SQLUtil.toSQL(psPurpose);
        
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
                + " AND a.sCategrCd = " + SQLUtil.toSQL(psCategorCd)
                + " AND e.sBranchNm LIKE " + SQLUtil.toSQL("%" + receivingBranch)
                + " AND b.sCompnyNm LIKE " + SQLUtil.toSQL("%" + supplier)
                + " AND a.sTransNox LIKE " + SQLUtil.toSQL("%" + sReferenceNo)
                + lsPurpose);
        if (psTranStat != null && !"".equals(psTranStat)) {
            lsSQL = lsSQL + lsTransStat;
        }

        System.out.println("Executing SQL: " + lsSQL);
        poJSON = ShowDialogFX.Browse(poGRider,
                lsSQL,
                "",
                "Transaction Date»Transaction No»Industry»Company»Supplier»Receiving Branch",
                "dTransact»sTransNox»sIndustry»sCompnyNm»sSupplrNm»sBranchNm",
                "a.dTransact»a.sTransNox»d.sDescript»c.sCompnyNm»b.sCompnyNm»e.sBranchNm",
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
    
    public JSONObject searchTransaction(String industryId, String companyId, String categoryId, String supplierId, String transactionNo, String referenceNo)
            throws CloneNotSupportedException,
            SQLException,
            GuanzonException {
        boolean lbByCode = false;
        if(supplierId == null){
            supplierId = "";
        }
        if(referenceNo == null){
            referenceNo = "";
            lbByCode = true;
        }
        if(transactionNo == null){
            transactionNo = "";
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
        
        
        String lsPurpose = " AND a.cPurposex = " + SQLUtil.toSQL(psPurpose);

        initSQL();
        String lsSQL = MiscUtil.addCondition(SQL_BROWSE, " a.sIndstCdx = " + SQLUtil.toSQL(industryId)
                + " AND a.sCompnyID = " + SQLUtil.toSQL(companyId)
                + " AND a.sCategrCd = " + SQLUtil.toSQL(categoryId)
                + " AND a.sBranchCD = " + SQLUtil.toSQL(poGRider.getBranchCode())
                + " AND a.sSupplier LIKE " + SQLUtil.toSQL("%" + supplierId)
                + " AND a.sTransNox LIKE " + SQLUtil.toSQL("%" + transactionNo)
                + " AND a.sReferNox LIKE " + SQLUtil.toSQL("%" + referenceNo)
                + lsPurpose);
        if (psTranStat != null && !"".equals(psTranStat)) {
            lsSQL = lsSQL + lsTransStat;
        }

        System.out.println("Executing SQL: " + lsSQL);
        poJSON = ShowDialogFX.Browse(poGRider,
                lsSQL,
                "",
                "Transaction Date»Transaction No»Reference No»Industry»Company»Supplier",
                "dTransact»sTransNox»sReferNox»sIndustry»sCompnyNm»sSupplrNm",
                "a.dTransact»a.sTransNox»a.sReferNox»d.sDescript»c.sCompnyNm»b.sCompnyNm",
                lbByCode ? 1 : 2);

        if (poJSON != null) {
            return Master().openRecord((String) poJSON.get("sTransNox"));
        } else {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No record loaded.");
            return poJSON;
        }
    }

    /*Search Master References*/
    public JSONObject SearchCompany(String value, boolean byCode)
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();

        Company object = new ParamControllers(poGRider, logwrapr).Company();
        object.setRecordStatus(RecordStatus.ACTIVE);
        poJSON = object.searchRecord(value, byCode);
        if ("success".equals((String) poJSON.get("result"))) {
            Master().setCompanyId(object.getModel().getCompanyId());
        }
        return poJSON;
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
//            Master().setTermCode("");//TODO
        }

        return poJSON;
    }
    
    public JSONObject SearchBranch(String value, boolean byCode)
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();

        Branch object = new ParamControllers(poGRider, logwrapr).Branch();
        object.setRecordStatus(RecordStatus.ACTIVE);
        poJSON = object.searchRecord(value, byCode);
        if ("success".equals((String) poJSON.get("result"))) {
            Master().setBranchCode(object.getModel().getBranchCode());
        }

        return poJSON;
    }

    public JSONObject SearchTrucking(String value, boolean byCode)
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();

        Client object = new ClientControllers(poGRider, logwrapr).Client();
        object.Master().setRecordStatus(RecordStatus.ACTIVE);
        object.Master().setClientType("1");
        poJSON = object.Master().searchRecord(value, byCode);
        if ("success".equals((String) poJSON.get("result"))) {
            Master().setTruckingId(object.Master().getModel().getClientId());
        }
        return poJSON;
    }

    public JSONObject SearchTerm(String value, boolean byCode)
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();

        Term object = new ParamControllers(poGRider, logwrapr).Term();
        object.getModel().setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);
        if ("success".equals((String) poJSON.get("result"))) {
            Master().setTermCode(object.getModel().getTermId());
        }
        return poJSON;
    }

    public JSONObject SearchBarcode(String value, boolean byCode, int row)
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        poJSON.put("row", row);
        String lsBrand = (Detail(row).getBrandId() != null && !Detail(row).getBrandId().isEmpty()) ? Detail(row).getBrandId() : null;
        String lsIndustry = Master().getIndustryId().isEmpty() ? null : Master().getIndustryId();
        Inventory object = new InvControllers(poGRider, logwrapr).Inventory();
        object.setRecordStatus(RecordStatus.ACTIVE);
        
        if(Master().getSupplierId() == null || "".equals(Master().getSupplierId())){
            poJSON.put("result", "error");
            poJSON.put("message", "Supplier is not set.");
            return poJSON;
        }
        
        poJSON = object.searchRecord(value, byCode, 
                Master().getSupplierId(),
                lsBrand, 
                lsIndustry,  
                Master().getCategoryCode());
        
        poJSON.put("row", row);
        System.out.println("result" + (String) poJSON.get("result"));
        if ("success".equals((String) poJSON.get("result"))) {
            poJSON = checkExistingStock(object.getModel().getStockId(), object.getModel().getBarCode(), "1900-01-01", row, false);
            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }

            Detail(row).setStockId(object.getModel().getStockId());
            Detail(row).setUnitType(object.getModel().getUnitType());
            Detail(row).isSerialized(object.getModel().isSerialized());
            Detail(row).setUnitPrce(object.getModel().getCost().doubleValue());
        }
        
        System.out.println("StockID : " + Detail(row).Inventory().getStockId());
        System.out.println("Model  : " + Detail(row).Inventory().Model().getDescription());
        return poJSON;
    }

    public JSONObject SearchDescription(String value, boolean byCode, int row)
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        poJSON.put("row", row);
        String lsBrand = (Detail(row).getBrandId() != null && !Detail(row).getBrandId().isEmpty()) ? Detail(row).getBrandId() : null;
        String lsIndustry = Master().getIndustryId().isEmpty() ? null : Master().getIndustryId();
        Inventory object = new InvControllers(poGRider, logwrapr).Inventory();
        object.setRecordStatus(RecordStatus.ACTIVE);
        
        if(Master().getSupplierId() == null || "".equals(Master().getSupplierId())){
            poJSON.put("result", "error");
            poJSON.put("message", "Supplier is not set.");
            return poJSON;
        }

        poJSON = object.searchRecord(value, byCode, 
                Master().getSupplierId(),
                lsBrand, 
                lsIndustry,  
                Master().getCategoryCode());
        
        poJSON.put("row", row);
        if ("success".equals((String) poJSON.get("result"))) {
            poJSON = checkExistingStock(object.getModel().getStockId(), object.getModel().getBarCode(), "1900-01-01", row, false);
            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }

            Detail(row).setStockId(object.getModel().getStockId());
            Detail(row).setUnitType(object.getModel().getUnitType());
            Detail(row).isSerialized(object.getModel().isSerialized());
            Detail(row).setUnitPrce(object.getModel().getCost().doubleValue());
        }
        
        
        System.out.println("StockID : " + Detail(row).Inventory().getStockId());
        System.out.println("Model  : " + Detail(row).Inventory().Model().getDescription());

        return poJSON;
    }

    public JSONObject SearchSupersede(String value, boolean byCode, int row)
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();

        if(Master().getSupplierId() == null || "".equals(Master().getSupplierId())){
            poJSON.put("result", "error");
            poJSON.put("message", "Supplier is not set.");
            return poJSON;
        }
        
        if(Detail(row).getStockId() == null || "".equals(Detail(row).getStockId())){
            poJSON.put("result", "error");
            poJSON.put("message", "Stock Id cannot be empty.");
            return poJSON;
        }
        
        Inventory object = new InvControllers(poGRider, logwrapr).Inventory();
        object.setRecordStatus(RecordStatus.ACTIVE);

//        poJSON = object.searchRecord(value, byCode); //TODO
        poJSON = object.searchRecord(value, byCode, Master().getSupplierId(),null, null,  Master().getCategoryCode());
        if ("success".equals((String) poJSON.get("result"))) {
            if(Detail(row).getStockId().equals(object.getModel().getStockId())){
                poJSON.put("result", "error");
                poJSON.put("message", "Selected supersede must not be equal to the current stock ID.");
                return poJSON;
            }
            Detail(row).setReplaceId(object.getModel().getStockId());
        }
        return poJSON;
    }
    
    /*HIDDEN FEATURE*/
    public JSONObject SearchBarcode(String value, boolean byCode, int row, boolean isWithSupplier)
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        poJSON.put("row", row);
        String lsBrand = (Detail(row).getBrandId() != null && !Detail(row).getBrandId().isEmpty()) ? Detail(row).getBrandId() : null;
        String lsIndustry = Master().getIndustryId().isEmpty() ? null : Master().getIndustryId();
        Inventory object = new InvControllers(poGRider, logwrapr).Inventory();
        object.setRecordStatus(RecordStatus.ACTIVE);
        
        if(isWithSupplier){
            if(Master().getSupplierId() == null || "".equals(Master().getSupplierId())){
                poJSON.put("result", "error");
                poJSON.put("message", "Supplier is not set.");
                return poJSON;
            }
        } 
        
        poJSON = object.searchRecord(value, byCode, 
                isWithSupplier ? Master().getSupplierId() : null,
                lsBrand, 
                lsIndustry,  
                Master().getCategoryCode());
        
        poJSON.put("row", row);
        System.out.println("result" + (String) poJSON.get("result"));
        if ("success".equals((String) poJSON.get("result"))) {
            poJSON = checkExistingStock(object.getModel().getStockId(), object.getModel().getBarCode(), "1900-01-01", row, false);
            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }

            Detail(row).setStockId(object.getModel().getStockId());
            Detail(row).setUnitType(object.getModel().getUnitType());
            Detail(row).isSerialized(object.getModel().isSerialized());
            Detail(row).setUnitPrce(object.getModel().getCost().doubleValue());
        }
        
        System.out.println("StockID : " + Detail(row).Inventory().getStockId());
        System.out.println("Model  : " + Detail(row).Inventory().Model().getDescription());
        return poJSON;
    }

    public JSONObject SearchDescription(String value, boolean byCode, int row, boolean isWithSupplier)
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        poJSON.put("row", row);
        String lsBrand = (Detail(row).getBrandId() != null && !Detail(row).getBrandId().isEmpty()) ? Detail(row).getBrandId() : null;
        String lsIndustry = Master().getIndustryId().isEmpty() ? null : Master().getIndustryId();
        Inventory object = new InvControllers(poGRider, logwrapr).Inventory();
        object.setRecordStatus(RecordStatus.ACTIVE);
        
        if(isWithSupplier){
            if(Master().getSupplierId() == null || "".equals(Master().getSupplierId())){
                poJSON.put("result", "error");
                poJSON.put("message", "Supplier is not set.");
                return poJSON;
            }
        } 
        
        poJSON = object.searchRecord(value, byCode, 
                isWithSupplier ? Master().getSupplierId() : null,
                lsBrand, 
                lsIndustry,  
                Master().getCategoryCode());
        
        poJSON.put("row", row);
        if ("success".equals((String) poJSON.get("result"))) {
            poJSON = checkExistingStock(object.getModel().getStockId(), object.getModel().getBarCode(), "1900-01-01", row, false);
            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }

            Detail(row).setStockId(object.getModel().getStockId());
            Detail(row).setUnitType(object.getModel().getUnitType());
            Detail(row).isSerialized(object.getModel().isSerialized());
            Detail(row).setUnitPrce(object.getModel().getCost().doubleValue());
        }
        
        
        System.out.println("StockID : " + Detail(row).Inventory().getStockId());
        System.out.println("Model  : " + Detail(row).Inventory().Model().getDescription());

        return poJSON;
    }

    public JSONObject SearchSupersede(String value, boolean byCode, int row, boolean isWithSupplier)
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        
        Inventory object = new InvControllers(poGRider, logwrapr).Inventory();
        object.setRecordStatus(RecordStatus.ACTIVE);
        
        if(Detail(row).getStockId() == null || "".equals(Detail(row).getStockId())){
            poJSON.put("result", "error");
            poJSON.put("message", "Stock Id cannot be empty.");
            return poJSON;
        }
        
        if(isWithSupplier){
            if(Master().getSupplierId() == null || "".equals(Master().getSupplierId())){
                poJSON.put("result", "error");
                poJSON.put("message", "Supplier is not set.");
                return poJSON;
            }
            poJSON = object.searchRecord(value, byCode, Master().getSupplierId(),null, null,  Master().getCategoryCode());
        } else {
            poJSON = object.searchRecord(value, byCode, null,null, null,  Master().getCategoryCode());
        }

        if ("success".equals((String) poJSON.get("result"))) {
            if(Detail(row).getStockId().equals(object.getModel().getStockId())){
                poJSON.put("result", "error");
                poJSON.put("message", "Selected supersede must not be equal to the current stock ID.");
                return poJSON;
            }
            Detail(row).setReplaceId(object.getModel().getStockId());
        }
        return poJSON;
    }
    
    /*Hidden Feature*/
    public JSONObject SearchModel(String value, boolean byCode, int row, boolean isWithSupplier)
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        poJSON.put("row", row);
        
        Inventory object = new InvControllers(poGRider, logwrapr).Inventory();
        object.setRecordStatus(RecordStatus.ACTIVE);
        System.out.println("Brand ID : "  + Detail(row).getBrandId());
        
        if(isWithSupplier){
            if(Master().getSupplierId() == null || "".equals(Master().getSupplierId())){
                poJSON.put("result", "error");
                poJSON.put("message", "Supplier is not set.");
                return poJSON;
            }
            poJSON = object.searchRecordOfVariants(value, byCode, Master().getSupplierId(),Detail(row).getBrandId(), Master().getIndustryId(),  Master().getCategoryCode());
        } else {
            poJSON = object.searchRecord(value, byCode, null,null, null,  Master().getCategoryCode());
        }
        
        poJSON.put("row", row);
        if ("success".equals((String) poJSON.get("result"))) {
            poJSON = checkExistingStock(object.getModel().getStockId(), object.getModel().getDescription(), "1900-01-01", row, false);
            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }

            if (!object.getModel().getStockId().equals(Detail(row).getStockId())) {
//                poJSON = checkExistingSerialId(row);
//                if ("error".equals((String) poJSON.get("result"))) {
//                    return poJSON;
//                }
                //remove existing por serial
                removePurchaseOrderReceivingSerial(row+1);
            }
            Detail(row).setBrandId(object.getModel().getBrandId());
            Detail(row).setStockId(object.getModel().getStockId());
            Detail(row).setUnitType(object.getModel().getUnitType());
            Detail(row).isSerialized(object.getModel().isSerialized());
            Detail(row).setUnitPrce(object.getModel().getCost().doubleValue());
            
            System.out.println("StockID : " + Detail(row).Inventory().getStockId());
            System.out.println("Model  : " + Detail(row).Inventory().Model().getDescription());
        }

        return poJSON;
    }

    public JSONObject SearchBrand(String value, boolean byCode, int row)
            throws ExceptionInInitializerError,
            SQLException,
            GuanzonException {
        poJSON = new JSONObject();

        if(Master().getSupplierId() == null || "".equals(Master().getSupplierId())){
            poJSON.put("result", "error");
            poJSON.put("message", "Supplier is not set.");
            return poJSON;
        }
        
        Brand object = new ParamControllers(poGRider, logwrapr).Brand();
        object.setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode, Master().getIndustryId());
        if ("success".equals((String) poJSON.get("result"))) {
            if (!object.getModel().getBrandId().equals(Detail(row).getBrandId())) {
//                poJSON = checkExistingSerialId(row);
//                if ("error".equals((String) poJSON.get("result"))) {
//                    return poJSON;
//                }

                //remove existing por serial
                removePurchaseOrderReceivingSerial(row+1);
                Detail(row).setStockId("");
            }

            Detail(row).setBrandId(object.getModel().getBrandId());
        }
        return poJSON;
    }

    public JSONObject SearchModel(String value, boolean byCode, int row)
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        poJSON.put("row", row);

        if(Master().getSupplierId() == null || "".equals(Master().getSupplierId())){
            poJSON.put("result", "error");
            poJSON.put("message", "Supplier is not set.");
            return poJSON;
        }
        
        Inventory object = new InvControllers(poGRider, logwrapr).Inventory();
        object.setRecordStatus(RecordStatus.ACTIVE);
        System.out.println("Brand ID : "  + Detail(row).getBrandId());
        poJSON = object.searchRecordOfVariants(value, byCode, Master().getSupplierId(),Detail(row).getBrandId(), Master().getIndustryId(),  Master().getCategoryCode());
//        poJSON = object.searchRecordOfVariants(value, byCode);
        poJSON.put("row", row);
        if ("success".equals((String) poJSON.get("result"))) {
            poJSON = checkExistingStock(object.getModel().getStockId(), object.getModel().getDescription(), "1900-01-01", row, false);
            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }

            if (!object.getModel().getStockId().equals(Detail(row).getStockId())) {
//                poJSON = checkExistingSerialId(row);
//                if ("error".equals((String) poJSON.get("result"))) {
//                    return poJSON;
//                }
                //remove existing por serial
                removePurchaseOrderReceivingSerial(row+1);
            }
            Detail(row).setBrandId(object.getModel().getBrandId());
            Detail(row).setStockId(object.getModel().getStockId());
            Detail(row).setUnitType(object.getModel().getUnitType());
            Detail(row).isSerialized(object.getModel().isSerialized());
            Detail(row).setUnitPrce(object.getModel().getCost().doubleValue());
            
            System.out.println("StockID : " + Detail(row).Inventory().getStockId());
            System.out.println("Model  : " + Detail(row).Inventory().Model().getDescription());
        }

        return poJSON;
    }

    public JSONObject SearchLocation(String value, boolean byCode, int row)
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();

        InvLocation object = new ParamControllers(poGRider, logwrapr).InventoryLocation();
        object.setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);
        if ("success".equals((String) poJSON.get("result"))) {
            PurchaseOrderReceivingSerialList(row).setLocationId(object.getModel().getLocationId());
        }
        return poJSON;
    }

    //TODO
    private JSONObject CheckSerial(String value, int row){
        poJSON = new JSONObject();
        if(value == null || "".equals(value)){
            poJSON.put("continue", true);
            return poJSON;
        }
        try {
            String lsSQL = "";
            if(Master().getPurpose().equals(PurchaseOrderReceivingStatus.Purpose.REPLACEMENT)){
                lsSQL = MiscUtil.addCondition( getPOReturnSerial(),
                        " ( b.sSerial01 = " + SQLUtil.toSQL(value)
                                + " OR b.sSerial02 = " + SQLUtil.toSQL(value)
                                + " ) "
                                + " AND a.sTransNox = " + SQLUtil.toSQL(Detail(PurchaseOrderReceivingSerialList(row).getEntryNo()-1).getOrderNo()) );
            } else {
                Model_Inv_Serial object = new InvModels(poGRider).InventorySerial();
                lsSQL = MiscUtil.addCondition( MiscUtil.makeSelect(object),
                        " ( sSerial01 = " + SQLUtil.toSQL(value)
                                + " OR sSerial02 = " + SQLUtil.toSQL(value)
                                + " ) ");
            }
            
            System.out.println("Executing SQL: " + lsSQL);
            ResultSet loRS = poGRider.executeQuery(lsSQL);
            if (MiscUtil.RecordCount(loRS) >= 0) {
                if (loRS.next()) {
                    // Print the result set
                    System.out.println("sSerialID: " + loRS.getString("sSerialID"));
                    System.out.println("sSerial01: " + loRS.getString("sSerial01"));
                    System.out.println("sSerial02: " + loRS.getString("sSerial02"));
                    System.out.println("------------------------------------------------------------------------------");
                    
                    //Check Exisiting Receiving
                    poJSON = checkExistingPOR(row, loRS.getString("sSerialID"));
                    if ("error".equals((String) poJSON.get("result"))) {
                        poJSON.put("continue", false);
                        return poJSON;
                    }
                    
                    PurchaseOrderReceivingSerialList(row).setSerialId(loRS.getString("sSerialID"));
                    PurchaseOrderReceivingSerialList(row).setSerial01(loRS.getString("sSerial01"));
                    PurchaseOrderReceivingSerialList(row).setSerial02(loRS.getString("sSerial02"));
                    PurchaseOrderReceivingSerialList(row).setConductionStickerNo(PurchaseOrderReceivingSerialList(row).InventorySerialRegistration().getConductionStickerNo());
                    PurchaseOrderReceivingSerialList(row).setPlateNo(PurchaseOrderReceivingSerialList(row).InventorySerialRegistration().getPlateNoP());
                }
                
            } else {
                if(PurchaseOrderReceivingSerialList(row).getSerialId() != null && !"".equals(PurchaseOrderReceivingSerialList(row).getSerialId())){
                    PurchaseOrderReceivingSerialList(row).setSerial01("");
                    PurchaseOrderReceivingSerialList(row).setSerial02("");
                    PurchaseOrderReceivingSerialList(row).setConductionStickerNo("");
                    PurchaseOrderReceivingSerialList(row).setPlateNo("");
                }
                PurchaseOrderReceivingSerialList(row).setSerialId("");
            }
            MiscUtil.close(loRS);
            
        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
        }
        
        poJSON.put("result", "success");
        poJSON.put("continue", true);
        poJSON.put("message", "Record loaded successfully.");
        return poJSON;
    }
    
    public JSONObject SearchSerial(String value, int row){
        JSONObject foJSON = new JSONObject();
        try {
            String lsHeader = "Serial 01»Serial 02»Description";
            String lsCriteria = "a.sSerial01»a.sSerial02»b.sDescript";
            switch(Master().getIndustryId()){
                case PurchaseOrderReceivingStatus.MOBILEPHONE:
                    lsHeader = "IMEI 1»IMEI02»Description";
                break;
                case PurchaseOrderReceivingStatus.CAR:
                case PurchaseOrderReceivingStatus.MOTORCYCLE:
                    lsHeader = "Engine No»Frame No»Description";
                break;
            }
            
            String lsSQL = "";
            if(Master().getPurpose().equals(PurchaseOrderReceivingStatus.Purpose.REPLACEMENT)){
                lsCriteria = "b.sSerial01»b.sSerial02»c.sDescript";
                lsSQL = MiscUtil.addCondition( getPOReturnSerial(),
                              " ( b.sSerial01 LIKE " + SQLUtil.toSQL("%"+value)
                            + " OR b.sSerial02 LIKE " + SQLUtil.toSQL("%"+value)
                            + " ) "
                            + " AND c.sIndstCdx LIKE " + SQLUtil.toSQL("%"+Master().getIndustryId()) //TODO a.sIndstCdx
                            + " AND a.sTransNox = " + SQLUtil.toSQL(Detail(PurchaseOrderReceivingSerialList(row).getEntryNo()-1).getOrderNo()) 
                            + " AND a.sStockIDx = " + SQLUtil.toSQL(Detail(PurchaseOrderReceivingSerialList(row).getEntryNo()-1).getStockId()) );
            } else {
                lsSQL = MiscUtil.addCondition( getInvSerial(),
                            " ( a.sSerial01  LIKE " + SQLUtil.toSQL("%"+value)
                            + " OR a.sSerial02  LIKE " + SQLUtil.toSQL("%"+value)
                            + " ) "
                            + " AND b.sIndstCdx LIKE " + SQLUtil.toSQL("%"+Master().getIndustryId()) //TODO a.sIndstCdx
                            + " AND a.sStockIDx = " + SQLUtil.toSQL(Detail(PurchaseOrderReceivingSerialList(row).getEntryNo()-1).getStockId()) );
            }
            
            System.out.println("Executing SQL: " + lsSQL);
            foJSON = ShowDialogFX.Browse(poGRider,
                    lsSQL,
                    "",
                    lsHeader,
                    "sSerial01»sSerial02»sDescript", 
                    lsCriteria, 
                    1);
            if (foJSON != null) {
                JSONObject loJSON = new JSONObject();
                //Check Serial 1
                loJSON = checkExistingSerialNo(row, "serial01", (String) foJSON.get("sSerial01"));
                if ("error".equals((String) loJSON.get("result"))) {
                    return loJSON;
                }
                //Check Serial 2
                loJSON = checkExistingSerialNo(row, "serial02", (String) foJSON.get("sSerial02"));
                if ("error".equals((String) loJSON.get("result"))) {
                    return loJSON;
                }
                //Check Exisiting Receiving
                loJSON = checkExistingPOR(row, (String) foJSON.get("sSerialID"));
                if ("error".equals((String) loJSON.get("result"))) {
                    return loJSON;
                }
                
                PurchaseOrderReceivingSerialList(row).setSerialId((String) foJSON.get("sSerialID"));
                PurchaseOrderReceivingSerialList(row).setSerial01((String) foJSON.get("sSerial01"));
                PurchaseOrderReceivingSerialList(row).setSerial02((String) foJSON.get("sSerial02"));
                PurchaseOrderReceivingSerialList(row).setConductionStickerNo(PurchaseOrderReceivingSerialList(row).InventorySerialRegistration().getConductionStickerNo());
                PurchaseOrderReceivingSerialList(row).setPlateNo(PurchaseOrderReceivingSerialList(row).InventorySerialRegistration().getPlateNoP());
            } else {
                foJSON = new JSONObject();
                foJSON.put("result", "error");
                foJSON.put("message", "No record loaded.");
                return foJSON;
            }
        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
        }
        
        foJSON.put("result", "success");
        foJSON.put("message", "Record loaded successfully.");
        return foJSON;
    }
    
    //For Car
    public JSONObject SearchSerialRegistration(String value, int row){
        JSONObject foJSON = new JSONObject();
        String  lsHeader = "Engine No»Frame No»Description»Conduction Sticker»Plate No";
        String lsCriteria = "a.sSerial01»a.sSerial02»b.sDescript»c.sCStckrNo»c.sPlateNoP";
        String lsSQL = "";
        if(Master().getPurpose().equals(PurchaseOrderReceivingStatus.Purpose.REPLACEMENT)){
            lsCriteria = "b.sSerial01»b.sSerial02»c.sDescript";
            lsSQL = MiscUtil.addCondition( getPOReturnSerial(),
                    " ( b.sSerial01 LIKE " + SQLUtil.toSQL("%"+PurchaseOrderReceivingSerialList(row).getSerial01())
                            + " OR b.sSerial02 LIKE " + SQLUtil.toSQL("%"+PurchaseOrderReceivingSerialList(row).getSerial02())
                            + " ) "
                                    + " AND ( d.sCStckrNo LIKE " + SQLUtil.toSQL("%"+value)
                            + " OR d.sPlateNoP LIKE " + SQLUtil.toSQL("%"+value)
                            + " ) "
                                    + " AND c.sIndstCdx LIKE " + SQLUtil.toSQL("%"+Master().getIndustryId()) //TODO a.sIndstCdx
                            + " AND a.sTransNox = " + SQLUtil.toSQL(Detail(PurchaseOrderReceivingSerialList(row).getEntryNo()-1).getOrderNo()) 
                            + " AND a.sStockIDx = " + SQLUtil.toSQL(Detail(PurchaseOrderReceivingSerialList(row).getEntryNo()-1).getStockId()) );
        } else {
            lsSQL = MiscUtil.addCondition( getInvSerial(),
                    " ( a.sSerial01  LIKE " + SQLUtil.toSQL("%"+PurchaseOrderReceivingSerialList(row).getSerial01())
                            + " OR a.sSerial02  LIKE " + SQLUtil.toSQL("%"+PurchaseOrderReceivingSerialList(row).getSerial02())
                            + " ) "
                                    + " AND ( c.sCStckrNo LIKE " + SQLUtil.toSQL("%"+value)
                            + " OR c.sPlateNoP LIKE " + SQLUtil.toSQL("%"+value)
                            + " ) "
                                    + " AND b.sIndstCdx LIKE " + SQLUtil.toSQL("%"+Master().getIndustryId()) //TODO a.sIndstCdx
                            + " AND a.sStockIDx = " + SQLUtil.toSQL(Detail(PurchaseOrderReceivingSerialList(row).getEntryNo()-1).getStockId()) );
        }
        System.out.println("Executing SQL: " + lsSQL);
        foJSON = ShowDialogFX.Browse(poGRider,
                lsSQL,
                "",
                lsHeader,
                "sSerial01»sSerial02»sDescript»sCStckrNo»sPlateNoP",
                lsCriteria,
                1);
        if (foJSON != null) {
            JSONObject loJSON = new JSONObject();
            //Check Serial 1
            loJSON = checkExistingSerialNo(row, "serial01", (String) foJSON.get("sSerial01"));
            if ("error".equals((String) loJSON.get("result"))) {
                return loJSON;
            }
            //Check Serial 2
            loJSON = checkExistingSerialNo(row, "serial02", (String) foJSON.get("sSerial02"));
            if ("error".equals((String) loJSON.get("result"))) {
                return loJSON;
            }
            //Check Cs No
            loJSON = checkExistingSerialNo(row, "csno", (String) foJSON.get("sCStckrNo"));
            if ("error".equals((String) loJSON.get("result"))) {
                return loJSON;
            }
            //Check Plate No
            loJSON = checkExistingSerialNo(row, "plateno", (String) foJSON.get("sPlateNoP"));
            if ("error".equals((String) loJSON.get("result"))) {
                return loJSON;
            }
            //Check Exisiting Receiving
            loJSON = checkExistingPOR(row, (String) foJSON.get("sSerialID"));
            if ("error".equals((String) loJSON.get("result"))) {
                return loJSON;
            }
            
            PurchaseOrderReceivingSerialList(row).setSerialId((String) foJSON.get("sSerialID"));
            PurchaseOrderReceivingSerialList(row).setSerial01((String) foJSON.get("sSerial01"));
            PurchaseOrderReceivingSerialList(row).setSerial02((String) foJSON.get("sSerial02"));
            PurchaseOrderReceivingSerialList(row).setConductionStickerNo((String) foJSON.get("sCStckrNo"));
            PurchaseOrderReceivingSerialList(row).setPlateNo((String) foJSON.get("sPlateNoP"));
        } else {
            foJSON = new JSONObject();
            foJSON.put("result", "error");
            foJSON.put("message", "No record loaded.");
            return foJSON;
        }
        
        foJSON.put("result", "success");
        foJSON.put("message", "Record loaded successfully.");
        return foJSON;
    }
    
    private JSONObject setSerialId(int row){
        poJSON = new JSONObject();
        String lsSerialId = "";
        if(PurchaseOrderReceivingSerialList(row).getSerialId() == null || "".equals(PurchaseOrderReceivingSerialList(row).getSerialId())){
            lsSerialId = getSerialId(row);
        
            //Check Exisiting Receiving
            if(!lsSerialId.isEmpty()){
                poJSON = checkExistingPOR(row, lsSerialId);
                if ("error".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }
            }

            PurchaseOrderReceivingSerialList(row).setSerialId(lsSerialId);
        }
        
        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }
    
    public String getSerialId(int row){
        try {
            String lsSQL = MiscUtil.addCondition( getInvSerial(),
                    " b.sIndstCdx = " + SQLUtil.toSQL(Master().getIndustryId())
                            + " AND a.sSerial01 = " + SQLUtil.toSQL(PurchaseOrderReceivingSerialList(row).getSerial01())
                            + " AND a.sSerial02 = " + SQLUtil.toSQL(PurchaseOrderReceivingSerialList(row).getSerial02())
                            + " AND c.sCStckrNo = " + SQLUtil.toSQL(PurchaseOrderReceivingSerialList(row).getConductionStickerNo())
                            + " AND c.sPlateNoP = " + SQLUtil.toSQL(PurchaseOrderReceivingSerialList(row).getPlateNo())
            );
            
            if(Master().getPurpose().equals(PurchaseOrderReceivingStatus.Purpose.REPLACEMENT)){
                lsSQL = MiscUtil.addCondition( getPOReturnSerial(),
                                " c.sIndstCdx = " + SQLUtil.toSQL(Master().getIndustryId())
                            + " AND b.sSerial01 = " + SQLUtil.toSQL(PurchaseOrderReceivingSerialList(row).getSerial01())
                            + " AND b.sSerial02 = " + SQLUtil.toSQL(PurchaseOrderReceivingSerialList(row).getSerial02())
                            + " AND d.sCStckrNo = " + SQLUtil.toSQL(PurchaseOrderReceivingSerialList(row).getConductionStickerNo())
                            + " AND d.sPlateNoP = " + SQLUtil.toSQL(PurchaseOrderReceivingSerialList(row).getPlateNo())
                );
            }
            System.out.println("Executing SQL: " + lsSQL);
            ResultSet loRS = poGRider.executeQuery(lsSQL);
            if (MiscUtil.RecordCount(loRS) >= 0) {
                if (loRS.next()) {
                    return loRS.getString("sSerialID");
                }
            } 
            MiscUtil.close(loRS);
            
        } catch (SQLException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            return "";
        }
        
        return "";
    }
    
    private JSONObject checkExistingPOR(int row, String fsSerialId){
        poJSON = new JSONObject();
        if(fsSerialId == null || "".equals(fsSerialId)){
            return poJSON;
        }
        try {
            String lsPurpose = "Receiving";
            if(Master().getPurpose().equals(PurchaseOrderReceivingStatus.Purpose.REPLACEMENT)){
                lsPurpose = "Replacement";
            }
            
            String lsSQL = MiscUtil.addCondition( getSerialPORecieving(),
                                " b.sSerialID = " + SQLUtil.toSQL(fsSerialId)
                                + " AND a.sTransNox <> " + SQLUtil.toSQL(Master().getTransactionNo())
                                + " AND a.sIndstCdx LIKE " + SQLUtil.toSQL(Master().getIndustryId())
                                + " AND c.sOrderNox = " + SQLUtil.toSQL(Detail(PurchaseOrderReceivingSerialList(row).getEntryNo()-1).getOrderNo()) 
                                + " AND ( a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderReceivingStatus.OPEN)
                                + " OR  a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderReceivingStatus.RETURNED)
                                + " OR  a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderReceivingStatus.CONFIRMED)
                                + " OR  a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderReceivingStatus.POSTED)
                                + " OR  a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderReceivingStatus.PAID)
                                + " ) " 
                                + " AND a.cPurposex = " + SQLUtil.toSQL(Master().getPurpose()));
            
            System.out.println("Executing SQL: " + lsSQL);
            ResultSet loRS = poGRider.executeQuery(lsSQL);
            if (MiscUtil.RecordCount(loRS) >= 0) {
                if (loRS.next()) {
                    poJSON.put("result", "error");
                    poJSON.put("message", "Found existing Purchase Order "+lsPurpose+" for serial "+loRS.getString("sSerial01")+" of entry no "+PurchaseOrderReceivingSerialList(row).getEntryNo()+".\n\n"
                                    + "Check Transaction No. " +  loRS.getString("sTransNox")
                                    + " dated on " +  loRS.getDate("dTransact"));
                }
                
            } else {
                poJSON.put("result", "success");
                poJSON.put("message", "No record found.");
            }
            MiscUtil.close(loRS);
            
        } catch (SQLException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
        }
        
        return poJSON;
    }
    
//    public JSONObject computeFields()
//            throws SQLException,
//            GuanzonException {
//        poJSON = new JSONObject();
//
//        //Compute Term Due Date
//        LocalDate ldReferenceDate = strToDate(xsDateShort(Master().getReferenceDate()));
//        Long lnTerm = Math.round(Double.parseDouble(Master().Term().getTermValue().toString()));
//        Date ldTermDue = java.util.Date.from(ldReferenceDate.plusDays(lnTerm).atStartOfDay(ZoneId.systemDefault()).toInstant());
//        Master().setDueDate(ldTermDue);
//        Master().setTermDueDate(ldTermDue);
//        
//        //Compute Transaction Total
//        Double ldblTotal = 0.00;
//        Double ldblDiscount = Master().getDiscount().doubleValue();
//        Double ldblDiscountRate = Master().getDiscountRate().doubleValue();
//        
//        for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
//            ldblTotal += (Detail(lnCtr).getUnitPrce().doubleValue() * Detail(lnCtr).getQuantity().doubleValue());
//        }
//        poJSON = Master().setTransactionTotal(ldblTotal); //Sum of purchase amount
//        if(ldblDiscountRate > 0){
//            ldblDiscountRate = ldblTotal * (ldblDiscountRate / 100);
//        }
//        
//        /*Compute VAT Amount*/
//        if(pbIsFinance){
//            double ldblVatSales = 0.0000;
//            double ldblVatAmount = 0.0000;
//            double ldblVatExemptSales = 0.0000;
//            
//            double ldblDetailVatAmount = 0.0000;
//            double ldblDetailVatSales = 0.0000;
//            double ldblDetailTotal = 0.0000;
//            for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
//                if(Detail(lnCtr).isVatable()){
////                    ldblDetailVatAmount = (Detail(lnCtr).getUnitPrce().doubleValue() * 0.12) * Detail(lnCtr).getQuantity().doubleValue();
////                    ldblDetailVatSales = (Detail(lnCtr).getUnitPrce().doubleValue() * Detail(lnCtr).getQuantity().doubleValue()) - ldblDetailVatAmount;
//                    ldblDetailTotal = Detail(lnCtr).getUnitPrce().doubleValue() * Detail(lnCtr).getQuantity().doubleValue();
//                    ldblDetailVatAmount = ldblDetailTotal - (ldblDetailTotal / 1.12);
//                    ldblVatAmount = ldblVatAmount + ldblDetailVatAmount;
//                    ldblDetailVatSales = ldblDetailTotal - ldblDetailVatAmount;
//                    ldblVatSales = ldblVatSales + ldblDetailVatSales;
//                } else {
//                    ldblVatExemptSales += (Detail(lnCtr).getUnitPrce().doubleValue() * Detail(lnCtr).getQuantity().doubleValue());
//                }
//            }
//            
//            poJSON = Master().setVatSales(ldblVatSales);
//            poJSON = Master().setVatAmount(ldblVatAmount);
//            poJSON = Master().setVatExemptSales(ldblVatExemptSales);
//            poJSON = Master().setZeroVatSales(0.00); //TODO
//            if(Master().getVatRate().doubleValue() == 0.00){
//                if(getEditMode() == EditMode.UNKNOWN || Master().getEditMode() == EditMode.UNKNOWN){
//                    poJSON = Master().setVatRate(0.00); //Set default value
//                } else {
//                    poJSON = Master().setVatRate(12.00); //Set default value
//                }
//            }
//            
//        }
//        return poJSON;
//    }
    
//    //TODO
//    public Double getNetTotal(){
//         //Net Total = Vat Amount - Tax Amount
//        Double ldblNetTotal = 0.00;
//        Double ldblTotal =  Master().getTransactionTotal().doubleValue();
//        Double ldblDiscount = Master().getDiscount().doubleValue();
//        Double ldblDiscountRate = Master().getDiscountRate().doubleValue();
//        if(ldblDiscountRate > 0){
//            ldblDiscountRate = ldblTotal * (ldblDiscountRate / 100);
//        }
//        ldblDiscount = ldblDiscount + ldblDiscountRate;
//        if (Master().isVatTaxable()) {
//            ldblNetTotal = Master().getVatSales().doubleValue()
//                        + Master().getVatAmount().doubleValue()
//                        + Master().getVatExemptSales().doubleValue();
//        } else {
//            ldblNetTotal = ldblTotal + Master().getVatAmount().doubleValue();
//        }
//        
//        ldblNetTotal = (ldblNetTotal - ldblDiscount) ;
//        
//        return ldblNetTotal;
//    }
    
    
    
    public JSONObject computeFields()
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();

        //Compute Term Due Date
        LocalDate ldReferenceDate = strToDate(xsDateShort(Master().getReferenceDate()));
        Long lnTerm = Math.round(Double.parseDouble(Master().Term().getTermValue().toString()));
        Date ldTermDue = java.util.Date.from(ldReferenceDate.plusDays(lnTerm).atStartOfDay(ZoneId.systemDefault()).toInstant());
        Master().setDueDate(ldTermDue);
        Master().setTermDueDate(ldTermDue);
        
        //Compute Transaction Total
        Double ldblTotal = 0.00;
        Double ldblDiscount = Master().getDiscount().doubleValue();
        Double ldblDiscountRate = Master().getDiscountRate().doubleValue();
        
        for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
            ldblTotal += (Detail(lnCtr).getUnitPrce().doubleValue() * Detail(lnCtr).getQuantity().doubleValue());
        }
        poJSON = Master().setTransactionTotal(ldblTotal); //Sum of purchase amount
        if(ldblDiscountRate > 0){
            ldblDiscountRate = ldblTotal * (ldblDiscountRate / 100);
        }
        
        /*Compute VAT Amount*/
        if(pbIsFinance){
            boolean lbIsWithVat = false;
            double ldblVatSales = 0.0000;
            double ldblVatAmount = 0.0000;
            double ldblVatExemptSales = 0.0000;
            
            double ldblDiscountTotal = ldblDiscountRate + ldblDiscount;
            double ldblDiscountVatAmount = 0.0000;
            double ldblFreightVatAmount = 0.0000;
            double ldblDetailVatAmount = 0.0000;
            double ldblDetailVatSales = 0.0000;
            double ldblDetailTotal = 0.0000;
            for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
                if(Detail(lnCtr).isVatable()){
                    lbIsWithVat = true;
                    ldblDetailTotal = Detail(lnCtr).getUnitPrce().doubleValue() * Detail(lnCtr).getQuantity().doubleValue();
                    if(Master().isVatTaxable()){
                        ldblDetailVatAmount = ldblDetailTotal - (ldblDetailTotal / 1.12);
                    } else {
                        ldblDetailVatAmount = ldblDetailTotal * 0.12;
                    }
                    
                    ldblDetailVatSales = ldblDetailTotal - ldblDetailVatAmount;
                    ldblVatAmount = ldblVatAmount + ldblDetailVatAmount;
                    ldblVatSales = ldblVatSales + ldblDetailVatSales;
                } else {
                    ldblVatExemptSales += (Detail(lnCtr).getUnitPrce().doubleValue() * Detail(lnCtr).getQuantity().doubleValue());
                }
            }
            
            System.out.println("Detail Vat Amount " + ldblVatAmount );
            System.out.println("Detail Vat Sales " + ldblVatSales );
            
            //If all items are non vatable no vatable sales and vat amount will be computed.
            if(lbIsWithVat){
                if(Master().isVatTaxable()){
                    ldblFreightVatAmount = Master().getFreight().doubleValue() - (Master().getFreight().doubleValue() / 1.12);
                    ldblDiscountVatAmount = ldblDiscountTotal - (ldblDiscountTotal / 1.12);
                } else {
                    ldblFreightVatAmount = Master().getFreight().doubleValue() * 0.12;
                    ldblDiscountVatAmount = ldblDiscountTotal * 0.12;
                }
            
                System.out.println("Freight Vat Amount " + ldblFreightVatAmount );
                System.out.println("Freight Vat Sales " +(Master().getFreight().doubleValue() - ldblFreightVatAmount));
                System.out.println("Discount Vat Amount " + ldblDiscountVatAmount );
                System.out.println("Discount Vat Sales " +(ldblDiscountTotal - ldblDiscountVatAmount));

                ldblVatAmount = (ldblVatAmount + ldblFreightVatAmount) - ldblDiscountVatAmount;
                ldblVatSales = (ldblVatSales 
                                + (Master().getFreight().doubleValue() - ldblFreightVatAmount))
                                - (ldblDiscountTotal - ldblDiscountVatAmount);
            } 
                    
            poJSON = Master().setVatSales(ldblVatSales);
            poJSON = Master().setVatAmount(ldblVatAmount);
            poJSON = Master().setVatExemptSales(ldblVatExemptSales);
            poJSON = Master().setZeroVatSales(0.00); //TODO
            if(Master().getVatRate().doubleValue() == 0.00){
                if(getEditMode() == EditMode.UNKNOWN || Master().getEditMode() == EditMode.UNKNOWN){
                    poJSON = Master().setVatRate(0.00); //Set default value
                } else {
                    poJSON = Master().setVatRate(12.00); //Set default value
                }
            }
            
        }
        return poJSON;
    }
    
    //TODO
    public Double getNetTotal(){
         //Net Total = Vat Amount - Tax Amount
        Double ldblNetTotal = 0.00;
        Double ldblTotal =  Master().getTransactionTotal().doubleValue();
        Double ldblDiscount = Master().getDiscount().doubleValue();
        Double ldblDiscountRate = Master().getDiscountRate().doubleValue();
        Double ldblDiscountVatAmount = 0.0000;
        if(ldblDiscountRate > 0){
            ldblDiscountRate = ldblTotal * (ldblDiscountRate / 100);
        }
        ldblDiscount = ldblDiscount + ldblDiscountRate;
        if (Master().isVatTaxable()) {
//            ldblDiscountVatAmount = ldblDiscount - (ldblDiscount / 1.12);
            ldblNetTotal = (Master().getVatSales().doubleValue()
                        + Master().getVatAmount().doubleValue()
                        + Master().getVatExemptSales().doubleValue());
        } else {
//            ldblDiscountVatAmount = ldblDiscount * 0.12;
            ldblNetTotal = (ldblTotal + Master().getVatAmount().doubleValue() + Master().getFreight().doubleValue()) - ldblDiscount;
        }
        
        return ldblNetTotal;
    }
    
    public Double getAdvancePayment() {
        double ldblAmtPaid = 0.0000;
        double ldblAdvPaymentTotal = 0.0000;
        List<String> llistPurchaseOrder = new ArrayList<>();
        try {
            for(int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++){
                if(Detail(lnCtr).getOrderNo() != null && !"".equals(Detail(lnCtr).getOrderNo())){
                    if(!llistPurchaseOrder.contains(Detail(lnCtr).getOrderNo())){
                            ldblAmtPaid = Detail(lnCtr).PurchaseOrderMaster().getAmountPaid().doubleValue();
                            ldblAdvPaymentTotal = ldblAdvPaymentTotal + ldblAmtPaid;
                            llistPurchaseOrder.add(Detail(lnCtr).getOrderNo());
                    } 
                }
            }
        
        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
        }
        return ldblAdvPaymentTotal;
    }

    /*Convert Date to String*/
    private static String xsDateShort(Date fdValue) {
        if(fdValue == null){
            return "1900-01-01";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String date = sdf.format(fdValue);
        return date;
    }

    private LocalDate strToDate(String val) {
        DateTimeFormatter date_formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate localDate = LocalDate.parse(val, date_formatter);
        return localDate;
    }

    public JSONObject computeDiscountRate(double discount) {
        poJSON = new JSONObject();
        Double ldblTotal = 0.00;
        Double ldblDiscRate = 0.00;

        for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
            ldblTotal += (Detail(lnCtr).getUnitPrce().doubleValue() * Detail(lnCtr).getQuantity().doubleValue());
        }
        
        if (discount < 0 || discount > ldblTotal) {
            Master().setDiscount(0.00);
            computeDiscountRate(0.00);
            poJSON.put("result", "error");
            poJSON.put("message", "Discount amount cannot be negative or exceed the transaction total.");
            return poJSON;
        } else {
//            ldblDiscRate = (discount / ldblTotal) * 100;
//            ldblDiscRate = (discount / ldblTotal);
            //nettotal = total - discount - rate
//            Master().setDiscountRate(ldblDiscRate);

            ldblTotal = ldblTotal - (discount + ((Master().getDiscountRate().doubleValue() / 100.00) * ldblTotal));
            if(ldblTotal < 0 ){
                poJSON.put("result", "error");
                poJSON.put("message", "Invalid transaction net total.");
                return poJSON;
            }
        }
        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }

    public JSONObject computeDiscount(double discountRate) {
        poJSON = new JSONObject();
        Double ldblTotal = 0.00;
        Double ldblDiscount = 0.00;

        for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
            ldblTotal += (Detail(lnCtr).getUnitPrce().doubleValue() * Detail(lnCtr).getQuantity().doubleValue());
        }

        if (discountRate < 0 || discountRate > 100.00) {
//        if (discountRate < 0 || discountRate > 1.00) {
            Master().setDiscountRate(0.00);
            computeDiscount(0.00);
            poJSON.put("result", "error");
            poJSON.put("message", "Discount rate cannot be negative or exceed 100.00");
            return poJSON;
        } else {
//            ldblDiscount = ldblTotal * (discountRate / 100.00);
//            ldblDiscount = ldblTotal * discountRate;
            //nettotal = total - discount - rate
//            Master().setDiscount(ldblDiscount);

            ldblTotal = ldblTotal - (Master().getDiscount().doubleValue() + ((discountRate / 100.00) * ldblTotal));
            if(ldblTotal < 0 ){
                poJSON.put("result", "error");
                poJSON.put("message", "Invalid transaction net total.");
                return poJSON;
            }
        }

        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }
    
    public JSONObject removePORDetails() {
        poJSON = new JSONObject();
        Iterator<Model> detail = Detail().iterator();
        while (detail.hasNext()) {
            Model item = detail.next();
            detail.remove();
        }

        Iterator<Model_POR_Serial> porSerials = PurchaseOrderReceivingSerialList().iterator();
        while (porSerials.hasNext()) {
            Model_POR_Serial item = porSerials.next();
            porSerials.remove();
        }

        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }

    public JSONObject checkExistingStock(String stockId, String description, String expiryDate, int row, boolean isSave) {
        poJSON = new JSONObject();
        for(int lnRow = 0; lnRow <= getDetailCount() - 1; lnRow++){
//            if(Detail(lnRow).getQuantity().doubleValue() > 0){ //Why did I add this condition...
                if("".equals(Detail(row).getOrderNo()) || Detail(row).getOrderNo() == null){
                    if(lnRow != row ){
                        if  ( ("".equals(Detail(lnRow).getOrderNo()) || Detail(lnRow).getOrderNo() == null)
                                && stockId.equals(Detail(lnRow).getStockId())) {
                            poJSON.put("result", "error");
                            poJSON.put("message", description+ " already exists in table at row " + (lnRow+1) + ".");
                            poJSON.put("row", lnRow);
                            System.out.println("json row : " + poJSON.get("row"));
                            return poJSON;
                        } 
                    }
                }
//            }
        }
        
        
        
//        for (int lnRow = 0; lnRow <= getDetailCount() - 1; lnRow++) {
//            if (Detail(lnRow).getStockId() != null && !"".equals(Detail(lnRow).getStockId())) {
//                if (lnRow != row) {
////                    if (("".equals(Detail(lnRow).getOrderNo()) || Detail(lnRow).getOrderNo() == null)
////                            && (stockId.equals(Detail(lnRow).getStockId()))) {
//
//                        if (isSave) {
//                            //Do not allow saving stock id without expiry date when it expiry date exist from another row
//                            if ("1900-01-01".equals(xsDateShort(Detail(lnRow).getExpiryDate()))
//                                    && !"1900-01-01".equals(xsDateShort(Detail(row).getExpiryDate()))) {
//                                poJSON.put("result", "error");
//                                poJSON.put("message", "Expiry date cannot be empty for row " + (lnRow + 1) + ". Please provide a valid expiry date.");
//                                return poJSON;
//                            }
//                        } else {
//                            //Do not allow same stock Id without expiry date that already exists in po receiving detail
//                            if ("1900-01-01".equals(xsDateShort(Detail(lnRow).getExpiryDate()))) {
//                                poJSON.put("result", "error");
//                                poJSON.put("message", barcode + " already exists in table at row " + (lnRow + 1) + ".");
//                                return poJSON;
//                            } else {
//                                //Do not allow same stock Id with the same expiry date
//                                if (expiryDate.equals(xsDateShort(Detail(lnRow).getExpiryDate()))) {
//                                    poJSON.put("result", "error");
//                                    poJSON.put("message", barcode + " already exists in table at row " + (lnRow + 1) + ".");
//                                    Detail().remove(row);
//                                    return poJSON;
//                                }
//                            }
//                        }
////                    }
//                }
//            }
//        }

        poJSON.put("result", "success");
        poJSON.put("message", "success");
        poJSON.put("row", row);
        return poJSON;
    }

    public JSONObject loadPurchaseOrderReceiving(String formName, String companyId, String supplierId, String referenceNo) {
        try {
            if (companyId == null) {
                companyId = "";
            }
            if (supplierId == null) {
                supplierId = "";
            }
            if (referenceNo == null) {
                referenceNo = "";
            }
            
            String lsPurpose = " AND a.cPurposex = " + SQLUtil.toSQL(psPurpose);
            initSQL();
            String lsSQL = MiscUtil.addCondition(SQL_BROWSE, //" a.sIndstCdx = " + SQLUtil.toSQL(psIndustryId)
                    " a.sCompnyID = " + SQLUtil.toSQL(companyId)
                    + " AND a.sCategrCd = "+ SQLUtil.toSQL(psCategorCd)
                    + " AND a.sBranchCd = " + SQLUtil.toSQL(poGRider.getBranchCode())
                    + " AND a.sSupplier LIKE " + SQLUtil.toSQL("%" + supplierId)
                    + " AND a.sTransNox LIKE " + SQLUtil.toSQL("%" + referenceNo)
                    + lsPurpose
            );
            switch (formName) {
                case "confirmation":
                    lsSQL = lsSQL + " AND ( a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderReceivingStatus.OPEN)
                                  + " OR a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderReceivingStatus.CONFIRMED) + " ) "
                                  + " AND a.cProcessd = " + SQLUtil.toSQL("0");
                    break;
                case "history":
                    //load all purchase order receiving
                    break;
            }
            
            if(psIndustryId == null || "".equals(psIndustryId)){
                lsSQL = lsSQL + " AND (a.sIndstCdx = '' OR a.sIndstCdx = null) " ;
            } else {
                lsSQL = lsSQL + " AND a.sIndstCdx = " + SQLUtil.toSQL(psIndustryId);
            }
            
            lsSQL = lsSQL + " ORDER BY a.dTransact DESC ";
            
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
                poJSON.put("message", "No record found.");
            }
            MiscUtil.close(loRS);
        } catch (SQLException e) {
            poJSON.put("result", "error");
            poJSON.put("message", e.getMessage());
        } catch (GuanzonException ex) {
            Logger.getLogger(PurchaseOrderReceiving.class.getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
        }
        return poJSON;
    }
    
    public JSONObject loadPurchaseOrderReceiving(String formName, String companyId, String supplierId, String branchCode, String referenceNo) {
        try {
            if (companyId == null) {
                companyId = "";
            }
            if (supplierId == null || "".equals(supplierId)) {
                supplierId = "";
            }
            if (branchCode == null) {
                branchCode = "";
            }
            if (referenceNo == null) {
                referenceNo = "";
            }
            
            String lsPurpose = " AND a.cPurposex = " + SQLUtil.toSQL(psPurpose);
            initSQL();
            String lsSQL = MiscUtil.addCondition(SQL_BROWSE, //" a.sIndstCdx = " + SQLUtil.toSQL(psIndustryId)
                    " a.sCompnyID = " + SQLUtil.toSQL(companyId)
                    + " AND a.sCategrCd = "+ SQLUtil.toSQL(psCategorCd)
                    + " AND a.sBranchCd LIKE " + SQLUtil.toSQL("%" + branchCode)
                    + " AND a.sSupplier LIKE " + SQLUtil.toSQL("%" + supplierId)
                    + " AND a.sTransNox LIKE " + SQLUtil.toSQL("%" + referenceNo)
                    + lsPurpose
            );
            switch (formName) {
                case "siposting":
                    lsSQL = lsSQL + " AND a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderReceivingStatus.CONFIRMED);
                    break;
                case "confirmation":
                    lsSQL = lsSQL + " AND ( a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderReceivingStatus.OPEN)
                                  + " OR a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderReceivingStatus.CONFIRMED) + " ) "
                                  + " AND a.cProcessd = " + SQLUtil.toSQL("0");
                    break;
                case "history":
                    //load all purchase order receiving
                    break;
            }
            
            if(psIndustryId == null || "".equals(psIndustryId)){
                lsSQL = lsSQL + " AND (a.sIndstCdx = '' OR a.sIndstCdx = null) " ;
            } else {
                lsSQL = lsSQL + " AND a.sIndstCdx = " + SQLUtil.toSQL(psIndustryId);
            }
            
            lsSQL = lsSQL + " ORDER BY a.dTransact DESC ";
            
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
                poJSON.put("message", "No record found.");
            }
            MiscUtil.close(loRS);
        } catch (SQLException e) {
            poJSON.put("result", "error");
            poJSON.put("message", e.getMessage());
        } catch (GuanzonException ex) {
            Logger.getLogger(PurchaseOrderReceiving.class.getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
        }
        return poJSON;
    }
    
    public JSONObject loadUnPostPurchaseOrderReceiving(String supplier, String branch, String referenceNo) {
        try {
            if (supplier == null || "".equals(supplier)) {
                supplier = "";
            }
            if (branch == null) {
                branch = "";
            }
            if (referenceNo == null) {
                referenceNo = "";
            }
            initSQL();
            String lsSQL = MiscUtil.addCondition(SQL_BROWSE, //" a.sIndstCdx = " + SQLUtil.toSQL(psIndustryId)
                    " a.sCompnyID = " + SQLUtil.toSQL(psCompanyId)
                    + " AND a.sCategrCd = "+ SQLUtil.toSQL(psCategorCd)
                    + " AND e.sBranchNm LIKE " + SQLUtil.toSQL("%" + branch)
                    + " AND b.sCompnyNm LIKE " + SQLUtil.toSQL("%" + supplier)
                    + " AND a.sTransNox LIKE " + SQLUtil.toSQL("%" + referenceNo)
                    + " AND a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderReceivingStatus.CONFIRMED)
            );
            
            if(psIndustryId == null || "".equals(psIndustryId)){
                lsSQL = lsSQL + " AND (a.sIndstCdx = '' OR a.sIndstCdx = null) " ;
            } else {
                lsSQL = lsSQL + " AND a.sIndstCdx = " + SQLUtil.toSQL(psIndustryId);
            }
            
            lsSQL = lsSQL + " ORDER BY a.dTransact DESC ";
            
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
                poJSON.put("message", "No record found.");
            }
            MiscUtil.close(loRS);
        } catch (SQLException e) {
            poJSON.put("result", "error");
            poJSON.put("message", e.getMessage());
        } catch (GuanzonException ex) {
            Logger.getLogger(PurchaseOrderReceiving.class.getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
        }
        return poJSON;
    }

    public JSONObject loadAttachments()
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        paAttachments = new ArrayList<>();

        TransactionAttachment loAttachment = new SysTableContollers(poGRider, null).TransactionAttachment();
        List loList = loAttachment.getAttachments(SOURCE_CODE, Master().getTransactionNo());
        for (int lnCtr = 0; lnCtr <= loList.size() - 1; lnCtr++) {
            paAttachments.add(TransactionAttachment());
            poJSON = paAttachments.get(getTransactionAttachmentCount() - 1).openRecord((String) loList.get(lnCtr));
            if ("success".equals((String) poJSON.get("result"))) {
                if(Master().getEditMode() == EditMode.UPDATE){
                   poJSON = paAttachments.get(getTransactionAttachmentCount() - 1).updateRecord();
                }
                System.out.println(paAttachments.get(getTransactionAttachmentCount() - 1).getModel().getTransactionNo());
                System.out.println(paAttachments.get(getTransactionAttachmentCount() - 1).getModel().getSourceNo());
                System.out.println(paAttachments.get(getTransactionAttachmentCount() - 1).getModel().getSourceCode());
                System.out.println(paAttachments.get(getTransactionAttachmentCount() - 1).getModel().getFileName());
            }
        }
        return poJSON;
    }
    
//    private String getCategory(){
//        switch(psIndustryId){
//            case "01": //Mobile Phone
//                if("0001".equals(psCategorCd)){
//                    return SQLUtil.toSQL(psCategorCd);
//                } else {
//                    return SQLUtil.toSQL("0005") + ", " +  SQLUtil.toSQL("0006");
//                }
//            case "02": //Motorcycle
//                if("0010".equals(psCategorCd)){
//                    return SQLUtil.toSQL(psCategorCd);
//                }
//                //Spare Parts, Accessories , Giveaways
//                if("0011".equals(psCategorCd)){
//                    return SQLUtil.toSQL(psCategorCd) + ", " + SQLUtil.toSQL("0012") + ", " +  SQLUtil.toSQL("0013");
//                } else {
//                    return SQLUtil.toSQL("0017") + ", " +  SQLUtil.toSQL("0018");
//                }
//            case "03": //Vehicle
//                if("0015".equals(psCategorCd)){
//                    return SQLUtil.toSQL(psCategorCd);
//                }
//                //Spare Parts
//                if("0016".equals(psCategorCd)){
//                    return SQLUtil.toSQL(psCategorCd);
//                } else { //Accessories , Giveaways
//                    return SQLUtil.toSQL("0017") + ", " +  SQLUtil.toSQL("0018");
//                }
//            case "04": //Hospitality
//                if("0023".equals(psCategorCd)){
//                    return SQLUtil.toSQL(psCategorCd);
//                } else { // Food Service, Baked Goods TODO GENERAL
//                    return SQLUtil.toSQL("0021") + ", " +  SQLUtil.toSQL("0022");
//                }
//            case "05": //Los Pedritos
//                // Food Service, Baked Goods TODO GENERAL
//                return SQLUtil.toSQL("0019") + ", " +  SQLUtil.toSQL("0020");
//            case "06": //Main Office
//                return SQLUtil.toSQL(psCategorCd);
//        }
//        
//        return psCategorCd;
//    }

    public JSONObject getApprovedPurchaseOrder() {
        try {
//            String lsSupplier = Master().getSupplierId().isEmpty()
//                    ? " (a.sSupplier = null OR TRIM(a.sSupplier) = '')"
//                    : " a.sSupplier = " + SQLUtil.toSQL(Master().getSupplierId());

            paPOMaster = new ArrayList<>();
            String lsSQL = " SELECT "
                    + "   a.sTransNox "
                    + " , a.dTransact "
                    + " , c.sCompnyNm AS sSupplier "
                    + " FROM po_master a "
                    + " LEFT JOIN po_detail b on b.sTransNox = a.sTransNox "
                    + " LEFT JOIN client_master c ON c.sClientID = a.sSupplier ";
            
            if(poGRider.isMainOffice() || poGRider.isWarehouse()){
                lsSQL = MiscUtil.addCondition(lsSQL, " a.sIndstCdx = " + SQLUtil.toSQL(psIndustryId)
                        + " AND a.sCompnyID LIKE " + SQLUtil.toSQL("%" + psCompanyId)
                        + " AND a.sSupplier LIKE " + SQLUtil.toSQL("%"+ Master().getSupplierId())
                        + " AND a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderStatus.APPROVED)
                        + " AND a.sCategrCd = "+ SQLUtil.toSQL(psCategorCd)
                        + " AND b.nQuantity > b.nReceived "
                );
            } else {
                lsSQL = MiscUtil.addCondition(lsSQL, " a.sIndstCdx = " + SQLUtil.toSQL(psIndustryId)
                        + " AND a.sCompnyID LIKE " + SQLUtil.toSQL("%" + psCompanyId)
                        + " AND a.sSupplier LIKE " + SQLUtil.toSQL("%"+ Master().getSupplierId())
                        + " AND a.sDestinat = " + SQLUtil.toSQL(poGRider.getBranchCode())
                        + " AND a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderStatus.APPROVED)
                        + " AND a.sCategrCd = "+ SQLUtil.toSQL(psCategorCd)
                        + " AND b.nQuantity > b.nReceived "
                );
            }
  
            lsSQL = lsSQL + " GROUP BY a.sTransNox "
                    + " ORDER BY dTransact ASC";
            
            System.out.println("Executing SQL: " + lsSQL);

            ResultSet loRS = poGRider.executeQuery(lsSQL);
            poJSON = new JSONObject();
            int lnctr = 0;

            if (MiscUtil.RecordCount(loRS) > 0) {
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
                poJSON.put("result", "error");
                poJSON.put("continue", true);
                poJSON.put("message", "No approved purchase order found .");
            }
            MiscUtil.close(loRS);
        } catch (SQLException ex) {
            poJSON.put("result", "error");
            poJSON.put("continue", false);
            poJSON.put("message", ex.getMessage());
        } catch (GuanzonException ex) {
            Logger.getLogger(PurchaseOrderReceiving.class.getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON.put("result", "error");
            poJSON.put("continue", false);
            poJSON.put("message", MiscUtil.getException(ex));
        }
        return poJSON;
    }

    public JSONObject addPurchaseOrderToPORDetail(String transactionNo)
            throws CloneNotSupportedException,
            SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        boolean lbExist = false;
        boolean lbReceived = false;
        int lnRow = 0;
        double lnAddOrderQty = 0.00;
        PurchaseOrderControllers loTrans = new PurchaseOrderControllers(poGRider, logwrapr);
        poJSON = loTrans.PurchaseOrder().InitTransaction();
        if ("success".equals((String) poJSON.get("result"))) {
            poJSON = loTrans.PurchaseOrder().OpenTransaction(transactionNo);
            if ("success".equals((String) poJSON.get("result"))) {
                for (int lnCtr = 0; lnCtr <= loTrans.PurchaseOrder().getDetailCount() - 1; lnCtr++) {
                    
                    //Check existing supplier
                    if(Master().getSupplierId() == null || "".equals(Master().getSupplierId())){
                        Master().setSupplierId(loTrans.PurchaseOrder().Master().getSupplierID());
                    } else {
                        if(!Master().getSupplierId().equals(loTrans.PurchaseOrder().Master().getSupplierID())){
                            if(getDetailCount() >= 0 ){
                                poJSON.put("result", "error");
                                poJSON.put("message", "Supplier must be equal to selected purchase order supplier.");
                                return poJSON;
                            } else {
                                Master().setSupplierId(loTrans.PurchaseOrder().Master().getSupplierID());
                            }
                        }
                    }
                    
                    for (lnRow = 0; lnRow <= getDetailCount() - 1; lnRow++) {
                        if (Detail(lnRow).getOrderNo() != null && !"".equals(Detail(lnRow).getOrderNo())) {
                            //check when pre-owned po is already exists in detail. 
                            //if exist only pre-owned purchase order will allow to insert in por detail 
                            if (Detail(lnRow).PurchaseOrderMaster().getPreOwned() != loTrans.PurchaseOrder().Master().getPreOwned()) {
                                poJSON.put("result", "error");
                                poJSON.put("message", "Purchase orders for pre-owned items cannot be combined with purchase orders for new items.");
                                return poJSON;
                            }
                        }
                        
                        if (Detail(lnRow).getOrderNo().equals(loTrans.PurchaseOrder().Detail(lnCtr).getTransactionNo())
                                && (Detail(lnRow).getStockId().equals(loTrans.PurchaseOrder().Detail(lnCtr).getStockID()))) {
                            lbExist = true;
                            break;
                        }
                    }

                    if (!lbExist) {
                        //Only insert po detail that has item to receive
                        if (loTrans.PurchaseOrder().Detail(lnCtr).getQuantity().doubleValue() > loTrans.PurchaseOrder().Detail(lnCtr).getReceivedQuantity().doubleValue()) {
                            Detail(getDetailCount() - 1).setBrandId(loTrans.PurchaseOrder().Detail(lnCtr).Inventory().getBrandId());
                            Detail(getDetailCount() - 1).setOrderNo(loTrans.PurchaseOrder().Detail(lnCtr).getTransactionNo());
                            Detail(getDetailCount() - 1).setStockId(loTrans.PurchaseOrder().Detail(lnCtr).getStockID());
                            Detail(getDetailCount() - 1).setUnitType(loTrans.PurchaseOrder().Detail(lnCtr).Inventory().getUnitType());
                            Detail(getDetailCount() - 1).setOrderQty(loTrans.PurchaseOrder().Detail(lnCtr).getQuantity().doubleValue() - loTrans.PurchaseOrder().Detail(lnCtr).getReceivedQuantity().doubleValue());
                            Detail(getDetailCount() - 1).setWhCount(loTrans.PurchaseOrder().Detail(lnCtr).getQuantity().doubleValue() - loTrans.PurchaseOrder().Detail(lnCtr).getReceivedQuantity().doubleValue());
                            Detail(getDetailCount() - 1).setUnitPrce(loTrans.PurchaseOrder().Detail(lnCtr).getUnitPrice());
                            Detail(getDetailCount() - 1).isSerialized(loTrans.PurchaseOrder().Detail(lnCtr).Inventory().isSerialized());

                            AddDetail();
                            lbReceived = true;
                        }
                    } else {
                        //sum order qty based on existing stock id in POR Detail
                        for (int lnOrder = 0; lnOrder <= loTrans.PurchaseOrder().getDetailCount() - 1; lnOrder++) {
                            if(Detail(lnRow).getOrderNo().equals(loTrans.PurchaseOrder().Detail(lnOrder).getTransactionNo())){
                                if(Detail(lnRow).getStockId().equals(loTrans.PurchaseOrder().Detail(lnOrder).getStockID())){
                                    lnAddOrderQty = lnAddOrderQty + (loTrans.PurchaseOrder().Detail(lnOrder).getQuantity().doubleValue() - loTrans.PurchaseOrder().Detail(lnOrder).getReceivedQuantity().doubleValue());
                                }
                            }
                        }
                        
                        Detail(lnRow).setOrderQty(lnAddOrderQty);
                        lbReceived = true;
                    }
                    
                    lbExist = false;
                    lnAddOrderQty = 0;
                }
                
                if(!lbReceived){
                    poJSON.put("result", "error");
                    poJSON.put("message", "No remaining order to be receive for Order No. " + transactionNo + ".");
                    return poJSON;
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
    
    //Purchase Order Return
    public JSONObject getPurchaseOrderReturn(String fsSupplier) {
        try {
            paPOReturnMaster = new ArrayList<>();
            String lsSQL = " SELECT "
                    + "   a.sTransNox "
                    + " , a.dTransact "
                    + " , c.sCompnyNm AS sSupplier "
                    + " FROM po_return_master a "
                    + " LEFT JOIN po_return_detail b on b.sTransNox = a.sTransNox "
                    + " LEFT JOIN client_master c ON c.sClientID = a.sSupplier ";
            
            if(poGRider.isMainOffice() || poGRider.isWarehouse()){
                lsSQL = MiscUtil.addCondition(lsSQL, " a.sIndstCdx = " + SQLUtil.toSQL(psIndustryId)
                        + " AND a.sCompnyID LIKE " + SQLUtil.toSQL("%" + psCompanyId)
                        + " AND c.sCompnyNm LIKE " + SQLUtil.toSQL("%"+ fsSupplier)
                        + " AND a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderReturnStatus.CONFIRMED)
                        + " AND a.sCategrCd = "+ SQLUtil.toSQL(psCategorCd)
                        + " AND b.nQuantity > b.nReceived "
                );
            } else {
                lsSQL = MiscUtil.addCondition(lsSQL, " a.sIndstCdx = " + SQLUtil.toSQL(psIndustryId)
                        + " AND a.sCompnyID LIKE " + SQLUtil.toSQL("%" + psCompanyId)
                        + " AND c.sCompnyNm LIKE " + SQLUtil.toSQL("%"+ fsSupplier)
                        + " AND a.sBranchCd = " + SQLUtil.toSQL(poGRider.getBranchCode())
                        + " AND a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderReturnStatus.CONFIRMED)
                        + " AND a.sCategrCd = "+ SQLUtil.toSQL(psCategorCd)
                        + " AND b.nQuantity > b.nReceived "
                );
            }
  
            lsSQL = lsSQL + " GROUP BY a.sTransNox "
                    + " ORDER BY dTransact ASC";
            
            System.out.println("Executing SQL: " + lsSQL);

            ResultSet loRS = poGRider.executeQuery(lsSQL);
            poJSON = new JSONObject();
            int lnctr = 0;

            if (MiscUtil.RecordCount(loRS) > 0) {
                while (loRS.next()) {
                    // Print the result set
                    System.out.println("sTransNox: " + loRS.getString("sTransNox"));
                    System.out.println("dTransact: " + loRS.getDate("dTransact"));
                    System.out.println("sSupplier: " + loRS.getString("sSupplier"));
                    System.out.println("------------------------------------------------------------------------------");

                    paPOReturnMaster.add(PurchaseOrderReturnMaster());
                    paPOReturnMaster.get(paPOReturnMaster.size() - 1).openRecord(loRS.getString("sTransNox"));
                    lnctr++;
                }

                System.out.println("Records found: " + lnctr);
                poJSON.put("result", "success");
                poJSON.put("message", "Record loaded successfully.");
            } else {
                poJSON.put("result", "error");
                poJSON.put("continue", true);
                poJSON.put("message", "No confirmed purchase order return found .");
            }
            MiscUtil.close(loRS);
        } catch (SQLException ex) {
            poJSON.put("result", "error");
            poJSON.put("continue", false);
            poJSON.put("message", ex.getMessage());
        } catch (GuanzonException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON.put("result", "error");
            poJSON.put("continue", false);
            poJSON.put("message", MiscUtil.getException(ex));
        }
        return poJSON;
    }
    
    public JSONObject addPurchaseOrderReturnToPORDetail(String transactionNo)
            throws CloneNotSupportedException,
            SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        boolean lbExist = false;
        boolean lbReceived = false;
        int lnRow = 0;
        double lnAddOrderQty = 0.00;
        boolean lbReturnPreOwned = false;
        boolean lbReplacePreOwned = false;
        //For Checking
        PurchaseOrderReturnControllers loReturn = new PurchaseOrderReturnControllers(poGRider, logwrapr);
        PurchaseOrderReturnControllers loTrans = new PurchaseOrderReturnControllers(poGRider, logwrapr);
        poJSON = loTrans.PurchaseOrderReturn().InitTransaction();
        if ("success".equals((String) poJSON.get("result"))) {
            poJSON = loTrans.PurchaseOrderReturn().OpenTransaction(transactionNo);
            if ("success".equals((String) poJSON.get("result"))) {
                for (int lnCtr = 0; lnCtr <= loTrans.PurchaseOrderReturn().getDetailCount() - 1; lnCtr++) {
                    //Check existing supplier
                    if(Master().getSupplierId() == null || "".equals(Master().getSupplierId())){
                        Master().setSupplierId(loTrans.PurchaseOrderReturn().Master().getSupplierId());
                    } else {
                        if(!Master().getSupplierId().equals(loTrans.PurchaseOrderReturn().Master().getSupplierId())){
                            if(getDetailCount() >= 0 ){
                                poJSON.put("result", "error");
                                poJSON.put("message", "Supplier must be equal to selected purchase order return supplier.");
                                return poJSON;
                            } else {
                                Master().setSupplierId(loTrans.PurchaseOrderReturn().Master().getSupplierId());
                            }
                        }
                    }
                    
                    for (lnRow = 0; lnRow <= getDetailCount() - 1; lnRow++) {
                        //Check the detail
                        poJSON = loReturn.PurchaseOrderReturn().InitTransaction();
                        poJSON = loReturn.PurchaseOrderReturn().OpenTransaction(Detail(lnRow).getOrderNo());
                        for(int lnRec = 0; lnRec <= loReturn.PurchaseOrderReturn().getDetailCount()-1;lnRec++){
                            if (loReturn.PurchaseOrderReturn().Detail(lnRec).getSourceNo()!= null && !"".equals(loReturn.PurchaseOrderReturn().Detail(lnRec).getSourceNo())) {
                                lbReplacePreOwned = loReturn.PurchaseOrderReturn().Detail(lnRec).PurchaseOrderMaster().getPreOwned();
                                break;
                            }
                        }
                        
//                        poJSON = loReceiving.PurchaseOrderReceiving().InitTransaction();
//                        poJSON = loReceiving.PurchaseOrderReceiving().OpenTransaction(loReturn.PurchaseOrderReturn().Master().getSourceNo());
//                        for(int lnRec = 0; lnRec <= loReceiving.PurchaseOrderReceiving().getDetailCount()-1;lnRec++){
//                            if (loReceiving.PurchaseOrderReceiving().Detail(lnRow).getOrderNo() != null && !"".equals(loReceiving.PurchaseOrderReceiving().Detail(lnRow).getOrderNo())) {
//                                lbReplacePreOwned = loReceiving.PurchaseOrderReceiving().Detail(lnRow).PurchaseOrderMaster().getPreOwned();
//                                break;
//                            }
//                        }

                        //Check the return to be insert in detail
                        poJSON = loReturn.PurchaseOrderReturn().InitTransaction();
                        poJSON = loReturn.PurchaseOrderReturn().OpenTransaction(transactionNo);
                        for(int lnRec = 0; lnRec <= loReturn.PurchaseOrderReturn().getDetailCount()-1;lnRec++){
                            if (loReturn.PurchaseOrderReturn().Detail(lnRec).getSourceNo()!= null && !"".equals(loReturn.PurchaseOrderReturn().Detail(lnRec).getSourceNo())) {
                                lbReturnPreOwned = loReturn.PurchaseOrderReturn().Detail(lnRec).PurchaseOrderMaster().getPreOwned();
                                break;
                            }
                        }
//                        poJSON = loReceiving.PurchaseOrderReceiving().InitTransaction();
//                        poJSON = loReceiving.PurchaseOrderReceiving().OpenTransaction(loReturn.PurchaseOrderReturn().Master().getSourceNo());
//                        for(int lnRec = 0; lnRec <= loReceiving.PurchaseOrderReceiving().getDetailCount()-1;lnRec++){
//                            if (loReceiving.PurchaseOrderReceiving().Detail(lnRow).getOrderNo() != null && !"".equals(loReceiving.PurchaseOrderReceiving().Detail(lnRow).getOrderNo())) {
//                                lbReturnPreOwned = loReceiving.PurchaseOrderReceiving().Detail(lnRow).PurchaseOrderMaster().getPreOwned();
//                                break;
//                            }
//                        }
                        
                        //check when pre-owned po is already exists in detail. 
                        //if exist only pre-owned purchase order will allow to insert in por detail 
                        if (lbReplacePreOwned != lbReturnPreOwned) {
                            poJSON.put("result", "error");
                            poJSON.put("message", "Purchase orders for pre-owned items cannot be combined with purchase orders for new items.");
                            return poJSON;
                        }
                        
                        if (Detail(lnRow).getOrderNo().equals(loTrans.PurchaseOrderReturn().Detail(lnCtr).getTransactionNo())
                                && (Detail(lnRow).getStockId().equals(loTrans.PurchaseOrderReturn().Detail(lnCtr).getStockId()))) {
                            lbExist = true;
                            break;
                        }
                    }

                    if (!lbExist) {
                        //Only insert po detail that has item to receive
                        if (loTrans.PurchaseOrderReturn().Detail(lnCtr).getQuantity().doubleValue() > loTrans.PurchaseOrderReturn().Detail(lnCtr).getReceivedQty().doubleValue()) {
                            Detail(getDetailCount() - 1).setBrandId(loTrans.PurchaseOrderReturn().Detail(lnCtr).Inventory().getBrandId());
                            Detail(getDetailCount() - 1).setOrderNo(loTrans.PurchaseOrderReturn().Detail(lnCtr).getTransactionNo());
                            Detail(getDetailCount() - 1).setStockId(loTrans.PurchaseOrderReturn().Detail(lnCtr).getStockId());
                            Detail(getDetailCount() - 1).setUnitType(loTrans.PurchaseOrderReturn().Detail(lnCtr).Inventory().getUnitType());
                            Detail(getDetailCount() - 1).setOrderQty(loTrans.PurchaseOrderReturn().Detail(lnCtr).getQuantity().doubleValue() - loTrans.PurchaseOrderReturn().Detail(lnCtr).getReceivedQty().doubleValue());
                            Detail(getDetailCount() - 1).setWhCount(loTrans.PurchaseOrderReturn().Detail(lnCtr).getQuantity().doubleValue() - loTrans.PurchaseOrderReturn().Detail(lnCtr).getReceivedQty().doubleValue());
                            Detail(getDetailCount() - 1).setUnitPrce(loTrans.PurchaseOrderReturn().Detail(lnCtr).getUnitPrce());
                            Detail(getDetailCount() - 1).isSerialized(loTrans.PurchaseOrderReturn().Detail(lnCtr).Inventory().isSerialized());
                            
                            AddDetail();
                            lbReceived = true;
                        }
                    } else {
                        //sum order qty based on existing stock id in POR Detail
                        for (int lnOrder = 0; lnOrder <= loTrans.PurchaseOrderReturn().getDetailCount() - 1; lnOrder++) {
                            if(Detail(lnRow).getOrderNo().equals(loTrans.PurchaseOrderReturn().Detail(lnOrder).getTransactionNo())){
                                if(Detail(lnRow).getStockId().equals(loTrans.PurchaseOrderReturn().Detail(lnOrder).getStockId())){
                                    lnAddOrderQty = lnAddOrderQty + (loTrans.PurchaseOrderReturn().Detail(lnOrder).getQuantity().doubleValue() - loTrans.PurchaseOrderReturn().Detail(lnOrder).getReceivedQty().doubleValue());
                                }
                            }
                        }
                        
                        Detail(lnRow).setOrderQty(lnAddOrderQty);
                        lbReceived = true;
                        
                    }
                    
                    lbExist = false;
                    lnAddOrderQty = 0;
                }
                
                if(!lbReceived){
                    poJSON.put("result", "error");
                    poJSON.put("message", "No remaining order to be receive for Order No. " + transactionNo + ".");
                    return poJSON;
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

    //Purchase Order Receiving Serial
    public JSONObject getPurchaseOrderReceivingSerial(int entryNo)
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        
        if(!Detail(entryNo-1).isSerialized()){
            poJSON.put("result", "success");
            return poJSON;
        }

        if (paOthers == null) {
            paOthers = new ArrayList<>();
        }

        try {
            String lsSQL = " SELECT "
                    + "    sTransNox "
                    + " ,  nEntryNox "
                    + " ,  sSerialID "
                    + " FROM po_receiving_serial ";

            lsSQL = MiscUtil.addCondition(lsSQL, " sTransNox = " + SQLUtil.toSQL(Master().getTransactionNo())
                    + " AND nEntryNox = " + SQLUtil.toSQL(entryNo)
            ) + " ORDER BY sSerialID ASC ";

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
                    if ("error".equals((String) poJSON.get("result"))) {
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
            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
            MiscUtil.close(loRS);
        } catch (SQLException e) {
            poJSON.put("result", "error");
            poJSON.put("message", e.getMessage());
        } catch (GuanzonException ex) {
            Logger.getLogger(PurchaseOrderReceiving.class.getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
        }

        return poJSON;
    }

    private JSONObject populatePurchaseOrderReceivingSerial(int entryNo, String serialId)
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        double lnQuantity = Detail(entryNo - 1).getQuantity().doubleValue();
        int lnSerialCnt = 0;
        boolean lbShowMessage = false;

        if (!serialId.isEmpty()) {
            //1. Checke Serial if already exists in POR Serial list
            for (int lnCtr = 0; lnCtr <= getPurchaseOrderReceivingSerialCount() - 1; lnCtr++) {
                if (paOthers.get(lnCtr).getSerialId().equals(serialId)) {
                    poJSON.put("result", "success");
                    return poJSON;
                }
            }

            paOthers.add(PurchaseOrderReceivingSerial());
            poJSON = paOthers.get(getPurchaseOrderReceivingSerialCount() - 1).openRecord(Master().getTransactionNo(), entryNo, serialId);
            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }

            poJSON = paOthers.get(getPurchaseOrderReceivingSerialCount() - 1).updateRecord();
            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }

            paOthers.get(getPurchaseOrderReceivingSerialCount() - 1).setSerial01(paOthers.get(getPurchaseOrderReceivingSerialCount() - 1).InventorySerial().getSerial01());
            paOthers.get(getPurchaseOrderReceivingSerialCount() - 1).setSerial02(paOthers.get(getPurchaseOrderReceivingSerialCount() - 1).InventorySerial().getSerial02());
            paOthers.get(getPurchaseOrderReceivingSerialCount() - 1).setConductionStickerNo(paOthers.get(getPurchaseOrderReceivingSerialCount() - 1).InventorySerialRegistration().getConductionStickerNo());
            paOthers.get(getPurchaseOrderReceivingSerialCount() - 1).setPlateNo(paOthers.get(getPurchaseOrderReceivingSerialCount() - 1).InventorySerialRegistration().getPlateNoP());

        } else {

            //get total count of por serial per entry no
            for (int lnCtr = 0; lnCtr <= getPurchaseOrderReceivingSerialCount() - 1; lnCtr++) {
                if (paOthers.get(lnCtr).getEntryNo() == entryNo) {
                    lnSerialCnt++;
                }
            }

            //if por serial is less than the quantity declared in por detail add row
            if (lnSerialCnt < lnQuantity) {
                //Add row for others
                while (lnSerialCnt < lnQuantity) {
                    paOthers.add(PurchaseOrderReceivingSerial());
                    poJSON = paOthers.get(getPurchaseOrderReceivingSerialCount() - 1).newRecord();
                    if ("success".equals((String) poJSON.get("result"))) {
                        paOthers.get(getPurchaseOrderReceivingSerialCount() - 1).setEntryNo(entryNo);
                        paOthers.get(getPurchaseOrderReceivingSerialCount() - 1).setStockId(Detail(entryNo - 1).getStockId());
                        paOthers.get(getPurchaseOrderReceivingSerialCount() - 1).setSerialId("");
                    } else {
                        return poJSON;
                    }

                    lnSerialCnt++;
                }
            }
            
//            for(int lnCtr = 0; lnCtr <= getPurchaseOrderReceivingSerialCount()-1;lnCtr++){
//                System.out.println("CLASS START POR SERIAL LIST");
//                System.out.println(" getEntryNo : " + PurchaseOrderReceivingSerialList(lnCtr).getEntryNo());
//                System.out.println(" getStockId : " + PurchaseOrderReceivingSerialList(lnCtr).getStockId());
//                System.out.println(" getSerial01 : " + PurchaseOrderReceivingSerialList(lnCtr).getSerial01());
//                System.out.println(" getSerial02 : " + PurchaseOrderReceivingSerialList(lnCtr).getSerial02());
//                System.out.println(" getLocationId : " + PurchaseOrderReceivingSerialList(lnCtr).getLocationId());
//                System.out.println("CLASS END POR SERIAL LIST");
//            }
//            else {
//                //Remove row for excess por serial
//                while(lnSerialCnt > lnQuantity){
//                    //get total count of serial per entry no
//                    for (int lnCtr = getPurchaseOrderReceivingSerialCount() - 1; lnCtr >= 0; lnCtr--) {  // Iterate backward
//                        if(paOthers.get(lnCtr).getEntryNo() == entryNo ){
//                            //Priority to remove the empty serial01 || emptry serial02
//                            if((paOthers.get(lnCtr).getSerial01() == null || "".equals(paOthers.get(lnCtr).getSerial01())) ||
//                                    (paOthers.get(lnCtr).getSerial02() == null || "".equals(paOthers.get(lnCtr).getSerial02()))){
//                                paOthers.remove(lnCtr);
//                                lnSerialCnt--;
//                                break;
//                            } 
//                            
//                            if ((paOthers.get(lnCtr).getSerial01() != null && !"".equals(paOthers.get(lnCtr).getSerial01())) ||
//                                    (paOthers.get(lnCtr).getSerial02() != null && !"".equals(paOthers.get(lnCtr).getSerial02()))){
//                                if(!lbShowMessage){
//                                    if (ShowMessageFX.OkayCancel(null, "Purchase Order Receiving Serial", 
//                                            "The quantity has been reduced. Do you want to disregard the changes and delete the serial number? ") == true) {
//                                    } else {
//                                        poJSON.put("result", "error");
//                                        poJSON.put("message", "You have cancelled the operation. The serial number was not deleted.");
//                                        return poJSON; 
//                                    }
//                                    lbShowMessage = true; //set true to identify that prompt message is already called
//                                }
//                                paOthers.remove(lnCtr);
//                                lnSerialCnt--;
//                                break;
//                            } 
//                        } 
//                    }
//                }
//            }
        }

        poJSON.put("result", "success");
        return poJSON;
    }
    
    private String getColumnName (String columnName){
        switch(columnName){
            case "serial01":
                switch(Master().getIndustryId()){
                    case "01":
                        return "IMEI 1";
                    case "02":
                    case "03":
                        return "Engine No";
                }
                
            break;
            case "serial02":
                switch(Master().getIndustryId()){
                    case "01":
                        return "IMEI 2";
                    case "02":
                    case "03":
                        return "Frame No";
                }
            break;
            case "csno":
                if("03".equals(Master().getIndustryId())){
                    return "CS No ";
                }
            break;
            case "plateno":
                if("03".equals(Master().getIndustryId())){
                    return "Plate No ";
                }
            break;
            default:
                return "Serial";
        }
        
        return "";
    }
    
    public JSONObject checkExistingSerialNo(int row, String columnName, String value) {
        poJSON = new JSONObject();
        int lnPrevEntryNo = -1;
        int lnRow = 0;
        int lnSerialRow = 0;
        String lsColName = "Serial";
        if(value == null || "".equals(value)){
            poJSON.put("set", false);
            return poJSON;
        }
        
        lsColName = getColumnName(columnName);

        //check when serial id already exists do not allow to change brand / model
        for (int lnCtr = 0; lnCtr <= getPurchaseOrderReceivingSerialCount() - 1; lnCtr++) {
            if(lnPrevEntryNo < 0){
                lnRow = 1;
                lnPrevEntryNo = paOthers.get(lnCtr).getEntryNo();
            } else {
                if(lnPrevEntryNo != paOthers.get(lnCtr).getEntryNo()){
                    lnRow = 1;
                    lnPrevEntryNo = paOthers.get(lnCtr).getEntryNo();
                } else {
                    lnRow++;
                }
            }
            
            if (lnCtr != row ) {
                lnSerialRow = lnCtr;
            } else {
                lnSerialRow = row;
            }
            
            if (value.equals(paOthers.get(lnSerialRow).getSerial01())){
                if(lnSerialRow == row){
                    if(!"serial01".equals(columnName)) {
                        poJSON.put("result", "error");
                    }
                } else {
                    poJSON.put("result", "error");
                }
            }
            if (value.equals(paOthers.get(lnSerialRow).getSerial02()) ){
                if(lnSerialRow == row){
                    if(!"serial02".equals(columnName)) {
                        poJSON.put("result", "error");
                    }
                } else {
                    poJSON.put("result", "error");
                }
            }
            if("03".equals(Master().getIndustryId())){
                if (value.equals(paOthers.get(lnSerialRow).getPlateNo()) ){
                    if(lnSerialRow == row){
                        if(!"plateno".equals(columnName)) {
                            poJSON.put("result", "error");
                        }
                    } else {
                        poJSON.put("result", "error");
                    }
                }
                if (value.equals(paOthers.get(lnSerialRow).getConductionStickerNo()) ){
                    if(lnSerialRow == row){
                        if(!"csno".equals(columnName)) {
                            poJSON.put("result", "error");
                        }
                    } else {
                        poJSON.put("result", "error");
                    }
                }
            }
            
            if ("error".equals((String) poJSON.get("result"))){
                poJSON.put("message", lsColName + " already exists for Entry No " + paOthers.get(lnCtr).getEntryNo() + "  at row " + lnRow + ".");
                poJSON.put("set", false);
                return poJSON;
            }
            
//            try {
//                if(paOthers.get(row).getSerialId() == null || "".equals(paOthers.get(lnSerialRow).getSerialId())){
//                    JSONObject loJSON = checkExistingSerialinDB(value, columnName, paOthers.get(row).getStockId());
//                    if("success".equals((String) loJSON.get("result"))){
//                        if( ShowMessageFX.YesNo(null, "Purchase Order Receiving Serial", lsColName + " already exists in database, do you want to set serial ID including serial information? ") == true){
//                            paOthers.get(row).setSerialId((String) loJSON.get("sSerialID"));
//                            paOthers.get(row).setSerial01((String) loJSON.get("sSerial01"));
//                            paOthers.get(row).setSerial02((String) loJSON.get("sSerial02"));
//                            paOthers.get(row).setConductionStickerNo((String) loJSON.get("sCStckrNo"));
//                            paOthers.get(row).setPlateNo((String) loJSON.get("sPlateNoP"));
//                            poJSON.put("set", true);
//                            return poJSON;
//                        }
//                    }
//                }
//            } catch (SQLException | GuanzonException ex) {
//                Logger.getLogger(PurchaseOrderReceiving.class.getName()).log(Level.SEVERE, null, ex);
//            }
        }
        
        poJSON.put("set", false);
        return poJSON;
    }
    
    private JSONObject checkExistingSerialinDB(String value, String columnName, String stockId)
            throws SQLException,
            GuanzonException {
        JSONObject loJSON = new JSONObject();
        String lsSQL =    " SELECT "                                                          
                        + " a.sSerialID   "                                                   
                        + " , a.sSerial01 "                                                   
                        + " , a.sSerial02 "                                                  
                        + " , a.sStockIDx "                                                    
                        + " , b.sCStckrNo "                                                   
                        + " , b.sPlateNoP "                                                   
                        + " FROM inv_serial a  "                                              
                        + " LEFT JOIN inv_serial_registration b ON b.sSerialID = a.sSerialID ";
        
        switch(columnName){
            case "serial01":
                lsSQL = MiscUtil.addCondition(lsSQL, " a.sStockIDx = " + SQLUtil.toSQL(stockId)
                                                        + " AND a.sSerial01 = " + SQLUtil.toSQL(value));
            break;
            case "serial02":
                lsSQL = MiscUtil.addCondition(lsSQL, " a.sStockIDx = " + SQLUtil.toSQL(stockId)
                                                        + " AND a.sSerial02 = " + SQLUtil.toSQL(value));
            break;
            case "csno":
                lsSQL = MiscUtil.addCondition(lsSQL, " a.sStockIDx = " + SQLUtil.toSQL(stockId)
                                                        + " AND b.sCStckrNo = " + SQLUtil.toSQL(value));
            break;
            case "plateno":
                lsSQL = MiscUtil.addCondition(lsSQL, " a.sStockIDx = " + SQLUtil.toSQL(stockId)
                                                        + " AND b.sPlateNoP = " + SQLUtil.toSQL(value));
            break;
        }
        
        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        try {
            if (MiscUtil.RecordCount(loRS) > 0) {
                if(loRS.next()){
                    loJSON.put("result", "success");
                    loJSON.put("sSerialID", loRS.getString("sSerialID"));
                    loJSON.put("sSerial01", loRS.getString("sSerial01"));
                    loJSON.put("sSerial02", loRS.getString("sSerial02"));
                    loJSON.put("sCStckrNo", loRS.getString("sCStckrNo"));
                    loJSON.put("sPlateNoP", loRS.getString("sPlateNoP"));
                }
            }
            MiscUtil.close(loRS);
        } catch (SQLException e) {
            System.out.println("No record loaded.");
            loJSON.put("result", "error");
        }
        
        return loJSON;
    }
    
    private JSONObject checkExistingSerialinDB(int row, String columnName)
            throws SQLException,
            GuanzonException {
        JSONObject loJSON = new JSONObject();
        String lsSQL =    " SELECT "                                                          
                        + " a.sSerialID   "                                                   
                        + " , a.sSerial01 "                                                   
                        + " , a.sSerial02 "                                                  
                        + " , a.sStockIDx "                                                    
                        + " , b.sCStckrNo "                                                   
                        + " , b.sPlateNoP "                                                  
                        + " , a.sIndstCdx "                                                  
                        + " , c.sIndstCdx "                                                   
                        + " FROM inv_serial a  "                                              
                        + " LEFT JOIN inv_serial_registration b ON b.sSerialID = a.sSerialID "
                        + " LEFT JOIN inventory c ON c.sStockIDx = a.sStockIDx ";
        
//        lsSQL = MiscUtil.addCondition(lsSQL, " a.sStockIDx = " + SQLUtil.toSQL(paOthers.get(row).getStockId())
//                                                + " AND a.sSerialID <> " +  SQLUtil.toSQL(paOthers.get(row).getSerialId()) 
        lsSQL = MiscUtil.addCondition(lsSQL, " a.sSerialID <> " +  SQLUtil.toSQL(paOthers.get(row).getSerialId())
                                            + " AND c.sIndstCdx = " + SQLUtil.toSQL(Master().getIndustryId()) //TODO
                                                
        );
        
//        lsSQL = lsSQL + " AND ( a.sSerial01 = " + SQLUtil.toSQL(paOthers.get(row).getSerial01())
//                    + " OR a.sSerial02 = " + SQLUtil.toSQL(paOthers.get(row).getSerial02());
//        
//        if(paOthers.get(row).getConductionStickerNo() != null && !"".equals(paOthers.get(row).getConductionStickerNo())){
//            lsSQL = lsSQL + " OR b.sCStckrNo = " + SQLUtil.toSQL(paOthers.get(row).getConductionStickerNo()) ;
//        }
//        
//        if(paOthers.get(row).getPlateNo() != null && !"".equals(paOthers.get(row).getPlateNo())){
//            lsSQL = lsSQL + " OR b.sPlateNoP = " + SQLUtil.toSQL(paOthers.get(row).getPlateNo()) ;
//        }
//        
//        lsSQL = lsSQL + " ) ";
        
        switch(columnName){
            case "serial01":
                lsSQL = lsSQL + " AND ( a.sSerial01 = " + SQLUtil.toSQL(paOthers.get(row).getSerial01())
                              +  " OR a.sSerial02 = " + SQLUtil.toSQL(paOthers.get(row).getSerial01())
                              + " ) " ;
            break;
            case "serial02":
                lsSQL = lsSQL + " AND ( a.sSerial02 = " + SQLUtil.toSQL(paOthers.get(row).getSerial02())
                              +  " OR a.sSerial01 = " + SQLUtil.toSQL(paOthers.get(row).getSerial02())
                              + " ) " ;
            break;
            case "csno":
                lsSQL = lsSQL + " AND ( b.sCStckrNo = " + SQLUtil.toSQL(paOthers.get(row).getConductionStickerNo())
                              +  " OR b.sPlateNoP = " + SQLUtil.toSQL(paOthers.get(row).getConductionStickerNo())
                              + " ) " ;
            break;
            case "plateno":
                lsSQL = lsSQL + " AND ( b.sPlateNoP = " + SQLUtil.toSQL(paOthers.get(row).getPlateNo())
                              +  " OR b.sCStckrNo = " + SQLUtil.toSQL(paOthers.get(row).getPlateNo())
                              + " ) " ;
            break;
        }
        
        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        try {
            if (MiscUtil.RecordCount(loRS) > 0) {
                if(loRS.next()){
                    loJSON.put("result", "error");
                    loJSON.put("sSerialID", loRS.getString("sSerialID"));
                    loJSON.put("sSerial01", loRS.getString("sSerial01"));
                    loJSON.put("sSerial02", loRS.getString("sSerial02"));
                    loJSON.put("sCStckrNo", loRS.getString("sCStckrNo"));
                    loJSON.put("sPlateNoP", loRS.getString("sPlateNoP"));
                }
            }
            MiscUtil.close(loRS);
        } catch (SQLException e) {
            System.out.println("No record loaded.");
            loJSON.put("result", "success");
        }
        
        return loJSON;
    }

    public JSONObject checkExistingSerialId(int entryNo) {
        poJSON = new JSONObject();

        //check when serial id already exists do not allow to change brand / model
        for (int lnCtr = 0; lnCtr <= getPurchaseOrderReceivingSerialCount() - 1; lnCtr++) {
            if (paOthers.get(lnCtr).getEntryNo() == entryNo) {
                if (paOthers.get(lnCtr).getSerialId() != null && !"".equals(paOthers.get(lnCtr).getSerialId())) {
                    poJSON.put("result", "error");
                    poJSON.put("message", "Serial ID already exist. Changing of brand / model is not allowed.");
                    return poJSON;
                }
            }
        }
        return poJSON;
    }

    //Use when changing of brand / model : remove all existing serials
    public void removePurchaseOrderReceivingSerial(int entryNo) {
        int lnEntryNo = 0;
        boolean lbRemoved = false;

        Iterator<Model_POR_Serial> detail = PurchaseOrderReceivingSerialList().iterator();
        while (detail.hasNext()) {
            Model_POR_Serial item = detail.next();
            if (item.getEntryNo() == entryNo) {
                System.out.println("remove por serial getEntryNo " + item.getEntryNo());
                detail.remove();
                lbRemoved = true;
            }
        }
        
        for(int lnCtr = 0; lnCtr <= getPurchaseOrderReceivingSerialCount()-1; lnCtr++){
            if(lbRemoved){
                if(PurchaseOrderReceivingSerialList(lnCtr).getEntryNo() > entryNo){
                    lnEntryNo = PurchaseOrderReceivingSerialList(lnCtr).getEntryNo() - entryNo;
                    if (lnEntryNo == 1) {
                        PurchaseOrderReceivingSerialList(lnCtr).setEntryNo(entryNo);
                    }
                    if (lnEntryNo > 1) {
                        PurchaseOrderReceivingSerialList(lnCtr).setEntryNo(entryNo + (lnEntryNo - 1));
                    }
                }
            }
        }
    }

    //Use for changing of quantity in por detail
    public JSONObject checkPurchaseOrderReceivingSerial(int entryNo, int quantity) {
        poJSON = new JSONObject();
        double lnQty = Detail(entryNo - 1).getQuantity().doubleValue();
        int lnEntryNo = 0;
        boolean lbChecked = false;

        if (getPurchaseOrderReceivingSerialCount() <= quantity) {
            return poJSON;
        }

        while (lnQty > quantity) {
            lbChecked = false;

            //1. Priority to remove the empty fields
            Iterator<Model_POR_Serial> detail = PurchaseOrderReceivingSerialList().iterator();
            while (detail.hasNext()) {
                Model_POR_Serial item = detail.next();
                if (item.getEntryNo() == entryNo) {
                    if (item.getSerialId() == null || "".equals(item.getSerialId())) {
                        if ((item.getSerial01() == null || "".equals(item.getSerial01()))
                                && (item.getSerial02() == null || "".equals(item.getSerial02()))) {
                            detail.remove();
                            lnQty--;
                            lbChecked = true;
                            break;
                        }
                    }
                }
            }

            if (lbChecked) {
                continue;
            }

            //2. Remove serials with value
            detail = PurchaseOrderReceivingSerialList().iterator();
            while (detail.hasNext()) {
                Model_POR_Serial item = detail.next();
                if (item.getEntryNo() == entryNo) {
                    if (item.getSerialId() == null || "".equals(item.getSerialId())) {
                        if ((item.getSerial01() != null || !"".equals(item.getSerial01()))
                                || (item.getSerial02() != null || !"".equals(item.getSerial02()))) {
                            detail.remove();
                            lnQty--;
                            lbChecked = true;
                            break;
                        }
                    }
                }
            }

            if (lbChecked) {
                continue;
            }

            //3. Check for serial Id: Do not allow to remove if exist
            detail = PurchaseOrderReceivingSerialList().iterator();
            while (detail.hasNext()) {
                Model_POR_Serial item = detail.next();
                if (item.getEntryNo() == entryNo) {
                    if (item.getSerialId() != null && !"".equals(item.getSerialId())) {
                        if(item.getEditMode() == EditMode.ADDNEW){
                            detail.remove();
                            lnQty--;
                            lbChecked = true;
                            break;
                        } else {
                            poJSON.put("result", "error");
                            poJSON.put("message", "Serial ID already exist, cannot be deleted.");
                            return poJSON;
                        }
                    }
                }
            }
            
            if (lbChecked) {
                continue;
            }

            //4. Update por serial entry no
//            detail = PurchaseOrderReceivingSerialList().iterator();
//            while (detail.hasNext()) {
//                Model_POR_Serial item = detail.next(); 
//                lnEntryNo = item.getEntryNo() - entryNo;
//                if (lnEntryNo == 1) {
//                    item.setEntryNo(entryNo);
//                } 
//                if (lnEntryNo > 1) {
//                    item.setEntryNo(entryNo+(lnEntryNo-1));
//                } 
//            }
            if (getPurchaseOrderReceivingSerialCount() <= 0) {
                break;
            }
        }
        return poJSON;
    }
    
    private Double pdblAdvPayment = 0.0000;
    private Double pdblTotalDiscAmt = 0.0000;
    private JSONObject populateCachePayable() throws SQLException, GuanzonException, CloneNotSupportedException{
        pdblAdvPayment = getAdvancePayment();
        pdblTotalDiscAmt = Master().getDiscount().doubleValue() + (Master().getTransactionTotal().doubleValue() * (Master().getDiscountRate().doubleValue() / 100));
        poJSON = new JSONObject();
        poCachePayable = new CashflowControllers(poGRider, logwrapr).CachePayable();
        poCachePayable.InitTransaction();
        poJSON = poCachePayable.NewTransaction();
        if ("error".equals((String) poJSON.get("result"))){
            return poJSON;
        }
        
        Double ldblGrossAmt = 0.0000;
        Double ldblTotal = 0.0000;
        Double ldblDetTotal = 0.0000;
        Double ldblVatAmountDetail = 0.0000;
        boolean lbExist = false;
        int lnCacheRow = 0;
        
        //Populate Cache Payable detail
        for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
            ldblDetTotal = (Detail(lnCtr).getUnitPrce().doubleValue() * Detail(lnCtr).getQuantity().doubleValue());
            ldblVatAmountDetail = ldblDetTotal - (ldblDetTotal / 1.12);
            ldblGrossAmt += ldblDetTotal;
            
            //If vat exclusive detail total + vat amount
            if(!Master().isVatTaxable()){
                if(Detail(lnCtr).isVatable()){
                    ldblDetTotal = ldblDetTotal + ldblVatAmountDetail;
                }
            }
            
            ldblTotal += ldblDetTotal;
            
            //Check existing transaction type
            if(lnCtr > 0) {
                for(lnCacheRow = 0; lnCacheRow <= poCachePayable.getDetailCount()-1; lnCacheRow++){
                    if(poCachePayable.Detail(lnCacheRow).getTransactionType().equals(Detail(lnCtr).Inventory().getInventoryTypeId())){
                        ldblTotal = poCachePayable.Detail(lnCacheRow).getGrossAmount() + ldblDetTotal;
                        lbExist = true;
                        break;
                    }
                }
                
                if(!lbExist){
                    poCachePayable.AddDetail();
                    lnCacheRow = poCachePayable.getDetailCount()-1;
                }
            }
            
            //Cache Payable Detail
            if(poCachePayable.getDetailCount() < 0){
                poCachePayable.AddDetail();
                lnCacheRow = 0;
            }
            
            poCachePayable.Detail(lnCacheRow).setTransactionType(Detail(lnCtr).Inventory().getInventoryTypeId());
            poCachePayable.Detail(lnCacheRow).setGrossAmount(ldblTotal); //TODO
            poCachePayable.Detail(lnCacheRow).setPayables(ldblTotal); //TODO
        }
        
        //Update cache payable
        for(int lnCtr = 0; lnCtr <= poCachePayable.getDetailCount()-1; lnCtr++){
            //Update AmountPaid
            if(pdblAdvPayment >= poCachePayable.Detail(lnCtr).getPayables()){
                poCachePayable.Detail(lnCtr).setAmountPaid(poCachePayable.Detail(lnCtr).getPayables());
                pdblAdvPayment = pdblAdvPayment - poCachePayable.Detail(lnCtr).getPayables();
            } else {
                if( pdblAdvPayment > 0.0000){
                    poCachePayable.Detail(lnCtr).setAmountPaid(pdblAdvPayment);
                    pdblAdvPayment = 0.0000;
                }
            }
            
            //Update Discount
            if(pdblTotalDiscAmt >= poCachePayable.Detail(lnCtr).getPayables()){
                poCachePayable.Detail(lnCtr).setDiscountAmount(poCachePayable.Detail(lnCtr).getPayables());
                pdblTotalDiscAmt = pdblTotalDiscAmt - poCachePayable.Detail(lnCtr).getPayables();
            } else {
                if( pdblTotalDiscAmt > 0.0000){
                    poCachePayable.Detail(lnCtr).setDiscountAmount(pdblTotalDiscAmt);
                    pdblTotalDiscAmt = 0.0000;
                }
            }
        }
       
        //Cache Payable Master
        Double ldblTotalDiscAmt =  Master().getDiscount().doubleValue() + (ldblGrossAmt * (Master().getDiscountRate().doubleValue() / 100));
        poCachePayable.Master().setIndustryCode(Master().getIndustryId());
        poCachePayable.Master().setBranchCode(Master().getBranchCode());
        poCachePayable.Master().setTransactionDate(poGRider.getServerDate()); 
        poCachePayable.Master().setCompanyId(Master().getCompanyId());
        poCachePayable.Master().setClientId(Master().getSupplierId());
        poCachePayable.Master().setDueDate(Master().getDueDate());
        poCachePayable.Master().setSourceCode(getSourceCode());
        poCachePayable.Master().setSourceNo(Master().getTransactionNo());
        poCachePayable.Master().setReferNo(Master().getReferenceNo()); 
        poCachePayable.Master().setGrossAmount(ldblGrossAmt); 
        poCachePayable.Master().setDiscountAmount(ldblTotalDiscAmt); 
        poCachePayable.Master().setVATSales(Master().getVatSales().doubleValue());
        poCachePayable.Master().setVATAmount(Master().getVatAmount().doubleValue());
        poCachePayable.Master().setVATExempt(Master().getVatExemptSales().doubleValue());
        poCachePayable.Master().setZeroRated(Master().getZeroVatSales().doubleValue());
        poCachePayable.Master().setTaxAmount(Master().getWithHoldingTax().doubleValue());
        poCachePayable.Master().setAmountPaid(getAdvancePayment());
        poCachePayable.Master().setTransactionStatus(CachePayableStatus.CONFIRMED); //set to 1
        poCachePayable.Master().setModifyingId(poGRider.Encrypt(poGRider.getUserID()));
        poCachePayable.Master().setModifiedDate(poGRider.getServerDate());
        
        if(Master().getTruckingId() == null || "".equals(Master().getTruckingId())){
            poCachePayable.Master().setFreight(Master().getFreight().doubleValue());
            poCachePayable.Master().setNetTotal(getNetTotal()); 
            poCachePayable.Master().setPayables(getNetTotal()); 
        } else {
            poCachePayable.Master().setNetTotal(getNetTotal() - Master().getFreight().doubleValue()); 
            poCachePayable.Master().setPayables(getNetTotal() - Master().getFreight().doubleValue()); 
        }
        
        Master().setAmountPaid(getAdvancePayment());
        
        return poJSON;
    }
    
    private JSONObject populateCachePayableFreight() throws SQLException, GuanzonException, CloneNotSupportedException{
        poJSON = new JSONObject();
        poCachePayableTrucking = new CashflowControllers(poGRider, logwrapr).CachePayable();
        poCachePayableTrucking.InitTransaction();
        poJSON = poCachePayableTrucking.NewTransaction();
        if ("error".equals((String) poJSON.get("result"))){
            return poJSON;
        }
        //Populate Cache Payable detail
        if(poCachePayableTrucking.getDetailCount() < 0){
            poCachePayableTrucking.AddDetail();
        }

        int lnRow = poCachePayableTrucking.getDetailCount() - 1;
        poCachePayableTrucking.Detail(lnRow).setTransactionType(getInvTypeCode("freight")); //TODO
        poCachePayableTrucking.Detail(lnRow).setGrossAmount(Master().getFreight());
        poCachePayableTrucking.Detail(lnRow).setPayables(Master().getFreight()); 
        
        //get the excess advance payment and total discount amount
//        Double ldblTotalDiscAmt =  pdblTotalDiscAmt;
//        Double ldblAdvPayment =  pdblAdvPayment;
        
//        //Update AmountPaid
//        if(pdblAdvPayment >= poCachePayableTrucking.Detail(lnRow).getPayables()){
//            poCachePayableTrucking.Detail(lnRow).setAmountPaid(poCachePayableTrucking.Detail(lnRow).getPayables());
//            pdblAdvPayment = pdblAdvPayment - poCachePayableTrucking.Detail(lnRow).getPayables();
//        } else {
//            if( pdblAdvPayment > 0.0000){
//                poCachePayableTrucking.Detail(lnRow).setAmountPaid(pdblAdvPayment);
//                pdblAdvPayment = 0.0000;
//            }
//        }
//
//        //Update Discount
//        if(pdblTotalDiscAmt >= poCachePayableTrucking.Detail(lnRow).getPayables()){
//            poCachePayableTrucking.Detail(lnRow).setDiscountAmount(poCachePayableTrucking.Detail(lnRow).getPayables());
//            pdblTotalDiscAmt = pdblTotalDiscAmt - poCachePayableTrucking.Detail(lnRow).getPayables();
//        } else {
//            if( pdblTotalDiscAmt > 0.0000){
//                poCachePayableTrucking.Detail(lnRow).setDiscountAmount(pdblTotalDiscAmt);
//                pdblTotalDiscAmt = 0.0000;
//            }
//        }
       
        //Cache Payable Trucking Master
        poCachePayableTrucking.Master().setIndustryCode(Master().getIndustryId());
        poCachePayableTrucking.Master().setBranchCode(Master().getBranchCode());
        poCachePayableTrucking.Master().setTransactionDate(poGRider.getServerDate()); 
        poCachePayableTrucking.Master().setCompanyId(Master().getCompanyId());
        poCachePayableTrucking.Master().setClientId(Master().getTruckingId());
        poCachePayableTrucking.Master().setDueDate(Master().getDueDate());
        poCachePayableTrucking.Master().setSourceCode(getSourceCode());
        poCachePayableTrucking.Master().setSourceNo(Master().getTransactionNo());
        poCachePayableTrucking.Master().setReferNo(Master().getReferenceNo()); 
        poCachePayableTrucking.Master().setGrossAmount(Master().getFreight().doubleValue()); 
//        poCachePayableTrucking.Master().setFreight(Master().getFreight().doubleValue()); //Do not set the freight since detail has already a freight
        poCachePayableTrucking.Master().setNetTotal(Master().getFreight().doubleValue()); 
        poCachePayableTrucking.Master().setPayables(Master().getFreight().doubleValue()); 
//        poCachePayableTrucking.Master().setDiscountAmount(ldblTotalDiscAmt); 
//        poCachePayableTrucking.Master().setAmountPaid(ldblAdvPayment);
        poCachePayableTrucking.Master().setTransactionStatus(CachePayableStatus.CONFIRMED); //set to 1
        poCachePayableTrucking.Master().setModifyingId(poGRider.Encrypt(poGRider.getUserID()));
        poCachePayableTrucking.Master().setModifiedDate(poGRider.getServerDate());
        
        return poJSON;
    }
    
    private String getInvTypeCode(String fsValue){
        try {
            String lsSQL = "SELECT sInvTypCd, sDescript FROM inv_type ";
            lsSQL = MiscUtil.addCondition(lsSQL, " cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE)
                                                + " AND lower(sDescript) LIKE " + SQLUtil.toSQL("%"+fsValue));
            System.out.println("Executing SQL: " + lsSQL);
            ResultSet loRS = poGRider.executeQuery(lsSQL);
            try {
                if (MiscUtil.RecordCount(loRS) > 0) {
                    if(loRS.next()){
                        return  loRS.getString("sInvTypCd");
                    }
                }
                MiscUtil.close(loRS);
            } catch (SQLException e) {
                System.out.println("No record loaded.");
            }
        } catch (SQLException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
        }
            
        return  "";
    }
    
    public Journal Journal(){
        try {
            if(poJournal == null){
                poJournal = new CashflowControllers(poGRider, logwrapr).Journal();
                poJournal.InitTransaction();
            }
            
        } catch (SQLException ex) {
            Logger.getLogger(PurchaseOrderReceiving.class.getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
        } catch (GuanzonException ex) {
            Logger.getLogger(PurchaseOrderReceiving.class.getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
        }
        return poJournal;
    }
    
    public JSONObject populateJournal() throws SQLException, GuanzonException, CloneNotSupportedException, ScriptException{
        poJSON = new JSONObject();
        if(getEditMode() == EditMode.UNKNOWN || Master().getEditMode() == EditMode.UNKNOWN){
            poJSON.put("result", "error");
            poJSON.put("message", "No record to load");
            return poJSON;
        }
        
        if(poJournal == null || getEditMode() == EditMode.READY){
            poJournal = new CashflowControllers(poGRider, logwrapr).Journal();
            poJournal.InitTransaction();
        }
        
        String lsJournal = existJournal();
        if(lsJournal != null && !"".equals(lsJournal)){
            if(getEditMode() == EditMode.READY){
                poJSON = poJournal.OpenTransaction(lsJournal);
                if ("error".equals((String) poJSON.get("result"))){
                    return poJSON;
                }
            }
            
            if(getEditMode() == EditMode.UPDATE){
                if(poJournal.getEditMode() == EditMode.READY || poJournal.getEditMode() == EditMode.UNKNOWN){
                    poJSON = poJournal.OpenTransaction(lsJournal);
                    if ("error".equals((String) poJSON.get("result"))){
                        return poJSON;
                    }
                    poJournal.UpdateTransaction();
                } 
            }
        } else {
            if(getEditMode() == EditMode.UPDATE && poJournal.getEditMode() != EditMode.ADDNEW){
                poJSON = poJournal.NewTransaction();
                if ("error".equals((String) poJSON.get("result"))){
                    return poJSON;
                }

//                double ldblNetTotal = 0.0000;
//                double ldblDiscount = Master().getDiscount().doubleValue();
//                double ldblDiscountRate = Master().getDiscountRate().doubleValue();
//                if(ldblDiscountRate > 0){
//                    ldblDiscountRate = Master().getTransactionTotal().doubleValue() * (ldblDiscountRate / 100);
//                }
//                ldblDiscount = ldblDiscount + ldblDiscountRate;
//                //Net Total = Vat Amount - Tax Amount
//                if (Master().isVatTaxable()) {
//                    //Net VAT Amount : VAT Sales - VAT Amount
//                    //Net Total : VAT Sales - Withholding Tax
//                    ldblNetTotal = Master().getVatSales().doubleValue() - Master().getWithHoldingTax().doubleValue();
//                } else {
//                    //Net VAT Amount : VAT Sales + VAT Amount
//                    //Net Total : Net VAT Amount - Withholding Tax
//                    ldblNetTotal = (Master().getVatSales().doubleValue()
//                            + Master().getVatAmount().doubleValue())
//                            - Master().getWithHoldingTax().doubleValue();
//
//                }
                
                System.out.println("MASTER");
                //retreiving using column index
                JSONObject jsonmaster = new JSONObject();
                for (int lnCtr = 1; lnCtr <= Master().getColumnCount(); lnCtr++){
                    System.out.println(Master().getColumn(lnCtr) + " ->> " + Master().getValue(lnCtr));
                    jsonmaster.put(Master().getColumn(lnCtr),  Master().getValue(lnCtr));
                }
                
                JSONArray jsondetails = new JSONArray();
                JSONObject jsondetail = new JSONObject();
                
                System.out.println("DETAIL");
                for (int lnCtr = 0; lnCtr <= Detail().size() - 1; lnCtr++){
                    jsondetail = new JSONObject();
                    System.out.println("DETAIL ROW : " + lnCtr);
                    for (int lnCol = 1; lnCol <= Detail(lnCtr).getColumnCount(); lnCol++){
                        System.out.println(Detail(lnCtr).getColumn(lnCol) + " ->> " + Detail(lnCtr).getValue(lnCol));
                        jsondetail.put(Detail(lnCtr).getColumn(lnCol),  Detail(lnCtr).getValue(lnCol));
                    }
                    jsondetails.add(jsondetail);
                }

                jsondetail = new JSONObject();
                jsondetail.put("PO_Receiving_Master", jsonmaster);
                jsondetail.put("PO_Receiving_Detail", jsondetails);

                TBJTransaction tbj = new TBJTransaction(SOURCE_CODE,Master().getIndustryId(), Master().getCategoryCode());
                tbj.setGRiderCAS(poGRider);
                tbj.setData(jsondetail);
                jsonmaster = tbj.processRequest();

                if(jsonmaster.get("result").toString().equalsIgnoreCase("success")){
                    List<TBJEntry> xlist = tbj.getJournalEntries();
                    for (TBJEntry xlist1 : xlist) {
                        System.out.println("Account:" + xlist1.getAccount() );
                        System.out.println("Debit:" + xlist1.getDebit());
                        System.out.println("Credit:" + xlist1.getCredit());
                        poJournal.Detail(poJournal.getDetailCount()-1).setForMonthOf(poGRider.getServerDate());
                        poJournal.Detail(poJournal.getDetailCount()-1).setAccountCode(xlist1.getAccount());
                        poJournal.Detail(poJournal.getDetailCount()-1).setCreditAmount(xlist1.getCredit());
                        poJournal.Detail(poJournal.getDetailCount()-1).setDebitAmount(xlist1.getDebit());
                        poJournal.AddDetail();
                    }
                } else {
                    System.out.println(jsonmaster.toJSONString());
                }

                //Journa Entry Master
                poJournal.Master().setAccountPerId("");
                poJournal.Master().setIndustryCode(Master().getIndustryId());
                poJournal.Master().setBranchCode(Master().getBranchCode());
                poJournal.Master().setDepartmentId(Master().getDepartmentId());
                poJournal.Master().setTransactionDate(poGRider.getServerDate()); 
                poJournal.Master().setCompanyId(Master().getCompanyId());
                poJournal.Master().setSourceCode(getSourceCode());
                poJournal.Master().setSourceNo(Master().getTransactionNo());
            } else if(getEditMode() == EditMode.UPDATE && poJournal.getEditMode() == EditMode.ADDNEW) {
                poJSON.put("result", "success");
                return poJSON;
            } else {
                poJSON.put("result", "error");
                poJSON.put("message", "No record to load");
                return poJSON;
            }
        
        }
        
        poJSON.put("result", "success");
        return poJSON;
    }
    
    public JSONObject checkExistAcctCode(int fnRow, String fsAcctCode){
        poJSON = new JSONObject();
        
        for(int lnCtr = 0;lnCtr <= poJournal.getDetailCount()-1; lnCtr++){
            if(fsAcctCode.equals(poJournal.Detail(lnCtr).getAccountCode()) && fnRow != lnCtr){
                poJSON.put("row", lnCtr);
                poJSON.put("result", "error");
                poJSON.put("message", "Account code " + fsAcctCode + " already exists at row " + (lnCtr+1) + ".");
                poJournal.Detail(fnRow).setAccountCode("");
                return poJSON;
            }
        }
    
        poJSON.put("result", "success");
        return poJSON;
    }
    
    private String existJournal() throws SQLException{
        Model_Journal_Master loMaster = new CashflowModels(poGRider).Journal_Master();
        String lsSQL = MiscUtil.makeSelect(loMaster);
        lsSQL = MiscUtil.addCondition(lsSQL,
                " sSourceNo = " + SQLUtil.toSQL(Master().getTransactionNo())
        );
        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        poJSON = new JSONObject();
        if (MiscUtil.RecordCount(loRS) > 0) {
            while (loRS.next()) {
                // Print the result set
                System.out.println("--------------------------JOURNAL ENTRY--------------------------");
                System.out.println("sTransNox: " + loRS.getString("sTransNox"));
                System.out.println("------------------------------------------------------------------------------");
                if(loRS.getString("sTransNox") != null && !"".equals(loRS.getString("sTransNox"))){
                    return loRS.getString("sTransNox");
                }  
            }
        }
        MiscUtil.close(loRS);

        return "";
    }
     
    @Override
    public String getSourceCode() {
        return SOURCE_CODE;
    }

    @Override
    public Model_POR_Master Master() {
        return (Model_POR_Master) poMaster;
    }
    
    public Model_POR_Detail getDetail() {
        return (Model_POR_Detail) poDetail;
    }
    
    public Model_POR_Serial getSerial() {
        return (Model_POR_Serial) poSerial;
    }

    private Model_POR_Serial PurchaseOrderReceivingSerial() {
        return new PurchaseOrderReceivingModels(poGRider).PurchaseOrderReceivingSerial();
    }
    
    private Model_PO_Master PurchaseOrderMaster() {
        return new PurchaseOrderModels(poGRider).PurchaseOrderMaster();
    }
    
    private Model_POReturn_Master PurchaseOrderReturnMaster() {
        return new PurchaseOrderReturnModels(poGRider).PurchaseOrderReturnMaster();
    }
    
    public List<Model_POR_Serial> PurchaseOrderReceivingSerialList() {
        return paOthers;
    }
    
    private Model_POR_Master PurchaseOrderReceivingMaster() {
        return new PurchaseOrderReceivingModels(poGRider).PurchaseOrderReceivingMaster();
    }

    private TransactionAttachment TransactionAttachment()
            throws SQLException,
            GuanzonException {
        return new SysTableContollers(poGRider, null).TransactionAttachment();
    }

    private List<TransactionAttachment> TransactionAttachmentList() {
        return paAttachments;
    }

    public TransactionAttachment TransactionAttachmentList(int row) {
        return (TransactionAttachment) paAttachments.get(row);
    }

    @Override
    public Model_POR_Detail Detail(int row) {
        return (Model_POR_Detail) paDetail.get(row);
    }
    
    public Model_POR_Detail DetailRemove(int row) {
        return (Model_POR_Detail) paDetailRemoved.get(row);
    }

    public Model_POR_Serial PurchaseOrderReceivingSerialList(int row) {
        return (Model_POR_Serial) paOthers.get(row);
    }
    
    public Model_PO_Master PurchaseOrderList(int row) {
        return (Model_PO_Master) paPOMaster.get(row);
    }
    
    public Model_POR_Master PurchaseOrderReceivingList(int row) {
        return (Model_POR_Master) paPORMaster.get(row);
    }
    
    public Model_POReturn_Master PurchaseOrderReturnList(int row) {
        return (Model_POReturn_Master) paPOReturnMaster.get(row);
    }
    
    public int getPurchaseOrderReceivingCount() {
        return this.paPORMaster.size();
    }
    
    @Override
    public int getDetailCount() {
        if (paDetail == null) {
            paDetail = new ArrayList<>();
        }

        return paDetail.size();
    }
    
    public int getDetailRemovedCount() {
        if (paDetailRemoved == null) {
            paDetailRemoved = new ArrayList<>();
        }

        return paDetailRemoved.size();
    }

    public int getPurchaseOrderReceivingSerialCount() {
        if (paOthers == null) {
            paOthers = new ArrayList<>();
        }

        return paOthers.size();
    }

    public int getTransactionAttachmentCount() {
        if (paAttachments == null) {
            paAttachments = new ArrayList<>();
        }

        return paAttachments.size();
    }

    public int getPurchaseOrderCount() {
        if (paPOMaster == null) {
            paPOMaster = new ArrayList<>();
        }

        return paPOMaster.size();
    }

    public int getPurchaseOrderReturnCount() {
        if (paPOReturnMaster == null) {
            paPOReturnMaster = new ArrayList<>();
        }

        return paPOReturnMaster.size();
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
    
    public JSONObject addPurchaseOrderReceivingSerial() {
        poJSON = new JSONObject();

        if (paOthers.isEmpty()) {
            paOthers.add(PurchaseOrderReceivingSerial());
        } else {
            if (!paOthers.get(paOthers.size() - 1).getTransactionNo().isEmpty()) {
                paOthers.add(PurchaseOrderReceivingSerial());
            } else {
                poJSON.put("result", "error");
                poJSON.put("message", "Unable to add purchase order receiving serial.");
                return poJSON;
            }
        }

        poJSON.put("result", "success");
        return poJSON;
    }
    
    public JSONObject addAttachment()
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();

        if (paAttachments.isEmpty()) {
            paAttachments.add(TransactionAttachment());
            poJSON = paAttachments.get(getTransactionAttachmentCount() - 1).newRecord();
        } else {
            if (!paAttachments.get(paAttachments.size() - 1).getModel().getTransactionNo().isEmpty()) {
                paAttachments.add(TransactionAttachment());
            } else {
                poJSON.put("result", "error");
                poJSON.put("message", "Unable to add transaction attachment.");
                return poJSON;
            }
        }

        poJSON.put("result", "success");
        return poJSON;

    }
    
    public void resetOthers() {
        paOthers = new ArrayList<>();
        paAttachments = new ArrayList<>();
    }

    public void resetMaster() {
        poMaster = new PurchaseOrderReceivingModels(poGRider).PurchaseOrderReceivingMaster();
    }
    
    public void resetJournal() {
        try {
            poJournal = new CashflowControllers(poGRider, logwrapr).Journal();
            poJournal.InitTransaction();
        } catch (SQLException ex) {
            Logger.getLogger(PurchaseOrderReceiving.class.getName()).log(Level.SEVERE, null, ex);
        } catch (GuanzonException ex) {
            Logger.getLogger(PurchaseOrderReceiving.class.getName()).log(Level.SEVERE, null, ex);
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

    public void isFinance(boolean isFinance) {
        pbIsFinance = isFinance;
    }

    public void setPurpose(String fsPurpose) {
        psPurpose = fsPurpose;
    }
    
    public double getQuantity(int row){
        if(Detail(row).getStockId() == null){
            return 0.00;
        }
        double lnQty = 0.00;
        for(int lnCtr = 0;lnCtr <= getDetailCount() - 1; lnCtr++){
            if(Detail(row).getStockId().equals(Detail(lnCtr).getStockId())){
                lnQty += Detail(row).getQuantity().doubleValue();
            }
        }
        
        return lnQty;
    }
    
    public JSONObject setQuantity(int row, double value){
        poJSON = new JSONObject();
        if(Detail(row).getStockId() == null || "".equals(Detail(row).getStockId())){
            poJSON.put("result", "error");
            poJSON.put("message", "Stock Id cannot be empty.");
            return poJSON;
        }
        try {
            double lnQuantity = 0.00;
            double lnQty = 0.00;
            int lnRow = -1;

            for(int lnCtr = 0;lnCtr <= getDetailCount() - 1; lnCtr++){
                if(Detail(row).getStockId().equals(Detail(lnCtr).getStockId())){
                    lnQty += Detail(row).getQuantity().doubleValue();
                    if(Detail(lnCtr).getEditMode() == EditMode.ADDNEW){
                        lnRow = lnCtr;
                    }
                }
            }
            
            if(lnQty != value){
                while(lnQty != value){
                    if(lnQty < value){
                        lnQty++;
                        lnQuantity++;
                    } else {
                        lnQty--;
                        lnQuantity--;
                    }
                }
                
                if(lnRow < 0 ){
                    Detail(row).isReverse(false);
                    AddDetail();
                    lnRow = getDetailCount() - 1;
                } 

                Detail(lnRow).setStockId(Detail(row).getStockId());
                Detail(lnRow).setReplaceId(Detail(row).getReplaceId());
                Detail(lnRow).setUnitType(Detail(row).getUnitType());
                Detail(lnRow).setUnitPrce(Detail(row).getUnitPrce());
                Detail(lnRow).setDiscountRate(Detail(row).getDiscountRate());
                Detail(lnRow).setDiscountAmount(Detail(row).getDiscountAmount());
                Detail(lnRow).setFreight(Detail(row).getFreight());
                Detail(lnRow).setExpiryDate(Detail(row).getExpiryDate());
                Detail(lnRow).setWhCount(Detail(row).getWhCount());
                Detail(lnRow).setOrderNo(Detail(row).getOrderNo());
                Detail(lnRow).setTotal(Detail(row).getTotal());
                Detail(lnRow).isVatable(Detail(row).isVatable());
                Detail(lnRow).setQuantity(lnQuantity);
                Detail(lnRow).isReverse(true);
            }

        } catch (CloneNotSupportedException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
        }
        return poJSON;
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
        String lsColumnName = "";
        
        if (paDetailRemoved == null) {
            paDetailRemoved = new ArrayList<>();
        }
        
        if(Master().getEditMode() == EditMode.ADDNEW){
            System.out.println("Will Save : " + Master().getNextCode());
            Master().setTransactionNo(Master().getNextCode());
        }

        Master().setModifyingId(poGRider.Encrypt(poGRider.getUserID()));
        Master().setModifiedDate(poGRider.getServerDate());
        
        if(pbIsFinance){
            //If trucking is not empty FREIGHT AMOUNT is required
            if (Master().getTruckingId()!= null && !"".equals(Master().getTruckingId())) {
                if (Master().getFreight().doubleValue() <= 0.00) {
                    poJSON.put("result", "error");
                    poJSON.put("message", "Invalid Freight Amount.");
                    return poJSON;
                }
            }
        }
        
        boolean lbHasQty = false;
        int lnEntryNo = 0;
        int lnNewEntryNo = 1;
        int lnPrevEntryNo = -1;
        boolean lbMatch = false;
        
        paOthers.sort(Comparator.comparingInt(item -> item.getEntryNo()));
        
        for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
            if(Detail(lnCtr).getQuantity().doubleValue() > 0){ 
                lnEntryNo = lnEntryNo + 1;
            }
            
            if(Detail(lnCtr).getQuantity().doubleValue() <= 0){ 
                lnNewEntryNo = lnEntryNo + 1;
                
                for(int lnRow = 0; lnRow <= paOthers.size()-1; lnRow++){
                    if(paOthers.get(lnRow).getEntryNo() == lnNewEntryNo){
                        lbMatch = true;
                        break;
                    }
                }
                
                if(!lbMatch){
                    for(int lnRow = 0; lnRow <= paOthers.size()-1; lnRow++){
                        lnPrevEntryNo = paOthers.get(lnRow).getEntryNo();

                        if(paOthers.get(lnRow).getEntryNo() > (lnCtr+1)){
                            paOthers.get(lnRow).setEntryNo(lnNewEntryNo);

                            if((lnRow+1) <= paOthers.size()-1){
                                if(lnPrevEntryNo <  paOthers.get(lnRow+1).getEntryNo()){
                                    lnNewEntryNo = lnNewEntryNo + 1;
                                }
                            }
                        }
                    }
                }
            }
            
            lbMatch = false;
            
            if (Detail(lnCtr).getQuantity().doubleValue() > 0) {
                lbHasQty = true;
            }
        }
        
        if(!lbHasQty){
            poJSON.put("result", "error");
            poJSON.put("message", "Your transaction cannot be zero quantity.");
            return poJSON;
        }
        
        Iterator<Model> detail = Detail().iterator();
        String lsQuantity = "0.00";
        while (detail.hasNext()) {
            Model item = detail.next();
            if(item.getValue("nQuantity") != null && !"".equals(item.getValue("nQuantity"))){
                lsQuantity = item.getValue("nQuantity").toString();
            }
            if ("".equals((String) item.getValue("sStockIDx"))
                    || Double.valueOf(lsQuantity) <= 0.00) {
                detail.remove();
                if (!"".equals((String) item.getValue("sOrderNox")) && (String) item.getValue("sOrderNox") != null) {
                    paDetailRemoved.add(item);
                }
                //TODO
//                if (item.getEditMode() == EditMode.ADDNEW) {
//                    detail.remove();
//                } else {
//                    if (!"".equals((String) item.getValue("sOrderNox")) && (String) item.getValue("sOrderNox") != null) {
//                        paDetailRemoved.add(item);
//                    }
//                    item.setValue("cReversex", PurchaseOrderReceivingStatus.Reverse.EXCLUDE);
//                }
            } else {
                item.setValue("cReversex", PurchaseOrderReceivingStatus.Reverse.INCLUDE); //TODO
            }
            lsQuantity = "0.00";
        }
        
        //Validate detail after removing all zero qty and empty stock Id
        if (getDetailCount() <= 0) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transaction detail to be save.");
            return poJSON;
        }

        if (getDetailCount() == 1) {
            //do not allow a single item detail with no quantity order
            if (Detail(0).getQuantity().doubleValue() == 0) {
                poJSON.put("result", "error");
                poJSON.put("message", "Your transaction has zero quantity.");
                return poJSON;
            }
        }
        
        //Validate transaction total
        double ldblTotal = Master().getTransactionTotal().doubleValue() - (Master().getDiscount().doubleValue() 
                         + ((Master().getDiscountRate().doubleValue() / 100.00) * Master().getTransactionTotal().doubleValue()));
        if(ldblTotal < 0.0000 ){
            poJSON.put("result", "error");
            poJSON.put("message", "Invalid transaction net total.");
            return poJSON;
        }

        if (getEditMode() == EditMode.UPDATE) {
            PurchaseOrderReceiving loRecord = new PurchaseOrderReceivingControllers(poGRider, null).PurchaseOrderReceiving();
            loRecord.InitTransaction();
            loRecord.OpenTransaction(Master().getTransactionNo());

            //Set original supplier Id
            if(!Master().getSupplierId().equals(loRecord.Master().getSupplierId())){
                Master().setSupplierId(loRecord.Master().getSupplierId());
                Master().setAddressId(loRecord.Master().getAddressId()); 
                Master().setContactId(loRecord.Master().getContactId()); 
            }
            //Set original branch code
            if(!Master().getBranchCode().equals(loRecord.Master().getBranchCode())){
                Master().setBranchCode(loRecord.Master().getBranchCode());
            }
            //Set original company id
            if(!Master().getCompanyId().equals(loRecord.Master().getCompanyId())){
                Master().setCompanyId(loRecord.Master().getCompanyId());
            }
            
            if(!pbIsPrint){
                if (PurchaseOrderReceivingStatus.CONFIRMED.equals(Master().getTransactionStatus())
                        && !pbIsFinance) {
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
                }

                if (PurchaseOrderReceivingStatus.RETURNED.equals(Master().getTransactionStatus())) {

                    lbUpdated = loRecord.getDetailCount() == getDetailCount();
                    if (lbUpdated) {
                        lbUpdated = loRecord.Master().getTransactionTotal().doubleValue() == Master().getTransactionTotal().doubleValue();
                    }
                    if (lbUpdated) {
                        lbUpdated = loRecord.Master().getReferenceNo().equals(Master().getReferenceNo());
                    }
                    if (lbUpdated) {
                        lbUpdated = loRecord.Master().getReferenceDate().equals(Master().getReferenceDate());
                    }
                    if (lbUpdated) {
                        lbUpdated = loRecord.Master().getTermCode().equals(Master().getTermCode());
                    }
                    if (lbUpdated) {
                        lbUpdated = loRecord.Master().getTruckingId().equals(Master().getTruckingId());
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

                            if (!lbUpdated) {
                                break;
                            }

                            loRecord.getPurchaseOrderReceivingSerial(lnCtr+1);

                        }
                    }

                    if (lbUpdated) {
                        lbUpdated = loRecord.getPurchaseOrderReceivingSerialCount() == getPurchaseOrderReceivingSerialCount();
                    }

                    if (lbUpdated) {
                        for (int lnCtr = 0; lnCtr <= loRecord.getPurchaseOrderReceivingSerialCount()- 1; lnCtr++) {
                            lbUpdated = loRecord.PurchaseOrderReceivingSerialList(lnCtr).getSerial01().equals(PurchaseOrderReceivingSerialList(lnCtr).getSerial01());
                            if (lbUpdated) {
                                lbUpdated = loRecord.PurchaseOrderReceivingSerialList(lnCtr).getSerial02().equals(PurchaseOrderReceivingSerialList(lnCtr).getSerial02());
                            }
                            if (lbUpdated) {
                                lbUpdated = loRecord.PurchaseOrderReceivingSerialList(lnCtr).getConductionStickerNo().equals(PurchaseOrderReceivingSerialList(lnCtr).getConductionStickerNo());
                            }
                            if (lbUpdated) {
                                lbUpdated = loRecord.PurchaseOrderReceivingSerialList(lnCtr).getPlateNo().equals(PurchaseOrderReceivingSerialList(lnCtr).getPlateNo());
                            }
                            if (lbUpdated) {
                                lbUpdated = loRecord.PurchaseOrderReceivingSerialList(lnCtr).getLocationId().equals(PurchaseOrderReceivingSerialList(lnCtr).getLocationId());
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
                    Master().setTransactionStatus(PurchaseOrderReceivingStatus.OPEN); //If edited update trasaction status into open
                
                }
            }
        }

        //assign other info on detail
        for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
            if(Detail(lnCtr).getOrderNo() != null && !"".equals(Detail(lnCtr).getOrderNo())){
                if(Detail(lnCtr).getOrderQty().doubleValue() < Detail(lnCtr).getQuantity().doubleValue()){
                    poJSON.put("result", "error");
                    poJSON.put("message", "Receive quantity cannot be greater than the order quantity for Order No. " + Detail(lnCtr).getOrderNo());
                    return poJSON;
                }
            }
            
            //Validate Existng Stock Id in POR Detail
            poJSON = checkExistingStock(Detail(lnCtr).getStockId(), Detail(lnCtr).Inventory().getDescription(), xsDateShort(Detail(lnCtr).getExpiryDate()), lnCtr, true);
            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }

            //Set value to por detail
            Detail(lnCtr).setTransactionNo(Master().getTransactionNo());
            Detail(lnCtr).setWhCount(Detail(lnCtr).getQuantity());
            Detail(lnCtr).setEntryNo(lnCtr + 1);
            Detail(lnCtr).setModifiedDate(poGRider.getServerDate());

            //POR Serial
            if(!pbIsFinance){ 
                String lsSerialId = "";
                //Validate POR Serial only in PO Receiving Form
                //Mobile Phone : 01 Motorcycle   : 02 Vehicle      : 03
                //Mobile Phone : 0001 Motorcycle   : 0010 Vehicle      : 0015
                if(Detail(lnCtr).isSerialized()){ 
                      //SPMC : 0004 SPCAR   : 0006 FOOD      : 0008 : GENERAL 0007
                      // &&  (!Master().getCategoryCode().equals("0004") && !Master().getCategoryCode().equals("0006") && !Master().getCategoryCode().equals("0008") && !Master().getCategoryCode().equals("0007"))){
                    //check serial list must be equal to por detail receive qty
                    for (int lnList = 0; lnList <= getPurchaseOrderReceivingSerialCount() - 1; lnList++) {
                        if (PurchaseOrderReceivingSerialList(lnList).getEntryNo() == Detail(lnCtr).getEntryNo()) {
                            //If there a value for serial 1 do not allow saving when serial 2 and location is empty 
                            if ((PurchaseOrderReceivingSerialList(lnList).getSerial01() == null || "".equals(PurchaseOrderReceivingSerialList(lnList).getSerial01()))
                                    || (PurchaseOrderReceivingSerialList(lnList).getSerial02() == null || "".equals(PurchaseOrderReceivingSerialList(lnList).getSerial02()))) {
                                poJSON.put("result", "error");
                                poJSON.put("message", "Serialized item found in transaction details."
                                                    + "\n\nEntry No " + PurchaseOrderReceivingSerialList(lnList).getEntryNo() + ": Serial cannot be empty.");
                                return poJSON;
                            }

                            if ("02".equals(Master().getIndustryId()) || "03".equals(Master().getIndustryId())) {
                                if (PurchaseOrderReceivingSerialList(lnList).getLocationId() == null || "".equals(PurchaseOrderReceivingSerialList(lnList).getLocationId())) {
                                    poJSON.put("result", "error");
                                    poJSON.put("message", "Serialized item found in transaction details."
                                                    + "\n\nEntry No " + PurchaseOrderReceivingSerialList(lnList).getEntryNo() + ": Location cannot be empty.");
                                    return poJSON;
                                }
                            }

                            if ("03".equals(Master().getIndustryId())) {
                                if ((PurchaseOrderReceivingSerialList(lnList).getPlateNo() == null || "".equals(PurchaseOrderReceivingSerialList(lnList).getPlateNo()))
                                        && (PurchaseOrderReceivingSerialList(lnList).getConductionStickerNo() == null || "".equals(PurchaseOrderReceivingSerialList(lnList).getConductionStickerNo()))) {
                                    poJSON.put("result", "error");
                                    poJSON.put("message", "Serialized item found in transaction details."
                                                    + "\n\nEntry No " + PurchaseOrderReceivingSerialList(lnList).getEntryNo() + ": CS / Plate No cannot be empty.");
                                    return poJSON;
                                }
                            }
                            
                            //Set Serial ID
                            if(Master().getPurpose().equals(PurchaseOrderReceivingStatus.Purpose.REGULAR)){
                                poJSON = setSerialId(lnList);
                                if("error".equals((String) poJSON.get("result"))){
                                    return poJSON;
                                }
                            } else if(Master().getPurpose().equals(PurchaseOrderReceivingStatus.Purpose.REPLACEMENT)){
                                if (PurchaseOrderReceivingSerialList(lnList).getSerialId() == null || "".equals(PurchaseOrderReceivingSerialList(lnList).getSerialId())) {
                                    lsSerialId = getSerialId(lnList);
                                    if(!lsSerialId.isEmpty()){
                                        PurchaseOrderReceivingSerialList(lnList).setSerialId(lsSerialId);
                                    } else {
                                        poJSON.put("result", "error");
                                        poJSON.put("message",  "Please select serial that exists in Purchase Order Return transaction at row "+PurchaseOrderReceivingSerialList(lnList).getEntryNo()+".");
                                        return poJSON;
                                    }
                                    break;
                                }
                                
                                poJSON = checkExistingPOR(lnList, PurchaseOrderReceivingSerialList(lnList).getSerialId());
                                if ("error".equals((String) poJSON.get("result"))) {
                                    return poJSON;
                                }
                                lsSerialId = "";
                            }
                            
                            //No need to validate Existing serial in DB: Inv_Serial Class will be the one to check it.
                            //Check for existing serial 01
                            JSONObject loJSON = checkExistingSerialinDB(lnList, "serial01");
                            lsColumnName = getColumnName("serial01");
                            if("error".equals((String) loJSON.get("result"))){
                                poJSON.put("result", "error");
                                poJSON.put("message", "Serialized item found in transaction details."
                                                    + "\n\nEntry No "+PurchaseOrderReceivingSerialList(lnList).getEntryNo() + ": " 
                                                    + lsColumnName +" < "+ PurchaseOrderReceivingSerialList(lnList).getSerial01() 
                                                    + " > already exists in the database." 
                                                    +"\nPlease contact the System Administrator for assistance.");
                                return poJSON;
                            } 
    
                            //Check for existing serial 02
                            loJSON = checkExistingSerialinDB(lnList, "serial02");
                            lsColumnName = getColumnName("serial02");
                            if("error".equals((String) loJSON.get("result"))){
                                poJSON.put("result", "error");
                                poJSON.put("message", "Serialized item found in transaction details."
                                                    + "\n\nEntry No "+PurchaseOrderReceivingSerialList(lnList).getEntryNo() + ": " 
                                                    + lsColumnName +" < "+ PurchaseOrderReceivingSerialList(lnList).getSerial02() 
                                                    + " > already exists in the database." 
                                                    +"\nPlease contact the System Administrator for assistance.");
                                return poJSON;
                            }

                            //Check for existing CS No
                            if (PurchaseOrderReceivingSerialList(lnList).getConductionStickerNo()!= null 
                                    && !"".equals(PurchaseOrderReceivingSerialList(lnList).getConductionStickerNo())){
                                loJSON = checkExistingSerialinDB(lnList, "csno");
                                lsColumnName = getColumnName("csno");
                                if("error".equals((String) loJSON.get("result"))){
                                    poJSON.put("result", "error");
                                    poJSON.put("message", "Serialized item found in transaction details."
                                                        + "\n\nEntry No "+PurchaseOrderReceivingSerialList(lnList).getEntryNo() + ": " 
                                                        + lsColumnName +" < "+ PurchaseOrderReceivingSerialList(lnList).getConductionStickerNo() 
                                                        + " > already exists in the database." 
                                                        +"\nPlease contact the System Administrator for assistance.");
                                    return poJSON;
                                }
                            }
    
                            //Check for existing Plate No
                            if (PurchaseOrderReceivingSerialList(lnList).getPlateNo() != null 
                                    && !"".equals(PurchaseOrderReceivingSerialList(lnList).getPlateNo())){
                                loJSON = checkExistingSerialinDB(lnList, "plateno");
                                lsColumnName = getColumnName("plateno");
                                if("error".equals((String) loJSON.get("result"))){
                                    poJSON.put("result", "error");
                                    poJSON.put("message", "Serialized item found in transaction details."
                                                        + "\n\nEntry No "+PurchaseOrderReceivingSerialList(lnList).getEntryNo() + ": " 
                                                        + lsColumnName +" < "+ PurchaseOrderReceivingSerialList(lnList).getPlateNo() 
                                                        + " > already exists in the database." 
                                                        +"\nPlease contact the System Administrator for assistance.");
                                    return poJSON;
                                }
                            }
                            lnSerialCnt++;
                        }
                    }

                    if (lnSerialCnt != Detail(lnCtr).getQuantity().doubleValue()) {
                        poJSON.put("result", "error");
                        poJSON.put("message", "Serialized item found in transaction details resulting to mismatch in POR serials.\nQuantity must be equal to POR Serial list for Entry No. " + (lnCtr + 1) + ".");
                        return poJSON;
                    }
                }

                lnSerialCnt = 0;
            }
        }
        
        
        //Update this fieds only in PO Receiving Form
        if(!pbIsFinance){
            //Remove Unnecessary Transaction Attachment
            if (getTransactionAttachmentCount() > 0) {
                Iterator<TransactionAttachment> attachment = TransactionAttachmentList().iterator();
                while (attachment.hasNext()) {
                    TransactionAttachment item = attachment.next();

                    if ((String) item.getModel().getFileName() == null || "".equals(item.getModel().getFileName())) {
                        attachment.remove();
                    }
                }
            }

            //Set Transaction Attachments
            for (int lnCtr = 0; lnCtr <= getTransactionAttachmentCount() - 1; lnCtr++) {
                TransactionAttachmentList(lnCtr).getModel().setSourceCode(SOURCE_CODE);
                TransactionAttachmentList(lnCtr).getModel().setSourceNo(Master().getTransactionNo());
            }

            //Allow the user to edit details but seek an approval from the approving officer
            if (PurchaseOrderReceivingStatus.CONFIRMED.equals(Master().getTransactionStatus())) {
                poJSON = setValueToOthers(Master().getTransactionStatus());
                if (!"success".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }
            }
        } else {
            if(poJournal != null){
                if(poJournal.getEditMode() == EditMode.ADDNEW || poJournal.getEditMode() == EditMode.UPDATE){
                    poJSON = validateJournal();
                    if ("error".equals((String) poJSON.get("result"))) {
                        return poJSON;
                    }
                }
            }
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
        
        try {
            //Skip Updating others when transaction triggered at SI Posting
            if(pbIsFinance){
                if(poJournal != null){
                    if(poJournal.getEditMode() == EditMode.ADDNEW || poJournal.getEditMode() == EditMode.UPDATE){
                        poJournal.setWithParent(true);
                        poJournal.Master().setModifiedDate(poGRider.getServerDate());
                        poJSON = poJournal.SaveTransaction();
                        if ("error".equals((String) poJSON.get("result"))) {
                            return poJSON;
                        }
                    }
                }
                poJSON.put("result", "success");
                return poJSON; 
            }
            
            int lnCtr, lnRow;
            for (lnRow = 0; lnRow <= getPurchaseOrderReceivingSerialCount() - 1; lnRow++) {        
                System.out.println("SAVE INVENTORY SERIAL ROW : " + lnRow);
                //Purchase Order Receiving Serial
                InvSerial loInvSerial = new InvControllers(poGRider, logwrapr).InventorySerial();
                //1. Check for Serial ID
                if ("".equals(paOthers.get(lnRow).getSerialId()) || paOthers.get(lnRow).getSerialId() == null) {
                    //1.1 Create New Inventory Serial
                    poJSON = loInvSerial.newRecord();
                    System.out.println("inv serial new record : " + (String) poJSON.get("message"));
                    if ("error".equals((String) poJSON.get("result"))) {
                        return poJSON;
                    }
                } else {
                    //1.2 Update Inventory Serial / Registration
                    poJSON = loInvSerial.openRecord(paOthers.get(lnRow).getSerialId());
                    System.out.println("inv serial open record : " + (String) poJSON.get("message"));
                    if ("error".equals((String) poJSON.get("result"))) {
                        return poJSON;
                    }
                    System.out.println("inv serial edit mode : " +  loInvSerial.getEditMode());
                    poJSON = loInvSerial.updateRecord();
                    System.out.println("inv serial update record : " + (String) poJSON.get("message"));
                    if ("error".equals((String) poJSON.get("result"))) {
                        return poJSON;
                    }
                }
                
                //2. Update values for serial
                if (loInvSerial.getEditMode() == EditMode.ADDNEW || loInvSerial.getEditMode() == EditMode.UPDATE) {
//                    loInvSerial.getModel().setIndustry(Master().getIndustryId()); //TODO
                    loInvSerial.getModel().setStockId(paOthers.get(lnRow).getStockId());
                    loInvSerial.getModel().setSerial01(paOthers.get(lnRow).getSerial01());
                    loInvSerial.getModel().setSerial02(paOthers.get(lnRow).getSerial02());
                    loInvSerial.getModel().setUnitType(paOthers.get(lnRow).Inventory().getUnitType());
                    
//                    if(poGRider.isWarehouse()){
//                        loInvSerial.getModel().setLocation("0"); 
//                    } else {
//                        loInvSerial.getModel().setLocation("1"); 
//                    }
                    
                    //Only set location of inv serial into 1 when confirmed according to ma'am she 05152025
                    if (PurchaseOrderReceivingStatus.CONFIRMED.equals(Master().getTransactionStatus())) {
                        loInvSerial.getModel().setLocation("1"); 
                    } else {
                        loInvSerial.getModel().setLocation("0"); 
                    }

                    //2.1 Only set branch code and company id during creation of serial in por
                    if (loInvSerial.getEditMode() == EditMode.ADDNEW) {
                        loInvSerial.getModel().setBranchCode(poGRider.getBranchCode());
                        loInvSerial.getModel().setCompnyId(Master().getCompanyId());
                    }
                    
                    if (!"".equals(paOthers.get(lnRow).getPlateNo()) && paOthers.get(lnRow).getPlateNo() != null) {
                        loInvSerial.SerialRegistration().setPlateNoP(paOthers.get(lnRow).getPlateNo());
                    }
                    
                    if (!"".equals(paOthers.get(lnRow).getConductionStickerNo()) && paOthers.get(lnRow).getConductionStickerNo() != null){
                        loInvSerial.SerialRegistration().setConductionStickerNo(paOthers.get(lnRow).getConductionStickerNo());
                    }

                    //3. Validation Serial
                    poJSON = loInvSerial.isEntryOkay();
                    System.out.println("inv serial validation : " + (String) poJSON.get("message"));
                    if ("error".equals((String) poJSON.get("result"))) {
                        return poJSON;
                    }

                    //4. Save Inventory Serial
                    System.out.println("----------------------SAVE INV SERIAL---------------------- ");
                    System.out.println("Serial ID  : " + loInvSerial.getModel().getSerialId());
                    System.out.println("Serial 01  : " + loInvSerial.getModel().getSerial01());
                    System.out.println("Serial 02  : " + loInvSerial.getModel().getSerial02());
                    System.out.println("Location   : " + loInvSerial.getModel().getLocation());
                    System.out.println("Edit Mode  : " + loInvSerial.getEditMode());
                    System.out.println("---------------------------------------------------------------------- ");
                    loInvSerial.setWithParentClass(true);
                    poJSON = loInvSerial.saveRecord();
                    if ("error".equals((String) poJSON.get("result"))) {
                        System.out.println("inv serial saving" + (String) poJSON.get("message"));
                        return poJSON;
                    }
                }
                //5. Set serial id to por serial
                if (paOthers.get(lnRow).getSerialId().equals("") || paOthers.get(lnRow).getSerialId() == null) {
                    paOthers.get(lnRow).setSerialId(loInvSerial.getModel().getSerialId());
                }
                //6. Save Purchase Order Receiving Serial
                System.out.println("----------------------SAVE PURCHASE ORDER RECEIVING SERIAL---------------------- ");
                System.out.println("Transaction No  : " + paOthers.get(lnRow).getTransactionNo());
                System.out.println("Entry No  : " + paOthers.get(lnRow).getEntryNo());
                System.out.println("Serial ID : " + paOthers.get(lnRow).getSerialId());
                System.out.println("Location  : " + paOthers.get(lnRow).getLocationId());
                System.out.println("Edit Mode : " + paOthers.get(lnRow).getEditMode());
                System.out.println("---------------------------------------------------------------------- ");
                paOthers.get(lnRow).setTransactionNo(Master().getTransactionNo());
                paOthers.get(lnRow).setModifiedDate(poGRider.getServerDate());
                poJSON = paOthers.get(lnRow).saveRecord();
                if ("error".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }
            }

            //Save Attachments
            for (lnCtr = 0; lnCtr <= getTransactionAttachmentCount() - 1; lnCtr++) {
                if (paAttachments.get(lnCtr).getEditMode() == EditMode.ADDNEW || paAttachments.get(lnCtr).getEditMode() == EditMode.UPDATE) {
                    paAttachments.get(lnCtr).getModel().setModifyingId(poGRider.Encrypt(poGRider.getUserID()));
                    paAttachments.get(lnCtr).getModel().setModifiedDate(poGRider.getServerDate());
                    paAttachments.get(lnCtr).setWithParentClass(true);
                    poJSON = paAttachments.get(lnCtr).saveRecord();
                    if ("error".equals((String) poJSON.get("result"))) {
                        return poJSON;
                    }
                }
            }

            //Save Purchase Order, Serial Ledger, Inventory
            if (PurchaseOrderReceivingStatus.CONFIRMED.equals(Master().getTransactionStatus())) {
                poJSON = saveUpdateOthers(PurchaseOrderReceivingStatus.CONFIRMED);
                if (!"success".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }
            }

        } catch (SQLException | GuanzonException | CloneNotSupportedException ex) {
            Logger.getLogger(PurchaseOrderReceiving.class.getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
            return poJSON;
        }
        poJSON.put("result", "success");
        return poJSON;
    }
    
    private JSONObject validateJournal(){
        poJSON = new JSONObject();
        double ldblCreditAmt = 0.0000;
        double ldblDebitAmt = 0.0000;
        for(int lnCtr = 0; lnCtr <= poJournal.getDetailCount()-1; lnCtr++){
            ldblDebitAmt += poJournal.Detail(lnCtr).getDebitAmount();
            ldblCreditAmt += poJournal.Detail(lnCtr).getCreditAmount();
            
            if(poJournal.Detail(lnCtr).getCreditAmount() > 0.0000 ||  poJournal.Detail(lnCtr).getDebitAmount() > 0.0000){
                if(poJournal.Detail(lnCtr).getAccountCode() != null && !"".equals(poJournal.Detail(lnCtr).getAccountCode())){
                    if(poJournal.Detail(lnCtr).getForMonthOf() == null || "1900-01-01".equals(xsDateShort(poJournal.Detail(lnCtr).getForMonthOf()))){
                        poJSON.put("result", "error");
                        poJSON.put("message", "Invalid reporting date of journal at row "+(lnCtr+1)+" .");
                        return poJSON;
                    }
                }
            }
        }
        
        if(ldblDebitAmt == 0.0000 ){
            poJSON.put("result", "error");
            poJSON.put("message", "Invalid journal entry debit amount.");
            return poJSON;
        }
        
        if(ldblCreditAmt == 0.0000){
            poJSON.put("result", "error");
            poJSON.put("message", "Invalid journal entry credit amount.");
            return poJSON;
        }
        
        if(ldblDebitAmt < ldblCreditAmt || ldblDebitAmt > ldblCreditAmt){
            poJSON.put("result", "error");
            poJSON.put("message", "Debit should be equal to credit amount.");
            return poJSON;
        }
        
//        if(ldblDebitAmt < Master().getTransactionTotal().doubleValue() || ldblDebitAmt > Master().getTransactionTotal().doubleValue()){
//            poJSON.put("result", "error");
//            poJSON.put("message", "Debit and credit amount should be equal to transaction total.");
//            return poJSON;
//        }
        
        
        return poJSON;
    }

    private PurchaseOrder PurchaseOrder() {
        return new PurchaseOrderControllers(poGRider, logwrapr).PurchaseOrder();
    }
    
    private PurchaseOrderReturn PurchaseOrderReturn() {
        return new PurchaseOrderReturnControllers(poGRider, logwrapr).PurchaseOrderReturn();
    }

//    private InventoryTransaction InventoryTransaction(){
//        return new InventoryTransactionControllers(poGRider, logwrapr).InventoryTransaction();
//    }
    private JSONObject setValueToOthers(String status)
            throws CloneNotSupportedException,
            SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        paPurchaseOrder = new ArrayList<>();
        paPurchaseOrderReturn = new ArrayList<>();
        paInventoryTransaction = new ArrayList<>();
        int lnCtr;

        //Update Purchase Order exist in PO Receiving Detail
        for (lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
            System.out.println("----------------------PURCHASE ORDER RECEIVING DETAIL---------------------- ");
            System.out.println("TransNo : " + (lnCtr + 1) + " : " + Detail(lnCtr).getTransactionNo());
            System.out.println("OrderNo : " + (lnCtr + 1) + " : " + Detail(lnCtr).getOrderNo());
            System.out.println("StockId : " + (lnCtr + 1) + " : " + Detail(lnCtr).getStockId());
            System.out.println("------------------------------------------------------------------ ");
            if (Detail(lnCtr).getOrderNo() != null && !"".equals(Detail(lnCtr).getOrderNo())) {
                //1. Check for discrepancy
                if (Detail(lnCtr).getOrderQty().doubleValue() != Detail(lnCtr).getQuantity().doubleValue()) {
                    System.out.println("Require Approval");
                    pbApproval = true;
                }
                
                switch(Master().getPurpose()){
                    case PurchaseOrderReceivingStatus.Purpose.REGULAR:
                        //Purchase Order
                        poJSON = updatePurchaseOrder(status, Detail(lnCtr).getOrderNo(), Detail(lnCtr).getStockId(), Detail(lnCtr).getQuantity().doubleValue());
                        if("error".equals((String) poJSON.get("result"))){
                            return poJSON;
                        }
                        break;
                    case PurchaseOrderReceivingStatus.Purpose.REPLACEMENT:
                        //Purchase Order Return
                        poJSON = updatePurchaseOrderReturn(status, Detail(lnCtr).getOrderNo(), Detail(lnCtr).getStockId(), Detail(lnCtr).getQuantity().doubleValue());
                        if("error".equals((String) poJSON.get("result"))){
                            return poJSON;
                        }
                        break;
                
                }

                //Inventory Transaction
                if(Detail(lnCtr).getReplaceId() != null && !"".equals(Detail(lnCtr).getReplaceId())){
                    updateInventoryTransaction(status, Detail(lnCtr).getReplaceId(), Detail(lnCtr).getQuantity().doubleValue());
                } else {
                    updateInventoryTransaction(status, Detail(lnCtr).getStockId(), Detail(lnCtr).getQuantity().doubleValue());
                }

            } else {
                //Require approve for all po receiving without po
                System.out.println("Require Approval");
                pbApproval = true;
            }
        }

        //Update purchase order removed in purchase order receiving
        for (lnCtr = 0; lnCtr <= getDetailRemovedCount() - 1; lnCtr++) {
            
            switch(Master().getPurpose()){
                case PurchaseOrderReceivingStatus.Purpose.REGULAR:
                    //Purchase Order
                    poJSON = updatePurchaseOrder(status, DetailRemove(lnCtr).getOrderNo(), DetailRemove(lnCtr).getStockId(), DetailRemove(lnCtr).getQuantity().doubleValue());
                    if("error".equals((String) poJSON.get("result"))){
                        return poJSON;
                    }
                    break;
                case PurchaseOrderReceivingStatus.Purpose.REPLACEMENT:
                    //Purchase Order Return
                    poJSON = updatePurchaseOrderReturn(status, DetailRemove(lnCtr).getOrderNo(), DetailRemove(lnCtr).getStockId(), DetailRemove(lnCtr).getQuantity().doubleValue());
                    if("error".equals((String) poJSON.get("result"))){
                        return poJSON;
                    }
                    break;

            }
            
            //Inventory Transaction TODO
            if(DetailRemove(lnCtr).getReplaceId() != null && !"".equals(DetailRemove(lnCtr).getReplaceId())){
                updateInventoryTransaction(status, DetailRemove(lnCtr).getReplaceId(), DetailRemove(lnCtr).getQuantity().doubleValue());
            } else {
                updateInventoryTransaction(status, DetailRemove(lnCtr).getStockId(), DetailRemove(lnCtr).getQuantity().doubleValue());
            }
        }

        poJSON.put("result", "success");
        return poJSON;
    }

    private JSONObject updatePurchaseOrder(String status, String orderNo, String stockId, double quantity)
            throws GuanzonException,
            SQLException,
            CloneNotSupportedException {
        int lnRow, lnList;
        double lnRecQty = 0;
        double lnOrderQty = 0;
        boolean lbExist = false;
        //2.check if order no is already exists in purchase order array list
        for (lnRow = 0; lnRow <= paPurchaseOrder.size() - 1; lnRow++) {
            System.out.println("paPurchaseOrder.get(lnRow).Master().getTransactionNo() : " + paPurchaseOrder.get(lnRow).Master().getTransactionNo());
            if (paPurchaseOrder.get(lnRow).Master().getTransactionNo() != null) {
                if (orderNo.equals(paPurchaseOrder.get(lnRow).Master().getTransactionNo())) {
                    lbExist = true;
                    break;
                }
            }
        }

        //3. If order no is not exist add it on puchase order array list then open the transaction
        if (!lbExist) {
            paPurchaseOrder.add(PurchaseOrder());
            paPurchaseOrder.get(paPurchaseOrder.size() - 1).InitTransaction();
            paPurchaseOrder.get(paPurchaseOrder.size() - 1).OpenTransaction(orderNo);
            paPurchaseOrder.get(paPurchaseOrder.size() - 1).UpdateTransaction();
            lnList = paPurchaseOrder.size() - 1;
        } else {
            //if already exist, get the row no of purchase order
            lnList = lnRow;
        }
        
        switch (status) {
            case PurchaseOrderReceivingStatus.CONFIRMED:
            case PurchaseOrderReceivingStatus.PAID:
            case PurchaseOrderReceivingStatus.POSTED:
                //Get total received qty from other po receiving entry
                lnRecQty = getReceivedQty(orderNo, stockId, true).doubleValue();
                //Add received qty in po receiving
                lnRecQty = lnRecQty + quantity;
                
                for (lnRow = 0; lnRow <= paPurchaseOrder.get(lnList).getDetailCount() - 1; lnRow++) {
                    if (stockId.equals(paPurchaseOrder.get(lnList).Detail(lnRow).getStockID())) {
                        lnOrderQty = lnOrderQty + paPurchaseOrder.get(lnList).Detail(lnRow).getQuantity().doubleValue();
                    }
                }
                
                if(lnRecQty > lnOrderQty){
                    poJSON.put("result", "error");
                    poJSON.put("message", "Confirmed receive quantity cannot be greater than the order quantity for Order No. " + orderNo);
                    return poJSON;
                }
                
                break;
            case PurchaseOrderReceivingStatus.VOID:
            case PurchaseOrderReceivingStatus.RETURNED:
                //Get total received qty from other po receiving entry
                lnRecQty = getReceivedQty(orderNo, stockId, false).doubleValue();
                //Deduct received qty in po receiving
                lnRecQty = lnRecQty - quantity;
                break;
        }
        
        for (lnRow = 0; lnRow <= paPurchaseOrder.get(lnList).getDetailCount() - 1; lnRow++) {
            if (stockId.equals(paPurchaseOrder.get(lnList).Detail(lnRow).getStockID())) {
                //set Receive qty in Purchase Order detail
                if(lnRecQty <= 0){
                    lnRecQty = 0;
                    paPurchaseOrder.get(lnList).Detail(lnRow).setReceivedQuantity(0);
                } else {
                    if(lnRecQty > paPurchaseOrder.get(lnList).Detail(lnRow).getQuantity().doubleValue()){
                        paPurchaseOrder.get(lnList).Detail(lnRow).setReceivedQuantity(paPurchaseOrder.get(lnList).Detail(lnRow).getQuantity());
                        lnRecQty = lnRecQty - paPurchaseOrder.get(lnList).Detail(lnRow).getQuantity().doubleValue();
                    } else {
                        paPurchaseOrder.get(lnList).Detail(lnRow).setReceivedQuantity(lnRecQty);
                        lnRecQty = 0;
                    }
                }

                paPurchaseOrder.get(lnList).Detail(lnRow).setModifiedDate(poGRider.getServerDate());
            }
        }
        
        poJSON.put("result", "success");
        return poJSON;
    }
    
    private JSONObject updatePurchaseOrderReturn(String status, String orderNo, String stockId, double quantity)
            throws GuanzonException,
            SQLException,
            CloneNotSupportedException {
        int lnRow, lnList;
        double lnRecQty = 0;
        double lnOrderQty = 0;
        boolean lbExist = false;
        //2.check if order no is already exists in purchase order array list
        for (lnRow = 0; lnRow <= paPurchaseOrderReturn.size() - 1; lnRow++) {
            System.out.println("paPurchaseOrderReturn.get(lnRow).Master().getTransactionNo() : " + paPurchaseOrderReturn.get(lnRow).Master().getTransactionNo());
            if (paPurchaseOrderReturn.get(lnRow).Master().getTransactionNo() != null) {
                if (orderNo.equals(paPurchaseOrderReturn.get(lnRow).Master().getTransactionNo())) {
                    lbExist = true;
                    break;
                }
            }
        }

        //3. If order no is not exist add it on puchase order array list then open the transaction
        if (!lbExist) {
            paPurchaseOrderReturn.add(PurchaseOrderReturn());
            paPurchaseOrderReturn.get(paPurchaseOrderReturn.size() - 1).InitTransaction();
            paPurchaseOrderReturn.get(paPurchaseOrderReturn.size() - 1).OpenTransaction(orderNo);
            paPurchaseOrderReturn.get(paPurchaseOrderReturn.size() - 1).UpdateTransaction();
            lnList = paPurchaseOrderReturn.size() - 1;
        } else {
            //if already exist, get the row no of purchase order
            lnList = lnRow;
        }
        
        switch (status) {
            case PurchaseOrderReceivingStatus.CONFIRMED:
            case PurchaseOrderReceivingStatus.PAID:
            case PurchaseOrderReceivingStatus.POSTED:
                //Get total received qty from other po receiving entry
                lnRecQty = getReceivedQty(orderNo, stockId, true).doubleValue();
                //Add received qty in po receiving
                lnRecQty = lnRecQty + quantity;
                
                for (lnRow = 0; lnRow <= paPurchaseOrderReturn.get(lnList).getDetailCount() - 1; lnRow++) {
                    if (stockId.equals(paPurchaseOrderReturn.get(lnList).Detail(lnRow).getStockId())) {
                        lnOrderQty = lnOrderQty + paPurchaseOrderReturn.get(lnList).Detail(lnRow).getQuantity().doubleValue();
                    }
                }
                
                if(lnRecQty > lnOrderQty){
                    poJSON.put("result", "error");
                    poJSON.put("message", "Confirmed receive quantity cannot be greater than the return quantity for Source No. " + orderNo);
                    return poJSON;
                }
                
                break;
            case PurchaseOrderReceivingStatus.VOID:
            case PurchaseOrderReceivingStatus.RETURNED:
                //Get total received qty from other po receiving entry
                lnRecQty = getReceivedQty(orderNo, stockId, false).doubleValue();
                //Deduct received qty in po receiving
                lnRecQty = lnRecQty - quantity;
                break;
        }
        
        for (lnRow = 0; lnRow <= paPurchaseOrderReturn.get(lnList).getDetailCount() - 1; lnRow++) {
            if (stockId.equals(paPurchaseOrderReturn.get(lnList).Detail(lnRow).getStockId())) {
                //set Receive qty in Purchase Order detail
                if(lnRecQty <= 0){
                    lnRecQty = 0;
                    paPurchaseOrderReturn.get(lnList).Detail(lnRow).setReceivedQty(0);
                } else {
                    if(lnRecQty > paPurchaseOrderReturn.get(lnList).Detail(lnRow).getQuantity().doubleValue()){
                        paPurchaseOrderReturn.get(lnList).Detail(lnRow).setReceivedQty(paPurchaseOrderReturn.get(lnList).Detail(lnRow).getQuantity());
                        lnRecQty = lnRecQty - paPurchaseOrderReturn.get(lnList).Detail(lnRow).getQuantity().doubleValue();
                    } else {
                        paPurchaseOrderReturn.get(lnList).Detail(lnRow).setReceivedQty(lnRecQty);
                        lnRecQty = 0;
                    }
                }

                paPurchaseOrderReturn.get(lnList).Detail(lnRow).setModifiedDate(poGRider.getServerDate());
            }
        }
        
        poJSON.put("result", "success");
        return poJSON;
    }

    //Open record for checking total receive qty per purchase order
    private Number getReceivedQty(String orderNo, String stockId, boolean isAdd)
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        double lnRecQty = 0;
        String lsSQL = " SELECT "
                + " b.nQuantity AS nQuantity "
                + " FROM po_receiving_master a "
                + " LEFT JOIN po_receiving_detail b ON b.sTransNox = a.sTransNox ";
        lsSQL = MiscUtil.addCondition(lsSQL, " b.sStockIDx = " + SQLUtil.toSQL(stockId)
                + " AND ( a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderReceivingStatus.CONFIRMED)
                + " OR a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderReceivingStatus.PAID)
                + " OR a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderReceivingStatus.POSTED)
                + " ) "
                + " AND a.cPurposex = " + SQLUtil.toSQL(Master().getPurpose()));
        
        if(orderNo != null && !"".equals(orderNo)){
            lsSQL = lsSQL + " AND b.sOrderNox = " + SQLUtil.toSQL(orderNo);
        }
        
        if (isAdd) {
            lsSQL = lsSQL + " AND a.sTransNox <> " + SQLUtil.toSQL(Master().getTransactionNo());
        }
        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        try {
            if (MiscUtil.RecordCount(loRS) >= 0) {
                while (loRS.next()) {
                    lnRecQty = lnRecQty + loRS.getDouble("nQuantity");
                }
            }
            MiscUtil.close(loRS);
        } catch (SQLException e) {
            System.out.println("No record loaded.");
            lnRecQty = 0;
        }
        return lnRecQty;
    }

    private JSONObject saveUpdateOthers(String status)
            throws CloneNotSupportedException {
        /*Only modify this if there are other tables to modify except the master and detail tables*/
        poJSON = new JSONObject();
        int lnCtr, lnRow;
        try {
            switch(Master().getPurpose()){
                case PurchaseOrderReceivingStatus.Purpose.REGULAR:
                    //1. Save Purchase Order exist in PO Receiving Detail 
                    for (lnCtr = 0; lnCtr <= paPurchaseOrder.size() - 1; lnCtr++) {
                        if(PurchaseOrderReceivingStatus.CONFIRMED.equals(status)){
                            paPurchaseOrder.get(lnCtr).Master().setProcessed(true); //when processed is true it will not go back to unprocessed.
                        }
                        paPurchaseOrder.get(lnCtr).Master().setModifyingId(poGRider.Encrypt(poGRider.getUserID()));
                        paPurchaseOrder.get(lnCtr).Master().setModifiedDate(poGRider.getServerDate());
                        paPurchaseOrder.get(lnCtr).setWithParent(true);
                        paPurchaseOrder.get(lnCtr).setWithUI(false);
                        poJSON = paPurchaseOrder.get(lnCtr).SaveTransaction();
                        if ("error".equals((String) poJSON.get("result"))) {
                            System.out.println("Purchase Order Saving " + (String) poJSON.get("message"));
                            return poJSON;
                        }
                    }
                    break;
                case PurchaseOrderReceivingStatus.Purpose.REPLACEMENT:
                    //1. Save Purchase Order Return exist in PO Receiving Detail 
                    for (lnCtr = 0; lnCtr <= paPurchaseOrderReturn.size() - 1; lnCtr++) {
                        if(PurchaseOrderReceivingStatus.CONFIRMED.equals(status)){
                            paPurchaseOrderReturn.get(lnCtr).Master().isProcessed(true); //when processed is true it will not go back to unprocessed.
                        }
                        paPurchaseOrderReturn.get(lnCtr).Master().setModifyingId(poGRider.Encrypt(poGRider.getUserID()));
                        paPurchaseOrderReturn.get(lnCtr).Master().setModifiedDate(poGRider.getServerDate());
                        paPurchaseOrderReturn.get(lnCtr).setWithParent(true);
                         paPurchaseOrderReturn.get(lnCtr).setWithUI(false);
                        poJSON = paPurchaseOrderReturn.get(lnCtr).SaveTransaction();
                        if ("error".equals((String) poJSON.get("result"))) {
                            System.out.println("Purchase Order Return Saving " + (String) poJSON.get("message"));
                            return poJSON;
                        }
                    }
                    break;

            }

            //2. Save Inventory Transaction TODO
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
            if (PurchaseOrderReceivingStatus.CONFIRMED.equals(Master().getTransactionStatus())
                    || PurchaseOrderReceivingStatus.PAID.equals(Master().getTransactionStatus())
                    || PurchaseOrderReceivingStatus.POSTED.equals(Master().getTransactionStatus())) {
                //Save Inventory Serial Ledger
                //InventoryTrans.POReceiving();
                
            }
        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(PurchaseOrderReceiving.class.getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
            return poJSON;
        }
        poJSON.put("result", "success");
        return poJSON;
    }
    
    private JSONObject saveUpdateInvSerial(String status){
        try {
            poJSON = new JSONObject();
            
            //Populate purhcase receiving serials
            for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
                getPurchaseOrderReceivingSerial(Detail(lnCtr).getEntryNo());
            }
            
            InvSerial loInvSerial = new InvControllers(poGRider, logwrapr).InventorySerial();
            loInvSerial.setWithParentClass(true);
            
            for( int lnCtr = 0; lnCtr <= getPurchaseOrderReceivingSerialCount()-1;lnCtr++){
                if(paOthers.get(lnCtr).getSerialId() != null && !"".equals(paOthers.get(lnCtr).getSerialId())){
                    poJSON = loInvSerial.openRecord(paOthers.get(lnCtr).getSerialId());
                    if ("error".equals((String) poJSON.get("result"))) {
                        return poJSON;
                    }
                    System.out.println(loInvSerial.getEditMode());
                    poJSON = loInvSerial.updateRecord();
                    if ("error".equals((String) poJSON.get("result"))) {
                        return poJSON;
                    }
                    
                    if(PurchaseOrderReceivingStatus.CONFIRMED.equals(status)){
                        loInvSerial.getModel().setLocation("1"); 
                    } else {
                        loInvSerial.getModel().setLocation("0"); 
                    }
                    
                    poJSON = loInvSerial.saveRecord();
                    if ("error".equals((String) poJSON.get("result"))) {
                        System.out.println("inv serial saving" + (String) poJSON.get("message"));
                        return poJSON;
                    } 
                }
            }
            
        } catch (SQLException | GuanzonException | CloneNotSupportedException ex) {
            Logger.getLogger(PurchaseOrderReceiving.class.getName()).log(Level.SEVERE, null, ex);
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
            return poJSON;
        }
           
        poJSON.put("result", "success");
        return poJSON;
    }
    
    private JSONObject deleteInvSerial(){
        try {
            poJSON = new JSONObject();
            switch(Master().getPurpose()){
                case PurchaseOrderReceivingStatus.Purpose.REGULAR:
                    //Do not delete Inv Serial when transaction is Pre-owned
                    if(paPurchaseOrder != null){
                        for (int lnCtr = 0; lnCtr <= paPurchaseOrder.size() - 1; lnCtr++) {
                            if(paPurchaseOrder.get(lnCtr).Master().getPreOwned()){
                                return poJSON;
                            }
                        }
                    }
                break;
                case PurchaseOrderReceivingStatus.Purpose.REPLACEMENT:
                    //Do not delete Inv Serial when transaction is Pre-owned
                    if(paPurchaseOrderReturn != null){
                        for (int lnCtr = 0; lnCtr <= paPurchaseOrderReturn.size() - 1; lnCtr++) {
                            for(int lnRow = 0; lnRow <= paPurchaseOrderReturn.get(lnCtr).getDetailCount()-1; lnRow++){
                                if(paPurchaseOrderReturn.get(lnCtr).Detail(lnRow).getSourceNo() != null 
                                    && !"".equals(paPurchaseOrderReturn.get(lnCtr).Detail(lnRow).getSourceNo())){
                                    if(paPurchaseOrderReturn.get(lnCtr).Detail(lnRow).PurchaseOrderMaster().getPreOwned()){
                                        return poJSON;
                                    }
                                } 
                            }
                        }
                    }
                break;
            }
            
            //Populate purhcase receiving serials
            for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
                getPurchaseOrderReceivingSerial(Detail(lnCtr).getEntryNo());
            }
            
            InvSerial loInvSerial = new InvControllers(poGRider, logwrapr).InventorySerial();
            loInvSerial.setWithParentClass(true);
            
            for( int lnCtr = 0; lnCtr <= getPurchaseOrderReceivingSerialCount()-1;lnCtr++){
                if(paOthers.get(lnCtr).getSerialId() != null && !"".equals(paOthers.get(lnCtr).getSerialId())){
                    poJSON = loInvSerial.openRecord(paOthers.get(lnCtr).getSerialId());
                    if ("error".equals((String) poJSON.get("result"))) {
                        return poJSON;
                    }
                    System.out.println(loInvSerial.getEditMode());
                    poJSON = loInvSerial.updateRecord();
                    if ("error".equals((String) poJSON.get("result"))) {
                        return poJSON;
                    }
                    
                    loInvSerial.getModel().setLocation("0");
                    poJSON = loInvSerial.saveRecord();
                    if ("error".equals((String) poJSON.get("result"))) {
                        return poJSON;
                    }
                    
                    //Not Supported yet
//                    poJSON = loInvSerial.deleteRecord();
//                    if ("error".equals((String) poJSON.get("result"))) {
//                        System.out.println("inv serial saving" + (String) poJSON.get("message"));
//                        return poJSON;
//                    }
                }
            }
            
        } catch (SQLException | GuanzonException  ex) {
            Logger.getLogger(PurchaseOrderReceiving.class.getName()).log(Level.SEVERE, null, ex);
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
            return poJSON;
        } catch (CloneNotSupportedException ex) {
            Logger.getLogger(PurchaseOrderReceiving.class.getName()).log(Level.SEVERE, null, ex);
        }
           
        poJSON.put("result", "success");
        return poJSON;
    }
    
    //TODO
    private void updateInventoryTransaction(String status, String stockId, double quantity)
            throws GuanzonException,
            SQLException,
            CloneNotSupportedException {
//        int lnRow, lnList;
//        int lnRecQty = 0;
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
//                case PurchaseOrderReceivingStatus.CONFIRMED:
//                case PurchaseOrderReceivingStatus.PAID:
//                case PurchaseOrderReceivingStatus.POSTED:
//                    //Get total received qty from other po receiving entry
//                    //lnRecQty = getReceivedQty("", stockId, true);
//                    //Add received qty in po receiving
//                    lnRecQty = paInventoryTransaction.get(lnList).Master().getQuantityOnHand() + quantity;
//                    break;
//                case PurchaseOrderReceivingStatus.VOID:
//                case PurchaseOrderReceivingStatus.RETURNED: 
//                    //Get total received qty from other po receiving entry
//                    //lnRecQty = getReceivedQty("", stockId, false);
//                    //Deduct received qty in po receiving
//                    lnRecQty = paInventoryTransaction.get(lnList).Master().getQuantityOnHand() - quantity;
//                    break;
//            }
//            //set Receive qty in Purchase Order detail
//            paInventoryTransaction.get(lnList).Master().setQuantityOnHand(lnRecQty);
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
            System.out.println("Dept ID : " + poGRider.getDepartment());
            Master().setBranchCode(poGRider.getBranchCode());
            Master().setIndustryId(psIndustryId);
            Master().setCompanyId(psCompanyId);
            Master().setCategoryCode(psCategorCd);
            Master().setDepartmentId(poGRider.getDepartment()); 
            Master().setTransactionDate(poGRider.getServerDate());
            Master().setReferenceDate(poGRider.getServerDate());
            Master().setInventoryTypeCode(getInventoryTypeCode());
            Master().setTermCode(getTermCode());
            Master().setTransactionStatus(PurchaseOrderReceivingStatus.OPEN);
            Master().setPurpose(PurchaseOrderReceivingStatus.Purpose.REGULAR); //Default
            Master().setPurpose(psPurpose);

        } catch (SQLException ex) {
            Logger.getLogger(PurchaseOrderReceiving.class.getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
            return poJSON;
        }

        poJSON.put("result", "success");
        return poJSON;
    }

    public String getTermCode()
            throws SQLException { //Default 45 Days
        String lsSQL = "SELECT sTermCode FROM term ";
        lsSQL = MiscUtil.addCondition(lsSQL, " sDescript LIKE " + SQLUtil.toSQL("45 %"));

        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        String lsInventoryTypeCode = "";

        if (loRS.next()) {
            lsInventoryTypeCode = loRS.getString("sTermCode");
        }

        MiscUtil.close(loRS);
        return lsInventoryTypeCode;
    }
    
    public String getInventoryTypeCode()
            throws SQLException {
        String lsSQL = "SELECT sInvTypCd FROM category ";
//        lsSQL = MiscUtil.addCondition(lsSQL, " sIndstCdx = " + SQLUtil.toSQL(psIndustryId)
//                                                +  " AND sCategrCd = " + SQLUtil.toSQL(psCategorCd));
        lsSQL = MiscUtil.addCondition(lsSQL, " sCategrCd = " + SQLUtil.toSQL(psCategorCd));

        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        String lsInventoryTypeCode = "";

        if (loRS.next()) {
            lsInventoryTypeCode = loRS.getString("sInvTypCd");
        }

        MiscUtil.close(loRS);
        return lsInventoryTypeCode;
    }

    public String getCompanyId() {
        String lsCompanyId = "";
        try {
            String lsSQL = "SELECT sCompnyID FROM branch ";
            lsSQL = MiscUtil.addCondition(lsSQL, " sBranchCd = " + SQLUtil.toSQL(poGRider.getBranchCode()));

            System.out.println("Executing SQL: " + lsSQL);
            ResultSet loRS = poGRider.executeQuery(lsSQL);

            if (loRS.next()) {
                lsCompanyId = loRS.getString("sCompnyID");
            }

            MiscUtil.close(loRS);
        } catch (SQLException ex) {
            Logger.getLogger(PurchaseOrderReceiving.class.getName()).log(Level.SEVERE, null, ex);
        }

        return lsCompanyId;
    }
    
    @Override
    public void initSQL() {
        SQL_BROWSE = " SELECT "
                + "   a.dTransact  "
                + " , a.sTransNox  "
                + " , a.sIndstCdx  "
                + " , a.sCompnyID  "
                + " , a.sSupplier  "
                + " , a.sReferNox  "
                + " , a.sCategrCd  "
                + " , e.sBranchNm  "
                + " , b.sCompnyNm  AS sSupplrNm"
                + " , c.sCompnyNm  AS sCompnyNm"
                + " , d.sDescript  AS sIndustry"
                + " FROM po_receiving_master a "
                + " LEFT JOIN client_master b ON b.sClientID = a.sSupplier "
                + " LEFT JOIN company c ON c.sCompnyID = a.sCompnyID "
                + " LEFT JOIN industry d ON d.sIndstCdx = a.sIndstCdx "
                + " LEFT JOIN branch e ON e.sBranchCd = a.sBranchCd ";
    }
    
//    @Override
//    public void initSQL() {
//        SQL_BROWSE = " SELECT "
//                + " DISTINCT a.sTransNox  "
//                + " , a.dTransact  "
//                + " , a.sIndstCdx  "
//                + " , a.sCompnyID  "
//                + " , a.sSupplier  "
//                + " , b.sCompnyNm  AS sSupplrNm"
//                + " , c.sCompnyNm  AS sCompnyNm"
//                + " , d.sDescript  AS sIndustry"
//                + " FROM po_receiving_master a "
//                + " LEFT JOIN client_master b ON b.sClientID = a.sSupplier "
//                + " LEFT JOIN company c ON c.sCompnyID = a.sCompnyID "
//                + " LEFT JOIN industry d ON d.sIndstCdx = a.sIndstCdx "
//                + " LEFT JOIN po_receiving_detail e ON e.sTransNox = a.sTransNox ";
//    }

    @Override
    protected JSONObject isEntryOkay(String status) {
        GValidator loValidator = PurchaseOrderReceivingValidatorFactory.make(Master().getIndustryId());

        loValidator.setApplicationDriver(poGRider);
        loValidator.setTransactionStatus(status);
        loValidator.setMaster(poMaster);
//        loValidator.setDetail(paDetail);

        poJSON = loValidator.validate();

        return poJSON;
    }

    private CustomJasperViewer poViewer = null;
    private String psTransactionNo = "";

    public JSONObject printRecord(Runnable onPrintedCallback) {
        poJSON = new JSONObject();
        String watermarkPath = System.getProperty("sys.default.path.config") + "/Reports//images/draft.png"; //set draft as default
        
        psTransactionNo = Master().getTransactionNo();
        try {
            
            //Reopen Transaction to get the accurate data
            poJSON = OpenTransaction(psTransactionNo);
            if ("error".equals((String) poJSON.get("result"))) {
                System.out.println("Print Record open transaction : " + (String) poJSON.get("message"));
                return poJSON;
            }
            
            // 1. Prepare parameters
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("sSupplierNm", Master().Supplier().getCompanyName());
            parameters.put("sBranchNm", poGRider.getBranchName()); //TODO
            parameters.put("sAddressx", poGRider.getAddress());
            parameters.put("sCompnyNm", poGRider.getClientName());
            parameters.put("sTransNox", Master().getTransactionNo());
            parameters.put("dReferDte", Master().getReferenceDate());
            parameters.put("sReferNox", Master().getReferenceNo());
            parameters.put("sRemarks", Master().getRemarks());
            parameters.put("dTransDte", new java.sql.Date(Master().getTransactionDate().getTime()));
            parameters.put("dDatexxx", new java.sql.Date(poGRider.getServerDate().getTime()));

            // Set watermark based on approval status
            switch (Master().getTransactionStatus()) {
                case PurchaseOrderReceivingStatus.CONFIRMED:
                case PurchaseOrderReceivingStatus.PAID:
                case PurchaseOrderReceivingStatus.POSTED:
                    if("1".equals(Master().getPrint())){
                        watermarkPath = System.getProperty("sys.default.path.config") + "/Reports//images/approvedreprint.png";
                    } else {
                        watermarkPath = System.getProperty("sys.default.path.config") + "/Reports//images/approved.png";
                    }
                    break;
//                case PurchaseOrderReceivingStatus.CANCELLED:
//                    watermarkPath = "D:\\GGC_Maven_Systems\\Reports\\images\\cancelled.png";
//                    break;
            }

            parameters.put("watermarkImagePath", watermarkPath);
            List<OrderDetail> orderDetails = new ArrayList<>();
            
            String jrxmlPath = System.getProperty("sys.default.path.config") + "/Reports/PurchaseOrderReceiving.jrxml";
            if(Master().getPurpose().equals(PurchaseOrderReceivingStatus.Purpose.REPLACEMENT)){
                jrxmlPath = System.getProperty("sys.default.path.config") + "/Reports/PurchaseOrderReplacement.jrxml";
            }
            double lnTotal = 0.0;
            int lnRow = 1;
            String lsDescription = "";
            String lsSerial = "";
            String lsBarcode = "";
            String lsMeasure = "";
            for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
                lnTotal = Detail(lnCtr).getUnitPrce().doubleValue() * Detail(lnCtr).getQuantity().doubleValue();
                
                if(Detail(lnCtr).isSerialized()){
                    getPurchaseOrderReceivingSerial(Detail(lnCtr).getEntryNo());
                    for(int lnList = 0; lnList <=getPurchaseOrderReceivingSerialCount()-1; lnList++){
                        if(PurchaseOrderReceivingSerialList(lnList).getEntryNo() == Detail(lnCtr).getEntryNo()){
                            if("".equals(lsSerial)){
                                lsSerial = PurchaseOrderReceivingSerialList(lnList).getSerial01();
                            } else {
                                lsSerial = lsSerial + "\n" + PurchaseOrderReceivingSerialList(lnList).getSerial01();
                            }
                        }
                    }
                }
                
                switch(Master().getCategoryCode()){
                    case PurchaseOrderReceivingStatus.CAR : 		//"0005": CAR        
                    case PurchaseOrderReceivingStatus.MOTORCYCLE :	//"0003": Motorcycle 
                    case PurchaseOrderReceivingStatus.MOBILEPHONE :	//"0001": Cellphone  
                    case PurchaseOrderReceivingStatus.APPLIANCES :	//"0002": Appliances 
                        lsBarcode = Detail(lnCtr).Inventory().Brand().getDescription();

                        if(Detail(lnCtr).Inventory().Model().getDescription() != null && !"".equals(Detail(lnCtr).Inventory().Model().getDescription())){
                            lsDescription = Detail(lnCtr).Inventory().Model().getDescription();
                        }
                        if(Detail(lnCtr).Inventory().Variant().getDescription() != null && !"".equals(Detail(lnCtr).Inventory().Variant().getDescription())){
                            lsDescription = lsDescription + " " + Detail(lnCtr).Inventory().Variant().getDescription();
                        }
                        if(Detail(lnCtr).Inventory().Variant().getYearModel()!= 0){
                            lsDescription = lsDescription + " " + Detail(lnCtr).Inventory().Variant().getYearModel();
                        }
                        if(Detail(lnCtr).Inventory().Color().getDescription() != null && !"".equals(Detail(lnCtr).Inventory().Color().getDescription())){
                            lsDescription = lsDescription + " " + Detail(lnCtr).Inventory().Color().getDescription();
                        }
                        
                        if(!"".equals(lsSerial)){
                            lsDescription = lsDescription + "\n" + lsSerial;
                        }
                        orderDetails.add(new OrderDetail(lnRow, String.valueOf(Detail(lnCtr).getOrderNo()), 
                                lsBarcode, lsDescription, Detail(lnCtr).getUnitPrce().doubleValue(), Detail(lnCtr).getQuantity().doubleValue(), lnTotal));
                    break;
                    case PurchaseOrderReceivingStatus.FOOD : //"0008": // Food  
                        lsBarcode = Detail(lnCtr).Inventory().getBarCode();
                        if (Detail(lnCtr).Inventory().Measure().getDescription() != null && !"".equals(Detail(lnCtr).Inventory().Measure().getDescription())){
                            lsMeasure = Detail(lnCtr).Inventory().Measure().getDescription();
                        }
                        lsDescription = Detail(lnCtr).Inventory().Brand().getDescription() 
                                + " " + Detail(lnCtr).Inventory().getDescription(); 
                        orderDetails.add(new OrderDetail(lnRow, String.valueOf(Detail(lnCtr).getOrderNo()), 
                                lsBarcode, lsDescription, lsMeasure ,Detail(lnCtr).getUnitPrce().doubleValue(), Detail(lnCtr).getQuantity().doubleValue(), lnTotal));
                        jrxmlPath = System.getProperty("sys.default.path.config") + "/Reports/PurchaseOrderReceiving_Food.jrxml";
                        if(Master().getPurpose().equals(PurchaseOrderReceivingStatus.Purpose.REPLACEMENT)){
                            jrxmlPath = System.getProperty("sys.default.path.config") + "/Reports/PurchaseOrderReplacement_Food.jrxml";
                        }
                    break;
                    case PurchaseOrderReceivingStatus.SPCAR :       //case "0006":  CAR SP       
                    case PurchaseOrderReceivingStatus.SPMC  :       //case "0004":  Motorcycle SP
                    case PurchaseOrderReceivingStatus.GENERAL :     //case "0007":  General      
                    case PurchaseOrderReceivingStatus.HOSPITALITY : //case "0009":  Hospitality                          
                    default:
                        lsBarcode = Detail(lnCtr).Inventory().getBarCode();
                        lsDescription = Detail(lnCtr).Inventory().getDescription();   
                        orderDetails.add(new OrderDetail(lnRow, String.valueOf(Detail(lnCtr).getOrderNo()), 
                                lsBarcode, lsDescription, Detail(lnCtr).getUnitPrce().doubleValue(), Detail(lnCtr).getQuantity().doubleValue(), lnTotal));
                    break;
                }
                
                lnRow++;
                lsDescription = "";
                lsBarcode = "";
                lsSerial = "";
            }
            
            File file = new File(jrxmlPath);

            if (file.exists()) {
            } else {
                poJSON.put("result", "error");
                poJSON.put("message", "Jasper file does not exist. \nEnsure the file is located in \"D:\\GGC_Maven_Systems\\reports\"");
                poViewer = null;
                onPrintedCallback.run();
                return poJSON;
            }

            // 3. Create data source
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(orderDetails);

            // 4. Compile and fill report
            JasperReport jasperReport = JasperCompileManager.compileReport(jrxmlPath);
            JasperPrint jasperPrint = JasperFillManager.fillReport(
                    jasperReport,
                    parameters,
                    dataSource
            );

            if (poViewer != null && poViewer.isDisplayable()) {
                poViewer.dispose();
                poViewer = null;

            }
            poViewer = new CustomJasperViewer(jasperPrint, onPrintedCallback);
            poViewer.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE); 
            poViewer.setVisible(true);
            poViewer.toFront();
            
            poViewer.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    poViewer = null;
                    System.out.println("Jasper viewer is closing...");
                }

                @Override
                public void windowClosed(java.awt.event.WindowEvent e) {
                    System.out.println("Jasper viewer closed.");
                    onPrintedCallback.run(); 
                }
            });
            
        } catch (JRException e) {
            System.err.println("Error generating report: " + e.getMessage());
            e.printStackTrace();
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(e));
        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(PurchaseOrderReceiving.class.getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
        } catch (CloneNotSupportedException ex) {
            Logger.getLogger(PurchaseOrderReceiving.class.getName()).log(Level.SEVERE, null, ex);
        }

        return poJSON;
    }

    public static class OrderDetail {

        private Integer nRowNo;
        private String sOrderNo;
        private String sBarcode;
        private String sDescription;
        private String sMeasure;
        private double nUprice;
        private double nOrder;
        private double nTotal;

        public OrderDetail(Integer rowNo, String orderNo, String barcode, String description,
                double uprice, double order, double total) {
            this.nRowNo = rowNo;
            this.sOrderNo = orderNo;
            this.sBarcode = barcode;
            this.sDescription = description;
            this.nUprice = uprice;
            this.nOrder = order;
            this.nTotal = total;
        }
        
        public OrderDetail(Integer rowNo, String orderNo, String barcode, String description, String measure,
                double uprice, double order, double total) {
            this.nRowNo = rowNo;
            this.sOrderNo = orderNo;
            this.sBarcode = barcode;
            this.sDescription = description;
            this.sMeasure = measure;
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

        public String getsMeasure() {
            return sMeasure;
        }

        public double getnUprice() {
            return nUprice;
        }

        public double getnOrder() {
            return nOrder;
        }

        public double getnTotal() {
            return nTotal;
        }
    }

    private class CustomJasperViewer extends JasperViewer {

        private final Runnable onPrintedCallback;

        public CustomJasperViewer(JasperPrint jasperPrint, Runnable onPrintedCallback) {
            super(jasperPrint, false);
            this.onPrintedCallback = onPrintedCallback;
            customizePrintButton(jasperPrint);

            
        }

        private void customizePrintButton(JasperPrint jasperPrint) {
            poJSON = new JSONObject();
            try {
                JRViewer viewer = findJRViewer(this);
                if (viewer == null) {
                    System.out.println("JRViewer not found!");
                    return;
                }

                for (int i = 0; i < viewer.getComponentCount(); i++) {
                    if (viewer.getComponent(i) instanceof JRViewerToolbar) {
                        JRViewerToolbar toolbar = (JRViewerToolbar) viewer.getComponent(i);

                        for (int j = 0; j < toolbar.getComponentCount(); j++) {
                            if (toolbar.getComponent(j) instanceof JButton) {
                                JButton button = (JButton) toolbar.getComponent(j);

                                if (button.getToolTipText() != null) {
                                    if (button.getToolTipText().equals("Save")) {
                                        button.setEnabled(false);  // Disable instead of hiding
                                        button.setVisible(false);  // Hide it completely
                                    }
                                }

                                if ("Print".equals(button.getToolTipText())) {
                                    for (ActionListener al : button.getActionListeners()) {
                                        button.removeActionListener(al);
                                    }
                                    button.addActionListener(e -> {
                                        try {
                                            boolean isPrinted = JasperPrintManager.printReport(jasperPrint, true);
                                            if (isPrinted) {
                                                PrintTransaction(true);
                                            } else {
                                                Platform.runLater(() -> {
                                                    ShowMessageFX.Warning(null, "Computerized Accounting System", "Printing was canceled by the user.");
                                                    SwingUtilities.invokeLater(() -> CustomJasperViewer.this.toFront());

                                                });
                                            }
                                        } catch (JRException ex) {
                                            Platform.runLater(() -> {
                                                ShowMessageFX.Warning(null, "Computerized Accounting System", "Print Failed: " + ex.getMessage());
                                                SwingUtilities.invokeLater(() -> CustomJasperViewer.this.toFront());
                                            });
                                        } catch (SQLException | GuanzonException | CloneNotSupportedException ex) {
                                            Logger.getLogger(PurchaseOrder.class.getName()).log(Level.SEVERE, null, ex);
                                        }
                                    });
                                }
                            }
                        }
                        // Force UI refresh after hiding the button
                        toolbar.revalidate();
                        toolbar.repaint();
                    }
                }
            } catch (Exception e) {
                System.out.println("Error customizing print button: " + e.getMessage());
            }
        }

        private void PrintTransaction(boolean fbIsPrinted)
                throws SQLException,
                CloneNotSupportedException,
                GuanzonException {
            poJSON = new JSONObject();
            if (fbIsPrinted) {
//                poJSON = OpenTransaction((String) poMaster.getValue("sTransNox"));
                poJSON = OpenTransaction(psTransactionNo);
                if ("error".equals((String) poJSON.get("result"))) {
                    Platform.runLater(() -> {
                        ShowMessageFX.Warning(null, "Print Purchase Order Receiving", "Printing of the transaction was aborted.\n" + (String) poJSON.get("message"));
                        SwingUtilities.invokeLater(() -> CustomJasperViewer.this.toFront());
                    });
                    fbIsPrinted = false;
                }
                
                if (PurchaseOrderReceivingStatus.CONFIRMED.equals(Master().getTransactionStatus())
                        || PurchaseOrderReceivingStatus.POSTED.equals(Master().getTransactionStatus())
                        || PurchaseOrderReceivingStatus.PAID.equals(Master().getTransactionStatus())) {
                    poJSON = UpdateTransaction();
                    if ("error".equals((String) poJSON.get("result"))) {
                        Platform.runLater(() -> {
                            ShowMessageFX.Warning(null, "Print Purchase Order Receiving", "Printing of the transaction was aborted.\n" + (String) poJSON.get("message"));
                            SwingUtilities.invokeLater(() -> CustomJasperViewer.this.toFront());
                        });
                        fbIsPrinted = false;
                    }
                     //Populate purchase receiving serials
                    for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
                        getPurchaseOrderReceivingSerial(Detail(lnCtr).getEntryNo());
                    }
                    poMaster.setValue("dModified", poGRider.getServerDate());
                    poMaster.setValue("sModified", poGRider.getUserID());
                    poMaster.setValue("cPrintxxx", Logical.YES);
                    pbIsPrint = fbIsPrinted;
                    poJSON = SaveTransaction();
                    if ("error".equals((String) poJSON.get("result"))) {
                        Platform.runLater(() -> {
                            ShowMessageFX.Warning(null, "Print Purchase Order Receiving", "Printing of the transaction was aborted.\n" + (String) poJSON.get("message"));
                            SwingUtilities.invokeLater(() -> CustomJasperViewer.this.toFront());
                        });
                        fbIsPrinted = false;
                    }

                    pbIsPrint = false;
                }
            }

            if (fbIsPrinted) {
                Platform.runLater(() -> {
                    ShowMessageFX.Information(null, "Print Purchase Order Receiving", "Transaction printed successfully.");
                });
            }

            if (onPrintedCallback != null) {
                poViewer = null;
                this.dispose();
//                onPrintedCallback.run();  // <- triggers controller method!
            }
            SwingUtilities.invokeLater(() -> CustomJasperViewer.this.toFront());
        }

        private JRViewer findJRViewer(Component parent) {
            if (parent instanceof JRViewer) {
                return (JRViewer) parent;
            }
            if (parent instanceof Container) {
                Component[] components = ((Container) parent).getComponents();
                for (Component component : components) {
                    JRViewer viewer = findJRViewer(component);
                    if (viewer != null) {
                        return viewer;
                    }
                }
            }
            return null;
        }
    }
    
    public JSONObject searchBarcodePORDetail(String value, String transactionNo) throws SQLException, GuanzonException{
        return searchPORDetail(value,transactionNo,"barcode");
    }
    
    public JSONObject searchDescriptionPORDetail(String value, String transactionNo) throws SQLException, GuanzonException{
        return searchPORDetail(value,transactionNo,"description");
    }
    
    public JSONObject searchImeiPORDetail(String value, String transactionNo) throws SQLException, GuanzonException{
        return searchPORDetail(value,transactionNo,"imei");
    }
    
    public JSONObject searchEnginePORDetail(String value, String transactionNo) throws SQLException, GuanzonException{
        return searchPORDetail(value,transactionNo,"engine");
    }
    
    public JSONObject searchFramePORDetail(String value, String transactionNo) throws SQLException, GuanzonException{
        return searchPORDetail(value,transactionNo,"frame");
    }
    
    public JSONObject searchPlatePORDetail(String value, String transactionNo) throws SQLException, GuanzonException{
        return searchPORDetail(value,transactionNo,"plateno");
    }
    
    public JSONObject searchConductionStickerNoPORDetail(String value, String transactionNo) throws SQLException, GuanzonException{
        return searchPORDetail(value,transactionNo,"csno");
    }
    
    private JSONObject searchPORDetail(String value, String transactionNo, String searchType) throws SQLException, GuanzonException{
        poJSON = new JSONObject();
        int lnSort = 0;
        String lsHeader = "Barcode»Description";
        String lsColName = "sBarCodex»sDescript";
        String lsColCriteria = "a.sBarCodex»a.sDescript";
        String lsTransStat = "";
        String lsSQL = getSQ_BrowseInvSerial();
        
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
        
        
        lsSQL = MiscUtil.addCondition(lsSQL, " a.sTransNox = " + SQLUtil.toSQL(transactionNo));
        if (psTranStat != null && !"".equals(psTranStat)) {
            lsSQL = lsSQL + lsTransStat;
        }
        lsSQL = lsSQL + " GROUP BY a.sStockIDx, i.sSerialID ";
        
        switch(searchType){
            case "engine":
            case "frame":
            case "csno":
            case "plateno":
                lsHeader = "Engine No»Frame No»Plate No»CS No»Description";
                lsColName = "sSerial01»sSerial02»sPlateNoP»sCStckrNo»sDescript";
                lsColCriteria = "j.sSerial01»j.sSerial02»IFNULL(k.sPlateNoP, '')»IFNULL(k.sCStckrNo, '')»b.sDescript";
                
                switch(searchType){
                    case "frame":
                        lnSort = 1;
                    break;
                    case "csno":
                        lnSort = 2;
                    break;
                    case "plateno":
                        lnSort = 3;
                    break;
                    default:
                        lnSort = 0;
                    break;
                }
            break;
            case "imei":
                lsHeader = "IMEI 1»IMEI 2»Barcode»Brand»Description";
                lsColName = "sSerial01»sSerial02»sBarCodex»xBrandNme»sDescript";
                lsColCriteria = "j.sSerial01»j.sSerial02»b.sBarCodex»IFNULL(c.sDescript, '')»b.sDescript";
                lnSort = 0;
            break;
            case "description":
                if("01".equals(psIndustryId) || "07".equals(psIndustryId)){ //Mobile Phone / Appliances
                    lsHeader = "IMEI 1»IMEI 2»Barcode»Brand»Description";
                    lsColName = "sSerial01»sSerial02»sBarCodex»xBrandNme»sDescript";
                    lsColCriteria = "j.sSerial01»j.sSerial02»b.sBarCodex»IFNULL(c.sDescript, '')»b.sDescript";
                    lnSort = 4;
                } else {
                    lsHeader = "Barcode»Description»Supersede";
                    lsColName = "sBarCodex»sDescript»xSupersde";
                    lsColCriteria = "b.sBarCodex»b.sDescript»bb.sBarCodex";
                    lnSort = 1;
                }
            break;
            case "barcode":
                if("01".equals(psIndustryId) || "07".equals(psIndustryId)){ //Mobile Phone / Appliances
                    lsHeader = "IMEI 1»IMEI 2»Barcode»Brand»Description";
                    lsColName = "sSerial01»sSerial02»sBarCodex»xBrandNme»sDescript";
                    lsColCriteria = "j.sSerial01»j.sSerial02»b.sBarCodex»IFNULL(c.sDescript, '')»b.sDescript";
                    lnSort = 2;
                } else {
                    lsHeader = "Barcode»Description»Supersede";
                    lsColName = "sBarCodex»sDescript»xSupersde";
                    lsColCriteria = "b.sBarCodex»b.sDescript»bb.sBarCodex";
                    lnSort = 0;
                }
            break;
        }    
        
        System.out.println("Executing SQL: " + lsSQL);
        poJSON = ShowDialogFX.Browse(poGRider,
                lsSQL,
                value,
                lsHeader,
                lsColName,
                lsColCriteria,
                lnSort);

        if (poJSON != null) {
            System.out.println("Stock ID : " + (String) poJSON.get("sStockIDx"));
            System.out.println("Spersede ID : " + (String) poJSON.get("sReplacID"));
//            if((String) poJSON.get("sReplacID") != null && !"".equals((String) poJSON.get("sReplacID"))){
//                poJSON.put("sStockIDx", (String) poJSON.get("sReplacID"));
//            }
            
//            if((String) poJSON.get("sSerialID") != null && !"".equals((String) poJSON.get("sSerialID"))){
//                return getSerial().openRecord(transactionNo, (String) poJSON.get("sSerialID"));
//            } else {
//                return getDetail().openRecord(transactionNo, (String) poJSON.get("sStockIDx"));
//            }
        } else {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No record loaded.");
            return poJSON;
        }
        
        System.out.println(poJSON.clone());
        poJSON.put("result", "success");
        return poJSON;
        
    }
    
    private String getSQ_BrowseInv(){
        return    " SELECT DISTINCT "                                                                  
                + "   a.sStockIDx, "                                                                   
                + "   b.sBarCodex, "                                                                   
                + "   b.sDescript, "                                                                   
                + "   IFNULL(c.sDescript, '') xBrandNme, "                                             
                + "   IFNULL(d.sDescript, '') xModelNme, "                                             
                + "   IFNULL(e.sDescript, '') xColorNme, "                                             
                + "   IFNULL(f.sDescript, '') xMeasurNm, "                                             
                + "   TRIM(CONCAT(IFNULL(g.sDescript, ''), ' ', IFNULL(g.nYearMdlx, ''))) xVrntName, " 
                + "   IFNULL(d.sModelCde, '') xModelCde "                                              
                + " FROM po_receiving_detail a "                                                       
                + " LEFT JOIN Inventory b            "                                                 
                + "     ON b.sStockIDx = a.sStockIDx "                                                 
                + " LEFT JOIN Brand c                "                                                 
                + "     ON b.sBrandIDx = c.sBrandIDx "                                                 
                + " LEFT JOIN Model d                "                                                 
                + "     ON b.sModelIDx = d.sModelIDx "                                                 
                + " LEFT JOIN Color e                "                                                 
                + "     ON b.sColorIDx = e.sColorIDx "                                                 
                + " LEFT JOIN Measure f              "                                                 
                + "     ON b.sMeasurID = f.sMeasurID "                                                 
                + " LEFT JOIN Model_Variant g        "                                                 
                + "     ON b.sVrntIDxx = g.sVrntIDxx "                                                 
                + " LEFT JOIN Inv_Supplier h         "                                                 
                + "     ON b.sStockIDx = h.sStockIDx "  ;                                               
    }
    
    private String getSQ_BrowseInvSerial(){
        return    " SELECT "                                                                             
                + "   a.sStockIDx, "                                                                     
                + "   a.sReplacID, "                                                                     
                + "   i.sSerialID, "                                                                     
                + "   j.sSerial01, "                                                                     
                + "   j.sSerial02, "                                                                     
                + "   k.sPlateNoP, "                                                                     
                + "   k.sCStckrNo, "                                                                     
                + "   b.sBarCodex, "                                                                     
                + "   bb.sBarCodex as xSupersde, "                                                                     
                + "   b.sDescript, "                                                                     
                + "   b.sCategCd1, "                                                                     
                + "   IFNULL(c.sDescript, '')    xBrandNme, "                                            
                + "   IFNULL(d.sDescript, '')    xModelNme, "                                            
                + "   IFNULL(e.sDescript, '')    xColorNme, "                                            
                + "   IFNULL(f.sDescript, '')    xMeasurNm, "                                            
                + "   TRIM(CONCAT(IFNULL(g.sDescript, ''), ' ', IFNULL(g.nYearMdlx, '')))    xVrntName, "
                + "   IFNULL(d.sModelCde, '')    xModelCde "                                              
                + "   FROM po_receiving_detail a "                                                         
                + "   LEFT JOIN Inventory b    "                                                         
                + "     ON b.sStockIDx = a.sStockIDx  "                                                      
                + "   LEFT JOIN Inventory bb    "                                                         
                + "     ON bb.sStockIDx = a.sReplacID  "                                                 
                + "   LEFT JOIN Brand c               "                                                  
                + "     ON b.sBrandIDx = c.sBrandIDx  "                                                  
                + "   LEFT JOIN Model d               "                                                  
                + "     ON b.sModelIDx = d.sModelIDx  "                                                  
                + "   LEFT JOIN Color e               "                                                  
                + "     ON b.sColorIDx = e.sColorIDx  "                                                  
                + "   LEFT JOIN Measure f             "                                                  
                + "     ON b.sMeasurID = f.sMeasurID  "                                                  
                + "   LEFT JOIN Model_Variant g       "                                                  
                + "     ON b.sVrntIDxx = g.sVrntIDxx  "                                                  
                + "   LEFT JOIN Inv_Supplier h        "                                                  
                + "     ON b.sStockIDx = h.sStockIDx  "                                                  
                + "   LEFT JOIN po_receiving_serial i "                                                  
                + "     ON i.sStockIDx = a.sStockIDx AND i.sTransNox = a.sTransNox "                     
                + "   LEFT JOIN inv_serial j "                                                           
                + "     ON j.sSerialID = i.sSerialID "                                                   
                + "   LEFT JOIN inv_serial_registration k "                                              
                + "     ON k.sSerialID = i.sSerialID "   ;
    }
    
    private String getPOReturnSerial(){
        return   " SELECT "
            + "   a.sTransNox  "
            + "  , a.sSerialID "
            + "  , b.sIndstCdx "
            + "  , b.sSerial01 "
            + "  , b.sSerial02 "
            + "  , b.sStockIDx "
            + "  , c.sDescript "
            + "  , d.sCStckrNo "  
            + "  , d.sPlateNoP "  
            + " FROM po_return_detail a "
            + " LEFT JOIN inv_serial b ON a.sSerialID = b.sSerialID "
            + " LEFT JOIN inventory c ON c.sStockIDx = b.sStockIDx "
            + " LEFT JOIN inv_serial_registration d ON d.sSerialID = a.sSerialID ";
    }
    
    private String getInvSerial(){
        return   " SELECT "                                              
            + "    a.sSerialID "                                       
            + "  , a.sSerial01 "                                       
            + "  , a.sSerial02 "                                       
            + "  , a.nUnitPrce "                                       
            + "  , a.sStockIDx "                                       
            + "  , a.cLocation "                                       
            + "  , a.cSoldStat "                                       
            + "  , a.cUnitType "                                       
            + "  , a.sCompnyID " 
            + "  , a.sIndstCdx "                                      
            + "  , a.sWarranty " 
            + "  , b.sDescript "  
            + "  , c.sCStckrNo "  
            + "  , c.sPlateNoP "                                          
            + " FROM Inv_Serial a "                                  
            + " LEFT JOIN Inventory b ON b.sStockIDx = a.sStockIDx "
            + " LEFT JOIN inv_serial_registration c ON c.sSerialID = a.sSerialID ";
    }
    
    
    private String getSerialPORecieving() {
        return    " SELECT "                                                      
                + "   a.sTransNox "                                                
                + " , a.dTransact "
                + " , a.sIndstCdx "                                                
                + " , a.sReferNox "                                               
                + " , a.cTranStat "                                               
                + " , b.sSerialID "                                                
                + " , c.sOrderNox "                                                
                + " , d.sSerial01 "                                                
                + " , d.sSerial02 "                                              
                + " FROM po_receiving_master a "                                  
                + " LEFT JOIN po_receiving_serial b ON b.sTransNox = a.sTransNox "
                + " LEFT JOIN po_receiving_detail c ON c.sTransNox = a.sTransNox "
                + " LEFT JOIN inv_serial d ON d.sSerialID = b.sSerialID ";
    }
    
//    private String getSQ_BrowseInvSerial(){
//        return    " SELECT DISTINCT "                                                                  
//                + "   a.sSerialID,  "                                                                  
//                + "   j.sSerial01,  "                                                                  
//                + "   j.sSerial02,  "                                                                  
//                + "   k.sPlateNoP,  "                                                                  
//                + "   k.sCStckrNo,  "                                                                  
//                + "   a.sStockIDx,  "                                                                  
//                + "   b.sBarCodex,  "                                                                  
//                + "   b.sDescript,  "                                                                  
//                + "   b.sCategCd1,  "                                                                  
//                + "   IFNULL(c.sDescript, '') xBrandNme, "                                             
//                + "   IFNULL(d.sDescript, '') xModelNme, "                                             
//                + "   IFNULL(e.sDescript, '') xColorNme, "                                             
//                + "   IFNULL(f.sDescript, '') xMeasurNm, "                                             
//                + "   TRIM(CONCAT(IFNULL(g.sDescript, ''), ' ', IFNULL(g.nYearMdlx, ''))) xVrntName, " 
//                + "   IFNULL(d.sModelCde, '') xModelCde "                                              
//                + " FROM po_receiving_serial a  "                                                      
//                + " LEFT JOIN Inventory b       "                                                      
//                + "     ON b.sStockIDx = a.sStockIDx    "                                              
//                + " LEFT JOIN Brand c                   "                                              
//                + "     ON b.sBrandIDx = c.sBrandIDx    "                                              
//                + " LEFT JOIN Model d                   "                                              
//                + "     ON b.sModelIDx = d.sModelIDx    "                                              
//                + " LEFT JOIN Color e                   "                                              
//                + "     ON b.sColorIDx = e.sColorIDx    "                                              
//                + " LEFT JOIN Measure f                 "                                              
//                + "     ON b.sMeasurID = f.sMeasurID    "                                              
//                + " LEFT JOIN Model_Variant g           "                                              
//                + "     ON b.sVrntIDxx = g.sVrntIDxx    "                                              
//                + " LEFT JOIN Inv_Supplier h            "                                              
//                + "     ON b.sStockIDx = h.sStockIDx    "                                              
//                + " INNER JOIN po_receiving_detail i     "                                              
//                + "     ON i.sStockIDx = a.sStockIDx AND i.sTransNox = a.sTransNox   "                                              
//                + " INNER JOIN inv_serial j              "                                              
//                + "     ON j.sSerialID = a.sSerialID    "                                              
//                + " LEFT JOIN inv_serial_registration k "                                              
//                + "    ON k.sSerialID = a.sSerialID     "   ;
//    }
    
}
