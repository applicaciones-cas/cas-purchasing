package org.guanzon.cas.purchasing.validator;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.constant.UserRight;
import org.guanzon.appdriver.iface.GValidator;
import org.guanzon.cas.inv.InvTransCons;
import org.json.simple.JSONObject;
import org.guanzon.cas.purchasing.status.POCancellationStatus;
import org.guanzon.cas.purchasing.model.Model_PO_Cancellation_Detail;
import org.guanzon.cas.purchasing.model.Model_PO_Cancellation_Master;

/**
 *
 * @author MNV t4
 */
public class POCancellation_Monarch implements GValidator {

    GRiderCAS poGRider;
    String psTranStat;
    JSONObject poJSON;

    Model_PO_Cancellation_Master poMaster;
    ArrayList<Model_PO_Cancellation_Detail> paDetail;

    String SOURCE_CD = InvTransCons.PURCHASE_ORDER_CANCELLATION;
    
    @Override
    public void setApplicationDriver(Object applicationDriver) {
        poGRider = (GRiderCAS) applicationDriver;
    }

    @Override
    public void setTransactionStatus(String transactionStatus) {
        psTranStat = transactionStatus;
    }

    @Override
    public void setMaster(Object value) {
        poMaster = (Model_PO_Cancellation_Master) value;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setDetail(ArrayList<Object> value) {
        paDetail = (ArrayList<Model_PO_Cancellation_Detail>) (ArrayList<?>) value;
    }

    @Override
    public void setOthers(ArrayList<Object> value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public JSONObject validate() {
        //validate status change request
        JSONObject loJson = StatusChangeValidator.validatePOCancellationStatChange(poMaster, psTranStat);
        if (!"success".equals((String) loJson.get("result"))) {
            return loJson;
        }
        
        try {
            switch (psTranStat) {
                case POCancellationStatus.OPEN:
                    return validateNew();
                case POCancellationStatus.CONFIRMED:
                    return validateConfirmed();
                case POCancellationStatus.POSTED:
                    return validatePosted();
                case POCancellationStatus.CANCELLED:
                    return validateCancelled();
                case POCancellationStatus.VOID:
                    return validateVoid();
                default:
                    poJSON = new JSONObject();
                    poJSON.put("result", "error");
                    poJSON.put("message", "unsupported function");
            }
        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(POCancellation_Monarch.class.getName()).log(Level.SEVERE, null, ex);
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", ex.getMessage());
        }

        return poJSON;
    }

    private JSONObject validateNew() throws SQLException {
        poJSON = new JSONObject();
        boolean isRequiredApproval = false;

        if (poMaster.getTransactionDate() == null) {
            poJSON.put("result", "error");
            poJSON.put("message", "Invalid Transaction Date.");
            return poJSON;
        }

        //change transaction date 
        if (poMaster.getTransactionDate().after((Date) poGRider.getServerDate())
                && poMaster.getTransactionDate().before((Date) poGRider.getServerDate())) {
            poJSON.put("message", "Change of transaction date are not allowed.! Approval is Required");
            isRequiredApproval = true;
        }
        if (poMaster.getIndustryId() == null || poMaster.getIndustryId().isEmpty()) {
            poJSON.put("result", "error");
            poJSON.put("message", "Industry is not set.");
            return poJSON;
        }
//        if (poMaster.getBranchCode() == null || poMaster.getBranchCode().isEmpty()) {
//            poJSON.put("result", "error");
//            poJSON.put("message", "Branch is not set.");
//            return poJSON;
//        }
        if (poMaster.getSourceNo() == null || poMaster.getSourceNo().isEmpty()) {
            poJSON.put("result", "error");
            poJSON.put("message", "Bank Account is not set.");
            return poJSON;
        }

        int lnDetailCount = 0;
        for (int lnCtr = 0; lnCtr < paDetail.size(); lnCtr++) {
            if (paDetail.get(lnCtr).getStockId() != null
                    && !paDetail.get(lnCtr).getStockId().isEmpty()) {
                if (paDetail.get(lnCtr).getQuantity() > 0) {
                    lnDetailCount++;
                }
            }
        }

        if (lnDetailCount <= 0) {
            poJSON.put("result", "error");
            poJSON.put("message", "Detail is not set.");
            return poJSON;
        }

        poJSON.put("result", "success");
        poJSON.put("isRequiredApproval", isRequiredApproval);

        return poJSON;
    }

    private JSONObject validateConfirmed() throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        boolean isRequiredApproval = false;

        if (poGRider.getUserLevel() <= UserRight.ENCODER) {
            isRequiredApproval = true;
        }
        if (poMaster.getTransactionDate() == null) {
            poJSON.put("result", "error");
            poJSON.put("message", "Invalid Transaction Date.");
            return poJSON;
        }

        if (poMaster.getIndustryId() == null) {
            poJSON.put("result", "error");
            poJSON.put("message", "Industry is not set.");
            return poJSON;
        }

        int lnDetailCount = 0;
        for (int lnCtr = 0; lnCtr < paDetail.size(); lnCtr++) {
            if (paDetail.get(lnCtr).getStockId() != null
                    && !paDetail.get(lnCtr).getStockId().isEmpty()) {
                double lnOrder = Double.valueOf(String.valueOf(paDetail.get(lnCtr).PurchaseOrderDetail().getQuantity()));
                double lnServed = Double.valueOf(String.valueOf(paDetail.get(lnCtr).PurchaseOrderDetail().getReceivedQuantity()));
                double lnprevCancelled = Double.valueOf(String.valueOf(paDetail.get(lnCtr).PurchaseOrderDetail().getCancelledQuantity()));
                double lnCanceled = paDetail.get(lnCtr).getQuantity();
                if (lnOrder < (lnServed + lnprevCancelled + lnCanceled)) {
                    poJSON.put("result", "error");
                    poJSON.put("message", "Detail Cancelled Quantity already exceed Order Quantity. Row = " + lnCtr);
                    return poJSON;
                }
                lnDetailCount++;

            }
        }

        if (lnDetailCount <= 0) {
            poJSON.put("result", "error");
            poJSON.put("message", "Detail is not set.");
            return poJSON;
        }

        if (poGRider.getUserLevel()
                <= UserRight.ENCODER) {
            isRequiredApproval = true;
        }

        poJSON.put(
                "result", "success");
        poJSON.put(
                "isRequiredApproval", isRequiredApproval);

        return poJSON;
    }

    private JSONObject validatePosted() {
        poJSON = new JSONObject();
        boolean isRequiredApproval = false;
        if (poGRider.getUserLevel() <= UserRight.ENCODER) {
            isRequiredApproval = true;
        }

        int lnDetailCount = 0;
        for (int lnCtr = 0; lnCtr < paDetail.size(); lnCtr++) {
            if (paDetail.get(lnCtr).getStockId() != null
                    && !paDetail.get(lnCtr).getStockId().isEmpty()) {

                lnDetailCount++;

            }
        }

        if (lnDetailCount <= 0) {
            poJSON.put("result", "error");
            poJSON.put("message", "Detail is not set.");
            return poJSON;
        }

        poJSON.put("result", "success");
        poJSON.put("isRequiredApproval", isRequiredApproval);
        return poJSON;
    }

    private JSONObject validateCancelled() throws SQLException {
        boolean isRequiredApproval = false;
        poJSON = new JSONObject();

        if (poGRider.getUserLevel() <= UserRight.ENCODER) {
            isRequiredApproval = true;
        }
        poJSON.put("result", "success");
        poJSON.put("isRequiredApproval", isRequiredApproval);
        return poJSON;
    }

    private JSONObject validateVoid() throws SQLException {
        boolean isRequiredApproval = false;
        poJSON = new JSONObject();

        if (poGRider.getUserLevel() <= UserRight.ENCODER) {
            isRequiredApproval = true;
        }
        poJSON.put("result", "success");
//        poJSON.put("isRequiredApproval", isRequiredApproval);
        return poJSON;
    }

}
