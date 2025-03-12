/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.guanzon.cas.purchasing.model;

import java.sql.SQLException;
import java.util.Date;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.cas.inv.model.Model_Inventory;
import org.guanzon.cas.inv.services.InvModels;
import org.json.simple.JSONObject;

/**
 *
 * @author Arsiela 03-12-2025
 */
public class Model_POR_Detail extends Model{
    
    //reference objects  
    Model_Inventory poInventory;    
    
    @Override
    public void initialize() {
        try {
            poEntity = MiscUtil.xml2ResultSet(System.getProperty("sys.default.path.metadata") + XML, getTable());
            
            poEntity.last();
            poEntity.moveToInsertRow();

            MiscUtil.initRowSet(poEntity);
            
            //assign default values
            poEntity.updateObject("dExpiryDt", "0000-00-00");
            poEntity.updateObject("nEntryNox", 0);
            poEntity.updateObject("nQuantity", 0);
            poEntity.updateObject("nWHCountx", 0);
            poEntity.updateObject("nOrderQty", 0);
            poEntity.updateDouble("nUnitPrce", 0.00);
            poEntity.updateDouble("nFreightx", 0.00);
            //end - assign default values

            poEntity.insertRow();
            poEntity.moveToCurrentRow();
            poEntity.absolute(1);

            ID = "sTransNox";
            ID2 = "nEntryNox";
            
            //initialize reference objects
            InvModels invModel = new InvModels(poGRider); 
//            poInventory = invModel.Inventory();
            //end - initialize reference objects
            
            pnEditMode = EditMode.UNKNOWN;
        } catch (SQLException e) {
            logwrapr.severe(e.getMessage());
            System.exit(1);
        }
    }
    
    public JSONObject setTransactionNo(String transactionNo){
        return setValue("sTransNox", transactionNo);
    }
    
    public String getTransactionNo  (){
        return (String) getValue("sTransNox");
    }
    
    public JSONObject setEntryNo(int entryNo){
        return setValue("nEntryNox", entryNo);
    }
    
    public int getEntryNo(){
        return (int) getValue("nEntryNox");
    }
    
    public JSONObject setOrderNo(String orderNo){
        return setValue("sOrderNox", orderNo);
    }
    
    public String getOrderNo(){
        return (String) getValue("sOrderNox");
    }
    
    public JSONObject setStockID(String stockID){
        return setValue("sStockIDx", stockID);
    }
    
    public String getStockID(){
        return (String) getValue("sStockIDx");
    }
    
    public JSONObject setReplaceID(String replaceID){
        return setValue("sReplacID", replaceID);
    }
    
    public String getReplaceID(){
        return (String) getValue("sReplacID");
    }
    
    public JSONObject setUnitType(String unitType){
        return setValue("cUnitType", unitType);
    }
    
    public String getUnitType(){
        return (String) getValue("cUnitType");
    }
    
    public JSONObject setQuantity(int quantity){
        return setValue("nQuantity", quantity);
    }
    
    public int getQuantity(){
        return (int) getValue("nQuantity");
    }
    
    public JSONObject setUnitPrce(Double unitPrce){
        return setValue("nUnitPrce", unitPrce);
    }
    
    public Double getUnitPrce(){
        return (Double) getValue("nUnitPrce");
    }
    
    public JSONObject setFreight(Double freight){
        return setValue("nFreightx", freight);
    }
    
    public Double getFreight(){
        return (Double) getValue("nFreightx");
    }
    
    public JSONObject setExpiryDate(Date expiryDate){
        return setValue("dExpiryDt", expiryDate);
    }
    
    public Date getExpiryDate(){
        return (Date) getValue("dExpiryDt");
    }
    
    public JSONObject setWhCount(int whCount){
        return setValue("nWHCountx", whCount);
    }
    
    public int getWhCount(){
        return (int) getValue("nWHCountx");
    }
    
    public JSONObject setOrderQty(int orderQty){
        return setValue("nOrderQty", orderQty);
    }
    
    public int getOrderQty(){
        return (int) getValue("nOrderQty");
    }
    
    public JSONObject setWithVat(String withVat){
        return setValue("cWithVATx", withVat);
    }
    
    public String getWithVat(){
        return (String) getValue("cWithVATx");
    }
    
    public JSONObject setModifiedDate(Date modifiedDate){
        return setValue("dModified", modifiedDate);
    }
    
    public Date getModifiedDate(){
        return (Date) getValue("dModified");
    }
    
    //reference object models
    public Model_Inventory Inventory() {
        if (!"".equals((String) getValue("sStockID"))) {
            if (poInventory.getEditMode() == EditMode.READY
                    && poInventory.getStockId().equals((String) getValue("sStockID"))) {
                return poInventory;
            } else {
                poJSON = poInventory.openRecord((String) getValue("sStockID"));

                if ("success".equals((String) poJSON.get("result"))) {
                    return poInventory;
                } else {
                    poInventory.initialize();
                    return poInventory;
                }
            }
        } else {
            poInventory.initialize();
            return poInventory;
        }
    }
    
    public Model_Inventory Supersede() {
        if (!"".equals((String) getValue("sReplacID"))) {
            if (poInventory.getEditMode() == EditMode.READY
                    && poInventory.getStockId().equals((String) getValue("sReplacID"))) {
                return poInventory;
            } else {
                poJSON = poInventory.openRecord((String) getValue("sReplacID"));

                if ("success".equals((String) poJSON.get("result"))) {
                    return poInventory;
                } else {
                    poInventory.initialize();
                    return poInventory;
                }
            }
        } else {
            poInventory.initialize();
            return poInventory;
        }
    }
    //end reference object models
}
