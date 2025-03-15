package org.guanzon.cas.purchasing.model;

import java.sql.SQLException;
import java.util.Date;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.cas.inv.model.Model_Inv_Master;
import org.guanzon.cas.inv.model.Model_Inventory;
import org.guanzon.cas.inv.services.InvModels;
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
import org.guanzon.cas.parameter.services.ParamModels;
import org.json.simple.JSONObject;

public class Model_PO_Detail extends Model {

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
            poEntity.updateObject("nUnitPrce", 0.00);
            poEntity.updateObject("nOldPrice", 0.00);
            poEntity.updateObject("nQtyOnHnd", 0);
            poEntity.updateObject("nRecOrder", 0);
            poEntity.updateObject("nQuantity", 0);
            poEntity.updateObject("nReceived", 0);
            poEntity.updateObject("nCancelld", 0);

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

    public JSONObject setOldPrice(double oldPrice) {
        return setValue("nOldPrice", oldPrice);
    }

    public Double getOldPrice() {
        return (Double) getValue("nOldPrice");
    }

    public JSONObject setUnitPrice(double unitPrice) {
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
    public Model_Branch Branch() throws GuanzonException, SQLException {
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

    public Model_Industry Industry() throws GuanzonException, SQLException {
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

    public Model_Category Category() throws GuanzonException, SQLException {
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

    public Model_Company Company() throws GuanzonException, SQLException {
        if (!"".equals((String) getValue("sCompanyID"))) {
            if (poCompany.getEditMode() == EditMode.READY
                    && poCompany.getCompanyId().equals((String) getValue("sCompanyID"))) {
                return poCompany;
            } else {

                poJSON = poCompany.openRecord((String) getValue("sCompanyID"));
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

    public Model_Term Term() throws GuanzonException, SQLException {
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

    public Model_Inventory Inventory() throws GuanzonException, SQLException {
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

    public Model_Inv_Master InventoryMaster() throws GuanzonException, SQLException {
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

    public Model_Brand Brand() throws GuanzonException, SQLException {
        if (!"".equals((String) getValue("sBrandIDx"))) {
            if (poBrand.getEditMode() == EditMode.READY
                    && poBrand.getBrandId().equals((String) getValue("sBrandIDx"))) {
                return poBrand;
            } else {
                poJSON = poBrand.openRecord((String) getValue("sBrandIDx"));

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

    public Model_Color Color() throws GuanzonException, SQLException {
        if (!"".equals((String) getValue("sColorIDx"))) {
            if (poColor.getEditMode() == EditMode.READY
                    && poColor.getColorId().equals((String) getValue("sColorIDx"))) {
                return poColor;
            } else {
                poJSON = poColor.openRecord((String) getValue("sColorIDx"));

                if ("success".equals((String) poJSON.get("result"))) {
                    return poColor;
                } else {
                    poColor.initialize();
                    return poColor;
                }
            }
        } else {
            poColor.initialize();
            return poColor;
        }
    }

    public Model_Inv_Type InventoryType() throws GuanzonException, SQLException {
        if (!"".equals((String) getValue("sInvTypCd"))) {
            if (poInv_Type.getEditMode() == EditMode.READY
                    && poInv_Type.getInventoryTypeId().equals((String) getValue("sInvTypCd"))) {
                return poInv_Type;
            } else {
                poJSON = poInv_Type.openRecord((String) getValue("sInvTypCd"));

                if ("success".equals((String) poJSON.get("result"))) {
                    return poInv_Type;
                } else {
                    poInv_Type.initialize();
                    return poInv_Type;
                }
            }
        } else {
            poInv_Type.initialize();
            return poInv_Type;
        }
    }

    public Model_Measure Measure() throws GuanzonException, SQLException {
        if (!"".equals((String) getValue("sMeasurID"))) {
            if (poMeasure.getEditMode() == EditMode.READY
                    && poMeasure.getMeasureId().equals((String) getValue("sMeasurID"))) {
                return poMeasure;
            } else {
                poJSON = poMeasure.openRecord((String) getValue("sMeasurID"));
                if ("success".equals((String) poJSON.get("result"))) {
                    return poMeasure;
                } else {
                    poMeasure.initialize();
                    return poMeasure;
                }
            }
        } else {
            poMeasure.initialize();
            return poMeasure;
        }
    }

    public Model_Model Model() throws GuanzonException, SQLException {
        if (!"".equals((String) getValue("sModelIDx"))) {
            if (poModel.getEditMode() == EditMode.READY
                    && poModel.getModelId().equals((String) getValue("sModelIDx"))) {
                return poModel;
            } else {
                poJSON = poModel.openRecord((String) getValue("sModelIDx"));
                if ("success".equals((String) poJSON.get("result"))) {
                    return poModel;
                } else {
                    poModel.initialize();
                    return poModel;
                }
            }
        } else {
            poModel.initialize();
            return poModel;
        }
    }
    //end - reference object models
}
