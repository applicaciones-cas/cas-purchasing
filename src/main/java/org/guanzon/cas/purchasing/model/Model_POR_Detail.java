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
import org.guanzon.cas.parameter.model.Model_Branch;
import org.guanzon.cas.parameter.model.Model_Brand;
import org.guanzon.cas.parameter.model.Model_Category;
import org.guanzon.cas.parameter.model.Model_Color;
import org.guanzon.cas.parameter.model.Model_Company;
import org.guanzon.cas.parameter.model.Model_Industry;
import org.guanzon.cas.parameter.model.Model_Inv_Type;
import org.guanzon.cas.parameter.model.Model_Measure;
import org.guanzon.cas.parameter.model.Model_Model;
import org.guanzon.cas.parameter.model.Model_Term;
import org.guanzon.cas.parameter.model.Model_Variant;
import org.guanzon.cas.parameter.services.ParamModels;
import org.json.simple.JSONObject;

/**
 *
 * @author Arsiela 03-12-2025
 */
public class Model_POR_Detail extends Model{
    
    //reference objects
    Model_Branch poBranch;
    Model_Industry poIndustry;
    Model_Company poCompany;
    Model_Term poTerm;
    Model_Brand poBrand;
    Model_Model poModel;
    Model_Color poColor;
    Model_Category poCategory;
    Model_Inv_Type poInv_Type;
    Model_Measure poMeasure;
    Model_Variant poModelVariant;
    Model_Inv_Stock_Request_Master poInvStockMaster;
    Model_Inv_Stock_Request_Detail poInvStockDetail;
    Model_Inventory poInventory;
    Model_Inv_Master poInventoryMaster;
    
    String psBrandId = "";
    
    @Override
    public void initialize() {
        try {
            poEntity = MiscUtil.xml2ResultSet(System.getProperty("sys.default.path.metadata") + XML, getTable());
            
            poEntity.last();
            poEntity.moveToInsertRow();

            MiscUtil.initRowSet(poEntity);
            
            //assign default values
            poEntity.updateObject("dExpiryDt", SQLUtil.toDate("1900-01-01", SQLUtil.FORMAT_SHORT_DATE));
            poEntity.updateObject("nEntryNox", 0);
            poEntity.updateObject("nQuantity", 0);
            poEntity.updateObject("nWHCountx", 0);
            poEntity.updateObject("nOrderQty", 0);
            poEntity.updateObject("nUnitPrce", 0.00);
            poEntity.updateObject("nFreightx", 0.00);
            //end - assign default values

            poEntity.insertRow();
            poEntity.moveToCurrentRow();
            poEntity.absolute(1);

            ID = "sTransNox";
            ID2 = "nEntryNox";
            
            //initialize reference objects
            ParamModels model = new ParamModels(poGRider);
            poBranch = model.Branch();
            poIndustry = model.Industry();
            poCategory = model.Category();
            poCompany = model.Company();
            poTerm = model.Term();
            poBrand = model.Brand();
            poColor = model.Color();
            poModel = model.Model();
            poInv_Type = model.InventoryType();
            poMeasure = model.Measurement();
            poModelVariant = model.ModelVariant();
            
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
    
    //TODO
    public JSONObject setBrandId(String brandId){
        psBrandId = brandId;
        poJSON = new JSONObject();
        poJSON.put("result", "success");
        return poJSON;
    }
    
    public String getBrandId(){
        return psBrandId;
    }
    
    public JSONObject setModelVariantId(String modelVariantId){
        return poModelVariant.setVariantId(modelVariantId);
    }
    
    public String getModelVariantId(){
        return poModelVariant.getVariantId();
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
    
    public Model_Variant ModelVariant() throws GuanzonException, SQLException {
        if (!"".equals(getModelVariantId())) {
            if (poModelVariant.getEditMode() == EditMode.READY
                    && poModelVariant.getVariantId().equals(getModelVariantId())) {
                return poModelVariant;
            } else {
                poJSON = poModelVariant.openRecord(getModelVariantId());
                if ("success".equals((String) poJSON.get("result"))) {
                    return poModelVariant;
                } else {
                    poModelVariant.initialize();
                    return poModelVariant;
                }
            }
        } else {
            poModelVariant.initialize();
            return poModelVariant;
        }
    }
    
//    public Model_Inv_Master InventoryMaster() throws GuanzonException, SQLException {
//        if (!"".equals((String) getValue("sStockIDx"))) {
//            if (poInventoryMaster.getEditMode() == EditMode.READY
//                    && poInventoryMaster.getStockId().equals((String) getValue("sStockIDx"))) {
//                return poInventoryMaster;
//            } else {
//                poJSON = poInventoryMaster.openRecord((String) getValue("sStockIDx"));
//
//                if ("success".equals((String) poJSON.get("result"))) {
//                    return poInventoryMaster;
//                } else {
//                    poInventoryMaster.initialize();
//                    return poInventoryMaster;
//                }
//            }
//        } else {
//            poInventoryMaster.initialize();
//            return poInventoryMaster;
//        }
//    }
//
//    public Model_Color Color() throws GuanzonException, SQLException {
//        if (!"".equals(Inventory().getColorId())) {
//            if (poColor.getEditMode() == EditMode.READY
//                    && poColor.getColorId().equals(Inventory().getColorId())) {
//                return poColor;
//            } else {
//                poJSON = poColor.openRecord(Inventory().getColorId());
//
//                if ("success".equals((String) poJSON.get("result"))) {
//                    return poColor;
//                } else {
//                    poColor.initialize();
//                    return poColor;
//                }
//            }
//        } else {
//            poColor.initialize();
//            return poColor;
//        }
//    }
//
//    public Model_Inv_Type InventoryType() throws GuanzonException, SQLException {
//        if (!"".equals((String) getValue("sInvTypCd"))) {
//            if (poInv_Type.getEditMode() == EditMode.READY
//                    && poInv_Type.getInventoryTypeId().equals((String) getValue("sInvTypCd"))) {
//                return poInv_Type;
//            } else {
//                poJSON = poInv_Type.openRecord((String) getValue("sInvTypCd"));
//
//                if ("success".equals((String) poJSON.get("result"))) {
//                    return poInv_Type;
//                } else {
//                    poInv_Type.initialize();
//                    return poInv_Type;
//                }
//            }
//        } else {
//            poInv_Type.initialize();
//            return poInv_Type;
//        }
//    }
//
//    public Model_Measure Measure() throws GuanzonException, SQLException {
//        if (!"".equals(Inventory().getMeasurementId())) {
//            if (poMeasure.getEditMode() == EditMode.READY
//                    && poMeasure.getMeasureId().equals(Inventory().getMeasurementId())) {
//                return poMeasure;
//            } else {
//                poJSON = poMeasure.openRecord(Inventory().getMeasurementId());
//                if ("success".equals((String) poJSON.get("result"))) {
//                    return poMeasure;
//                } else {
//                    poMeasure.initialize();
//                    return poMeasure;
//                }
//            }
//        } else {
//            poMeasure.initialize();
//            return poMeasure;
//        }
//    }
//
//    public Model_Model Model() throws GuanzonException, SQLException {
//        if (!"".equals(Inventory().getModelId())) {
//            if (poModel.getEditMode() == EditMode.READY
//                    && poModel.getModelId().equals(Inventory().getModelId())) {
//                return poModel;
//            } else {
//                poJSON = poModel.openRecord(Inventory().getModelId());
//                if ("success".equals((String) poJSON.get("result"))) {
//                    return poModel;
//                } else {
//                    poModel.initialize();
//                    return poModel;
//                }
//            }
//        } else {
//            poModel.initialize();
//            return poModel;
//        }
//    }
    
    
    //end reference object models
}
