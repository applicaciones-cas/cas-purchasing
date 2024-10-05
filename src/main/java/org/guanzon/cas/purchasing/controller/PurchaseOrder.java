package org.guanzon.cas.purchasing.controller;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import org.guanzon.appdriver.iface.GTranDet;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JsonDataSource;
import net.sf.jasperreports.swing.JRViewer;
import net.sf.jasperreports.view.JasperViewer;
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
import org.guanzon.cas.clients.Client_Master;
import org.guanzon.cas.inventory.models.Model_Inv_Stock_Request_Detail;
import org.guanzon.cas.model.clients.Model_Client_Address;
import org.guanzon.cas.model.clients.Model_Client_Institution_Contact;
import org.guanzon.cas.model.clients.Model_Client_Mobile;
import org.guanzon.cas.parameters.Company;
import org.guanzon.cas.parameters.Model;
import org.guanzon.cas.parameters.Term;
import org.guanzon.cas.purchasing.model.Model_PO_Detail;
import org.guanzon.cas.purchasing.model.Model_PO_Master;
import org.guanzon.cas.validators.ValidatorFactory;
import org.guanzon.cas.validators.poquotation.Validator_PO_Quotation_Request_Detail;
import org.guanzon.cas.validators.poquotation.Validator_PO_Quotation_Request_Master;
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
    public boolean psSavingStatus;

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

        pnEditMode = poModelMaster.getEditMode();

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
        System.out.println(poJSON.toJSONString());
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        //checkTest
        poModelDetail = openTransactionDetail(poModelMaster.getTransactionNo());
//        System.out.println(poModelMaster.getEntryNo());
//        System.out.println(poModelDetail.size());

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

//        Validator_PO_Quotation_Request_Detail ValidateDetails = new Validator_PO_Quotation_Request_Detail(poModelDetail);
//        if (!ValidateDetails.isEntryOkay()) {
//            poJSON.put("result", "error");
//            poJSON.put("message", ValidateDetails.getMessage());
//
//            return poJSON;
//        }
//        Validator_PO_Quotation_Request_Master ValidateMasters = new Validator_PO_Quotation_Request_Master(poModelMaster);
//        if (!ValidateMasters.isEntryOkay()) {
//            poJSON.put("result", "error");
//            poJSON.put("message", ValidateMasters.getMessage());
//            return poJSON;
//        }
        if (!pbWthParent) {
            poGRider.beginTrans();
        }
        poJSON = new JSONObject();
        //delete empty detail
        Inventory loInventory;
        Model_Inv_Stock_Request_Detail lo_inv_stock_request_detail;
        if (poModelDetail.get(getItemCount() - 1).getStockID().equals("") && poModelDetail.get(getItemCount() - 1).getStockID().equals("")) {
            RemoveModelDetail(getItemCount() - 1);
        }
        System.out.println("this is saving status " + getSavingStatus());
        for (lnCtr = 0; lnCtr <= getItemCount() - 1; lnCtr++) {
            if (getSavingStatus()) {
                lo_inv_stock_request_detail = this.GetModel_Inv_Stock_Request_Detail(poModelDetail.get(lnCtr).getStockID());
                loInventory = this.GetInventory(poModelDetail.get(lnCtr).getStockID(), true);
                loInventory.getModel().setUnitPrice(poModelDetail.get(lnCtr).getUnitPrice().doubleValue());
                poJSON = loInventory.saveRecord();

                poModelDetail.get(lnCtr).setQtyOnHand(lnCtr);
                poModelDetail.get(lnCtr).setTransactionNo(poModelMaster.getTransactionNo());
                poModelDetail.get(lnCtr).setEntryNo(lnCtr + 1);
//                poModelDetail.get(lnCtr).setUnitPrice( (Number)loInventory.getModel().getUnitPrice());
//                poModelDetail.get(lnCtr).setRecOrder((int) lo_inv_stock_request_detail.getQuantityOnHand()); //ROQ
//                poModelDetail.get(lnCtr).setReceiveNo(1);
//                poModelDetail.get(lnCtr).setCancelledNo(1);
                poJSON = poModelDetail.get(lnCtr).saveRecord();

            }
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
        poModelMaster.setTransactionTotal(poModelMaster.getTransactionTotal());

        for (lnCtr = 0; lnCtr <= poModelDetail.size() - 1; lnCtr++) {
            poModelMaster.setEntryNo(lnCtr + 1);
        }
        poModelMaster.setCategoryCode("0001");
        poModelMaster.setPreparedDate(poGRider.getServerDate());

        poModelMaster.setCompanyID("0");
//        poModelMaster.setAddressID("samp");
//        poModelMaster.setContactID(poModelMaster.getContactID());

//        poModelMaster.setTermCode(poModelMaster.getTermCode());

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

//            case "sDescript": // sDescript
            case "sStockIDx": // 3 // 8-xCategrNm // 9-xInvTypNm
                Inventory loInventory = new Inventory(poGRider, true);
                loInventory.setRecordStatus("1");
                loJSON = loInventory.searchRecord(fsValue, fbByCode);
                double lnTotalTransaction = 0;

                Model_Inv_Stock_Request_Detail lo_inv_stock_request_detail;

                if (loJSON != null) {
                    String newStockID = (String) loInventory.getMaster("sStockIDx");
                    System.out.println(fsValue);
                    boolean isDuplicate = false;

                    for (int i = 0; i < poModelDetail.size() - 1; i++) {
                        String existingStockID = (String) poModelDetail.get(i).getValue("sStockIDx");

                        if (newStockID.equals(existingStockID)) {
                            // Duplicate found, increment quantity by 1
                            int currentQuantity = (Integer) poModelDetail.get(i).getValue("nQuantity");
                            poModelDetail.get(i).setValue("nQuantity", currentQuantity + 1);
                            lnTotalTransaction += Double.parseDouble((poModelDetail.get(i).getValue("nUnitPrce")).toString()) * Double.parseDouble(poModelDetail.get(i).getValue("nQuantity").toString());
                            isDuplicate = true;
                            break;
                        } else {
                            lnTotalTransaction += Double.parseDouble((poModelDetail.get(i).getValue("nUnitPrce")).toString()) * Double.parseDouble(poModelDetail.get(i).getValue("nQuantity").toString());
                            System.out.println(lnTotalTransaction);
                        }
                    }

//                    setMaster("nTranTotl", lnTotalTransaction); // make an additition of unitprce
                    if (!isDuplicate) {
                        // No duplicate found, add new details
                        lo_inv_stock_request_detail = this.GetModel_Inv_Stock_Request_Detail(newStockID);

                        setDetail(fnRow, "sDescript", (String) loInventory.getMaster("sDescript"));
                        setDetail(fnRow, "sStockIDx", (String) loInventory.getMaster("sStockIDx"));
                        setDetail(fnRow, "nUnitPrce", loInventory.getMaster("nUnitPrce"));
                        setDetail(fnRow, "nRecOrder", lo_inv_stock_request_detail.getRecordOrder());
                        setDetail(fnRow, "nQtyOnHnd", lo_inv_stock_request_detail.getQuantity());

//                        setDetail(fnRow, "xCategrNm", (String) loInventory.getMaster("xCategNm2"));
//                        setDetail(fnRow, "xInvTypNm", (String) loInventory.getMaster("xInvTypNm"));
//                        Brand loCategrNm = new Brand(poGRider, true);
//                        loCategrNm.openRecord( (String) loInventory.getMaster("sBrandIDx"));
//                        setMaster( "xCategrNm", (String) loCategrNm.getMaster("sDescript"));
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
                "Transaction No»Destination»Supplier",
                "sTransNox»sDestinat»sSupplier",
                "sTransNox»sDestinat»sSupplier",
                fbByCode ? 0 : 1);

        if (poJSON != null) {
            poJSON = openTransaction((String) poJSON.get("sTransNox"));

        } else {
            poJSON.put("result", "error");
            poJSON.put("message", "No Transaction loaded to update.");
            return poJSON;
        }
        return poJSON;
    }

    public JSONObject searchDestination(String fsColNme, String fsValue, boolean fbByCode) {
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
                "Destination»Transaction No»Supplier",
                "sDestinat»sTransNox»sSupplier",
                "sDestinat»sTransNox»sSupplier",
                fbByCode ? 0 : 0);

        if (poJSON != null) {
            return openTransaction((String) poJSON.get("sTransNox"));

        } else {
            poJSON.put("result", "error");
            poJSON.put("message", "No Transaction loaded to update.");
            return poJSON;
        }
    }

    public JSONObject searchSupplier(String fsColNme, String fsValue, boolean fbByCode) {
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
                "Supplier»Transaction No»Destination",
                "sSupplier»sTransNox»sDestinat",
                "sSupplier»sTransNox»sDestinat",
                fbByCode ? 0 : 0);

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

    public void setSavingStatus(boolean fsValue) {
        psSavingStatus = fsValue;
    }

    public boolean getSavingStatus() {
        return psSavingStatus;
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
        instance.openRecord(fsPrimaryKey); //
        return instance;
    }

    public Model GetModel(String fsPrimaryKey, boolean fbByCode) {
        Model instance = new Model(poGRider, fbByCode);
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

    public int getDetailModel() {
        return poModelDetail.size();
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

        params.put("sReportNm", "samp");
        params.put("sReportDt", "samp");
        params.put("sPrintdBy", "samp"); //poModelMaster.getPreparedBy()
        params.put("sCompnyNm", "Guanzon Group");

        params.put("sTransNox", poModelMaster.getTransactionNo());
        params.put("dReferDte", SQLUtil.dateFormat(poModelMaster.getTransactionDate(), SQLUtil.FORMAT_LONG_DATE));
        params.put("dTransact", SQLUtil.dateFormat(poModelMaster.getTransactionDate(), SQLUtil.FORMAT_LONG_DATE));
        params.put("sReferNox", poModelMaster.getReferenceNo());
//        params.put("sPrintdBy", psClientNm);
        params.put("xRemarksx", poModelMaster.getRemarks());

        params.put("xBranchNm", poModelMaster.getBranchName());
        params.put("xDestinat", poModelMaster.getDestination());
        params.put("sApprval1", loClient_Master.getModel().getFullName());
        params.put("sApprval2", poModelMaster.getApprovedBy());

//        String lsSQL = "SELECT sClientNm FROM Client_Master WHERE sClientID IN ("
//                + "SELECT sEmployNo FROM xxxSysUser WHERE sUserIDxx = " + SQLUtil.toSQL(poData.getApprovedBy().isEmpty() ? poData.getPreparedBy() : poData.getApprovedBy()) + ")";
//        ResultSet loRS = poGRider.executeQuery(lsSQL);
//
//        try {
//            if (loRS.next()) {
//                params.put("sApprval1", loRS.getString("sClientNm"));
//            } else {
//                params.put("sApprval1", "");
//            }
//        } catch (SQLException ex) {
//            Logger.getLogger(POReturn.class.getName()).log(Level.SEVERE, null, ex);
//        }
        JSONArray loArray = new JSONArray();
        JSONObject loJSON2 = new JSONObject();

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
        System.out.println(loArray);
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
            System.out.println("Waiting for Jasper Report to close...");
            try {
                latch.await(); // This will block until latch.countDown() is called
            } catch (InterruptedException ex) {
                Logger.getLogger(PurchaseOrder.class.getName()).log(Level.SEVERE, null, ex);
            }
            // For printing
            JasperExportManager.exportReportToPdfFile(jrprint, "C:/Users/User/Downloads/report.pdf");

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
}
