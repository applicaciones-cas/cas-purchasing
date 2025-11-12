package org.guanzon.cas.purchasing.controller;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionListener;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
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
import org.guanzon.cas.client.Client;
import org.guanzon.cas.client.account.AP_Client_Master;
import org.guanzon.cas.client.services.ClientControllers;
import org.guanzon.cas.inv.InvTransCons;
import org.guanzon.cas.inv.Inventory;
import org.guanzon.cas.inv.InventoryTransaction;
//import org.guanzon.cas.inv.Inventory;
import org.guanzon.cas.inv.services.InvControllers;
import org.guanzon.cas.inv.warehouse.StockRequest;
import org.guanzon.cas.inv.warehouse.model.Model_Inv_Stock_Request_Master;
import org.guanzon.cas.inv.warehouse.services.InvWarehouseControllers;
import org.guanzon.cas.inv.warehouse.status.StockRequestStatus;
import org.guanzon.cas.parameter.Branch;
import org.guanzon.cas.parameter.Brand;
import org.guanzon.cas.parameter.Company;
import org.guanzon.cas.parameter.Industry;
import org.guanzon.cas.parameter.Term;
import org.guanzon.cas.parameter.services.ParamControllers;
import org.guanzon.cas.purchasing.model.Model_PO_Detail;
import org.guanzon.cas.purchasing.model.Model_PO_Master;
import org.guanzon.cas.purchasing.services.PurchaseOrderControllers;
import org.guanzon.cas.purchasing.services.PurchaseOrderModels;
import org.guanzon.cas.purchasing.status.PurchaseOrderStaticData;
import org.guanzon.cas.purchasing.status.PurchaseOrderStatus;
import org.guanzon.cas.purchasing.validator.PurchaseOrderValidatorFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import ph.com.guanzongroup.cas.cashflow.Payee;
import ph.com.guanzongroup.cas.cashflow.services.CashflowControllers;
import org.guanzon.cas.purchasing.services.QuotationControllers;
import org.guanzon.cas.purchasing.status.POQuotationStatus;

public class PurchaseOrder extends Transaction {

    List<Model_Inv_Stock_Request_Master> paStockRequest;
    List<Model_PO_Master> paPOMaster;
    List<StockRequest> poStockRequest;
    List<POQuotation> poPOQuotation;
    List<POQuotation> poPOQuotationStatus;
    List<POQuotation> poPOQuotationRemovedStatus;
    List<Model> paDetailRemoved;
    CashflowControllers poPaymentRequest;
    String PayeeID;
    private boolean pbApproval = false;

    public JSONObject InitTransaction() {
        SOURCE_CODE = InvTransCons.PURCHASE_ORDER;
        poMaster = new PurchaseOrderModels(poGRider).PurchaseOrderMaster();
        poDetail = new PurchaseOrderModels(poGRider).PurchaseOrderDetails();
        paDetail = new ArrayList<>();
        poStockRequest = new ArrayList<>();
        poPOQuotation = new ArrayList<>();
        poPOQuotationStatus = new ArrayList<>();
        poPOQuotationRemovedStatus = new ArrayList<>();

        return initialize();
    }
    
    /**
     * Seek Approval method 
     * @return JSON
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
            //show process needs authorization
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
                 poJSON.put("message", "User is not an authorized approving officer..");
                 return poJSON;
            }
        }  
        
        poJSON.put("result", "success");
        return poJSON;
    }

    @Override
    public JSONObject initFields() {
        /*Put initial model values here*/
        poJSON = new JSONObject();

        poJSON.put("result", "success");
        return poJSON;
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

    private StockRequest StockRequest() {
        return new InvWarehouseControllers(poGRider, logwrapr).StockRequest();
    }
    
    private POQuotation POQuotation() {
        return new QuotationControllers(poGRider, logwrapr).POQuotation();
    }

    public Model_Inv_Stock_Request_Master InvStockRequestMaster(int row) {
        return (Model_Inv_Stock_Request_Master) paStockRequest.get(row);
    }

    public int getInvStockRequestCount() {
        return this.paStockRequest.size();
    }

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

    public int getDetailRemovedCount() {
        if (paDetailRemoved == null) {
            paDetailRemoved = new ArrayList<>();
        }

        return paDetailRemoved.size();
    }

    public Model_PO_Detail DetailRemove(int row) {
        return (Model_PO_Detail) paDetailRemoved.get(row);
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

    public JSONObject NewTransaction() throws CloneNotSupportedException, SQLException, GuanzonException {
        if(!pbWthParent){
            //Replaced script above by calling of method Arsiela 10-15-2025 09:25:01
            poJSON = seekApproval();
            if("error".equalsIgnoreCase((String)poJSON.get("result"))){
                return poJSON;
            }
        }
        
        return newTransaction();
    }

    public JSONObject SaveTransaction() throws SQLException, GuanzonException, CloneNotSupportedException {
        return saveTransaction();
    }

    @Override
    public JSONObject willSave() throws SQLException, CloneNotSupportedException, GuanzonException {
        /* Put system validations and other assignments here */
        poJSON = new JSONObject();
        boolean lbUpdated = false;

        if (Master().getTransactionStatus().equals(PurchaseOrderStatus.CONFIRMED) && pnEditMode == EditMode.UPDATE) {
            if (poGRider.getUserLevel() <= UserRight.ENCODER) {
                poJSON = ShowDialogFX.getUserApproval(poGRider);
                if (!"success".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }
            }
        }
//
        Iterator<Model> detail = Detail().iterator();
        while (detail.hasNext()) {
            Model item = detail.next();
            Object quantityObj = item.getValue("nQuantity");
            Object stockIDObj = item.getValue("sStockIDx");

            // Check if the values are not null
            if (quantityObj != null && stockIDObj != null) {
                double quantity = ((Number) quantityObj).doubleValue();
                String stockID = (String) stockIDObj;

                // Remove only items with empty stock ID or zero quantity
                if (stockID.isEmpty() || quantity <= 0.00) {
                    detail.remove();
                }
            } else {
                // Handle the case where the values are null
                detail.remove();
            }
        }
        if (PurchaseOrderStatus.RETURNED.equals(Master().getTransactionStatus())) {
            PurchaseOrder loRecord = new PurchaseOrderControllers(poGRider, null).PurchaseOrder();
            loRecord.InitTransaction();
            loRecord.OpenTransaction(Master().getTransactionNo());

            lbUpdated = loRecord.getDetailCount() == getDetailCount();
            if (lbUpdated) {
                lbUpdated = loRecord.Master().getTransactionDate().equals(Master().getTransactionDate());
            }
            if (lbUpdated) {
                lbUpdated = loRecord.Master().getSupplierID().equals(Master().getSupplierID());
            }
            if (lbUpdated) {
                lbUpdated = loRecord.Master().getDestinationID().equals(Master().getDestinationID());
            }
            if (lbUpdated) {
                lbUpdated = loRecord.Master().getTermCode().equals(Master().getTermCode());
            }
            if (lbUpdated) {
                lbUpdated = loRecord.Master().getDiscount().doubleValue() == Master().getDiscount().doubleValue();
            }
            if (lbUpdated) {
                lbUpdated = loRecord.Master().getAdditionalDiscount().doubleValue() == Master().getAdditionalDiscount().doubleValue();
            }

            if (loRecord.Master().getWithAdvPaym() == true) {
                if (lbUpdated) {
                    lbUpdated = loRecord.Master().getDownPaymentRatesPercentage().doubleValue() == Master().getDownPaymentRatesPercentage().doubleValue();
                }
            }
            if (loRecord.Master().getWithAdvPaym() == true) {
                if (lbUpdated) {
                    lbUpdated = loRecord.Master().getDownPaymentRatesAmount().doubleValue() == Master().getDownPaymentRatesAmount().doubleValue();
                }
            }
            if (lbUpdated) {
                lbUpdated = loRecord.Master().getTranTotal().doubleValue() == Master().getTranTotal().doubleValue();
            }
            if (lbUpdated) {
                lbUpdated = loRecord.Master().getRemarks().equals(Master().getRemarks());
            }
            if (lbUpdated) {
                for (int lnCtr = 0; lnCtr <= loRecord.getDetailCount() - 1; lnCtr++) {
                    lbUpdated = loRecord.Detail(lnCtr).getStockID().equals(Detail(lnCtr).getStockID());
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
            //enable this settrans if saving of return stat will change the status to the last status
//            Master().setTransactionStatus(PurchaseOrderStatus.OPEN); //If edited update trasaction status into open
        }

        //Allow the user to edit details but seek an approval from the approving officer
        if (PurchaseOrderStatus.CONFIRMED.equals(Master().getTransactionStatus())) {
            poJSON = setValueToOthers(Master().getTransactionStatus());
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
        }

        for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
            Detail(lnCtr).setTransactionNo(Master().getTransactionNo());
            Detail(lnCtr).setEntryNo(lnCtr + 1);
            Detail(lnCtr).setModifiedDate(poGRider.getServerDate());
        }
        poJSON.put("result", "success");
        return poJSON;
    }

    @Override
    protected JSONObject save() throws CloneNotSupportedException, SQLException, GuanzonException {
        return isEntryOkay(PurchaseOrderStatus.OPEN);
    }

    @Override
    protected JSONObject isEntryOkay(String status) throws CloneNotSupportedException {
        GValidator loValidator = PurchaseOrderValidatorFactory.make(Master().getIndustryID());

        loValidator.setApplicationDriver(poGRider);
        loValidator.setTransactionStatus(status);
        loValidator.setMaster(Master());

        poJSON = loValidator.validate();
        return poJSON;
    }

    @Override
    public JSONObject saveOthers() {
        JSONObject poJSON = new JSONObject();
        try {
            poJSON = saveUpdates(PurchaseOrderStatus.CONFIRMED);
            if (!"success".equals((String) poJSON.get("result"))) {
                poGRider.rollbackTrans();
                return poJSON;
            }
        } catch (CloneNotSupportedException | SQLException ex) {
            Logger.getLogger(PurchaseOrder.class.getName()).log(Level.SEVERE, null, ex);
            poJSON.put("result", "error");
            poJSON.put("message", ex.getMessage());
            return poJSON;
        }

        poJSON.put("result", "success");
        return poJSON;
    }

    @Override
    public void saveComplete() {
        /*This procedure was called when saving was complete*/
        System.out.println("Transaction saved successfully.");
    }

    public JSONObject OpenTransaction(String transactionNo) throws CloneNotSupportedException, SQLException, GuanzonException {
        if(!pbWthParent){
            //Replaced script above by calling of method Arsiela 10-15-2025 09:25:01
            poJSON = seekApproval();
            if("error".equalsIgnoreCase((String)poJSON.get("result"))){
                return poJSON;
            }
        }
        
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

    public JSONObject ConfirmTransaction(String remarks) throws ParseException, SQLException, CloneNotSupportedException, GuanzonException {
        poJSON = new JSONObject();

        String lsStatus = PurchaseOrderStatus.CONFIRMED;
        boolean lbConfirm = true;

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
                        }
                    }

                    //check if authorization request is already approved by all authorizing personnel
                    if(!check.isAuthOkay()){
                        //check  the user level again then if he/she allow to confirm
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
        
        poJSON = setValueToOthers(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        //check  the user level again then if he/she allow to approve
        poGRider.beginTrans("UPDATE STATUS", "ConfirmTransaction", SOURCE_CODE, Master().getTransactionNo());

        poJSON = saveUpdates(PurchaseOrderStatus.CONFIRMED);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }
        
        //Update Transaction Status of PO Quotation
        poJSON = updatePOQuotationStatus(lsStatus);
        if ("error".equals((String) poJSON.get("result"))) {
            System.out.println("PO Quotation Saving " + (String) poJSON.get("message"));
            return poJSON;
        }

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

        return poJSON;
    }

    public JSONObject ApproveTransaction(String remarks) throws ParseException, SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = PurchaseOrderStatus.APPROVED;
        boolean lbApprove = true;

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
                        }
                    }

                    //check if authorization request is already approved by all authorizing personnel
                    if(!check.isAuthOkay()){
                        //check  the user level again then if he/she allow to approve
                        poGRider.beginTrans("UPDATE STATUS", "Approve Transaction", SOURCE_CODE, Master().getTransactionNo());

                        System.out.println("Status: " + lsStatus);
                        lsStatus = Character.toString((char)(64 + Integer.parseInt(lsStatus)));
                        System.out.println("Status: " + lsStatus);
                        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbApprove, true);
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
        
        System.out.println("poJSON = setValueToOthers(lsStatus)");
        poJSON = setValueToOthers(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        System.out.println("poJSON = generatePRF()");
        poJSON = generatePRF();
         if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        //check  the user level again then if he/she allow to approve
        poGRider.beginTrans("UPDATE STATUS", "Approve Transaction", SOURCE_CODE, Master().getTransactionNo());

        System.out.println("poJSON = saveUpdates(PurchaseOrderStatus.CONFIRMED)");
        poJSON = saveUpdates(PurchaseOrderStatus.CONFIRMED);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        poJSON = savePRF();
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        //kalyptus - 2025.10.08 02:36pm
        //save to inventory ledger
        InventoryTransaction loTrans = new InventoryTransaction(poGRider);
        loTrans.PurchaseOrder((String)poMaster.getValue("sTransNox"), (Date)poMaster.getValue("dTransact"), false);
        
        for (Model loDetail : paDetail) {
            Model_PO_Detail detail = (Model_PO_Detail) loDetail;
            loTrans.addDetail((String)poMaster.getValue("sIndstCdx"), detail.getStockID(), "0", 0, detail.getQuantity().doubleValue(), detail.getUnitPrice().doubleValue());
        }
        loTrans.saveTransaction();

        System.out.println("poJSON = statusChange(");
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbApprove, true);
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

        if (lbApprove) {
            poJSON.put("message", "Transaction approved successfully.");
        } else {
            poJSON.put("message", "Transaction approving request submitted successfully.");
        }

        return poJSON;
    }

    public JSONObject PostTransaction(String remarks) throws ParseException, SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = PurchaseOrderStatus.POSTED;
        boolean lbPost = true;

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
                        }
                    }

                    //check if authorization request is already approved by all authorizing personnel
                    if(!check.isAuthOkay()){
                        //check  the user level again then if he/she allow to confirm
                        poGRider.beginTrans("UPDATE STATUS", "Post Transaction", SOURCE_CODE, Master().getTransactionNo());

                        lsStatus = Character.toString((char)(64 + Integer.parseInt(lsStatus)));
                        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbPost, true);
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

        poJSON = setValueToOthers(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        //check  the user level again then if he/she allow to approve
        poGRider.beginTrans("UPDATE STATUS", "Post Transaction", SOURCE_CODE, Master().getTransactionNo());

        poJSON = saveUpdates(PurchaseOrderStatus.CONFIRMED);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbPost, true);
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

        if (lbPost) {
            poJSON.put("message", "Transaction posted successfully.");
        } else {
            poJSON.put("message", "Transaction posting request submitted successfully.");
        }

        return poJSON;
    }

    public JSONObject CancelTransaction(String remarks) throws ParseException, SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = PurchaseOrderStatus.CANCELLED;
        boolean lnCancel = true;

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
                        }
                    }

                    //check if authorization request is already approved by all authorizing personnel
                    if(!check.isAuthOkay()){
                        //check  the user level again then if he/she allow to approve
                        poGRider.beginTrans("UPDATE STATUS", "Cancel Transaction", SOURCE_CODE, Master().getTransactionNo());

                        lsStatus = Character.toString((char)(64 + Integer.parseInt(lsStatus)));
                        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lnCancel, true);
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
        
        poJSON = setValueToOthers(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        //check  the user level again then if he/she allow to approve
        poGRider.beginTrans("UPDATE STATUS", "Cancel Transaction", SOURCE_CODE, Master().getTransactionNo());

        //kalyptus-2025.10.08 02:52pm
        //save to inventory ledger
        if(PurchaseOrderStatus.APPROVED.equalsIgnoreCase((String) poMaster.getValue("cTranStat"))){
            InventoryTransaction loTrans = new InventoryTransaction(poGRider);
            loTrans.PurchaseOrder((String)poMaster.getValue("sTransNox"), (Date)poMaster.getValue("dTransact"), true);

            for (Model loDetail : paDetail) {
                Model_PO_Detail detail = (Model_PO_Detail) loDetail;
                loTrans.addDetail((String)detail.getValue("sIndstCdx"), detail.getStockID(), "0", 0, detail.getQuantity().doubleValue(), detail.getUnitPrice().doubleValue());
            }
            loTrans.saveTransaction();
        }
        
        poJSON = saveUpdates(PurchaseOrderStatus.CONFIRMED);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }
        
        //Update Transaction Status of PO Quotation
        poJSON = updatePOQuotationStatus(lsStatus);
        if ("error".equals((String) poJSON.get("result"))) {
            System.out.println("PO Quotation Saving " + (String) poJSON.get("message"));
            return poJSON;
        }

        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lnCancel, true);
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

        if (lnCancel) {
            poJSON.put("message", "Transaction cancelled successfully.");
        } else {
            poJSON.put("message", "Transaction cancellation request submitted successfully.");
        }

        return poJSON;
    }

    public JSONObject VoidTransaction(String remarks) throws ParseException, SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = PurchaseOrderStatus.VOID;
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
                        }
                    }

                    //check if authorization request is already approved by all authorizing personnel
                    if(!check.isAuthOkay()){
                        poGRider.beginTrans("UPDATE STATUS", "Void Transaction", SOURCE_CODE, Master().getTransactionNo());

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
        
        poJSON = setValueToOthers(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        //check  the user level again then if he/she allow to approve
        poGRider.beginTrans("UPDATE STATUS", "Void Transaction", SOURCE_CODE, Master().getTransactionNo());

        poJSON = saveUpdates(PurchaseOrderStatus.CONFIRMED);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }
        
        //Update Transaction Status of PO Quotation
        poJSON = updatePOQuotationStatus(lsStatus);
        if ("error".equals((String) poJSON.get("result"))) {
            System.out.println("PO Quotation Saving " + (String) poJSON.get("message"));
            return poJSON;
        }

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

    public JSONObject ReturnTransaction(String remarks) throws ParseException, SQLException, GuanzonException, CloneNotSupportedException {
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
        if (poGRider.getUserLevel() <= UserRight.ENCODER) {
            poJSON = ShowDialogFX.getUserApproval(poGRider);
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            } else {
                if (Integer.parseInt(poJSON.get("nUserLevl").toString()) <= UserRight.ENCODER) {
                    poJSON.put("result", "error");
                    poJSON.put("message", "User is not an authorized approving officer..");
                    return poJSON;
                }
            }
        }
        poJSON = setValueToOthers(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        //check  the user level again then if he/she allow to approve
        poGRider.beginTrans("UPDATE STATUS", "Return Transaction", SOURCE_CODE, Master().getTransactionNo());

        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbReturn, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }
        poJSON = saveUpdates(PurchaseOrderStatus.CONFIRMED);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }
        
        //Update Transaction Status of PO Quotation
        poJSON = updatePOQuotationStatus(lsStatus);
        if ("error".equals((String) poJSON.get("result"))) {
            System.out.println("PO Quotation Saving " + (String) poJSON.get("message"));
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

    public JSONObject isDetailHasZeroQty() {
        poJSON = new JSONObject();
        int zeroQtyRow = -1;
        boolean hasNonZeroQty = false;
        boolean hasZeroQty = false;
        int lastRow = getDetailCount() - 1;

        for (int lnRow = 0; lnRow <= lastRow; lnRow++) {
            double quantity = Detail(lnRow).getQuantity().doubleValue();
            String stockID = (String) Detail(lnRow).getValue("sStockIDx");

            if (!stockID.isEmpty()) {
                if (quantity == 0.00) {
                    hasZeroQty = true;
                    if (zeroQtyRow == -1) {
                        zeroQtyRow = lnRow;
                    }
                } else {
                    hasNonZeroQty = true;
                }
            }
        }

        if (!hasNonZeroQty && hasZeroQty) {
            poJSON.put("result", "error");
            poJSON.put("message", "All items have zero quantity. Please enter a valid quantity.");
            poJSON.put("tableRow", zeroQtyRow);
            poJSON.put("warning", "true");
        } else if (hasZeroQty) {
            poJSON.put("result", "error");
            poJSON.put("message", "Some items have zero quantity. Please review.");
            poJSON.put("tableRow", zeroQtyRow);
            poJSON.put("warning", "false");
        } else {
            poJSON.put("result", "success");
            poJSON.put("message", "All items have valid quantities.");
            poJSON.put("tableRow", lastRow);
        }

        return poJSON;
    }

    private JSONObject setValueToOthers(String status)
            throws CloneNotSupportedException,
            SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        poStockRequest = new ArrayList<>();
        poPOQuotation = new ArrayList<>();
        poPOQuotationStatus = new ArrayList<>();
        poPOQuotationRemovedStatus = new ArrayList<>();
        int lnCtr;

        //Update Purchase Order exist in PO Receiving Detail
        for (lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
            System.out.println("----------------------PURCHASE ORDER RECEIVING DETAIL---------------------- ");
            System.out.println("TransNo : " + (lnCtr + 1) + " : " + Detail(lnCtr).getTransactionNo());
            System.out.println("OrderNo : " + (lnCtr + 1) + " : " + Detail(lnCtr).getSouceNo());
            System.out.println("StockId : " + (lnCtr + 1) + " : " + Detail(lnCtr).getStockID());
            System.out.println("------------------------------------------------------------------ ");
            if (Detail(lnCtr).getSouceNo() != null && !"".equals(Detail(lnCtr).getSouceNo())) {
                double totalRequest = 0.00;
                switch(Detail(lnCtr).getSouceCode()){
                    case PurchaseOrderStatus.SourceCode.STOCKREQUEST:
                        totalRequest = (Detail(lnCtr).InvStockRequestDetail().getApproved());
        //                        - (Detail(lnCtr).InvStockRequestDetail().getIssued()
        //                        + Detail(lnCtr).InvStockRequestDetail().getPurchase()));

                        //1. Check discrepancy if the order quantity is not greater than request

                        if (!Detail(lnCtr).getSouceNo().isEmpty()) {
        //                    remaining = nApproved - (nCancelld + nIssueQty + nOrderQty)

                           if (status.equals(PurchaseOrderStatus.CONFIRMED) ||
                                   status.equals(PurchaseOrderStatus.APPROVED)) {
                                double remaining = 0.0000;
                                    remaining = (Detail(lnCtr).InvStockRequestDetail().getApproved() -
                                            (Detail(lnCtr).InvStockRequestDetail().getCancelled() + 
                                            Detail(lnCtr).InvStockRequestDetail().getIssued() + 
                                            Detail(lnCtr).InvStockRequestDetail().getPurchase()));
                                if (remaining == 0.00) {
                                    poJSON.put("result", "error");
                                    poJSON.put("message", "Discrepancy: Stock requests related to this order number have already been processed.");
                                    return poJSON;
                                }
                            }

                            if (Detail(lnCtr).getQuantity().doubleValue() > totalRequest) {
                                poJSON.put("result", "error");
                                poJSON.put("message", "Discrepancy: Order Quantity cannot greater than request quantity, please enter valid order quantity.");
                                return poJSON;
                            }
                        }

                        //1. Check for discrepancy
                        if (Detail(lnCtr).getQuantity().doubleValue() != Detail(lnCtr).InvStockRequestDetail().getQuantity()) {
                            System.out.println("Require Approval");
                            pbApproval = true;
                        }

                        if (Master().getTransactionStatus().equals(PurchaseOrderStatus.RETURNED)) {

                            if (totalRequest == 0) {
                                poJSON.put("result", "error");
                                poJSON.put("message", "All stock requests related to this order number have already been processed.");
                                return poJSON;
                            }
                        }

                        updateInvStockRequest(status, Detail(lnCtr).getSouceNo(), Detail(lnCtr).getStockID(), Detail(lnCtr).getQuantity().doubleValue());
                    break;
                    case PurchaseOrderStatus.SourceCode.POQUOTATION: 
                        //TODO Validate
                        poJSON = checkExistingPO(Detail(lnCtr).getSouceNo(),Detail(lnCtr).getSouceCode());
                        if ("error".equals((String) poJSON.get("result"))) {
                            return poJSON;
                        }
                        //TODO UPDATING OF DETAILS
//                        totalRequest = (Detail(lnCtr).POQuotationDetail().getQuantity());
                        //1. Check discrepancy if the order quantity is not greater than request
//                        if (!Detail(lnCtr).getSouceNo().isEmpty()) {
//        //                    remaining = nApproved - (nCancelld + nIssueQty + nOrderQty)
//
//                           if (status.equals(PurchaseOrderStatus.CONFIRMED) ||
//                                   status.equals(PurchaseOrderStatus.APPROVED)) {
//                                double remaining = 0.0000;
//                                    remaining = (Detail(lnCtr).InvStockRequestDetail().getApproved() -
//                                            (Detail(lnCtr).InvStockRequestDetail().getCancelled() + 
//                                            Detail(lnCtr).InvStockRequestDetail().getIssued() + 
//                                            Detail(lnCtr).InvStockRequestDetail().getPurchase()));
//                                if (remaining == 0.00) {
//                                    poJSON.put("result", "error");
//                                    poJSON.put("message", "Discrepancy: Stock requests related to this order number have already been processed.");
//                                    return poJSON;
//                                }
//                            }
//
//                            if (Detail(lnCtr).getQuantity().doubleValue() > totalRequest) {
//                                poJSON.put("result", "error");
//                                poJSON.put("message", "Discrepancy: Order Quantity cannot greater than request quantity, please enter valid order quantity.");
//                                return poJSON;
//                            }
//                        }
//
//                        //1. Check for discrepancy
//                        if (Detail(lnCtr).getQuantity().doubleValue() != Detail(lnCtr).InvStockRequestDetail().getQuantity()) {
//                            System.out.println("Require Approval");
//                            pbApproval = true;
//                        }
//
//                        if (Master().getTransactionStatus().equals(PurchaseOrderStatus.RETURNED)) {
//                            if (totalRequest == 0) {
//                                poJSON.put("result", "error");
//                                poJSON.put("message", "All stock requests related to this order number have already been processed.");
//                                return poJSON;
//                            }
//                        }

                        updatePOQuotation(status, Detail(lnCtr).getSouceNo(), Detail(lnCtr).getStockID(), Detail(lnCtr).getQuantity().doubleValue(),false );
                    break;
                }
                
            } else {
                //Require approve for all po receiving without po
                System.out.println("Require Approval");
                pbApproval = true;
            }
        }
        //Update stock request removed in purchase order
        for (lnCtr = 0; lnCtr <= getDetailRemovedCount() - 1; lnCtr++) {
            switch(Detail(lnCtr).getSouceCode()){
                case PurchaseOrderStatus.SourceCode.STOCKREQUEST:
                    updateInvStockRequest(status, DetailRemove(lnCtr).getSouceNo(), DetailRemove(lnCtr).getStockID(), DetailRemove(lnCtr).getQuantity().doubleValue());
                break;
                case PurchaseOrderStatus.SourceCode.POQUOTATION:
                    updatePOQuotation(status, DetailRemove(lnCtr).getSouceNo(), DetailRemove(lnCtr).getStockID(), DetailRemove(lnCtr).getQuantity().doubleValue(), true);
                break;
            }
        }
        poJSON.put("result", "success");
        return poJSON;
    }
    
    //TODO
    private void updatePOQuotation(String status, String orderNo, String stockId, double quantity, boolean isRemoved)
            throws GuanzonException,
            SQLException,
            CloneNotSupportedException {
        int lnRow, lnList;
        double lnRecQty = 0.00;
        boolean lbExist = false;
        //2.check if order no is already exist in purchase order array list
        for (lnRow = 0; lnRow <= poPOQuotation.size() - 1; lnRow++) {
            if (poPOQuotation.get(lnRow).Master().getTransactionNo() != null) {
                if (orderNo.equals(poPOQuotation.get(lnRow).Master().getTransactionNo())) {
                    lbExist = true;
                    break;
                }
            }
        }

        if (!lbExist) {
            //Populate updating of status
            if(isRemoved){
                poPOQuotationRemovedStatus.add(POQuotation());
                poPOQuotationRemovedStatus.get(poPOQuotationRemovedStatus.size() - 1).InitTransaction();
                poPOQuotationRemovedStatus.get(poPOQuotationRemovedStatus.size() - 1).OpenTransaction(orderNo);
            } else {
                poPOQuotationStatus.add(POQuotation());
                poPOQuotationStatus.get(poPOQuotationStatus.size() - 1).InitTransaction();
                poPOQuotationStatus.get(poPOQuotationStatus.size() - 1).OpenTransaction(orderNo);
            }
            
            poPOQuotation.add(POQuotation());
            poPOQuotation.get(poPOQuotation.size() - 1).InitTransaction();
            poPOQuotation.get(poPOQuotation.size() - 1).OpenTransaction(orderNo);
            //Update PO Quotation
            poPOQuotation.get(poPOQuotation.size() - 1).UpdateTransaction();
            lnList = poPOQuotation.size() - 1;
        } else {
            //if already exist, get the row no of purchase order
            lnList = lnRow;
        }
        //TODO UPDATING OF DETAILS
//        for (lnRow = 0; lnRow <= poPOQuotation.get(lnList).getDetailCount() - 1; lnRow++) {
//            if (stockId.equals(poPOQuotation.get(lnList).Detail(lnRow).getStockId())) {
//
//                switch (status) {
//                    case PurchaseOrderStatus.CONFIRMED:
////                        poPOQuotation.get(lnList).Master().setTransactionStatus(POQuotationStatus.POSTED);
//                        lnRecQty = getRequestQty(orderNo, stockId, true,poPOQuotation.get(lnList).getSourceCode());
//                        lnRecQty = lnRecQty + quantity;
//                        break;
//                    case PurchaseOrderStatus.APPROVED:
//                        lnRecQty = poPOQuotation.get(lnList).Detail(lnRow).getQuantity();
////                        poPOQuotation.get(lnList).Master().setTransactionStatus(POQuotationStatus.POSTED);
//                        poPOQuotation.get(lnList).Master().setModifiedDate(poGRider.getServerDate());
//                        poPOQuotation.get(lnList).Master().setModifyingId(poGRider.getUserID());
//                        break;
//                    case PurchaseOrderStatus.VOID:
//                    case PurchaseOrderStatus.RETURNED:
////                        poPOQuotation.get(lnList).Master().setTransactionStatus(POQuotationStatus.APPROVED); //TODO
//                        lnRecQty = getRequestQty(orderNo, stockId, false,poPOQuotation.get(lnList).getSourceCode());
//                        lnRecQty = lnRecQty - quantity;
//                        break;
//                }
//                if (lnRecQty < 0) {
//                    lnRecQty = 0;
//                }
////                poPOQuotation.get(lnList).Detail(lnRow).setPurchase(lnRecQty);
//                poPOQuotation.get(lnList).Detail(lnRow).setModifiedDate(poGRider.getServerDate());
//                break;
//            }
//        }
    }

    private void updateInvStockRequest(String status, String orderNo, String stockId, double quantity)
            throws GuanzonException,
            SQLException,
            CloneNotSupportedException {
        int lnRow, lnList;
        double lnRecQty = 0.00;
        boolean lbExist = false;
        //2.check if order no is already exist in purchase order array list
        for (lnRow = 0; lnRow <= poStockRequest.size() - 1; lnRow++) {
            if (poStockRequest.get(lnRow).Master().getTransactionNo() != null) {
                if (orderNo.equals(poStockRequest.get(lnRow).Master().getTransactionNo())) {
                    lbExist = true;
                    break;
                }
            }
        }

        if (!lbExist) {
            poStockRequest.add(StockRequest());
            poStockRequest.get(poStockRequest.size() - 1).InitTransaction();
            poStockRequest.get(poStockRequest.size() - 1).OpenTransaction(orderNo);
            poStockRequest.get(poStockRequest.size() - 1).UpdateTransaction();
            lnList = poStockRequest.size() - 1;
        } else {
            //if already exist, get the row no of purchase order
            lnList = lnRow;
        }

        for (lnRow = 0; lnRow <= poStockRequest.get(lnList).getDetailCount() - 1; lnRow++) {
            if (stockId.equals(poStockRequest.get(lnList).Detail(lnRow).getStockId())) {

                switch (status) {
                    case PurchaseOrderStatus.CONFIRMED:
                        lnRecQty = getRequestQty(orderNo, stockId, true,poStockRequest.get(lnList).getSourceCode());
                        lnRecQty = lnRecQty + quantity;
                        break;
                    case PurchaseOrderStatus.APPROVED:
                        lnRecQty = poStockRequest.get(lnList).Detail(lnRow).getPurchase();
                        poStockRequest.get(lnList).Master().setProcessed(true);
                        poStockRequest.get(lnList).Master().setModifiedDate(poGRider.getServerDate());
                        poStockRequest.get(lnList).Master().setModifyingId(poGRider.getUserID());
                        break;
                    case PurchaseOrderStatus.VOID:
                    case PurchaseOrderStatus.RETURNED:
                        lnRecQty = getRequestQty(orderNo, stockId, false,poStockRequest.get(lnList).getSourceCode());
                        lnRecQty = lnRecQty - quantity;
                        break;
                }
                if (lnRecQty < 0) {
                    lnRecQty = 0;
                }
                poStockRequest.get(lnList).Detail(lnRow).setPurchase(lnRecQty);
                poStockRequest.get(lnList).Detail(lnRow).setModifiedDate(poGRider.getServerDate());
                break;
            }
        }
    }

    private JSONObject generatePRF()
            throws CloneNotSupportedException,
            SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        try {
            if (Master().getWithAdvPaym() && Master().getDownPaymentRatesAmount().doubleValue() > PurchaseOrderStaticData.default_value_double) {
                poPaymentRequest = new CashflowControllers(poGRider, null);

                poPaymentRequest.PaymentRequest().InitTransaction();
                poPaymentRequest.PaymentRequest().NewTransaction();

                SearchPayee(Master().getSupplierID());

                poPaymentRequest.PaymentRequest().Master().setTransactionDate(Master().getTransactionDate());
                poPaymentRequest.PaymentRequest().Master().setBranchCode(Master().getBranchCode());
                if (poGRider.isMainOffice() || poGRider.isWarehouse()) {
                    poPaymentRequest.PaymentRequest().Master().setDepartmentID(poGRider.getDepartment());
                }

                double transactionTotal = Master().getTranTotal().doubleValue();
                double discountRate = Master().getDiscount().doubleValue();
                double additionalDiscount = Master().getAdditionalDiscount().doubleValue();
                double downPaymentPercentage = Master().getDownPaymentRatesPercentage().doubleValue();
                double downPaymentFixedAmount = Master().getDownPaymentRatesAmount().doubleValue();

                double discountAmount = transactionTotal * discountRate;
                double netTotal = transactionTotal - discountAmount;

                netTotal -= additionalDiscount;

                double downPaymentPercentageAmount = netTotal * downPaymentPercentage;
                netTotal -= downPaymentPercentageAmount;
                netTotal -= downPaymentFixedAmount;

                double totalAdv = downPaymentPercentageAmount + downPaymentFixedAmount;

                poPaymentRequest.PaymentRequest().Master().setRemarks(Master().getRemarks());
                poPaymentRequest.PaymentRequest().Master().setSourceCode(SOURCE_CODE);
                poPaymentRequest.PaymentRequest().Master().setIndustryID(poGRider.getIndustry());
                poPaymentRequest.PaymentRequest().Master().setCompanyID(poGRider.getCompnyId());
                poPaymentRequest.PaymentRequest().Master().setSourceCode(SOURCE_CODE);
                poPaymentRequest.PaymentRequest().Master().setSourceNo(Master().getTransactionNo());
                poPaymentRequest.PaymentRequest().Master().setPayeeID(PayeeID); //Master().getSupplierID()
                poPaymentRequest.PaymentRequest().Master().setEntryNo(1);
                poPaymentRequest.PaymentRequest().Master().setSeriesNo(poPaymentRequest.PaymentRequest().getSeriesNoByBranch());
                poPaymentRequest.PaymentRequest().Master().setTranTotal(totalAdv);
                poPaymentRequest.PaymentRequest().Master().setNetTotal(totalAdv);
                poPaymentRequest.PaymentRequest().Master().setTransactionStatus(PurchaseOrderStatus.CONFIRMED);

                poPaymentRequest.PaymentRequest().Detail(0).setEntryNo((int) 1);
                poPaymentRequest.PaymentRequest().Detail(0).setParticularID(PurchaseOrderStaticData.PurchaseOrder);
                poPaymentRequest.PaymentRequest().Detail(0).setPRFRemarks(PurchaseOrderStaticData.default_empty_string);
                poPaymentRequest.PaymentRequest().Detail(0).setAmount(totalAdv);
                poPaymentRequest.PaymentRequest().Detail(0).setDiscount(PurchaseOrderStaticData.default_value_double);
                poPaymentRequest.PaymentRequest().Detail(0).setAddDiscount(PurchaseOrderStaticData.default_value_double);
                poPaymentRequest.PaymentRequest().Detail(0).setVatable(PurchaseOrderStaticData.default_value_string);
                poPaymentRequest.PaymentRequest().Detail(0).setWithHoldingTax(PurchaseOrderStaticData.default_value_double);
                poPaymentRequest.PaymentRequest().AddDetail();
            }

        } catch (Exception e) {
            poJSON.put("result", "error");
            poJSON.put("message", e.getMessage());
            return poJSON;
        }
        poJSON.put("result", "success");
        return poJSON;
    }

    private int getRequestQty(String orderNo, String stockId, boolean isAdd, String sourceCode) throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        int lnRecQty = 0;
        String lsSQL = "SELECT b.nQuantity AS nQuantity "
                + "FROM po_master a "
                + "LEFT JOIN po_detail b ON b.sTransNox = a.sTransNox ";

        lsSQL = MiscUtil.addCondition(lsSQL, " b.sSourceNo = " + SQLUtil.toSQL(orderNo)
                + " AND b.sStockIDx = " + SQLUtil.toSQL(stockId)
                + " AND ( a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderStatus.CONFIRMED)
                + " OR a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderStatus.APPROVED)
                + " ) "
                + " AND b.sSourceCd = " + SQLUtil.toSQL(sourceCode)
                );
        if (isAdd) {
            lsSQL = lsSQL + " AND a.sTransNox <> " + SQLUtil.toSQL(Master().getTransactionNo());
        }

        System.out.println("executeQuery: >>>> " + lsSQL);
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

    private JSONObject savePRF()
            throws CloneNotSupportedException {
        poJSON = new JSONObject();
        try {
            if (Master().getWithAdvPaym() && Master().getDownPaymentRatesAmount().doubleValue() > PurchaseOrderStaticData.default_value_double) {
                poPaymentRequest.PaymentRequest().setWithParent(true);
                poJSON = poPaymentRequest.PaymentRequest().SaveTransaction();
                if ("error".equals((String) poJSON.get("result"))) {
                    poJSON.put("result", "error");
                    return poJSON;

                }
            }
        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(PurchaseOrder.class
                    .getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
            return poJSON;
        }
        poJSON.put("result", "success");
        return poJSON;
    }

    private JSONObject saveUpdates(String status)
            throws CloneNotSupportedException {
        poJSON = new JSONObject();
        int lnCtr;
        try {
            for (lnCtr = 0; lnCtr <= poStockRequest.size() - 1; lnCtr++) {
                if (PurchaseOrderStatus.APPROVED.equals(status)) {
                    poStockRequest.get(lnCtr).Master().setProcessed(true);
                }
                poStockRequest.get(lnCtr).Master().setModifyingId(poGRider.getUserID());
                poStockRequest.get(lnCtr).Master().setModifiedDate(poGRider.getServerDate());
                poStockRequest.get(lnCtr).setWithParent(true);
                poJSON = poStockRequest.get(lnCtr).SaveTransaction();
                if ("error".equals((String) poJSON.get("result"))) {
                    System.out.println("Stock Request Saving " + (String) poJSON.get("message"));
                    return poJSON;

                }
            }
            
            //TODO Save Ordered Quantity
            for (lnCtr = 0; lnCtr <= poPOQuotation.size() - 1; lnCtr++) {
                poPOQuotation.get(lnCtr).Master().setModifyingId(poGRider.getUserID());
                poPOQuotation.get(lnCtr).Master().setModifiedDate(poGRider.getServerDate());
                poPOQuotation.get(lnCtr).setWithParent(true);
                poJSON = poPOQuotation.get(lnCtr).SaveTransaction();
                if ("error".equals((String) poJSON.get("result"))) {
                    System.out.println("PO Quotation Saving " + (String) poJSON.get("message"));
                    return poJSON;
                }
            }

        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(PurchaseOrder.class
                    .getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
            return poJSON;
        } 
        poJSON.put("result", "success");
        return poJSON;
    }
    
    private JSONObject updatePOQuotationStatus(String fsStatus) throws CloneNotSupportedException, SQLException, GuanzonException, ParseException{
        for ( int lnCtr = 0; lnCtr <= poPOQuotationStatus.size() - 1; lnCtr++) {
           switch(fsStatus){
            case PurchaseOrderStatus.CONFIRMED:
                    poPOQuotationStatus.get(lnCtr).setWithParent(true);
                    poJSON = poPOQuotationStatus.get(lnCtr).PostTransaction("");
                    if ("error".equals((String) poJSON.get("result"))) {
                        System.out.println("PO Quotation Posting " + (String) poJSON.get("message"));
                        return poJSON;
                    }
            break;
            case PurchaseOrderStatus.RETURNED:
            case PurchaseOrderStatus.VOID:
            case PurchaseOrderStatus.CANCELLED:
                poJSON = checkExistingPO(poPOQuotationStatus.get(lnCtr).Master().getTransactionNo(),PurchaseOrderStatus.SourceCode.POQUOTATION);
                if ("success".equals((String) poJSON.get("result"))) {
                    poPOQuotationStatus.get(lnCtr).setWithParent(true);
                    poJSON = poPOQuotationStatus.get(lnCtr).ApproveTransaction("");
                    if ("error".equals((String) poJSON.get("result"))) {
                        System.out.println("PO Quotation Approving " + (String) poJSON.get("message"));
                        return poJSON;
                    }
                }
            break;
            }
        }
        
        for ( int lnCtr = 0; lnCtr <= poPOQuotationRemovedStatus.size() - 1; lnCtr++) {
            poJSON = checkExistingPO(poPOQuotationRemovedStatus.get(lnCtr).Master().getTransactionNo(),PurchaseOrderStatus.SourceCode.POQUOTATION);
            if ("success".equals((String) poJSON.get("result"))) {
                poPOQuotationRemovedStatus.get(lnCtr).setWithParent(true);
                poJSON = poPOQuotationRemovedStatus.get(lnCtr).ApproveTransaction("");
                if ("error".equals((String) poJSON.get("result"))) {
                    System.out.println("PO Quotation Approving " + (String) poJSON.get("message"));
                    return poJSON;
                }
            }
        }
    
        poJSON.put("result", "success");
        return poJSON;
    }
    
    private JSONObject checkExistingPO(String fsSourceNo, String fsSourceCode) throws SQLException{
        String detailQuery = " SELECT b.sSourceNo, b.sSourceCd "
                + " FROM po_master a "
                + " LEFT JOIN po_detail b ON a.sTransNox = b.sTransNox ";

        String lsFilterCondition = String.join(" AND ",
                    " b.sSourceNo = " + SQLUtil.toSQL(fsSourceNo),
                    " b.sSourceCd = " + SQLUtil.toSQL(fsSourceCode),
                    " ( a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderStatus.CONFIRMED)
                    + " OR a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderStatus.APPROVED) + " ) ",
                    " a.sTransNox <> " + SQLUtil.toSQL(Master().getTransactionNo())
                    );

        detailQuery = MiscUtil.addCondition(detailQuery, lsFilterCondition);
        System.out.println("Executing SQL: " + detailQuery);
        ResultSet loRS = poGRider.executeQuery(detailQuery);
        if (MiscUtil.RecordCount(loRS) >= 0) {
            if(loRS.next()) {
                poJSON.put("result", "error");
                poJSON.put("message", "Selected PO Quotation already have an existing Purchase Order.");
                return poJSON;
            }
        }
        
        poJSON.put("result", "success");
        poJSON.put("message", "No Record Found."); //Allow to set / update
        return poJSON;
    }

    @Override
    public void initSQL() {
        SQL_BROWSE = "SELECT "
                + "  a.sTransNox,"
                + "  a.dTransact,"
                + "  b.sDescript,"
                + "  c.sCompnyNm,"
                + "  e.sCompnyNm, "
                + "  f.sDescript,"
                + "  a.sBranchCd"
                + " FROM po_master a "
                + " LEFT JOIN Industry b ON a.sIndstCdx = b.sIndstCdx "
                + " LEFT JOIN company c ON c.sCompnyID = a.sCompnyID "
                + " LEFT JOIN inv_supplier d ON a.sSupplier = d.sSupplier"
                + " LEFT JOIN client_master e ON d.sSupplier = e.sClientID"
                + " , category f ";
    }

    public JSONObject SearchTransaction(String fsValue, String fsSupplierID, String fsReferID) throws CloneNotSupportedException, SQLException, GuanzonException {
        poJSON = new JSONObject();
        String lsTransStat = "";
        String lsBranch = "";
        if (psTranStat.length() > 1) {
            for (int lnCtr = 0; lnCtr <= psTranStat.length() - 1; lnCtr++) {
                lsTransStat += ", " + SQLUtil.toSQL(Character.toString(psTranStat.charAt(lnCtr)));
            }
            lsTransStat = " AND ( a.cTranStat IN (" + lsTransStat.substring(2) + ")"
                        + " OR (ASCII(a.cTranStat) - 64) IN (" + lsTransStat.substring(2) + ") )";
        } else {
            lsTransStat = " AND ( a.cTranStat = " + SQLUtil.toSQL(psTranStat)
                        + " OR (ASCII(a.cTranStat) - 64) = " + SQLUtil.toSQL(psTranStat) +" ) ";
        }

        initSQL();
        String lsFilterCondition = String.join(" AND ", "a.sIndstCdx = " + SQLUtil.toSQL(Master().getIndustryID()),
                " a.sCompnyID = " + SQLUtil.toSQL(Master().getCompanyID()),
                " a.sSupplier LIKE " + SQLUtil.toSQL("%" + fsSupplierID),
                " a.sCategrCd LIKE " + SQLUtil.toSQL("%" + Master().getCategoryCode()),
                " a.sTransNox LIKE " + SQLUtil.toSQL("%" + fsReferID));

        String lsSQL = MiscUtil.addCondition(SQL_BROWSE, lsFilterCondition);
        if (!psTranStat.isEmpty()) {
            lsSQL = lsSQL + lsTransStat;
        }
        if (!poGRider.isMainOffice() || !poGRider.isWarehouse()) {
            lsSQL = lsSQL + " AND a.sBranchCd LIKE " + SQLUtil.toSQL(poGRider.getBranchCode());
        }

        lsSQL = lsSQL + " GROUP BY a.sTransNox";
        System.out.println("SQL EXECUTED: " + lsSQL);
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

    public JSONObject SearchBarcode(String value, boolean byCode, int row, boolean hasNoSupplier)
            throws ExceptionInInitializerError, SQLException, GuanzonException, CloneNotSupportedException, NullPointerException {

        Inventory object = new InvControllers(poGRider, logwrapr).Inventory();
        object.setRecordStatus(RecordStatus.ACTIVE);

        String supplier = Master().getSupplierID().isEmpty() ? null : Master().getSupplierID();
        String brand = (Detail(row).getBrandId() != null && !Detail(row).getBrandId().isEmpty()) ? Detail(row).getBrandId() : null;
        String industry = Master().getIndustryID().isEmpty() ? null : Master().getIndustryID();
        String category = Master().getCategoryCode();

        poJSON = object.searchRecord(
                value,
                byCode,
                hasNoSupplier ? supplier : null,
                brand,
                industry,
                category
        );

        if ("success".equals((String) poJSON.get("result"))) {
            for (int lnRow = 0; lnRow <= getDetailCount() - 1; lnRow++) {
                if (lnRow != row) {
                    if ((Detail(lnRow).getSouceNo().equals("") || Detail(lnRow).getSouceNo() == null)
                            && (Detail(lnRow).getStockID().equals(object.getModel().getStockId()))) {
                        poJSON.put("result", "error");
                        poJSON.put("message", "Model: " + object.getModel().getDescription() + " already exist in table at row " + (lnRow + 1) + ".");
                        poJSON.put("tableRow", lnRow);
                        return poJSON;
                    }
                }
            }
            Detail(row).setStockID(object.getModel().getStockId());
            Detail(row).setUnitPrice(object.getModel().getCost().doubleValue());
            Detail(row).setOldPrice(object.getModel().getCost().doubleValue());
        }
        return poJSON;
    }

    public JSONObject SearchBarcodeGeneral(String value, boolean byCode, int row, boolean hasNoSupplier)
            throws ExceptionInInitializerError, SQLException, GuanzonException, CloneNotSupportedException, NullPointerException {

        Inventory object = new InvControllers(poGRider, logwrapr).Inventory();
        object.setRecordStatus(RecordStatus.ACTIVE);

        String supplier = Master().getSupplierID().isEmpty() ? null : Master().getSupplierID();
        String brand = (Detail(row).getBrandId() != null && !Detail(row).getBrandId().isEmpty()) ? Detail(row).getBrandId() : null;
        String industry = Master().getIndustryID().isEmpty() ? null : Master().getIndustryID();
        String category = Master().getCategoryCode();

        poJSON = object.searchRecord(
                value,
                byCode,
                hasNoSupplier ? supplier : null,
                brand,
                null,
                category
        );

        if ("success".equals((String) poJSON.get("result"))) {
            for (int lnRow = 0; lnRow <= getDetailCount() - 1; lnRow++) {
                if (lnRow != row) {
                    if ((Detail(lnRow).getSouceNo().equals("") || Detail(lnRow).getSouceNo() == null)
                            && (Detail(lnRow).getStockID().equals(object.getModel().getStockId()))) {
                        poJSON.put("result", "error");
                        poJSON.put("message", "Model: " + object.getModel().getDescription() + " already exist in table at row " + (lnRow + 1) + ".");
                        poJSON.put("tableRow", lnRow);
                        return poJSON;
                    }
                }
            }
            Detail(row).setStockID(object.getModel().getStockId());
            Detail(row).setUnitPrice(object.getModel().getCost().doubleValue());
            Detail(row).setOldPrice(object.getModel().getCost().doubleValue());
//            Detail(row).isSerialized(object.getModel().isSerialized());
//            Detail(row).setCategoryCode(object.getModel().getCategoryFirstLevelId());
        }
        return poJSON;
    }

    public JSONObject SearchBarcodeDescription(String value, boolean byCode, int row, boolean hasNoSupplier)
            throws ExceptionInInitializerError, SQLException, GuanzonException, CloneNotSupportedException, NullPointerException {

        Inventory object = new InvControllers(poGRider, logwrapr).Inventory();
        object.setRecordStatus(RecordStatus.ACTIVE);

        String supplier = Master().getSupplierID().isEmpty() ? null : Master().getSupplierID();
        String brand = (Detail(row).getBrandId() != null && !Detail(row).getBrandId().isEmpty()) ? Detail(row).getBrandId() : null;
        String industry = Master().getIndustryID().isEmpty() ? null : Master().getIndustryID();
        String category = Master().getCategoryCode();

        poJSON = object.searchRecord(
                value,
                byCode,
                hasNoSupplier ? supplier : null,
                brand,
                industry,
                category
        );

        if ("success".equals((String) poJSON.get("result"))) {
            for (int lnRow = 0; lnRow <= getDetailCount() - 1; lnRow++) {
                if (lnRow != row) {
                    if ((Detail(lnRow).getSouceNo().equals("") || Detail(lnRow).getSouceNo() == null)
                            && (Detail(lnRow).getStockID().equals(object.getModel().getStockId()))) {
                        poJSON.put("result", "error");
                        poJSON.put("message", "Barcode: " + object.getModel().getDescription() + " already exist in table at row " + (lnRow + 1) + ".");
                        poJSON.put("tableRow", lnRow);
                        return poJSON;
                    }
                }
            }
            Detail(row).setStockID(object.getModel().getStockId());
            Detail(row).setUnitPrice(object.getModel().getCost().doubleValue());
            Detail(row).setOldPrice(object.getModel().getCost().doubleValue());
        }
        return poJSON;
    }

    public JSONObject SearchBarcodeDescriptionGeneral(String value, boolean byCode, int row, boolean hasNoSupplier)
            throws ExceptionInInitializerError, SQLException, GuanzonException, CloneNotSupportedException, NullPointerException {
        Inventory object = new InvControllers(poGRider, logwrapr).Inventory();
        object.setRecordStatus(RecordStatus.ACTIVE);

        String supplier = Master().getSupplierID().isEmpty() ? null : Master().getSupplierID();
        String brand = (Detail(row).getBrandId() != null && !Detail(row).getBrandId().isEmpty()) ? Detail(row).getBrandId() : null;
        String industry = Master().getIndustryID().isEmpty() ? null : Master().getIndustryID();
        String category = Master().getCategoryCode();

        poJSON = object.searchRecord(
                value,
                byCode,
                hasNoSupplier ? supplier : null,
                brand,
                null,
                category
        );

        if ("success".equals((String) poJSON.get("result"))) {
            for (int lnRow = 0; lnRow <= getDetailCount() - 1; lnRow++) {
                if (lnRow != row) {
                    if ((Detail(lnRow).getSouceNo().equals("") || Detail(lnRow).getSouceNo() == null)
                            && (Detail(lnRow).getStockID().equals(object.getModel().getStockId()))) {
                        poJSON.put("result", "error");
                        poJSON.put("message", "Barcode: " + object.getModel().getDescription() + " already exist in table at row " + (lnRow + 1) + ".");
                        poJSON.put("tableRow", lnRow);
                        return poJSON;
                    }
                }
            }
            Detail(row).setStockID(object.getModel().getStockId());
            Detail(row).setUnitPrice(object.getModel().getCost().doubleValue());
            Detail(row).setOldPrice(object.getModel().getCost().doubleValue());
        }
        return poJSON;
    }

    public JSONObject SearchSupplier(String value, boolean byCode) throws SQLException, GuanzonException {
        
        AP_Client_Master object = new ClientControllers(poGRider, logwrapr).APClientMaster();
        object.setRecordStatus(RecordStatus.ACTIVE);
        poJSON = object.searchRecord(value, byCode);
        if ("success".equals((String) poJSON.get("result"))) {
            Master().setSupplierID(object.getModel().getClientId());
            Master().setAddressID(object.getModel().ClientAddress().getAddressId()); 
            Master().setContactID(object.getModel().ClientInstitutionContact().getContactPId()); 
            Master().setTermCode(object.getModel().getTermId());
        }

        return poJSON;
    }

    public JSONObject SearchPayee(String value) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Payee object = new CashflowControllers(poGRider, logwrapr).Payee();
        object.setRecordStatus("1");

        poJSON = object.searchPayee(value);

        if ("success".equals((String) poJSON.get("result"))) {
            PayeeID = (String) poJSON.get("sPayeeIDx");
//            Master().setPayeeID(object.getModel().getPayeeID());
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
        Brand brand = new ParamControllers(poGRider, logwrapr).Brand();
        brand.getModel().setRecordStatus(RecordStatus.ACTIVE);

        poJSON = brand.searchRecord(value, byCode, Master().getIndustryID());

        if ("success".equals((String) poJSON.get("result"))) {
            Detail(row).setBrandId(brand.getModel().getBrandId());
        }

        return poJSON;
    }

    public JSONObject SearchModel(String value, boolean byCode, int row, boolean hasNoSupplier)
            throws SQLException, GuanzonException, NullPointerException {
        Inventory object = new InvControllers(poGRider, logwrapr).Inventory();
        object.getModel().setRecordStatus(RecordStatus.ACTIVE);

        String supplier = Master().getSupplierID().isEmpty() ? null : Master().getSupplierID();
        String brand = (Detail(row).getBrandId() != null && !Detail(row).getBrandId().isEmpty()) ? Detail(row).getBrandId() : null;
        String industry = Master().getIndustryID().isEmpty() ? null : Master().getIndustryID();
        String category = Master().getCategoryCode();

        poJSON = object.searchRecord(
                value,
                byCode,
                hasNoSupplier ? supplier : null,
                brand,
                industry,
                category
        );

        if ("success".equals((String) poJSON.get("result"))) { 
           for (int lnRow = 0; lnRow <= getDetailCount() - 1; lnRow++) {
                if (lnRow != row) {
                    if ((Detail(lnRow).getSouceNo().equals("") || Detail(lnRow).getSouceNo() == null)
                            && (Detail(lnRow).getStockID().equals(object.getModel().getStockId()))) {
                        poJSON.put("result", "error");
                        poJSON.put("message", "Barcode: " + object.getModel().getDescription() + " already exist in table at row " + (lnRow + 1) + ".");
                        poJSON.put("tableRow", lnRow);
                        return poJSON;
                    }
                }
            }
            Detail(row).setStockID(object.getModel().getStockId());
            Detail(row).setUnitPrice(object.getModel().getCost().doubleValue());
            Detail(row).setOldPrice(object.getModel().getCost().doubleValue());
        }
        return poJSON;
    }
    
    private String getInvStockRequest_SQL(){
        return  "SELECT"
                + "  a.sTransNox,"
                + "  e.sBranchNm,"
                + "  a.sBranchCd,"
                + "  a.cTranStat,"
                + "  a.dTransact,"
                + "  a.sReferNox,"
                + "  a.cTranStat,"
                + "  a.sIndstCdx,"
                + "  COUNT(DISTINCT b.sStockIDx) AS total_details,"
                + "  SUM(b.nApproved - (b.nIssueQty + b.nOrderQty)) AS total_request,"
                + " '" +PurchaseOrderStatus.SourceCode.STOCKREQUEST+"' AS request_type"
                + " FROM inv_stock_request_master a"
                + " LEFT JOIN inv_stock_request_detail b ON a.sTransNox = b.sTransNox"
                + " LEFT JOIN inventory c ON b.sStockIDx = c.sStockIDx"
                + " LEFT JOIN branch e ON a.sBranchCd = e.sBranchCd"
                + " LEFT JOIN industry f ON a.sIndstCdx = f.sIndstCdx"
                + " LEFT JOIN inv_supplier g ON g.sStockIDx = c.sStockIDx"
                + " LEFT JOIN category h ON c.sCategCd1 = h.sCategrCd";
    }
    
    private String getPOQuotation_SQL(){
        return  "SELECT"
                + "  a.sTransNox,"
                + "  e.sBranchNm,"
                + "  a.sBranchCd,"
                + "  a.cTranStat,"
                + "  a.dTransact,"
                + "  a.sReferNox,"
                + "  a.cTranStat,"
                + "  a.sIndstCdx,"
                + "  COUNT(DISTINCT b.sStockIDx) AS total_details,"
                + "  SUM(b.nQuantity) AS total_request,"
                + "  '"+PurchaseOrderStatus.SourceCode.POQUOTATION+"' AS request_type"
                + " FROM po_quotation_master a"
                + " LEFT JOIN po_quotation_detail b ON a.sTransNox = b.sTransNox"
                + " LEFT JOIN inventory c ON b.sStockIDx = c.sStockIDx"
                + " LEFT JOIN branch e ON a.sBranchCd = e.sBranchCd"
                + " LEFT JOIN industry f ON a.sIndstCdx = f.sIndstCdx"
                + " LEFT JOIN inv_supplier g ON g.sStockIDx = c.sStockIDx"
                + " LEFT JOIN category h ON c.sCategCd1 = h.sCategrCd";
    }

    public JSONObject getApprovedStockRequests() throws SQLException, GuanzonException {
        String lsSQL = getInvStockRequest_SQL();
        String lsFilterCondition = String.join(" AND ", " a.sIndstCdx = " + SQLUtil.toSQL(Master().getIndustryID()),
                " e.sCompnyID = " + SQLUtil.toSQL(Master().getCompanyID()),
                " (g.sSupplier = " + SQLUtil.toSQL(Master().getSupplierID()) + " OR g.sSupplier IS NULL )",
                " b.nApproved > 0 ",
                " a.cProcessd = " + SQLUtil.toSQL(Logical.NO),
                " b.nApproved <> (b.nIssueQty + b.nOrderQty) ",
                " c.sCategCd1 = " + SQLUtil.toSQL(Master().getCategoryCode()),
                " a.cTranStat = " + SQLUtil.toSQL(StockRequestStatus.CONFIRMED));
        lsSQL = MiscUtil.addCondition(lsSQL, lsFilterCondition);
        if (!poGRider.isMainOffice() || !poGRider.isWarehouse()) {
            lsSQL = lsSQL + " AND a.sBranchCd = " + SQLUtil.toSQL(poGRider.getBranchCode());
        }
        lsSQL = lsSQL
                + " GROUP BY a.sTransNox "
                + " HAVING SUM(b.nApproved - (b.nIssueQty + b.nOrderQty)) > 0 ";
        //For General Only
        if(Master().getIndustryID() == null || "".equals(Master().getIndustryID())){
            lsSQL = lsSQL + " UNION " +  getPOQuotation_SQL();
            lsFilterCondition = String.join(" AND ",  //" a.sIndstCdx = " + SQLUtil.toSQL(Master().getIndustryID()),
                    " a.sCompnyID = " + SQLUtil.toSQL(Master().getCompanyID()),
                    " a.sSupplier = " + SQLUtil.toSQL(Master().getSupplierID()),
                    " a.sCategrCd = " + SQLUtil.toSQL(Master().getCategoryCode()),
                    " a.cTranStat = " + SQLUtil.toSQL(POQuotationStatus.APPROVED));
            lsSQL = lsSQL + " WHERE " + lsFilterCondition;
            //Do not include branch according to ma'am she 10082025
//            if (!poGRider.isMainOffice() || !poGRider.isWarehouse()) {
//                lsSQL = lsSQL + " AND a.sBranchCd = " + SQLUtil.toSQL(poGRider.getBranchCode());
//            }
            lsSQL = lsSQL + " GROUP BY a.sTransNox ";
        }
        
        lsSQL = lsSQL + " ORDER BY dTransact, sTransNox DESC";
        
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
                    request.put("sTransNox", loRS.getString("sTransNox"));
                    request.put("sBranchCd", loRS.getString("sBranchCd"));
                    request.put("dTransact", loRS.getDate("dTransact"));
                    request.put("sReferNox", loRS.getString("sReferNox"));
                    request.put("cTranStat", loRS.getString("cTranStat"));
                    request.put("sBranchNm", loRS.getString("sBranchNm"));
                    request.put("total_details", loRS.getInt("total_details"));
                    request.put("request_type", loRS.getString("request_type"));

                    dataArray.add(request);
                    lnctr++;
                }
                loJSON.put("data", dataArray);
                loJSON.put("result", "success");
                loJSON.put("message", "Record loaded successfully.");
            } else {
                loJSON.put("result", "error");
                loJSON.put("data", new JSONArray());
                loJSON.put("message", "No records found.");
            }
            MiscUtil.close(loRS);
        } catch (SQLException e) {
            loJSON.put("result", "error");
            loJSON.put("message", e.getMessage());
        }

        return loJSON;
    }

    public JSONObject addStockRequestOrdersToPODetail(String transactionNo) throws CloneNotSupportedException, SQLException, GuanzonException {
        poJSON = new JSONObject();
        InvWarehouseControllers loTrans = new InvWarehouseControllers(poGRider, logwrapr);
        poJSON = loTrans.StockRequest().InitTransaction();

        if (!"success".equals(poJSON.get("result"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "No records found.");
            return poJSON;
        }

        poJSON = loTrans.StockRequest().OpenTransaction(transactionNo);
        if (!"success".equals(poJSON.get("result"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "No records found.");
            return poJSON;
        }

        if (areAllStockRequestDetailsInPODetail(loTrans)) {
            poJSON.put("result", "error");
            poJSON.put("message", "All stock request details are already in purchase order detail.");
            return poJSON;
        }
        String detailQuery = "SELECT b.sStockIDx "
                + "FROM inv_stock_request_master a "
                + "LEFT JOIN branch e ON a.sBranchCd = e.sBranchCd "
                + "LEFT JOIN inv_stock_request_detail b ON a.sTransNox = b.sTransNox "
                + "LEFT JOIN inventory c ON b.sStockIDx = c.sStockIDx "
                + "LEFT JOIN inv_supplier g ON g.sStockIDx = c.sStockIDx "
                + "LEFT JOIN category h ON h.sCategrCd = c.sCategCd1";

        String lsFilterCondition = String.join(" AND ",
                " a.sIndstCdx LIKE " + SQLUtil.toSQL("%" + Master().getIndustryID()),
                " e.sCompnyID LIKE " + SQLUtil.toSQL("%" + Master().getCompanyID()),
                " h.sCategrCd LIKE " + SQLUtil.toSQL("%" + Master().getCategoryCode()),
                " g.sSupplier LIKE " + SQLUtil.toSQL("%" + Master().getSupplierID()),
                " b.nApproved > 0",
                " a.cTranStat = " + SQLUtil.toSQL(Logical.YES));

        detailQuery = MiscUtil.addCondition(detailQuery, lsFilterCondition);
        System.out.println("Executing SQL: " + detailQuery);
        // Fetch valid stock IDs
        Set<String> validStockIds = new HashSet<>();
        try (ResultSet rsDetail = poGRider.executeQuery(detailQuery)) {
            if (rsDetail == null) {
                poJSON.put("result", "error");
                poJSON.put("message", "Query execution failed.");
                return poJSON;
            }
            while (rsDetail.next()) {
                validStockIds.add(rsDetail.getString("sStockIDx"));
            }
        }

        boolean allProcessed = true;

        for (int lnCtr = 0; lnCtr < loTrans.StockRequest().getDetailCount(); lnCtr++) {
            if (!validStockIds.contains(loTrans.StockRequest().Detail(lnCtr).getStockId())) {
                continue; // Skip details that do not match the master conditions
            }

            // If at least one stock is not fully processed, set flag to false
            if (loTrans.StockRequest().Detail(lnCtr).getApproved()
                    != loTrans.StockRequest().Detail(lnCtr).getIssued()
                    + loTrans.StockRequest().Detail(lnCtr).getPurchase()) {
                allProcessed = false;
            }

            boolean exists = false;
            for (int lnRow = 0; lnRow < getDetailCount(); lnRow++) {
                if (Detail(lnRow).getSouceNo().equals(loTrans.StockRequest().Detail(lnCtr).getTransactionNo())
                        && Detail(lnRow).getStockID().equals(loTrans.StockRequest().Detail(lnCtr).getStockId())) {
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                double remainingStock = loTrans.StockRequest().Detail(lnCtr).getApproved()
                        - (loTrans.StockRequest().Detail(lnCtr).getIssued() + loTrans.StockRequest().Detail(lnCtr).getPurchase());
                if (remainingStock > 0) {
                    AddDetail();
                    int lnLastIndex = getDetailCount() - 1;
                    Detail(lnLastIndex).setSouceNo(loTrans.StockRequest().Detail(lnCtr).getTransactionNo());
                    Detail(lnLastIndex).setTransactionNo(loTrans.StockRequest().Detail(lnCtr).getTransactionNo());
                    Detail(lnLastIndex).setEntryNo(loTrans.StockRequest().Detail(lnCtr).getEntryNumber());
                    Detail(lnLastIndex).setStockID(loTrans.StockRequest().Detail(lnCtr).getStockId());
                    Detail(lnLastIndex).setRecordOrder(0);
                    Detail(lnLastIndex).setUnitPrice(loTrans.StockRequest().Detail(lnCtr).Inventory().getCost().doubleValue());
                    Detail(lnLastIndex).setQuantity(0);
                    Detail(lnLastIndex).setSouceCode(SOURCE_CODE);
                }
            }
        }

        // ✅ Only check `allProcessed` **after** the loop
        if (allProcessed) {
            poJSON.put("result", "error");
            poJSON.put("message", "All records are already processed!");
            return poJSON;
        }

        poJSON.put("result", "success");
        poJSON.put("message", "Record loaded successfully.");
        return poJSON;
    }

    private boolean areAllStockRequestDetailsInPODetail(InvWarehouseControllers loTrans) {
        for (int lnCtr = 0; lnCtr < loTrans.StockRequest().getDetailCount(); lnCtr++) {
            boolean found = false;

            for (int lnRow = 0; lnRow < getDetailCount(); lnRow++) {
                if (Detail(lnRow).getSouceNo().equals(loTrans.StockRequest().Detail(lnCtr).getTransactionNo())
                        && Detail(lnRow).getStockID().equals(loTrans.StockRequest().Detail(lnCtr).getStockId()
                        )) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                return false;
            }
        }
        return true;
    }
    
    public JSONObject addPOQuotationToPODetail(String transactionNo) throws CloneNotSupportedException, SQLException, GuanzonException {
        poJSON = new JSONObject();
        QuotationControllers loTrans = new QuotationControllers(poGRider, logwrapr);
        poJSON = loTrans.POQuotation().InitTransaction();

        if (!"success".equals(poJSON.get("result"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "No records found.");
            return poJSON;
        }

        poJSON = loTrans.POQuotation().OpenTransaction(transactionNo);
        if (!"success".equals(poJSON.get("result"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "No records found.");
            return poJSON;
        }

        if (areAllQuotationControllerIsInPODetail(loTrans)) {
            poJSON.put("result", "error");
            poJSON.put("message", "All PO Quotation details are already in purchase order detail.");
            return poJSON;
        }

        poJSON = checkExistingPO(transactionNo,loTrans.POQuotation().getSourceCode());
        if ("error".equals(poJSON.get("result"))) {
            return poJSON;
        }
        
//        boolean allProcessed = true;
        for (int lnCtr = 0; lnCtr < loTrans.POQuotation().getDetailCount(); lnCtr++) {
            boolean exists = false;
            for (int lnRow = 0; lnRow < getDetailCount(); lnRow++) {
                if(loTrans.POQuotation().Detail(lnCtr).getReplaceId() != null && !"".equals(loTrans.POQuotation().Detail(lnCtr).getReplaceId())){
                    if (Detail(lnRow).getSouceNo().equals(loTrans.POQuotation().Detail(lnCtr).getTransactionNo())
                            && Detail(lnRow).getStockID().equals(loTrans.POQuotation().Detail(lnCtr).getReplaceId())) {
                        exists = true;
                        break;
                    }
                } else {
                    if (Detail(lnRow).getSouceNo().equals(loTrans.POQuotation().Detail(lnCtr).getTransactionNo())
                            && Detail(lnRow).getStockID().equals(loTrans.POQuotation().Detail(lnCtr).getStockId())) {
                        exists = true;
                        break;
                    }
                } 
                
                
            }

            if (!exists) {
                int lnLastIndex = getDetailCount() - 1;
                Detail(lnLastIndex).setSouceNo(loTrans.POQuotation().Detail(lnCtr).getTransactionNo());
                Detail(lnLastIndex).setTransactionNo(loTrans.POQuotation().Detail(lnCtr).getTransactionNo());
                Detail(lnLastIndex).setRecordOrder(loTrans.POQuotation().Detail(lnCtr).getQuantity());
                Detail(lnLastIndex).setUnitPrice(loTrans.POQuotation().Detail(lnCtr).getUnitPrice());
                Detail(lnLastIndex).setQuantity(loTrans.POQuotation().Detail(lnCtr).getQuantity());
                Detail(lnLastIndex).setSouceCode(loTrans.POQuotation().getSourceCode());
                
                if(loTrans.POQuotation().Detail(lnCtr).getReplaceId() != null && !"".equals(loTrans.POQuotation().Detail(lnCtr).getReplaceId())){
                    Detail(lnLastIndex).setStockID(loTrans.POQuotation().Detail(lnCtr).getReplaceId());
                } else {
                    Detail(lnLastIndex).setStockID(loTrans.POQuotation().Detail(lnCtr).getStockId());
                }                
                AddDetail();
            }
        }

        // ✅ Only check `allProcessed` **after** the loop
//        if (allProcessed) {
//            poJSON.put("result", "error");
//            poJSON.put("message", "All records are already processed!");
//            return poJSON;
//        }

        poJSON.put("result", "success");
        poJSON.put("message", "Record loaded successfully.");
        return poJSON;
    }

    private boolean areAllQuotationControllerIsInPODetail(QuotationControllers loTrans) {
        for (int lnCtr = 0; lnCtr < loTrans.POQuotation().getDetailCount(); lnCtr++) {
            boolean found = false;

            for (int lnRow = 0; lnRow < getDetailCount(); lnRow++) {
                if(loTrans.POQuotation().Detail(lnCtr).getReplaceId() != null && !"".equals(loTrans.POQuotation().Detail(lnCtr).getReplaceId())){
                    if (Detail(lnRow).getSouceNo().equals(loTrans.POQuotation().Detail(lnCtr).getTransactionNo())
                            && Detail(lnRow).getStockID().equals(loTrans.POQuotation().Detail(lnCtr).getReplaceId()
                            )) {
                        found = true;
                        break;
                    }
                } else {
                    if (Detail(lnRow).getSouceNo().equals(loTrans.POQuotation().Detail(lnCtr).getTransactionNo())
                            && Detail(lnRow).getStockID().equals(loTrans.POQuotation().Detail(lnCtr).getStockId()
                            )) {
                        found = true;
                        break;
                    }
                }
            }

            if (!found) {
                return false;
            }
        }
        return true;
    }

    public JSONObject getPurchaseOrder(String fsSupplierID, String fsReferID) throws SQLException, GuanzonException {
        JSONObject loJSON = new JSONObject();
        String lsTransStat = "";
        if (psTranStat.length() > 1) {
            for (int lnCtr = 0; lnCtr <= psTranStat.length() - 1; lnCtr++) {
                lsTransStat += ", " + SQLUtil.toSQL(Character.toString(psTranStat.charAt(lnCtr)));
            }
            lsTransStat = " AND ( a.cTranStat IN (" + lsTransStat.substring(2) + ")"
                        + " OR (ASCII(a.cTranStat) - 64) IN (" + lsTransStat.substring(2) + ") )";
        } else {
            lsTransStat = " AND ( a.cTranStat = " + SQLUtil.toSQL(psTranStat)
                        + " OR (ASCII(a.cTranStat) - 64) = " + SQLUtil.toSQL(psTranStat) +" ) ";
        }

        String lsSQL = " SELECT "
                + "  a.sTransNox,"
                + "  c.sBranchNm,"
                + "  a.sBranchCd,"
                + "  a.dTransact,"
                + "  a.sTransNox,"
                + "  a.cTranStat"
                + " FROM po_master a "
                + " LEFT JOIN po_detail b ON b.sTransNox = a.sTransNox"
                + " LEFT JOIN branch c ON a.sBranchCd = c.sBranchCd"
                + " LEFT JOIN industry d ON a.sIndstCdx = d.sIndstCdx";
        String lsFilterCondition = String.join(" AND ",
                " a.sIndstCdx = " + SQLUtil.toSQL(Master().getIndustryID()),
                " a.sCompnyID = " + SQLUtil.toSQL(Master().getCompanyID()),
                " a.sSupplier LIKE " + SQLUtil.toSQL("%" + fsSupplierID),
                " a.sCategrCd = " + SQLUtil.toSQL(Master().getCategoryCode()),
                " a.sTransNox LIKE " + SQLUtil.toSQL("%" + fsReferID));
        lsSQL = MiscUtil.addCondition(lsSQL, lsFilterCondition);
        if (!psTranStat.isEmpty()) {
            lsSQL = lsSQL + lsTransStat;
        }
        if (!poGRider.isMainOffice() || !poGRider.isWarehouse()) {
            lsSQL = lsSQL + " AND a.sBranchCd LIKE " + SQLUtil.toSQL(poGRider.getBranchCode());
        }
        lsSQL = lsSQL + " GROUP BY  a.sTransNox"
                + " ORDER BY dTransact ASC";
        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);

        int lnCtr = 0;
        if (MiscUtil.RecordCount(loRS) >= 0) {
            paPOMaster = new ArrayList<>();
            while (loRS.next()) {
                // Print the result set
                System.out.println("sTransNox: " + loRS.getString("sTransNox"));
                System.out.println("dTransact: " + loRS.getDate("dTransact"));
                System.out.println("------------------------------------------------------------------------------");

                paPOMaster.add(POMasterList());
                paPOMaster.get(paPOMaster.size() - 1).openRecord(loRS.getString("sTransNox"));
                lnCtr++;
            }
            System.out.println("Records found: " + lnCtr);
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

    public String getInventoryTypeCode() throws SQLException {
        String lsSQL = "SELECT a.sInvTypCd FROM category a"
                + " LEFT JOIN inv_type b ON a.sInvTypCd = b.sInvTypCd";
        lsSQL = MiscUtil.addCondition(lsSQL, " a.sIndstCdx = " + SQLUtil.toSQL(Master().getIndustryID()
                + " AND a.sCategrCd = " + SQLUtil.toSQL(Master().getCategoryCode())));

        ResultSet loRS = poGRider.executeQuery(lsSQL);
        String inventoryTypeCode = null;

        if (loRS.next()) {
            inventoryTypeCode = loRS.getString("sInvTypCd");
        }

        MiscUtil.close(loRS);
        return inventoryTypeCode;
    }

    public JSONObject setDiscountRate(String fsValue) {
        poJSON = new JSONObject();
        if (fsValue == null || fsValue.isEmpty()) {
            fsValue = "0.0000";
        }
        double lnTotalAmount = (double) Master().getTranTotal();
        if (lnTotalAmount == 0.0000) {
            poJSON.put("message", "You're not allowed to enter discount rate, no detail amount entered.");
            poJSON.put("result", "error");
            Master().setDiscount(PurchaseOrderStaticData.default_value_double);
            computeNetTotal();
            return poJSON;
        }
        double lnDiscountRate = Double.parseDouble(fsValue);
        if (lnDiscountRate < PurchaseOrderStaticData.default_value_double || lnDiscountRate > 100.0000) {
            poJSON.put("message", "Invalid Discount Rate.  Must be between 0.00 and 100.00");
            poJSON.put("result", "error");
            Master().setDiscount(PurchaseOrderStaticData.default_value_double);
            computeNetTotal();
            return poJSON;
        }

        Master().setDiscount(lnDiscountRate);
        computeNetTotal();
        double lnNetTotal = Master().getNetTotal().doubleValue();
        if (lnNetTotal < PurchaseOrderStaticData.default_value_double) {
            poJSON.put("message", "Invalid Total Transaction Amount");
            poJSON.put("result", "error");
            Master().setAdditionalDiscount(PurchaseOrderStaticData.default_value_double);
            computeNetTotal();
            return poJSON;
        }
        poJSON.put("result", "success");
        return poJSON;
    }

    public JSONObject setDiscountAmount(String fsValue) {
        poJSON = new JSONObject();
        if (fsValue == null || fsValue.isEmpty()) {
            fsValue = "0.0000";
        }
        if (Double.parseDouble(fsValue) >= 1000000.0000) {
            poJSON.put("message", "Discount amount must not exceed 1 Million.");
            poJSON.put("result", "error");
            Master().setAdditionalDiscount(PurchaseOrderStaticData.default_value_double);
            computeNetTotal();
            return poJSON;
        }
        double lnTotalAmount = Master().getTranTotal().doubleValue() - ((Master().getDiscount().doubleValue() / 100) * Master().getTranTotal().doubleValue());
        if (lnTotalAmount == PurchaseOrderStaticData.default_value_double) {
            poJSON.put("message", "You're not allowed to enter discount amount, no amount entered.");
            poJSON.put("result", "error");
            Master().setAdditionalDiscount(PurchaseOrderStaticData.default_value_double);
            computeNetTotal();
            return poJSON;
        }
        double lnDiscountAmount = Double.parseDouble(fsValue.replace(",", ""));
        if (lnDiscountAmount < 0.0 || lnDiscountAmount > lnTotalAmount) {
            poJSON.put("message", "Invalid Discount Amount");
            poJSON.put("result", "error");
            Master().setAdditionalDiscount(PurchaseOrderStaticData.default_value_double);
            computeNetTotal();
            return poJSON;
        }

        Master().setAdditionalDiscount(lnDiscountAmount);
        computeNetTotal();
        double lnNetTotal = Master().getNetTotal().doubleValue();
        if (lnNetTotal < PurchaseOrderStaticData.default_value_double) {
            poJSON.put("message", "Invalid Total Transaction Amount");
            poJSON.put("result", "error");
            Master().setAdditionalDiscount(PurchaseOrderStaticData.default_value_double);
            computeNetTotal();
            return poJSON;
        }
        poJSON.put("result", "success");
        return poJSON;
    }

    public JSONObject setAdvancePaymentRate(String fsValue) {
        poJSON = new JSONObject();
        if (fsValue == null || fsValue.isEmpty()) {
            fsValue = "0.00";
        }
        double amountAfterDiscounts = Master().getTranTotal().doubleValue() - (((Master().getTranTotal().doubleValue() / 100) * Master().getDiscount().doubleValue())
                + Master().getAdditionalDiscount().doubleValue());

        if (amountAfterDiscounts <= PurchaseOrderStaticData.default_value_double) {
            poJSON.put("message", "Invalid to enter Advance Payment Rate, the total transaction amount is 0.0000");
            poJSON.put("result", "error");
            Master().setDownPaymentRatesPercentage(PurchaseOrderStaticData.default_value_double);
            computeNetTotal();
            return poJSON;
        }

        double lnAdvanceRate = Double.parseDouble(fsValue);
        if (lnAdvanceRate < PurchaseOrderStaticData.default_value_double || lnAdvanceRate > 100.0000) {
            poJSON.put("message", "Invalid Advance Payment Rate. Must be between 0.0000 and 100.0000");
            poJSON.put("result", "error");
            Master().setDownPaymentRatesPercentage(PurchaseOrderStaticData.default_value_double);
            computeNetTotal();
            return poJSON;
        }

        Master().setDownPaymentRatesPercentage(lnAdvanceRate);
        computeNetTotal();
        double lnNetTotal = Master().getNetTotal().doubleValue();
        if (lnNetTotal < PurchaseOrderStaticData.default_value_double) {
            poJSON.put("message", "Invalid Total Transaction Amount");
            poJSON.put("result", "error");
            Master().setDownPaymentRatesPercentage(PurchaseOrderStaticData.default_value_double);
            computeNetTotal();
            return poJSON;
        }

        poJSON.put("result", "success");
        return poJSON;
    }

    public JSONObject setAdvancePaymentAmount(String fsValue) {
        poJSON = new JSONObject();
        if (fsValue == null || fsValue.isEmpty()) {
            fsValue = "0.0000";
        }
        double totalAmountAfterDiscount = Master().getTranTotal().doubleValue() - ((Master().getTranTotal().doubleValue() * Master().getDiscount().doubleValue())
                + Master().getAdditionalDiscount().doubleValue());
        double totalAdvRateAmount = totalAmountAfterDiscount * Master().getDownPaymentRatesPercentage().doubleValue();
        double totalAmountDiscountWithRate = totalAmountAfterDiscount - totalAdvRateAmount;

        double lnAdvanceAmount = Double.parseDouble(fsValue.replace(",", ""));
        if (totalAmountDiscountWithRate <= PurchaseOrderStaticData.default_value_double) {
            poJSON.put("message", "Invalid to enter Advance Payment Amount, the total transaction amount is 0.0000");
            poJSON.put("result", "error");
            Master().setDownPaymentRatesPercentage(PurchaseOrderStaticData.default_value_double);
            computeNetTotal();
            return poJSON;
        }

        if (lnAdvanceAmount < PurchaseOrderStaticData.default_value_double || lnAdvanceAmount > totalAmountDiscountWithRate) {
            poJSON.put("message", "Invalid Advance Payment Amount");
            poJSON.put("result", "error");
            Master().setDownPaymentRatesAmount(PurchaseOrderStaticData.default_value_double);
            computeNetTotal();
            return poJSON;
        }

        Master().setDownPaymentRatesAmount(lnAdvanceAmount);
        computeNetTotal();
        double lnNetTotal = Master().getNetTotal().doubleValue();
        if (lnNetTotal < PurchaseOrderStaticData.default_value_double) {
            poJSON.put("message", "Invalid Total Transaction Amount");
            poJSON.put("result", "error");
            Master().setDownPaymentRatesAmount(PurchaseOrderStaticData.default_value_double);
            computeNetTotal();
            return poJSON;
        }
        poJSON.put("result", "success");
        return poJSON;
    }

    public void computeNetTotal() {
        double totalAmount = Master().getTranTotal().doubleValue(); // Total transaction amount
        double discountRate = Master().getDiscount().doubleValue() / 100;
        System.out.printf("Discount: %.2f%%\n", discountRate);
        double discountRateWithTotalAmount = totalAmount * discountRate;
        double additionalDiscountAmount = Master().getAdditionalDiscount().doubleValue();

        // Amount after all discounts
        double totalDiscount = (discountRateWithTotalAmount + additionalDiscountAmount);

//        // Compute down payment from rate
//        double downPaymentRate = Master().getDownPaymentRatesPercentage().doubleValue(); // e.g., 0.20 for 20%
//        double downPaymentAmountFromRate = totalDiscount * downPaymentRate;
//
//        // Subtract down payment amount (fixed)
//        double downPaymentAmount = Master().getDownPaymentRatesAmount().doubleValue();
//        double totalDownPayment = downPaymentAmountFromRate + downPaymentAmount;
        // Final net total
//        double netTotal = totalAmount - totalDiscount - totalDownPayment;
//
        double netTotal = totalAmount - totalDiscount;
        Master().setNetTotal(netTotal);

    }

    public JSONObject netTotalChecker(int pnRow) {
        poJSON = new JSONObject();
        double NetTotl = Master().getNetTotal().doubleValue() + (Detail(pnRow).getQuantity().doubleValue() * Detail(pnRow).getUnitPrice().doubleValue());
        if (NetTotl >= 100000000.0000) {
            poJSON.put("result", "error");
            poJSON.put("message", "The net total exceeds the maximum allowed amount. Please reduce the value and try again.");
            Detail(pnRow).setQuantity(0);
            computeNetTotal();
            return poJSON;
        }
        poJSON.put("result", "success");
        return poJSON;
    }

    public JSONObject printTransaction(String jasperType) {
        poJSON = new JSONObject();
        String watermarkPath = "D:\\GGC_Maven_Systems\\Reports\\images\\draft.png"; //set draft as default
        try {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("sBranchNm", poGRider.getBranchName());
            parameters.put("sAddressx", poGRider.getAddress());
            parameters.put("sCompnyNm", poGRider.getClientName());
            parameters.put("sTransNox", Master().getTransactionNo());
            parameters.put("sDestination", Master().Branch().getBranchName());
            
            //TODO
            if(Master().getTransactionStatus().equals(PurchaseOrderStatus.APPROVED)){
                parameters.put("sApprval1","MX0125024178 - Yambao, Jeffrey Torres" ); //poGRider.getLogName()
                parameters.put("sApprval2", "MX0125024179 - Adversalo, Rex Soriano");    
                parameters.put("sApprval3", "");
            } else {
                parameters.put("sApprval1",""); //poGRider.getLogName()
                parameters.put("sApprval2", "");
                parameters.put("sApprval3", "");
            }
            parameters.put("sRemarks", Master().getRemarks());
            parameters.put("dTransDte", new java.sql.Date(Master().getTransactionDate().getTime()));
            parameters.put("dDatexxx", new java.sql.Date(poGRider.getServerDate().getTime()));
            parameters.put("nTtlAmntx", Master().getTranTotal().doubleValue());
            parameters.put("nDiscRate", Master().getDiscount().doubleValue());
            parameters.put("nDiscAmnt", Master().getAdditionalDiscount().doubleValue());
            parameters.put("nAdvAmntx", Master().getDownPaymentRatesAmount().doubleValue());
            parameters.put("nAdvRatex", Master().getDownPaymentRatesPercentage().doubleValue());
            parameters.put("nNetAmntx", Master().getNetTotal().doubleValue());

            switch (Master().getTransactionStatus()) {
                case PurchaseOrderStatus.POSTED:
                case PurchaseOrderStatus.APPROVED:
                    if ("1".equals(Master().getPrint())) {
                        watermarkPath = "D:\\GGC_Maven_Systems\\Reports\\images\\approvedreprint.png";
                    } else {
                        watermarkPath = "D:\\GGC_Maven_Systems\\Reports\\images\\approved.png";
                    }
                    break;
            }
            parameters.put("watermarkImagePath", watermarkPath);
            List<OrderDetail> orderDetails = new ArrayList<>();

            double lnTotal = 0.0000;
            
            for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
                lnTotal = Detail(lnCtr).getUnitPrice().doubleValue() * Detail(lnCtr).getQuantity().doubleValue();

                
                orderDetails.add(new OrderDetail(lnCtr + 1, // Line No.
                        safeString(Detail(lnCtr).getSouceNo()), // Source No
                        safeString(Detail(lnCtr).Inventory().getBarCode()), // Barcode
                        safeString(Detail(lnCtr).Inventory().Brand().getDescription()), // Brand
                        safeString(Detail(lnCtr).Inventory().Variant().getDescription()) + " "
                        + safeString(Detail(lnCtr).Inventory().Variant().getYearModel()), // Variant + Year Model
                        safeString(Detail(lnCtr).Inventory().Model().getDescription()), // Model
                        safeString(Detail(lnCtr).Inventory().Measure().getDescription()),// Unit of Measure
                        safeString(Detail(lnCtr).Inventory().Color().getDescription()), // Color
                        safeString(Detail(lnCtr).Inventory().getDescription()), // Item Description
                        Detail(lnCtr).getUnitPrice() != null
                        ? Detail(lnCtr).getUnitPrice().doubleValue() : 0.00, // Unit Price
                        Detail(lnCtr).getQuantity() != null
                        ? Detail(lnCtr).getQuantity().doubleValue() : 0.00, // Quantity
                        lnTotal // Line Total
                ));
            }

            // 3. Create data source
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(orderDetails);

            // 4. Compile and fill report
            String jrxmlPath = "";
            switch (jasperType) {
                case PurchaseOrderStaticData.Printing_CAR_MC_MPUnit_Appliance:
                    jrxmlPath = "D:\\GGC_Maven_Systems\\Reports\\PurchaseOrderCarMcMPUnitAppliance.jrxml"; //TODO
                    break;
                case PurchaseOrderStaticData.Printing_CARSp_MCSp_General:
                    jrxmlPath = "D:\\GGC_Maven_Systems\\Reports\\PurchaseOrderCARSpMCSpGeneral.jrxml"; //TODO PurchaseOrderPedritos
                    break;
                case PurchaseOrderStaticData.Printing_Pedritos:
                    jrxmlPath = "D:\\GGC_Maven_Systems\\Reports\\PurchaseOrderPedritos.jrxml"; //TODO PurchaseOrderPedritos
                    break;
                default:
                    throw new AssertionError();
            }

            JasperReport jasperReport;

            jasperReport = JasperCompileManager.compileReport(jrxmlPath);

            JasperPrint jasperPrint;
            jasperPrint = JasperFillManager.fillReport(
                    jasperReport,
                    parameters,
                    dataSource
            );

            CustomJasperViewer viewer = new CustomJasperViewer(jasperPrint);
            viewer.setVisible(true);

            poJSON.put("result", "success");
        } catch (JRException | SQLException | GuanzonException ex) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction print aborted!");
            Logger
                    .getLogger(PurchaseOrder.class
                            .getName()).log(Level.SEVERE, null, ex);
        }
        return poJSON;

    }
    private String safeString(Object value) {
        return value == null ? "" : value.toString();
    }
    public static class OrderDetail {

        private Integer nRowNo;
        private String sOrderNo;
        private String sBarcode;
        private String sBrandName;
        private String sVariant;
        private String sModel;
        private String sMeasure;
        private String sColor;
        private String sDescription;
        private double nUprice;
        private double nOrder;
        private double nTotal;
        private double nDiscRate;
        private double nDiscAmt;
        private double nAdvRate;
        private double nAdvAmt;
        private double nNetAmt;

        public OrderDetail(Integer rowNo, String orderNo, String barcode, String BrandName, String Variant, String Model, String Measure, String Color, String description,
                double uprice, double order, double nTotal) {
            this.nRowNo = rowNo;
            this.sOrderNo = orderNo;
            this.sBarcode = barcode;
            this.sBrandName = BrandName;
            this.sVariant = Variant;
            this.sModel = Model;
            this.sMeasure = Measure;
            this.sColor = Color;
            this.sDescription = description;
            this.nUprice = uprice;
            this.nOrder = order;
            this.nTotal = nTotal;
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

        public String getsBrandName() {
            return sBrandName;
        }

        public String getsVariant() {
            return sVariant;
        }

        public String getsModel() {
            return sModel;
        }

        public String getsMeasure() {
            return sMeasure;
        }

        public String getsColor() {
            return sColor;
        }

        public String getsDescription() {
            return sDescription;
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

        public double getnDiscRate() {
            return nDiscRate;
        }

        public double getnDiscAmt() {
            return nDiscAmt;
        }

        public double getnAdvRate() {
            return nAdvRate;
        }

        public double getnAdvAmt() {
            return nAdvAmt;
        }

        public double getnNetAmt() {
            return nNetAmt;
        }

    }

    public class CustomJasperViewer extends JasperViewer {

        public CustomJasperViewer(JasperPrint jasperPrint) {
            super(jasperPrint, false);
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

                                //if ever na kailangan e hide si button save
//                                if (button.getToolTipText() != null) {
//                                    if (button.getToolTipText().equals("Save")) {
//                                        button.setEnabled(false);  // Disable instead of hiding
//                                        button.setVisible(false);  // Hide it completely
//                                    }
//                                }
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
                                                    ShowMessageFX.Warning("Printing was canceled by the user.", "Print Purchase Order", null);
                                                    SwingUtilities.invokeLater(() -> CustomJasperViewer.this.toFront());

                                                });
                                            }
                                        } catch (JRException ex) {
                                            Platform.runLater(() -> {
                                                ShowMessageFX.Warning("Print Failed: " + ex.getMessage(), "Computerized Accounting System", null);
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

        private void PrintTransaction(boolean fbIsPrinted) throws SQLException, CloneNotSupportedException, GuanzonException {
            poJSON = new JSONObject();
            if (fbIsPrinted) {
                if (((String) poMaster.getValue("cTranStat")).equals(PurchaseOrderStatus.APPROVED)) {
                    poJSON = OpenTransaction((String) poMaster.getValue("sTransNox"));
                    if ("error".equals((String) poJSON.get("result"))) {
                        Platform.runLater(() -> {
                            ShowMessageFX.Warning((String) poJSON.get("message"), "Print Purchase Order", null);
                            SwingUtilities.invokeLater(() -> CustomJasperViewer.this.toFront());
                        });
                        fbIsPrinted = false;
                    }
                    poJSON = UpdateTransaction();
                    if ("error".equals((String) poJSON.get("result"))) {
                        Platform.runLater(() -> {
                            ShowMessageFX.Warning((String) poJSON.get("message"), "Print Purchase Order", null);
                            SwingUtilities.invokeLater(() -> CustomJasperViewer.this.toFront());
                        });
                        fbIsPrinted = false;
                    }

                    poMaster.setValue("dModified", poGRider.getServerDate());
                    poMaster.setValue("sModified", poGRider.getUserID());
                    poMaster.setValue("cPrintxxx", Logical.YES);

                    poJSON = SaveTransaction();
                    if ("error".equals((String) poJSON.get("result"))) {
                        Platform.runLater(() -> {
                            ShowMessageFX.Warning((String) poJSON.get("message"), "Print Purchase Order", null);
                            SwingUtilities.invokeLater(() -> CustomJasperViewer.this.toFront());
                        });
                        fbIsPrinted = false;
                    }
                }
            }

            if (fbIsPrinted) {
                Platform.runLater(() -> {
                    ShowMessageFX.Information("Transaction printed successfully.", "Print Purchase Order", null);
                    SwingUtilities.invokeLater(() -> CustomJasperViewer.this.toFront());
                });
            } else {
                Platform.runLater(() -> {
                    ShowMessageFX.Information("Transaction printed aborted.", "Print Purchase Order", null);
                    SwingUtilities.invokeLater(() -> CustomJasperViewer.this.toFront());
                });
            }
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
    
    
//    private JSONObject getLastTransStatus(){
//        poJSON = new JSONObject();
//        
//            poJSON = poGRider.g
//        
//        return poJSON;
//    }

}
