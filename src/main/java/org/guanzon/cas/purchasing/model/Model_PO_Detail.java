package org.guanzon.cas.purchasing.model;

import java.sql.SQLException;
import java.util.Date;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.constant.Logical;
import org.guanzon.cas.inv.model.Model_Inv_Master;
import org.guanzon.cas.inv.model.Model_Inventory;
import org.guanzon.cas.inv.services.InvModels;
import org.guanzon.cas.inv.warehouse.status.StockRequestStatus;
import org.guanzon.cas.parameter.model.Model_Branch;
import org.guanzon.cas.parameter.model.Model_Category;
import org.guanzon.cas.parameter.model.Model_Company;
import org.guanzon.cas.parameter.model.Model_Industry;
import org.guanzon.cas.parameter.model.Model_Term;
import org.guanzon.cas.parameter.services.ParamModels;
import org.json.simple.JSONObject;

public class Model_PO_Detail extends Model{      
    //reference objects
    Model_Branch poBranch;
    Model_Industry poIndustry;
    Model_Category poCategory;
    Model_Company poCompany;    
    Model_Term poTerm;
    
    Model_Inventory poInventory;    
    Model_Inv_Master poInventoryMaster;
    
    
    @Override
    public void initialize() {
        try {
            poEntity = MiscUtil.xml2ResultSet(System.getProperty("sys.default.path.metadata") + XML, getTable());
            
            poEntity.last();
            poEntity.moveToInsertRow();

            MiscUtil.initRowSet(poEntity);
            
            //assign default values
            poEntity.updateObject("dTransact", "0000-00-00");
            poEntity.updateObject("dApproved", "0000-00-00");
            poEntity.updateObject("nCurrInvx", 0);
            poEntity.updateObject("nEstInvxx", 0);
            poEntity.updateObject("nEntryNox", 0);
            poEntity.updateString("cConfirmd", Logical.NO);
            poEntity.updateString("cTranStat", StockRequestStatus.OPEN);
            //end - assign default values

            poEntity.insertRow();
            poEntity.moveToCurrentRow();

            poEntity.absolute(1);

            ID = "sTransNox";
            
            //initialize reference objects
            ParamModels model = new ParamModels(poGRider);
            poBranch = model.Branch();
            poIndustry = model.Industry();
            poCategory = model.Category();
            poCompany = model.Company();
            poTerm = model.Term();
            
            InvModels invModel = new InvModels(poGRider); 
            poInventory = invModel.Inventory();
            poInventoryMaster = invModel.InventoryMaster();
            
            
           
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
    
    public JSONObject setStockID(String stockID){
        return setValue("sStockIDx", stockID);
    }
    
    public String getStockID(){
        return (String) getValue("sStockIDx");
    }
    
    public JSONObject setDescription(String description){
        return setValue("sDescript", description);
    }
    
    public String getDescription(){
        return (String) getValue("sDescript");
    }
    
    public JSONObject setOriginalCost(Number originalCost){
        return setValue("nOrigCost", originalCost);
    }
    
    public Number getOriginalCost(){
        return (Number) getValue("nOrigCost");
    }
    
    public JSONObject setQuantityOnHand(int quantityOnHand){
        return setValue("nQtyOnHnd", quantityOnHand);
    }
    
    public int getQuantityOnHand(){
        return (int) getValue("nQtyOnHnd");
    }
    
    public JSONObject setRecordOrder(int recordOrder){
        return setValue("nRecOrder", recordOrder);
    }
    
    public int getRecordOrder(){
        return (int) getValue("nRecOrder");
    }
    
    public JSONObject setUnitPrice(Number unitPrice){
        return setValue("nUnitPrce", unitPrice);
    }
    
    public Number getUnitPrice(){
        return (Number) getValue("nUnitPrce");
    }
    
    public JSONObject setReceivedQunatity(int receivedQuantity){
        return setValue("nReceived", receivedQuantity);
    }
    
    public int getReceivedQunatity(){
        return (int) getValue("nReceived");
    }
    
    public JSONObject setCancelledQuantity(int cancelledQuantity){
        return setValue("nCancelld", cancelledQuantity);
    }
    
    public int getCancelledQuantity(){
        return (int) getValue("nCancelld");
    }
    
    public JSONObject setModifiedDate(Date modifiedDate){
        return setValue("dModified", modifiedDate);
    }
    
    public Date getModifiedDate(){
        return (Date) getValue("dModified");
    }
    
    
    //reference object models
    public Model_Branch Branch() {
        if (!"".equals((String) getValue("sBranchCd"))) {
            if (poBranch.getEditMode() == EditMode.READY
                    && poBranch.getBranchCode().equals((String) getValue("sBranchCd"))) {
                return poBranch;
            } else {
                poJSON = poBranch.openRecord((String) getValue("sBranchCd"));

                if ("success".equals((String) poJSON.get("result"))) {
                    return poBranch;
                } else {
                    poBranch.initialize();
                    return poBranch;
                }
            }
        } else {
            poBranch.initialize();
            return poBranch;
        }
    }
    
    public Model_Industry Industry() {
        if (!"".equals((String) getValue("sIndstCdx"))) {
            if (poIndustry.getEditMode() == EditMode.READY
                    && poIndustry.getIndustryId().equals((String) getValue("sIndstCdx"))) {
                return poIndustry;
            } else {
                poJSON = poIndustry.openRecord((String) getValue("sIndstCdx"));

                if ("success".equals((String) poJSON.get("result"))) {
                    return poIndustry;
                } else {
                    poIndustry.initialize();
                    return poIndustry;
                }
            }
        } else {
            poIndustry.initialize();
            return poIndustry;
        }
    }
    
    public Model_Category Category() {
        if (!"".equals((String) getValue("sCategrCd"))) {
            if (poCategory.getEditMode() == EditMode.READY
                    && poCategory.getCategoryId().equals((String) getValue("sCategrCd"))) {
                return poCategory;
            } else {
                poJSON = poCategory.openRecord((String) getValue("sCategrCd"));

                if ("success".equals((String) poJSON.get("result"))) {
                    return poCategory;
                } else {
                    poCategory.initialize();
                    return poCategory;
                }
            }
        } else {
            poCategory.initialize();
            return poCategory;
        }
    }
    
    public Model_Company Company() {
        if (!"".equals((String) getValue("sCompanyID"))) {
            if (poCompany.getEditMode() == EditMode.READY
                    && poCompany.getCompanyId().equals((String) getValue("sCompanyID"))) {
                return poCompany;
            } else {
                poJSON = poCompany.openRecord((String) getValue("sCategrCd"));

                if ("success".equals((String) poJSON.get("result"))) {
                    return poCompany;
                } else {
                    poCompany.initialize();
                    return poCompany;
                }
            }
        } else {
            poCompany.initialize();
            return poCompany;
        }
    }
    
    public Model_Term Term() {
        if (!"".equals((String) getValue("sTermCode"))) {
            if (poTerm.getEditMode() == EditMode.READY
                    && poTerm.getTermCode().equals((String) getValue("sTermCode"))) {
                return poTerm;
            } else {
                poJSON = poTerm.openRecord((String) getValue("sTermCode"));

                if ("success".equals((String) poJSON.get("result"))) {
                    return poTerm;
                } else {
                    poTerm.initialize();
                    return poTerm;
                }
            }
        } else {
            poTerm.initialize();
            return poTerm;
        }
    }
    
    
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
    
    public Model_Inv_Master InventoryMaster() {
        if (!"".equals((String) getValue("sStockID"))) {
            if (poInventoryMaster.getEditMode() == EditMode.READY
                    && poInventoryMaster.getStockId().equals((String) getValue("sStockID"))) {
                return poInventoryMaster;
            } else {
                poJSON = poInventoryMaster.openRecord((String) getValue("sStockID"));

                if ("success".equals((String) poJSON.get("result"))) {
                    return poInventoryMaster;
                } else {
                    poInventoryMaster.initialize();
                    return poInventoryMaster;
                }
            }
        } else {
            poInventoryMaster.initialize();
            return poInventoryMaster;
        }
    }
    
    
    //end - reference object models
}