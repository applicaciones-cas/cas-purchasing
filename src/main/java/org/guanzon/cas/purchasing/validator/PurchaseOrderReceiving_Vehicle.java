/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.guanzon.cas.purchasing.validator;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.iface.GValidator;
import org.guanzon.cas.purchasing.model.Model_POR_Detail;
import org.guanzon.cas.purchasing.model.Model_POR_Master;
import org.guanzon.cas.purchasing.status.PurchaseOrderReceivingStatus;
import org.guanzon.cas.purchasing.status.PurchaseOrderReturnStatus;
import org.json.simple.JSONObject;

/**
 *
 * @author Arsiela 03-12-2025
 */
public class PurchaseOrderReceiving_Vehicle implements GValidator{
    GRiderCAS poGRider;
    String psTranStat;
    JSONObject poJSON;
    
    Model_POR_Master poMaster;
    ArrayList<Model_POR_Detail> poDetail;

    @Override
    public void setApplicationDriver(Object applicationDriver) {
        poGRider = (GRiderCAS) applicationDriver;
    }

    @Override
    public void setTransactionStatus(String transactionStatus) {
        psTranStat = transactionStatus;
    }

    @Override
    public void setMaster(Object value) {
        poMaster = (Model_POR_Master) value;
    }

    @Override
    public void setDetail(ArrayList<Object> value) {
        poDetail.clear();
        for(int lnCtr = 0; lnCtr <= value.size() - 1; lnCtr++){
            poDetail.add((Model_POR_Detail) value.get(lnCtr));
        }
    }

    @Override
    public void setOthers(ArrayList<Object> value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public JSONObject validate() {
        try {
            switch (psTranStat){
                case PurchaseOrderReceivingStatus.OPEN:
                    return validateNew();
                case PurchaseOrderReceivingStatus.CONFIRMED:
                    return validateConfirmed();
                case PurchaseOrderReceivingStatus.POSTED:
                    return validatePosted();
                case PurchaseOrderReceivingStatus.PAID:
                    return validatePaid();
                case PurchaseOrderReceivingStatus.CANCELLED:
                    return validateCancelled();
                case PurchaseOrderReceivingStatus.VOID:
                    return validateVoid();
                case PurchaseOrderReceivingStatus.RETURNED:
                    return validateReturned();
                default:
                    poJSON = new JSONObject();
                    poJSON.put("result", "success");
            }
        } catch (SQLException ex) {
            Logger.getLogger(PurchaseOrderReceiving_Vehicle.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return poJSON;
    }
    
    private JSONObject validateNew() throws SQLException{
        poJSON = new JSONObject();
        Date loTransactionDate = poMaster.getTransactionDate();
        Date loReferenceDate = poMaster.getReferenceDate();
        LocalDate serverDate = strToDate(xsDateShort(poGRider.getServerDate()));
        LocalDate oneYearAgo = serverDate.minusYears(1);
        
        if (loTransactionDate == null) {
            poJSON.put("message", "Invalid Transaction Date.");
            return poJSON;
        }

        if ("1900-01-01".equals(xsDateShort(loTransactionDate))) {
            poJSON.put("message", "Invalid Transaction Date.");
            return poJSON;
        }
        LocalDate transactionDate = strToDate(xsDateShort(poMaster.getTransactionDate()));
        if (transactionDate.isAfter(serverDate)) {
            poJSON.put("message", "Future transaction dates are not allowed.");
            return poJSON;
        }
        // Backdated beyond 1 year validation
        if (transactionDate.isBefore(oneYearAgo)) {
            poJSON.put("message", "Backdated transactions beyond 1 year are not allowed.");
            return poJSON;
        }
        if (poMaster.getIndustryId() == null) {
            poJSON.put("message", "Industry is not set.");
            return poJSON;
        }
        if (poMaster.getCompanyId() == null || poMaster.getCompanyId().isEmpty()) {
            poJSON.put("message", "Company is not set.");
            return poJSON;
        }
        if (poMaster.getDepartmentId()== null || poMaster.getDepartmentId().isEmpty()) {
            poJSON.put("message", "Department is not set.");
            return poJSON;
        }
        if (poMaster.getCategoryCode()== null || poMaster.getCategoryCode().isEmpty()) {
            poJSON.put("message", "Category is not set.");
            return poJSON;
        }
        //Do not validate supplier: PO Receiving is allowed to save without supplier according to sir mac
        //Need to validate supplier : based on meeting 04142025
        if (poMaster.getSupplierId() == null || poMaster.getSupplierId().isEmpty()) {
            poJSON.put("message", "Supplier is not set.");
            return poJSON;
        }
//        if (poMaster.getTruckingId()== null || poMaster.getTruckingId().isEmpty()) {
//            poJSON.put("message", "Trucking is not set.");
//            return poJSON;
//        }
        //If trucking is not empty FREIGHT AMOUNT is required
        if (poMaster.getTruckingId()!= null && !"".equals(poMaster.getTruckingId())) {
            if (poMaster.getFreight().doubleValue() <= 0.00) {
                poJSON.put("message", "Invalid Freight Amount.");
                return poJSON;
            }
        }
        if (loReferenceDate == null) {
            poJSON.put("message", "Invalid Reference Date.");
            return poJSON;
        }
        if ("1900-01-01".equals(xsDateShort(loReferenceDate))) {
            poJSON.put("message", "Invalid Reference Date.");
            return poJSON;
        }
        LocalDate referenceDate = strToDate(xsDateShort(poMaster.getReferenceDate()));
        if (referenceDate.isAfter(serverDate)) {
            poJSON.put("message", "Future reference dates are not allowed.");
            return poJSON;
        }
        if (referenceDate.isAfter(transactionDate)) {
            poJSON.put("message", "Reference date cannot be later than the receiving date.");
            return poJSON;
        }
        // Backdated beyond 1 year validation
        if (referenceDate.isBefore(oneYearAgo)) {
            poJSON.put("message", "Backdated reference dates beyond 1 year are not allowed.");
            return poJSON;
        }
        if (poMaster.getReferenceNo()== null || poMaster.getReferenceNo().isEmpty()) {
            poJSON.put("message", "Reference is not set.");
            return poJSON;
        }
        if (poMaster.getTermCode() == null || poMaster.getTermCode().isEmpty()) {
            poJSON.put("message", "Invalid Term.");
            return poJSON;
        }
        
        poJSON.put("result", "success");
        return poJSON;
    }
    
    private JSONObject validateConfirmed()throws SQLException{
        poJSON = new JSONObject();
        Date loTransactionDate = poMaster.getTransactionDate();
        Date loReferenceDate = poMaster.getReferenceDate();
        LocalDate serverDate = strToDate(xsDateShort(poGRider.getServerDate()));
        LocalDate oneYearAgo = serverDate.minusYears(1);
        
        if (loTransactionDate == null) {
            poJSON.put("message", "Invalid Transaction Date.");
            return poJSON;
        }

        if ("1900-01-01".equals(xsDateShort(loTransactionDate))) {
            poJSON.put("message", "Invalid Transaction Date.");
            return poJSON;
        }
        LocalDate transactionDate = strToDate(xsDateShort(poMaster.getTransactionDate()));
        if (transactionDate.isAfter(serverDate)) {
            poJSON.put("message", "Future transaction dates are not allowed.");
            return poJSON;
        }
        // Backdated beyond 1 year validation
        if (transactionDate.isBefore(oneYearAgo)) {
            poJSON.put("message", "Backdated transactions beyond 1 year are not allowed.");
            return poJSON;
        }
        if (poMaster.getIndustryId() == null) {
            poJSON.put("message", "Industry is not set.");
            return poJSON;
        }
        if (poMaster.getCompanyId() == null || poMaster.getCompanyId().isEmpty()) {
            poJSON.put("message", "Company is not set.");
            return poJSON;
        }
        if (poMaster.getDepartmentId()== null || poMaster.getDepartmentId().isEmpty()) {
            poJSON.put("message", "Department is not set.");
            return poJSON;
        }
        if (poMaster.getCategoryCode()== null || poMaster.getCategoryCode().isEmpty()) {
            poJSON.put("message", "Category is not set.");
            return poJSON;
        }
        //Do not validate supplier: PO Receiving is allowed to save without supplier according to sir mac
        //Need to validate supplier : based on meeting 04142025
        if (poMaster.getSupplierId() == null || poMaster.getSupplierId().isEmpty()) {
            poJSON.put("message", "Supplier is not set.");
            return poJSON;
        }
//        if (poMaster.getTruckingId()== null || poMaster.getTruckingId().isEmpty()) {
//            poJSON.put("message", "Trucking is not set.");
//            return poJSON;
//        }
        //If trucking is not empty FREIGHT AMOUNT is required
        if (poMaster.getTruckingId()!= null && !"".equals(poMaster.getTruckingId())) {
            if (poMaster.getFreight().doubleValue() <= 0.00) {
                poJSON.put("message", "Invalid Freight Amount.");
                return poJSON;
            }
        }
        if (loReferenceDate == null) {
            poJSON.put("message", "Invalid Reference Date.");
            return poJSON;
        }
        if ("1900-01-01".equals(xsDateShort(loReferenceDate))) {
            poJSON.put("message", "Invalid Reference Date.");
            return poJSON;
        }
        LocalDate referenceDate = strToDate(xsDateShort(poMaster.getTransactionDate()));
        if (referenceDate.isAfter(serverDate)) {
            poJSON.put("message", "Future reference dates are not allowed.");
            return poJSON;
        }
        // Backdated beyond 1 year validation
        if (referenceDate.isBefore(oneYearAgo)) {
            poJSON.put("message", "Backdated reference dates beyond 1 year are not allowed.");
            return poJSON;
        }
        if (poMaster.getReferenceNo()== null || poMaster.getReferenceNo().isEmpty()) {
            poJSON.put("message", "Reference is not set.");
            return poJSON;
        }
        if (poMaster.getTermCode() == null || poMaster.getTermCode().isEmpty()) {
            poJSON.put("message", "Invalid Term.");
            return poJSON;
        }
        
        poJSON.put("result", "success");
        return poJSON;
    }
    
    private JSONObject validateApproved(){
        poJSON = new JSONObject();
                
        poJSON.put("result", "success");
        return poJSON;
    }
    
    private JSONObject validateProcessed(){
        poJSON = new JSONObject();
                
        poJSON.put("result", "success");
        return poJSON;
    }
    
    private JSONObject validatePosted(){
        poJSON = new JSONObject();
                
        poJSON.put("result", "success");
        return poJSON;
    }
    
    private JSONObject validatePaid(){
        poJSON = new JSONObject();
                
        poJSON.put("result", "success");
        return poJSON;
    }
    
    private JSONObject validateCancelled() throws SQLException{
        poJSON = new JSONObject();
        
        //TODO
//        poJSON = checkPOReturn();
//        if("error".equals((String) poJSON.get("result"))){
//            poJSON.put("message", "Found existing Purchase Order Return <" + (String) poJSON.get("sTransNox") + ">. Cancellation of transaction aborted.");
//            return poJSON;
//        }
        
        poJSON.put("result", "success");
        return poJSON;
    }
    
    private JSONObject validateVoid() throws SQLException{
        poJSON = new JSONObject();
        
        //TODO
//        poJSON = checkPOReturn();
//        if("error".equals((String) poJSON.get("result"))){
//            poJSON.put("message", "Found existing Purchase Order Return <" + (String) poJSON.get("sTransNox") + ">. Voiding of transaction aborted.");
//            return poJSON;
//        }
        
        poJSON.put("result", "success");
        return poJSON;
    }
    
    private JSONObject validateReturned() throws SQLException{
        poJSON = new JSONObject();
        
            //TODO
//        poJSON = checkPOReturn();
//        if("error".equals((String) poJSON.get("result"))){
//            poJSON.put("message", "Found existing Purchase Order Return <" + (String) poJSON.get("sTransNox") + ">. Returning of transaction aborted.");
//            return poJSON;
//        }
        
        poJSON.put("result", "success");
        return poJSON;
    }
    
    //check existing PO Return
    private JSONObject checkPOReturn() throws SQLException{
        poJSON = new JSONObject();
        String lsPOReturn = "";
        String lsSQL = " SELECT "
                + " a.sTransNox "
                + " FROM po_return_master a ";
        lsSQL = MiscUtil.addCondition(lsSQL, " a.sSourceNo = " + SQLUtil.toSQL(poMaster.getTransactionNo())
                + " AND ( a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderReturnStatus.CONFIRMED)
                + " OR a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderReturnStatus.PAID)
                + " OR a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderReturnStatus.POSTED)
                + " ) ");
        
        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        try {
            if (MiscUtil.RecordCount(loRS) > 0) {
                while (loRS.next()) {
                    if(lsPOReturn.isEmpty()){
                        lsPOReturn = loRS.getString("sTransNox");
                    } else {
                        lsPOReturn = lsPOReturn + ", " +loRS.getString("sTransNox");
                    }
                }
                poJSON.put("result", "error");
                poJSON.put("result", "sTransNox");
                return poJSON;
            }
            MiscUtil.close(loRS);
        } catch (SQLException e) {
            System.out.println("No record loaded.");
        }
        
        poJSON.put("result", "success");
        return poJSON; 
    }
    
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
    
    
}
