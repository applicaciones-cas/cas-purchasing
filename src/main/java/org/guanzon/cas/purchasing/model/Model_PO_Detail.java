package org.guanzon.cas.purchasing.model;

import java.sql.SQLException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.cas.inv.model.Model_Inv_Master;
import org.guanzon.cas.inv.model.Model_Inventory;
import org.guanzon.cas.inv.services.InvModels;
import org.guanzon.cas.parameter.model.Model_Branch;
import org.guanzon.cas.parameter.model.Model_Category;
import org.guanzon.cas.parameter.model.Model_Company;
import org.guanzon.cas.parameter.model.Model_Industry;
import org.guanzon.cas.parameter.model.Model_Term;
import org.guanzon.cas.parameter.services.ParamModels;
import org.json.simple.JSONObject;

public class Model_PO_Detail extends Model {

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
//            poEntity.updateObject("nEntryNox", 0);
//            poEntity.updateString("cTranStat", PurchaseOrderStatus.CONFIRMED);
//            poEntity.updateString("cTranStat", PurchaseOrderStatus.OPEN);
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

    public JSONObject setTransactionNo(String transactionNo) {
        return setValue("sTransNox", transactionNo);
    }

    public String getTransactionNo() {
        return (String) getValue("sTransNox");
    }

    public JSONObject setEntryNo(int entryNo) {
        return setValue("nEntryNox", entryNo);
    }

    public int getEntryNo() {
        return (int) getValue("nEntryNox");
    }

    public JSONObject setStockID(String stockID) {
        return setValue("sStockIDx", stockID);
    }

    public String getStockID() {
        return (String) getValue("sStockIDx");
    }

    public JSONObject setDescription(String description) {
        return setValue("sDescript", description);
    }

    public String getDescription() {
        return (String) getValue("sDescript");
    }

    public JSONObject setOldPrice(Double oldPrice) {
        return setValue("nOldPrice", oldPrice);
    }

    public Double getOldPrice() {
        return (Double) getValue("nOldPrice");
    }

    public JSONObject setUnitPrice(Double unitPrice) {
        return setValue("nUnitPrce", unitPrice);
    }

    public Double getUnitPrice() {
        return (Double) getValue("nUnitPrce");
    }

    public JSONObject setQuantityOnHand(int quantityOnHand) {
        return setValue("nQtyOnHnd", quantityOnHand);
    }

    public int getQuantityOnHand() {
        return (int) getValue("nQtyOnHnd");
    }

    public JSONObject setRecordOrder(int recordOrder) {
        return setValue("nRecOrder", recordOrder);
    }

    public int getRecordOrder() {
        return (int) getValue("nRecOrder");
    }

    public JSONObject setQuantity(Number quantity) {
        return setValue("nQuantity", quantity);
    }

    public Number getQuantity() {
        return (Number) getValue("nQuantity");
    }

    public JSONObject setReceivedQunatity(int receivedQuantity) {
        return setValue("nReceived", receivedQuantity);
    }

    public int getReceivedQunatity() {
        return (int) getValue("nReceived");
    }

    public JSONObject setCancelledQuantity(int cancelledQuantity) {
        return setValue("nCancelld", cancelledQuantity);
    }

    public int getCancelledQuantity() {
        return (int) getValue("nCancelld");
    }

    public JSONObject setSouceCode(String sourceCode) {
        return setValue("sSourceCd", sourceCode);
    }

    public String getSouceCode() {
        return (String) getValue("sSourceCd");
    }

    public JSONObject setSouceNo(String sourceNo) {
        return setValue("sSourceNo", sourceNo);
    }

    public String getSouceNo() {
        return (String) getValue("sSourceNo");
    }

    public JSONObject setModifiedDate(Date modifiedDate) {
        return setValue("dModified", modifiedDate);
    }

    public Date getModifiedDate() {
        return (Date) getValue("dModified");
    }

    //reference object models
    public Model_Branch Branch() {
        if (!"".equals((String) getValue("sBranchCd"))) {
            if (poBranch.getEditMode() == EditMode.READY
                    && poBranch.getBranchCode().equals((String) getValue("sBranchCd"))) {
                return poBranch;
            } else {
                try {
                    poJSON = poBranch.openRecord((String) getValue("sBranchCd"));
                    
                    if ("success".equals((String) poJSON.get("result"))) {
                        return poBranch;
                    } else {
                        poBranch.initialize();
                        return poBranch;
                    }
                } catch (SQLException | GuanzonException ex) {
                    Logger.getLogger(Model_PO_Detail.class.getName()).log(Level.SEVERE, null, ex);
                } 
            }
        } else {
            poBranch.initialize();
            return poBranch;
        }
            return poBranch;
    }

    public Model_Industry Industry() {
        if (!"".equals((String) getValue("sIndstCdx"))) {
            if (poIndustry.getEditMode() == EditMode.READY
                    && poIndustry.getIndustryId().equals((String) getValue("sIndstCdx"))) {
                return poIndustry;
            } else {
                try {
                    poJSON = poIndustry.openRecord((String) getValue("sIndstCdx"));
                    
                    if ("success".equals((String) poJSON.get("result"))) {
                        return poIndustry;
                    } else {
                        poIndustry.initialize();
                        return poIndustry;
                    }
                } catch (SQLException | GuanzonException ex) {
                    Logger.getLogger(Model_PO_Detail.class.getName()).log(Level.SEVERE, null, ex);
                } 
            }
        } else {
            poIndustry.initialize();
            return poIndustry;
        }
            return poIndustry;
    }

    public Model_Category Category() {
        if (!"".equals((String) getValue("sCategrCd"))) {
            if (poCategory.getEditMode() == EditMode.READY
                    && poCategory.getCategoryId().equals((String) getValue("sCategrCd"))) {
                return poCategory;
            } else {
                try {
                    poJSON = poCategory.openRecord((String) getValue("sCategrCd"));
                    
                    if ("success".equals((String) poJSON.get("result"))) {
                        return poCategory;
                    } else {
                        poCategory.initialize();
                        return poCategory;
                    }
                } catch (SQLException | GuanzonException ex) {
                    Logger.getLogger(Model_PO_Detail.class.getName()).log(Level.SEVERE, null, ex);
                } 
            }
        } else {
            poCategory.initialize();
            return poCategory;
        }
            return poCategory;
    }

    public Model_Company Company() {
        if (!"".equals((String) getValue("sCompanyID"))) {
            if (poCompany.getEditMode() == EditMode.READY
                    && poCompany.getCompanyId().equals((String) getValue("sCompanyID"))) {
                return poCompany;
            } else {
                try {
                    poJSON = poCompany.openRecord((String) getValue("sCategrCd"));
                    
                    if ("success".equals((String) poJSON.get("result"))) {
                        return poCompany;
                    } else {
                        poCompany.initialize();
                        return poCompany;
                    }
                } catch (SQLException | GuanzonException ex) {
                    Logger.getLogger(Model_PO_Detail.class.getName()).log(Level.SEVERE, null, ex);
                } 
            }
        } else {
            poCompany.initialize();
            return poCompany;
        }
            return poCompany;
    }

    public Model_Term Term() {
        if (!"".equals((String) getValue("sTermCode"))) {
            if (poTerm.getEditMode() == EditMode.READY
                    && poTerm.getTermCode().equals((String) getValue("sTermCode"))) {
                return poTerm;
            } else {
                try {
                    poJSON = poTerm.openRecord((String) getValue("sTermCode"));
                    
                    if ("success".equals((String) poJSON.get("result"))) {
                        return poTerm;
                    } else {
                        poTerm.initialize();
                        return poTerm;
                    }
                } catch (SQLException | GuanzonException ex) {
                    Logger.getLogger(Model_PO_Detail.class.getName()).log(Level.SEVERE, null, ex);
                } 
            }
        } else {
            poTerm.initialize();
            return poTerm;
        }
            return poTerm;
    }

    public Model_Inventory Inventory() {
        if (!"".equals((String) getValue("sStockIDx"))) {
            if (poInventory.getEditMode() == EditMode.READY
                    && poInventory.getStockId().equals((String) getValue("sStockIDx"))) {
                return poInventory;
            } else {
                try {
                    poJSON = poInventory.openRecord((String) getValue("sStockIDx"));
                    
                    if ("success".equals((String) poJSON.get("result"))) {
                        return poInventory;
                    } else {
                        poInventory.initialize();
                        return poInventory;
                    }
                } catch (SQLException | GuanzonException ex) {
                    Logger.getLogger(Model_PO_Detail.class.getName()).log(Level.SEVERE, null, ex);
                } 
            }
        } else {
            poInventory.initialize();
            return poInventory;
        }
            return poInventory;
    }

    public Model_Inv_Master InventoryMaster() {
        if (!"".equals((String) getValue("sStockIDx"))) {
            if (poInventoryMaster.getEditMode() == EditMode.READY
                    && poInventoryMaster.getStockId().equals((String) getValue("sStockIDx"))) {
                return poInventoryMaster;
            } else {
                poJSON = poInventoryMaster.openRecord((String) getValue("sStockIDx"));

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
