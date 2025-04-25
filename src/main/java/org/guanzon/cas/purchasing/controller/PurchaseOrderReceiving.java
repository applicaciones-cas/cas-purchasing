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
import javafx.stage.WindowEvent;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
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
import org.guanzon.appdriver.iface.GValidator;
import org.guanzon.cas.client.Client;
import org.guanzon.cas.client.services.ClientControllers;
import org.guanzon.cas.inv.InvSerial;
import org.guanzon.cas.inv.Inventory;
import org.guanzon.cas.inv.InventoryTransaction;
import org.guanzon.cas.inv.services.InvControllers;
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
import org.guanzon.cas.purchasing.services.PurchaseOrderReceivingControllers;
import org.guanzon.cas.purchasing.services.PurchaseOrderReceivingModels;
import org.guanzon.cas.purchasing.status.PurchaseOrderReceivingStatus;
import org.guanzon.cas.purchasing.status.PurchaseOrderStatus;
import org.guanzon.cas.purchasing.validator.PurchaseOrderReceivingValidatorFactory;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

/**
 *
 * @author Arsiela 03-12-2025
 */
public class PurchaseOrderReceiving extends Transaction {

    private boolean pbApproval = false;
    private boolean pbIsPrint = false;
    private String psIndustryId = "";
    private String psCompanyId = "";
    private String psCategorCd = "";

    List<Model_PO_Master> paPOMaster;
    List<Model_POR_Master> paPORMaster;
    List<Model_POR_Serial> paOthers;
    List<PurchaseOrder> paPurchaseOrder;
    List<InventoryTransaction> paInventoryTransaction;
    List<TransactionAttachment> paAttachments;
    List<Model> paDetailRemoved;

    public JSONObject InitTransaction() {
        SOURCE_CODE = "POR";

        poMaster = new PurchaseOrderReceivingModels(poGRider).PurchaseOrderReceivingMaster();
        poDetail = new PurchaseOrderReceivingModels(poGRider).PurchaseOrderReceivingDetails();

        paPORMaster = new ArrayList<>();
        paOthers = new ArrayList<>();
        paDetail = new ArrayList<>();
        paDetailRemoved = new ArrayList<>();
        paAttachments = new ArrayList<>();
        paPOMaster = new ArrayList<>();
        paPurchaseOrder = new ArrayList<>();
        paInventoryTransaction = new ArrayList<>();

        psCompanyId = getCompanyId();
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
            resetOthers();
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

        //Update Purchase Order, Inventory, Serial Ledger
        poJSON = saveUpdateOthers(PurchaseOrderReceivingStatus.CONFIRMED);
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
        
        if (PurchaseOrderReceivingStatus.CONFIRMED.equals(Master().getTransactionStatus())) {
            //Update Purchase Order, Inventory, Serial Ledger
            poJSON = saveUpdateOthers(PurchaseOrderReceivingStatus.CONFIRMED);
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

    public JSONObject ApproveTransaction(String remarks)
            throws ParseException,
            SQLException,
            GuanzonException,
            CloneNotSupportedException {
//        poJSON = new JSONObject();
//
//        String lsStatus = PurchaseOrderReceivingStatus.APPROVED;
//        boolean lbApprove = true;
//
//        if (getEditMode() != EditMode.READY) {
//            poJSON.put("result", "error");
//            poJSON.put("message", "No transacton was loaded.");
//            return poJSON;
//        }
//
//        if (lsStatus.equals((String) poMaster.getValue("cTranStat"))) {
//            poJSON.put("result", "error");
//            poJSON.put("message", "Transaction was already approved.");
//            return poJSON;
//        }
//
//        //validator
//        poJSON = isEntryOkay(PurchaseOrderReceivingStatus.APPROVED);
//        if (!"success".equals((String) poJSON.get("result"))) {
//            return poJSON;
//        }
//
//        //Update purchase order
//        poJSON = setValueToOthers(lsStatus);
//        if (!"success".equals((String) poJSON.get("result"))) {
//            return poJSON;
//        }
//
//        if (pbApproval) {
//            poJSON = ShowDialogFX.getUserApproval(poGRider);
//            if (!"success".equals((String) poJSON.get("result"))) {
//                return poJSON;
//            }
//        }
//
//        poGRider.beginTrans("UPDATE STATUS", "ApproveTransaction", SOURCE_CODE, Master().getTransactionNo());
//
//        //change status
//        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbApprove, true);
//        if (!"success".equals((String) poJSON.get("result"))) {
//            poGRider.rollbackTrans();
//            return poJSON;
//        }
//
//        //Update Purchase Order, Serial Ledger, Inventory
//        poJSON = saveUpdateOthers(PurchaseOrderReceivingStatus.APPROVED);
//        if (!"success".equals((String) poJSON.get("result"))) {
//            poGRider.rollbackTrans();
//            return poJSON;
//        }
//
//        poGRider.commitTrans();
//
//        poJSON = new JSONObject();
//        poJSON.put("result", "success");
//        if (lbApprove) {
//            poJSON.put("message", "Transaction approved successfully.");
//        } else {
//            poJSON.put("message", "Transaction approved request submitted successfully.");
//        }
//
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

        if (pbApproval) {
            poJSON = ShowDialogFX.getUserApproval(poGRider);
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
        }

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
        poJSON = isEntryOkay(PurchaseOrderReceivingStatus.POSTED);
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
        
        if (PurchaseOrderReceivingStatus.CONFIRMED.equals(Master().getTransactionStatus())) {
            //Update Purchase Order, Serial Ledger, Inventory
            poJSON = saveUpdateOthers(PurchaseOrderReceivingStatus.CONFIRMED);
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
        
        if (PurchaseOrderReceivingStatus.CONFIRMED.equals(Master().getTransactionStatus())) {
            //Update Purchase Order, Serial Ledger, Inventory
            poJSON = saveUpdateOthers(PurchaseOrderReceivingStatus.CONFIRMED);
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

    public void setIndustryId(String industryId) {
        psIndustryId = industryId;
    }

    public void setCompanyId(String companyId) {
        psCompanyId = companyId;
    }

    public void setCategoryId(String categoryId) {
        psCategorCd = categoryId;
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

    public Model_POR_Detail DetailRemove(int row) {
        return (Model_POR_Detail) paDetailRemoved.get(row);
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
        Inventory object = new InvControllers(poGRider, logwrapr).Inventory();
        object.setRecordStatus(RecordStatus.ACTIVE);
        
        if(Master().getSupplierId() == null || "".equals(Master().getSupplierId())){
            poJSON.put("result", "error");
            poJSON.put("message", "Supplier is not set.");
            return poJSON;
        }
        
//        poJSON = object.searchRecord(value, byCode);
        poJSON = object.searchRecord(value, byCode, Master().getSupplierId(),null, Master().getIndustryId(),  Master().getCategoryCode());
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
        
        if(Master().getSupplierId() == null || "".equals(Master().getSupplierId())){
            poJSON.put("result", "error");
            poJSON.put("message", "Supplier is not set.");
            return poJSON;
        }
        
        Inventory object = new InvControllers(poGRider, logwrapr).Inventory();
        object.setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode, Master().getSupplierId(),null, Master().getIndustryId(),  Master().getCategoryCode());
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
        
        Inventory object = new InvControllers(poGRider, logwrapr).Inventory();
        object.setRecordStatus(RecordStatus.ACTIVE);

//        poJSON = object.searchRecord(value, byCode); //TODO
        poJSON = object.searchRecord(value, byCode, Master().getSupplierId(),null, Master().getIndustryId(),  Master().getCategoryCode());
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
    public JSONObject SearchSerial(String value, boolean byCode, int row)
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();

//        InvSerial object = new InvControllers(poGRider, logwrapr).InventorySerial();
//        object.getModel().setRecordStatus(RecordStatus.ACTIVE);
//        
////        poJSON = object.searchRecord(value, byCode, PurchaseOrderReceivingSerialList(row).getStockId());
//        if ("success".equals((String) poJSON.get("result"))) {
//            PurchaseOrderReceivingSerialList(row).setSerialId(object.getModel().getSerialId());
//            PurchaseOrderReceivingSerialList(row).setSerial01(object.getModel().getSerial01());
//            PurchaseOrderReceivingSerialList(row).setSerial02(object.getModel().getSerial02());
//            PurchaseOrderReceivingSerialList(row).setConductionStickerNo(object.SerialRegistration().getConductionStickerNo());
//            PurchaseOrderReceivingSerialList(row).setPlateNo(object.SerialRegistration().getPlateNoP());
//        }
        return poJSON;
    }

    public JSONObject computeFields()
            throws SQLException,
            GuanzonException {
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
        }
        
        Master().setTransactionTotal(ldblTotal);
        
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

    public JSONObject computeDiscountRate(double discount) {
        poJSON = new JSONObject();
        Double ldblTotal = 0.00;
        Double ldblDiscRate = 0.00;

        for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
            ldblTotal += (Detail(lnCtr).getUnitPrce().doubleValue() * Detail(lnCtr).getQuantity().intValue());
        }
        System.out.println("total : " + ldblTotal);
        if (discount < 0 || discount > ldblTotal) {
            Master().setDiscount(0.00);
            computeDiscountRate(0.00);
            poJSON.put("result", "error");
            poJSON.put("message", "Discount amount cannot be negative or exceed the transaction total.");
        } else {
            poJSON.put("result", "success");
            poJSON.put("message", "success");
            ldblDiscRate = (discount / ldblTotal) * 100;
            Master().setDiscountRate(ldblDiscRate);
        }
        return poJSON;
    }

    public JSONObject computeDiscount(double discountRate) {
        poJSON = new JSONObject();
        Double ldblTotal = 0.00;
        Double ldblDiscount = 0.00;

        for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
            ldblTotal += (Detail(lnCtr).getUnitPrce().doubleValue() * Detail(lnCtr).getQuantity().intValue());
        }

        if (discountRate < 0 || discountRate > 100.00) {
            Master().setDiscountRate(0.00);
            computeDiscount(0.00);
            poJSON.put("result", "error");
            poJSON.put("message", "Discount rate cannot be negative or exceed 100.00");
        } else {
            poJSON.put("result", "success");
            poJSON.put("message", "success");
            ldblDiscount = ldblTotal * (discountRate / 100.00);
            Master().setDiscount(ldblDiscount);
        }

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
        for(int lnRow = 0; lnRow <= getDetailCount() - 1; lnRow++){
            if(lnRow != row ){
                if  (("".equals(Detail(lnRow).getOrderNo()) || Detail(lnRow).getOrderNo() == null) &&
                    (stockId.equals(Detail(lnRow).getStockId()))) {
                    poJSON.put("result", "error");
                    poJSON.put("message", description+ " already exist in table at row " + (lnRow+1) + ".");
                    poJSON.put("row", lnRow);
                    System.out.println("json row : " + poJSON.get("row"));
                    return poJSON;
                } 
            }
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
//                            //Do not allow same stock Id without expiry date that already exist in po receiving detail
//                            if ("1900-01-01".equals(xsDateShort(Detail(lnRow).getExpiryDate()))) {
//                                poJSON.put("result", "error");
//                                poJSON.put("message", barcode + " already exist in table at row " + (lnRow + 1) + ".");
//                                return poJSON;
//                            } else {
//                                //Do not allow same stock Id with the same expiry date
//                                if (expiryDate.equals(xsDateShort(Detail(lnRow).getExpiryDate()))) {
//                                    poJSON.put("result", "error");
//                                    poJSON.put("message", barcode + " already exist in table at row " + (lnRow + 1) + ".");
//                                    Detail().remove(row);
//                                    return poJSON;
//                                }
//                            }
//                        }
////                    }
//                }
//            }
//        }
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
            initSQL();
            String lsSQL = MiscUtil.addCondition(SQL_BROWSE, " a.sIndstCdx = " + SQLUtil.toSQL(psIndustryId)
                    + " AND a.sCompnyID = " + SQLUtil.toSQL(companyId)
                    + " AND a.sBranchCd = " + SQLUtil.toSQL(poGRider.getBranchCode())
                    + " AND a.sCategrCd = "+ SQLUtil.toSQL(psCategorCd)
                    + " AND a.sSupplier LIKE " + SQLUtil.toSQL("%" + supplierId)
                    + " AND a.sTransNox LIKE " + SQLUtil.toSQL("%" + referenceNo)
            );
            switch (formName) {
//                case "approval":
//                    lsSQL = lsSQL + " AND a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderReceivingStatus.CONFIRMED);
//                    break;
                case "confirmation":
                    lsSQL = lsSQL + " AND ( a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderReceivingStatus.OPEN)
                                  + " OR a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderReceivingStatus.CONFIRMED) + " ) "
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
            Logger.getLogger(PurchaseOrderReceiving.class.getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
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

    public int getTransactionAttachmentCount() {
        if (paAttachments == null) {
            paAttachments = new ArrayList<>();
        }

        return paAttachments.size();
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

            lsSQL = MiscUtil.addCondition(lsSQL, " a.sIndstCdx = " + SQLUtil.toSQL(psIndustryId)
                    + " AND a.sCompnyID LIKE " + SQLUtil.toSQL("%" + psCompanyId)
                    + " AND a.sSupplier LIKE " + SQLUtil.toSQL("%"+ Master().getSupplierId())
                    + " AND a.sDestinat = " + SQLUtil.toSQL(poGRider.getBranchCode())
                    + " AND a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderStatus.APPROVED)
                    + " AND a.sCategrCd = "+ SQLUtil.toSQL(psCategorCd)
                    + " AND b.nQuantity > b.nReceived "
//                    + " AND b.sCategrCd IN ( " + getCategory() + " ) "
//                    + " AND a.cProcessd = '0'" //get po that is approve but not yet processed
            )       + " GROUP BY a.sTransNox "
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

    private Model_PO_Master PurchaseOrderMaster() {
        return new PurchaseOrderModels(poGRider).PurchaseOrderMaster();
    }

    public Model_PO_Master PurchaseOrderList(int row) {
        return (Model_PO_Master) paPOMaster.get(row);
    }

    public int getPurchaseOrderCount() {
        if (paPOMaster == null) {
            paPOMaster = new ArrayList<>();
        }

        return paPOMaster.size();
    }

    public JSONObject addPurchaseOrderToPORDetail(String transactionNo)
            throws CloneNotSupportedException,
            SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        boolean lbExist = false;
        int lnRow = 0;
        int lnAddOrderQty = 0;
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
                            //check when pre-owned po is already exist in detail. 
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
                        if (loTrans.PurchaseOrder().Detail(lnCtr).getQuantity().intValue() > loTrans.PurchaseOrder().Detail(lnCtr).getReceivedQuantity().intValue()) {
                            Detail(getDetailCount() - 1).setBrandId(loTrans.PurchaseOrder().Detail(lnCtr).Inventory().getBrandId());
                            Detail(getDetailCount() - 1).setOrderNo(loTrans.PurchaseOrder().Detail(lnCtr).getTransactionNo());
                            Detail(getDetailCount() - 1).setStockId(loTrans.PurchaseOrder().Detail(lnCtr).getStockID());
                            Detail(getDetailCount() - 1).setUnitType(loTrans.PurchaseOrder().Detail(lnCtr).Inventory().getUnitType());
                            Detail(getDetailCount() - 1).setOrderQty(loTrans.PurchaseOrder().Detail(lnCtr).getQuantity());
                            Detail(getDetailCount() - 1).setWhCount(loTrans.PurchaseOrder().Detail(lnCtr).getQuantity());
                            Detail(getDetailCount() - 1).setUnitPrce(loTrans.PurchaseOrder().Detail(lnCtr).getUnitPrice());
                            Detail(getDetailCount() - 1).isSerialized(loTrans.PurchaseOrder().Detail(lnCtr).Inventory().isSerialized());

                            AddDetail();
                        }
                    } else {
                        //sum order qty based on existing stock id in POR Detail
                        for (int lnOrder = 0; lnOrder <= loTrans.PurchaseOrder().getDetailCount() - 1; lnOrder++) {
                            if(Detail(lnRow).getOrderNo().equals(loTrans.PurchaseOrder().Detail(lnOrder).getTransactionNo())){
                                if(Detail(lnRow).getStockId().equals(loTrans.PurchaseOrder().Detail(lnOrder).getStockID())){
                                    lnAddOrderQty = lnAddOrderQty + (loTrans.PurchaseOrder().Detail(lnOrder).getQuantity().intValue() - loTrans.PurchaseOrder().Detail(lnOrder).getReceivedQuantity().intValue());
                                }
                            }
                        }
                        
                        Detail(lnRow).setOrderQty(lnAddOrderQty);
                    }
                    
                    lbExist = false;
                    lnAddOrderQty = 0;
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

    public void resetOthers() {
        paOthers = new ArrayList<>();
        paAttachments = new ArrayList<>();
    }

    public void resetMaster() {
        poMaster = new PurchaseOrderReceivingModels(poGRider).PurchaseOrderReceivingMaster();
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
        int lnQuantity = Detail(entryNo - 1).getQuantity().intValue();
        int lnSerialCnt = 0;
        boolean lbShowMessage = false;

        if (!serialId.isEmpty()) {
            //1. Checke Serial if already exist in POR Serial list
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

    private Model_POR_Serial PurchaseOrderReceivingSerial() {
        return new PurchaseOrderReceivingModels(poGRider).PurchaseOrderReceivingSerial();
    }

    public Model_POR_Serial PurchaseOrderReceivingSerialList(int row) {
        return (Model_POR_Serial) paOthers.get(row);
    }

    public int getPurchaseOrderReceivingSerialCount() {
        if (paOthers == null) {
            paOthers = new ArrayList<>();
        }

        return paOthers.size();
    }

    public List<Model_POR_Serial> PurchaseOrderReceivingSerialList() {
        return paOthers;
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

        //check when serial id already exist do not allow to change brand / model
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
                poJSON.put("message", lsColName + " already exist for Entry No " + paOthers.get(lnCtr).getEntryNo() + "  at row " + lnRow + ".");
                poJSON.put("set", false);
                return poJSON;
            }
            
//            try {
//                if(paOthers.get(row).getSerialId() == null || "".equals(paOthers.get(lnSerialRow).getSerialId())){
//                    JSONObject loJSON = checkExistingSerialinDB(value, columnName, paOthers.get(row).getStockId());
//                    if("success".equals((String) loJSON.get("result"))){
//                        if( ShowMessageFX.YesNo(null, "Purchase Order Receiving Serial", lsColName + " already exist in database, do you want to set serial ID including serial information? ") == true){
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
                        + " FROM inv_serial a  "                                              
                        + " LEFT JOIN inv_serial_registration b ON b.sSerialID = a.sSerialID ";
        
        lsSQL = MiscUtil.addCondition(lsSQL, " a.sStockIDx = " + SQLUtil.toSQL(paOthers.get(row).getStockId())
                                                + " AND a.sSerialID <> " +  SQLUtil.toSQL(paOthers.get(row).getSerialId()) 
                                                
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
                lsSQL = lsSQL + " AND a.sSerial01 = " + SQLUtil.toSQL(paOthers.get(row).getSerial01());
            break;
            case "serial02":
                lsSQL = lsSQL + " AND a.sSerial02 = " + SQLUtil.toSQL(paOthers.get(row).getSerial02());
            break;
            case "csno":
                lsSQL = lsSQL + " AND b.sCStckrNo = " + SQLUtil.toSQL(paOthers.get(row).getConductionStickerNo());
            break;
            case "plateno":
                lsSQL = lsSQL + " AND b.sPlateNoP = " + SQLUtil.toSQL(paOthers.get(row).getPlateNo());
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

        //check when serial id already exist do not allow to change brand / model
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
        int lnQty = Detail(entryNo - 1).getQuantity().intValue();
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

    @Override
    public String getSourceCode() {
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
        
        if(!pbIsPrint){
            if (PurchaseOrderReceivingStatus.CONFIRMED.equals(Master().getTransactionStatus())) {
                poJSON = ShowDialogFX.getUserApproval(poGRider);
                if (!"success".equals((String) poJSON.get("result"))) {
                    return poJSON;
                } 
            }
        }

        Master().setModifyingId(poGRider.getUserID());
        Master().setModifiedDate(poGRider.getServerDate());
        
        boolean lbHasQty = false;
        int lnEntryNo = 0;
        int lnNewEntryNo = 1;
        int lnPrevEntryNo = -1;
        boolean lbMatch = false;
        
        paOthers.sort(Comparator.comparingInt(item -> item.getEntryNo()));
        
        for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
            if(Detail(lnCtr).getQuantity().intValue() > 0){ 
                lnEntryNo = lnEntryNo + 1;
            }
            
            if(Detail(lnCtr).getQuantity().intValue() <= 0){ 
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
            
            if (Detail(lnCtr).getQuantity().intValue() > 0) {
                lbHasQty = true;
            }
        }
        
        if(!lbHasQty){
            poJSON.put("result", "error");
            poJSON.put("message", "Your Purchase order receiving cannot be zero quantity.");
            return poJSON;
        }
        
        Iterator<Model> detail = Detail().iterator();
        while (detail.hasNext()) {
            Model item = detail.next();

            if ("".equals((String) item.getValue("sStockIDx"))
                    || (int) item.getValue("nQuantity") <= 0) {
                detail.remove();

                if (!"".equals((String) item.getValue("sOrderNox")) && (String) item.getValue("sOrderNox") != null) {
                    paDetailRemoved.add(item);
                }
            }
        }
        
        //Validate detail after removing all zero qty and empty stock Id
        if (getDetailCount() <= 0) {
            poJSON.put("result", "error");
            poJSON.put("message", "No Purchase order receiving detail to be save.");
            return poJSON;
        }

        if (getDetailCount() == 1) {
            //do not allow a single item detail with no quantity order
            if (Detail(0).getQuantity().intValue() == 0) {
                poJSON.put("result", "error");
                poJSON.put("message", "Your Purchase order receiving has zero quantity.");
                return poJSON;
            }
        }

        if (PurchaseOrderReceivingStatus.RETURNED.equals(Master().getTransactionStatus())) {
            PurchaseOrderReceiving loRecord = new PurchaseOrderReceivingControllers(poGRider, null).PurchaseOrderReceiving();
            loRecord.InitTransaction();
            loRecord.OpenTransaction(Master().getTransactionNo());

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
                for (int lnCtr = 0; lnCtr <= loRecord.getDetailCount() - 1; lnCtr++) {
                    lbUpdated = loRecord.Detail(lnCtr).getStockId().equals(Detail(lnCtr).getStockId());
                    if (lbUpdated) {
                        lbUpdated = loRecord.Detail(lnCtr).getQuantity().equals(Detail(lnCtr).getQuantity());
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

        //assign other info on detail
        for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
            if(Detail(lnCtr).getOrderNo() != null && !"".equals(Detail(lnCtr).getOrderNo())){
                if(Detail(lnCtr).getOrderQty().intValue() < Detail(lnCtr).getQuantity().intValue()){
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
            //Mobile Phone : 01 Motorcycle   : 02 Vehicle      : 03
            //Mobile Phone : 0001 Motorcycle   : 0010 Vehicle      : 0015
            if(Detail(lnCtr).isSerialized()){
                //check serial list must be equal to por detail receive qty
                for (int lnList = 0; lnList <= getPurchaseOrderReceivingSerialCount() - 1; lnList++) {
                    if (PurchaseOrderReceivingSerialList(lnList).getEntryNo() == Detail(lnCtr).getEntryNo()) {
                        //If there a value for serial 1 do not allow saving when serial 2 and location is empty 
                        if ((PurchaseOrderReceivingSerialList(lnList).getSerial01() == null || "".equals(PurchaseOrderReceivingSerialList(lnList).getSerial01()))
                                || (PurchaseOrderReceivingSerialList(lnList).getSerial02() == null || "".equals(PurchaseOrderReceivingSerialList(lnList).getSerial02()))) {
                            poJSON.put("result", "error");
                            poJSON.put("message", "Serial cannot be empty for Entry No " + PurchaseOrderReceivingSerialList(lnList).getEntryNo());
                            return poJSON;
                        }

                        if ("02".equals(Master().getIndustryId()) || "03".equals(Master().getIndustryId())) {
                            if (PurchaseOrderReceivingSerialList(lnList).getLocationId() == null || "".equals(PurchaseOrderReceivingSerialList(lnList).getLocationId())) {
                                poJSON.put("result", "error");
                                poJSON.put("message", "Location cannot be empty for Entry No " + PurchaseOrderReceivingSerialList(lnList).getEntryNo());
                                return poJSON;
                            }
                        }

                        if ("03".equals(Master().getIndustryId())) {
                            if ((PurchaseOrderReceivingSerialList(lnList).getPlateNo() == null || "".equals(PurchaseOrderReceivingSerialList(lnList).getPlateNo()))
                                    && (PurchaseOrderReceivingSerialList(lnList).getConductionStickerNo() == null || "".equals(PurchaseOrderReceivingSerialList(lnList).getConductionStickerNo()))) {
                                poJSON.put("result", "error");
                                poJSON.put("message", "CS / Plate No cannot be empty for Entry No " + PurchaseOrderReceivingSerialList(lnList).getEntryNo());
                                return poJSON;
                            }
                        }
                        //No need to validate Existing serial in DB: Inv_Serial Class will be the one to check it.
                        //Check for existing serial 01
//                        JSONObject loJSON = checkExistingSerialinDB(lnList, "serial01");
//                        lsColumnName = getColumnName("serial01");
//                        if("error".equals((String) loJSON.get("result"))){
//                            poJSON.put("result", "error");
//                            poJSON.put("message", lsColumnName +" "+ PurchaseOrderReceivingSerialList(lnList).getSerial01() + " already exist in database see Serial ID: " + (String) loJSON.get("sSerialID"));
//                            return poJSON;
//                        } 
//
//                        //Check for existing serial 02
//                        loJSON = checkExistingSerialinDB(lnList, "serial02");
//                        lsColumnName = getColumnName("serial02");
//                        if("error".equals((String) loJSON.get("result"))){
//                            poJSON.put("result", "error");
//                            poJSON.put("message", lsColumnName +" "+ PurchaseOrderReceivingSerialList(lnList).getSerial02() + " already exist in database see Serial ID: " + (String) loJSON.get("sSerialID"));
//                            return poJSON;
//                        }
//
//                        //Check for existing CS No
//                        if (PurchaseOrderReceivingSerialList(lnList).getConductionStickerNo()!= null 
//                                && !"".equals(PurchaseOrderReceivingSerialList(lnList).getConductionStickerNo())){
//                            loJSON = checkExistingSerialinDB(lnList, "csno");
//                            lsColumnName = getColumnName("csno");
//                            if("error".equals((String) loJSON.get("result"))){
//                                poJSON.put("result", "error");
//                                poJSON.put("message", lsColumnName +" "+ PurchaseOrderReceivingSerialList(lnList).getConductionStickerNo() + " already exist in database see Serial ID: " + (String) loJSON.get("sSerialID"));
//                                return poJSON;
//                            }
//                        }
//
//                        //Check for existing Plate No
//                        if (PurchaseOrderReceivingSerialList(lnList).getPlateNo() != null 
//                                && !"".equals(PurchaseOrderReceivingSerialList(lnList).getPlateNo())){
//                            loJSON = checkExistingSerialinDB(lnList, "plateno");
//                            lsColumnName = getColumnName("plateno");
//                            if("error".equals((String) loJSON.get("result"))){
//                                poJSON.put("result", "error");
//                                poJSON.put("message", lsColumnName +" "+ PurchaseOrderReceivingSerialList(lnList).getPlateNo() + " already exist in database see Serial ID: " + (String) loJSON.get("sSerialID"));
//                                return poJSON;
//                            }
//                        }
                        lnSerialCnt++;
                    }
                }

                if (lnSerialCnt != Detail(lnCtr).getQuantity().intValue()) {
                    poJSON.put("result", "error");
                    poJSON.put("message", "Descrepancy found in POR Serial. Quantity must be equal to POR Serial list for Entry No. " + (lnCtr + 1) + ".");
                    return poJSON;
                }
            }

            lnSerialCnt = 0;
        }

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
        int lnCtr, lnRow;

        try {
            //Purchase Order Receiving Serial
            InvSerial loInvSerial = new InvControllers(poGRider, logwrapr).InventorySerial();
            loInvSerial.setWithParentClass(true);

            for (lnRow = 0; lnRow <= getPurchaseOrderReceivingSerialCount() - 1; lnRow++) {
                //1. Check for Serial ID
                if ("".equals(paOthers.get(lnRow).getSerialId()) || paOthers.get(lnRow).getSerialId() == null) {
                    //1.1 Create New Inventory Serial
                    poJSON = loInvSerial.newRecord();
                    if ("error".equals((String) poJSON.get("result"))) {
                        System.out.println("inv serial " + (String) poJSON.get("message"));
                        return poJSON;
                    }
                } else {
                    //1.2 Update Inventory Serial / Registration
                    poJSON = loInvSerial.openRecord(paOthers.get(lnRow).getSerialId());
                    if ("error".equals((String) poJSON.get("result"))) {
                        return poJSON;
                    }
                    System.out.println(loInvSerial.getEditMode());
                    poJSON = loInvSerial.updateRecord();
                    if ("error".equals((String) poJSON.get("result"))) {
                        return poJSON;
                    }
                }
                
                //2. Update values for serial
                if (loInvSerial.getEditMode() == EditMode.ADDNEW || loInvSerial.getEditMode() == EditMode.UPDATE) {
                    loInvSerial.getModel().setStockId(paOthers.get(lnRow).getStockId());
                    loInvSerial.getModel().setSerial01(paOthers.get(lnRow).getSerial01());
                    loInvSerial.getModel().setSerial02(paOthers.get(lnRow).getSerial02());
                    loInvSerial.getModel().setUnitType(paOthers.get(lnRow).Inventory().getUnitType());
                    
                    if(poGRider.isWarehouse()){
                        loInvSerial.getModel().setLocation("0"); 
                    } else {
                        loInvSerial.getModel().setLocation("1"); 
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
                    if ("error".equals((String) poJSON.get("result"))) {
                        System.out.println("inv serial validation : " + (String) poJSON.get("message"));
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

    private PurchaseOrder PurchaseOrder() {
        return new PurchaseOrderControllers(poGRider, logwrapr).PurchaseOrder();
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
                if (Detail(lnCtr).getOrderQty().intValue() != Detail(lnCtr).getQuantity().intValue()) {
                    System.out.println("Require Approval");
                    pbApproval = true;
                }
                //Purchase Order
                poJSON = updatePurchaseOrder(status, Detail(lnCtr).getOrderNo(), Detail(lnCtr).getStockId(), Detail(lnCtr).getQuantity().intValue());
                if("error".equals((String) poJSON.get("result"))){
                    return poJSON;
                }

                //Inventory Transaction
                if(Detail(lnCtr).getReplaceId() != null && !"".equals(Detail(lnCtr).getReplaceId())){
                    updateInventoryTransaction(status, Detail(lnCtr).getReplaceId(), Detail(lnCtr).getQuantity().intValue());
                } else {
                    updateInventoryTransaction(status, Detail(lnCtr).getStockId(), Detail(lnCtr).getQuantity().intValue());
                }

            } else {
                //Require approve for all po receiving without po
                System.out.println("Require Approval");
                pbApproval = true;
            }
        }

        //Update purchase order removed in purchase order receiving
        for (lnCtr = 0; lnCtr <= getDetailRemovedCount() - 1; lnCtr++) {
            //Purchase Order
            poJSON = updatePurchaseOrder(status, DetailRemove(lnCtr).getOrderNo(), DetailRemove(lnCtr).getStockId(), DetailRemove(lnCtr).getQuantity().intValue());
            if("error".equals((String) poJSON.get("result"))){
                return poJSON;
            }
            
            //Inventory Transaction TODO
            if(DetailRemove(lnCtr).getReplaceId() != null && !"".equals(DetailRemove(lnCtr).getReplaceId())){
                updateInventoryTransaction(status, DetailRemove(lnCtr).getReplaceId(), DetailRemove(lnCtr).getQuantity().intValue());
            } else {
                updateInventoryTransaction(status, DetailRemove(lnCtr).getStockId(), DetailRemove(lnCtr).getQuantity().intValue());
            }
        }

        poJSON.put("result", "success");
        return poJSON;
    }

    private JSONObject updatePurchaseOrder(String status, String orderNo, String stockId, int quantity)
            throws GuanzonException,
            SQLException,
            CloneNotSupportedException {
        int lnRow, lnList;
        int lnRecQty = 0;
        int lnOrderQty = 0;
        boolean lbExist = false;
        //2.check if order no is already exist in purchase order array list
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
                lnRecQty = getReceivedQty(orderNo, stockId, true);
                //Add received qty in po receiving
                lnRecQty = lnRecQty + quantity;
                
                for (lnRow = 0; lnRow <= paPurchaseOrder.get(lnList).getDetailCount() - 1; lnRow++) {
                    if (stockId.equals(paPurchaseOrder.get(lnList).Detail(lnRow).getStockID())) {
                        lnOrderQty = lnOrderQty + paPurchaseOrder.get(lnList).Detail(lnRow).getQuantity().intValue();
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
                lnRecQty = getReceivedQty(orderNo, stockId, false);
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
                    if(lnRecQty > paPurchaseOrder.get(lnList).Detail(lnRow).getQuantity().intValue()){
                        paPurchaseOrder.get(lnList).Detail(lnRow).setReceivedQuantity(paPurchaseOrder.get(lnList).Detail(lnRow).getQuantity());
                        lnRecQty = lnRecQty - paPurchaseOrder.get(lnList).Detail(lnRow).getQuantity().intValue();
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

    //Open record for checking total receive qty per purchase order
    private int getReceivedQty(String orderNo, String stockId, boolean isAdd)
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        int lnRecQty = 0;
        String lsSQL = " SELECT "
                + " b.nQuantity AS nQuantity "
                + " FROM po_receiving_master a "
                + " LEFT JOIN po_receiving_detail b ON b.sTransNox = a.sTransNox ";
        lsSQL = MiscUtil.addCondition(lsSQL, " b.sOrderNox = " + SQLUtil.toSQL(orderNo)
                + " AND b.sStockIDx = " + SQLUtil.toSQL(stockId)
                + " AND ( a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderReceivingStatus.CONFIRMED)
                + " OR a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderReceivingStatus.PAID)
                + " OR a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderReceivingStatus.POSTED)
                + " ) ");

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

    private JSONObject saveUpdateOthers(String status)
            throws CloneNotSupportedException {
        /*Only modify this if there are other tables to modify except the master and detail tables*/
        poJSON = new JSONObject();
        int lnCtr, lnRow;
//        boolean lbProcessed = true;
        try {

            //1. Save Purchase Order exist in PO Receiving Detail 
            for (lnCtr = 0; lnCtr <= paPurchaseOrder.size() - 1; lnCtr++) {
                //Check Order qty vs Received qty 
//                for (lnRow = 0; lnRow <= paPurchaseOrder.get(lnCtr).getDetailCount() - 1; lnRow++) {
//                    if (paPurchaseOrder.get(lnCtr).Detail(lnRow).getQuantity().intValue() > paPurchaseOrder.get(lnCtr).Detail(lnRow).getReceivedQuantity().intValue()) {
//                        lbProcessed = false;
//                        break;
//                    }
//                }
                if(PurchaseOrderReceivingStatus.CONFIRMED.equals(status)){
                    paPurchaseOrder.get(lnCtr).Master().setProcessed(true);
                }
                paPurchaseOrder.get(lnCtr).Master().setModifyingId(poGRider.getUserID());
                paPurchaseOrder.get(lnCtr).Master().setModifiedDate(poGRider.getServerDate());
                paPurchaseOrder.get(lnCtr).setWithParent(true);
                poJSON = paPurchaseOrder.get(lnCtr).SaveTransaction();
                if ("error".equals((String) poJSON.get("result"))) {
                    System.out.println("Purchase Order Saving " + (String) poJSON.get("message"));
                    return poJSON;
                }
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
                InvSerial loInvSerial = new InvControllers(poGRider, logwrapr).InventorySerial();
                loInvSerial.initialize();
                loInvSerial.setWithParentClass(true);
                //            InventoryTrans.POReceiving();
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

    //TODO
    private void updateInventoryTransaction(String status, String stockId, int quantity)
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
//                case PurchaseOrderReceivingStatus.APPROVED: 
//                    //Get total received qty from other po receiving entry
//                    lnRecQty = getReceivedQty("", stockId, true);
//                    //Add received qty in po receiving
//                    lnRecQty = lnRecQty + quantity;
//                    break;
//                case PurchaseOrderReceivingStatus.VOID:
//                case PurchaseOrderReceivingStatus.RETURNED: 
//                    //Get total received qty from other po receiving entry
//                    lnRecQty = getReceivedQty("", stockId, false);
//                    //Deduct received qty in po receiving
//                    lnRecQty = lnRecQty - quantity;
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
            Master().setTermCode("0000004");
            Master().setTransactionStatus(PurchaseOrderReceivingStatus.OPEN);

        } catch (SQLException ex) {
            Logger.getLogger(PurchaseOrderReceiving.class.getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
            return poJSON;
        }

        poJSON.put("result", "success");
        return poJSON;
    }

    public String getInventoryTypeCode()
            throws SQLException {
        String lsSQL = "SELECT sInvTypCd FROM category ";
        lsSQL = MiscUtil.addCondition(lsSQL, " sIndstCdx = " + SQLUtil.toSQL(psIndustryId)
                                                +  " AND sCategrCd = " + SQLUtil.toSQL(psCategorCd));

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
                + " , b.sCompnyNm  AS sSupplrNm"
                + " , c.sCompnyNm  AS sCompnyNm"
                + " , d.sDescript  AS sIndustry"
                + " FROM po_receiving_master a "
                + " LEFT JOIN client_master b ON b.sClientID = a.sSupplier "
                + " LEFT JOIN company c ON c.sCompnyID = a.sCompnyID "
                + " LEFT JOIN industry d ON d.sIndstCdx = a.sIndstCdx ";
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

    public JSONObject printRecord(Runnable onPrintedCallback) {
        poJSON = new JSONObject();
        String watermarkPath = "D:\\GGC_Maven_Systems\\Reports\\images\\draft.png"; //set draft as default
        try {
            // 1. Prepare parameters
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("sBranchNm", poGRider.getBranchName()); //TODO
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
            switch (Master().getTransactionStatus()) {
                case PurchaseOrderReceivingStatus.CONFIRMED:
                case PurchaseOrderReceivingStatus.PAID:
                case PurchaseOrderReceivingStatus.POSTED:
                    if("1".equals(Master().getPrint())){
                        watermarkPath = "D:\\GGC_Maven_Systems\\Reports\\images\\approvedreprint.png";
                    } else {
                        watermarkPath = "D:\\GGC_Maven_Systems\\Reports\\images\\approved.png";
                    }
                    break;
//                case PurchaseOrderReceivingStatus.CANCELLED:
//                    watermarkPath = "D:\\GGC_Maven_Systems\\Reports\\images\\cancelled.png";
//                    break;
            }

            parameters.put("watermarkImagePath", watermarkPath);
            List<OrderDetail> orderDetails = new ArrayList<>();

            double lnTotal = 0.0;
            int lnRow = 1;
            for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
                lnTotal = Detail(lnCtr).getUnitPrce().doubleValue() * Detail(lnCtr).getQuantity().intValue();
                orderDetails.add(new OrderDetail(lnRow, String.valueOf(Detail(lnCtr).getOrderNo()), Detail(lnCtr).Inventory().getBarCode(), Detail(lnCtr).Inventory().getDescription(), Detail(lnCtr).getUnitPrce().doubleValue(), Detail(lnCtr).getQuantity().intValue(), lnTotal));
                lnRow++;
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

            if (poViewer != null && poViewer.isDisplayable()) {
                poViewer.dispose();
                poViewer = null;

            }
            poViewer = new CustomJasperViewer(jasperPrint, onPrintedCallback);
            poViewer.setVisible(true);
            poViewer.toFront();

        } catch (JRException e) {
            System.err.println("Error generating report: " + e.getMessage());
            e.printStackTrace();
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(e));
        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(PurchaseOrderReceiving.class.getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
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

        private class CustomJasperViewer extends JasperViewer {

        private final Runnable onPrintedCallback;

        public CustomJasperViewer(JasperPrint jasperPrint, Runnable onPrintedCallback) {
            super(jasperPrint, false);
            this.onPrintedCallback = onPrintedCallback;
            customizePrintButton(jasperPrint);

            this.addWindowListener(new WindowAdapter() {
                public void windowClosed(WindowEvent e) {
                    poViewer = null;
                }

                public void windowClosing(WindowEvent e) {
                    poViewer = null;
                }
            });
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
                if (PurchaseOrderReceivingStatus.CONFIRMED.equals(Master().getTransactionStatus())) {
                    poJSON = OpenTransaction((String) poMaster.getValue("sTransNox"));
                    if ("error".equals((String) poJSON.get("result"))) {
                        Platform.runLater(() -> {
                            ShowMessageFX.Warning(null, "Print Purchase Order Receiving", "Printing of the transaction was aborted.\n" + (String) poJSON.get("message"));
                            SwingUtilities.invokeLater(() -> CustomJasperViewer.this.toFront());
                        });
                        fbIsPrinted = false;
                    }
                    poJSON = UpdateTransaction();
                    if ("error".equals((String) poJSON.get("result"))) {
                        Platform.runLater(() -> {
                            ShowMessageFX.Warning(null, "Print Purchase Order Receiving", "Printing of the transaction was aborted.\n" + (String) poJSON.get("message"));
                            SwingUtilities.invokeLater(() -> CustomJasperViewer.this.toFront());
                        });
                        fbIsPrinted = false;
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
                onPrintedCallback.run();  // <- triggers controller method!
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
}
