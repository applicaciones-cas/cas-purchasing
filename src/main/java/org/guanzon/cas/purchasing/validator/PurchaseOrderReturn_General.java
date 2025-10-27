/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.guanzon.cas.purchasing.validator;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.iface.GValidator;
import ph.com.guanzongroup.cas.model.Model_POReturn_Detail;
import ph.com.guanzongroup.cas.model.Model_POReturn_Master;
import org.guanzon.cas.purchasing.status.PurchaseOrderReturnStatus;
import org.json.simple.JSONObject;

/**
 *
 * @author Arsiela 04-28-2025
 */
public class PurchaseOrderReturn_General implements GValidator{
    GRiderCAS poGrider;
    String psTranStat;
    JSONObject poJSON;
    
    Model_POReturn_Master poMaster;
    ArrayList<Model_POReturn_Detail> paDetail;

    @Override
    public void setApplicationDriver(Object applicationDriver) {
        poGrider = (GRiderCAS) applicationDriver;
    }

    @Override
    public void setTransactionStatus(String transactionStatus) {
        psTranStat = transactionStatus;
    }

    @Override
    public void setMaster(Object value) {
        poMaster = (Model_POReturn_Master) value;
    }

    @Override
    public void setDetail(ArrayList<Object> value) {
        paDetail.clear();
        for(int lnCtr = 0; lnCtr <= value.size() - 1; lnCtr++){
            paDetail.add((Model_POReturn_Detail) value.get(lnCtr));
        }
    }

    @Override
    public void setOthers(ArrayList<Object> value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public JSONObject validate() {
        //validate status change request
        JSONObject loJson = StatusChangeValidator.validatePOReturnStatChange(poMaster, psTranStat);
        if (!"success".equals((String) loJson.get("result"))) {
            return loJson;
        }
        
        try {
            switch (psTranStat){
                case PurchaseOrderReturnStatus.OPEN:
                    return validateNew();
                case PurchaseOrderReturnStatus.CONFIRMED:
                    return validateConfirmed();
                case PurchaseOrderReturnStatus.POSTED:
                    return validatePosted();
                case PurchaseOrderReturnStatus.PAID:
                    return validatePaid();
                case PurchaseOrderReturnStatus.CANCELLED:
                    return validateCancelled();
                case PurchaseOrderReturnStatus.VOID:
                    return validateVoid();
                case PurchaseOrderReturnStatus.RETURNED:
                    return validateReturned();
                default:
                    poJSON = new JSONObject();
                    poJSON.put("result", "success");
            }
        } catch (SQLException ex) {
            Logger.getLogger(PurchaseOrderReturn_General.class.getName()).log(Level.SEVERE, null, ex);
        } catch (GuanzonException ex) {
            Logger.getLogger(PurchaseOrderReturn_General.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return poJSON;
    }
    
    private JSONObject validateNew() throws SQLException, GuanzonException{
        poJSON = new JSONObject();
        Date loTransactionDate = poMaster.getTransactionDate();
        LocalDate serverDate = strToDate(xsDateShort(poGrider.getServerDate()));
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
//        if (poMaster.getIndustryId() == null) {
//            poJSON.put("message", "Industry is not set.");
//            return poJSON;
//        }
        if (poMaster.getCompanyId() == null || poMaster.getCompanyId().isEmpty()) {
            poJSON.put("message", "Company is not set.");
            return poJSON;
        }
        if (poMaster.getBranchCode()== null || poMaster.getBranchCode().isEmpty()) {
            poJSON.put("message", "Branch Code is not set.");
            return poJSON;
        }
        if (poMaster.getCategoryCode()== null || poMaster.getCategoryCode().isEmpty()) {
            poJSON.put("message", "Category is not set.");
            return poJSON;
        }
        if (poMaster.getSupplierId() == null || poMaster.getSupplierId().isEmpty()) {
            poJSON.put("message", "Supplier is not set.");
            return poJSON;
        }
        if (poMaster.getSourceNo()== null || poMaster.getSourceNo().isEmpty()) {
            poJSON.put("message", "Source No is not set.");
            return poJSON;
        }
        if (poMaster.getSourceCode()== null || poMaster.getSourceCode().isEmpty()) {
            poJSON.put("message", "Source Code is not set.");
            return poJSON;
        }
        
        LocalDate loPOReceivingDate = strToDate(xsDateShort(poMaster.PurchaseOrderReceivingMaster().getTransactionDate()));
        if (transactionDate.isBefore(loPOReceivingDate)) {
            poJSON.put("message", "Transaction date cannot be before the receiving date.");
            return poJSON;
        }
        
        poJSON.put("result", "success");
        return poJSON;
    }
    
    private JSONObject validateConfirmed() throws SQLException, GuanzonException{
        poJSON = new JSONObject();
        Date loTransactionDate = poMaster.getTransactionDate();
        LocalDate serverDate = strToDate(xsDateShort(poGrider.getServerDate()));
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
//        if (poMaster.getIndustryId() == null) {
//            poJSON.put("message", "Industry is not set.");
//            return poJSON;
//        }
        if (poMaster.getCompanyId() == null || poMaster.getCompanyId().isEmpty()) {
            poJSON.put("message", "Company is not set.");
            return poJSON;
        }
        if (poMaster.getBranchCode()== null || poMaster.getBranchCode().isEmpty()) {
            poJSON.put("message", "Branch Code is not set.");
            return poJSON;
        }
        if (poMaster.getCategoryCode()== null || poMaster.getCategoryCode().isEmpty()) {
            poJSON.put("message", "Category is not set.");
            return poJSON;
        }
        if (poMaster.getSupplierId() == null || poMaster.getSupplierId().isEmpty()) {
            poJSON.put("message", "Supplier is not set.");
            return poJSON;
        }
        if (poMaster.getSourceNo()== null || poMaster.getSourceNo().isEmpty()) {
            poJSON.put("message", "Source No is not set.");
            return poJSON;
        }
        if (poMaster.getSourceCode()== null || poMaster.getSourceCode().isEmpty()) {
            poJSON.put("message", "Source Code is not set.");
            return poJSON;
        }
        
        LocalDate loPOReceivingDate = strToDate(xsDateShort(poMaster.PurchaseOrderReceivingMaster().getTransactionDate()));
        if (transactionDate.isBefore(loPOReceivingDate)) {
            poJSON.put("message", "Transaction date cannot be before the receiving date.");
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
    
    private JSONObject validateCancelled(){
        poJSON = new JSONObject();
                
        poJSON.put("result", "success");
        return poJSON;
    }
    
    private JSONObject validateVoid(){
        poJSON = new JSONObject();
                
        poJSON.put("result", "success");
        return poJSON;
    }
    
    private JSONObject validateReturned() throws SQLException, GuanzonException{
        poJSON = new JSONObject();
        Date loTransactionDate = poMaster.getTransactionDate();
        LocalDate serverDate = strToDate(xsDateShort(poGrider.getServerDate()));
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
//        if (poMaster.getIndustryId() == null) {
//            poJSON.put("message", "Industry is not set.");
//            return poJSON;
//        }
        if (poMaster.getCompanyId() == null || poMaster.getCompanyId().isEmpty()) {
            poJSON.put("message", "Company is not set.");
            return poJSON;
        }
        if (poMaster.getBranchCode()== null || poMaster.getBranchCode().isEmpty()) {
            poJSON.put("message", "Branch Code is not set.");
            return poJSON;
        }
        if (poMaster.getCategoryCode()== null || poMaster.getCategoryCode().isEmpty()) {
            poJSON.put("message", "Category is not set.");
            return poJSON;
        }
        if (poMaster.getSupplierId() == null || poMaster.getSupplierId().isEmpty()) {
            poJSON.put("message", "Supplier is not set.");
            return poJSON;
        }
        if (poMaster.getSourceNo()== null || poMaster.getSourceNo().isEmpty()) {
            poJSON.put("message", "Source No is not set.");
            return poJSON;
        }
        if (poMaster.getSourceCode()== null || poMaster.getSourceCode().isEmpty()) {
            poJSON.put("message", "Source Code is not set.");
            return poJSON;
        }
        
        LocalDate loPOReceivingDate = strToDate(xsDateShort(poMaster.PurchaseOrderReceivingMaster().getTransactionDate()));
        if (transactionDate.isBefore(loPOReceivingDate)) {
            poJSON.put("message", "Transaction date cannot be before the receiving date.");
            return poJSON;
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
