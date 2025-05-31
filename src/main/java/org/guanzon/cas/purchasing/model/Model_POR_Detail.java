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
import org.guanzon.cas.inv.model.Model_Inv_Master;
import org.guanzon.cas.inv.model.Model_Inventory;
import org.guanzon.cas.inv.services.InvModels;
import org.guanzon.cas.inv.warehouse.model.Model_Inv_Stock_Request_Detail;
import org.guanzon.cas.inv.warehouse.model.Model_Inv_Stock_Request_Master;
import org.guanzon.cas.parameter.model.Model_Brand;
import org.guanzon.cas.parameter.services.ParamModels;
import org.guanzon.cas.purchasing.services.PurchaseOrderModels;
import org.json.simple.JSONObject;

/**
 *
 * @author Arsiela 03-12-2025
 */
public class Model_POR_Detail extends Model{
    
    String psBrandId = "";
    
    //reference objects
    Model_Brand poBrand;
    Model_Inv_Stock_Request_Master poInvStockMaster;
    Model_Inv_Stock_Request_Detail poInvStockDetail;
    Model_Inventory poInventory;
    Model_Inv_Master poInventoryMaster;
    Model_PO_Master poPurchaseOrder;
    
    @Override
    public void initialize() {
        try {
            poEntity = MiscUtil.xml2ResultSet(System.getProperty("sys.default.path.metadata") + XML, getTable());
            
            poEntity.last();
            poEntity.moveToInsertRow();

            MiscUtil.initRowSet(poEntity);
            
            //assign default values
            poEntity.updateObject("dExpiryDt", SQLUtil.toDate("1900-01-01", SQLUtil.FORMAT_SHORT_DATE));
            poEntity.updateObject("dModified", SQLUtil.toDate("1900-01-01", SQLUtil.FORMAT_SHORT_DATE));
            poEntity.updateObject("cSerialze", "0");
            poEntity.updateObject("nEntryNox", 0);
            poEntity.updateObject("nWHCountx", 0);
            poEntity.updateObject("nQuantity", 0.00);
            poEntity.updateObject("nOrderQty", 0.00);
            poEntity.updateObject("nUnitPrce", 0.00);
            poEntity.updateObject("nFreightx", 0.00);
//            poEntity.updateObject("nDiscount", 0.00);
//            poEntity.updateObject("nAddDiscx", 0.00);
//            poEntity.updateObject("nTranTotl", 0.00);
            //end - assign default values

            poEntity.insertRow();
            poEntity.moveToCurrentRow();
            poEntity.absolute(1);

            ID = "sTransNox";
            ID2 = "nEntryNox";
            
            //initialize reference objects
            ParamModels model = new ParamModels(poGRider);
            poBrand = model.Brand();
            
            InvModels invModel = new InvModels(poGRider); 
            poInventory = invModel.Inventory();
            
            Model_PO_Master purchaseOrderModel = new PurchaseOrderModels(poGRider).PurchaseOrderMaster(); 
            poPurchaseOrder = purchaseOrderModel;
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
    
    public JSONObject setStockId(String stockID){
        return setValue("sStockIDx", stockID);
    }
    
    public String getStockId(){
        return (String) getValue("sStockIDx");
    }
    
    public JSONObject setReplaceId(String replaceID){
        return setValue("sReplacID", replaceID);
    }
    
    public String getReplaceId(){
        return (String) getValue("sReplacID");
    }
    
    public JSONObject setUnitType(String unitType){
        return setValue("cUnitType", unitType);
    }
    
    public String getUnitType(){
        return (String) getValue("cUnitType");
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
    
    public JSONObject setDiscount(Number discount){
        return setValue("nDiscount", discount);
    }
    
    public Number getDiscount(){
        return (Number) getValue("nDiscount");
    }
    
    public JSONObject setAdditionalDiscount(Number additionalDiscount){
        return setValue("nAddDiscx", additionalDiscount);
    }
    
    public Number getAdditionalDiscount(){
        return (Number) getValue("nAddDiscx");
    }
    
    public JSONObject setTotal(Number total){
        return setValue("nTranTotl", total);
    }
    
    public Number getTotal(){
        return (Number) getValue("nTranTotl");
    }
    
    public JSONObject setExpiryDate(Date expiryDate){
        return setValue("dExpiryDt", expiryDate);
    }
    
    public Date getExpiryDate(){
        return (Date) getValue("dExpiryDt");
    }
    
    public JSONObject setWhCount(Number whCount){
        return setValue("nWHCountx", whCount);
    }
    
    public Number getWhCount(){
        return (Number) getValue("nWHCountx");
    }
    
    public JSONObject setOrderQty(Number orderQty){
        return setValue("nOrderQty", orderQty);
    }
    
    public Number getOrderQty(){
        return (Number) getValue("nOrderQty");
    }
    
//    public JSONObject setWithVat(String withVat){
//        return setValue("cWithVATx", withVat);
//    }
//    
//    public String getWithVat(){
//        return (String) getValue("cWithVATx");
//    }
    
    public JSONObject isVatable(boolean isVatable){
        return setValue("cWithVATx", isVatable ? "1" : "0");
    } 
    
    public boolean isVatable(){
        return ((String) getValue("cWithVATx")).equals("1");
    }
    
    public JSONObject isSerialized(boolean isSerialize){
        return setValue("cSerialze", isSerialize ? "1" : "0");
    } 
    
    public boolean isSerialized(){
        return ((String) getValue("cSerialze")).equals("1");
    }
    
    public JSONObject setModifiedDate(Date modifiedDate){
        return setValue("dModified", modifiedDate);
    }
    
    public Date getModifiedDate(){
        return (Date) getValue("dModified");
    }
    
    public void setBrandId(String brandId){
        psBrandId = brandId;
    }
    
    public String getBrandId(){
        return psBrandId;
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
    
    public Model_Inventory Supersede() throws SQLException, GuanzonException {
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
    
    public Model_Brand Brand() throws GuanzonException, SQLException {
        if (!"".equals((String) getValue("sStockIDx")) && (String) getValue("sStockIDx") != null) {
            setBrandId(Inventory().getBrandId());
        }
        
        if (!"".equals(getBrandId())) {
            if (poBrand.getEditMode() == EditMode.READY
                    && poBrand.getBrandId().equals(getBrandId())) {
                return poBrand;
            } else {
                poJSON = poBrand.openRecord(getBrandId());
                if ("success".equals((String) poJSON.get("result"))) {
                    return poBrand;
                } else {
                    poBrand.initialize();
                    return poBrand;
                }
            }
        } else {
            poBrand.initialize();
            return poBrand;
        }
    }
    
    public Model_PO_Master PurchaseOrderMaster() throws SQLException, GuanzonException {
            if (!"".equals((String) getValue("sOrderNox"))) {
                if (poPurchaseOrder.getEditMode() == EditMode.READY
                        && poPurchaseOrder.getTransactionNo().equals((String) getValue("sOrderNox"))) {
                    return poPurchaseOrder;
                } else {
                    poJSON = poPurchaseOrder.openRecord((String) getValue("sOrderNox"));

                    if ("success".equals((String) poJSON.get("result"))) {
                        return poPurchaseOrder;
                    } else {
                        poPurchaseOrder.initialize();
                        return poPurchaseOrder;
                    }
                }
            } else {
                poPurchaseOrder.initialize();
                return poPurchaseOrder;
            }
    }
    
    public JSONObject openRecord(String transactionNo, String stockId) throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        String lsSQL = MiscUtil.makeSelect(this);
        lsSQL = MiscUtil.addCondition(lsSQL, " sTransNox = " + SQLUtil.toSQL(transactionNo) 
                                        + " AND sStockIDx = " + SQLUtil.toSQL(stockId));
        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        try {
            if (loRS.next()) {
                for (int lnCtr = 1; lnCtr <= loRS.getMetaData().getColumnCount(); lnCtr++){
                    setValue(lnCtr, loRS.getObject(lnCtr)); 
                }
                MiscUtil.close(loRS);
                pnEditMode = EditMode.READY;
                poJSON = new JSONObject();
                poJSON.put("result", "success");
                poJSON.put("message", "Record loaded successfully.");
            } else {
                poJSON = new JSONObject();
                poJSON.put("result", "error");
                poJSON.put("message", "No record to load.");
            } 
        } catch (SQLException e) {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", e.getMessage());
        } 
        return poJSON;
    }
    //end reference object models
}
