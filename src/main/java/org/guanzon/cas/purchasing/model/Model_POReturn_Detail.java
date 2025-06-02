/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.guanzon.cas.purchasing.model;

import java.sql.SQLException;
import java.util.Date;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.cas.inv.model.Model_Inv_Serial;
import org.guanzon.cas.inv.model.Model_Inv_Serial_Registration;
import org.guanzon.cas.inv.model.Model_Inventory;
import org.guanzon.cas.inv.services.InvModels;
import org.json.simple.JSONObject;

/**
 *
 * @author Arsiela 04-28-2025
 */
public class Model_POReturn_Detail extends Model{
    
    Number psReceiveQty = 1;
    
    //reference objects
    Model_Inventory poInventory;
    Model_Inv_Serial poInvSerial;
    Model_Inv_Serial_Registration poInvSerialRegistration;
    
    @Override
    public void initialize() {
        try {
            poEntity = MiscUtil.xml2ResultSet(System.getProperty("sys.default.path.metadata") + XML, getTable());
            
            poEntity.last();
            poEntity.moveToInsertRow();

            MiscUtil.initRowSet(poEntity);
            
            //assign default values
            poEntity.updateObject("dModified", SQLUtil.toDate("1900-01-01", SQLUtil.FORMAT_SHORT_DATE));
            poEntity.updateObject("nEntryNox", 0);
            poEntity.updateObject("nQuantity", 0.00);
            poEntity.updateObject("nUnitPrce", 0.0000);
            poEntity.updateObject("nFreightx", 0.00);
            //end - assign default values

            poEntity.insertRow();
            poEntity.moveToCurrentRow();
            poEntity.absolute(1);

            ID = "sTransNox";
            ID2 = "nEntryNox";
            
            //initialize reference objects
            InvModels invModel = new InvModels(poGRider); 
            poInventory = invModel.Inventory();
            poInvSerial = invModel.InventorySerial();
            poInvSerialRegistration = invModel.InventorySerialRegistration();
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
    
    public String getTransactionNo(){
        return (String) getValue("sTransNox");
    }
    
    public JSONObject setEntryNo(int entryNo){
        return setValue("nEntryNox", entryNo);
    }
    
    public int getEntryNo(){
        return (int) getValue("nEntryNox");
    }
    
    public JSONObject setStockId(String stockID){
        return setValue("sStockIDx", stockID);
    }
    
    public String getStockId(){
        return (String) getValue("sStockIDx");
    }
    
    public JSONObject setUnitType(String unitType){
        return setValue("cUnitType", unitType);
    }
    
    public String getUnitType(){
        return (String) getValue("cUnitType");
    }
    
    public JSONObject setSerialId(String serialId){
        return setValue("sSerialID", serialId);
    }
    
    public String getSerialId(){
        return (String) getValue("sSerialID");
    }
    
    public JSONObject setQuantity(Number quantity){
        return setValue("nQuantity", quantity);
    }
    
    public Number getQuantity(){
        return (Number) getValue("nQuantity");
    }
    
    public JSONObject setUnitPrce(Number unitPrce){
        return setValue("nUnitPrce", unitPrce);
    }
    
    public Number getUnitPrce(){
        return (Number) getValue("nUnitPrce");
    }
    
    public JSONObject setFreight(Number freight){
        return setValue("nFreightx", freight);
    }
    
    public Number getFreight(){
        return (Number) getValue("nFreightx");
    }
    
    public JSONObject setSourceNo(String sourceNo){
        return setValue("sSourceNo", sourceNo);
    }
    
    public String getSourceNo(){
        return (String) getValue("sSourceNo");
    }
    
    public JSONObject setBatchNo(String batchNo){
        return setValue("sBatchNox", batchNo);
    }
    
    public String getBatchNo(){
        return (String) getValue("sBatchNox");
    }
    
    public JSONObject setModifiedDate(Date modifiedDate){
        return setValue("dModified", modifiedDate);
    }
    
    public Date getModifiedDate(){
        return (Date) getValue("dModified");
    }
    
    @Override
    public String getNextCode() {
        return "";
    }
    
    //reference object models
    public Model_Inventory Inventory() throws SQLException, GuanzonException {
        if (!"".equals((String) getValue("sStockIDx"))) {
            if (poInventory.getEditMode() == EditMode.READY
                    && poInventory.getStockId().equals((String) getValue("sStockIDx"))) {
                return poInventory;
            } else {
                poJSON = poInventory.openRecord((String) getValue("sStockIDx"));

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
    
    public Model_Inv_Serial InventorySerial() throws SQLException, GuanzonException {
        if (!"".equals((String) getValue("sSerialID"))) {
            if (poInvSerial.getEditMode() == EditMode.READY
                    && poInvSerial.getSerialId().equals((String) getValue("sSerialID"))) {
                return poInvSerial;
            } else {
                poJSON = poInvSerial.openRecord((String) getValue("sSerialID"));

                if ("success".equals((String) poJSON.get("result"))) {
                    return poInvSerial;
                } else {
                    poInvSerial.initialize();
                    return poInvSerial;
                }
            }
        } else {
            poInvSerial.initialize();
            return poInvSerial;
        }
    }
    
    public Model_Inv_Serial_Registration InventorySerialRegistration() throws SQLException, GuanzonException {
        if (!"".equals((String) getValue("sSerialID"))) {
            if (poInvSerialRegistration.getEditMode() == EditMode.READY
                    && poInvSerialRegistration.getSerialId().equals((String) getValue("sSerialID"))) {
                return poInvSerialRegistration;
            } else {
                poJSON = poInvSerialRegistration.openRecord((String) getValue("sSerialID"));

                if ("success".equals((String) poJSON.get("result"))) {
                    return poInvSerialRegistration;
                } else {
                    poInvSerialRegistration.initialize();
                    return poInvSerialRegistration;
                }
            }
        } else {
            poInvSerialRegistration.initialize();
            return poInvSerialRegistration;
        }
    }
    
    //end reference object models
}
