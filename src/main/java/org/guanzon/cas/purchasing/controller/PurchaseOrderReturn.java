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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javax.script.ScriptException;
import javax.sql.rowset.CachedRowSet;
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
import org.guanzon.appdriver.agent.ActionAuthManager;
import org.guanzon.appdriver.agent.MatrixAuthChecker;
import org.guanzon.appdriver.agent.ShowDialogFX;
import org.guanzon.appdriver.agent.ShowMessageFX;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.agent.services.Transaction;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.constant.Logical;
import org.guanzon.appdriver.constant.RecordStatus;
import org.guanzon.appdriver.constant.UserRight;
import org.guanzon.appdriver.iface.GValidator;
import org.guanzon.cas.client.account.AP_Client_Master;
import org.guanzon.cas.client.services.ClientControllers;
import org.guanzon.cas.inv.InvSupplierPrice;
import org.guanzon.cas.inv.InvTransCons;
import org.guanzon.cas.inv.InventoryTransaction;
import org.guanzon.cas.purchasing.model.Model_POReturn_Detail;
import org.guanzon.cas.purchasing.model.Model_POReturn_Master;
import org.guanzon.cas.purchasing.services.PurchaseOrderReceivingControllers;
import org.guanzon.cas.purchasing.services.PurchaseOrderReturnControllers;
import org.guanzon.cas.purchasing.services.PurchaseOrderReturnModels;
import org.guanzon.cas.purchasing.status.PurchaseOrderReturnStatus;
import org.guanzon.cas.purchasing.validator.PurchaseOrderReturnValidatorFactory;
import org.guanzon.cas.tbjhandler.TBJEntry;
import org.guanzon.cas.tbjhandler.TBJTransaction;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.rmj.cas.core.APTransaction;
import org.rmj.cas.core.GLTransaction;
import ph.com.guanzongroup.cas.cashflow.CachePayable;
import ph.com.guanzongroup.cas.cashflow.Journal;
import ph.com.guanzongroup.cas.cashflow.model.Model_Journal_Master;
import ph.com.guanzongroup.cas.cashflow.services.CashflowControllers;
import ph.com.guanzongroup.cas.cashflow.services.CashflowModels;
import ph.com.guanzongroup.cas.cashflow.status.CachePayableStatus;

/**
 *
 * @author Arsiela 04-28-2025
 */
public class PurchaseOrderReturn extends Transaction{
    private boolean pbIsFinance = false;
    private boolean pbIsPrint = false;
    private String psIndustryId = "";
    private String psCompanyId = "";
    private String psCategorCd = "";
    private String psTransNox = "";
    
    private Journal poJournal;
    private CachePayable poCachePayable;
    List<Model_POReturn_Master> paPORMaster;
    List<InventoryTransaction> paInventoryTransaction;
    List<Model> paDetailRemoved;
    
    public JSONObject InitTransaction() {
        SOURCE_CODE = InvTransCons.PURCHASE_RETURN;

        poMaster = new PurchaseOrderReturnModels(poGRider).PurchaseOrderReturnMaster();
        poDetail = new PurchaseOrderReturnModels(poGRider).PurchaseOrderReturnDetails();

        paDetail = new ArrayList<>();
        paDetailRemoved = new ArrayList<>();
        paInventoryTransaction = new ArrayList<>();

//        psIndustryId = poGRider.getIndustry();

        return initialize();
    }
    
    /**
     * Seek Approval method 
     * @return JSON
     * @throws SQLException
     * @throws SQLException
     * @throws GuanzonException 
     */
    public JSONObject seekApproval() 
            throws SQLException, SQLException, GuanzonException{
        poJSON = new JSONObject();
        //Moved only the script for seeking of approval - Arsiela 10-15-2025 - 14:11:01
            
        //load authorization manager that evaluates current users authority for this process
        ActionAuthManager loAuth = new ActionAuthManager(poGRider, "cas-purchasing");
        poJSON = loAuth.isAuthorized();

        //check if currenty user is authorized
        if(!((String)poJSON.get("result")).equalsIgnoreCase("true")){
           //if not authorized, check the type type of authorization required 
           if(((String)poJSON.get("code")).equalsIgnoreCase("regular")){
                //show process need regular authorization
                ShowMessageFX.Warning((String)poJSON.get("warning"), "Authorization Required", null);
                //get authorization from authoried personnel
                poJSON = ShowDialogFX.getUserApproval(poGRider);
                if("error".equals((String)poJSON.get("result"))){
                    return poJSON;
                }

               //check if approving officer is authorized
               String lsUserIDxx = poJSON.get("sUserIDxx").toString();
               int lnUserLevl = Integer.parseInt(poJSON.get("nUserLevl").toString());
               poJSON = loAuth.isAuthorized(lsUserIDxx, lnUserLevl);

               //if approving is not authorized then do not continue process
               if(!((String)poJSON.get("result")).equalsIgnoreCase("true")){
                   ShowMessageFX.Warning((String)poJSON.get("warning"), "Authorization Required", null);
                    poJSON.put("result", "error");
                    poJSON.put("message", "User is not an authorized approving officer.");
                    return poJSON;
               }
               setApproving(lsUserIDxx);
            }
            //needs authorization thru authorization matrix
            else{
               //show process needs authorization through the authority matrix
               ShowMessageFX.Warning((String)poJSON.get("warning"), "Authorization Required", null);
               poJSON.put("result", "error");
               poJSON.put("message", "User is not an authorized approving officer.");
               return poJSON;
            }
        }  
        
        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    
    }
    
    public JSONObject NewTransaction()
            throws CloneNotSupportedException, SQLException, GuanzonException {
        
        if(!pbWthParent){
            //Replaced script above by calling of method Arsiela 10-15-2025 09:25:01
            poJSON = seekApproval();
            if("error".equalsIgnoreCase((String)poJSON.get("result"))){
                return poJSON;
            }
        }
        
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
        if(!pbWthParent){
            //Replaced script above by calling of method Arsiela 10-15-2025 09:25:01
            poJSON = seekApproval();
            if("error".equalsIgnoreCase((String)poJSON.get("result"))){
                return poJSON;
            }
        }
        
        //Clear data
        resetMaster();
        Detail().clear();
        Journal();
        return openTransaction(transactionNo);
    }

    public JSONObject UpdateTransaction() throws SQLException, GuanzonException {
        if(!pbWthParent){
            //Replaced script above by calling of method Arsiela 10-15-2025 09:25:01
            poJSON = seekApproval();
            if("error".equalsIgnoreCase((String)poJSON.get("result"))){
                return poJSON;
            }
        }
        
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

        //Make sure edit mode is ready
        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

        MatrixAuthChecker check = null; 
        
        if(!pbWthParent){
            //validator
            poJSON = isEntryOkay(lsStatus);
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }

            //get the matrix return from isEntryOkey
            JSONArray loMatrix = (JSONArray) poJSON.get("matrix");

            //Check if there is a authorization request
            if(loMatrix != null){
                //initialized MatrixAuthChecker object
                check = new MatrixAuthChecker(poGRider, SOURCE_CODE, Master().getTransactionNo());
                //load the current autorization matrix request
                poJSON = check.loadAuth();

                //check if loading is okey
                if (!"success".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }

                //check if authorization request is already approved by all authorizing personnel
                if(!check.isAuthOkay()){
                    //check if authorization request allows system approval
                    if(!check.isAllowSys()){
                        //extract the JSONObject from JSONArray
                        JSONObject loJson = (JSONObject)loMatrix.get(0);

                        //check if current user is authorized to approved this transaction
                        poJSON = check.authTrans((String) loJson.get("sAuthType"), poGRider.getUserID());
                        
                        //If not authorized/request system approval
                        if(!"success".equalsIgnoreCase((String)poJSON.get("result"))){
                            poJSON = ShowDialogFX.getUserApproval(poGRider);
                            if("error".equals((String)poJSON.get("result"))){
                                return poJSON;
                            }

                            //check if approving officer is authorized
                            String lsUserIDxx = poJSON.get("sUserIDxx").toString();
                            //check if current user is authorized to approved this transaction
                            poJSON = check.authTrans((String) loJson.get("sAuthType"), poGRider.getUserID());
                            //user is not authorized
                            if(!"success".equalsIgnoreCase((String)poJSON.get("result"))){
                                return poJSON;
                            }
                            setApproving(lsUserIDxx);
                        }
                    }

                    //check if authorization request is already approved by all authorizing personnel
                    if(!check.isAuthOkay()){
                        poGRider.beginTrans("UPDATE STATUS", "ConfirmTransaction", SOURCE_CODE, Master().getTransactionNo());

                        lsStatus = Character.toString((char)(64 + Integer.parseInt(lsStatus)));
                        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbConfirm, true);
                        if (!"success".equals((String) poJSON.get("result"))) {
                            poGRider.rollbackTrans();
                            return poJSON;
                        }

                        poGRider.commitTrans();
                        
                        poJSON.put("result", "matrix");
                        return poJSON;
                    }
                }
            }
            //there are no authorization event request
            else{
                //Replaced script above by calling of method Arsiela 10-15-2025 09:25:01
                poJSON = seekApproval();
                if("error".equalsIgnoreCase((String)poJSON.get("result"))){
                    return poJSON;
                }
            }
        }
        //Set receive qty to Purchase Order
        poJSON = setValueToOthers(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poGRider.beginTrans("UPDATE STATUS", "ConfirmTransaction", SOURCE_CODE, Master().getTransactionNo());
        try {
            //kalyptus - 2025.10.10 09:31am
            //Update the inventory for this Received Purchase
            InventoryTransaction loTrans = new InventoryTransaction(poGRider);
            loTrans.PurchaseReturn((String)poMaster.getValue("sTransNox"), (Date)poMaster.getValue("dTransact"), false);

            for (Model loDetail : paDetail) {
                Model_POReturn_Detail detail = (Model_POReturn_Detail) loDetail;
                //TODO: make sure to replace the detail.InventorySerial().getLocation() with detail.InventorySerial().getWarehouseID
                if(detail.getSerialId().trim().length() > 0){
                    loTrans.addSerial((String)poMaster.getValue("sIndstCdx"), detail.getSerialId(), false , detail.getUnitPrce().doubleValue(), detail.InventorySerial().getLocation());
                }
                else{
                    loTrans.addDetail((String)poMaster.getValue("sIndstCdx"), detail.getStockId(), detail.PurchaseOrderMaster().getPreOwned() ? "1" : "0", detail.getQuantity().doubleValue(), 0, detail.getUnitPrce().doubleValue()); //FIX: Changed passing of quantity value and order quantity Arsiela 10-16-2025 14:22:00
                }
            }
            loTrans.saveTransaction();


            //change status
            poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbConfirm, true);
            if (!"success".equals((String) poJSON.get("result"))) {
                poGRider.rollbackTrans();
                return poJSON;
            }

            if(check != null){
                check.postAuth();
            }

            poGRider.commitTrans();

            poJSON = new JSONObject();
            poJSON.put("result", "success");
            if (lbConfirm) {
                poJSON.put("message", "Transaction confirmed successfully.");
            } else {
                poJSON.put("message", "Transaction confirmation request submitted successfully.");
            }
            
        } catch (GuanzonException | SQLException | CloneNotSupportedException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poGRider.rollbackTrans();
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
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
            //Validate return
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
                setApproving((String) poJSON.get("sUserIDxx"));
            }
        }

        poGRider.beginTrans("UPDATE STATUS", "ReturnTransaction", SOURCE_CODE, Master().getTransactionNo());

        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbReturn, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }
        
//        if (PurchaseOrderReturnStatus.CONFIRMED.equals(Master().getTransactionStatus())) {
//            //Update Inventory, Serial Ledger
//            poJSON = saveUpdateOthers(PurchaseOrderReturnStatus.RETURNED);
//            if (!"success".equals((String) poJSON.get("result"))) {
//                poGRider.rollbackTrans();
//                return poJSON;
//            }
//        }

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

        computeFields(); //Recompute
        MatrixAuthChecker check = null; 
        
        if(!pbWthParent){
            //validator
            poJSON = isEntryOkay(lsStatus);
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }

            //get the matrix return from isEntryOkey
            JSONArray loMatrix = (JSONArray) poJSON.get("matrix");

            //Check if there is a authorization request
            if(loMatrix != null){
                //initialized MatrixAuthChecker object
                check = new MatrixAuthChecker(poGRider, SOURCE_CODE, Master().getTransactionNo());
                //load the current autorization matrix request
                poJSON = check.loadAuth();

                //check if loading is okey
                if (!"success".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }

                //check if authorization request is already approved by all authorizing personnel
                if(!check.isAuthOkay()){
                    //check if authorization request allows system approval
                    if(!check.isAllowSys()){
                        //extract the JSONObject from JSONArray
                        JSONObject loJson = (JSONObject)loMatrix.get(0);

                        //check if current user is authorized to approved this transaction
                        poJSON = check.authTrans((String) loJson.get("sAuthType"), poGRider.getUserID());
                        
                        //If not authorized/request system approval
                        if(!"success".equalsIgnoreCase((String)poJSON.get("result"))){
                            poJSON = ShowDialogFX.getUserApproval(poGRider);
                            if("error".equals((String)poJSON.get("result"))){
                                return poJSON;
                            }

                            //check if approving officer is authorized
                            String lsUserIDxx = poJSON.get("sUserIDxx").toString();
                            //check if current user is authorized to approved this transaction
                            poJSON = check.authTrans((String) loJson.get("sAuthType"), poGRider.getUserID());
                            //user is not authorized
                            if(!"success".equalsIgnoreCase((String)poJSON.get("result"))){
                                return poJSON;
                            }
                            setApproving(lsUserIDxx);
                        }
                    }

                    //check if authorization request is already approved by all authorizing personnel
                    if(!check.isAuthOkay()){
                        poGRider.beginTrans("UPDATE STATUS", "PostTransaction", SOURCE_CODE, Master().getTransactionNo());

                        lsStatus = Character.toString((char)(64 + Integer.parseInt(lsStatus)));
                        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbPosted, true);
                        if (!"success".equals((String) poJSON.get("result"))) {
                            poGRider.rollbackTrans();
                            return poJSON;
                        }

                        poGRider.commitTrans();
                        
                        poJSON.put("result", "matrix");
                        return poJSON;
                    }
                }
            }
            //there are no authorization event request
            else{
                //Replaced script above by calling of method Arsiela 10-15-2025 09:25:01
                poJSON = seekApproval();
                if("error".equalsIgnoreCase((String)poJSON.get("result"))){
                    return poJSON;
                }
            }
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

        //populate cache payable
        poJSON = populateCachePayable();
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
        poGRider.beginTrans("UPDATE STATUS", "PostTransaction", SOURCE_CODE, Master().getTransactionNo());
        
        try {
            double ldblAmountPaid = poCachePayable.Master().getAmountPaid();
            boolean lbIsPaid = Master().getNetTotal() == ldblAmountPaid;
            if(!lbIsPaid){
                poCachePayable.setWithParent(true);
                poJSON = poCachePayable.SaveTransaction();
                if (!"success".equals((String) poJSON.get("result"))) {
                    poGRider.rollbackTrans();
                    return poJSON;
                }
            }

            poJournal.setWithParent(true);
            poJournal.setWithUI(false);
            poJSON = poJournal.ConfirmTransaction("");
            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }

            System.out.println("----------INVENTORY SUPPLIER----------");
            InvSupplierPrice loTrans = new InvSupplierPrice(poGRider, Master().getBranchCode(), Master().getSupplierId(), poGRider.getUserID());
            loTrans.initTransaction(Master().getTransactionNo());
            for(int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++){
                loTrans.addDetail(Master().getIndustryId(), 
                        Detail(lnCtr).getStockId(), 
                        Detail(lnCtr).getQuantity().doubleValue(), 
                        Detail(lnCtr).getUnitPrce().doubleValue());

            }
            loTrans.saveTransaction();
            System.out.println("-----------------------------------");

            System.out.println("----------AP CLIENT MASTER----------");
            //Insert AP Client
            APTransaction loAPTrans = new APTransaction(poGRider, Master().getBranchCode());
            poJSON = loAPTrans.PurchaseReturn(Master().getSupplierId(), 
                    "",
                    Master().getTransactionNo(),
                    Master().getTransactionDate(),  
                    Master().getNetTotal(), 
                    false);
            if ("error".equals((String) poJSON.get("result"))) {
                poGRider.rollbackTrans();
                return poJSON;
            }
            System.out.println("-----------------------------------");

            System.out.println("----------ACCOUNT MASTER / LEDGER----------");
            //GL Transaction Account Ledger
            GLTransaction loGLTrans = new GLTransaction(poGRider,Master().getBranchCode());
            loGLTrans.initTransaction(getSourceCode(), Master().getTransactionNo());
            for(int lnCtr = 0; lnCtr <= Journal().getDetailCount() - 1; lnCtr++){
                if(Journal().Detail(lnCtr).getCreditAmount() > 0.0000 || Journal().Detail(lnCtr).getDebitAmount() > 0.0000){
                    loGLTrans.addDetail(Journal().Master().getBranchCode(), 
                            Journal().Detail(lnCtr).getAccountCode(),
                            SQLUtil.toDate(xsDateShort(Journal().Detail(lnCtr).getForMonthOf()), SQLUtil.FORMAT_SHORT_DATE) , 
                            Journal().Detail(lnCtr).getDebitAmount(), 
                            Journal().Detail(lnCtr).getCreditAmount());
                }
            }
            loGLTrans.saveTransaction();
            System.out.println("-----------------------------------");
             
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
            
            //change status
            poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbPosted, true);
            if (!"success".equals((String) poJSON.get("result"))) {
                poGRider.rollbackTrans();
                return poJSON;
            }

            if(check != null){
                check.postAuth();
            }
            
            poGRider.commitTrans();

            poJSON = new JSONObject();
            poJSON.put("result", "success");
            if (lbPosted) {
                poJSON.put("message", "Transaction posted successfully.");
            } else {
                poJSON.put("message", "Transaction posting request submitted successfully.");
            }
        
        } catch (GuanzonException | SQLException | CloneNotSupportedException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poGRider.rollbackTrans();
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
        } catch (NullPointerException npe) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(npe), npe);
            poGRider.rollbackTrans();
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(npe));
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

        //Make sure that edit mode is ready
        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

        MatrixAuthChecker check = null; 
        
        if(!pbWthParent){
            //validator
            poJSON = isEntryOkay(lsStatus);
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }

            //get the matrix return from isEntryOkey
            JSONArray loMatrix = (JSONArray) poJSON.get("matrix");

            //Check if there is a authorization request
            if(loMatrix != null){
                //initialized MatrixAuthChecker object
                check = new MatrixAuthChecker(poGRider, SOURCE_CODE, Master().getTransactionNo());
                //load the current autorization matrix request
                poJSON = check.loadAuth();

                //check if loading is okey
                if (!"success".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }

                //check if authorization request is already approved by all authorizing personnel
                if(!check.isAuthOkay()){
                    //check if authorization request allows system approval
                    if(!check.isAllowSys()){
                        //extract the JSONObject from JSONArray
                        JSONObject loJson = (JSONObject)loMatrix.get(0);

                        //check if current user is authorized to approved this transaction
                        poJSON = check.authTrans((String) loJson.get("sAuthType"), poGRider.getUserID());
                        
                        //If not authorized/request system approval
                        if(!"success".equalsIgnoreCase((String)poJSON.get("result"))){
                            poJSON = ShowDialogFX.getUserApproval(poGRider);
                            if("error".equals((String)poJSON.get("result"))){
                                return poJSON;
                            }

                            //check if approving officer is authorized
                            String lsUserIDxx = poJSON.get("sUserIDxx").toString();
                            //check if current user is authorized to approved this transaction
                            poJSON = check.authTrans((String) loJson.get("sAuthType"), poGRider.getUserID());
                            //user is not authorized
                            if(!"success".equalsIgnoreCase((String)poJSON.get("result"))){
                                return poJSON;
                            }
                            setApproving(lsUserIDxx);
                        }
                    }

                    //check if authorization request is already approved by all authorizing personnel
                    if(!check.isAuthOkay()){
                        poGRider.beginTrans("UPDATE STATUS", "Cancel Transaction", SOURCE_CODE, Master().getTransactionNo());

                        lsStatus = Character.toString((char)(64 + Integer.parseInt(lsStatus)));
                        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbCancelled, true);
                        if (!"success".equals((String) poJSON.get("result"))) {
                            poGRider.rollbackTrans();
                            return poJSON;
                        }

                        poGRider.commitTrans();
                        
                        poJSON.put("result", "matrix");
                        return poJSON;
                    }
                }
            }
            //there are no authorization event request
            else{
                //Replaced script above by calling of method Arsiela 10-15-2025 09:25:01
                poJSON = seekApproval();
                if("error".equalsIgnoreCase((String)poJSON.get("result"))){
                    return poJSON;
                }
            }
        }

        if (PurchaseOrderReturnStatus.CONFIRMED.equals(Master().getTransactionStatus())) {
            //update Purchase Order
            poJSON = setValueToOthers(lsStatus);
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
        }

        poGRider.beginTrans("UPDATE STATUS", "CancelledTransaction", SOURCE_CODE, Master().getTransactionNo());
        
        try {
            //kalyptus - 2025.10.10 09:31am
            //Update the inventory for this Received Purchase
            InventoryTransaction loTrans = new InventoryTransaction(poGRider);
            loTrans.PurchaseReturn((String)poMaster.getValue("sTransNox"), (Date)poMaster.getValue("dTransact"), true);

            for (Model loDetail : paDetail) {
                Model_POReturn_Detail detail = (Model_POReturn_Detail) loDetail;
                //TODO: make sure to replace the detail.InventorySerial().getLocation() with detail.InventorySerial().getWarehouseID
                if(detail.getSerialId().trim().length() > 0){
                    loTrans.addSerial((String)poMaster.getValue("sIndstCdx"), detail.getSerialId(), false , detail.getUnitPrce().doubleValue(), detail.InventorySerial().getLocation());
                }
                else{
                    loTrans.addDetail((String)poMaster.getValue("sIndstCdx"), detail.getStockId(), Master().PurchaseOrderReceivingMaster().getPurpose(), detail.getQuantity().doubleValue(),0 , detail.getUnitPrce().doubleValue()); //FIX: Changed passing of quantity value and order quantity Arsiela 10-16-2025 14:22:00
    //                loTrans.addDetail((String)poMaster.getValue("sIndstCdx"), detail.getStockId(), (String)poMaster.getValue("cPurposex"), 0, (double)detail.getQuantity(), (double)detail.getUnitPrce());
                }
            }
            loTrans.saveTransaction();

            //change status
            poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbCancelled, true);
            if (!"success".equals((String) poJSON.get("result"))) {
                poGRider.rollbackTrans();
                return poJSON;
            }

            if(check != null){
                check.postAuth();
            }

            poGRider.commitTrans();

            poJSON = new JSONObject();
            poJSON.put("result", "success");
            if (lbCancelled) {
                poJSON.put("message", "Transaction cancelled successfully.");
            } else {
                poJSON.put("message", "Transaction cancellation request submitted successfully.");
            }
        
        } catch (GuanzonException | SQLException | CloneNotSupportedException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poGRider.rollbackTrans();
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
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

        MatrixAuthChecker check = null; 
        
        if(!pbWthParent){
            //validator
            poJSON = isEntryOkay(lsStatus);
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }

            //get the matrix return from isEntryOkey
            JSONArray loMatrix = (JSONArray) poJSON.get("matrix");

            //Check if there is a authorization request
            if(loMatrix != null){
                //initialized MatrixAuthChecker object
                check = new MatrixAuthChecker(poGRider, SOURCE_CODE, Master().getTransactionNo());
                //load the current autorization matrix request
                poJSON = check.loadAuth();

                //check if loading is okey
                if (!"success".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }

                //check if authorization request is already approved by all authorizing personnel
                if(!check.isAuthOkay()){
                    //check if authorization request allows system approval
                    if(!check.isAllowSys()){
                        //extract the JSONObject from JSONArray
                        JSONObject loJson = (JSONObject)loMatrix.get(0);

                        //check if current user is authorized to approved this transaction
                        poJSON = check.authTrans((String) loJson.get("sAuthType"), poGRider.getUserID());
                        
                        //If not authorized/request system approval
                        if(!"success".equalsIgnoreCase((String)poJSON.get("result"))){
                            poJSON = ShowDialogFX.getUserApproval(poGRider);
                            if("error".equals((String)poJSON.get("result"))){
                                return poJSON;
                            }

                            //check if approving officer is authorized
                            String lsUserIDxx = poJSON.get("sUserIDxx").toString();
                            //check if current user is authorized to approved this transaction
                            poJSON = check.authTrans((String) loJson.get("sAuthType"), poGRider.getUserID());
                            //user is not authorized
                            if(!"success".equalsIgnoreCase((String)poJSON.get("result"))){
                                return poJSON;
                            }
                            setApproving(lsUserIDxx);
                        }
                    }

                    //check if authorization request is already approved by all authorizing personnel
                    if(!check.isAuthOkay()){
                        poGRider.beginTrans("UPDATE STATUS", "VoidTransaction", SOURCE_CODE, Master().getTransactionNo());

                        lsStatus = Character.toString((char)(64 + Integer.parseInt(lsStatus)));
                        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbVoid, true);
                        if (!"success".equals((String) poJSON.get("result"))) {
                            poGRider.rollbackTrans();
                            return poJSON;
                        }

                        poGRider.commitTrans();
                        
                        poJSON.put("result", "matrix");
                        return poJSON;
                    }
                }
            }
            //there are no authorization event request
            else{
                //Replaced script above by calling of method Arsiela 10-15-2025 09:25:01
                poJSON = seekApproval();
                if("error".equalsIgnoreCase((String)poJSON.get("result"))){
                    return poJSON;
                }
            }
        }
        
        if (PurchaseOrderReturnStatus.CONFIRMED.equals(Master().getTransactionStatus())) {
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

        if(check != null){
            check.postAuth();
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
                + " AND a.sBranchCD = " + SQLUtil.toSQL(poGRider.getBranchCode()));
//                + " AND a.sSupplier LIKE " + SQLUtil.toSQL("%" + Master().getSupplierId()));
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
    
    public JSONObject searchTransaction(String industryId, String companyId, String categoryId, String supplier, String sReferenceNo)
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
        
        if(categoryId == null || "".equals(categoryId)){
            categoryId = psCategorCd;
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
                + " AND a.sCategrCd = " + SQLUtil.toSQL(categoryId)
                + " AND a.sBranchCD = " + SQLUtil.toSQL(poGRider.getBranchCode())
                + " AND b.sCompnyNm  LIKE " + SQLUtil.toSQL("%" + supplier)
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
    
    /**
    * Searches transaction records using supplier name and reference number.
    * Applies optional transaction status, company, and category filters.
    * Returns the selected transaction details or an error message if no record is found.
    *
    * @param supplier Supplier name filter.
    * @param sReferenceNo Transaction reference number filter.
    * @return JSONObject containing transaction data or error details.
    * @throws CloneNotSupportedException
    * @throws SQLException
    * @throws GuanzonException
    */
    public JSONObject searchTransaction(String supplier, String sReferenceNo)
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
        
        if(supplier == null){
            supplier = "";
        }
        if(sReferenceNo == null){
            sReferenceNo = "";
        }
        String lsFilter = "";
        if(psCompanyId != null && !"".equals(psCompanyId)){
            lsFilter = lsFilter + " AND a.sCompnyID = " + SQLUtil.toSQL(psCompanyId);
        }
        if(psCategorCd != null && !"".equals(psCategorCd)){
            lsFilter = lsFilter + " AND a.sCategrCd = " + SQLUtil.toSQL(psCategorCd);
        }
        
        initSQL();
        String lsSQL = MiscUtil.addCondition(SQL_BROWSE, " b.sCompnyNm  LIKE " + SQLUtil.toSQL("%" + supplier)
                                                            + " AND a.sTransNox LIKE " + SQLUtil.toSQL("%" + sReferenceNo));
        if (psTranStat != null && !"".equals(psTranStat)) {
            lsSQL = lsSQL + lsTransStat;
        }
        if (lsFilter != null && !"".equals(lsFilter)) {
            lsSQL = lsSQL + lsFilter;
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

    public void isFinance(boolean isFinance) {
        pbIsFinance = isFinance;
    }
    
    public void resetMaster() {
        poMaster = new PurchaseOrderReturnModels(poGRider).PurchaseOrderReturnMaster();
    }
    
    public void resetJournal() {
        try {
            poJournal = new CashflowControllers(poGRider, logwrapr).Journal();
            poJournal.InitTransaction();
        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
        }
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

        
        AP_Client_Master object = new ClientControllers(poGRider, logwrapr).APClientMaster();
        object.setRecordStatus(RecordStatus.ACTIVE);
        poJSON = object.searchRecord(value, byCode);
        if ("success".equals((String) poJSON.get("result"))) {
            Master().setSupplierId(object.getModel().getClientId());
            Master().setAddressId(object.getModel().ClientAddress().getAddressId()); 
            Master().setContactId(object.getModel().ClientInstitutionContact().getContactPId()); 
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
            poJSON = object.searchTransaction(psIndustryId, psCompanyId, psCategorCd, Master().getSupplierId(), value, null);
        } else {
            poJSON = object.searchTransaction(psIndustryId, psCompanyId, psCategorCd, Master().getSupplierId(), null, value);
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
        object.setIndustryId(psIndustryId);
        object.setCompanyId(psCompanyId);
        object.setCategoryId(psCategorCd);
        poJSON = object.searchBarcodePORDetail(value, Master().getSourceNo());
        System.out.println("result" + (String) poJSON.get("result"));
        if ("success".equals((String) poJSON.get("result"))) {
            poJSON = populateDetail(object, (String) poJSON.get("sSerialID"), (String) poJSON.get("sStockIDx"), row);
            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
        }
        
        poJSON.put("row", row);
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
        object.setIndustryId(psIndustryId);
        object.setCompanyId(psCompanyId);
        object.setCategoryId(psCategorCd);
        poJSON = object.searchDescriptionPORDetail(value, Master().getSourceNo());
        poJSON.put("row", row);
        System.out.println("result" + (String) poJSON.get("result"));
        if ("success".equals((String) poJSON.get("result"))) {
            poJSON = populateDetail(object, (String) poJSON.get("sSerialID"), (String) poJSON.get("sStockIDx"), row);
            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
        }
        
        poJSON.put("row", row);
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
        object.setIndustryId(psIndustryId);
        object.setCompanyId(psCompanyId);
        object.setCategoryId(psCategorCd);
        poJSON = object.searchEnginePORDetail(value, Master().getSourceNo());
        poJSON.put("row", row);
        System.out.println("result" + (String) poJSON.get("result"));
        if ("success".equals((String) poJSON.get("result"))) {
            poJSON = populateDetail(object, (String) poJSON.get("sSerialID"), (String) poJSON.get("sStockIDx"), row);
            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
        }
        
        poJSON.put("row", row);
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
        object.setIndustryId(psIndustryId);
        object.setCompanyId(psCompanyId);
        object.setCategoryId(psCategorCd);
        poJSON = object.searchFramePORDetail(value, Master().getSourceNo());
        poJSON.put("row", row);
        System.out.println("result" + (String) poJSON.get("result"));
        if ("success".equals((String) poJSON.get("result"))) {
            poJSON = populateDetail(object, (String) poJSON.get("sSerialID"), (String) poJSON.get("sStockIDx"), row);
            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
        }
        
        poJSON.put("row", row);
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
        object.setIndustryId(psIndustryId);
        object.setCompanyId(psCompanyId);
        object.setCategoryId(psCategorCd);
        poJSON = object.searchImeiPORDetail(value, Master().getSourceNo());
        poJSON.put("row", row);
        System.out.println("result" + (String) poJSON.get("result"));
        if ("success".equals((String) poJSON.get("result"))) {
            poJSON = populateDetail(object, (String) poJSON.get("sSerialID"), (String) poJSON.get("sStockIDx"), row);
            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
        }
        
        poJSON.put("row", row);
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
            poJSON = populateDetail(object, (String) poJSON.get("sSerialID"), (String) poJSON.get("sStockIDx"), row);
            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
        }
        
        poJSON.put("row", row);
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
            poJSON = populateDetail(object, (String) poJSON.get("sSerialID"), (String) poJSON.get("sStockIDx"), row);
            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
        }
        
        poJSON.put("row", row);
        return poJSON;
    }
    
    private JSONObject populateDetail(PurchaseOrderReceiving object, String serialId, String stockId, int row) throws SQLException, GuanzonException{
        String lsSerialId = "";
        String lsStockId = "";
        String lsUnitType = "";
        String lsDescription = "";
        String lsSourceNo = "";
        Double ldblUnitPrice = 0.00;
        
        if(stockId == null){
            poJSON.put("row", row);
            poJSON.put("result", "error");
            poJSON.put("message", "Stock ID cannot be empty.");
            return poJSON;
        }
        
        if(serialId != null && !"".equals(serialId)){
            poJSON = object.getSerial().openRecord(Master().getSourceNo(), serialId);
            if ("error".equals((String) poJSON.get("result"))) {
                poJSON.put("row", row);
                return poJSON;
            }

            lsSerialId = object.getSerial().getSerialId();
            lsStockId = object.getSerial().getStockId();
            lsDescription = object.getSerial().Inventory().getDescription();
            lsUnitType = object.getSerial().PurchaseOrderReceivingDetails().getUnitType();
            ldblUnitPrice = object.getSerial().PurchaseOrderReceivingDetails().getUnitPrce().doubleValue();
            lsSourceNo = object.getSerial().PurchaseOrderReceivingDetails().getOrderNo();
        } else {
            poJSON = object.getDetail().openRecord(Master().getSourceNo(), stockId);
            if ("error".equals((String) poJSON.get("result"))) {
                poJSON.put("row", row);
                return poJSON;
            }
            
            if(object.getDetail().getReplaceId() != null && !"".equals(object.getDetail().getReplaceId())){
                lsStockId = object.getDetail().getReplaceId();
            } else {
                lsStockId = object.getDetail().getStockId();
            }
            
            lsUnitType = object.getDetail().getUnitType();
            lsDescription = object.getDetail().Inventory().getDescription();
            ldblUnitPrice = object.getDetail().getUnitPrce().doubleValue();
            lsSourceNo = object.getDetail().getOrderNo();
        }

        poJSON = checkExistingStock(lsStockId, lsSerialId,lsDescription, row, false);
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        Detail(row).setSerialId(lsSerialId);
        Detail(row).setStockId(lsStockId);
        Detail(row).setUnitPrce(ldblUnitPrice);
        Detail(row).setUnitType(lsUnitType);
        Detail(row).setSourceNo(lsSourceNo);
        
        poJSON.put("result", "success");
        return poJSON;
    }
    
    public Number getReceiveQty(int row) {
        poJSON = new JSONObject();
        double lnRecQty = 0;
        try {
            
            if(Master().getSourceNo() == null || "".equals(Master().getSourceNo())
             || Detail(row).getStockId() == null || "".equals(Detail(row).getStockId())){
                return 0;
            }
            
            if(Detail(row).getSerialId() != null && !"".equals(Detail(row).getSerialId())){
                return 1;
            }
            
            String lsSQL = " SELECT "
                    + " a.nQuantity AS nQuantity "
                    + " FROM PO_Receiving_Detail a " ;
            lsSQL = MiscUtil.addCondition(lsSQL, " a.sTransNox = " + SQLUtil.toSQL(Master().getSourceNo())
                                                    + " AND (a.sStockIDx = " + SQLUtil.toSQL(Detail(row).getStockId())
                                                    + " OR a.sReplacID = " + SQLUtil.toSQL(Detail(row).getStockId()) + " ) "
                                                    );
            
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
        } catch (SQLException ex) {
            Logger.getLogger(PurchaseOrderReturn.class.getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
        }
        
        return lnRecQty;
    }
    
    public Number getReturnQty(String stockId, String serialId, boolean isAdd)
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        
        if(Master().getSourceNo() == null || "".equals(Master().getSourceNo())){
            return 0;
        }
        
        double lnRetQty = 0;
        String lsSQL = " SELECT "
                + "  a.sTransNox "
                + " ,b.nQuantity AS nQuantity "
                + " FROM PO_Return_Master a "
                + " LEFT JOIN PO_Return_Detail b ON b.sTransNox = a.sTransNox " ;
        lsSQL = MiscUtil.addCondition(lsSQL, " a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderReturnStatus.CONFIRMED)
                                                + " AND a.sSourceNo = " + SQLUtil.toSQL(Master().getSourceNo())
                                                + " AND b.sStockIDx = " + SQLUtil.toSQL(stockId));
        
        if(serialId != null && !"".equals(serialId)){
            lsSQL = lsSQL + " AND b.sSerialID = " + SQLUtil.toSQL(serialId);
        }
        
        if (isAdd) {
            lsSQL = lsSQL + " AND a.sTransNox <> " + SQLUtil.toSQL(Master().getTransactionNo());
        }

        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        try {
            if (MiscUtil.RecordCount(loRS) >= 0) {
                while (loRS.next()) {
                    lnRetQty = lnRetQty + loRS.getDouble("nQuantity");
                    if(psTransNox.isEmpty()){
                        psTransNox = loRS.getString("sTransNox");
                    } else {
                        psTransNox = psTransNox + ", " + loRS.getString("sTransNox");
                    }
                }
            }
            MiscUtil.close(loRS);
        } catch (SQLException e) {
            System.out.println("No record loaded.");
            lnRetQty = 0;
        }
        
        return lnRetQty;
    }
    
    public JSONObject checkExistingStock(String stockId, String serialId, String description, int row, boolean isSave) throws SQLException, GuanzonException {
        for(int lnRow = 0; lnRow <= getDetailCount() - 1; lnRow++){
            if(lnRow != row ){
                if (stockId.equals(Detail(lnRow).getStockId()) && serialId.equals(Detail(lnRow).getSerialId())) {
                    if(!"".equals(serialId)){
                        poJSON.put("message", Detail(lnRow).InventorySerial().getSerial01() + " " + description+ " already exists in table at row " + (lnRow+1) + ".");
                    } else {
                        poJSON.put("message", description+ " already exists in table at row " + (lnRow+1) + ".");
                    }
                    poJSON.put("result", "error");
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
    
    //Added by Arsiela 05-12-2026
    //Implement this for PO Return Posting
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
            Master().setDiscountRate(0.00);
            computeDiscount(0.00);
            poJSON.put("result", "error");
            poJSON.put("message", "Discount rate cannot be negative or exceed 100.00");
            return poJSON;
        } else {

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
    
    public JSONObject computeFields()
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();

        //Compute Transaction Total
        Double ldblTotal = 0.00;
        Double ldblFreightTotal = 0.00;
        for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
            ldblTotal += (Detail(lnCtr).getUnitPrce().doubleValue() * Detail(lnCtr).getQuantity().doubleValue());
            ldblFreightTotal += Detail(lnCtr).getFreight().doubleValue();
        }
        
        Master().setTransactionTotal(ldblTotal);
        
        //Added by Arsiela 05-12-2026
        //Implement this for PO Return Posting
        /*Compute VAT Amount*/
        if(pbIsFinance){
            Master().setFreight(ldblFreightTotal);
            Double ldblDiscount = Master().getDiscount().doubleValue();
            Double ldblDiscountRate = Master().getDiscountRate().doubleValue();
            if(ldblDiscountRate > 0){
                ldblDiscountRate = ldblTotal * (ldblDiscountRate / 100);
            }
            double ldblDiscountTotal = ldblDiscountRate + ldblDiscount;
            boolean lbIsWithVat = false;
            double ldblVatSales = 0.0000;
            double ldblVatAmount = 0.0000;
            double ldblVatExemptSales = 0.0000;
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
                        ldblDetailVatSales = ldblDetailTotal - ldblDetailVatAmount; 
                    } else {
                        ldblDetailVatAmount = ldblDetailTotal * 0.12;
                        ldblDetailVatSales = ldblDetailTotal;  
                    }
                    
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
                    
                    ldblVatAmount = (ldblVatAmount + ldblFreightVatAmount) - ldblDiscountVatAmount;
                    ldblVatSales = (ldblVatSales 
                                    + (Master().getFreight().doubleValue() - ldblFreightVatAmount))
                                    - (ldblDiscountTotal - ldblDiscountVatAmount);
                } else {
                    ldblFreightVatAmount = Master().getFreight().doubleValue() * 0.12;
                    ldblDiscountVatAmount = ldblDiscountTotal * 0.12;
                    
                    ldblVatAmount = (ldblVatAmount + ldblFreightVatAmount) - ldblDiscountVatAmount;
                    ldblVatSales = (ldblVatSales 
                                    + Master().getFreight().doubleValue())
                                    - (ldblDiscountTotal);
                }
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
    
    //Added by Arsiela 05-12-2026
    //Implement this for PO Return Posting
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
    
    public JSONObject loadPurchaseOrderReturn(String formName, String supplierId, String referenceNo) {
        try {
            if (supplierId == null) {
                supplierId = "";
            }
            if (referenceNo == null) {
                referenceNo = "";
            }
            initSQL();
            String lsSQL = MiscUtil.addCondition(SQL_BROWSE, //" a.sIndstCdx = " + SQLUtil.toSQL(psIndustryId)
                    " a.sCompnyID = " + SQLUtil.toSQL(psCompanyId)
                    + " AND a.sCategrCd = "+ SQLUtil.toSQL(psCategorCd)
                    + " AND a.sSupplier LIKE " + SQLUtil.toSQL("%" + supplierId)
                    + " AND a.sTransNox LIKE " + SQLUtil.toSQL("%" + referenceNo)
            );
            switch (formName) {
                case "confirmation":
                    lsSQL = lsSQL  + " AND a.sBranchCd = " + SQLUtil.toSQL(poGRider.getBranchCode())
                                   + " AND ( a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderReturnStatus.OPEN)
                                   + " OR a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderReturnStatus.CONFIRMED) + " ) "
                                   + " AND a.cProcessd = " + SQLUtil.toSQL("0");
                    if(psIndustryId != null && !"".equals(psIndustryId)){
                        lsSQL = lsSQL + " AND a.sIndstCdx = " + SQLUtil.toSQL(psIndustryId);
                    }
                    break;
                case "history":
                    //load all purchase order return
                    lsSQL = lsSQL  + " AND a.sBranchCd = " + SQLUtil.toSQL(poGRider.getBranchCode());
                    if(psIndustryId != null && !"".equals(psIndustryId)){
                        lsSQL = lsSQL + " AND a.sIndstCdx = " + SQLUtil.toSQL(psIndustryId);
                    }
                case "posting":
                    lsSQL = lsSQL + " AND a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderReturnStatus.CONFIRMED)
                                  + " AND a.cProcessd = " + SQLUtil.toSQL("0");
                    break;
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
    
    @Override
    public JSONObject willSave()
            throws SQLException,
            GuanzonException,
            CloneNotSupportedException {
        /*Put system validations and other assignments here*/
        poJSON = new JSONObject();
        boolean lbUpdated = false;
        
        if (paDetailRemoved == null) {
            paDetailRemoved = new ArrayList<>();
        }

        if(Master().getEditMode() == EditMode.ADDNEW){
            System.out.println("Will Save : " + Master().getNextCode());
            Master().setTransactionNo(Master().getNextCode());
        }
        
        Master().setModifyingId(poGRider.Encrypt(poGRider.getUserID()));
        Master().setModifiedDate(poGRider.getServerDate());
        
        boolean lbHasQty = false;
        for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
            if (Detail(lnCtr).getQuantity().doubleValue() > 0.00) {
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
        String lsQuantity = "0.00";
        while (detail.hasNext()) {
            Model item = detail.next();
            if(item.getValue("nQuantity") != null && !"".equals(item.getValue("nQuantity"))){
                lsQuantity = item.getValue("nQuantity").toString();
            }

            if ("".equals((String) item.getValue("sStockIDx"))
                    || Double.valueOf(lsQuantity) <= 0.00) {
                detail.remove();
                paDetailRemoved.add(item);
                //TODO
//                if (item.getEditMode() == EditMode.ADDNEW) {
//                    detail.remove();
//                } else {
//                    if (!"".equals((String) item.getValue("sOrderNox")) && (String) item.getValue("sOrderNox") != null) {
//                        paDetailRemoved.add(item);
//                    }
//                    item.setValue("cReversex", PurchaseOrderReturnStatus.Reverse.EXCLUDE);
//                }
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
            if (Detail(0).getQuantity().doubleValue() == 0.00) {
                poJSON.put("result", "error");
                poJSON.put("message", "Your transaction has zero quantity.");
                return poJSON;
            }
        }
        
        if (getEditMode() == EditMode.UPDATE && !pbIsFinance) {
            PurchaseOrderReturn loRecord = new PurchaseOrderReturnControllers(poGRider, null).PurchaseOrderReturn();
            loRecord.InitTransaction();
            loRecord.OpenTransaction(Master().getTransactionNo());
            
            //Set original supplier Id
            if(!Master().getSupplierId().equals(loRecord.Master().getSupplierId())){
                Master().setSupplierId(loRecord.Master().getSupplierId());
                Master().setAddressId(loRecord.Master().getAddressId()); 
                Master().setContactId(loRecord.Master().getContactId()); 
            }
            
            //seek approval when user changed trasanction date
            if(!pbIsPrint && !pbWthParent){
                if(PurchaseOrderReturnStatus.CONFIRMED.equals(Master().getTransactionStatus())) {
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
                        setApproving((String) poJSON.get("sUserIDxx"));
                    }
                }
            
                if(PurchaseOrderReturnStatus.RETURNED.equals(Master().getTransactionStatus())){
                    lbUpdated = loRecord.getDetailCount() == getDetailCount();
                    if (lbUpdated) {
                        lbUpdated = loRecord.Master().getTransactionTotal().doubleValue() == Master().getTransactionTotal().doubleValue();
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
            }
        }

        //assign other info on detail
        for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
            if(!pbIsFinance){
                if(getReceiveQty(lnCtr).doubleValue() < Detail(lnCtr).getQuantity().doubleValue()){
                    poJSON.put("result", "error");
                    poJSON.put("message", "Return quantity cannot be greater than the receive quantity.");
                    return poJSON;
                }
            }

            //Set value to por detail
            Detail(lnCtr).setTransactionNo(Master().getTransactionNo());
            Detail(lnCtr).setEntryNo(lnCtr + 1);
            Detail(lnCtr).setModifiedDate(poGRider.getServerDate());
        }
        
        if(pbIsFinance){
            if(poJournal != null){
                if(poJournal.getEditMode() == EditMode.ADDNEW || poJournal.getEditMode() == EditMode.UPDATE){
                    poJSON = validateJournal();
                    if ("error".equals((String) poJSON.get("result"))) {
                        return poJSON;
                    }
                }
            }
        } else {
            //Allow the user to edit details but seek an approval from the approving officer
            if (PurchaseOrderReturnStatus.CONFIRMED.equals(Master().getTransactionStatus())) {
                poJSON = setValueToOthers(Master().getTransactionStatus());
                if (!"success".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }
            }
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
        ldblDebitAmt = BigDecimal.valueOf(ldblDebitAmt)
                .setScale(4, RoundingMode.HALF_UP)
                .doubleValue();
        ldblCreditAmt = BigDecimal.valueOf(ldblCreditAmt)
                .setScale(4, RoundingMode.HALF_UP)
                .doubleValue();
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
        
        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }

    
    private static String xsDateShort(Date fdValue) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String date = sdf.format(fdValue);
        return date;
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
            
            //Save Serial Ledger, Inventory
//            if (PurchaseOrderReturnStatus.CONFIRMED.equals(Master().getTransactionStatus())) {
//                poJSON = saveUpdateOthers(PurchaseOrderReturnStatus.CONFIRMED);
//                if (!"success".equals((String) poJSON.get("result"))) {
//                    return poJSON;
//                }
//            }

        } catch (SQLException | GuanzonException | CloneNotSupportedException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
            return poJSON;
        } catch (Exception ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
            return poJSON;
        }
        poJSON.put("result", "success");
        return poJSON;
    }
    
    /**
     * Validate value of return quantity
     * @param status
     * @return
     * @throws CloneNotSupportedException
     * @throws SQLException
     * @throws GuanzonException 
     */
    private JSONObject setValueToOthers(String status)
            throws CloneNotSupportedException,
            SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        paInventoryTransaction = new ArrayList<>();
        int lnCtr;
        double lnRetQty = 0;
        double lnRecQty = 0;

        for (lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
            System.out.println("----------------------PURCHASE ORDER RETURN DETAIL---------------------- ");
            System.out.println("TransNo : " + (lnCtr + 1) + " : " + Detail(lnCtr).getTransactionNo());
            System.out.println("SerialId : " + (lnCtr + 1) + " : " + Detail(lnCtr).getSerialId());
            System.out.println("StockId : " + (lnCtr + 1) + " : " + Detail(lnCtr).getStockId());
            System.out.println("------------------------------------------------------------------ ");
            
            lnRecQty = getReceiveQty(lnCtr).doubleValue();
            switch (status) {
                case PurchaseOrderReturnStatus.CONFIRMED:
//                case PurchaseOrderReturnStatus.PAID:
//                case PurchaseOrderReturnStatus.POSTED:
                    //Total return qty for specific po receiving
                    lnRetQty = getReturnQty( Detail(lnCtr).getStockId(),Detail(lnCtr).getSerialId(), true).doubleValue();
                    lnRetQty = lnRetQty + Detail(lnCtr).getQuantity().doubleValue();

                    if(lnRetQty > lnRecQty){
                        poJSON.put("result", "error");
//                        poJSON.put("message", "Confirmed return quantity cannot be greater than the receive quantity for PO Receiving No. " + Master().getSourceNo() 
//                                + ".\nHas been linked to PO Return No. " + psTransNox);
                        poJSON.put("message", 
                                    "The confirmed return quantity cannot exceed the received quantity for PO Receiving No. " + Master().getSourceNo() +
                                    ".\nThis transaction is linked to PO Return No. " + psTransNox + ".");

                        psTransNox = "";
                        return poJSON;
                    }
                    break;
            }
            
            //Inventory Transaction
//            updateInventoryTransaction(status, Detail(lnCtr).getStockId(), Detail(lnCtr).getQuantity().doubleValue());
        }

        //Update inventory removed in purchase order return
//        for (lnCtr = 0; lnCtr <= getDetailRemovedCount() - 1; lnCtr++) {
//            //Inventory Transaction 
//            updateInventoryTransaction(status, DetailRemove(lnCtr).getStockId(), DetailRemove(lnCtr).getQuantity().doubleValue());
//        }

        poJSON.put("result", "success");
        return poJSON;
    }

    //TODO
//    private void updateInventoryTransaction(String status, String stockId, double quantity)
//            throws GuanzonException,
//            SQLException,
//            CloneNotSupportedException {
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
//                        if(lnQtyOnHnd < 0){
////                        poJSON.put("result", "error");
////                        poJSON.put("message", "Descrepancy for quantity on hand of Barcode <" + paInventoryTransaction.get(lnList).Master().getBarcode() + ">.\n Contact System Administrator.");
////                        return poJSON;
////                    }

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
//    }
    
    

//    private JSONObject saveUpdateOthers(String status)
//            throws CloneNotSupportedException {
//        /*Only modify this if there are other tables to modify except the master and detail tables*/
//        poJSON = new JSONObject();
//        int lnCtr, lnRow;
//        try {
//            InvSerial loInvSerial = new InvControllers(poGRider, logwrapr).InventorySerial();
//            loInvSerial.setWithParentClass(true);
//            for(lnCtr = 0; lnCtr < getDetailCount(); lnCtr++ ){
//                if(Detail(lnCtr).getSerialId() != null && !"".equals(Detail(lnCtr).getSerialId())){
//                    poJSON = loInvSerial.openRecord(Detail(lnCtr).getSerialId());
//                    if ("error".equals((String) poJSON.get("result"))) {
//                        return poJSON;
//                    }
//                    System.out.println(loInvSerial.getEditMode());
//                    poJSON = loInvSerial.updateRecord();
//                    if ("error".equals((String) poJSON.get("result"))) {
//                        return poJSON;
//                    }
//                    if(status.equals(PurchaseOrderReturnStatus.CONFIRMED) 
//                            || status.equals(PurchaseOrderReturnStatus.POSTED)
//                            || status.equals(PurchaseOrderReturnStatus.PAID)){
//                        loInvSerial.getModel().setLocation("2"); //Return to Supplier
//                    } else {
//                        loInvSerial.getModel().setLocation("1"); 
//                    }
//                    poJSON = loInvSerial.saveRecord();
//                    if ("error".equals((String) poJSON.get("result"))) {
//                        return poJSON;
//                    }
//                    
//                }
//            }
//            
//            //1. Save Inventory Transaction TODO
//            for (lnCtr = 0; lnCtr <= paInventoryTransaction.size() - 1; lnCtr++) {
////                paInventoryTransaction.get(lnCtr).Master().setModifiedDate(poGRider.getServerDate());
////                paInventoryTransaction.get(lnCtr).setWithParent(true);
////                poJSON = paInventoryTransaction.get(lnCtr).SaveTransaction();
//                if ("error".equals((String) poJSON.get("result"))) {
//                    System.out.println("Inventory Transaction Saving " + (String) poJSON.get("message"));
//                    return poJSON;
//                }
//            }
//
//            //3. Save Inventory Serial Ledger TODO
//            if (PurchaseOrderReturnStatus.CONFIRMED.equals(Master().getTransactionStatus())
//                    || PurchaseOrderReturnStatus.POSTED.equals(Master().getTransactionStatus())) {
////                InvSerial loInvSerial = new InvControllers(poGRider, logwrapr).InventorySerial();
////                loInvSerial.setWithParentClass(true);
//                //            InventoryTrans.POReturn();
//            }
//
//        } catch (SQLException | GuanzonException ex) {
//            Logger.getLogger(PurchaseOrderReturn.class.getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
//            poJSON.put("result", "error");
//            poJSON.put("message", MiscUtil.getException(ex));
//            return poJSON;
//        }
//        poJSON.put("result", "success");
//        return poJSON;
//    }
    
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
    
    //Added by Arsiela 05-12-2026
    //Implement this for PO Return Posting
    public Journal Journal(){
        try {
            if(poJournal == null){
                poJournal = new CashflowControllers(poGRider, logwrapr).Journal();
                poJournal.InitTransaction();
            }
            
        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
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
                jsondetail.put("PO_Return_Master", jsonmaster);
                jsondetail.put("PO_Return_Detail", jsondetails);

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
                poJournal.Master().setDepartmentId(Master().PurchaseOrderReceivingMaster().getDepartmentId());
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
        poJSON.put("row", fnRow);
        poJSON.put("result", "success");
        return poJSON;
    }
    
    private String existJournal() throws SQLException{
        Model_Journal_Master loMaster = new CashflowModels(poGRider).Journal_Master();
        String lsSQL = MiscUtil.makeSelect(loMaster);
        lsSQL = MiscUtil.addCondition(lsSQL,
                " sSourceNo = " + SQLUtil.toSQL(Master().getTransactionNo())
                + " AND sSourceCD = " + SQLUtil.toSQL(getSourceCode())
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
    
    private boolean pbVatableExist = false;
    private JSONObject populateCachePayable() throws SQLException, GuanzonException, CloneNotSupportedException{
        double ldblTotalDiscAmt = Master().getDiscount().doubleValue() + (Master().getTransactionTotal().doubleValue() * (Master().getDiscountRate().doubleValue() / 100));
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
//            ldblVatAmountDetail = ldblDetTotal - (ldblDetTotal / 1.12);
            ldblGrossAmt += ldblDetTotal;   
            
            if(Detail(lnCtr).isVatable()){
                pbVatableExist = true;
                if(Master().isVatTaxable()){
                    ldblVatAmountDetail = ldblDetTotal - (ldblDetTotal / 1.12);
                } else {
                    ldblVatAmountDetail = ldblDetTotal * 0.12;
                    ldblDetTotal = ldblDetTotal + ldblVatAmountDetail; 
                }
            }
            
            ldblTotal = ldblDetTotal;
            
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
            
            if(Detail(lnCtr).Inventory().getInventoryTypeId() == null || "".equals(Detail(lnCtr).Inventory().getInventoryTypeId())){
                poJSON.put("result", "error");
                poJSON.put("message", "Inventory type cannot be empty at row "+(lnCtr+1)+".\nContact the system administrator for assistance.");
                return poJSON;
            }
            
            poCachePayable.Detail(lnCacheRow).setTransactionType(Detail(lnCtr).Inventory().getInventoryTypeId());
            poCachePayable.Detail(lnCacheRow).setGrossAmount(ldblTotal); 
            poCachePayable.Detail(lnCacheRow).setReceivables(ldblTotal); 
            lbExist = false;
        }
        
        //Update cache payable
        for(int lnCtr = 0; lnCtr <= poCachePayable.getDetailCount()-1; lnCtr++){
            //Update Discount
            if(ldblTotalDiscAmt >= poCachePayable.Detail(lnCtr).getReceivables()){
                poCachePayable.Detail(lnCtr).setDiscountAmount(poCachePayable.Detail(lnCtr).getReceivables());
                ldblTotalDiscAmt = ldblTotalDiscAmt - poCachePayable.Detail(lnCtr).getReceivables();
            } else {
                if( ldblTotalDiscAmt > 0.0000){
                    poCachePayable.Detail(lnCtr).setDiscountAmount(ldblTotalDiscAmt);
                    ldblTotalDiscAmt = 0.0000;
                }
            }
        }
        
        Double ldblVatAmount = 0.0000;
        Double ldblVatSales = 0.0000;
        Double ldblNetTotal = 0.0000;
        Double ldblFreight = 0.0000;
        Double ldblFreightVatAmount = 0.0000;
//        if((Master().getTruckingId() == null || "".equals(Master().getTruckingId())) 
//            || (Master().getSupplierId().equals(Master().getTruckingId())) ){
            ldblVatAmount = Master().getVatAmount().doubleValue();
            ldblVatSales = Master().getVatSales().doubleValue();
            ldblFreight = Master().getFreight().doubleValue();
            ldblNetTotal = getNetTotal();
            
//        } 
//        else {
//            //If all items are non vatable no vatable sales and vat amount will be computed.
//            if(pbVatableExist){
//                if(Master().isVatTaxable()){
//                    ldblFreightVatAmount = Master().getFreight().doubleValue() - (Master().getFreight().doubleValue() / 1.12);
//                    ldblVatAmount = Master().getVatAmount().doubleValue() - ldblFreightVatAmount;
//                    ldblVatSales = Master().getVatSales().doubleValue() - ( Master().getFreight().doubleValue() - ldblFreightVatAmount);
////                    ldblNetTotal =  getNetTotal() - (Master().getFreight().doubleValue() - ldblFreightVatAmount);
//                    ldblNetTotal =  getNetTotal() - Master().getFreight().doubleValue();
//                } else {
//                    ldblFreightVatAmount = Master().getFreight().doubleValue() * 0.12;
//                    ldblVatAmount = Master().getVatAmount().doubleValue() - ldblFreightVatAmount;
//                    ldblVatSales = Master().getVatSales().doubleValue() - Master().getFreight().doubleValue();
//                    ldblNetTotal =  getNetTotal() - (Master().getFreight().doubleValue() + ldblFreightVatAmount);
//                }
//            } else {
//                ldblNetTotal = getNetTotal() - Master().getFreight().doubleValue();
//            }
//        }
        
        //Cache Payable Master
        poCachePayable.Master().setDiscountAmount(Master().getDiscount().doubleValue() + (ldblGrossAmt * (Master().getDiscountRate().doubleValue() / 100)));
        poCachePayable.Master().setIndustryCode(Master().getIndustryId());
        poCachePayable.Master().setBranchCode(Master().getBranchCode());
        poCachePayable.Master().setTransactionDate(poGRider.getServerDate()); 
        poCachePayable.Master().setCompanyId(Master().getCompanyId());
        poCachePayable.Master().setClientId(Master().getSupplierId());
        poCachePayable.Master().setSourceCode(getSourceCode());
        poCachePayable.Master().setSourceNo(Master().getTransactionNo());
        poCachePayable.Master().setReferNo(Master().PurchaseOrderReceivingMaster().getReferenceNo()); 
        poCachePayable.Master().setGrossAmount(ldblGrossAmt); 
        poCachePayable.Master().setVATExempt(Master().getVatExemptSales().doubleValue());
        poCachePayable.Master().setZeroRated(Master().getZeroVatSales().doubleValue());
        poCachePayable.Master().setTaxAmount(Master().getWithHoldingTax().doubleValue());
        poCachePayable.Master().setFreight(Master().getFreight().doubleValue());
        poCachePayable.Master().setVATSales(ldblVatSales);
        poCachePayable.Master().setVATAmount(ldblVatAmount);
        poCachePayable.Master().setNetTotal(ldblNetTotal); 
        poCachePayable.Master().setReceivables(ldblNetTotal); 
        poCachePayable.Master().setTransactionStatus(CachePayableStatus.CONFIRMED); //set to 1
        poCachePayable.Master().setModifyingId(poGRider.Encrypt(poGRider.getUserID()));
        poCachePayable.Master().setModifiedDate(poGRider.getServerDate());
        
        poJSON.put("result", "success");
        poJSON.put("message", "success");
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
                + " FROM PO_Return_Master a "
                + " LEFT JOIN Client_Master b ON b.sClientID = a.sSupplier "
                + " LEFT JOIN Company c ON c.sCompnyID = a.sCompnyID "
                + " LEFT JOIN Industry d ON d.sIndstCdx = a.sIndstCdx ";
    }
    
    @Override
    protected JSONObject isEntryOkay(String status) {
        GValidator loValidator = PurchaseOrderReturnValidatorFactory.make(Master().getIndustryId());

        loValidator.setApplicationDriver(poGRider);
        loValidator.setTransactionStatus(status);
        loValidator.setMaster(poMaster);

        poJSON = loValidator.validate();

        return poJSON;
    }
    
    public JSONObject getConfirmedBy() throws SQLException, GuanzonException {
        String lsConfirm = "";
        String lsDate = "";
        String lsSQL = "SELECT b.sModified,b.dModified FROM " + Master().getTable() +" a "
                     + " LEFT JOIN Transaction_Status_History b ON b.sSourceNo = a.sTransNox AND b.sTableNme = "+ SQLUtil.toSQL(Master().getTable()) 
                     + " AND ( b.cRefrStat = "+ SQLUtil.toSQL(PurchaseOrderReturnStatus.CONFIRMED) 
                     + " OR (ASCII(b.cRefrStat) - 64)  = "+ SQLUtil.toSQL(PurchaseOrderReturnStatus.CONFIRMED) + " )";
        lsSQL = MiscUtil.addCondition(lsSQL, " a.sTransNox = " + SQLUtil.toSQL(Master().getTransactionNo())) ;
        lsSQL = lsSQL + " ORDER BY b.dModified DESC ";
        System.out.println("Execute SQL : " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        try {
          if (MiscUtil.RecordCount(loRS) > 0L) {
            if (loRS.next()) {
                if(loRS.getString("sModified") != null && !"".equals(loRS.getString("sModified"))){
                    if(loRS.getString("sModified").length() > 10){
                        lsConfirm = getSysUser(poGRider.Decrypt(loRS.getString("sModified"))); 
                    } else {
                        lsConfirm = getSysUser(loRS.getString("sModified")); 
                    }
                    // Get the LocalDateTime from your result set
                    LocalDateTime dModified = loRS.getObject("dModified", LocalDateTime.class);
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss");
                    lsDate =  dModified.format(formatter);
                }
            } 
          }
          MiscUtil.close(loRS);
        } catch (SQLException e) {
          poJSON.put("result", "error");
          poJSON.put("message", e.getMessage());
          return poJSON;
        } 
        
        poJSON.put("result", "success");
        poJSON.put("sConfirmed", lsConfirm);
        poJSON.put("sConfrmDte", lsDate);
        return poJSON;
    }
    
    private CustomJasperViewer poViewer = null;
    private String psTransactionNo = "";

    public JSONObject printRecord(Runnable onPrintedCallback) {
        poJSON = new JSONObject();
        String watermarkPath = "D:\\GGC_Maven_Systems\\Reports\\images\\draft.png"; //set draft as default
        psTransactionNo = Master().getTransactionNo();
        try {
            System.out.println("Edit Mode : " + getEditMode());
            System.out.println("TransactionNo : " + Master().getTransactionNo());
            //Reopen Transaction to get the accurate data
            poJSON = OpenTransaction(psTransactionNo);
            if ("error".equals((String) poJSON.get("result"))) {
                System.out.println("Print Record open transaction : " + (String) poJSON.get("message"));
                poJSON.put("message", "Printing of the transaction was aborted.\n" + (String) poJSON.get("message"));
                return poJSON;
            }
            
            // 1. Prepare parameters
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("sSupplierNm", Master().Supplier().getCompanyName());
            parameters.put("sBranchNm", poGRider.getBranchName());
            parameters.put("sAddressx", poGRider.getAddress());
            parameters.put("sCompnyNm", Master().Company().getCompanyName());
            parameters.put("sTransNox", Master().getTransactionNo());
            parameters.put("dReferDte", Master().PurchaseOrderReceivingMaster().getTransactionDate());
            parameters.put("sReferNox", Master().getSourceNo());
            parameters.put("sRemarks", Master().getRemarks());
            parameters.put("dTransDte", new java.sql.Date(Master().getTransactionDate().getTime()));
            parameters.put("dDatexxx", new java.sql.Date(poGRider.getServerDate().getTime()));
            parameters.put("sPreparedBy", "");
            parameters.put("sApprovedBy", "");
            
            JSONObject loJSONEntry = getEntryBy();
            if("error".equals((String) loJSONEntry.get("result"))){
                return loJSONEntry;
            }
            
            if((String) loJSONEntry.get("sCompnyNm") != null && !"".equals((String) loJSONEntry.get("sCompnyNm"))){
                parameters.put("sPreparedBy", "Prepared by: "+ (String) loJSONEntry.get("sCompnyNm") + " " + String.valueOf((String) loJSONEntry.get("sEntryDte"))); 
            }
            
            JSONObject loJSONConfirm = getConfirmedBy();
            if("error".equals((String) loJSONConfirm.get("result"))){
                return loJSONConfirm;
            } else {
                if((String) loJSONConfirm.get("sConfirmed") != null && !"".equals((String) loJSONConfirm.get("sConfirmed"))){
                    parameters.put("sApprovedBy", "Confirmed by: "+ (String) loJSONConfirm.get("sConfirmed") + " " + String.valueOf((String) loJSONConfirm.get("sConfrmDte"))); 
                }
            }

            // Set watermark based on approval status
            switch (Master().getTransactionStatus()) {
                case PurchaseOrderReturnStatus.CONFIRMED:
                case PurchaseOrderReturnStatus.PAID:
                case PurchaseOrderReturnStatus.POSTED:
                    if("1".equals(Master().getPrint())){
                        watermarkPath = "D:\\GGC_Maven_Systems\\Reports\\images\\approvedreprint.png";
                    } else {
                        watermarkPath = "D:\\GGC_Maven_Systems\\Reports\\images\\approved.png";
                    }
                    break;
//                case PurchaseOrderReturnStatus.CANCELLED:
//                    watermarkPath = "D:\\GGC_Maven_Systems\\Reports\\images\\cancelled.png";
//                    break;
            }

            parameters.put("watermarkImagePath", watermarkPath);
            List<TransactionDetail> transDetails = new ArrayList<>();
            String jrxmlPath = "D:\\GGC_Maven_Systems\\Reports\\PurchaseOrderReturn.jrxml";//Food and Non Serialize

            double lnTotal = 0.0;
            int lnRow = 1;
            String lsDescription = "";
            String lsBarcode = "";
            String lsMeasure = "";
            for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
                lnTotal = Detail(lnCtr).getUnitPrce().doubleValue() * Detail(lnCtr).getQuantity().doubleValue();
                
                System.out.println("--------------------------------------------------------------------");
                System.out.println("DETAIL ROW : " + lnCtr + 1);
                System.out.println("BARCODE : " + Detail(lnCtr).Inventory().getBarCode());
                System.out.println("BRAND : " + Detail(lnCtr).Inventory().Brand().getDescription());
                System.out.println("MODEL : " + Detail(lnCtr).Inventory().Model().getDescription());
                System.out.println("COLOR : " + Detail(lnCtr).Inventory().Color().getDescription());
                System.out.println("MEASURE : " + Detail(lnCtr).Inventory().Measure().getDescription());
                System.out.println("DESCRIPTION : " + Detail(lnCtr).Inventory().getDescription());
                System.out.println("VARIANT : " + Detail(lnCtr).Inventory().Variant().getDescription());
                System.out.println("YEAR MODEL : " + Detail(lnCtr).Inventory().Variant().getYearModel());
                System.out.println("--------------------------------------------------------------------");
                
                switch(Master().getCategoryCode()){
                    case PurchaseOrderReturnStatus.Category.CAR: //"0005": //CAR
                    case PurchaseOrderReturnStatus.Category.MOTORCYCLE: //"0003": //Motorcycle
                    case PurchaseOrderReturnStatus.Category.MOBILEPHONE: //"0001": //Cellphone   
                    case PurchaseOrderReturnStatus.Category.APPLIANCES: //"0002": //Appliances  
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
                        if(lsDescription != null){
                            lsDescription = lsDescription.trim();
                        }
                        
                        transDetails.add(new TransactionDetail(lnRow, Master().getSourceNo(), lsBarcode, 
                                lsDescription + "\n" + Detail(lnCtr).InventorySerial().getSerial01(), Detail(lnCtr).getUnitPrce().doubleValue(), 
                                Detail(lnCtr).getQuantity().doubleValue(), lnTotal));
                        jrxmlPath = "D:\\GGC_Maven_Systems\\Reports\\PurchaseOrderReturn_Serialize.jrxml";
                    break;
                    case PurchaseOrderReturnStatus.Category.FOOD: //"0008": // Food  
                        lsBarcode = Detail(lnCtr).Inventory().getBarCode();
                        lsDescription = Detail(lnCtr).Inventory().Brand().getDescription() 
                                + " " + Detail(lnCtr).Inventory().getDescription();
                        if(lsDescription != null){
                            lsDescription = lsDescription.trim().toUpperCase();
                        }
                        if (Detail(lnCtr).Inventory().Measure().getDescription() != null && !"".equals(Detail(lnCtr).Inventory().Measure().getDescription())){
                            lsMeasure = Detail(lnCtr).Inventory().Measure().getDescription();
                        }
                        
                        transDetails.add(new TransactionDetail(lnRow, Master().getSourceNo(), 
                                lsBarcode, lsDescription,lsMeasure ,Detail(lnCtr).getUnitPrce().doubleValue(), Detail(lnCtr).getQuantity().doubleValue(), lnTotal));
                    break;
                    case PurchaseOrderReturnStatus.Category.SPCAR: //"0006": // CAR SP
                    case PurchaseOrderReturnStatus.Category.SPMC: //"0004": // Motorcycle SP
                    case PurchaseOrderReturnStatus.Category.GENERAL: //"0007": // General
                    case PurchaseOrderReturnStatus.Category.HOSPITALITY: //"0009": // Hospitality
                    default:
                        lsBarcode = Detail(lnCtr).Inventory().getBarCode();
                        
                        //Concat Description
                        if(Detail(lnCtr).Inventory().Brand().getDescription() != null && !"".equals(Detail(lnCtr).Inventory().Brand().getDescription())){
                            lsDescription = Detail(lnCtr).Inventory().Brand().getDescription();
                        }
                        if(Detail(lnCtr).Inventory().Model().getDescription() != null && !"".equals(Detail(lnCtr).Inventory().Model().getDescription())){
                            lsDescription = lsDescription + " " + Detail(lnCtr).Inventory().Model().getDescription();
                        }
                        if(Detail(lnCtr).Inventory().Color().getDescription() != null && !"".equals(Detail(lnCtr).Inventory().Color().getDescription())){
                            lsDescription = lsDescription + " " + Detail(lnCtr).Inventory().Color().getDescription();
                        }
                        if(Detail(lnCtr).Inventory().getDescription() != null && !"".equals(Detail(lnCtr).Inventory().getDescription())){
                            lsDescription = lsDescription + " " + Detail(lnCtr).Inventory().getDescription();
                        }
                        
                        if(lsDescription != null){
                            lsDescription = lsDescription.trim().toUpperCase();
                        }
                        
                        if (Detail(lnCtr).Inventory().Measure().getDescription() != null && !"".equals(Detail(lnCtr).Inventory().Measure().getDescription())){
                            lsMeasure = Detail(lnCtr).Inventory().Measure().getDescription();
                        }
                        
                        transDetails.add(new TransactionDetail(lnRow, Master().getSourceNo(), 
                                lsBarcode, lsDescription,lsMeasure ,Detail(lnCtr).getUnitPrce().doubleValue(), Detail(lnCtr).getQuantity().doubleValue(), lnTotal));
//                        transDetails.add(new TransactionDetail(lnRow, Master().getSourceNo(), lsBarcode, 
//                                lsDescription, Detail(lnCtr).getUnitPrce().doubleValue(), 
//                                Detail(lnCtr).getQuantity().doubleValue(), lnTotal));
                        break;
                }
                
//                lnTotal = Detail(lnCtr).getUnitPrce().doubleValue() * Detail(lnCtr).getQuantity().doubleValue();
//                transDetails.add(new TransactionDetail(lnRow, Master().getSourceNo(), Detail(lnCtr).Inventory().getBarCode(), 
//                        Detail(lnCtr).Inventory().getDescription(), Detail(lnCtr).getUnitPrce().doubleValue(), 
//                        Detail(lnCtr).getQuantity().doubleValue(), lnTotal));
                lnRow++;
                lsDescription = "";
                lsBarcode = "";
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
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(transDetails);

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
            Logger.getLogger(PurchaseOrderReturn.class.getName()).log(Level.SEVERE, null, ex);
        }

        return poJSON;
    }

    public static class TransactionDetail {

        private Integer nRowNo;
        private String sOrderNo;
        private String sBarcode;
        private String sDescription;
        private String sMeasure;
        private double nUprice;
        private double nOrder;
        private double nTotal;

        public TransactionDetail(Integer rowNo, String orderNo, String barcode, String description,
                double uprice, double order, double total) {
            this.nRowNo = rowNo;
            this.sOrderNo = orderNo;
            this.sBarcode = barcode;
            this.sDescription = description;
            this.nUprice = uprice;
            this.nOrder = order;
            this.nTotal = total;
        }
        
        public TransactionDetail(Integer rowNo, String orderNo, String barcode, String description, String measure,
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

        public double getnUprice() {
            return nUprice;
        }
        
        public String getsMeasure() {
            return sMeasure;
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
                        ShowMessageFX.Warning(null, "Print Purchase Order Return", "Printing of the transaction was aborted.\n" + (String) poJSON.get("message"));
                        SwingUtilities.invokeLater(() -> CustomJasperViewer.this.toFront());
                    });
                    fbIsPrinted = false;
                }
                
                if (PurchaseOrderReturnStatus.CONFIRMED.equals(Master().getTransactionStatus())
                        || PurchaseOrderReturnStatus.POSTED.equals(Master().getTransactionStatus())
                        || PurchaseOrderReturnStatus.PAID.equals(Master().getTransactionStatus())) {
                    poJSON = UpdateTransaction();
                    if ("error".equals((String) poJSON.get("result"))) {
                        Platform.runLater(() -> {
                            ShowMessageFX.Warning(null, "Print Purchase Order Return", "Printing of the transaction was aborted.\n" + (String) poJSON.get("message"));
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
                            ShowMessageFX.Warning(null, "Print Purchase Order Return", "Printing of the transaction was aborted.\n" + (String) poJSON.get("message"));
                            SwingUtilities.invokeLater(() -> CustomJasperViewer.this.toFront());
                        });
                        fbIsPrinted = false;
                    }

                    pbIsPrint = false;
                }
            }

            if (fbIsPrinted) {
                Platform.runLater(() -> {
                    ShowMessageFX.Information(null, "Print Purchase Order Return", "Transaction printed successfully.");
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
    
    public void ShowStatusHistory() throws SQLException, GuanzonException, Exception{
        CachedRowSet crs = getStatusHistory();
        
        crs.beforeFirst();
        
        while(crs.next()){
            switch (crs.getString("cRefrStat")){
                case "":
                    crs.updateString("cRefrStat", "-");
                    break;
                case PurchaseOrderReturnStatus.OPEN:
                    crs.updateString("cRefrStat", "OPEN");
                    break;
                case PurchaseOrderReturnStatus.CONFIRMED:
                    crs.updateString("cRefrStat", "CONFIRMED");
                    break;
                case PurchaseOrderReturnStatus.POSTED:
                    crs.updateString("cRefrStat", "POSTED");
                    break;
                case PurchaseOrderReturnStatus.CANCELLED:
                    crs.updateString("cRefrStat", "CANCELLED");
                    break;
                case PurchaseOrderReturnStatus.VOID:
                    crs.updateString("cRefrStat", "VOID");
                    break;
                case PurchaseOrderReturnStatus.PAID:
                    crs.updateString("cRefrStat", "PAID");
                    break;
                case PurchaseOrderReturnStatus.RETURNED:
                    crs.updateString("cRefrStat", "RETURNED");
                    break;
                default:
                    char ch = crs.getString("cRefrStat").charAt(0);
                    String stat = String.valueOf((int) ch - 64);
                    
                    switch (stat){
                        case PurchaseOrderReturnStatus.OPEN:
                            crs.updateString("cRefrStat", "OPEN");
                            break;
                        case PurchaseOrderReturnStatus.CONFIRMED:
                            crs.updateString("cRefrStat", "CONFIRMED");
                            break;
                        case PurchaseOrderReturnStatus.POSTED:
                            crs.updateString("cRefrStat", "POSTED");
                            break;
                        case PurchaseOrderReturnStatus.CANCELLED:
                            crs.updateString("cRefrStat", "CANCELLED");
                            break;
                        case PurchaseOrderReturnStatus.VOID:
                            crs.updateString("cRefrStat", "VOID");
                            break;
                        case PurchaseOrderReturnStatus.PAID:
                            crs.updateString("cRefrStat", "PAID");
                            break;
                        case PurchaseOrderReturnStatus.RETURNED:
                            crs.updateString("cRefrStat", "RETURNED");
                            break;
                    }
            }
            crs.updateRow(); 
        }
        
        JSONObject loJSON  = getEntryBy();
        String entryBy = "";
        String entryDate = "";
        
        if ("success".equals((String) loJSON.get("result"))){
            entryBy = (String) loJSON.get("sCompnyNm");
            entryDate = (String) loJSON.get("sEntryDte");
        }
        
        showStatusHistoryUI("Purchase Order Return", (String) poMaster.getValue("sTransNox"), entryBy, entryDate, crs);
    }
    
    public JSONObject getEntryBy() throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        String lsEntry = "";
        String lsEntryDate = "";
        String lsSQL =  " SELECT b.sModified, b.dModified " 
                        + " FROM PO_Return_Master a "
                        + " LEFT JOIN xxxAuditLogMaster b ON b.sSourceNo = a.sTransNox AND b.sEventNme LIKE 'ADD%NEW' AND b.sRemarksx = " + SQLUtil.toSQL(Master().getTable());
        lsSQL = MiscUtil.addCondition(lsSQL, " a.sTransNox =  " + SQLUtil.toSQL(Master().getTransactionNo())) ;
        lsSQL = lsSQL + " ORDER BY b.dModified DESC ";
        System.out.println("Execute SQL : " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        try {
          if (MiscUtil.RecordCount(loRS) > 0L) {
            if (loRS.next()) {
                if(loRS.getString("sModified") != null && !"".equals(loRS.getString("sModified"))){
                    if(loRS.getString("sModified").length() > 10){
                        lsEntry = getSysUser(poGRider.Decrypt(loRS.getString("sModified"))); 
                    } else {
                        lsEntry = getSysUser(loRS.getString("sModified")); 
                    }
                    // Get the LocalDateTime from your result set
                    LocalDateTime dModified = loRS.getObject("dModified", LocalDateTime.class);
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss");
                    lsEntryDate =  dModified.format(formatter);
                }
            } 
          }
          MiscUtil.close(loRS);
        } catch (SQLException e) {
          poJSON.put("result", "error");
          poJSON.put("message", e.getMessage());
          return poJSON;
        } 
        
        poJSON.put("result", "success");
        poJSON.put("sCompnyNm", lsEntry);
        poJSON.put("sEntryDte", lsEntryDate);
        return poJSON;
    }
    
    public String getSysUser(String fsId) throws SQLException, GuanzonException {
        String lsEntry = "";
        String lsSQL =   " SELECT b.sCompnyNm from xxxSysUser a " 
                       + " LEFT JOIN Client_Master b ON b.sClientID = a.sEmployNo ";
        lsSQL = MiscUtil.addCondition(lsSQL, " a.sUserIDxx =  " + SQLUtil.toSQL(fsId)) ;
        System.out.println("SQL " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        try {
          if (MiscUtil.RecordCount(loRS) > 0L) {
            if (loRS.next()) {
                lsEntry = loRS.getString("sCompnyNm");
            } 
          }
          MiscUtil.close(loRS);
        } catch (SQLException e) {
          poJSON.put("result", "error");
          poJSON.put("message", e.getMessage());
        } 
        return lsEntry;
    }
    
}
