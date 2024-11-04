package org.guanzon.cas.purchasing.controller;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import org.guanzon.appdriver.iface.GTranDet;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JsonDataSource;
import net.sf.jasperreports.view.JasperViewer;

import org.guanzon.appdriver.agent.ShowDialogFX;
import org.guanzon.appdriver.agent.ShowMessageFX;
import org.guanzon.appdriver.base.GRider;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.constant.TransactionStatus;
import org.guanzon.cas.inventory.base.Inventory;
import org.guanzon.cas.parameters.Branch;
import org.guanzon.cas.parameters.Category_Level2;
import org.guanzon.cas.parameters.Color;
import org.guanzon.cas.parameters.Inv_Type;
import org.guanzon.cas.parameters.Measure;
import org.guanzon.cas.clients.Client_Master;
import org.guanzon.cas.inventory.base.InvMaster;
import org.guanzon.cas.inventory.base.PO_Quotation;
import org.guanzon.cas.inventory.models.Model_Inv_Stock_Request_Detail;
import org.guanzon.cas.model.clients.Model_Client_Address;
import org.guanzon.cas.model.clients.Model_Client_Institution_Contact;
import org.guanzon.cas.model.clients.Model_Client_Mobile;
import org.guanzon.cas.parameters.Category;
import org.guanzon.cas.parameters.Company;
import org.guanzon.cas.parameters.Model;
import org.guanzon.cas.parameters.Model_Variant;
import org.guanzon.cas.parameters.Term;
import org.guanzon.cas.purchasing.model.Model_PO_Detail;
import org.guanzon.cas.purchasing.model.Model_PO_Master;
import org.guanzon.cas.validators.ValidatorFactory;
import org.guanzon.cas.purchasing.validator.Validator_PurchaseOrder_Detail;
import org.guanzon.cas.purchasing.validator.Validator_PurchaseOrder_Master;
import org.json.simple.JSONArray;

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
    public String transType;
    public int rowselect;

    public void setTransType(String value) {
        transType = value;
    }

    public String getTransType() {
        return transType;
    }

    public void setRowSelect(int value) {
        rowselect = value;
    }

    public int getRowSelect() {
        return rowselect;
    }

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

        pnEditMode = poModelMaster.getEditMode();

        poModelMaster.setBranchCode(poGRider.getBranchCode());

        Category loCateg = new Category(poGRider, true);
        switch (poGRider.getDivisionCode()) {
            case "0"://mobilephone
                loCateg.openRecord("0002");
                break;

            case "1"://motorycycle
                loCateg.openRecord("0001");
                break;

            case "2"://Auto Group - Honda Cars
            case "5"://Auto Group - Nissan
            case "6"://Auto Group - Any
                loCateg.openRecord("0003");
                break;

            case "3"://Hospitality
            case "4"://Pedritos Group
                loCateg.openRecord("0004");
                break;

            case "7"://Guanzon Services Office
                break;

            case "8"://Main Office
                break;
        }
        poModelMaster.setCategoryCode((String) loCateg.getMaster("sCategrCd"));
        poModelMaster.setCategoryName((String) loCateg.getMaster("sDescript"));

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        return poJSON;
    }

    @Override
    public JSONObject openTransaction(String fsValue) {

        poModelDetail = new ArrayList<Model_PO_Detail>();
        poModelMaster = new Model_PO_Master(poGRider);

        //open the master table
        poJSON = poModelMaster.openRecord(fsValue);
        System.out.println(poJSON.toJSONString());
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poModelDetail = openTransactionDetail(poModelMaster.getTransactionNo());

        System.out.println(poModelMaster.getEmailSentNo());
        System.out.println(poModelMaster.getSourceNo());
        System.out.println(poModelMaster.getCategoryCode());
        System.out.println(poModelMaster.getEntryNo());

        if ((Integer) poModelMaster.getEntryNo() == poModelDetail.size()) {
            poJSON.put("result", "success");
            poJSON.put("message", "Record loaded successfully.");
        } else {
            poJSON.put("result", "error");
            poJSON.put("message", "Unable to load, Transaction seems having discrepancy");
        }

        pnEditMode = poModelMaster.getEditMode();
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
        Validator_PurchaseOrder_Detail ValidateDetails = new Validator_PurchaseOrder_Detail(poModelDetail);
        if (!ValidateDetails.isEntryOkay()) {
            poJSON.put("result", "error");
            poJSON.put("message", ValidateDetails.getMessage());
            return poJSON;
        }

        Validator_PurchaseOrder_Master ValidateMasters = new Validator_PurchaseOrder_Master(poModelMaster);
        if (!ValidateMasters.isEntryOkay()) {
            poJSON.put("result", "error");
            poJSON.put("message", ValidateMasters.getMessage());
            return poJSON;
        }

        if (!pbWthParent) {
            poGRider.beginTrans();
        }
        poJSON = new JSONObject();

        if (poModelDetail.get(getItemCount() - 1).getStockID().equals("") && poModelDetail.get(getItemCount() - 1).getStockID().equals("")) {
            RemoveModelDetail(getItemCount() - 1);
        }
        int lnEntryNo = 0;
        Model_PO_Detail loOldEntity = new Model_PO_Detail(poGRider);

        poModelMaster.getDiscount();
        for (lnCtr = 0; lnCtr <= getItemCount() - 1; lnCtr++) {
            if (poModelMaster.getEditMode() != EditMode.DELETE) {
                // transNox already exists and entryNox then update
                poJSON = updateTransaction();
                lnEntryNo = (lnCtr + 1);
                poModelDetail.get(lnCtr).setTransactionNo(poModelMaster.getTransactionNo());
                poModelDetail.get(lnCtr).setEntryNo(lnEntryNo);
                poJSON = poModelDetail.get(lnCtr).saveRecord();
            }
        }

        if ("error".equals((String) poJSON.get("result"))) {
            if (!pbWthParent) {
                poGRider.rollbackTrans();
            }
            return poJSON;
        }

        if (poModelMaster.getEditMode() == EditMode.UPDATE) {
            ArrayList<Model_PO_Detail> laOldTransaction = openTransactionDetail(poModelMaster.getTransactionNo());

            if (laOldTransaction != null && !laOldTransaction.isEmpty()) {
                for (int lnCtr2 = lnCtr; lnCtr2 <= laOldTransaction.size() - 1; lnCtr2++) {
                    Model_PO_Detail detail = laOldTransaction.get(lnCtr2);

//                    lsSQL = "DELETE FROM " + detail.getTable()
//                            + " WHERE nEntryNox = " + SQLUtil.toSQL(detail.getEntryNo());
                    lsSQL = "DELETE FROM " + detail.getTable()
                            + " WHERE sTransNox = " + SQLUtil.toSQL(poModelMaster.getTransactionNo())
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

        for (lnCtr = 0; lnCtr <= poModelDetail.size() - 1; lnCtr++) {
            poModelMaster.setEntryNo(lnCtr + 1);
        }

//        poModelMaster.setCategoryCode("0001");
        poModelMaster.setPreparedDate(poGRider.getServerDate());
        poModelMaster.setModifiedBy(poGRider.getUserID());
        poModelMaster.setModifiedDate(poGRider.getServerDate());
        poModelMaster.getDiscount();
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
        poJSON = openTransaction(fsTransNox);
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
        poJSON = openTransaction(fsTransNox);
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
        poJSON = openTransaction(fsTransNox);
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        if (poModelMaster.getEditMode() == EditMode.READY
                || poModelMaster.getEditMode() == EditMode.UPDATE) {

            poModelMaster.setModifiedBy(poGRider.getUserID());
            poModelMaster.setModifiedDate(poGRider.getServerDate());
            poJSON = poModelMaster.setTransactionStatus(TransactionStatus.STATE_VOID);

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

        poJSON = openTransaction(fsTransNox);
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

                return poJSON;
            case "nUnitPrce":
                if ("error".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }

                poJSON = poModelDetail.get(fnRow).setValue(fsCol, foData);
                return poJSON;
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

            case "sStockIDx":
//                System.out.println("searchDetail p_bWithUI = " + p_bWithUI);
                Inventory loInventory = new Inventory(poGRider, true);
                loInventory.setRecordStatus(psTranStatus);
                loInventory.setWithUI(true);
                double lnTotalTransaction = 0;

                String lstranstype = getTransType();
                String lscondition = "";
                switch (lstranstype) {
                    case "SP":
                        lscondition = "a.sCategCd1 = " + SQLUtil.toSQL(poModelMaster.getCategoryCode()) + " AND a.sCategCd2 = " + SQLUtil.toSQL("0007");
                        break;
                    case "MC":
                        lscondition = "a.sCategCd1 = " + SQLUtil.toSQL(poModelMaster.getCategoryCode()) + " AND a.sCategCd2 != " + SQLUtil.toSQL("0007");
                        break;
                }

                loJSON = loInventory.searchRecordWithContition(fsValue, lscondition, fbByCode);
                System.out.println("poJSON = " + poJSON);

                if (loJSON != null) {
                    String newStockID = (String) loInventory.getMaster("sStockIDx");
                    boolean isDuplicate = false;
                    for (int i = 0; i < poModelDetail.size() - 1; i++) {
                        String existingStockID = (String) poModelDetail.get(i).getValue("sStockIDx");

                        if (newStockID.equals(existingStockID)) {
                            isDuplicate = true;
                            setRowSelect(i);
                            int currentQuantity = (Integer) poModelDetail.get(i).getValue("nQuantity");
                            poModelDetail.get(i).setValue("nQuantity", currentQuantity + 1);
                            lnTotalTransaction += Double.parseDouble((poModelDetail.get(i).getValue("nUnitPrce")).toString()) * Double.parseDouble(poModelDetail.get(i).getValue("nQuantity").toString());
                            break;
                        } else {
                            lnTotalTransaction += Double.parseDouble((poModelDetail.get(i).getValue("nUnitPrce")).toString()) * Double.parseDouble(poModelDetail.get(i).getValue("nQuantity").toString());
                            System.out.println(lnTotalTransaction);
                        }
                    }

                    if (!isDuplicate) {
                        setDetail(fnRow, "sDescript", (String) loInventory.getMaster("sDescript"));
                        setDetail(fnRow, "sStockIDx", (String) loInventory.getMaster("sStockIDx"));
                        setDetail(fnRow, "nOrigCost", loInventory.getMaster("nUnitPrce"));
                        setDetail(fnRow, "nUnitPrce", loInventory.getMaster("nUnitPrce"));
                        setDetail(fnRow, "nQuantity", "0");
                        InvMaster loInvMaster = new InvMaster(poGRider, true);
                        loInvMaster.setRecordStatus(psTranStatus);
                        loInvMaster.setWithUI(true);
                        loInvMaster.openRecord(loInventory.getModel().getStockID());

                        if ("success".equals((String) poJSON.get("result"))) {
//                            setDetail(fnRow, "nRecOrder", loInvMaster.getModel().ge);
                            setDetail(fnRow, "nQtyOnHnd", loInvMaster.getModel().getQtyOnHnd());
                        } else {
                            setDetail(fnRow, "nQtyOnHnd", 0.00);
                        }
                    } else {
//                        if (fnRow == poModelDetail.size() - 1 || poModelDetail.get(poModelDetail.size() - 1).getStockID().isEmpty()) {
//                            RemoveModelDetail(poModelDetail.size() - 1);
//                        }
                    }
                    return loJSON;
                } else {
                    loJSON = new JSONObject();
                    loJSON.put("result", "error");
                    loJSON.put("message", "No Transaction found.");
                    return loJSON;
                }

//                Inventory loInventory = new Inventory(poGRider, true);
//                loInventory.setRecordStatus("1");
//                loInventory.setWithUI(true);
//                loJSON = loInventory.searchRecord(fsValue, fbByCode);
//                double lnTotalTransaction = 0;
//
//                Model_Inv_Stock_Request_Detail lo_inv_stock_request_detail;
//
//                if (loJSON != null) {
//                    String newStockID = (String) loInventory.getMaster("sStockIDx");
//                    System.out.println(fsValue);
//                    boolean isDuplicate = false;
//
//                    for (int i = 0; i < poModelDetail.size() - 1; i++) {
//                        String existingStockID = (String) poModelDetail.get(i).getValue("sStockIDx");
//
//                        if (newStockID.equals(existingStockID)) {
//                            isDuplicate = true;
//                            int currentQuantity = (Integer) poModelDetail.get(i).getValue("nQuantity");
//                            poModelDetail.get(i).setValue("nQuantity", currentQuantity + 1);
//                            lnTotalTransaction += Double.parseDouble((poModelDetail.get(i).getValue("nUnitPrce")).toString()) * Double.parseDouble(poModelDetail.get(i).getValue("nQuantity").toString());
//                            break;
//                        } else {
//                            lnTotalTransaction += Double.parseDouble((poModelDetail.get(i).getValue("nUnitPrce")).toString()) * Double.parseDouble(poModelDetail.get(i).getValue("nQuantity").toString());
//                            System.out.println(lnTotalTransaction);
//                        }
//                    }
//                    if (!isDuplicate) {
//                        lo_inv_stock_request_detail = this.GetModel_Inv_Stock_Request_Detail(newStockID);
//                        setDetail(fnRow, "sDescript", (String) loInventory.getMaster("sDescript"));
//                        setDetail(fnRow, "sStockIDx", (String) loInventory.getMaster("sStockIDx"));
//                        setDetail(fnRow, "nOrigCost", loInventory.getMaster("nUnitPrce"));
//                        setDetail(fnRow, "nUnitPrce", loInventory.getMaster("nUnitPrce"));
//                        setDetail(fnRow, "nRecOrder", lo_inv_stock_request_detail.getRecordOrder());
//                        setDetail(fnRow, "nQtyOnHnd", lo_inv_stock_request_detail.getQuantity());
//                        setDetail(fnRow, "nQuantity", "0");
//                    } else {
//
//                        if (fnRow == poModelDetail.size() - 1 || poModelDetail.get(poModelDetail.size() - 1).getStockID().isEmpty()) {
//                            RemoveModelDetail(poModelDetail.size() - 1);
//                        }
//                    }
//                    return loJSON;
//                } else {
//                    loJSON = new JSONObject();
//                    loJSON.put("result", "error");
//                    loJSON.put("message", "No Transaction found.");
//                    return loJSON;
//                }
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
        Properties po_props = new Properties();
        try {
            po_props.load(new FileInputStream("D:\\GGC_Maven_Systems\\config\\cas.properties"));
        } catch (IOException ex) {
            Logger.getLogger(PurchaseOrder.class.getName()).log(Level.SEVERE, null, ex);
        }
        String a = po_props.getProperty("store.inventory.category");

        String lsCondition = "";
        String lsFilter = "";

        if (psTranStatus.length() > 1) {
            for (int lnCtr = 0; lnCtr <= psTranStatus.length() - 1; lnCtr++) {
                lsCondition += ", " + SQLUtil.toSQL(Character.toString(psTranStatus.charAt(lnCtr)));
            }

            lsCondition = "a.cTranStat" + " IN (" + lsCondition.substring(2) + ")";
        } else {
            lsCondition = "a.cTranStat" + " = " + SQLUtil.toSQL(psTranStatus);
        }

//        String lsSQL = MiscUtil.addCondition(poModelMaster.makeSelectSQL(), lsCondition);
        String lsSQL = MiscUtil.addCondition(getSQL(), lsCondition);

        String lstranstype = getTransType();
        switch (lstranstype) {
            case "SP":
                
                lsSQL += "AND " + "n.sCategCd1 = " + SQLUtil.toSQL(poModelMaster.getCategoryCode()) + " AND n.sCategCd2 = '0007'";
            case "MC":
                lsSQL += "AND "+ "n.sCategCd1 = " + SQLUtil.toSQL(poModelMaster.getCategoryCode()) + " AND n.sCategCd2 != '0007'";
        }

        lsSQL += " GROUP BY a.sTransNox";
        System.out.println("This is resulta " + lsSQL);
        poJSON = new JSONObject();

        switch (fsColNme) {
            case "sTransNox": // 1
                poJSON = ShowDialogFX.Search(poGRider,
                        lsSQL,
                        fsValue,
                        "Transaction No»Destination»Supplier",
                        "sTransNox»sDestinat»sSupplier",
                        "a.sTransNox»a.sDestinat»a.sSupplier",
                        fbByCode ? 0 : 1);
                break;
            case "sSupplier":
                poJSON = ShowDialogFX.Search(poGRider,
                        lsSQL,
                        fsValue,
                        "Supplier»Transaction No»Destination",
                        "sSupplier»sTransNox»sDestinat",
                        "sSupplier»sTransNox»sDestinat",
                        fbByCode ? 0 : 0);
                break;
            case "sDestinat":
                poJSON = ShowDialogFX.Search(poGRider,
                        lsSQL,
                        fsValue,
                        "Destination»Transaction No»Supplier",
                        "sDestinat»sTransNox»sSupplier",
                        "sDestinat»sTransNox»sSupplier",
                        fbByCode ? 0 : 0);
                break;
            default:
                break;
        }

        if (poJSON != null) {
            poJSON = openTransaction((String) poJSON.get("sTransNox"));

        } else {
            poJSON.put("result", "error");
            poJSON.put("message", "No Transaction loaded to update.");
            return poJSON;
        }
        return poJSON;
    }

    @Override
    public JSONObject searchMaster(String fsColNme, String fsValue, boolean fbByCode) {

        String lsHeader = "";
        String lsColName = "";
        String lsColCrit = "";
        String lsSQL = "";
        String lsCondition = "";
        JSONObject loJSON;

        switch (fsColNme) {
            case "sSupplier": //4 //16-xDestinat
                Client_Master loSupplier = new Client_Master(poGRider, true, poGRider.getBranchCode());
                loSupplier.setType(ValidatorFactory.ClientTypes.COMPANY);
                loJSON = loSupplier.searchRecord(fsValue, fbByCode);

                if (loJSON != null) {
                    String lsClientID = (String) loSupplier.getMaster("sClientID");
                    setMaster("sSupplier", lsClientID); // SupplierID 
                    setMaster("xSupplier", (String) loSupplier.getMaster("sCompnyNm")); // CompanyName

                    Model_Client_Institution_Contact loContctP = GetModel_Client_Institution_Contact(lsClientID);
                    setMaster("xCPerson1", (String) loContctP.getContactPerson());

                    Model_Client_Mobile loContctNo = GetModel_Client_Mobile(lsClientID);
                    setMaster("xCPMobil1", loContctNo.getContactNo());
                    setMaster("sContctID", loContctNo.getMobileID());

                    Model_Client_Address loAddressID = GetModel_Client_Address(lsClientID);
                    setMaster("sAddrssID", loAddressID.getAddressID());

                    return loJSON;

                } else {
                    loJSON = new JSONObject();
                    loJSON.put("result", "error");
                    loJSON.put("message", "No Transaction found.");
                    return loJSON;
                }
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
            case "sTermCode": //4 //16-xDestinat
                Term loTerm = new Term(poGRider, true);
                loTerm.setRecordStatus(psTranStatus);
                loJSON = loTerm.searchRecord(fsValue, fbByCode);

                if (loJSON != null) {
                    setMaster("sTermCode", (String) loTerm.getMaster("sTermCode"));
                    setMaster("xTermName", (String) loTerm.getMaster("sDescript"));
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
            case "sSourceNo": // For the FIND SOURCE FUNCTION
                int fnRow = 1;
                Inventory loInventory = new Inventory(poGRider, true);
                loInventory.setRecordStatus("1");
                loInventory.setWithUI(true);
                PO_Quotation loPO_Quotation = new PO_Quotation(poGRider, true);
                loPO_Quotation.setTransactionStatus("12");
                loJSON = loPO_Quotation.searchTransaction("sTransNox", "", false);
                double lnTotalTransaction2 = 0;
                int lndetailsize = poModelDetail.size() - 1;
                int lnpo_detailsize = loPO_Quotation.getItemCount() - 1;

                if (loJSON != null) {
                    for (int i = 0; i < loPO_Quotation.getItemCount(); i++) {
                        fnRow = i;
                        String newStockID = String.valueOf(loPO_Quotation.getDetailModel(i).getStockID());
                        loInventory = GetInventory(newStockID, true);
                        System.out.println(fsValue);
                        boolean isDuplicate = false;

                        if (poModelDetail.size() - 1 < lnpo_detailsize) {
                            loJSON = AddModelDetail();
                            if ("Information".equals((String) poJSON.get("result"))) {
                                fnRow = i;

                                setDetail(fnRow, "nQuantity", (int) getDetailModel(fnRow).getQuantity() + 1);
                                AddModelDetail();
                                setDetail(fnRow, "sTransNox", this.getMasterModel().getTransactionNo());
                                setDetail(fnRow, "nEntryNox", loPO_Quotation.getDetailModel(i).getEntryNumber());
                                setDetail(fnRow, "sStockIDx", (String) loPO_Quotation.getDetailModel(i).getStockID());
                                setDetail(fnRow, "sDescript", (String) loPO_Quotation.getDetailModel(i).getDescription());
                                setDetail(fnRow, "nQuantity", (int) loPO_Quotation.getDetailModel(i).getQuantity());
                                setDetail(fnRow, "nUnitPrce", loPO_Quotation.getDetailModel(i).getUnitPrice());
                                setDetail(fnRow, "nOrigCost", loInventory.getModel().getUnitPrice());
                                continue;
                            } else {
                                fnRow = i;
                                setDetail(fnRow, "sTransNox", this.getMasterModel().getTransactionNo());
                                setDetail(fnRow, "nEntryNox", loPO_Quotation.getDetailModel(i).getEntryNumber());
                                setDetail(fnRow, "sStockIDx", (String) loPO_Quotation.getDetailModel(i).getStockID());
                                setDetail(fnRow, "sDescript", (String) loPO_Quotation.getDetailModel(i).getDescription());
                                setDetail(fnRow, "nQuantity", (int) loPO_Quotation.getDetailModel(i).getQuantity());
                                setDetail(fnRow, "nUnitPrce", loPO_Quotation.getDetailModel(i).getUnitPrice());
                                setDetail(fnRow, "nOrigCost", loInventory.getModel().getUnitPrice());
                                continue;
                            }

                        } else {
                            fnRow = i;
                            setDetail(fnRow, "nEntryNox", loPO_Quotation.getDetailModel(i).getEntryNumber());
                            setDetail(fnRow, "sStockIDx", (String) loPO_Quotation.getDetailModel(i).getStockID());
                            setDetail(fnRow, "sDescript", (String) loPO_Quotation.getDetailModel(i).getDescription());
                            setDetail(fnRow, "nQuantity", (int) loPO_Quotation.getDetailModel(i).getQuantity());
                            setDetail(fnRow, "nUnitPrce", loPO_Quotation.getDetailModel(i).getUnitPrice());
                            setDetail(fnRow, "nOrigCost", loInventory.getModel().getUnitPrice());
                            continue;
                        }
                    }

                    if (poModelDetail.size() - 1 > lnpo_detailsize) {
                        fnRow = poModelDetail.size() - 1;
                        for (int i = fnRow; i >= loPO_Quotation.getItemCount(); i--) {
                            RemoveModelDetail(i);
                        }
                    }
                    String[] lacolumn = {
                        "sReferNox", "sSupplier", "sAddrssID", "sContctID",
                        "sTermCode", "xTermName", "nDiscount", "nAddDiscx",
                        "nVatRatex", "nTWithHld", "nTranTotl", "sRemarksx",
                        "nEntryNox", "sCategrCd"
                    };

                    setMaster("sSourceNo", (String) loPO_Quotation.getMasterModel().getTransactionNumber());
                    String lsClientID = (String) loPO_Quotation.getMasterModel().getValue("sSupplier");
                    try {
                        loSupplier = GetClient_Master(lsClientID, true, poGRider.getBranchCode());
                        setMaster("xSupplier", (String) loSupplier.getMaster("sCompnyNm")); // CompanyNam

                        Model_Client_Institution_Contact loContctP = GetModel_Client_Institution_Contact(lsClientID);
                        setMaster("xCPerson1", (String) loContctP.getContactPerson());

                        Model_Client_Mobile loContctNo = GetModel_Client_Mobile(lsClientID);
                        setMaster("xCPMobil1", loContctNo.getContactNo());

                        Model_Client_Address loAddressID = GetModel_Client_Address(lsClientID);
                        setMaster("sAddrssID", loAddressID.getAddressID());

                    } catch (Exception e) {
                        setMaster("xSupplier", "");
                        setMaster("xCPerson1", "");
                        setMaster("xCPMobil1", "");
                        setMaster("sAddrssID", "");
                    }

                    loTerm = new Term(poGRider, true);
                    loTerm.setRecordStatus(psTranStatus);
                    loTerm.searchRecord((String) loPO_Quotation.getMasterModel().getValue("sTermCode"), true);
                    setMaster("xTermName", (String) loTerm.getMaster("sDescript"));

                    for (int i = 0; i < lacolumn.length; i++) {
                        Object loval = loPO_Quotation.getMasterModel().getValue(lacolumn[i]);
                        setMaster(lacolumn[i], loval instanceof String ? (String) loval : loval); // instanceof: a shortcut to try-catch and used shorthand if else
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
                detail.openRecord(loRS.getString("sTransNox"), loRS.getString("nEntryNox"));
                loDetail.add(detail);
            }

            if (loDetail.isEmpty()) {
                new ArrayList<>();
            }

            return loDetail;

        } catch (SQLException ex) {
// Handle exceptions by returning an empty list or logging the error
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
        instance.openRecord(fsPrimaryKey); //
        return instance;
    }

    public Model GetModel(String fsPrimaryKey, boolean fbByCode) {
        Model instance = new Model(poGRider, fbByCode);
        instance.openRecord(fsPrimaryKey); //
        return instance;
    }

    public Model_Variant GetModel_Variant(String fsPrimaryKey, boolean fbByCode) {
        Model_Variant instance = new Model_Variant(poGRider, fbByCode);
        instance.openRecord(fsPrimaryKey); //
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

    public Client_Master GetClient_Master(String fsPrimaryKey, boolean fbWtParent, String fsBranchCd) {
        Client_Master instance = new Client_Master(poGRider, fbWtParent, fsBranchCd);
        instance.setType(ValidatorFactory.ClientTypes.COMPANY);
        instance.openRecord(fsPrimaryKey);
        return instance;
    }

    public Model_Client_Institution_Contact GetModel_Client_Institution_Contact(String fsPrimaryKey) {
        Model_Client_Institution_Contact instance = new Model_Client_Institution_Contact(poGRider);
        instance.openRecord(fsPrimaryKey);
        return instance;
    }

    public Model_Client_Mobile GetModel_Client_Mobile(String fsPrimaryKey) {
        Model_Client_Mobile instance = new Model_Client_Mobile(poGRider);
        instance.openRecord(fsPrimaryKey);
        return instance;
    }

    public Model_Inv_Stock_Request_Detail GetModel_Inv_Stock_Request_Detail(String fsPrimaryKey) {
        Model_Inv_Stock_Request_Detail instance = new Model_Inv_Stock_Request_Detail(poGRider);
        instance.openRecord(fsPrimaryKey);
        return instance;
    }

    public Company GetCompany(String fsPrimaryKey, boolean fbByCode) {
        Company instance = new Company(poGRider, fbByCode);
        instance.openRecord(fsPrimaryKey);
        return instance;
    }

    public Model_Client_Address GetModel_Client_Address(String fsPrimaryKey) {
        Model_Client_Address instance = new Model_Client_Address(poGRider);
        instance.openRecord(fsPrimaryKey);
        return instance;
    }

    public Branch GetBranch(String fsPrimaryKey) {
        Branch instance = new Branch(poGRider, true);
        instance.openRecord(fsPrimaryKey);
        return instance;
    }

    public JSONObject printRecord() {
        JSONObject loJSON = new JSONObject();
        if (poModelMaster == null) {
            ShowMessageFX.Warning("Unable to print transaction.", "Warning", "No record loaded.");
            loJSON.put("result", "error");
            loJSON.put("message", "Model Master is null");
            return loJSON;
        }

        if (poModelDetail.isEmpty()) {
            ShowMessageFX.Warning("Unable to print transaction.", "Warning", "No record loaded.");
            loJSON.put("result", "error");
            loJSON.put("message", "Model Detail is empty");
            return loJSON;
        }

        Client_Master loClient_Master = GetClient_Master(poModelMaster.getSupplier(), true, poGRider.getBranchCode()); //poModelMaster.getPreparedBy()

        //Create the parameter
        Map<String, Object> params = new HashMap<>();
        params.put("sBranchNm", poGRider.getBranchName());
        params.put("sAddressx", poGRider.getAddress() + ", " + poGRider.getTownName() + " " + poGRider.getProvince());
        params.put("xSupplier", poModelMaster.getSupplierName());

        params.put("sReportNm", "");
        params.put("sReportDt", "");
        params.put("sPrintdBy", "");

        Branch loCompnyNm = GetBranch(poGRider.getBranchCode());
        Company loCompnyName = GetCompany(loCompnyNm.getModel().getCompanyID(), true);
        params.put("sCompnyNm", loCompnyName.getModel().getCompanyName());

        params.put("sTransNox", poModelMaster.getTransactionNo());
        params.put("dReferDte", SQLUtil.dateFormat(poModelMaster.getTransactionDate(), SQLUtil.FORMAT_LONG_DATE));
        params.put("dTransact", SQLUtil.dateFormat(poModelMaster.getTransactionDate(), SQLUtil.FORMAT_LONG_DATE));
        params.put("sReferN4ox", poModelMaster.getReferenceNo());
        params.put("xRemarksx", poModelMaster.getRemarks());

        params.put("xBranchNm", poModelMaster.getBranchName());
        params.put("xDestinat", poModelMaster.getDestination());
        params.put("sApprval1", loClient_Master.getModel().getFullName());
        params.put("sApprval2", poModelMaster.getApprovedBy());

        JSONArray loArray = new JSONArray();
        JSONObject loJSON2;

        for (int lnCtr = 0; lnCtr <= getItemCount() - 1; lnCtr++) {
            String lsBarcode = "";
            String lsDescript = "";
            Inventory loInventory = GetInventory(poModelDetail.get(lnCtr).getStockID(), true);
            if (loInventory != null) {
                lsBarcode = loInventory.getModel().getBarcode();
                lsDescript = loInventory.getModel().getDescription();
            }

            loJSON2 = new JSONObject();
            loJSON2.put("sField01", lsBarcode);
            loJSON2.put("sField02", lsDescript);
            loJSON2.put("nField01", poModelDetail.get(lnCtr).getQuantity());
            loJSON2.put("nField02", poModelDetail.get(lnCtr).getUnitPrice());
            loJSON2.put("nField03", (poModelDetail.get(lnCtr).getQuantity() * (Double.parseDouble(poModelDetail.get(lnCtr).getUnitPrice().toString()))));
            loArray.add(loJSON2);

        }

        try {
            InputStream stream = new ByteArrayInputStream(loArray.toJSONString().getBytes("UTF-8"));
            JsonDataSource jrjson;

            jrjson = new JsonDataSource(stream);
            JasperPrint jrprint = JasperFillManager.fillReport(poGRider.getReportPath()
                    + "PurchaseOrder.jasper", params, jrjson);

            JasperViewer jv = new JasperViewer(jrprint, false);
            jv.setVisible(true);
            jv.setAlwaysOnTop(true);

            //Create a CountDownLatch
            CountDownLatch latch = new CountDownLatch(1);

            //Add a WindowListener to detect closure
            jv.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    System.out.println("Jasper Report UI is closing");
                    latch.countDown(); // Decrement the latch count
                }
            });

            jv.setVisible(true);

            // Sleep until the latch is counted down
            try {
                latch.await(); // This will block until latch.countDown() is called
            } catch (InterruptedException ex) {
                Logger.getLogger(PurchaseOrder.class.getName()).log(Level.SEVERE, null, ex);
            }

            // For printing
//            JasperExportManager.exportReportToPdfFile(jrprint, "C:/Users/User/Downloads/report.pdf");
        } catch (JRException | UnsupportedEncodingException ex) {
            Logger.getLogger(PurchaseOrder.class.getName()).log(Level.SEVERE, null, ex);
            loJSON.put("result", "error");
            loJSON.put("message", "JRException");
            return loJSON;
        }
        loJSON.put("result", "success");
        loJSON.put("message", "Jasper print record succcess");
        return loJSON;
    }

    private String getSQL() {
        String lsSQL = "SELECT "
                + " a.sTransNox sTransNox "
                + ", a.sBranchCd sBranchCd "
                + ", a.dTransact dTransact "
                + ", a.sCompnyID sCompnyID "
                + ", a.sDestinat sDestinat "
                + ", a.sSupplier sSupplier "
                + ", a.sAddrssID sAddrssID "
                + ", a.sContctID sContctID "
                + ", a.sReferNox sReferNox "
                + ", a.sTermCode sTermCode "
                + ", a.nTranTotl nTranTotl "
                + ", a.cVATaxabl cVATaxabl "
                + ", a.nVatRatex nVatRatex "
                + ", a.nTWithHld nTWithHld "
                + ", a.nDiscount nDiscount "
                + ", a.nAddDiscx nAddDiscx "
                + ", a.nAmtPaidx nAmtPaidx "
                + ", a.nNetTotal nNetTotal "
                + ", a.sRemarksx sRemarksx "
                + ", a.sSourceCd sSourceCd "
                + ", a.sSourceNo sSourceNo "
                + ", a.cEmailSnt cEmailSnt "
                + ", a.nEmailSnt nEmailSnt "
                + ", a.nEntryNox nEntryNox "
                + ", a.sCategrCd sCategrCd "
                + ", a.cTranStat cTranStat "
                + ", a.dPrepared dPrepared "
                + ", a.sApproved sApproved "
                + ", a.dApproved dApproved "
                + ", a.sAprvCode sAprvCode "
                + ", a.sPostedxx sPostedxx "
                + ", a.dPostedxx dPostedxx "
                + ", a.sModified sModified "
                + ", a.dModified dModified "
                + ", b.sBranchNm xBranchNm "
                + ", c.sCompnyNm xCompnyNm "
                + ", d.sBranchNm xDestinat "
                + ", e.sCompnyNm xSupplier "
                + ", f.sAddressx xAddressx "
                + ", g.sCPerson1 xCPerson1 "
                + ", h.sCPerson1 xCPerson2 "
                + ", i.sMobileNo xCPMobil1 "
                + ", j.sDescript xTermName "
                + ",  k.sDescript xCategrNm "
                + " FROM " + "PO_Master" + " a "
                + " LEFT JOIN Branch b  ON a.sBranchCd = b.sBranchCd "
                + " LEFT JOIN Company c  ON a.sCompnyID = c.sCompnyID "
                + " LEFT JOIN Branch d ON a.sBranchCd = d.sBranchCd "
                + " LEFT JOIN Client_Master e  ON a.sSupplier = e.sClientID "
                + " LEFT JOIN Client_Address f  ON a.sAddrssID = f.sAddrssID "
                + " LEFT JOIN Client_Institution_Contact_Person g  ON a.sContctID = g.sContctID AND  g.cPrimaryx = '1'"
                + " LEFT JOIN Client_Institution_Contact_Person h  ON a.sContctID = g.sContctID AND  h.cPrimaryx = '0'"
                + " LEFT JOIN Client_Mobile i  ON a.sContctID = i.sClientID "
                + " LEFT JOIN Term j  ON a.sTermCode = j.sTermCode "
                + "  LEFT JOIN Category k "
                + "    ON a.sCategrCd = k.sCategrCd "
                + "  LEFT JOIN PO_Detail m "
                + "	on  m.sTransNox = a.sTransNox "
                + "  LEFT JOIN Inventory n "
                + "	on n.sStockIDx = m.sStockIDx";
        return lsSQL;
    }
}
