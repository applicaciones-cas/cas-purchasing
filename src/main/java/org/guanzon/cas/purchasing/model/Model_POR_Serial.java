/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.guanzon.cas.purchasing.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.cas.inv.model.Model_Inv_Serial;
import org.guanzon.cas.inv.model.Model_Inv_Serial_Ledger;
import org.guanzon.cas.inv.model.Model_Inv_Serial_Registration;
import org.guanzon.cas.inv.model.Model_Inventory;
import org.guanzon.cas.inv.services.InvModels;
import org.guanzon.cas.parameter.model.Model_Inv_Location;
import org.guanzon.cas.parameter.services.ParamModels;
import org.json.simple.JSONObject;

/**
 *
 * @author Arsiela
 */
public class Model_POR_Serial extends Model {
    
    //reference objects  
    Model_Inventory poInventory;  
    Model_Inv_Location poLocation;  
    Model_Inv_Serial poInvSerial; 
    Model_Inv_Serial_Registration poInvSerialRegistration; 
    Model_Inv_Serial_Ledger poInvSerialLedger;    
    
    @Override
    public void initialize() {
        try {
            poEntity = MiscUtil.xml2ResultSet(System.getProperty("sys.default.path.metadata") + XML, getTable());
            
            poEntity.last();
            poEntity.moveToInsertRow();

            MiscUtil.initRowSet(poEntity);
            
            //assign default values
            poEntity.updateObject("nEntryNox", 0);
            //end - assign default values

            poEntity.insertRow();
            poEntity.moveToCurrentRow();
            poEntity.absolute(1);

            ID = "sTransNox";
            ID2 = "nEntryNox";
            ID3 = "sSerialID";
            
            //initialize reference objects
            ParamModels location = new ParamModels(poGRider);
            poLocation = location.InventoryLocation();
            
            InvModels invSerial = new InvModels(poGRider); 
            poInvSerial = invSerial.InventorySerial();
            poInvSerialRegistration = invSerial.InventorySerialRegistration();
            
            InvModels invModel = new InvModels(poGRider); 
            poInventory = invModel.Inventory();
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
    
    public JSONObject setStockId(String stockId){
        return setValue("sStockIDx", stockId);
    }
    
    public String getStockId(){
        return (String) getValue("sStockIDx");
    }
    
    public JSONObject setSerialId(String serialId){
        return setValue("sSerialID", serialId);
    }
    
    public String getSerialId(){
        return (String) getValue("sSerialID");
    }
    
    public JSONObject setLocationID(String locationId){
        return setValue("sLocatnID", locationId);
    }
    
    public String getLocationID(){
        return (String) getValue("sLocatnID");
    }
    
    public JSONObject setModifiedDate(Date modifiedDate){
        return setValue("dModified", modifiedDate);
    }
    
    public Date getModifiedDate(){
        return (Date) getValue("dModified");
    }
    
    public JSONObject setSerial01(String serialNumber) {
        return poInvSerial.setSerial01(serialNumber);
    }

    public String getSerial01() {
        return poInvSerial.getSerial01();
    }

    public JSONObject setSerial02(String serialNumber) {
        return poInvSerial.setSerial02(serialNumber);
    }

    public String getSerial02() {
        return poInvSerial.getSerial02();
    }

    public JSONObject setConductionStickerNo(String conductionStickerNo) {
        return poInvSerialRegistration.setConductionStickerNo(conductionStickerNo);
    }

    public String getConductionStickerNo() {
        return poInvSerialRegistration.getConductionStickerNo();
    }

    public JSONObject setPlateNo(String plateNo) {
        return poInvSerialRegistration.setPlateNoP(plateNo);
    }

    public String getPlateNo() {
        return poInvSerialRegistration.getPlateNoP();
    }
    
    @Override
    public String getNextCode() {
        return "";
//        return MiscUtil.getNextCode(this.getTable(), ID, true, poGRider.getGConnection().getConnection(), poGRider.getBranchCode());
    }
    
//    public JSONObject openRecord(String transactionNo, int entryNo, String serialId) throws SQLException, GuanzonException {
//        poJSON = new JSONObject();
//        String lsSQL = MiscUtil.makeSelect(this);
//        lsSQL = MiscUtil.addCondition(lsSQL, " sTransNox = " + SQLUtil.toSQL(transactionNo) 
//                                    + " AND nEntryNox = " + SQLUtil.toSQL(entryNo) 
//                                    + " AND sSerialID = " + SQLUtil.toSQL(serialId));
//        ResultSet loRS = poGRider.executeQuery(lsSQL);
//        try {
//            if (loRS.next()) {
//                for (int lnCtr = 1; lnCtr <= loRS.getMetaData().getColumnCount(); lnCtr++){
//                  setValue(lnCtr, loRS.getObject(lnCtr)); 
//                }
//                MiscUtil.close(loRS);
//                pnEditMode = 1;
//                poJSON = new JSONObject();
//                poJSON.put("result", "success");
//                poJSON.put("message", "Record loaded successfully.");
//            } else {
//                poJSON = new JSONObject();
//                poJSON.put("result", "error");
//                poJSON.put("message", "No record to load.");
//            } 
//        } catch (SQLException e) {
//          poJSON = new JSONObject();
//          poJSON.put("result", "error");
//          poJSON.put("message", e.getMessage());
//        } 
//        return this.poJSON;
//    }
    
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
    
    public Model_Inv_Location Location() throws SQLException, GuanzonException { 
        if (!"".equals((String) getValue("sLocatnID"))) {
            if (poLocation.getEditMode() == EditMode.READY
                    && poLocation.getLocationId().equals((String) getValue("sLocatnID"))) {
                return poLocation;
            } else {
                poJSON = poLocation.openRecord((String) getValue("sLocatnID"));

                if ("success".equals((String) poJSON.get("result"))) {
                    return poLocation;
                } else {
                    poLocation.initialize();
                    return poLocation;
                }
            }
        } else {
            poLocation.initialize();
            return poLocation;
        }
    }
    
    public Model_Inv_Serial InventorySerial() throws SQLException, GuanzonException {
        if (!"".equals((String) getValue("sSerialID"))) {
            if (poInvSerial.getEditMode() == EditMode.READY
                    && poInvSerial.getStockId().equals((String) getValue("sSerialID"))) {
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
    
    public Model_Inv_Serial_Ledger InventorySerialLedger() throws SQLException, GuanzonException {
        if (!"".equals((String) getValue("sSerialID"))) {
            if (poInvSerialLedger.getEditMode() == EditMode.READY
                    && poInvSerialLedger.getSerialId().equals((String) getValue("sSerialID"))) {
                return poInvSerialLedger;
            } else {
                poJSON = poInvSerialLedger.openRecord((String) getValue("sSerialID"));

                if ("success".equals((String) poJSON.get("result"))) {
                    return poInvSerialLedger;
                } else {
                    poInvSerialLedger.initialize();
                    return poInvSerialLedger;
                }
            }
        } else {
            poInvSerialLedger.initialize();
            return poInvSerialLedger;
        }
    }
    
    //end reference object models
}
