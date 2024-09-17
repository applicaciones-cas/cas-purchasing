package org.guanzon.cas.purchasing.controller;

import org.guanzon.appdriver.iface.GTranDet;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import org.guanzon.appdriver.agent.ShowDialogFX;
import org.guanzon.appdriver.agent.ShowMessageFX;
import org.guanzon.appdriver.base.GRider;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.constant.TransactionStatus;
import org.guanzon.cas.inventory.base.Inventory;
import org.guanzon.cas.inventory.models.Model_PO_Quotation_Request_Detail;
import org.guanzon.cas.inventory.models.Model_PO_Quotation_Request_Master;
import org.guanzon.cas.parameters.Branch;
import org.guanzon.cas.parameters.Category_Level2;
import org.guanzon.cas.parameters.Color;
import org.guanzon.cas.parameters.Inv_Type;
import org.guanzon.cas.parameters.Measure;
import org.guanzon.cas.purchasing.model.Model_PO_Detail;
import org.guanzon.cas.purchasing.model.Model_PO_Master;
import org.guanzon.cas.validators.poquotation.Validator_PO_Quotation_Request_Detail;
import org.guanzon.cas.validators.poquotation.Validator_PO_Quotation_Request_Master;

import org.json.simple.JSONObject;

/**
 *
 * @author Maynard
 */
public class PurchaseOrder implements GTranDet {

    private GRider poGRider;
    private boolean pbWthParent;
    private int pnEditMode;
    private String psTranStatus;

    Model_PO_Master poModelMaster;
    ArrayList<Model_PO_Detail> poModelDetail;

    JSONObject poJSON;

    public PurchaseOrder(GRider foGRider, boolean fbWthParent) {
        poGRider = foGRider;
        pbWthParent = fbWthParent;

        poModelMaster = new Model_PO_Master(poGRider);
        poModelDetail = new ArrayList<Model_PO_Detail>();
        pnEditMode = EditMode.UNKNOWN;
    }

    @Override
    public JSONObject newTransaction() {
        poModelMaster = new Model_PO_Master(poGRider);
        poModelDetail = new ArrayList<Model_PO_Detail>();
        poModelMaster.newRecord();
        AddModelDetail();

        poModelMaster.setBranchCode(poGRider.getBranchCode());
        poJSON = new JSONObject();
        poJSON.put("result", "success");
        return poJSON;
    }

    @Override
    public JSONObject openTransaction(String fsValue) {

        poModelMaster = new Model_PO_Master(poGRider);
        
        //open the master table
        poJSON = poModelMaster.openRecord(fsValue);
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        poModelDetail = openTransactionDetail(poModelMaster.getTransactionNo());
        System.out.println(poModelMaster.getEntryNo());
        System.out.println(poModelDetail.size());
        if ((Integer) poModelMaster.getEntryNo() == poModelDetail.size()) {
            poJSON.put("result", "success");
            poJSON.put("message", "Record loaded successfully.");
        } else {
            poJSON.put("result", "error");
            poJSON.put("message", "Unable to load, Transaction seems having discrepancy");
        }

        return poJSON;

    }

    @Override
    public JSONObject updateTransaction() {
        JSONObject loJSON = new JSONObject();

        if (poModelMaster.getEditMode() == EditMode.UPDATE) {
            loJSON.put("result", "success");
            loJSON.put("message", "Edit mode has changed to update.");
        } else {
            loJSON.put("result", "error");
            loJSON.put("message", "No Transaction loaded to update.");
        }

        return loJSON;
    }

    @Override
    public JSONObject saveTransaction() {
        int lnCtr;
        String lsSQL;

//        Validator_PO_Quotation_Request_Detail ValidateDetails = new Validator_PO_Quotation_Request_Detail(poModelDetail);
//        if (!ValidateDetails.isEntryOkay()) {
//            poJSON.put("result", "error");
//            poJSON.put("message", ValidateDetails.getMessage());
//
//            return poJSON;
//
//        }
//
//        Validator_PO_Quotation_Request_Master ValidateMasters = new Validator_PO_Quotation_Request_Master(poModelMaster);
//        if (!ValidateMasters.isEntryOkay()) {
//            poJSON.put("result", "error");
//            poJSON.put("message", ValidateMasters.getMessage());
//
//            return poJSON;
//
//        }

        if (!pbWthParent) {
            poGRider.beginTrans();
        }

        poJSON = new JSONObject();
        //delete empty detail
        if (poModelDetail.get(getItemCount() - 1).getStockID().equals("") && poModelDetail.get(getItemCount() - 1).getStockID().equals("")) {
            RemoveModelDetail(getItemCount() - 1);
        }

        for (lnCtr = 0; lnCtr <= getItemCount() - 1; lnCtr++) {

            poModelDetail.get(lnCtr).setTransactionNo(poModelMaster.getTransactionNo());
            poModelDetail.get(lnCtr).setEntryNo(lnCtr + 1);

            poJSON = poModelDetail.get(lnCtr).saveRecord();

            if ("error".equals((String) poJSON.get("result"))) {
                if (!pbWthParent) {
                    poGRider.rollbackTrans();
                }
                return poJSON;
            }

        }
        if (poModelMaster.getEditMode() == EditMode.UPDATE) {
            ArrayList<Model_PO_Detail> laOldTransaction = openTransactionDetail(poModelMaster.getTransactionNo());

            if (laOldTransaction != null && !laOldTransaction.isEmpty()) {
                for (int lnCtr2 = lnCtr; lnCtr2 <= laOldTransaction.size() - 1; lnCtr2++) {
                    Model_PO_Detail detail = laOldTransaction.get(lnCtr2);

                    lsSQL = "DELETE FROM " + detail.getTable()
                            + " WHERE sStockIDx = " + SQLUtil.toSQL(detail.getStockID())
                            + " AND nEntryNox = " + SQLUtil.toSQL(detail.getEntryNo());

                    if (!lsSQL.isEmpty()) {
                        if (poGRider.executeQuery(lsSQL, detail.getTable(), "", "") == 0) {
                            if (!pbWthParent) {
                                poJSON.put("result", "error");
                                poJSON.put("message", "No Transaction loaded to update.");
                                poGRider.rollbackTrans();
                                return poJSON;

                            }
                        }
                    }
                }
            }
        }
        poModelMaster.setEntryNo(poModelDetail.size());
        poModelMaster.setPreparedDate(poGRider.getServerDate());
        poModelMaster.setPreparedBy(poGRider.getUserID());
        poModelMaster.setModifiedBy(poGRider.getUserID());
        poModelMaster.setModifiedDate(poGRider.getServerDate());

        poJSON = poModelMaster.saveRecord();
        if ("success".equals((String) poJSON.get("result"))) {
            if (!pbWthParent) {
                poGRider.commitTrans();
            }
        } else {
            if (!pbWthParent) {
                poGRider.rollbackTrans();
                poJSON.put("result", "error");
                poJSON.put("message", "Unable to Save Transaction.");
            }
        }

        return poJSON;
    }

    @Override
    public JSONObject deleteTransaction(String fsTransNox) {
        poJSON = new JSONObject();
        openTransaction(fsTransNox);
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        if (poModelMaster.getEditMode() == EditMode.READY || poModelMaster.getEditMode() == EditMode.UPDATE) {

            if (!pbWthParent) {
                poGRider.beginTrans();
            }
            String lsSQL = "DELETE FROM " + poModelMaster.getTable()
                    + " WHERE sTransNox = " + SQLUtil.toSQL(fsTransNox);
            
            String lsSQL2 = "DELETE FROM " + poModelDetail.get(poModelDetail.size() - 1).getTable()
                    + " WHERE sTransNox = " + SQLUtil.toSQL(fsTransNox);
            System.out.println(lsSQL2);

            if (!lsSQL.isEmpty()) {
                if (poGRider.executeQuery(lsSQL, poModelMaster.getTable(), poGRider.getBranchCode(), "") > 0) {
                    poJSON.put("result", "success");
                    poJSON.put("message", "Transaction saved successfully.");
                } else {
                    poJSON.put("result", "error");
                    poJSON.put("message", poGRider.getErrMsg());
                }
                
                if (poGRider.executeQuery(lsSQL2, poModelDetail.get(poModelDetail.size() - 1).getTable(), poGRider.getBranchCode(), "") > 0) {
                    poJSON.put("result", "success");
                    poJSON.put("message", "Transaction saved successfully.");
                } else {
                    poJSON.put("result", "error");
                    poJSON.put("message", poGRider.getErrMsg());
                }

                if ("success".equals((String) poJSON.get("result"))) {
                    if (!pbWthParent) {
                        poGRider.commitTrans();
                        poJSON.put("result", "success");
                        poJSON.put("message", "Transaction saved successfully.");
                    }
                } else {
                    if (!pbWthParent) {
                        poGRider.rollbackTrans();
                    }
                    poJSON.put("result", "error");
                    poJSON.put("message", "Unable to Delete Transaction.");
                }
            } else {
                poJSON.put("result", "error");
                poJSON.put("message", "Unable to Delete Transaction.");
            }
            

            
            
            
        } else {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No Transaction loaded to update.");
        }
        return poJSON;
    }

    @Override
    public JSONObject closeTransaction(String fsTransNox) {
        poJSON = new JSONObject();
        openTransaction(fsTransNox);
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        if (poModelMaster.getEditMode() == EditMode.READY || poModelMaster.getEditMode() == EditMode.UPDATE) {

            poModelMaster.setModifiedBy(poGRider.getUserID());
            poModelMaster.setModifiedDate(poGRider.getServerDate());
            poJSON = poModelMaster.setTransactionStatus(TransactionStatus.STATE_CLOSED);
            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }

            if (!pbWthParent) {
                poGRider.beginTrans();
            }
            poJSON = poModelMaster.saveRecord();

            if ("success".equals((String) poJSON.get("result"))) {
                if (!pbWthParent) {
                    poGRider.commitTrans();
                    poJSON.put("result", "success");
                    poJSON.put("message", "Transaction saved successfully.");
                }
            } else {
                if (!pbWthParent) {
                    poGRider.rollbackTrans();
                }
                poJSON.put("result", "error");
                poJSON.put("message", "Unable to Closed Transaction.");
            }
        } else {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No Transaction loaded to update.");
        }
        return poJSON;
    }

    @Override
    public JSONObject postTransaction(String fsTransNox) {
        poJSON = new JSONObject();
        openTransaction(fsTransNox);
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        if (poModelMaster.getEditMode() == EditMode.READY
                || poModelMaster.getEditMode() == EditMode.UPDATE) {

            poModelMaster.setModifiedBy(poGRider.getUserID());
            poModelMaster.setModifiedDate(poGRider.getServerDate());
            poJSON = poModelMaster.setTransactionStatus(TransactionStatus.STATE_POSTED);
            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
            if (!pbWthParent) {
                poGRider.beginTrans();
            }

            poJSON = poModelMaster.saveRecord();
            if ("success".equals((String) poJSON.get("result"))) {
                if (!pbWthParent) {
                    poGRider.commitTrans();
                    poJSON.put("result", "success");
                    poJSON.put("message", "Transaction Posted successfully.");
                }
            } else {
                if (!pbWthParent) {
                    poGRider.rollbackTrans();
                }
                poJSON.put("result", "error");
                poJSON.put("message", "Unable to Post Transaction.");
            }
        } else {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No Transaction loaded to update.");
        }
        return poJSON;

    }

    @Override
    public JSONObject voidTransaction(String fsTransNox) {
        poJSON = new JSONObject();
        openTransaction(fsTransNox);
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        if (poModelMaster.getEditMode() == EditMode.READY
                || poModelMaster.getEditMode() == EditMode.UPDATE) {

            poModelMaster.setModifiedBy(poGRider.getUserID());
            poModelMaster.setModifiedDate(poGRider.getServerDate());
            poJSON = poModelMaster.setTransactionNo(TransactionStatus.STATE_VOID);

            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }

            if (!pbWthParent) {
                poGRider.beginTrans();
            }

            poJSON = poModelMaster.saveRecord();
            if ("success".equals((String) poJSON.get("result"))) {
                if (!pbWthParent) {
                    poGRider.commitTrans();
                    poJSON.put("result", "success");
                    poJSON.put("message", "Transaction saved successfully.");
                }
            } else {
                if (!pbWthParent) {
                    poGRider.rollbackTrans();
                }
                poJSON.put("result", "error");
                poJSON.put("message", "Unable to Delete Transaction.");
            }
        } else {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No Transaction loaded to update.");
        }
        return poJSON;
    }

    @Override
    public JSONObject cancelTransaction(String fsTransNox) {
        poJSON = new JSONObject();

        openTransaction(fsTransNox);
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        if (poModelMaster.getEditMode() == EditMode.READY
                || poModelMaster.getEditMode() == EditMode.UPDATE) {
            poModelMaster.setModifiedBy(poGRider.getUserID());
            poModelMaster.setModifiedDate(poGRider.getServerDate());
            poJSON = poModelMaster.setTransactionNo(TransactionStatus.STATE_CANCELLED);

            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }

            if (!pbWthParent) {
                poGRider.beginTrans();
            }

            poJSON = poModelMaster.saveRecord();

            if ("success".equals((String) poJSON.get("result"))) {
                if (!pbWthParent) {
                    poGRider.commitTrans();
                    poJSON.put("result", "success");
                    poJSON.put("message", "Transaction saved successfully.");
                }
            } else {
                if (!pbWthParent) {
                    poGRider.rollbackTrans();
                }
                poJSON.put("result", "error");
                poJSON.put("message", "Unable to Delete Transaction.");
            }
        } else {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No Transaction loaded to update.");
        }
        return poJSON;
    }

    @Override
    public int getItemCount() {
        return poModelDetail.size();
    }

    @Override
    public Model_PO_Detail getDetailModel(int fnRow) {
        return poModelDetail.get(fnRow);

    }

    @Override
    public JSONObject setDetail(int fnRow, int fnCol, Object foData) {
        return setDetail(fnRow, poModelDetail.get(fnRow).getColumn(fnCol), foData);
    }

    @Override
    public JSONObject setDetail(int fnRow, String fsCol, Object foData) {
        poJSON = new JSONObject();

        switch (fsCol) {
            case "nQuantity":
                if ("error".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }

                poJSON = poModelDetail.get(fnRow).setValue(fsCol, foData);
                if (poModelDetail.get(fnRow).getQuantity() > 0
                        && !poModelDetail.get(fnRow).getStockID().isEmpty()) {
                    AddModelDetail();
                }
            default:
                return poModelDetail.get(fnRow).setValue(fsCol, foData);
        }

    }

    @Override
    public JSONObject searchDetail(int fnRow, String fsColumn, String fsValue, boolean fbByCode) {

        String lsHeader = "";
        String lsColName = "";
        String lsColCrit = "";
        String lsSQL = "";
        String lsCondition = "";
        JSONObject loJSON;

        switch (fsColumn) {

            case "sDescript": // sDescript
            case "sStockIDx": // 3 // 8-xCategrNm // 9-xInvTypNm
                Inventory loInventory = new Inventory(poGRider, true);
                loInventory.setRecordStatus(psTranStatus);
                loJSON = loInventory.searchRecord(fsValue, fbByCode);

                if (loJSON != null) {
                    String newStockID = (String) loInventory.getMaster("sStockIDx");

                    boolean isDuplicate = false;

                    for (int i = 0; i < poModelDetail.size(); i++) {
                        String existingStockID = (String) poModelDetail.get(i).getValue("sStockIDx");
                        if (newStockID.equals(existingStockID)) {
                            // Duplicate found, increment quantity by 1
                            int currentQuantity = (Integer) poModelDetail.get(i).getValue("nQuantity");
                            poModelDetail.get(i).setValue("nQuantity", currentQuantity + 1);
                            isDuplicate = true;
                            break;
                        }
                    }

                    if (!isDuplicate) {
                        // No duplicate found, add new details
                        setDetail(fnRow, "sStockIDx", (String) loInventory.getMaster("sStockIDx"));
                        setDetail(fnRow, "xCategrNm", (String) loInventory.getMaster("xCategNm2"));
                        setDetail(fnRow, "sDescript", (String) loInventory.getMaster("sDescript"));
                        loJSON = setDetail(fnRow, "xInvTypNm", (String) loInventory.getMaster("xInvTypNm"));
                    }

                    return loJSON;

                } else {
                    loJSON = new JSONObject();
                    loJSON.put("result", "error");
                    loJSON.put("message", "No Transaction found.");
                    return loJSON;
                }

            default:
                return null;

        }
    }

    @Override
    public JSONObject searchDetail(int fnRow, int fnColumn, String fsValue, boolean fbByCode) {
        return searchDetail(fnRow, poModelDetail.get(fnRow).getColumn(fnColumn), fsValue, fbByCode);

    }

    @Override
    public JSONObject searchWithCondition(String string) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public JSONObject searchTransaction(String fsColNme, String fsValue, boolean fbByCode) {
        String lsCondition = "";
        String lsFilter = "";

        if (psTranStatus.length() > 1) {
            for (int lnCtr = 0; lnCtr <= psTranStatus.length() - 1; lnCtr++) {
                lsCondition += ", " + SQLUtil.toSQL(Character.toString(psTranStatus.charAt(lnCtr)));
            }

            lsCondition = "cTranStat" + " IN (" + lsCondition.substring(2) + ")";
        } else {
            lsCondition = "cTranStat" + " = " + SQLUtil.toSQL(psTranStatus);
        }

        String lsSQL = MiscUtil.addCondition(poModelMaster.makeSelectSQL(), lsCondition);

        poJSON = new JSONObject();

        poJSON = ShowDialogFX.Search(poGRider,
                lsSQL,
                fsValue,
                "Transaction No»Date»Refer No",
                "sTransNox»dTransact»sReferNox",
                "sTransNox»dTransact»sReferNox",
                fbByCode ? 0 : 2);

        if (poJSON != null) {
            return openTransaction((String) poJSON.get("sTransNox"));

        } else {
            poJSON.put("result", "error");
            poJSON.put("message", "No Transaction loaded to update.");
            return poJSON;
        }
    }

    @Override
    public JSONObject searchMaster(String fsColNme, String fsValue, boolean fbByCode) {

        String lsHeader = "";
        String lsColName = "";
        String lsColCrit = "";
        String lsSQL = "";
        String lsCondition = "";
        JSONObject loJSON;
//        if (fsValue.equals("") && fbByCode) return null;

        switch (fsColNme) {
            case "sTransNox": // 1
                return searchTransaction(fsColNme, fsValue, fbByCode);

            case "sDestinat": //4 //16-xDestinat
                Branch loDestinat = new Branch(poGRider, true);
                loDestinat.setRecordStatus(psTranStatus);
                loJSON = loDestinat.searchRecord(fsValue, fbByCode);

                if (loJSON != null) {
                    setMaster("sDestinat", (String) loDestinat.getMaster("sBranchCd"));
                    setMaster("xDestinat", (String) loDestinat.getMaster("sBranchNm"));

                    return loJSON;

                } else {
                    loJSON = new JSONObject();
                    loJSON.put("result", "error");
                    loJSON.put("message", "No Transaction found.");
                    return loJSON;
                }

            case "sCategrCd": //9 //17-xCategrNm

                Category_Level2 loCategory2 = new Category_Level2(poGRider, true);
                loCategory2.setRecordStatus("01");
                loJSON = loCategory2.searchRecord(fsValue, fbByCode);

                Inv_Type loInvType = new Inv_Type(poGRider, true);
                loInvType.openRecord((String) loCategory2.getMaster("sInvTypCd"));

                if (loJSON != null) {
                    setMaster("sCategrCd", (String) loCategory2.getMaster("sCategrCd"));
                    setMaster("xCategrNm", (String) loCategory2.getMaster("sDescript"));
                    return setMaster("xInvTypNm", (String) loCategory2.getMaster("xInvTypNm"));

                } else {

                    loJSON = new JSONObject();
                    loJSON.put("result", "error");
                    loJSON.put("message", "No Transaction found.");
                    return loJSON;
                }

            default:
                return null;
        }
    }

    @Override
    public JSONObject searchMaster(int fnCol, String fsValue, boolean fbByCode) {
        return searchMaster(poModelMaster.getColumn(fnCol), fsValue, fbByCode);
    }

    @Override
    public Model_PO_Master getMasterModel() {
        return poModelMaster;
    }

    @Override
    public JSONObject setMaster(int fnCol, Object foData) {
        return poModelMaster.setValue(fnCol, foData);
    }

    @Override
    public JSONObject setMaster(String fsCol, Object foData) {
        return poModelMaster.setValue(fsCol, foData);
    }

    @Override
    public int getEditMode() {
        return pnEditMode;
    }

    @Override
    public void setTransactionStatus(String fsValue) {
        psTranStatus = fsValue;
    }

    public ArrayList<Model_PO_Detail> openTransactionDetail(String fsTransNo) {
        ArrayList<Model_PO_Detail> loDetail = new ArrayList<>();

        try {
            Model_PO_Detail placeholder = new Model_PO_Detail(poGRider);
            String lsSQL = MiscUtil.addCondition(placeholder.makeSelectSQL(), "sTransNox = " + SQLUtil.toSQL(fsTransNo));
            ResultSet loRS = poGRider.executeQuery(lsSQL);

            while (loRS.next()) {
                Model_PO_Detail detail = new Model_PO_Detail(poGRider);
                detail.openRecord(loRS.getString("sTransNox"), loRS.getString("sStockIDx"));
                loDetail.add(detail);
            }

            if (loDetail.isEmpty()) {
                new ArrayList<>();
            }

            return loDetail;

        } catch (SQLException ex) {
            // Handle exceptions by returning an empty list or logging the error
            ex.printStackTrace();
            return new ArrayList<>();
        }
    }

    public JSONObject AddModelDetail() {
        if (poModelDetail.isEmpty()) {
            poModelDetail.add(new Model_PO_Detail(poGRider));
            poModelDetail.get(poModelDetail.size() - 1).setTransactionNo(poModelMaster.getTransactionNo());
        } else {

            boolean lsModelRequired = poModelDetail.get(poModelDetail.size() - 1).getQuantity() > 0;
            if (lsModelRequired) {
                poModelDetail.add(new Model_PO_Detail(poGRider));
                poModelDetail.get(poModelDetail.size() - 1).setTransactionNo(poModelMaster.getTransactionNo());

            } else {
                poJSON = new JSONObject();
                poJSON.put("result", "Information");
                poJSON.put("message", "Please Fill up Required Record First!");
            }
        }

        return poJSON;
    }

    public void RemoveModelDetail(int fnRow) {
        poModelDetail.remove(fnRow);

    }

    public Inventory GetInventory(String fsPrimaryKey, boolean fbByCode) {
        Inventory instance = new Inventory(poGRider, fbByCode);
        instance.openRecord(fsPrimaryKey);
        return instance;
    }

    public Color GetColor(String fsPrimaryKey, boolean fbByCode) {
        Color instance = new Color(poGRider, fbByCode);
        instance.openRecord(fsPrimaryKey);
        return instance;
    }

    public Measure GetMeasure(String fsPrimaryKey, boolean fbByCode) {
        Measure instance = new Measure(poGRider, fbByCode);
        instance.openRecord(fsPrimaryKey);
        return instance;
    }

}
