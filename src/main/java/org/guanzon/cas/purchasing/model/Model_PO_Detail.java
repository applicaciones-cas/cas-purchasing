package org.guanzon.cas.purchasing.model;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
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
import org.guanzon.cas.inv.warehouse.services.InvWarehouseModels;
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
    Model_Inv_Stock_Request_Master poInvStockMaster;
    Model_Inv_Stock_Request_Detail poInvStockDetail;
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
            poEntity.updateObject("nEntryNox", 0);
            poEntity.updateObject("nCancelld", 0);
            poEntity.updateObject("nUnitPrce", 0.0);
            poEntity.updateObject("nOldPrice", 0.0);
            poEntity.updateObject("nQtyOnHnd", 0);
            poEntity.updateObject("nRecOrder", 0);
            poEntity.updateObject("nQuantity", 0);
            poEntity.updateObject("nReceived", 0);            
            poEntity.updateObject("cSerialze", false);
            poEntity.updateObject("dModified", SQLUtil.toDate(xsDateShort(poGRider.getServerDate()), SQLUtil.FORMAT_SHORT_DATE));

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

            InvWarehouseModels invWarehouseModel = new InvWarehouseModels(poGRider);
            poInvStockMaster = invWarehouseModel.InventoryStockRequestMaster();
            poInvStockDetail = invWarehouseModel.InventoryStockRequestDetail();
            //end - initialize reference objects
            pnEditMode = EditMode.UNKNOWN;
        } catch (SQLException e) {
            logwrapr.severe(e.getMessage());
            System.exit(1);
        }
    }

    private static String xsDateShort(Date fdValue) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String date = sdf.format(fdValue);
        return date;
    }

    @Override
    public String getNextCode() {
        return "";
    }

    public JSONObject setTransactionNo(String transactionNo) {
        return setValue("sTransNox", transactionNo);
    }

    public String getTransactionNo() {
        return (String) getValue("sTransNox");
    }

    public JSONObject setEntryNo(Number entryNo) {
        return setValue("nEntryNox", entryNo);
    }

    public Number getEntryNo() {
        return (Number) getValue("nEntryNox");
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

    public JSONObject setOldPrice(Number oldPrice) {
        return setValue("nOldPrice", oldPrice);
    }

    public Number getOldPrice() {
        return (Number) getValue("nOldPrice");
    }

    public JSONObject setUnitPrice(Number unitPrice) {
        return setValue("nUnitPrce", unitPrice);
    }

    public Number getUnitPrice() {
        return (Number) getValue("nUnitPrce");
    }

    public JSONObject setQuantityOnHand(Number quantityOnHand) {
        return setValue("nQtyOnHnd", quantityOnHand);
    }

    public Number getQuantityOnHand() {
        return (Number) getValue("nQtyOnHnd");
    }

    public JSONObject setRecordOrder(Number recordOrder) {
        return setValue("nRecOrder", recordOrder);
    }

    public Number getRecordOrder() {
        return (Number) getValue("nRecOrder");
    }

    public JSONObject setQuantity(Number quantity) {
        return setValue("nQuantity", quantity);
    }

    public Number getQuantity() {
        return (Number) getValue("nQuantity");
    }

    public JSONObject setReceivedQuantity(Number receivedQuantity) {
        return setValue("nReceived", receivedQuantity);
    }

    public Number getReceivedQuantity() {
        return (Number) getValue("nReceived");
    }

    public JSONObject setCancelledQuantity(Number cancelledQuantity) {
        return setValue("nCancelld", cancelledQuantity);
    }

    public Number getCancelledQuantity() {
        return (Number) getValue("nCancelld");
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

    public JSONObject setCategoryCode(String categoryCode) {
        return setValue("sCategrCd", categoryCode);
    }

    public String getCategoryCode() {
        return (String) getValue("sCategrCd");
    }
    
    public JSONObject isSerialized(boolean isSerialized) {
      return setValue("cSerialze", isSerialized ? "1" : "0");
    }

    public boolean isSerialized() {
      return ((String)getValue("cSerialze")).equals("1");
    }
    
    public JSONObject setModifiedDate(Date modifiedDate) {
        return setValue("dModified", modifiedDate);
    }

    public Date getModifiedDate() {
        return (Date) getValue("dModified");
    }

    public JSONObject setBrandId(String brandId) {
        return poBrand.setBrandId(brandId);
    }

    public String getBrandId() {
        return poBrand.getBrandId();
    }

    public JSONObject setSourceEntryNo(Number entryNo) {
        return setValue("nSrEtryNo", entryNo);
    }

    public Number getSourceEntryNo() {
        return (Number) getValue("nSrEtryNo");
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
                    && poTerm.getTermId().equals((String) getValue("sTermCode"))) {
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
        if (!"".equals(getBrandId())) {
//            if (poBrand.getEditMode() == EditMode.READY
//                    && poBrand.getBrandId().equals(getBrandId())) {
//                return poBrand;
//            } else {
            poJSON = poBrand.openRecord(getBrandId());
            if ("success".equals((String) poJSON.get("result"))) {
                return poBrand;
            } else {
                poBrand.initialize();
                return poBrand;
            }
//            }
        } else {
            poBrand.initialize();
            return poBrand;
        }
    }

//    public Model_Brand Brand() throws GuanzonException, SQLException {
//        if (!"".equals((String) getValue("sBrandIDx"))) {
//            if (poBrand.getEditMode() == EditMode.READY
//                    && poBrand.getBrandId().equals((String) getValue("sBrandIDx"))) {
//                return poBrand;
//            } else {
//                poJSON = poBrand.openRecord((String) getValue("sBrandIDx"));
//
//                if ("success".equals((String) poJSON.get("result"))) {
//                    return poBrand;
//                } else {
//                    poBrand.initialize();
//                    return poBrand;
//                }
//            }
//        } else {
//            poBrand.initialize();
//            return poBrand;
//        }
//    }
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

    public Model_Inv_Stock_Request_Master InvStockRequestMaster() throws GuanzonException, SQLException {
        if (!"".equals((String) getValue("sTransNox"))) {
            if (poInvStockMaster.getEditMode() == EditMode.READY
                    && poInvStockMaster.getTransactionNo().equals((String) getValue("sTransNox"))) {
                return poInvStockMaster;
            } else {
                poJSON = poInvStockMaster.openRecord((String) getValue("sTransNox"));
                if ("success".equals((String) poJSON.get("result"))) {
                    return poInvStockMaster;
                } else {
                    poInvStockMaster.initialize();
                    return poInvStockMaster;
                }
            }
        } else {
            poInvStockMaster.initialize();
            return poInvStockMaster;
        }
    }

    public Model_Inv_Stock_Request_Detail InvStockRequestDetail() throws GuanzonException, SQLException {
        if (!"".equals((String) getValue("sSourceNo"))) {
            if (poInvStockDetail.getEditMode() == EditMode.READY
                    && poInvStockDetail.getTransactionNo().equals((String) getValue("sSourceNo"))) {
                return poInvStockDetail;
            } else {
                poJSON = poInvStockDetail.openRecordByReference((String) getValue("sSourceNo"), getValue("sStockIDx"));
                if ("success".equals((String) poJSON.get("result"))) {
                    return poInvStockDetail;
                } else {
                    poInvStockMaster.initialize();
                    return poInvStockDetail;
                }
            }
        } else {
            poInvStockDetail.initialize();
            return poInvStockDetail;
        }
    }
}
