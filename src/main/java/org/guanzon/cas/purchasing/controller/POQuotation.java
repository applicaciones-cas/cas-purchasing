/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.guanzon.cas.purchasing.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.rowset.CachedRowSet;
import org.apache.commons.codec.binary.Base64;
import org.guanzon.appdriver.agent.ActionAuthManager;
import org.guanzon.appdriver.agent.MatrixAuthChecker;
import org.guanzon.appdriver.agent.ShowDialogFX;
import org.guanzon.appdriver.agent.ShowMessageFX;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.agent.services.Transaction;
import org.guanzon.appdriver.agent.systables.Model_Transaction_Attachment;
import org.guanzon.appdriver.agent.systables.SysTableContollers;
import org.guanzon.appdriver.agent.systables.TransactionAttachment;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscReplUtil;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.base.WebFile;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.constant.RecordStatus;
import org.guanzon.appdriver.constant.UserRight;
import org.guanzon.appdriver.iface.GValidator;
import org.guanzon.appdriver.token.RequestAccess;
import org.guanzon.cas.client.Client;
import org.guanzon.cas.client.account.AP_Client_Master;
import org.guanzon.cas.client.services.ClientControllers;
import org.guanzon.cas.inv.InvTransCons;
import org.guanzon.cas.inv.Inventory;
import org.guanzon.cas.inv.services.InvControllers;
import org.guanzon.cas.parameter.Branch;
import org.guanzon.cas.parameter.CategoryLevel2;
import org.guanzon.cas.parameter.Company;
import org.guanzon.cas.parameter.Department;
import org.guanzon.cas.parameter.Term;
import org.guanzon.cas.parameter.services.ParamControllers;
import org.guanzon.cas.parameter.services.ParamModels;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.guanzon.cas.purchasing.model.Model_PO_Quotation_Detail;
import org.guanzon.cas.purchasing.model.Model_PO_Quotation_Master;
import org.guanzon.cas.purchasing.model.Model_PO_Quotation_Request_Detail;
import org.guanzon.cas.purchasing.model.Model_PO_Quotation_Request_Supplier;
import org.guanzon.cas.purchasing.services.QuotationControllers;
import org.guanzon.cas.purchasing.services.QuotationModels;
import org.guanzon.cas.purchasing.status.POQuotationRequestStatus;
import org.guanzon.cas.purchasing.status.POQuotationStatus;
import org.guanzon.cas.purchasing.validator.POQuotationValidatorFactory;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author Arsiela
 */
public class POQuotation extends Transaction {
    private String psIndustryId = "";
    private String psCompanyId = "";
    private String psCategorCd = "";
    
    private String psSearchBranch;
    private String psSearchDepartment;
    private String psSearchSupplier;
    private String psSearchCategory;
    
    List<Model_PO_Quotation_Request_Supplier> paPORequestSupplier;
    List<Model_PO_Quotation_Master> paMasterList;
    List<Model> paDetailRemoved;
    List<TransactionAttachment> paAttachments;
    
    POQuotationRequest poQuoationRequest;
    
    public JSONObject InitTransaction() {
        SOURCE_CODE = InvTransCons.PURCHASE_ORDER_QUOTATION;

        poMaster = new QuotationModels(poGRider).POQuotationMaster();
        poDetail = new QuotationModels(poGRider).POQuotationDetails();

        paMasterList = new ArrayList<>();
        paDetail = new ArrayList<>();
        paDetailRemoved = new ArrayList<>();
        paPORequestSupplier = new ArrayList<>();
        paAttachments = new ArrayList<>();

        return initialize();
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
        resetOthers();
        Detail().clear();
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
    
    public JSONObject ConfirmTransaction(String remarks)
            throws ParseException,
            SQLException,
            GuanzonException,
            CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = POQuotationStatus.CONFIRMED;

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
                        poGRider.beginTrans("UPDATE STATUS", "Confirm Transaction", SOURCE_CODE, Master().getTransactionNo());

                        System.out.println("Status: " + lsStatus);
                        lsStatus = Character.toString((char)(64 + Integer.parseInt(lsStatus)));
                        System.out.println("Status: " + lsStatus);
                        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false);
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
        
        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        if(check != null){
            check.postAuth();
        }
        
        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "Transaction confirmed successfully.");
        return poJSON;
    }
    
    public JSONObject ApproveTransaction(String remarks)
            throws ParseException,
            SQLException,
            GuanzonException,
            CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = POQuotationStatus.APPROVED;

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
                        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false);
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
        
        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false, pbWthParent);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        if(check != null){
            check.postAuth();
        }
        
        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "Transaction approved successfully.");
        return poJSON;
    }
    
    public JSONObject DisApproveTransaction(String remarks)
            throws ParseException,
            SQLException,
            GuanzonException,
            CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = POQuotationStatus.CANCELLED;

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
                        poGRider.beginTrans("UPDATE STATUS", "Disapprove Transaction", SOURCE_CODE, Master().getTransactionNo());

                        System.out.println("Status: " + lsStatus);
                        lsStatus = Character.toString((char)(64 + Integer.parseInt(lsStatus)));
                        System.out.println("Status: " + lsStatus);
                        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false);
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
        
        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        if(check != null){
            check.postAuth();
        }
        
        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "Transaction disapproved successfully.");
        return poJSON;
    }
    
    public JSONObject VoidTransaction(String remarks)
            throws ParseException,
            SQLException,
            GuanzonException,
            CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = POQuotationStatus.VOID;
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
                        //check  the user level again then if he/she allow to approve
                        poGRider.beginTrans("UPDATE STATUS", "Void Transaction", SOURCE_CODE, Master().getTransactionNo());

                        System.out.println("Status: " + lsStatus);
                        lsStatus = Character.toString((char)(64 + Integer.parseInt(lsStatus)));
                        System.out.println("Status: " + lsStatus);
                        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false);
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
        
        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        if(check != null){
            check.postAuth();
        }
        
        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "Transaction voided successfully.");
        return poJSON;
    }
    
    public JSONObject CancelTransaction(String remarks)
            throws ParseException,
            SQLException,
            GuanzonException,
            CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = POQuotationStatus.CANCELLED;

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

                        System.out.println("Status: " + lsStatus);
                        lsStatus = Character.toString((char)(64 + Integer.parseInt(lsStatus)));
                        System.out.println("Status: " + lsStatus);
                        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false);
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
        
        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        if(check != null){
            check.postAuth();
        }
        
        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "Transaction cancelled successfully.");
        return poJSON;
    }
    
    public JSONObject PostTransaction(String remarks)
            throws ParseException,
            SQLException,
            GuanzonException,
            CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = POQuotationStatus.POSTED;

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
                        poGRider.beginTrans("UPDATE STATUS", "Post Transaction", SOURCE_CODE, Master().getTransactionNo());

                        System.out.println("Status: " + lsStatus);
                        lsStatus = Character.toString((char)(64 + Integer.parseInt(lsStatus)));
                        System.out.println("Status: " + lsStatus);
                        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false);
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
        
        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false, pbWthParent);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        if(check != null){
            check.postAuth();
        }
        
        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "Transaction posted successfully.");
        return poJSON;
    }
    
    public JSONObject ReturnTransaction(String remarks)
            throws ParseException,
            SQLException,
            GuanzonException,
            CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = POQuotationStatus.RETURNED;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

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
                MatrixAuthChecker check = new MatrixAuthChecker(poGRider, SOURCE_CODE, Master().getTransactionNo());
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
                        poGRider.beginTrans("UPDATE STATUS", "Return Transaction", SOURCE_CODE, Master().getTransactionNo());

                        System.out.println("Status: " + lsStatus);
                        lsStatus = Character.toString((char)(64 + Integer.parseInt(lsStatus)));
                        System.out.println("Status: " + lsStatus);
                        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false);
                        if (!"success".equals((String) poJSON.get("result"))) {
                            poGRider.rollbackTrans();
                            return poJSON;
                        }

                        poGRider.commitTrans();
                        
                        poJSON.put("result", "success");
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
        
        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "Transaction returned successfully.");
        return poJSON;
    }
    
    /*Search References*/
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
        String lsSQL = MiscUtil.addCondition(SQL_BROWSE, 
                   " a.sIndstCdx = " + SQLUtil.toSQL(psIndustryId)
                 + " AND a.sCategrCd = " + SQLUtil.toSQL(psCategorCd)
                );
        
        if (lsTransStat != null && !"".equals(lsTransStat)) {
            lsSQL = lsSQL + lsTransStat;
        }
        
        System.out.println("Executing SQL: " + lsSQL);
        poJSON = ShowDialogFX.Browse(poGRider,
                lsSQL,
                "",
                "Transaction Date»Transaction No»Supplier»Branch»Department»Category",
                "dTransact»sTransNox»SpplierNm»Branch»Department»Category2", 
                "a.dTransact»a.sTransNox»i.sCompnyNm»c.sBranchNm»h.sDeptName»f.sDescript", 
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
    
    public JSONObject searchTransaction(String fsBranch, String fsDepartment, String fsSupplier, String fsCateogry, String fsTransactionNo)
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
        
        String lsBranch = fsBranch != null && !"".equals(fsBranch) 
                                                    ? " AND c.sBranchNm LIKE " + SQLUtil.toSQL("%"+fsBranch)
                                                    : "";
        
        String lsDepartment = fsDepartment != null && !"".equals(fsDepartment) 
                                                    ? " AND h.sDeptName LIKE " + SQLUtil.toSQL("%"+fsDepartment)
                                                    : "";                                           

        String lsSupplier = fsSupplier != null && !"".equals(fsSupplier) 
                                                    ? " AND i.sCompnyNm LIKE " + SQLUtil.toSQL("%"+fsSupplier)
                                                    : "";    
        
        String lsCategory = fsCateogry != null && !"".equals(fsCateogry) 
                                                    ? " AND f.sDescript LIKE " + SQLUtil.toSQL("%"+fsCateogry)
                                                    : "";  
        
        String lsTransactionNo = fsTransactionNo != null && !"".equals(fsTransactionNo) 
                                                    ? " AND a.sTransNox LIKE " + SQLUtil.toSQL("%"+fsTransactionNo)
                                                    : "";

        initSQL();
        String lsSQL = MiscUtil.addCondition(SQL_BROWSE, 
                   " a.sIndstCdx = " + SQLUtil.toSQL(psIndustryId)
                 + " AND a.sCategrCd = " + SQLUtil.toSQL(psCategorCd)
                 + lsBranch
                 + lsDepartment
                 + lsSupplier
                 + lsCategory
                 + lsTransactionNo
                );
        
        if (lsTransStat != null && !"".equals(lsTransStat)) {
            lsSQL = lsSQL + lsTransStat;
        }

        System.out.println("Executing SQL: " + lsSQL);
        poJSON = ShowDialogFX.Browse(poGRider,
                lsSQL,
                "",
                "Transaction Date»Transaction No»Supplier»Branch»Department»Category",
                "dTransact»sTransNox»SpplierNm»Branch»Department»Category2", 
                "a.dTransact»a.sTransNox»i.sCompnyNm»c.sBranchNm»h.sDeptName»f.sDescript", 
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
//    
//    public JSONObject SearchInventory(String value, boolean byCode, int row) throws SQLException, GuanzonException {
//        poJSON = new JSONObject();
//        poJSON.put("row", row);
//        
//        if(Master().getSourceNo() == null || "".equals(Master().getSourceNo())){
//            poJSON.put("result", "error");
//            poJSON.put("message", "Source No is not set.");
//            return poJSON;
//        }
//        
//        Inventory object = new InvControllers(poGRider, logwrapr).Inventory();
//        String lsSQL = MiscUtil.addCondition(object.getSQ_Browse(), 
//                                            " a.sCategCd1 = " + SQLUtil.toSQL(Master().getCategoryCode())
//                                            + " AND a.sCategCd2 = " + SQLUtil.toSQL(Master().POQuotationRequest().getCategoryLevel2())
//                                            );
//        
//        System.out.println("Executing SQL: " + lsSQL);
//        poJSON = ShowDialogFX.Browse(poGRider,
//                lsSQL,
//                value,
//                "Barcode»Description»Brand»Model»Variant»UOM",
//                "sBarCodex»sDescript»xBrandNme»xModelNme»xVrntName»xMeasurNm",
//                "a.sBarCodex»a.sDescript»IFNULL(b.sDescript, '')»IFNULL(c.sDescript, '')»IFNULL(f.sDescript, '')»IFNULL(e.sDescript, '')",
//                byCode ? 0 : 1);
//
//        if (poJSON != null) {
//            poJSON = object.getModel().openRecord((String) poJSON.get("sStockIDx"));
//            if ("success".equals((String) poJSON.get("result"))) {
//                
//                if(Detail(row).getStockId().equals(object.getModel().getStockId()) 
//                        || Detail(row).getDescription().equals(object.getModel().getDescription())){
//                    poJSON.put("result", "error");
//                    poJSON.put("message", "Selected item replacement description is the same in the request.");
//                    poJSON.put("row", row);
//                    return poJSON;
//                }
//                
//                JSONObject loJSON = checkExistingDetail(row,
//                        object.getModel().getStockId(),
//                        object.getModel().getDescription(),
//                        true
//                        );
//                if ("error".equals((String) loJSON.get("result"))) {
//                    if((boolean) loJSON.get("reverse")){
//                        return loJSON;
//                    } else {
//                        row = (int) loJSON.get("row");
//                        Detail(row).isReverse(true);
//                    }
//                }
//                
//                if(Detail(row).getStockId().equals(object.getModel().getStockId()) 
//                        || Detail(row).getDescription().equals(object.getModel().getDescription())){
//                    Detail(row).setReplaceId("");
//                    Detail(row).setReplaceDescription("");
//                } else {
//                    Detail(row).setReplaceId(object.getModel().getStockId());
//                    Detail(row).setReplaceDescription(object.getModel().getDescription());
//                }
//            }
//            
//            System.out.println("Barcode : " + Detail(row).Inventory().getBarCode());
//            System.out.println("Description : " + Detail(row).Inventory().getDescription());
//            
//        } else {
//            poJSON = new JSONObject();
//            poJSON.put("result", "error");
//            poJSON.put("message", "No record loaded.");
//        }
//        if ("error".equals((String) poJSON.get("result"))) {
//            if(!"".equals(value)){
//                poJSON = checkExistingDetail(row,
//                        "",
//                        value,
//                        false
//                        );
//                if ("error".equals((String) poJSON.get("result"))) {
//                    if((boolean) poJSON.get("reverse")){
//                        return poJSON;
//                    } else {
//                        row = (int) poJSON.get("row");
//                        Detail(row).isReverse(true);
//                    }
//                }
//            }
//
//            Detail(row).setReplaceId("");
//            Detail(row).setReplaceDescription(value);
//        }
//        
//        
//        poJSON.put("row", row);
//        return poJSON;
//    }
    
    
    public JSONObject SearchRequestItem(String value, boolean byCode, int row) throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        poJSON.put("row", row);
        
        if(Master().getSourceNo() == null || "".equals(Master().getSourceNo())){
//            poJSON.put("result", "error");
//            poJSON.put("message", "Source No is not set.");
//            return poJSON;

//            if(psSearchCategory == null || "".equals(psSearchCategory)){
//                poJSON.put("result", "error");
//                poJSON.put("message", "Category is not set.");
//                return poJSON;
//            }
                
            if(Master().getBranchCode() == null || "".equals(Master().getBranchCode())){
                poJSON.put("result", "error");
                poJSON.put("message", "Branch is not set.");
                return poJSON;
            }
            
            if(Master().getSupplierId() == null || "".equals(Master().getSupplierId())){
                poJSON.put("result", "error");
                poJSON.put("message", "Supplier is not set.");
                return poJSON;
            }

            Inventory object = new InvControllers(poGRider, logwrapr).Inventory();
            object.setRecordStatus(RecordStatus.ACTIVE);
            String lsSQL = MiscUtil.addCondition(object.getSQ_Browse(), 
                                                " a.sCategCd1 = " + SQLUtil.toSQL(Master().getCategoryCode())
//                                                + " AND i.sDescript LIKE " + SQLUtil.toSQL("%"+psSearchCategory)
                                                );
            lsSQL = lsSQL + " GROUP BY a.sStockIDx ";
            System.out.println("Executing SQL: " + lsSQL);
            poJSON = ShowDialogFX.Browse(poGRider,
                    lsSQL,
                    value,
                    "Barcode»Description»Brand»Model»Variant»UOM",
                    "sBarCodex»sDescript»xBrandNme»xModelNme»xVrntName»xMeasurNm",
                    "a.sBarCodex»a.sDescript»IFNULL(b.sDescript, '')»IFNULL(c.sDescript, '')»IFNULL(f.sDescript, '')»IFNULL(e.sDescript, '')",
                    byCode ? 0 : 1);

            if (poJSON != null) {
                poJSON = object.getModel().openRecord((String) poJSON.get("sStockIDx"));
                if ("success".equals((String) poJSON.get("result"))) {
                    int lnRow = 0;
                    for (int lnCtr = 0; lnCtr <= getDetailCount()- 1; lnCtr++) {
                        if(Detail(lnCtr).isReverse()){
                            lnRow++;
                        }
                        if (lnCtr != row) {
                            if(Detail(lnCtr).getStockId().equals(object.getModel().getStockId())){
                                if(!Detail(lnCtr).isReverse()){
                                    row = lnCtr;
                                    break;
                                } else {
                                    poJSON.put("result", "error");
                                    poJSON.put("message", Detail(lnCtr).Inventory().getDescription() + " already exists at row"+lnRow+".");
                                    poJSON.put("row", row);
                                    return poJSON;
                                }
                            } 
                        }
                    }
                    Detail(row).isReverse(true);
                    Detail(row).setStockId(object.getModel().getStockId());
                    Detail(row).setDescription(object.getModel().getDescription());
                }

                System.out.println("Barcode : " + Detail(row).Inventory().getBarCode());
                System.out.println("Description : " + Detail(row).Inventory().getDescription());

            } else {
                poJSON = new JSONObject();
                poJSON.put("result", "error");
                poJSON.put("message", "No record loaded.");
            }

        } else {
            Model_PO_Quotation_Request_Detail object = new QuotationModels(poGRider).POQuotationRequestDetails();
            String lsSQL = MiscUtil.addCondition( MiscUtil.makeSelect(object), 
                                                    " cReversex = " + SQLUtil.toSQL(POQuotationRequestStatus.Reverse.INCLUDE)
                                                   + " AND sTransNox = " + SQLUtil.toSQL(Master().getSourceNo())); 

            System.out.println("Executing SQL: " + lsSQL);
            poJSON = ShowDialogFX.Browse(poGRider,
                    lsSQL,
                    value,
                    "Transaction No»Description",
                    "sTransNox»sDescript",
                    "sTransNox»sDescript",
                    byCode ? 0 : 1);

            if (poJSON != null) {
                poJSON = object.openRecord((String) poJSON.get("sTransNox"), (String) poJSON.get("nEntryNox"));
                if ("success".equals((String) poJSON.get("result"))) {

                    JSONObject loJSON = checkRequestItem(row,
                            object.getDescription()
                            );
                    if ("error".equals((String) loJSON.get("result"))) {
                        if((boolean) loJSON.get("reverse")){
                            return loJSON;
                        } else {
                            row = (int) loJSON.get("row");
                            Detail(row).isReverse(true);
                        }
                    }

                    Detail(row).setStockId(object.getStockId());
                    Detail(row).setDescription(object.getDescription());
                }
            } else {
                poJSON = new JSONObject();
                poJSON.put("result", "error");
                poJSON.put("message", "No record loaded.");
            }
        }
        
        poJSON.put("row", row);
        return poJSON;
    }
    
    public JSONObject SearchInventory(String value, boolean byCode, int row) throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        poJSON.put("row", row);
        
        if(Master().getSourceNo() == null || "".equals(Master().getSourceNo())){
            poJSON.put("result", "error");
            poJSON.put("message", "Source No is not set.");
            return poJSON;
        }
        
        if((Detail(row).getStockId() == null || "".equals(Detail(row).getStockId()))
            && Detail(row).getDescription() == null || "".equals(Detail(row).getDescription())){
            poJSON.put("result", "error");
            poJSON.put("message", "Request Description is not set.");
            return poJSON;
        }
        
        Inventory object = new InvControllers(poGRider, logwrapr).Inventory();
        object.setRecordStatus(RecordStatus.ACTIVE);
        String lsSQL = MiscUtil.addCondition(object.getSQ_Browse(), 
                                            " a.sCategCd1 = " + SQLUtil.toSQL(Master().getCategoryCode())
                                            + " AND a.sCategCd2 = " + SQLUtil.toSQL(Master().POQuotationRequest().getCategoryLevel2())
                                            );
        lsSQL = lsSQL + " GROUP BY a.sStockIDx ";
        System.out.println("Executing SQL: " + lsSQL);
        poJSON = ShowDialogFX.Browse(poGRider,
                lsSQL,
                value,
                "Barcode»Description»Brand»Model»Variant»UOM",
                "sBarCodex»sDescript»xBrandNme»xModelNme»xVrntName»xMeasurNm",
                "a.sBarCodex»a.sDescript»IFNULL(b.sDescript, '')»IFNULL(c.sDescript, '')»IFNULL(f.sDescript, '')»IFNULL(e.sDescript, '')",
                byCode ? 0 : 1);

        if (poJSON != null) {
            poJSON = object.getModel().openRecord((String) poJSON.get("sStockIDx"));
            if ("success".equals((String) poJSON.get("result"))) {
                
                if(Detail(row).getStockId().equals(object.getModel().getStockId()) 
                        || Detail(row).getDescription().equals(object.getModel().getDescription())){
                    poJSON.put("result", "error");
                    poJSON.put("message", "Selected item replacement description is the same in the request.");
                    poJSON.put("row", row);
                    return poJSON;
                }
                
                JSONObject loJSON = checkExistingReverse(row);
                if ("error".equals((String) loJSON.get("result"))) {
                    row = (int) loJSON.get("row");
                    Detail(row).isReverse(true);
                } else {
                    loJSON = checkExistingDetail(row,
                            object.getModel().getStockId(),
                            object.getModel().getDescription()
                            );
                    if ("error".equals((String) loJSON.get("result"))) {
                        if((boolean) loJSON.get("reverse")){
                            return loJSON;
                        } else {
                            row = (int) loJSON.get("row");
                            Detail(row).isReverse(true);
                        }
                    }
                }
                
                Detail(row).setReplaceId(object.getModel().getStockId());
                Detail(row).setReplaceDescription(object.getModel().getDescription());
            }
            
            System.out.println("Barcode : " + Detail(row).Inventory().getBarCode());
            System.out.println("Description : " + Detail(row).Inventory().getDescription());
            
        } else {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No record loaded.");
        }
        
        poJSON.put("row", row);
        return poJSON;
    }
    
    public JSONObject SearchCompany(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Company object = new ParamControllers(poGRider, logwrapr).Company();
        object.setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);
        if ("success".equals((String) poJSON.get("result"))) {
            Master().setCompanyId(object.getModel().getCompanyId());
        }

        return poJSON;
    }
    
    public JSONObject SearchBranch(String value, boolean byCode, boolean isSearch) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Branch object = new ParamControllers(poGRider, logwrapr).Branch();
        object.setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);
        if ("success".equals((String) poJSON.get("result"))) {
            if(isSearch){
                setSearchBranch(object.getModel().getBranchName());
            } else {
                Master().setBranchCode(object.getModel().getBranchCode());
            }
        }

        return poJSON;
    }
    
    public JSONObject SearchDepartment(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Department object = new ParamControllers(poGRider, logwrapr).Department();
        object.setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
            setSearchDepartment(object.getModel().getDescription());
        }
        return poJSON;
    }

    public JSONObject SearchSupplier(String value, boolean byCode, boolean isSearch) throws ExceptionInInitializerError, SQLException, GuanzonException {
        
        AP_Client_Master object = new ClientControllers(poGRider, logwrapr).APClientMaster();
        object.setRecordStatus(RecordStatus.ACTIVE);
        poJSON = object.searchRecord(value, byCode);
        if ("success".equals((String) poJSON.get("result"))) {
            if(isSearch){
                setSearchSupplier(object.getModel().Client().getCompanyName());
            } else {
                Master().setSupplierId(object.getModel().getClientId());
            }
        }

        return poJSON;
    }
    
    public JSONObject SearchCategory(String value, boolean byCode) throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        
//        CategoryLevel2 object = new ParamControllers(poGRider, logwrapr).CategoryLevel2();
//        object.setRecordStatus(RecordStatus.ACTIVE);
//
//        poJSON = object.searchRecord(value, byCode);
//        if ("success".equals((String) poJSON.get("result"))) {
//            if(isSearch){
//                setSearchCategory(object.getModel().getDescription());
//            } else {
//                System.out.println("Category ID: " + object.getModel().getCategoryId());
//                System.out.println("Description " + object.getModel().getDescription());
//                Master().setCategoryLevel2(object.getModel().getCategoryId());
//            }
//        }
        
        
        CategoryLevel2 object = new ParamControllers(poGRider, logwrapr).CategoryLevel2();
        String lsSQL = MiscUtil.addCondition(object.getSQ_Browse(), "cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE)
                                            + " AND (sIndstCdx = '' OR ISNULL(sIndstCdx))"); //+ SQLUtil.toSQL(Master().getIndustryId()));
        
        System.out.println("Executing SQL: " + lsSQL);
        poJSON = ShowDialogFX.Browse(poGRider,
                lsSQL,
                value,
                "Category ID»Description",
                "sCategrCd»sDescript",
                "sCategrCd»sDescript",
                byCode ? 0 : 1);

        if (poJSON != null) {
            poJSON = object.getModel().openRecord((String) poJSON.get("sCategrCd"));
            if ("success".equals((String) poJSON.get("result"))) {
                setSearchCategory(object.getModel().getDescription());
            }
        } else {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No record loaded.");
        }
        
        return poJSON;
    }
    
    public JSONObject SearchTerm(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Term object = new ParamControllers(poGRider, logwrapr).Term();
        object.setRecordStatus("1");

        poJSON = object.searchRecord(value, byCode);
        if ("success".equals((String) poJSON.get("result"))) {
            Master().setTerm(object.getModel().getTermId());
        }

        return poJSON;
    }
    
    /*Validate*/
    public void ReverseItem(int row){
        int lnExist = 0;
        for (int lnCtr = 0; lnCtr <= getDetailCount()- 1; lnCtr++) {
            if(lnCtr != row){
                if(Detail(row).getDescription().equals(Detail(lnCtr).getDescription())){
                    lnExist++;
                    break; 
                }
            }
        }
        
        if(lnExist >= 1){
            Detail().remove(row);
        } else {
            Detail(row).isReverse(false);
        }
    }
    
    public JSONObject RemovedExcessRequestItem(int row){
        poJSON = new JSONObject();
        boolean lbRepEmpty = false;
        int lnCtr = 0;
        for (lnCtr = 0; lnCtr <= getDetailCount()- 1; lnCtr++) {
            if(lnCtr != row){
                if(Detail(row).getDescription().equals(Detail(lnCtr).getDescription())){
                    if(Detail(lnCtr).getReplaceDescription() == null || "".equals(Detail(lnCtr).getReplaceDescription())){
                        if(Detail(lnCtr).getEditMode() == EditMode.ADDNEW){
                            Detail().remove(lnCtr);
                            break;
                        }
                        lbRepEmpty = true;
                        break;
                    }
                }
            }
        }
        
        if(Detail(row).getEditMode() == EditMode.UPDATE){
            if(lbRepEmpty){
                poJSON.put("result", "error");
                poJSON.put("message", "Replace Description cannot be empty.");
                return poJSON;
            }
        } 
        
        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }
    
    //check if there is not empty replacement if true all request description must have a replacement description value
    private boolean validateReplacement(String description){
        for(int lnCtr = 0; lnCtr <= getDetailCount()-1; lnCtr++){
            if(description.equals(Detail(lnCtr).getDescription())){
                if (Detail(lnCtr).getReplaceDescription() != null && !"".equals(Detail(lnCtr).getReplaceDescription())) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    public boolean RequestMultipleItem(String description){
        int lnExist = 0;
        for (int lnCtr = 0; lnCtr <= getDetailCount()- 1; lnCtr++) {
            if(description.equals(Detail(lnCtr).getDescription())){
                lnExist++;
            }
        }
        
        return lnExist > 1;
    }
    
    public JSONObject checkRequestItem(int row, String description){
        JSONObject loJSON = new JSONObject();
        loJSON.put("row", row);
        if(description == null){
            description = "";
        }
        int lnRow = 0;
        for (int lnCtr = 0; lnCtr <= getDetailCount()- 1; lnCtr++) {
            //if(Detail(lnCtr).isReverse()){
                lnRow++;
            //}
            if (lnCtr != row) {
                //Check Existing Stock and Description
                if(!"".equals(description) ){
                    if(description.equals(Detail(lnCtr).getDescription()) 
                        ){
                        
                        if((Detail(lnCtr).getReplaceId() == null || "".equals(Detail(lnCtr).getReplaceId()))){
                            if(Detail(lnCtr).isReverse()){
                                loJSON.put("result", "error");
                                loJSON.put("message", "Item Description already exists in the transaction detail at row "+lnRow+" without replace description.");
                                loJSON.put("reverse", true);
                                loJSON.put("row", lnCtr);
                                System.out.println("ROW : " + loJSON.put("row", lnCtr));
                                return loJSON;
                            } else {
                                loJSON.put("result", "error");
                                loJSON.put("reverse", false);
                                loJSON.put("row", lnCtr);
                                System.out.println("ROW : " + loJSON.put("row", lnCtr));
                                return loJSON;
                            }
                        } else {
                            if(!Detail(lnCtr).isReverse()){
                                loJSON.put("result", "error");
                                loJSON.put("reverse", false);
                                loJSON.put("row", lnCtr);
                                System.out.println("ROW : " + loJSON.put("row", lnCtr));
                                return loJSON;
                            }
                        }
                    }
                }    
            }
        }
        
        loJSON.put("result", "success");
        loJSON.put("message", "success");
        return loJSON;
    }
    
    public JSONObject checkExistingDetail(int row, String stockId, String Description){
        JSONObject loJSON = new JSONObject();
        loJSON.put("row", row);
        if(stockId == null){
            stockId = "";
        }
        if(Description == null){
            Description = "";
        }
        int lnRow = 0;
        for (int lnCtr = 0; lnCtr <= getDetailCount()- 1; lnCtr++) {
            if(Detail(lnCtr).isReverse()){
                lnRow++;
            }
            if (lnCtr != row) {
                //Check Existing Stock and Description
                if(!"".equals(stockId) && !"".equals(Description) ){
                    if(
                        stockId.equals(Detail(lnCtr).getStockId())
                        || stockId.equals(Detail(lnCtr).getReplaceId())
                        || Description.equals(Detail(lnCtr).getDescription())
                        || Description.equals(Detail(lnCtr).getReplaceDescription())
                        ){
                    
                        if(Detail(lnCtr).isReverse()){
                            loJSON.put("result", "error");
                            loJSON.put("message", "Item Description already exists in the transaction detail at row "+lnRow+".");
                            loJSON.put("reverse", true);
                            loJSON.put("row", lnCtr);
                            return loJSON;
                        } else {
                            loJSON.put("result", "error");
                            loJSON.put("reverse", false);
                            loJSON.put("row", lnCtr);
                            return loJSON;
                        }
                    
                    }
                }    
            }
        }
        
        loJSON.put("result", "success");
        loJSON.put("message", "success");
        return loJSON;
    }
    
    public JSONObject checkExistingReverse(int row){
        JSONObject loJSON = new JSONObject();
        loJSON.put("row", row);
        int lnRow = 0;
        for (int lnCtr = 0; lnCtr <= getDetailCount()- 1; lnCtr++) {
            if(Detail(lnCtr).isReverse()){
                lnRow++;
            }
            if (lnCtr != row) {
                if(Detail(lnCtr).getDescription().equals(Detail(row).getDescription())
                    && !Detail(lnCtr).isReverse()){
                    if(Detail(lnCtr).isReverse()){
                        loJSON.put("result", "error");
                        loJSON.put("reverse", false);
                        loJSON.put("row", lnCtr);
                        return loJSON;
                    }

                }
            }
        }
        
        loJSON.put("result", "success");
        loJSON.put("message", "success");
        return loJSON;
    }
    
    
//    public JSONObject checkExistingDetail(int row, String stockId, String description, boolean isSearch){
//        JSONObject loJSON = new JSONObject();
//        loJSON.put("row", row);
//        if(stockId == null){
//            stockId = "";
//        }
//        if(description == null){
//            description = "";
//        }
//        int lnRow = 0;
//        for (int lnCtr = 0; lnCtr <= getDetailCount()- 1; lnCtr++) {
//            if(Detail(lnCtr).isReverse()){
//                lnRow++;
//            }
//            if (lnCtr != row) {
//                //Check Existing Stock ID and Description
//                if(!"".equals(stockId) || !"".equals(description)){
//                    
//                    if(((stockId.equals(Detail(lnCtr).getStockId())  && isSearch )
//                            || (description.equals(Detail(lnCtr).getDescription())))
//                            || ((stockId.equals(Detail(lnCtr).getReplaceId())  && isSearch )
//                            || (description.equals(Detail(lnCtr).getReplaceDescription())))){
//                        if(Detail(lnCtr).isReverse()){
//                            loJSON.put("result", "error");
//                            loJSON.put("message", "Item Description already exists in the transaction detail at row "+lnRow+".");
//                            loJSON.put("reverse", true);
//                            loJSON.put("row", lnCtr);
//                            return loJSON;
//                        } else {
//                            loJSON.put("result", "error");
//                            loJSON.put("reverse", false);
//                            loJSON.put("row", lnCtr);
//                            return loJSON;
//                        }
//                    }
//                    
//                }    
//            }
//        }
//        
//        loJSON.put("result", "success");
//        loJSON.put("message", "success");
//        return loJSON;
//    }
    
    public JSONObject computeFields()
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();

        //Compute Transaction Total
        Double ldblTotal = 0.0000;
        Double ldblDiscount = Master().getAdditionalDiscountAmount();
        Double ldblDiscountRate = Master().getDiscountRate();
        Double ldblDetailDiscountRate = 0.00;
        Double ldblDetailTotal = 0.0000;
        
        for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
            if(Detail(lnCtr).isReverse()){
                if(Detail(lnCtr).getDiscountRate() > 0){
                    ldblDetailDiscountRate = Detail(lnCtr).getUnitPrice() * (Detail(lnCtr).getDiscountRate() / 100);
                }
                //Cost = (Unit Price - (Discount Rate + Additional Discount) * Quantity)
                ldblDetailTotal = (Detail(lnCtr).getUnitPrice() - (ldblDetailDiscountRate + Detail(lnCtr).getDiscountAmount())) *  Detail(lnCtr).getQuantity();
                ldblTotal = ldblTotal + ldblDetailTotal;

                ldblDetailTotal = 0.0000;
                ldblDetailDiscountRate = 0.00;
            }
        }
        
        poJSON = Master().setGrossAmount(ldblTotal);
        if(ldblDiscountRate > 0){
            ldblDiscountRate = ldblTotal * (ldblDiscountRate / 100);
        }
        
        /*Compute VAT Amount*/
        double ldblVatSales = 0.0000;
        double ldblVatAmount = 0.0000;
        double ldblTransactionTotal = 0.0000;
        double ldblVatExempt = 0.00;
            
        //VAT Sales : (Vatable Total + Freight Amount) - Discount Amount
        ldblVatSales = (Master().getGrossAmount() + Master().getFreightAmount()) - (ldblDiscount + ldblDiscountRate);
        //VAT Amount : VAT Sales - (VAT Sales / 1.12)
        ldblVatAmount = ldblVatSales - ( ldblVatSales / 1.12);

        if(Master().isVatable()){ //Add VAT
            //Net VAT Sales : VAT Sales - VAT Amount
            ldblTransactionTotal = ldblVatSales + ldblVatAmount;
        } else {
            ldblTransactionTotal = ldblVatSales;
        } 
        //else {
//            //VAT Amount : VAT Sales - (VAT Sales / 1.12)
//            ldblVatAmount = ldblVatSales - ( ldblVatSales / 1.12);
//            //Net : VAT Sales - VAT Amount
//            ldblTransactionTotal = ldblVatSales + ldblVatAmount;
        //}

//        System.out.println("Vat Sales " + ldblTransactionTotal);
//        System.out.println("Vat Amount " + ldblVatAmount);
//        System.out.println("Vat Exempt " + ldblVatExempt);

        poJSON = Master().setTransactionTotal(ldblTransactionTotal);
        poJSON = Master().setVatAmount(ldblVatAmount);
        if(Master().getVatRate() == 0.00){
            if(getEditMode() == EditMode.UNKNOWN || Master().getEditMode() == EditMode.UNKNOWN){
                poJSON = Master().setVatRate(0.00); //Set default value
            } else {
                poJSON = Master().setVatRate(12.00); //Set default value
            }
        }
        return poJSON;
    }
    
    public Double getCost(int row){
//        Double ldblDetailDiscountRate = 0.00;
//        if(Detail(row).getDiscountRate() > 0){
//            ldblDetailDiscountRate = Detail(row).getUnitPrice() * (Detail(row).getDiscountRate() / 100);
//        }
        //Cost = (Unit Price - (Discount Rate + Additional Discount) * Quantity)
        return (Detail(row).getUnitPrice() - getDiscount(row)) *  Detail(row).getQuantity();
    }
    
    public Double getDiscount(int row){
        Double ldblDetailDiscountRate = 0.00;
        if(Detail(row).getDiscountRate() > 0){
            ldblDetailDiscountRate = Detail(row).getUnitPrice() * (Detail(row).getDiscountRate() / 100);
        }
        //Cost = (Unit Price - (Discount Rate + Additional Discount) * Quantity)
        return ldblDetailDiscountRate + Detail(row).getDiscountAmount();
    }
    
    public JSONObject computeCost(int row, double discountRate, double discount){
        poJSON = new JSONObject();
        Double ldblDetailDiscountRate = 0.00;
        if(discountRate > 0){
            ldblDetailDiscountRate = Detail(row).getUnitPrice() * (discountRate / 100);
        }
        
        if(discountRate > 0.00 && Detail(row).getUnitPrice() <= 0.0000){
            poJSON.put("result", "error");
            poJSON.put("message", "Invalid discount rate.");
            return poJSON;
        }
        
        Double ldblDetailTotalDiscount = ldblDetailDiscountRate + discount;
        if(ldblDetailTotalDiscount > Detail(row).getUnitPrice()){
            poJSON.put("result", "error");
            poJSON.put("message", "Invalid discount.");
            return poJSON;
        }
        
        if((Detail(row).getUnitPrice() - ldblDetailTotalDiscount) *  Detail(row).getQuantity() < 0.0000){
            poJSON.put("result", "error");
            poJSON.put("message", "Invalid computed cost amount.");
            return poJSON;
        }
        
        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }
    
    public JSONObject computeDiscount(double discount) {
        poJSON = new JSONObject();
        Double ldblTotal = 0.00;
        Double ldblDiscRate = 0.00;

        for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
            ldblTotal += (Detail(lnCtr).getUnitPrice()  * Detail(lnCtr).getQuantity());
        }
        
        if (discount < 0 || discount > ldblTotal) {
            Master().setAdditionalDiscountAmount(0.00);
            computeDiscount(0.00);
            poJSON.put("result", "error");
            poJSON.put("message", "Discount amount cannot be negative or exceed the transaction total.");
            return poJSON;
        } else {
//            ldblDiscRate = (discount / ldblTotal) * 100;
//            ldblDiscRate = (discount / ldblTotal);
            //nettotal = total - discount - rate
//            Master().setDiscountRate(ldblDiscRate);

            ldblTotal = ldblTotal - (discount + ((Master().getDiscountRate() / 100.00) * ldblTotal));
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

    public JSONObject computeDiscountRate(double discountRate) {
        poJSON = new JSONObject();
        Double ldblTotal = 0.00;
        Double ldblDiscount = 0.00;

        for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
            ldblTotal += (Detail(lnCtr).getUnitPrice() * Detail(lnCtr).getQuantity());
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

            ldblTotal = ldblTotal - (Master().getAdditionalDiscountAmount() + ((discountRate / 100.00) * ldblTotal));
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
    
    /*Load*/
    public JSONObject loadPOQuotationList(String fsBranch, String fsDepartment ,String fsSupplier, String fsCateogry, String fsSourceNo, boolean isApproval) {
        try {
            String lsBranch = fsBranch != null && !"".equals(fsBranch) 
                                                        ? " AND c.sBranchNm LIKE " + SQLUtil.toSQL("%"+fsBranch)
                                                        : "";

            String lsDepartment = fsDepartment != null && !"".equals(fsDepartment) 
                                                        ? " AND h.sDeptName LIKE " + SQLUtil.toSQL("%"+fsDepartment)
                                                        : "";

            String lsCategory = fsCateogry != null && !"".equals(fsCateogry) 
                                                        ? " AND f.sDescript LIKE " + SQLUtil.toSQL("%"+fsCateogry)
                                                        : "";

            String lsSupplier = fsSupplier != null && !"".equals(fsSupplier) 
                                                        ? " AND i.sCompnyNm LIKE " + SQLUtil.toSQL("%"+fsSupplier)
                                                        : "";

            String lsSourceNo = fsSourceNo != null && !"".equals(fsSourceNo) 
                                                        ? " AND a.sSourceNo LIKE " + SQLUtil.toSQL("%"+fsSourceNo)
                                                        : "";
            
            //LOAD only PO Quotation that is not yet linked to po_detail whether po is cancelled as long as it was already linked to PO it should not be include in retrieval
            String lsApproval = "";
            if(isApproval){
                lsApproval =  " AND a.sTransNox NOT IN (SELECT PO_Detail.sSourceNo from PO_Detail WHERE PO_Detail.sSourceNo = a.sTransNox AND PO_Detail.sSourceCd = "+SQLUtil.toSQL(getSourceCode())+" ) ";
            }

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
                    + " AND a.sCategrCd = " + SQLUtil.toSQL(psCategorCd)
                    + lsBranch
                    + lsDepartment
                    + lsCategory
                    + lsSupplier
                    + lsSourceNo )
                    + lsApproval ;

            if (lsTransStat != null && !"".equals(lsTransStat)) {
                lsSQL = lsSQL + lsTransStat;
            }

            lsSQL = lsSQL + " ORDER BY a.dTransact DESC ";

            System.out.println("Executing SQL: " + lsSQL);
            ResultSet loRS = poGRider.executeQuery(lsSQL);
            poJSON = new JSONObject();

            int lnctr = 0;

            if (MiscUtil.RecordCount(loRS) >= 0) {
                paMasterList = new ArrayList<>();
                while (loRS.next()) {
                    // Print the result set
                    System.out.println("sTransNox: " + loRS.getString("sTransNox"));
                    System.out.println("dTransact: " + loRS.getDate("dTransact"));
                    System.out.println("sCompnyNm: " + loRS.getString("SpplierNm"));
                    System.out.println("------------------------------------------------------------------------------");

                    paMasterList.add(POQuotationMaster());
                    paMasterList.get(paMasterList.size() - 1).openRecord(loRS.getString("sTransNox"));
                    lnctr++;
                }

                System.out.println("Records found: " + lnctr);
                poJSON.put("result", "success");
                poJSON.put("message", "Record loaded successfully.");
            } else {
                paMasterList = new ArrayList<>();
                paMasterList.add(POQuotationMaster());
                poJSON.put("result", "error");
                poJSON.put("continue", true);
                poJSON.put("message", "No record found.");
            }
            MiscUtil.close(loRS);
        } catch (SQLException e) {
            poJSON.put("result", "error");
            poJSON.put("message", e.getMessage());
        } catch (GuanzonException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
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
            
            //Download Attachments
            poJSON = WebFile.DownloadFile(WebFile.getAccessToken(System.getProperty("sys.default.access.token"))
                    , "0032" //Constant
                    , "" //Empty
                    , paAttachments.get(getTransactionAttachmentCount() - 1).getModel().getFileName()
                    , SOURCE_CODE
                    , paAttachments.get(getTransactionAttachmentCount() - 1).getModel().getSourceNo()
                    , "");
            if ("success".equals((String) poJSON.get("result"))) {
                
                poJSON = (JSONObject) poJSON.get("payload");
                if(WebFile.Base64ToFile((String) poJSON.get("data")
                        , (String) poJSON.get("hash")
                        , System.getProperty("sys.default.path.temp.attachments") + "/"
                        , (String) poJSON.get("filename"))){
                    System.out.println("poJSON success: " +  poJSON.toJSONString());
                    System.out.println("File downloaded succesfully.");
                } else {
                    System.out.println("poJSON error: " + poJSON.toJSONString());
                    poJSON.put("result", "error");
                    poJSON.put("message", "Unable to download file.");
                }
                
            } else {
                System.out.println("poJSON error WebFile.DownloadFile: " + poJSON.toJSONString());
            }
        }
        return poJSON;
    }
    
    public JSONObject loadPOQuotationRequestSupplierList(String company, String branch, String department, String supplier, String category2) {
        paPORequestSupplier = new ArrayList<>();
        try {
            if (company == null) {
                company = "";
            }
            if (branch == null) {
                branch = "";
            }
            if (department == null) {
                department = "";
            }
            if (supplier == null) {
                supplier = "";
            }
            if (category2 == null) {
                category2 = "";
            }
            String lsSQL = MiscUtil.addCondition(getSupplierSQL(), 
                      " b.sIndstCdx = " + SQLUtil.toSQL(psIndustryId)
                    + " AND b.sCategrCd = " + SQLUtil.toSQL(psCategorCd)
                    + " AND i.sCompnyNm LIKE " + SQLUtil.toSQL("%" + company)
                    + " AND d.sBranchNm LIKE " + SQLUtil.toSQL("%" + branch)
                    + " AND e.sDeptName LIKE " + SQLUtil.toSQL("%" + department) 
                    + " AND h.sCompnyNm LIKE " + SQLUtil.toSQL("%" + supplier) 
                    + " AND g.sDescript LIKE " + SQLUtil.toSQL("%" + category2) 
                    + " AND b.cTranStat = " + SQLUtil.toSQL(POQuotationRequestStatus.APPROVED)
                    + " AND a.cReversex = "+SQLUtil.toSQL(POQuotationRequestStatus.Reverse.INCLUDE)
                    + " AND a.sTransNox NOT IN (SELECT q.sSourceNo FROM PO_Quotation_Master q "
                            + " WHERE q.sSourceNo = a.sTransNox AND q.sCompnyID = a.sCompnyID AND q.sSupplier = a.sSupplier "
                            + " AND (q.cTranStat != "+SQLUtil.toSQL(POQuotationStatus.CANCELLED) +" AND q.cTranStat != "+SQLUtil.toSQL(POQuotationStatus.VOID)+")) "
            );

            lsSQL = lsSQL + " ORDER BY b.dTransact DESC ";

            System.out.println("Executing SQL: " + lsSQL);
            ResultSet loRS = poGRider.executeQuery(lsSQL);
            poJSON = new JSONObject();

            int lnctr = 0;

            if (MiscUtil.RecordCount(loRS) >= 0) {
                while (loRS.next()) {
                    // Print the result set
                    System.out.println("sTransNox: " + loRS.getString("sTransNox"));
                    System.out.println("dTransact: " + loRS.getDate("dTransact"));
                    System.out.println("sCompnyNm: " + loRS.getString("SpplierNm"));
                    System.out.println("------------------------------------------------------------------------------");

                    paPORequestSupplier.add(POQuotationRequestSupplier());
                    paPORequestSupplier.get(paPORequestSupplier.size() - 1).openRecord(loRS.getString("sTransNox"), loRS.getString("sSupplier"), loRS.getString("sCompnyID"));
                    lnctr++;
                }

                System.out.println("Records found: " + lnctr);
                poJSON.put("result", "success");
                poJSON.put("message", "Record loaded successfully.");
            } else {
                paPORequestSupplier = new ArrayList<>();
                paPORequestSupplier.add(POQuotationRequestSupplier());
                poJSON.put("result", "error");
                poJSON.put("continue", true);
                poJSON.put("message", "No record found.");
            }
            MiscUtil.close(loRS);
        } catch (SQLException e) {
            poJSON.put("result", "error");
            poJSON.put("message", e.getMessage());
        } catch (GuanzonException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, getClass().getName(), ex);
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
        }
        return poJSON;
    }
    
    public JSONObject resetTransaction() throws CloneNotSupportedException, SQLException, GuanzonException{
        poJSON = new JSONObject();
        removeDetails();
        resetOthers();
        ReloadDetail();
        
        Master().setSupplierId("");
        Master().setAddressId("");
        Master().setContactId("");
        Master().setTerm("");
        Master().setSourceNo("");
        Master().setSourceCode("");
        Master().setCompanyId("");
        Master().setBranchCode("");
        
        computeFields();
    
        return poJSON;
    }
    
    
    //Validate
    private JSONObject checkExistingQuotation(int row, boolean isPopulate) throws SQLException, GuanzonException{
        poJSON = new JSONObject();
        String lsSourceNo, lsSupplierId, lsCompanyId;
        if(isPopulate){
            lsSourceNo = POQuotationRequestSupplierList(row).getTransactionNo();
            lsSupplierId = POQuotationRequestSupplierList(row).getSupplierId();
            lsCompanyId = POQuotationRequestSupplierList(row).getCompanyId();
        } else {
            lsSourceNo = Master().getSourceNo();
            lsSupplierId = Master().getSupplierId();
            lsCompanyId = Master().getCompanyId();
        }
        
        initSQL();
        String lsSQL = MiscUtil.addCondition(SQL_BROWSE, 
                  " a.sIndstCdx = " + SQLUtil.toSQL(psIndustryId)
                + " AND a.sCategrCd = " + SQLUtil.toSQL(psCategorCd)
                + " AND a.sSourceNo = " + SQLUtil.toSQL(lsSourceNo) 
                + " AND a.sSupplier = " + SQLUtil.toSQL(lsSupplierId) 
                + " AND a.sCompnyID = " + SQLUtil.toSQL(lsCompanyId) 
                + " AND a.sTransNox != " + SQLUtil.toSQL(Master().getTransactionNo())
                + " AND NOT ( a.cTranStat = " + SQLUtil.toSQL(POQuotationStatus.CANCELLED)
                + " OR a.cTranStat = " + SQLUtil.toSQL(POQuotationStatus.VOID)
                + " ) "
                );
        
        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        poJSON = new JSONObject();

        if (MiscUtil.RecordCount(loRS) >= 0) {
            if (loRS.next()) {
                poJSON.put("result", "error");
                poJSON.put("message", "The selected quotation request already has an existing quotation <"+loRS.getString("sTransNox")+">.");
            }
        } else {
            poJSON.put("result", "success");
            poJSON.put("continue", true);
            poJSON.put("message", "No record found.");
        }
        MiscUtil.close(loRS);
        
        return poJSON;
    }
    
    public JSONObject populatePOQuotation(int row) {
        poJSON = new JSONObject();
        try {
            
            if((Master().getSourceNo() == null || "".equals(Master().getSourceNo())) 
                && (Detail(0).getStockId() != null && !"".equals(Detail(0).getStockId()) )){
                poJSON.put("result", "error");
                poJSON.put("message", "PO Quotation detail cannot be mixed with non quotation request.");
                return poJSON;
            }
            
//            if(!POQuotationRequestSupplierList(row).getTransactionNo().equals(Master().getSourceNo())){
//                resetTransaction();
//            }
            poJSON = checkExistingQuotation(row, true);
            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
            
            POQuotationRequest object = new QuotationControllers(poGRider, logwrapr).POQuotationRequest();
            object.InitTransaction();
            poJSON = object.OpenTransaction(POQuotationRequestSupplierList(row).getTransactionNo());
            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
            
            Master().setSupplierId(POQuotationRequestSupplierList(row).getSupplierId());
            Master().setAddressId(POQuotationRequestSupplierList(row).getAddressId());
            Master().setContactId(POQuotationRequestSupplierList(row).getContactId());
            Master().setTerm(POQuotationRequestSupplierList(row).getTerm());
            Master().setSourceNo(POQuotationRequestSupplierList(row).getTransactionNo());
            Master().setSourceCode(object.getSourceCode());
            Master().setCompanyId(POQuotationRequestSupplierList(row).getCompanyId());
            Master().setBranchCode(object.Master().getBranchCode());
//            Master().setValidityDate(object.Master().getExpectedPurchaseDate());
            
            for(int lnCtr = 0; lnCtr <= object.getDetailCount()-1; lnCtr++){
                if(object.Detail(lnCtr).isReverse()){
                    Detail(getDetailCount()-1).setStockId(object.Detail(lnCtr).getStockId());
                    Detail(getDetailCount()-1).setDescription(object.Detail(lnCtr).getDescription());
//                    Detail(getDetailCount()-1).setQuantity(object.Detail(lnCtr).getQuantity());   //User Input
//                    Detail(getDetailCount()-1).setUnitPrice(object.Detail(lnCtr).getUnitPrice()); //User Input
                    AddDetail();
                }
            }
            
        } catch (SQLException e) {
            poJSON.put("result", "error");
            poJSON.put("message", e.getMessage());
        } catch (GuanzonException | CloneNotSupportedException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, getClass().getName(), ex);
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
        }
        return poJSON;
    }
    
    public void ReloadDetail() throws CloneNotSupportedException{
        int lnCtr = getDetailCount() - 1;
        while (lnCtr >= 0) {
            if (((Detail(lnCtr).getStockId() == null || "".equals(Detail(lnCtr).getStockId()))
                    && (Detail(lnCtr).getDescription()== null || "".equals(Detail(lnCtr).getDescription())))
                && ((Detail(lnCtr).getReplaceId()== null || "".equals(Detail(lnCtr).getReplaceId()))
                    && (Detail(lnCtr).getReplaceDescription()== null || "".equals(Detail(lnCtr).getReplaceDescription())))){
                
                if(Detail(lnCtr).getEditMode() == EditMode.ADDNEW){
                    deleteDetail(lnCtr); 
                }
            }
            lnCtr--;
        }

        if ((getDetailCount() - 1) >= 0) {
            
            if ( ( (Detail(getDetailCount() - 1).getStockId() != null && !"".equals(Detail(getDetailCount() - 1).getStockId()))
                || (Detail(getDetailCount() - 1).getDescription()!= null && !"".equals(Detail(getDetailCount() - 1).getDescription())) )
                    
            || ((Detail(getDetailCount() - 1).getReplaceId()!= null && !"".equals(Detail(getDetailCount() - 1).getReplaceId()))
                || (Detail(getDetailCount() - 1).getReplaceDescription()!= null && !"".equals(Detail(getDetailCount() - 1).getReplaceDescription())))){
                AddDetail();
            }
        }

        if ((getDetailCount() - 1) < 0) {
            AddDetail();
        }
    }
    
    private Model_PO_Quotation_Master POQuotationMaster() {
        return new QuotationModels(poGRider).POQuotationMaster();
    }
    
    private Model_PO_Quotation_Request_Supplier POQuotationRequestSupplier() {
        return new QuotationModels(poGRider).POQuotationRequestSupplier();
    }
    
    private TransactionAttachment TransactionAttachment()
            throws SQLException,
            GuanzonException {
        return new SysTableContollers(poGRider, null).TransactionAttachment();
    }

    @Override
    public Model_PO_Quotation_Master Master() {
        return (Model_PO_Quotation_Master) poMaster;
    }

    public Model_PO_Quotation_Detail getDetail() {
        return (Model_PO_Quotation_Detail) poDetail;
    }
    
    public List<Model_PO_Quotation_Request_Supplier> POQuotationRequestSupplierList() {
        return paPORequestSupplier;
    }
    
    private List<TransactionAttachment> TransactionAttachmentList() {
        return paAttachments;
    }

    public List<Model_PO_Quotation_Master> POQuotationList() {
        return paMasterList;
    }
    
    public Model_PO_Quotation_Master POQuotationList(int row) {
        return (Model_PO_Quotation_Master) paMasterList.get(row);
    }

    @Override
    public Model_PO_Quotation_Detail Detail(int row) {
        return (Model_PO_Quotation_Detail) paDetail.get(row);
    }
    
    public Model_PO_Quotation_Request_Supplier POQuotationRequestSupplierList(int row) {
        return (Model_PO_Quotation_Request_Supplier) paPORequestSupplier.get(row);
    }
    
    public TransactionAttachment TransactionAttachmentList(int row) {
        return (TransactionAttachment) paAttachments.get(row);
    }

    private Model_PO_Quotation_Detail DetailRemoved(int row) {
        return (Model_PO_Quotation_Detail) paDetailRemoved.get(row);
    }
    
    @Override
    public int getDetailCount() {
        if (paDetail == null) {
            paDetail = new ArrayList<>();
        }

        return paDetail.size();
    }
    
    public int getPOQuotationCount() {
        if (paMasterList == null) {
            paMasterList = new ArrayList<>();
        }

        return paMasterList.size();
    }
    
    public int getPOQuotationRequestSupplierCount() {
        if (paPORequestSupplier == null) {
            paPORequestSupplier = new ArrayList<>();
        }

        return paPORequestSupplier.size();
    }
    
    public int getTransactionAttachmentCount() {
        if (paAttachments == null) {
            paAttachments = new ArrayList<>();
        }

        return paAttachments.size();
    }
    
    private int getDetailRemovedCount() {
        if (paDetailRemoved == null) {
            paDetailRemoved = new ArrayList<>();
        }

        return paDetailRemoved.size();
    }
    
    public JSONObject AddDetail()
            throws CloneNotSupportedException {
        poJSON = new JSONObject();

        if (getDetailCount() > 0) {
            if ((Detail(getDetailCount() - 1).getStockId() != null || Detail(getDetailCount() - 1).getDescription() != null)
                    && (Detail(getDetailCount() - 1).getReplaceId() != null || Detail(getDetailCount() - 1).getReplaceDescription() != null)){
                if ((Detail(getDetailCount() - 1).getStockId().isEmpty() && Detail(getDetailCount() - 1).getDescription().isEmpty())
                    && (Detail(getDetailCount() - 1).getReplaceId().isEmpty() && Detail(getDetailCount() - 1).getReplaceDescription().isEmpty())){
                    poJSON.put("result", "error");
                    poJSON.put("message", "Last row has empty item.");
                    return poJSON;
                }
            }
        }

        return addDetail();
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
    
    public JSONObject removeAttachment(int fnRow) throws GuanzonException, SQLException{
        poJSON = new JSONObject();
        if(getTransactionAttachmentCount() <= 0){
            poJSON.put("result", "error");
            poJSON.put("message", "No transaction attachment to be removed.");
            return poJSON;
        }
        
        if(paAttachments.get(fnRow).getEditMode() == EditMode.ADDNEW){
            paAttachments.remove(fnRow);
            System.out.println("Attachment :"+ fnRow+" Removed");
        } else {
            paAttachments.get(fnRow).getModel().setRecordStatus(RecordStatus.INACTIVE);
            System.out.println("Attachment :"+ fnRow+" Deactivate");
        }
        
        poJSON.put("result", "success");
        return poJSON;
    }
    
    public int addAttachment(String fFileName) throws SQLException, GuanzonException{
        for(int lnCtr = 0;lnCtr <= getTransactionAttachmentCount() - 1;lnCtr++){
            if(fFileName.equals(paAttachments.get(lnCtr).getModel().getFileName())
                && RecordStatus.INACTIVE.equals(paAttachments.get(lnCtr).getModel().getRecordStatus())){
                paAttachments.get(lnCtr).getModel().setRecordStatus(RecordStatus.ACTIVE);
                System.out.println("Attachment :"+ lnCtr+" Activate");
                return lnCtr;
            }
        }
        
        addAttachment();
        paAttachments.get(getTransactionAttachmentCount() - 1).getModel().setFileName(fFileName);
        paAttachments.get(getTransactionAttachmentCount() - 1).getModel().setSourceNo(Master().getTransactionNo());
        paAttachments.get(getTransactionAttachmentCount() - 1).getModel().setRecordStatus(RecordStatus.ACTIVE);
        return getTransactionAttachmentCount() - 1;
    }
    
    public void copyFile(String fsPath){
        Path source = Paths.get(fsPath);
        Path targetDir = Paths.get(System.getProperty("sys.default.path.temp") + "/Attachments");

        try {
            // Ensure target directory exists
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            // Copy file into the target directory
            Files.copy(source, targetDir.resolve(source.getFileName()),
                       StandardCopyOption.REPLACE_EXISTING);

            System.out.println("File copied successfully!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public JSONObject checkExistingFileName(String fsFileName) throws SQLException, GuanzonException{
        poJSON = new JSONObject();
        
        String lsSQL = MiscUtil.addCondition(MiscUtil.makeSelect(TransactionAttachment().getModel()), 
                                                                    " sFileName = " + SQLUtil.toSQL(fsFileName)
                                                                    );
        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        try {
            if (MiscUtil.RecordCount(loRS) > 0) {
                if(loRS.next()){
                    if(loRS.getString("sFileName") != null && !"".equals(loRS.getString("sFileName"))){
                        poJSON.put("result", "error");
                        poJSON.put("message", "File name already exist in database.\nTry changing the file name to upload.");
                    }
                }
            }
            MiscUtil.close(loRS);
        } catch (SQLException e) {
            System.out.println("No record loaded.");
        }
        return poJSON;
    }
    
    public JSONObject removeDetails() {
        poJSON = new JSONObject();
        Iterator<Model> detail = Detail().iterator();
        while (detail.hasNext()) {
            Model item = detail.next();
            if (item.getEditMode() == EditMode.UPDATE) {
                paDetailRemoved.add(item);
            }
            
            detail.remove();
        }

        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }

    private void removeDetail(Model_PO_Quotation_Detail item) {
        if (paDetailRemoved == null) {
            paDetailRemoved = new ArrayList<>();
        }
        
        paDetailRemoved.add(item);
    }

    public void resetMaster() {
        poMaster = new QuotationModels(poGRider).POQuotationMaster();
    }
    
    public void resetOthers() {
        paAttachments = new ArrayList<>();
        paDetailRemoved = new ArrayList<>();
        setSearchBranch("");
        setSearchDepartment("");
        setSearchSupplier("");
        setSearchCategory("");
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
    
    public void setSearchBranch(String searchBranch) {
        psSearchBranch = searchBranch;
    }
    
    public String getSearchBranch() {
        return psSearchBranch;
    }

    public void setSearchDepartment(String searchDepartment) {
        psSearchDepartment = searchDepartment;
    }
    
    public String getSearchDepartment() {
        return psSearchDepartment;
    }
    
    public void setSearchSupplier(String searchSupplier) {
        psSearchSupplier = searchSupplier;
    }
    
    public String getSearchSupplier() {
        return psSearchSupplier;
    }

    public void setSearchCategory(String searchCategory) {
        psSearchCategory = searchCategory;
    }
    
    public String getSearchCategory() {
        return psSearchCategory;
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

    @Override
    public String getSourceCode() {
        return SOURCE_CODE;
    }
    
    @Override
    public JSONObject willSave()
            throws SQLException,
            GuanzonException,
            CloneNotSupportedException {
        /*Put system validations and other assignments here*/
        poJSON = new JSONObject();
        
        //Only trigger this validation when source no is not null or not empty.
        if(Master().getSourceNo() != null && !"".equals(Master().getSourceNo())){
            poJSON = checkExistingQuotation(0, false);
            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
        }
        
        //Prevent null in reference no
        if(Master().getReferenceNo() == null){
            Master().setReferenceNo("");
        }
        
        //if current status is Return check changes
        if(POQuotationRequestStatus.RETURNED.equals(Master().getTransactionStatus())){
            boolean lbUpdated = false;
            POQuotation loRecord = new QuotationControllers(poGRider, null).POQuotation();
            loRecord.InitTransaction();
            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
            loRecord.OpenTransaction(Master().getTransactionNo());
            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
            
            lbUpdated = loRecord.getDetailCount() == (getDetailCount() - 1);
            if (lbUpdated) {
                lbUpdated = loRecord.Master().getReferenceNo().equals(Master().getReferenceNo());
            }
            if (lbUpdated) {
                lbUpdated = Objects.equals(loRecord.Master().getReferenceDate(), Master().getReferenceDate());
            }
            if (lbUpdated) {
                lbUpdated = loRecord.Master().getSupplierId().equals(Master().getSupplierId());
            }
            if (lbUpdated) {
                lbUpdated = loRecord.Master().getTerm().equals(Master().getTerm());
            }
            if (lbUpdated) {
                lbUpdated = Objects.equals(loRecord.Master().getGrossAmount(), Master().getGrossAmount());
            }
            if (lbUpdated) {
                lbUpdated = Objects.equals(loRecord.Master().getFreightAmount(), Master().getFreightAmount());
            }
            if (lbUpdated) {
                lbUpdated = Objects.equals(loRecord.Master().getDiscountRate(), Master().getDiscountRate());
            }
            if (lbUpdated) {
                lbUpdated = Objects.equals(loRecord.Master().getAdditionalDiscountAmount(), Master().getAdditionalDiscountAmount());
            }
            if (lbUpdated) {
                lbUpdated = loRecord.Master().isVatable() == Master().isVatable();
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
                    if (lbUpdated) {
                        lbUpdated = loRecord.Detail(lnCtr).getDescription().equals(Detail(lnCtr).getDescription());
                    }
                    if (lbUpdated) {
                        lbUpdated = loRecord.Detail(lnCtr).getReplaceId().equals(Detail(lnCtr).getReplaceId());
                    }
                    if (lbUpdated) {
                        lbUpdated = loRecord.Detail(lnCtr).getReplaceDescription().equals(Detail(lnCtr).getReplaceDescription());
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
        
        }
        
        switch(Master().getTransactionStatus()){
            case POQuotationStatus.CONFIRMED:
            case POQuotationStatus.RETURNED:
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
            break;
        }

        if (paDetailRemoved == null) {
            paDetailRemoved = new ArrayList<>();
        }
        
        if(Master().getEditMode() == EditMode.ADDNEW){
            System.out.println("Will Save : " + Master().getNextCode());
            Master().setTransactionNo(Master().getNextCode());
            Master().setPrepared(poGRider.Encrypt(poGRider.getUserID()));
        }
        
        Master().setModifyingId(poGRider.Encrypt(poGRider.getUserID()));
        Master().setModifiedDate(poGRider.getServerDate());
        Master().setPreparedDate(poGRider.getServerDate()); //Re-updated prepared date when user edited the transaction conflict in null when updating transaction
        
        //Check detail
        boolean lbWillDelete = true;
        for(int lnCtr = 0; lnCtr <= getDetailCount()-1; lnCtr++){
            System.out.println("Detail Discount Rate: " + Detail(lnCtr).getDiscountRate());
            if ((Detail(lnCtr).getQuantity() > 0.00) && Detail(lnCtr).isReverse() ) {
                lbWillDelete = false;
            }
        }
        
        if(lbWillDelete){
            poJSON.put("result", "error");
            poJSON.put("message", "No transaction quantity to be save.");
            return poJSON;
        }
        
        String lsQuantity = "0.00";
        int lnCount = 0;
        boolean lbReqItem, lbReqDescription, lbReqStockID, lbReplaceID, lbReplaceDesc; 
        boolean lbCheck = false;
        Iterator<Model> detail = Detail().iterator();
        
        //Only trigger this validation when source no is not null or not empty.
        if(Master().getSourceNo() != null && !"".equals(Master().getSourceNo())){
            //check if there is not empty replacement if true all request description must have a replacement description value
            for(int lnCtr = 0; lnCtr <= getDetailCount()-1; lnCtr++){
                lbReplaceID = (Detail(lnCtr).getReplaceId() == null || "".equals(Detail(lnCtr).getReplaceId()));
                lbReplaceDesc = (Detail(lnCtr).getReplaceDescription() == null || "".equals(Detail(lnCtr).getReplaceDescription()));

                if(validateReplacement(Detail(lnCtr).getDescription())){
                    if((lbReplaceID || lbReplaceDesc) && Detail(lnCtr).isReverse()){ 
                        poJSON.put("result", "error");
                        poJSON.put("message", "Found request description <"+Detail(lnCtr).getDescription()+"> with matching replacement.\n\nReplacement Description for row "+(lnCtr+1)+" must not be empty." );
                        return poJSON;

                    }
                }
            }
        }
           
        while (detail.hasNext()) {
            Model item = detail.next();
            if(item.getValue("nQuantity") != null && !"".equals(item.getValue("nQuantity"))){
                lsQuantity = item.getValue("nQuantity").toString();
            }
            lbReqDescription = (item.getValue("sDescript") == null || "".equals(item.getValue("sDescript")));
            lbReqStockID = (item.getValue("sStockIDx") == null || "".equals(item.getValue("sStockIDx")));
            lbReplaceID = (item.getValue("sReplacID") == null || "".equals(item.getValue("sReplacID")));
            lbReplaceDesc = (item.getValue("sReplacDs") == null || "".equals(item.getValue("sReplacDs")));
            lbReqItem = RequestMultipleItem((String) item.getValue("sDescript"));
            
            if(lbReqDescription){
                if (item.getEditMode() == EditMode.ADDNEW) {
                    detail.remove();
                    continue;
                } else {
                    poJSON.put("result", "error");
                    poJSON.put("message", "Request description cannot be empty.\nContact system administrator for assistance.");
                    return poJSON;
                }
            } else {
                if (item.getEditMode() == EditMode.ADDNEW) {
                    if(Double.valueOf(lsQuantity) <= 0.00){
                        if(lbReqItem){
                            detail.remove();
                            continue;
                        } else {
                            item.setValue("cReversex", POQuotationStatus.Reverse.EXCLUDE);
                        }
                    } 
                } else {
                    if(Double.valueOf(lsQuantity) <= 0.00){
                        item.setValue("cReversex", POQuotationStatus.Reverse.EXCLUDE);
                    }
                }
            }
            
            lbReqDescription = false;lbReqStockID = false;lbReplaceID = false;lbReplaceDesc = false;lbReqItem=false;
            lsQuantity = "0.00";
        }
        
//        String lsQuantity = "0.00";
//        boolean lbReqDescription, lbReqStockID, lbReplaceID, lbReplaceDesc; 
//        Iterator<Model> detail = Detail().iterator();
//        while (detail.hasNext()) {
//            Model item = detail.next();
//            if(item.getValue("nQuantity") != null && !"".equals(item.getValue("nQuantity"))){
//                lsQuantity = item.getValue("nQuantity").toString();
//            }
//            
//            lbReqDescription = (item.getValue("sDescript") == null || "".equals(item.getValue("sDescript")));
//            lbReqStockID = (item.getValue("sStockIDx") == null || "".equals(item.getValue("sStockIDx")));
//            lbReplaceID = (item.getValue("sReplacID") == null || "".equals(item.getValue("sReplacID")));
//            lbReplaceDesc = (item.getValue("sReplacDs") == null || "".equals(item.getValue("sReplacDs")));
//            
//            if ( (lbReqDescription && lbReqStockID && lbReplaceID && lbReplaceDesc)
//                  || (Double.valueOf(lsQuantity) <= 0.00)) {
//                if (item.getEditMode() == EditMode.ADDNEW) {
//                    if(item.getValue("sDescript") != null && !"".equals(item.getValue("sDescript"))
//                        && Double.valueOf(lsQuantity) <= 0.00){
//                        item.setValue("cReversex", POQuotationStatus.Reverse.EXCLUDE);
//                    } else {
//                        detail.remove();
//                    }
//                } else {
//                    paDetailRemoved.add(item);
//                    item.setValue("cReversex", POQuotationStatus.Reverse.EXCLUDE);
//                }
//            }
//            
//            lbReqDescription = false;lbReqStockID = false;lbReplaceID = false;lbReplaceDesc = false;
//        }

        //Validate detail after removing all zero qty and empty stock Id
        if (getDetailCount() <= 0) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transaction detail to be save.");
            return poJSON;
        }

        //assign other info on detail
        for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
            Detail(lnCtr).setTransactionNo(Master().getTransactionNo());
            Detail(lnCtr).setEntryNo(lnCtr+1);
        }

        //assign other info on attachment
        for (int lnCtr = 0; lnCtr <= getTransactionAttachmentCount()- 1; lnCtr++) {
            TransactionAttachmentList(lnCtr).getModel().setSourceNo(Master().getTransactionNo());
            TransactionAttachmentList(lnCtr).getModel().setSourceCode(getSourceCode());
            TransactionAttachmentList(lnCtr).getModel().setBranchCode(Master().getBranchCode());
            TransactionAttachmentList(lnCtr).getModel().setImagePath(System.getProperty("sys.default.path.temp.attachments"));
            
            //Check existing file name in database
            if(EditMode.ADDNEW == TransactionAttachmentList(lnCtr).getModel().getEditMode()){
                int lnCopies = 0;
                String fsFilePath = TransactionAttachmentList(lnCtr).getModel().getImagePath() + "/" + TransactionAttachmentList(lnCtr).getModel().getFileName();
                String lsNewFileName = TransactionAttachmentList(lnCtr).getModel().getFileName();
                while ("error".equals((String)checkExistingFileName(lsNewFileName).get("result"))) {
                    lnCopies++;
                    //Rename the file
                    int dotIndex = TransactionAttachmentList(lnCtr).getModel().getFileName().lastIndexOf(".");
                    if (dotIndex == -1) {
                        lsNewFileName = TransactionAttachmentList(lnCtr).getModel().getFileName() +"_"+lnCopies;
                    } else {
                        lsNewFileName = TransactionAttachmentList(lnCtr).getModel().getFileName().substring(0, dotIndex) +"_"+ lnCopies +TransactionAttachmentList(lnCtr).getModel().getFileName().substring(dotIndex);
                    }
                }

                if(lnCopies > 0){
                    Path source = Paths.get(fsFilePath);
                    try {
                        // Copy file into the target directory with a new name
                        Path target = Paths.get(System.getProperty("sys.default.path.temp") + "/Attachments").resolve(lsNewFileName);
                        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                        TransactionAttachmentList(lnCtr).getModel().setFileName(lsNewFileName);
                        System.out.println("File copied successfully as " + lsNewFileName);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            
            //Upload Attachment when send status is 0
            try {
                if("0".equals(TransactionAttachmentList(lnCtr).getModel().getSendStatus())){
                    poJSON = uploadCASAttachments(poGRider, System.getProperty("sys.default.access.token"), lnCtr);
                    if ("error".equals((String) poJSON.get("result"))) {
                        return poJSON;
                    }
                }
            } catch (Exception ex) {
                Logger.getLogger(PurchaseOrderReceiving.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
        
        //Recompute amount
        computeFields();
        
        poJSON.put("result", "success");
        return poJSON;
    }
    
    @Override
    public JSONObject save() {
        /*Put saving business rules here*/
        return isEntryOkay(POQuotationStatus.OPEN);
    }
    
    @Override
    public void saveComplete() {
        /*This procedure was called when saving was complete*/
        System.out.println("Transaction saved successfully.");
    }
    
    @Override
    public JSONObject saveOthers() {
        /*Only modify this if there are other tables to modify except the master and detail tables*/
        poJSON = new JSONObject();
        try {
            //Save Attachments
            for (int lnCtr = 0; lnCtr <= getTransactionAttachmentCount() - 1; lnCtr++) {
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
             
            //Upload attachments to server
//            uploadCASAttachments(poGRider, System.getProperty("sys.default.client.token"), getSourceCode(), Master().getTransactionNo());
            
//            POQuotationRequest object = new QuotationControllers(poGRider, logwrapr).POQuotationRequest();
//            object.InitTransaction();
//            poJSON = object.OpenTransaction(Master().getSourceNo());
//            if ("error".equals((String) poJSON.get("result"))) {
//                return poJSON;
//            }
//            object.setWithParent(true);
//            object.setWithUI(true);
//            switch(Master().getTransactionStatus()){
//                case POQuotationStatus.CONFIRMED:
//                    object.PostTransaction("Post Transaction");
//                    if ("error".equals((String) poJSON.get("result"))) {
//                        return poJSON;
//                    }
//                    break;
//            
//            } 
            
        } catch (SQLException | GuanzonException | CloneNotSupportedException  ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
        } catch (Exception ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
        }
        poJSON.put("result", "success");
        return poJSON;
    }
    
    /**
     * Upload Attachment
     * @param instance
     * @param access 
     * @param fnRow 
     * @return  
     * @throws java.lang.Exception 
     */
    public JSONObject uploadCASAttachments(GRiderCAS instance, String access, int fnRow) throws Exception{       
        poJSON = new JSONObject();
        System.out.println("Uploading... : " + paAttachments.get(fnRow).getModel().getFileName());
        String hash;
        File file = new File(paAttachments.get(fnRow).getModel().getImagePath() + "/" + paAttachments.get(fnRow).getModel().getFileName());

        //check if file is existing
        if(!file.exists()){
            poJSON.put("result", "error");
            poJSON.put("message", "Cannot locate file in " + paAttachments.get(fnRow).getModel().getImagePath() + "/" + paAttachments.get(fnRow).getModel().getFileName()
                                    + ".\nContact system administrator for assistance.");
            return poJSON;  
        }

        //check if file hash is not empty
        hash = paAttachments.get(fnRow).getModel().getMD5Hash();
        if(paAttachments.get(fnRow).getModel().getMD5Hash() == null || "".equals(paAttachments.get(fnRow).getModel().getMD5Hash())){
            hash = MiscReplUtil.md5Hash(paAttachments.get(fnRow).getModel().getImagePath() + "/" + paAttachments.get(fnRow).getModel().getFileName());
        }

        JSONObject result = WebFile.UploadFile(getAccessToken(access)
                                , "0032"
                                , ""
                                , paAttachments.get(fnRow).getModel().getFileName()
                                , instance.getBranchCode()
                                , hash
                                , encodeFileToBase64Binary(file)
                                , paAttachments.get(fnRow).getModel().getSourceCode()
                                , paAttachments.get(fnRow).getModel().getSourceNo()
                                , "");

        if("error".equalsIgnoreCase((String) result.get("result"))){
            System.out.println("Upload Error : " + result.toJSONString());
            System.out.println("Upload Error : " + paAttachments.get(fnRow).getModel().getFileName());
            poJSON.put("result", "error");
            poJSON.put("message", "System error while uploading file "+ paAttachments.get(fnRow).getModel().getFileName()
                                    + ".\nContact system administrator for assistance.");
            return poJSON;
        }
        paAttachments.get(fnRow).getModel().setMD5Hash(hash);
        paAttachments.get(fnRow).getModel().setSendStatus("1");
        System.out.println("Upload Success : " + paAttachments.get(fnRow).getModel().getFileName());
        poJSON.put("result", "success");
        return poJSON;
    }
    
    private static String encodeFileToBase64Binary(File file) throws Exception{
         FileInputStream fileInputStreamReader = new FileInputStream(file);
         byte[] bytes = new byte[(int)file.length()];
         fileInputStreamReader.read(bytes);
         return new String(Base64.encodeBase64(bytes), "UTF-8");
     }    
         
    private static JSONObject token = null;
    private static String getAccessToken(String access){
        try {
            JSONParser oParser = new JSONParser();
            if(token == null){
                token = (JSONObject)oParser.parse(new FileReader(access));
            }
            
            Calendar current_date = Calendar.getInstance();
            current_date.add(Calendar.MINUTE, -25);
            Calendar date_created = Calendar.getInstance();
            date_created.setTime(SQLUtil.toDate((String) token.get("created") , SQLUtil.FORMAT_TIMESTAMP));
            
            //Check if token is still valid within the time frame
            //Request new access token if not in the current period range
            if(current_date.after(date_created)){
                String[] xargs = new String[] {(String) token.get("parent"), access};
                RequestAccess.main(xargs);
                token = (JSONObject)oParser.parse(new FileReader(access));
            }
            
            return (String)token.get("access_key");
        } catch (IOException ex) {
            return null;
        } catch (ParseException ex) {
            return null;
        }
    }

    @Override
    public JSONObject initFields() {
        try {
            /*Put initial model values here*/
            poJSON = new JSONObject();
            System.out.println("Dept ID : " + poGRider.getDepartment());
            System.out.println("Current User : " + poGRider.getUserID());
            
            Master().setIndustryId(psIndustryId);
            Master().setCompanyId(psCompanyId);
            Master().setCategoryCode(psCategorCd);
            Master().setTransactionDate(poGRider.getServerDate());
            Master().setTransactionStatus(POQuotationStatus.OPEN);
            Master().setReferenceNo(""); //prevent null
            Master().setReferenceDate(poGRider.getServerDate());
            LocalDateTime ldt = poGRider.getServerDate().toLocalDateTime().plusMonths(1);
            Master().setValidityDate( Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant()));
            System.out.println("Validity Date : " + Master().getValidityDate());
        } catch (SQLException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
            return poJSON;
        }

        poJSON.put("result", "success");
        return poJSON;
    }
    
    @Override
    protected JSONObject isEntryOkay(String status) {
        GValidator loValidator = POQuotationValidatorFactory.make(Master().getIndustryId());

        loValidator.setApplicationDriver(poGRider);
        loValidator.setTransactionStatus(status);
        loValidator.setMaster(poMaster);
//        loValidator.setDetail(paDetail);

        poJSON = loValidator.validate();

        return poJSON;
    }

    @Override
    public void initSQL() {
        SQL_BROWSE =  " SELECT  "       
                    + "     a.sTransNox  "
                    + "   , a.sIndstCdx  "
                    + "   , a.sBranchCd  "
                    + "   , a.sCategrCd  "
                    + "   , a.dTransact  "
                    + "   , a.sReferNox  "
                    + "   , a.cTranStat  "
                    + "   , a.sCompnyID  "
                    + "   , a.sSourceNo  "
                    + "   , b.sDescript AS Industry      "
                    + "   , c.sBranchNm AS Branch        "
                    + "   , f.sDescript AS Category2     "
                    + "   , h.sDeptName AS Department    "
                    + "   , i.sCompnyNm AS SpplierNm  "
                    + "  FROM PO_Quotation_Master a      "
                    + "  LEFT JOIN Industry b ON b.sIndstCdx = a.sIndstCdx          "
                    + "  LEFT JOIN Branch c ON c.sBranchCd = a.sBranchCd            "
                    + "  LEFT JOIN Company d ON d.sCompnyID = a.sCompnyID           "
                    + "  LEFT JOIN Category e ON e.sCategrCd = a.sCategrCd          "
                    + "  LEFT JOIN PO_Quotation_Request_Master g ON g.sTransNox = a.sSourceNo "
                    + "  LEFT JOIN Category_Level2 f ON f.sCategrCd = g.sCategCd2   "
                    + "  LEFT JOIN Department h ON h.sDeptIDxx = g.sDeptIDxx        "
                    + "  LEFT JOIN Client_Master i ON i.sClientID = a.sSupplier     ";
        
    }
    
    private String getSupplierSQL(){
        return   " SELECT "
                + "   b.sTransNox "
                + " , b.sIndstCdx "
                + " , b.sBranchCd "
                + " , b.sDeptIDxx "
                + " , b.sCategrCd "
                + " , b.dTransact "
                + " , b.sCategCd2 "
                + " , b.sDestinat "
                + " , b.sReferNox "
                + " , b.cTranStat "
                + " , a.sSupplier "
                + " , a.sCompnyID "
                + " , c.sDescript AS Industry   "
                + " , i.sCompnyNm AS Company    "
                + " , d.sBranchNm AS Branch     "
                + " , e.sDeptName AS Department "
                + " , f.sDescript AS Category   "
                + " , g.sDescript AS Category2  "
                + " , h.sCompnyNm AS  SpplierNm       "
                + " FROM PO_Quotation_Request_Supplier a "
                + " LEFT JOIN PO_Quotation_Request_Master b ON b.sTransNox = a.sTransNox "
                + " LEFT JOIN Industry c ON c.sIndstCdx = b.sIndstCdx                    "
                + " LEFT JOIN Branch d ON d.sBranchCd = b.sBranchCd                      "
                + " LEFT JOIN Department e ON e.sDeptIDxx = b.sDeptIDxx                  "
                + " LEFT JOIN Category f ON f.sCategrCd = b.sCategrCd                    "
                + " LEFT JOIN Category_Level2 g ON g.sCategrCd = b.sCategCd2             "
                + " LEFT JOIN Client_Master h ON h.sClientID = a.sSupplier               "
                + " LEFT JOIN Company i ON i.sCompnyID = a.sCompnyID                     ";
    
    }
    
    private String getInventorySQL(){
        return  " SELECT "
            + "   a.sStockIDx, "
            + "   a.sBarCodex, "
            + "   a.sDescript, "
            + "   a.sSupersed, "
            + "   a.cRecdStat, "
            + "   IFNULL(b.sDescript, '')    xBrandNme, "
            + "   IFNULL(c.sDescript, '')    xModelNme, "
            + "   IFNULL(d.sDescript, '')    xColorNme, "
            + "   IFNULL(e.sDescript, '')    xMeasurNm, "
            + "   TRIM(CONCAT(IFNULL(f.sDescript, ''), ' ', IFNULL(f.nYearMdlx, '')))    xVrntName, "
            + "   IFNULL(c.sModelCde, '')    xModelCde, "
            + "   IFNULL(h.sDescript, '')    xCategory, "
            + "   IFNULL(i.sDescript, '')    xCategry2  "
            + " FROM Inventory a                        "
            + " LEFT JOIN Brand b ON a.sBrandIDx = b.sBrandIDx   "
            + " LEFT JOIN Model c ON a.sModelIDx = c.sModelIDx   "
            + " LEFT JOIN Color d ON a.sColorIDx = d.sColorIDx   "
            + " LEFT JOIN Measure e ON a.sMeasurID = e.sMeasurID "
            + " LEFT JOIN Model_Variant f ON a.sVrntIDxx = f.sVrntIDxx  " 
           + " LEFT JOIN Inv_Supplier g ON a.sStockIDx = g.sStockIDx   "
            + " LEFT JOIN Category h ON h.sCategrCd = a.sCategCd1       "
            + " LEFT JOIN Category_Level2 i ON i.sCategrCd = a.sCategCd2";
    }
    
    public void ShowStatusHistory() throws SQLException, GuanzonException, Exception{
        CachedRowSet crs = getStatusHistory();
        
        crs.beforeFirst();
        
        while(crs.next()){
            switch (crs.getString("cRefrStat")){
                case "":
                    crs.updateString("cRefrStat", "-");
                    break;
                case POQuotationStatus.OPEN:
                    crs.updateString("cRefrStat", "OPEN");
                    break;
                case POQuotationStatus.CONFIRMED:
                    crs.updateString("cRefrStat", "CONFIRMED");
                    break;
                case POQuotationStatus.POSTED:
                    crs.updateString("cRefrStat", "POSTED");
                    break;
                case POQuotationStatus.CANCELLED:
                    crs.updateString("cRefrStat", "CANCELLED");
                    break;
                case POQuotationStatus.VOID:
                    crs.updateString("cRefrStat", "VOID");
                    break;
                case POQuotationStatus.APPROVED:
                    crs.updateString("cRefrStat", "APPROVED");
                    break;
                case POQuotationStatus.RETURNED:
                    crs.updateString("cRefrStat", "RETURNED");
                    break;
                default:
                    char ch = crs.getString("cRefrStat").charAt(0);
                    String stat = String.valueOf((int) ch - 64);
                    
                    switch (stat){
                        case POQuotationStatus.OPEN:
                            crs.updateString("cRefrStat", "OPEN");
                            break;
                        case POQuotationStatus.CONFIRMED:
                            crs.updateString("cRefrStat", "CONFIRMED");
                            break;
                        case POQuotationStatus.POSTED:
                            crs.updateString("cRefrStat", "POSTED");
                            break;
                        case POQuotationStatus.CANCELLED:
                            crs.updateString("cRefrStat", "CANCELLED");
                            break;
                        case POQuotationStatus.VOID:
                            crs.updateString("cRefrStat", "VOID");
                            break;
                        case POQuotationStatus.APPROVED:
                            crs.updateString("cRefrStat", "APPROVED");
                            break;
                        case POQuotationStatus.RETURNED:
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
        
        showStatusHistoryUI("Purchase Order Quotation", (String) poMaster.getValue("sTransNox"), entryBy, entryDate, crs);
    }
    
    public JSONObject getEntryBy() throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        String lsEntry = "";
        String lsEntryDate = "";
        String lsSQL =  " SELECT b.sModified, b.dModified " 
                        + " FROM PO_Quotation_Master a "
                        + " LEFT JOIN xxxAuditLogMaster b ON b.sSourceNo = a.sTransNox AND b.sEventNme LIKE 'ADD%NEW' AND b.sRemarksx = " + SQLUtil.toSQL(Master().getTable());
        lsSQL = MiscUtil.addCondition(lsSQL, " a.sTransNox =  " + SQLUtil.toSQL(Master().getTransactionNo())) ;
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
