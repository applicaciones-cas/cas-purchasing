package org.guanzon.cas.purchasing.validator;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.guanzon.appdriver.agent.MatrixAuthManager;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.iface.GValidator;
import org.guanzon.cas.inv.InvTransCons;
import org.guanzon.cas.purchasing.status.PurchaseOrderStatus;
import org.guanzon.cas.purchasing.model.Model_PO_Detail;
import org.guanzon.cas.purchasing.model.Model_PO_Master;
import org.json.simple.JSONObject;

public class PurchaseOrder_General implements GValidator {
    MatrixAuthManager poMatrix;
    GRiderCAS poGrider;
    String psTranStat;
    JSONObject poJSON;
    String SOURCE_CD = InvTransCons.PURCHASE_ORDER;
    
    Model_PO_Master poMaster;
    ArrayList<Model_PO_Detail> poDetail;

    @Override
    public void setApplicationDriver(Object applicationDriver) {
        poGrider = (GRiderCAS) applicationDriver;
    }

    @Override
    public void setTransactionStatus(String transactionStatus) {
        psTranStat = transactionStatus;
    }

    @Override
    public void setMaster(Object value) {
        poMaster = (Model_PO_Master) value;
    }

    @Override
    public void setDetail(ArrayList<Object> value) {
        poDetail.clear();
        for (int lnCtr = 0; lnCtr <= value.size() - 1; lnCtr++) {
            poDetail.add((Model_PO_Detail) value.get(lnCtr));
        }
    }

    @Override
    public void setOthers(ArrayList<Object> value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public JSONObject validate() {
        //validate status change request
        JSONObject loJson = StatusChangeValidator.validatePOStatChange(poMaster, psTranStat);
        if (!"success".equals((String) loJson.get("result"))) {
            return loJson;
        }

        try {
            switch (psTranStat) {
                case PurchaseOrderStatus.OPEN:
                    return validateNew();
                case PurchaseOrderStatus.CONFIRMED:
                    return validateConfirmed();
                case PurchaseOrderStatus.APPROVED:
                    return validateApproved();
                case PurchaseOrderStatus.CANCELLED:
                    return validateCancelled();
                case PurchaseOrderStatus.VOID:
                    return validateVoid();
                default:
                    poJSON = new JSONObject();
                    poJSON.put("result", "success");
            }

            return poJSON;
        } catch (SQLException ex) {
            Logger.getLogger(PurchaseOrder_MP.class.getName()).log(Level.SEVERE, null, ex);
        } catch (GuanzonException ex) {
            Logger.getLogger(PurchaseOrder_General.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private JSONObject validateNew() throws SQLException {
        poJSON = new JSONObject();
        LocalDate transactionDate = new java.sql.Date(poMaster.getTransactionDate().getTime()).toLocalDate();
        LocalDate expectedDate = new java.sql.Date(poMaster.getExpectedDate().getTime()).toLocalDate();
        LocalDate serverDate = new java.sql.Date(poGrider.getServerDate().getTime()).toLocalDate();
        LocalDate oneYearAgo = serverDate.minusYears(1);

        if (poMaster.getSupplierID() == null || poMaster.getSupplierID().isEmpty()) {
            poJSON.put("message", "Invalid Suuplier.");
            return poJSON;
        }
        if (transactionDate == null) {
            poJSON.put("message", "Invalid Transaction Date.");
            return poJSON;
        }

        if (LocalDate.of(1900, 1, 1).equals(transactionDate)) {
            poJSON.put("message", "Invalid Transaction Date.");
            return poJSON;
        }

        if (transactionDate.isAfter(serverDate)) {
            poJSON.put("message", "Future transaction dates are not allowed.");
            return poJSON;
        }

        // Backdated beyond 1 year validation
        if (transactionDate.isBefore(oneYearAgo)) {
            poJSON.put("message", "Backdated transactions beyond 1 year are not allowed.");
            return poJSON;
        }

        if (poMaster.getDestinationID() == null || poMaster.getDestinationID().isEmpty()) {
            poJSON.put("message", "Destination is not set.");
            return poJSON;
        }
        if (poMaster.getExpectedDate() == null) {
            poJSON.put("message", "Invalid Expected Delivery Date.");
            return poJSON;
        }
        if ("1900-01-01".equals(expectedDate)) {
            poJSON.put("message", "Invalid Expected Delivery Transaction Date.");
            return poJSON;
        }
        if (expectedDate.isBefore(transactionDate)) {
            poJSON.put("message", "The expected date cannot be earlier than the transaction date.");
            return poJSON;
        }

        if (poMaster.getTermCode() == null || poMaster.getTermCode().isEmpty()) {
            poJSON.put("message", "Invalid Term.");
            return poJSON;
        }
        if (poMaster.getWithAdvPaym()) {
            double rate = poMaster.getDownPaymentRatesPercentage().doubleValue();
            double amount = poMaster.getDownPaymentRatesAmount().doubleValue();
            if (rate <= 0.0000 && amount <= 0.0000) {
                poJSON.put("message", "You selected advance rate, but no Adv Rate and Adv Amount.");
                return poJSON;
            }
        }
        if (poMaster.getEditMode() == EditMode.ADDNEW) {
            if (transactionDate.isBefore(serverDate) && poMaster.getReference().trim().isEmpty()) {
                poJSON.put("message", "A reference number is required for backdated transactions.");
                return poJSON;
            }
        }

        poJSON.put("result", "success");
        return poJSON;
    }

    private JSONObject validateConfirmed() throws SQLException, GuanzonException {
        poJSON = new JSONObject();

        poJSON.put("result", "success");
        return poJSON;
    }

//    private JSONObject validateApproved() throws SQLException, GuanzonException {
//        String lsRemarks;
//        poJSON = new JSONObject();
//        
//        //crete a remarks for the Purchase Order Transaction
//        lsRemarks = poMaster.getBranchCode()  
//            + "/" + poMaster.getTransactionNo() 
//            + ";" + SQLUtil.dateFormat(poMaster.getTransactionDate(), "yyyy-MM-dd") 
//            + ";" + poMaster.getTranTotal().toString() 
//            + ";" + poMaster.getRemarks();
//
//        
//        String lsSQL = "SELECT sAuthType FROM xxxSysAuth_Matrix_Master" +
//                        " WHERE " + poMaster.getTranTotal().doubleValue() + " BETWEEN nValueFrm AND nValueTox";
//        
//        ResultSet loRS = poGrider.executeQuery(lsSQL);
//        
//        if (!loRS.next()){
//            poJSON.put("result", "error");
//            poJSON.put("message", "Transaction amount is out of range from the matrix.");
//            return poJSON;
//        }
//        
//        switch (loRS.getString("sAuthType")) {
//            case "PO*S":
//                poMatrix = new MatrixAuthManager(poGrider, SOURCE_CD, poMaster.getTransactionNo());
//                poMatrix.addAuthRequest("PO*S", "", "", lsRemarks);
//                break;
//            case "PO*M":
//                poMatrix = new MatrixAuthManager(poGrider, SOURCE_CD, poMaster.getTransactionNo());
//                poMatrix.addAuthRequest("PO*M", "", "", lsRemarks);
//                break;
//            case "PO*L":
//                poMatrix = new MatrixAuthManager(poGrider, SOURCE_CD, poMaster.getTransactionNo());
//                poMatrix.addAuthRequest("PO*L", "", "", lsRemarks);
//                break;
//            case "PO*XL":
//                poMatrix = new MatrixAuthManager(poGrider, SOURCE_CD, poMaster.getTransactionNo());
//                poMatrix.addAuthRequest("PO*XL", "", "", lsRemarks);
//                break;
//            default:
//                poJSON.put("result", "error");
//                poJSON.put("message", "Transaction amount is out of range from the matrix.");
//                return poJSON;
//        }
//        
//        //This will create an authorization request for Validation
//        poJSON = poMatrix.processAuth();
//
//        return poJSON;
//    }

    private JSONObject validateApproved() throws SQLException, GuanzonException {
        poMatrix = new MatrixAuthManager(poGrider, SOURCE_CD, poMaster.getTransactionNo());
        
        //Get the difference of purchase order receiving and purchase order
        String lsSQL = "SELECT " +
                            "  COALESCE(a.sTransNox, b.sTransNox) AS sTransNox" +
                            ", COALESCE(c.sStockIDx, d.sStockIDx) AS sStockIDx" +
                            ", ((IFNULL(b.nTranTotl, 0) - IFNULL(a.nTranTotl, 0)) / NULLIF(b.nTranTotl, 0) * 100) AS nTotlDiff" +
                            ", ((IFNULL(d.nQuantity, 0) - IFNULL(c.nQuantity, 0)) / NULLIF(d.nQuantity, 0) * 100) AS nQtyxDiff" +
                            ", ((IFNULL(d.nUnitPrce, 0) - IFNULL(c.nUnitPrce, 0)) / NULLIF(d.nUnitPrce, 0) * 100) AS nPrceDiff" +
                            ", c.sSourceNo sOrderNox" +
                            ", b.nTranTotl nPTotlAmt" +
                            ", a.nTranTotl nRTotlAmt" +
                            ", d.nQuantity nPOQtyxxx" +
                            ", c.nQuantity nRcQtyxxx" +
                            ", d.nUnitPrce nPUntPrce" +
                            ", c.nUnitPrce nRUntPrce" +
                      " FROM PO_Master a" +
                            " LEFT JOIN PO_Detail c ON a.sTransNox = c.sTransNox" +
                            " LEFT JOIN PO_Quotation_Detail d ON c.sSourceNo = d.sTransNox AND c.sStockIDx = d.sStockIDx " +
                            " LEFT JOIN PO_Quotation_Master b ON d.sTransNox = b.sTransNox" +
                      " WHERE a.sTransNox = " + SQLUtil.toSQL(poMaster.getTransactionNo()) +
                      " UNION" +
                      " SELECT " +
                            "  COALESCE(a.sTransNox, b.sTransNox) AS sTransNox" +
                            ", COALESCE(c.sStockIDx, d.sStockIDx) AS sStockIDx" +
                            ", ((IFNULL(b.nTranTotl, 0) - IFNULL(a.nTranTotl, 0)) / NULLIF(b.nTranTotl, 0) * 100) AS nTotlDiff" +
                            ", ((IFNULL(d.nQuantity, 0) - IFNULL(c.nQuantity, 0)) / NULLIF(d.nQuantity, 0) * 100) AS nQtyxDiff" +
                            ", ((IFNULL(d.nUnitPrce, 0) - IFNULL(c.nUnitPrce, 0)) / NULLIF(d.nUnitPrce, 0) * 100) AS nPrceDiff" +
                            ", c.sSourceNo sOrderNox" +
                            ", b.nTranTotl nPTotlAmt" +
                            ", a.nTranTotl nRTotlAmt" +
                            ", d.nQuantity nPOQtyxxx" +
                            ", c.nQuantity nRcQtyxxx" +
                            ", d.nUnitPrce nPUntPrce" +
                            ", c.nUnitPrce nRUntPrce" +
                      " FROM PO_Master a" +
                            " RIGHT JOIN PO_Detail c ON a.sTransNox = c.sTransNox" +
                            " RIGHT JOIN PO_Quotation_Detail d ON c.sSourceNo = d.sTransNox AND c.sStockIDx = d.sStockIDx " +
                            " RIGHT JOIN PO_Quotation_Master b ON d.sTransNox = b.sTransNox" +
                      " WHERE a.sTransNox = " + SQLUtil.toSQL(poMaster.getTransactionNo());
        ResultSet loRS = poGrider.executeQuery(lsSQL);
        
        //set to 100% meaning no purchase order
        double lnTotlDiff = 100;
        double lnQtyxDiff = 100;
        double lnPrceDiff = 100;
        
        double lnPTotlAmt = 0;
        double lnRTotlAmt = 0;
        
        int lnRecCtrxx = 0;
        int lnNullCtrx = 0;
        
        //Check for the overall difference between the purchase delivery and purchase order...
        String lsRemarks;
        while(loRS.next()){
            //Get the total purchase receiving total amount
            if(lnRTotlAmt == 0){
                lnRTotlAmt = loRS.getDouble("nRTotlAmt");
            }

            //Count the number of records and the number of records with no order no
            lnRecCtrxx++;
            if(loRS.getString("sOrderNox").isEmpty()){
                lnNullCtrx++;
                continue;
            }
            
            //set the purchase order total, purchase receiving total, and their difference if not yet set
            if(lnTotlDiff != 100){
                lnTotlDiff = loRS.getDouble("nTotlDiff");
                lnPTotlAmt = loRS.getDouble("nPTotlAmt");
            }  
            
            //A difference of positive value means that purchase order quantity is higher than purchase delivery quantity
            //A difference of negative value means that purchase order quantity is lower than purchase delivery quantity
            //A difference of zero(0) means the quantity is the same...
            if(loRS.getDouble("nQtyxDiff") < lnQtyxDiff){
                lnQtyxDiff = loRS.getDouble("nQtyxDiff");
            }
            
            //A difference of positive value means that purchase order price is higher than purchase delivery price
            //A difference of negative value means that purchase order price is lower than purchase delivery price
            //A difference of zero(0) means the price is the same...
            if(loRS.getDouble("nPrceDiff") < lnPrceDiff){
                lnPrceDiff = loRS.getDouble("nPrceDiff");
            }
        }

        //Is the purchase delivery  without a purchase order or is there an item delivered but not ordered
        if(lnRecCtrxx == lnNullCtrx || lnNullCtrx != 0){
            lsRemarks = poMaster.getBranchCode()  
                + "/" + poMaster.getTransactionNo() 
                + ";" + SQLUtil.dateFormat(poMaster.getTransactionDate(), "yyyy-MM-dd") 
                + ";" + poMaster.getTranTotal().toString() 
                + ";" + poMaster.getRemarks();
            
            String lsAuthCode = poMatrix.getAuthType("PURCHASE ORDER MATRIX", String.valueOf(poMaster.getTranTotal().toString()), "");
            poMatrix.addAuthRequest(lsAuthCode, "", "", lsRemarks);
        }
        else{
            //chekc if quantity or price from delivery is higher the purchase order
            if(lnPrceDiff != 0 || lnQtyxDiff != 0 ){
                lsRemarks = poMaster.getBranchCode()  
                    + "/" + poMaster.getTransactionNo() 
                    + ";" + SQLUtil.dateFormat(poMaster.getTransactionDate(), "yyyy-MM-dd") 
                    + ";" + poMaster.getTranTotal().toString() 
                    + ";" + poMaster.getRemarks();

                String lsAuthCode = poMatrix.getAuthType("PURCHASE ORDER MATRIX", String.valueOf(poMaster.getTranTotal().toString()), "");
                poMatrix.addAuthRequest(lsAuthCode, "", "", lsRemarks);
            }
        }
        
        if(poMatrix.hasAuthRequest()){
            try {
                poJSON = poMatrix.processAuth();
            } catch (GuanzonException ex) {
                poJSON.put("result", "error");
                poJSON.put("message", ex.getMessage());
                return poJSON;
            }
            return poJSON;
        }
        
        poJSON.put("result", "success");
        return poJSON;
    }
    
    
    private JSONObject validateCancelled() {
        poJSON = new JSONObject();

        poJSON.put("result", "success");
        return poJSON;
    }

    private JSONObject validateVoid() {
        poJSON = new JSONObject();

        poJSON.put("result", "success");
        return poJSON;
    }
}
