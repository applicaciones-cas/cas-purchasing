/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.guanzon.cas.purchasing.validator;

import ph.com.guanzongroup.cas.model.Model_POR_Master;
import ph.com.guanzongroup.cas.model.Model_POReturn_Master;
import ph.com.guanzongroup.cas.constants.PurchaseOrderReceivingStatus;
import ph.com.guanzongroup.cas.constants.PurchaseOrderReturnStatus;
import org.json.simple.JSONObject;

/**
 *
 * @author Administrator
 */
public class StatusChangeValidator {
    public static JSONObject validatePORcvdStatChange(Model_POR_Master foMaster, String fsTranStat){
        Model_POR_Master poMaster = foMaster;
        String psTranStat = fsTranStat;
        JSONObject poJson = new JSONObject();

        switch (psTranStat){
            case PurchaseOrderReceivingStatus.CONFIRMED:
                if(psTranStat.equalsIgnoreCase(poMaster.getTransactionStatus())){
                    poJson.put("result", "error");
                    poJson.put("message", "Transaction was already confirmed.");
                    return poJson;
                }
                else if(!poMaster.getTransactionStatus().equalsIgnoreCase(PurchaseOrderReceivingStatus.OPEN)){
                    poJson.put("result", "error");
                    poJson.put("message", "Transaction confirmation failed! Please check transaction status.");
                    return poJson;
                }
                break;
            case PurchaseOrderReceivingStatus.POSTED:
                if(psTranStat.equalsIgnoreCase(poMaster.getTransactionStatus())){
                    poJson.put("result", "error");
                    poJson.put("message", "Transaction was already posted.");
                    return poJson;
                }
                //Allow confirmed to the the only transaction status to be posted
                else if(!poMaster.getTransactionStatus().equalsIgnoreCase(PurchaseOrderReceivingStatus.CONFIRMED)){
                    poJson.put("result", "error");
                    poJson.put("message", "Transaction posting failed! Please check transaction status.");
                    return poJson;
                }
                break;
            case PurchaseOrderReceivingStatus.PAID:
                if(psTranStat.equalsIgnoreCase(poMaster.getTransactionStatus())){
                    poJson.put("result", "error");
                    poJson.put("message", "Transaction was already tagged PAID.");
                    return poJson;
                }
                //Allow posted to the the only transaction status to be paid
                else if(!poMaster.getTransactionStatus().equalsIgnoreCase(PurchaseOrderReceivingStatus.POSTED)){
                    poJson.put("result", "error");
                    poJson.put("message", "Transaction tagging to PAID failed! Please check transaction status.");
                    return poJson;
                }
                break;
            case PurchaseOrderReceivingStatus.CANCELLED:
                if(psTranStat.equalsIgnoreCase(poMaster.getTransactionStatus())){
                    poJson.put("result", "error");
                    poJson.put("message", "Transaction was already CANCELLED.");
                    return poJson;
                }
                //Allow confirmed to the the only transaction status to be cancelled
                else if(!poMaster.getTransactionStatus().equalsIgnoreCase(PurchaseOrderReceivingStatus.CONFIRMED)){
                    poJson.put("result", "error");
                    poJson.put("message", "Transaction CANCELLATION failed! Please check transaction status.");
                    return poJson;
                }
                break;
            case PurchaseOrderReceivingStatus.VOID:
                if(psTranStat.equalsIgnoreCase(poMaster.getTransactionStatus())){
                    poJson.put("result", "error");
                    poJson.put("message", "Transaction was already tagged VOID.");
                    return poJson;
                }
                //Allow open to the the only transaction status to be cancelled
                else if(!poMaster.getTransactionStatus().equalsIgnoreCase(PurchaseOrderReceivingStatus.OPEN)){
                    poJson.put("result", "error");
                    poJson.put("message", "Transaction tagging to VOID failed! Please check transaction status.");
                    return poJson;
                }
                break;
        }

        poJson.put("result", "success");
        return poJson;
    }
    
    public static JSONObject validatePOReturnStatChange(Model_POReturn_Master foMaster, String fsTranStat){
        Model_POReturn_Master poMaster = foMaster;
        String psTranStat = fsTranStat;
        JSONObject poJson = new JSONObject();

        switch (psTranStat){
            case PurchaseOrderReturnStatus.CONFIRMED:
                if(psTranStat.equalsIgnoreCase(poMaster.getTransactionStatus())){
                    poJson.put("result", "error");
                    poJson.put("message", "Transaction was already confirmed.");
                    return poJson;
                }
                else if(!poMaster.getTransactionStatus().equalsIgnoreCase(PurchaseOrderReturnStatus.OPEN)){
                    poJson.put("result", "error");
                    poJson.put("message", "Transaction confirmation failed! Please check transaction status.");
                    return poJson;
                }
                break;
            case PurchaseOrderReturnStatus.POSTED:
                if(psTranStat.equalsIgnoreCase(poMaster.getTransactionStatus())){
                    poJson.put("result", "error");
                    poJson.put("message", "Transaction was already posted.");
                    return poJson;
                }
                //Allow confirmed to the the only transaction status to be posted
                else if(!poMaster.getTransactionStatus().equalsIgnoreCase(PurchaseOrderReturnStatus.CONFIRMED)){
                    poJson.put("result", "error");
                    poJson.put("message", "Transaction posting failed! Please check transaction status.");
                    return poJson;
                }
                break;
            case PurchaseOrderReturnStatus.PAID:
                if(psTranStat.equalsIgnoreCase(poMaster.getTransactionStatus())){
                    poJson.put("result", "error");
                    poJson.put("message", "Transaction was already tagged PAID.");
                    return poJson;
                }
                //Allow posted to the the only transaction status to be paid
                else if(!poMaster.getTransactionStatus().equalsIgnoreCase(PurchaseOrderReturnStatus.POSTED)){
                    poJson.put("result", "error");
                    poJson.put("message", "Transaction tagging to PAID failed! Please check transaction status.");
                    return poJson;
                }
                break;
            case PurchaseOrderReturnStatus.CANCELLED:
                if(psTranStat.equalsIgnoreCase(poMaster.getTransactionStatus())){
                    poJson.put("result", "error");
                    poJson.put("message", "Transaction was already CANCELLED.");
                    return poJson;
                }
                //Allow confirmed to the the only transaction status to be cancelled
                else if(!poMaster.getTransactionStatus().equalsIgnoreCase(PurchaseOrderReturnStatus.CONFIRMED)){
                    poJson.put("result", "error");
                    poJson.put("message", "Transaction CANCELLATION failed! Please check transaction status.");
                    return poJson;
                }
                break;
            case PurchaseOrderReturnStatus.VOID:
                if(psTranStat.equalsIgnoreCase(poMaster.getTransactionStatus())){
                    poJson.put("result", "error");
                    poJson.put("message", "Transaction was already tagged VOID.");
                    return poJson;
                }
                //Allow open to the the only transaction status to be cancelled
                else if(!poMaster.getTransactionStatus().equalsIgnoreCase(PurchaseOrderReturnStatus.OPEN)){
                    poJson.put("result", "error");
                    poJson.put("message", "Transaction tagging to VOID failed! Please check transaction status.");
                    return poJson;
                }
                break;
        }

        poJson.put("result", "success");
        return poJson;
    }
}
