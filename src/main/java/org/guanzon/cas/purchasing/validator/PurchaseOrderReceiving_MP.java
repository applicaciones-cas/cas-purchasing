/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.guanzon.cas.purchasing.validator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import org.guanzon.appdriver.base.GRider;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.iface.GValidator;
import org.guanzon.cas.purchasing.model.Model_POR_Detail;
import org.guanzon.cas.purchasing.model.Model_POR_Master;
import org.guanzon.cas.purchasing.status.PurchaseOrderReceivingStatus;
import org.json.simple.JSONObject;

/**
 *
 * @author Arsiela 03-12-2025
 */
public class PurchaseOrderReceiving_MP implements GValidator{
    GRiderCAS poGrider;
    String psTranStat;
    JSONObject poJSON;
    
    Model_POR_Master poMaster;
    ArrayList<Model_POR_Detail> poDetail;

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
        switch (psTranStat){
            case PurchaseOrderReceivingStatus.OPEN:
                return validateNew();
            case PurchaseOrderReceivingStatus.CONFIRMED:
                return validateConfirmed();
            case PurchaseOrderReceivingStatus.POSTED:
                return validateProcessed();
            case PurchaseOrderReceivingStatus.CANCELLED:
                return validateCancelled();
            case PurchaseOrderReceivingStatus.VOID:
                return validateVoid();
            case PurchaseOrderReceivingStatus.RETURNED:
                return validateProcessed();
            default:
                poJSON = new JSONObject();
                poJSON.put("result", "success");
        }
        
        return poJSON;
    }
    
    private JSONObject validateNew(){
        poJSON = new JSONObject();
        Date loTransactionDate = poMaster.getTransactionDate();

        if (loTransactionDate == null) {
            poJSON.put("message", "Invalid Transaction Date.");
            return poJSON;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        if ("1900-01-01".equals(sdf.format(loTransactionDate))) {
            poJSON.put("message", "Invalid Transaction Date.");
            return poJSON;
        }

        Date currentDate = new Date();
        String currentDateStr = sdf.format(currentDate);
        String transactionDateStr = sdf.format(loTransactionDate);
        if (transactionDateStr.compareTo(currentDateStr) > 0) {
            poJSON.put("message", "Future transaction dates are not allowed.");
            return poJSON;
        }

        if (poMaster.getIndustryId() == null) {
            poJSON.put("message", "Industry is not set.");
            return poJSON;
        }
        if (poMaster.getCompanyId() == null || poMaster.getCompanyId().equals("")) {
            poJSON.put("message", "Company is not set.");
            return poJSON;
        }
        if (poMaster.getSupplierId() == null || poMaster.getSupplierId().equals("")) {
            poJSON.put("message", "Supplier is not set.");
            return poJSON;
        }
        if (poMaster.getTermCode() == null || poMaster.getTermCode().equals("")) {
            poJSON.put("message", "Invalid Term.");
            return poJSON;
        }
        poJSON.put("result", "success");
        return poJSON;
    }
    
    private JSONObject validateConfirmed(){
        poJSON = new JSONObject();
                
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
    
    private JSONObject validateReturned(){
        poJSON = new JSONObject();
                
        poJSON.put("result", "success");
        return poJSON;
    }
    
}
